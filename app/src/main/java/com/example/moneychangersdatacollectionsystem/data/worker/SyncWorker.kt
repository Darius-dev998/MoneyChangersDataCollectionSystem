package com.example.moneychangersdatacollectionsystem.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CollectionRepository,
    private val supabaseClient: SupabaseClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pendingRecords = repository.getPendingCollections()
            if (pendingRecords.isEmpty()) {
                Log.d("SyncWorker", "No pending records found to sync")
                return@withContext Result.success()
            }

            Log.d("SyncWorker", "Starting UPLOAD sync for ${pendingRecords.size} records")

            for (record in pendingRecords) {
                try {
                    repository.updateSyncStatus(record.id, SyncStatus.SYNCING)

                    // 1. Upload Photos to Supabase Storage
                    var finalPhotoUrl = record.photoUrl
                    var finalIdCardUrl = record.idCardPhotoUrl
                    
                    val originalPhotoUri = record.photoUrl
                    val originalIdCardUri = record.idCardPhotoUrl

                    // Upload Individual Photo if it's a local URI
                    if (finalPhotoUrl.startsWith("content://") || finalPhotoUrl.startsWith("file://")) {
                        Log.d("SyncWorker", "Uploading individual photo for ${record.id}")
                        finalPhotoUrl = uploadFile(record.id, "individual", finalPhotoUrl)
                    }

                    // Upload ID Card Photo if it's a local URI
                    if (finalIdCardUrl.startsWith("content://") || finalIdCardUrl.startsWith("file://")) {
                        Log.d("SyncWorker", "Uploading ID card photo for ${record.id}")
                        finalIdCardUrl = uploadFile(record.id, "id_card", finalIdCardUrl)
                    }

                    // 2. Update local record with public URLs before sending to remote
                    repository.updatePhotoUrls(record.id, finalPhotoUrl, finalIdCardUrl)

                    // 3. Upsert to Postgrest (Remote Database)
                    val finalRecord = record.copy(
                        photoUrl = finalPhotoUrl,
                        idCardPhotoUrl = finalIdCardUrl,
                        syncStatus = SyncStatus.SYNCED 
                    )
                    
                    Log.d("SyncWorker", "Sending record ${record.id} to Supabase Database...")
                    val response = supabaseClient.postgrest.from("collections").upsert(finalRecord)
                    
                    // 4. If successful, update local status and cleanup
                    repository.updateSyncStatus(record.id, SyncStatus.SYNCED)
                    Log.d("SyncWorker", "SUCCESS: Record ${record.id} is now LIVE on Supabase")
                    
                    cleanupLocalFile(originalPhotoUri)
                    cleanupLocalFile(originalIdCardUri)
                    
                } catch (e: Exception) {
                    Log.e("SyncWorker", "FAILED to sync record ${record.id}: ${e.localizedMessage}")
                    repository.updateSyncStatus(record.id, SyncStatus.FAILED)
                    // We don't return Result.retry() here immediately so we can try the next record
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "SyncWorker CRITICAL failure: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun uploadFile(recordId: String, type: String, uriString: String): String {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = applicationContext.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Could not read bytes from $uriString")
            inputStream.close()

            val fileName = "${recordId.replace("/", "_")}/${type}_${System.currentTimeMillis()}.jpg"
            val bucket = supabaseClient.storage.from("collection-photos")
            bucket.upload(fileName, bytes)
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            Log.e("SyncWorker", "File upload error ($type): ${e.message}")
            throw e
        }
    }
    
    private fun cleanupLocalFile(uriString: String) {
        try {
            if (uriString.startsWith("file://")) {
                val path = uriString.substringAfter("file://")
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } else if (uriString.contains("/files/collections/")) {
                val path = uriString.substringAfter("content://${applicationContext.packageName}.fileprovider/my_files")
                val file = File(applicationContext.filesDir, path)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Cleanup failed: ${e.message}")
        }
    }
}
