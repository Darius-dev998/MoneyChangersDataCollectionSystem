package com.example.moneychangersdatacollectionsystem.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moneychangersdatacollectionsystem.data.local.entity.CollectionEntity
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<CollectionEntity>)

    @Query("SELECT * FROM collections ORDER BY timestamp DESC, id DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY timestamp DESC, id DESC")
    fun getAllCollectionsPaged(): PagingSource<Int, CollectionEntity>

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

    @Query("SELECT * FROM collections WHERE syncStatus = :status")
    suspend fun getCollectionsBySyncStatus(status: SyncStatus): List<CollectionEntity>

    @Query("UPDATE collections SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE collections SET photoUrl = :photoUrl, idCardPhotoUrl = :idCardUrl WHERE id = :id")
    suspend fun updatePhotoUrls(id: String, photoUrl: String, idCardUrl: String)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)

    @Query("DELETE FROM collections WHERE syncStatus = :status")
    suspend fun deleteCollectionsBySyncStatus(status: SyncStatus)
}
