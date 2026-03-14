package com.example.moneychangersdatacollectionsystem.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.moneychangersdatacollectionsystem.data.local.dao.CollectionDao
import com.example.moneychangersdatacollectionsystem.data.local.entity.toDomain
import com.example.moneychangersdatacollectionsystem.data.local.entity.toEntity
import com.example.moneychangersdatacollectionsystem.domain.model.CollectionRecord
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import com.example.moneychangersdatacollectionsystem.domain.repository.CollectionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    private val supabaseClient: SupabaseClient
) : CollectionRepository {

    private val postgrest = supabaseClient.postgrest
    private val storage = supabaseClient.storage

    override fun getCollections(): Flow<List<CollectionRecord>> {
        return collectionDao.getAllCollections().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCollectionsPaged(): Flow<PagingData<CollectionRecord>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { collectionDao.getAllCollectionsPaged() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun searchCollectionsPaged(
        id: String?,
        name: String?,
        county: String?,
        category: String?,
        agentId: String?,
        marital: String?,
        edu: String?,
        origin: String?,
        gender: String?,
        idType: String?,
        role: String?,
        hasBank: Boolean?,
        remittance: Boolean?,
        address: String?,
        nationality: String?
    ): Flow<PagingData<CollectionRecord>> {
        val sId = id?.takeIf { it.isNotBlank() }
        val sName = name?.takeIf { it.isNotBlank() }
        val sCounty = county?.takeIf { it.isNotBlank() }
        val sCategory = category?.takeIf { it.isNotBlank() }
        val sAgentId = agentId?.takeIf { it.isNotBlank() }
        val sMarital = marital?.takeIf { it.isNotBlank() }
        val sEdu = edu?.takeIf { it.isNotBlank() }
        val sOrigin = origin?.takeIf { it.isNotBlank() }
        val sGender = gender?.takeIf { it.isNotBlank() }
        val sIdType = idType?.takeIf { it.isNotBlank() }
        val sRole = role?.takeIf { it.isNotBlank() }
        val sAddress = address?.takeIf { it.isNotBlank() }
        val sNationality = nationality?.takeIf { it.isNotBlank() }

        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                collectionDao.searchCollectionsPaged(
                    sId, sName, sCounty, sCategory, sAgentId, sMarital, sEdu, sOrigin, sGender, sIdType, sRole, hasBank, remittance, sAddress, sNationality
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun getPendingCollections(): List<CollectionRecord> {
        return collectionDao.getCollectionsBySyncStatus(SyncStatus.PENDING).map { it.toDomain() }
    }

    override suspend fun saveCollection(record: CollectionRecord): Result<Unit> {
        return try {
            collectionDao.insertCollection(record.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CollectionRepo", "Save Local Error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateSyncStatus(id: String, status: SyncStatus): Result<Unit> {
        return try {
            collectionDao.updateSyncStatus(id, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePhotoUrls(id: String, photoUrl: String, idCardUrl: String): Result<Unit> {
        return try {
            collectionDao.updatePhotoUrls(id, photoUrl, idCardUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPhoto(byteArray: ByteArray, fileName: String): Result<String> {
        return try {
            val bucket = storage.from("collection-photos")
            bucket.upload(fileName, byteArray)
            val url = bucket.publicUrl(fileName)
            Result.success(url)
        } catch (e: Exception) {
            Log.e("CollectionRepo", "Upload Photo Error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun syncFromRemote(): Result<Unit> {
        return try {
            Log.d("CollectionRepo", "Starting Smart Sync from Supabase...")
            
            // 1. Fetch data from Supabase in batches
            val batchSize = 1000
            var offset = 0
            var hasMore = true
            var totalSynced = 0

            while (hasMore) {
                val remoteRecords = postgrest.from("collections")
                    .select {
                        order("timestamp", Order.DESCENDING)
                        range(offset.toLong(), (offset + batchSize - 1).toLong())
                    }
                    .decodeList<CollectionRecord>()

                if (remoteRecords.isEmpty()) {
                    hasMore = false
                } else {
                    val entities = remoteRecords.map { it.copy(syncStatus = SyncStatus.SYNCED).toEntity() }
                    collectionDao.insertCollections(entities)
                    
                    totalSynced += remoteRecords.size
                    Log.d("CollectionRepo", "Synced batch: $offset to ${offset + remoteRecords.size}. Total: $totalSynced")
                    
                    if (remoteRecords.size < batchSize) {
                        hasMore = false
                    } else {
                        offset += batchSize
                    }
                }
            }
            
            Log.d("CollectionRepo", "Smart Sync completed. Total synced: $totalSynced")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CollectionRepo", "Sync Error: ${e.message}")
            Result.failure(e)
        }
    }
}
