package com.example.moneychangersdatacollectionsystem.domain.repository

import androidx.paging.PagingData
import com.example.moneychangersdatacollectionsystem.domain.model.CollectionRecord
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * CollectionRepository — Domain interface for collection record operations.
 *
 * FIXES APPLIED:
 *  #1  updatePhotoUrls parameters changed from String to String?.
 *      CollectionEntity.photoUrl and idCardPhotoUrl are now String? (nullable).
 *      The interface must match so implementations can pass null values when
 *      a photo has not yet been uploaded or the URL is unknown.
 *
 *  #2  updatePersonPhotoUrls — signature unchanged (List<PersonDetail>).
 *      The domain interface accepts the domain model list. The implementation
 *      is responsible for serializing to JSON before calling the DAO.
 *      This keeps the interface clean while handling the Room constraint
 *      (cannot bind List<PersonDetail> as a @Query parameter) internally.
 *
 *  #3  getPendingCollections() documented as retry-budget-aware.
 *      Implementations must use getPendingWithRetryBudget() in the DAO —
 *      not getCollectionsBySyncStatus(PENDING) — to avoid infinite retry loops
 *      on permanently broken records.
 *
 *  #4  searchCollectionsFts() added — FTS-based full-text search.
 *      Delegates to the FTS MATCH query in the DAO for efficient text search.
 *      The existing searchCollectionsPaged() is kept for structured filter-only
 *      queries where no free-text input is entered.
 */
interface CollectionRepository {

    fun getCollections(): Flow<List<CollectionRecord>>

    fun getCollectionsPaged(): Flow<PagingData<CollectionRecord>>

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
    ): Flow<PagingData<CollectionRecord>>

    // FIX #4: FTS-based search for free-text queries
    fun searchCollectionsFts(
        query: String,
        county: String? = null,
        category: String? = null,
        agentId: String? = null,
        gender: String? = null,
        idType: String? = null,
        marital: String? = null,
        edu: String? = null,
        origin: String? = null,
        role: String? = null,
        hasBank: Boolean? = null,
        remittance: Boolean? = null
    ): Flow<PagingData<CollectionRecord>>

    // FIX #3: Implementation must use retry budget (getPendingWithRetryBudget in DAO)
    suspend fun getPendingCollections(): List<CollectionRecord>

    suspend fun saveCollection(record: CollectionRecord): Result<Unit>

    suspend fun updateSyncStatus(id: String, status: SyncStatus): Result<Unit>

    suspend fun resetSyncStatus(oldStatus: SyncStatus, newStatus: SyncStatus): Result<Unit>

    // FIX #1: String? parameters — photos may not be available at time of call
    suspend fun updatePhotoUrls(id: String, photoUrl: String?, idCardUrl: String?): Result<Unit>

    // FIX #2: Interface keeps domain model list — impl serializes internally
    suspend fun updatePersonPhotoUrls(id: String, persons: List<PersonDetail>): Result<Unit>

    suspend fun incrementRetryCount(id: String): Result<Unit>

    suspend fun uploadPhoto(byteArray: ByteArray, fileName: String): Result<String>

    suspend fun syncFromRemote(): Result<Unit>
}