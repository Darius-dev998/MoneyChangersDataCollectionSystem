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
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import com.example.moneychangersdatacollectionsystem.domain.repository.CollectionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CollectionRepositoryImpl — Implementation of CollectionRepository.
 *
 * FIXES APPLIED:
 *  #1  updatePersonPhotoUrls() fixed. [Compile Error]
 *      Called collectionDao.updatePersons(id, List<PersonDetail>) — removed method.
 *      Room @Query cannot bind List<PersonDetail> as a SQL parameter.
 *      Fixed: serializes the list to JSON and calls collectionDao.updatePersonsJson().
 *      Uses the same lenient Json config as Converters to ensure compatibility.
 *
 *  #2  syncFromRemote() now calls insertCollectionsAndRebuildFts(). [FTS Stale]
 *      insertCollections() bypassed the FTS rebuild. After every remote sync,
 *      the FTS index was left stale — searches returned ghost/missing results.
 *      insertCollectionsAndRebuildFts() wraps both in a @Transaction.
 *
 *  #3  getPendingCollections() uses retry budget. [Infinite Retry]
 *      Was: getCollectionsBySyncStatus(PENDING) — returned ALL pending records
 *      including those that have failed 10+ times and will never succeed.
 *      Fixed: getPendingWithRetryBudget(maxRetries = 5) — excludes permanently
 *      broken records so SyncWorker doesn't loop on them forever.
 *
 *  #4  updatePhotoUrls() parameters updated to String?. [Type Mismatch]
 *      CollectionEntity.photoUrl and idCardPhotoUrl are now String?.
 *      Interface and implementation updated accordingly.
 *
 *  #5  incrementRetryCount() implemented. [New Method]
 *      Delegates to the atomic DAO method — single SQL UPDATE, no race condition.
 *
 *  #6  searchCollectionsFts() implemented. [New Method]
 *      Delegates to the FTS MATCH DAO query with input sanitization.
 */
@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    private val supabaseClient: SupabaseClient
) : CollectionRepository {

    companion object {
        private const val TAG = "CollectionRepo"
        private const val PAGE_SIZE = 20
        private const val SYNC_BATCH_SIZE = 1000
        private const val MAX_RETRY_COUNT = 5
    }

    // Lenient Json for person serialization — same config as Converters.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    // ── Flows ──────────────────────────────────────────────────────────────

    override fun getCollections(): Flow<List<CollectionRecord>> {
        return collectionDao.getAllCollections().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCollectionsPaged(): Flow<PagingData<CollectionRecord>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { collectionDao.getAllCollectionsPaged() }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override fun searchCollectionsPaged(
        id: String?, name: String?, county: String?, category: String?,
        agentId: String?, marital: String?, edu: String?, origin: String?,
        gender: String?, idType: String?, role: String?,
        hasBank: Boolean?, remittance: Boolean?, address: String?, nationality: String?
    ): Flow<PagingData<CollectionRecord>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                collectionDao.searchCollectionsPaged(
                    id?.takeIf { it.isNotBlank() },
                    name?.takeIf { it.isNotBlank() },
                    county?.takeIf { it.isNotBlank() },
                    category?.takeIf { it.isNotBlank() },
                    agentId?.takeIf { it.isNotBlank() },
                    marital?.takeIf { it.isNotBlank() },
                    edu?.takeIf { it.isNotBlank() },
                    origin?.takeIf { it.isNotBlank() },
                    gender?.takeIf { it.isNotBlank() },
                    idType?.takeIf { it.isNotBlank() },
                    role?.takeIf { it.isNotBlank() },
                    hasBank, remittance,
                    address?.takeIf { it.isNotBlank() },
                    nationality?.takeIf { it.isNotBlank() }
                )
            }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    // FIX #6: FTS-based search implementation
    override fun searchCollectionsFts(
        query: String, county: String?, category: String?, agentId: String?,
        gender: String?, idType: String?, marital: String?, edu: String?,
        origin: String?, role: String?, hasBank: Boolean?, remittance: Boolean?
    ): Flow<PagingData<CollectionRecord>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = {
                collectionDao.searchCollectionsFts(
                    query = sanitizeFtsQuery(query),
                    county = county?.takeIf { it.isNotBlank() },
                    category = category?.takeIf { it.isNotBlank() },
                    agentId = agentId?.takeIf { it.isNotBlank() },
                    gender = gender?.takeIf { it.isNotBlank() },
                    idType = idType?.takeIf { it.isNotBlank() },
                    marital = marital?.takeIf { it.isNotBlank() },
                    edu = edu?.takeIf { it.isNotBlank() },
                    origin = origin?.takeIf { it.isNotBlank() },
                    role = role?.takeIf { it.isNotBlank() },
                    hasBank = hasBank,
                    remittance = remittance
                )
            }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    // ── Reads ──────────────────────────────────────────────────────────────

    // FIX #3: Uses retry budget — excludes permanently broken records.
    override suspend fun getPendingCollections(): List<CollectionRecord> {
        return collectionDao.getPendingWithRetryBudget(MAX_RETRY_COUNT).map { it.toDomain() }
    }

    // ── Writes ──────────────────────────────────────────────────────────────

    override suspend fun saveCollection(record: CollectionRecord): Result<Unit> {
        return try {
            collectionDao.insertCollection(record.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Save local error: ${e.message}")
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

    override suspend fun resetSyncStatus(oldStatus: SyncStatus, newStatus: SyncStatus): Result<Unit> {
        return try {
            collectionDao.resetSyncStatus(oldStatus, newStatus)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FIX #4: String? parameters to match nullable entity fields
    override suspend fun updatePhotoUrls(id: String, photoUrl: String?, idCardUrl: String?): Result<Unit> {
        return try {
            collectionDao.updatePhotoUrls(id, photoUrl, idCardUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * FIX #1: updatePersonPhotoUrls() — serializes persons to JSON before DAO call.
     *
     * Room @Query cannot bind List<PersonDetail> as a SQL parameter.
     * This method serializes the list to JSON string and calls
     * collectionDao.updatePersonsJson(), which stores it as text in the
     * 'persons' column — the same format used by the TypeConverter.
     */
    override suspend fun updatePersonPhotoUrls(id: String, persons: List<PersonDetail>): Result<Unit> {
        return try {
            val personsJson = json.encodeToString(persons)
            collectionDao.updatePersonsJson(id, personsJson)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update persons error: ${e.message}")
            Result.failure(e)
        }
    }

    // FIX #5: Atomic retry count increment via DAO
    override suspend fun incrementRetryCount(id: String): Result<Unit> {
        return try {
            collectionDao.incrementRetryCount(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Remote Operations ──────────────────────────────────────────────────

    override suspend fun uploadPhoto(byteArray: ByteArray, fileName: String): Result<String> {
        return try {
            val bucket = supabaseClient.storage.from("collection-photos")
            bucket.upload(fileName, byteArray) { upsert = true }
            val url = bucket.publicUrl(fileName)
            Result.success(url)
        } catch (e: Exception) {
            Log.e(TAG, "Upload photo error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * FIX #2: syncFromRemote() uses insertCollectionsAndRebuildFts().
     *
     * The original called insertCollections() directly — bypassing the FTS
     * rebuild. After every remote sync, FTS index was stale: searches missed
     * newly synced records and returned deleted ones.
     * insertCollectionsAndRebuildFts() wraps both in a Room @Transaction.
     */
    override suspend fun syncFromRemote(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting remote sync...")

            val localUnsyncedIds = collectionDao.getAllCollections().first()
                .filter { it.syncStatus != SyncStatus.SYNCED }
                .map { it.id }
                .toSet()

            var offset = 0
            var hasMore = true
            var totalSynced = 0

            while (hasMore) {
                val remoteRecords = supabaseClient.postgrest.from("collections")
                    .select {
                        order("timestamp", Order.DESCENDING)
                        range(offset.toLong(), (offset + SYNC_BATCH_SIZE - 1).toLong())
                    }
                    .decodeList<CollectionRecord>()

                if (remoteRecords.isEmpty()) {
                    hasMore = false
                } else {
                    val entities = remoteRecords
                        .filter { it.id !in localUnsyncedIds }
                        .map { it.copy(syncStatus = SyncStatus.SYNCED).toEntity() }

                    if (entities.isNotEmpty()) {
                        // FIX #2: Rebuild FTS after bulk insert.
                        collectionDao.insertCollectionsAndRebuildFts(entities)
                    }

                    totalSynced += remoteRecords.size
                    Log.d(TAG, "Synced batch $offset–${offset + remoteRecords.size}. Total: $totalSynced")

                    hasMore = remoteRecords.size >= SYNC_BATCH_SIZE
                    if (hasMore) offset += SYNC_BATCH_SIZE
                }
            }

            Log.d(TAG, "Remote sync complete. Total synced: $totalSynced")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Remote sync error: ${e.message}")
            Result.failure(e)
        }
    }

    // ── FTS Query Sanitizer ────────────────────────────────────────────────

    /**
     * Sanitizes a free-text query before passing to FTS MATCH.
     * Raw apostrophes, hyphens, and FTS operators crash the MATCH operator.
     */
    private fun sanitizeFtsQuery(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace("'", "")
            .replace("\"", "")
            .replace("-", " ")
            .replace("*", "")
            .replace("(", "")
            .replace(")", "")
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .let { if (it.isBlank()) "" else "$it*" }
    }
}