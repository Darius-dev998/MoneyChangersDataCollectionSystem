package com.example.moneychangersdatacollectionsystem.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.moneychangersdatacollectionsystem.data.local.entity.CollectionEntity
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * CollectionDao — Data access for money changer collection records.
 *
 * FIXES APPLIED:
 *  #1  updatePersons() REMOVED. [Compile Error]
 *      Room @Query cannot bind List<PersonDetail> as a SQL parameter.
 *      TypeConverters apply to column storage, not to query bind parameters.
 *      To update persons: fetch the entity, copy with new persons value,
 *      and call insertCollection() (REPLACE conflict strategy handles upsert).
 *      A targeted updatePersonsJson() using the pre-serialized String is
 *      provided instead for cases where the Repository has already serialized.
 *
 *  #2  FTS-based searchCollectionsFts() added.
 *      The original searchCollectionsPaged() used LIKE '%..%' queries despite
 *      a CollectionFtsEntity existing — defeating the entire purpose of FTS.
 *      FTS MATCH is orders of magnitude faster than LIKE on large datasets.
 *      The LIKE-based method is kept for structured filter-only searches
 *      (county, category, etc.) where no text query is entered.
 *
 *  #3  rebuildFts() added.
 *      When insertCollection() fires with OnConflictStrategy.REPLACE on an
 *      existing record, SQLite deletes the old row and inserts a new one with
 *      a new rowid. The FTS4 content table retains the old rowid → search
 *      returns stale/ghost results. Call rebuildFts() after bulk operations.
 *
 *  #4  Atomic status operations added.
 *      updateSyncStatusAtomic() and incrementRetryCount() perform single-
 *      statement SQL updates — no fetch-copy-write race condition.
 *
 *  #5  updatePhotoUrls() kept but signature fixed to reflect nullable URLs.
 *      CollectionEntity.photoUrl is now String? — parameters updated accordingly.
 */
@Dao
interface CollectionDao {

    // ── Writes ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<CollectionEntity>)

    // ── FTS Rebuild ──────────────────────────────────────────────────────────

    /**
     * FIX #3: Rebuild FTS index after bulk operations.
     *
     * REPLACE conflict strategy causes DELETE + INSERT with a new rowid.
     * The FTS4 content table links rows via rowid — stale rowids produce
     * ghost search results. Call this after insertCollections() for bulk syncs.
     */
    @Query("INSERT INTO collections_fts(collections_fts) VALUES('rebuild')")
    suspend fun rebuildFts()

    /**
     * Transactional bulk insert that automatically rebuilds FTS afterward.
     * Use this for SyncWorker remote data ingestion.
     */
    @Transaction
    suspend fun insertCollectionsAndRebuildFts(collections: List<CollectionEntity>) {
        insertCollections(collections)
        rebuildFts()
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM collections ORDER BY timestamp DESC, id DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY timestamp DESC, id DESC")
    fun getAllCollectionsPaged(): PagingSource<Int, CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: String): CollectionEntity?

    // ── Sync Queries ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM collections WHERE syncStatus = :status")
    suspend fun getCollectionsBySyncStatus(status: SyncStatus): List<CollectionEntity>

    /**
     * FIX #4: Atomic status update — single SQL statement, no race condition.
     */
    @Query("UPDATE collections SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE collections SET syncStatus = :newStatus WHERE syncStatus = :oldStatus")
    suspend fun resetSyncStatus(oldStatus: SyncStatus, newStatus: SyncStatus)

    /**
     * FIX #4: Atomic retry count increment.
     * Uses SQL arithmetic — no read-modify-write race condition.
     */
    @Query("UPDATE collections SET syncRetryCount = syncRetryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: String)

    /**
     * Returns records that haven't exceeded the max retry budget.
     * Prevents SyncWorker from looping forever on permanently broken records.
     */
    @Query("SELECT * FROM collections WHERE syncStatus = 'PENDING' AND syncRetryCount < :maxRetries")
    suspend fun getPendingWithRetryBudget(maxRetries: Int = 5): List<CollectionEntity>

    // ── Photo URL Update ─────────────────────────────────────────────────────

    /**
     * FIX #5: Parameters updated to String? to match nullable entity fields.
     * Called by SyncWorker after successful photo upload to Supabase Storage.
     */
    @Query("UPDATE collections SET photoUrl = :photoUrl, idCardPhotoUrl = :idCardUrl WHERE id = :id")
    suspend fun updatePhotoUrls(id: String, photoUrl: String?, idCardUrl: String?)

    // ── FIX #1: Persons update via pre-serialized JSON string ───────────────
    /**
     * FIX #1: Replaces the removed updatePersons(id, List<PersonDetail>).
     *
     * Room cannot bind List<PersonDetail> as a @Query parameter.
     * The Repository layer must serialize the list to JSON first
     * (using the same Json instance as Converters) and pass the string here.
     *
     * Example in Repository:
     *   val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
     *   val personsJson = json.encodeToString(persons)
     *   collectionDao.updatePersonsJson(id, personsJson)
     */
    @Query("UPDATE collections SET persons = :personsJson WHERE id = :id")
    suspend fun updatePersonsJson(id: String, personsJson: String)

    // ── Delete ───────────────────────────────────────────────────────────────

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Query("DELETE FROM collections WHERE syncStatus = :status")
    suspend fun deleteCollectionsBySyncStatus(status: SyncStatus)

    // ── FTS Search ───────────────────────────────────────────────────────────

    /**
     * FIX #2: FTS-based full-text search.
     *
     * Uses FTS MATCH for the text query — orders of magnitude faster than
     * LIKE '%..%' on large datasets. Combined with structured field filters.
     *
     * IMPORTANT: The :query parameter MUST be sanitized before calling.
     * Raw apostrophes, hyphens, and FTS operators crash MATCH. Sanitize:
     *   fun sanitizeFts(raw: String) = raw
     *       .replace("'", "").replace("-", " ").replace("\"", "")
     *       .trim().let { if (it.isBlank()) "" else "$it*" }
     *
     * Pass query = "" to skip FTS filtering (structured filters only).
     */
    @Query("""
        SELECT c.* FROM collections c
        JOIN collections_fts fts ON c.rowid = fts.rowid
        WHERE (:query = '' OR collections_fts MATCH :query)
        AND (:county IS NULL OR c.county = :county)
        AND (:category IS NULL OR c.category = :category)
        AND (:agentId IS NULL OR c.agentId = :agentId)
        AND (:gender IS NULL OR c.gender = :gender)
        AND (:idType IS NULL OR c.idType = :idType)
        AND (:marital IS NULL OR c.maritalStatus = :marital)
        AND (:edu IS NULL OR c.educationStatus = :edu)
        AND (:origin IS NULL OR c.originCounty = :origin)
        AND (:role IS NULL OR c.ownerOperatorStatus = :role)
        AND (:hasBank IS NULL OR c.hasBankAccount = :hasBank)
        AND (:remittance IS NULL OR c.moneyRemittance = :remittance)
        ORDER BY c.timestamp DESC, c.id DESC
    """)
    fun searchCollectionsFts(
        query: String,
        county: String?,
        category: String?,
        agentId: String?,
        gender: String?,
        idType: String?,
        marital: String?,
        edu: String?,
        origin: String?,
        role: String?,
        hasBank: Boolean?,
        remittance: Boolean?
    ): PagingSource<Int, CollectionEntity>

    /**
     * Legacy LIKE-based search — kept for structured filter-only queries
     * where no free-text input is entered. Less efficient than FTS for text
     * but handles partial businessName/address/nationality matching.
     */
    @Query("""
        SELECT * FROM collections 
        WHERE (:id IS NULL OR id LIKE '%' || :id || '%')
        AND (:name IS NULL OR businessName LIKE '%' || :name || '%')
        AND (:county IS NULL OR county = :county)
        AND (:category IS NULL OR category = :category)
        AND (:agentId IS NULL OR agentId LIKE '%' || :agentId || '%')
        AND (:marital IS NULL OR maritalStatus = :marital)
        AND (:edu IS NULL OR educationStatus = :edu)
        AND (:origin IS NULL OR originCounty = :origin)
        AND (:gender IS NULL OR gender = :gender)
        AND (:idType IS NULL OR idType = :idType)
        AND (:role IS NULL OR ownerOperatorStatus = :role)
        AND (:hasBank IS NULL OR hasBankAccount = :hasBank)
        AND (:remittance IS NULL OR moneyRemittance = :remittance)
        AND (:address IS NULL OR physicalAddress LIKE '%' || :address || '%')
        AND (:nationality IS NULL OR nationality LIKE '%' || :nationality || '%')
        ORDER BY timestamp DESC, id DESC
    """)
    fun searchCollectionsPaged(
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
    ): PagingSource<Int, CollectionEntity>
}