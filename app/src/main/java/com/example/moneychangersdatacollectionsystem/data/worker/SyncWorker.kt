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

                    // 1. Prepare files for upload and cleanup
                    var finalPhotoUrl = record.photoUrl
                    var finalIdCardUrl = record.idCardPhotoUrl
                    val filesToCleanup = mutableListOf<String>()

                    // 2. Upload Main Photos
                    if (isLocalUri(finalPhotoUrl)) {
                        filesToCleanup.add(finalPhotoUrl)
                        finalPhotoUrl = uploadFile(record.id, "individual", finalPhotoUrl)
                    }

                    if (isLocalUri(finalIdCardUrl)) {
                        filesToCleanup.add(finalIdCardUrl)
                        finalIdCardUrl = uploadFile(record.id, "id_card", finalIdCardUrl)
                    }

                    // Update local record main URLs
                    repository.updatePhotoUrls(record.id, finalPhotoUrl, finalIdCardUrl)

                    // 3. Upload Nested Person Photos (for Forex Bureau)
                    val updatedPersons = record.persons.map { person ->
                        if (isLocalUri(person.idCardPhotoUrl)) {
                            filesToCleanup.add(person.idCardPhotoUrl)
                            val remoteUrl = uploadFile(person.id, "person_id_card", person.idCardPhotoUrl)
                            person.copy(idCardPhotoUrl = remoteUrl)
                        } else {
                            person
                        }
                    }
                    
                    if (updatedPersons != record.persons) {
                        repository.updatePersonPhotoUrls(record.id, updatedPersons)
                    }

                    // 4. Send the complete record with remote URLs to Supabase Database
                    val finalRecord = record.copy(
                        photoUrl = finalPhotoUrl,
                        idCardPhotoUrl = finalIdCardUrl,
                        persons = updatedPersons,
                        syncStatus = SyncStatus.SYNCED 
                    )
                    
                    Log.d("SyncWorker", "Sending record ${record.id} to Supabase Database...")
                    supabaseClient.postgrest.from("collections").upsert(finalRecord)
                    
                    // 5. Success! Mark as SYNCED and cleanup local files
                    repository.updateSyncStatus(record.id, SyncStatus.SYNCED)
                    Log.d("SyncWorker", "SUCCESS: Record ${record.id} is now LIVE")
                    
                    filesToCleanup.forEach { cleanupLocalFile(it) }
                    
                } catch (e: Exception) {
                    Log.e("SyncWorker", "FAILED to sync record ${record.id}: ${e.localizedMessage}")
                    repository.updateSyncStatus(record.id, SyncStatus.FAILED)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "SyncWorker CRITICAL failure: ${e.message}")
            Result.retry()
        }
    }

    private fun isLocalUri(uri: String): Boolean {
        return uri.startsWith("content://") || uri.startsWith("file://")
    }

    private suspend fun uploadFile(recordId: String, type: String, uriString: String): String {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = applicationContext.contentResolver.openInputStream(uri) 
                ?: throw Exception("Could not open stream for $uriString")
            
            inputStream.use { input ->
                val bytes = input.readBytes()
                val fileName = "${recordId.replace("/", "_")}/${type}_${System.currentTimeMillis()}.jpg"
                val bucket = supabaseClient.storage.from("collection-photos")
                bucket.upload(fileName, bytes)
                bucket.publicUrl(fileName)
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "File upload error ($type) for $recordId: ${e.message}")
            throw e
        }
    }
    
    private fun cleanupLocalFile(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("SyncWorker", "Cleanup: Deleted local file $uriString -> $deleted")
                }
            } else if (uriString.contains("/files/collections/")) {
                val path = uriString.substringAfter("/files/collections/")
                val file = File(File(applicationContext.filesDir, "collections"), path)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("SyncWorker", "Cleanup: Deleted internal file $path -> $deleted")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Cleanup failed for $uriString: ${e.message}")
        }
    }
}
