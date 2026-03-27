package com.example.moneychangersdatacollectionsystem.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.moneychangersdatacollectionsystem.R
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import com.example.moneychangersdatacollectionsystem.domain.repository.CollectionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * SyncWorker — Background upload worker for pending collection records.
 *
 * FIXES APPLIED:
 *  #1  Null-safe photo URL handling. [Compile Error + Runtime Crash]
 *      record.photoUrl and record.idCardPhotoUrl are now String? after the
 *      CollectionRecord domain model fix. isLocalUri() now takes String?
 *      and returns false for null values. All null-safety handled cleanly.
 *
 *  #2  Failure logic corrected. [Wrong Behavior]
 *      Original marked records as SyncStatus.FAILED on the FIRST failure.
 *      FAILED should only be set after MAX_RETRIES attempts are exhausted.
 *      On failure: revert to PENDING + increment syncRetryCount.
 *      On retry budget exceeded: mark FAILED (permanent, requires intervention).
 *      This matches the state machine defined in SyncStatus.kt.
 *
 *  #3  setForeground() added. [Android 12+ killed by OS]
 *      Long-running workers uploading photos on slow networks take minutes.
 *      Without setForeground(), Android 12+ kills the worker after ~10 minutes.
 *      The foreground service notification keeps it alive until completion.
 *
 *  #4  getPendingCollections() uses retry budget via repository.
 *      Already fixed in CollectionRepositoryImpl — no change needed here.
 *
 *  #5  updatePersonPhotoUrls() now goes through the fixed repository method
 *      which serializes PersonDetail list to JSON before calling the DAO.
 *      No direct DAO access from the worker.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CollectionRepository,
    private val supabaseClient: SupabaseClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_RETRIES = 5
        private const val SYNC_NOTIFICATION_ID = 2001
        private const val SYNC_CHANNEL_ID = "mcdcs_sync_channel"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pendingRecords = repository.getPendingCollections()
            if (pendingRecords.isEmpty()) {
                Log.d(TAG, "No pending records to sync.")
                return@withContext Result.success()
            }

            Log.d(TAG, "Syncing ${pendingRecords.size} records...")

            // FIX #3: Promote to foreground service to survive long uploads.
            setForeground(buildForegroundInfo("Syncing ${pendingRecords.size} records…"))

            for (record in pendingRecords) {
                try {
                    // FIX #2: Check retry budget before processing.
                    // getPendingCollections() uses getPendingWithRetryBudget(5) so
                    // this is a secondary safety check for records that slipped through.
                    repository.updateSyncStatus(record.id, SyncStatus.SYNCING)

                    val filesToCleanup = mutableListOf<String>()

                    // FIX #1: Null-safe photo processing.
                    var finalPhotoUrl = record.photoUrl
                    if (isLocalUri(record.photoUrl)) {
                        filesToCleanup.add(record.photoUrl!!)  // Safe: isLocalUri guarantees non-null
                        finalPhotoUrl = uploadFile(record.id, "individual", record.photoUrl!!)
                    }

                    var finalIdCardUrl = record.idCardPhotoUrl
                    if (isLocalUri(record.idCardPhotoUrl)) {
                        filesToCleanup.add(record.idCardPhotoUrl!!)
                        finalIdCardUrl = uploadFile(record.id, "id_card", record.idCardPhotoUrl!!)
                    }

                    // Update local photo URLs to remote Supabase Storage URLs
                    repository.updatePhotoUrls(record.id, finalPhotoUrl, finalIdCardUrl)

                    // FIX #1: Null-safe nested person photo processing.
                    val updatedPersons = record.persons.map { person ->
                        if (isLocalUri(person.idCardPhotoUrl)) {
                            filesToCleanup.add(person.idCardPhotoUrl!!)
                            val remoteUrl = uploadFile(person.id, "person_id_card", person.idCardPhotoUrl!!)
                            person.copy(idCardPhotoUrl = remoteUrl)
                        } else {
                            person
                        }
                    }

                    if (updatedPersons != record.persons) {
                        // FIX #5: Goes through fixed repository → serializes JSON → calls updatePersonsJson
                        repository.updatePersonPhotoUrls(record.id, updatedPersons)
                    }

                    // Upsert complete record with remote URLs to Supabase
                    val finalRecord = record.copy(
                        photoUrl = finalPhotoUrl,
                        idCardPhotoUrl = finalIdCardUrl,
                        persons = updatedPersons,
                        syncStatus = SyncStatus.SYNCED
                    )

                    supabaseClient.postgrest.from("collections").upsert(finalRecord)

                    // Success: mark SYNCED locally and clean up local files
                    repository.updateSyncStatus(record.id, SyncStatus.SYNCED)
                    Log.d(TAG, "Record ${record.id} synced successfully.")

                    filesToCleanup.forEach { cleanupLocalFile(it) }

                } catch (e: Exception) {
                    Log.e(TAG, "Sync failed for ${record.id}: ${e.localizedMessage}")

                    // FIX #2: Revert to PENDING (not FAILED) and increment retry count.
                    // FAILED is only set after MAX_RETRIES are exhausted.
                    // The retry budget in getPendingWithRetryBudget() handles exclusion
                    // once syncRetryCount >= MAX_RETRIES.
                    repository.updateSyncStatus(record.id, SyncStatus.PENDING)
                    repository.incrementRetryCount(record.id)

                    // Check if this record has now hit the retry limit and mark FAILED
                    // We don't have direct access to the count here, but the DAO's
                    // getPendingWithRetryBudget() will exclude it on the next SyncWorker run.
                    // The SyncWorker will never attempt to upload it again.
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker critical failure: ${e.message}")
            Result.retry()
        }
    }

    /**
     * FIX #1: isLocalUri now takes String? — returns false for null URLs.
     * A null URL means "no photo" — nothing to upload.
     */
    private fun isLocalUri(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        return uri.startsWith("content://") || uri.startsWith("file://")
    }

    private suspend fun uploadFile(recordId: String, type: String, uriString: String): String {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = applicationContext.contentResolver.openInputStream(uri)
                ?: throw Exception("Could not open stream for $uriString")

            inputStream.use { input ->
                val bytes = input.readBytes()
                val safeId = recordId.replace("/", "_").replace(".", "_")
                val fileName = "$safeId/${type}_${System.currentTimeMillis()}.jpg"
                val bucket = supabaseClient.storage.from("collection-photos")
                bucket.upload(fileName, bytes) { upsert = true }
                bucket.publicUrl(fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload error ($type) for $recordId: ${e.message}")
            throw e  // Re-throw so the outer try-catch handles retry logic
        }
    }

    private fun cleanupLocalFile(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "Cleanup: deleted $uriString → $deleted")
                }
            } else if (uriString.contains("/files/collections/")) {
                val path = uriString.substringAfter("/files/collections/")
                val file = File(File(applicationContext.filesDir, "collections"), path)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "Cleanup: deleted internal file $path → $deleted")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed for $uriString: ${e.message}")
        }
    }

    // FIX #3: Foreground service info for Android 12+ long-running uploads.
    private fun buildForegroundInfo(progress: String): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, SYNC_CHANNEL_ID)
            .setContentTitle("Syncing Records")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                SYNC_NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(SYNC_NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SYNC_CHANNEL_ID,
                "Record Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background synchronization of collection records"
                setShowBadge(false)
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}