package com.example.moneychangersdatacollectionsystem.domain.repository

import androidx.paging.PagingData
import com.example.moneychangersdatacollectionsystem.domain.model.CollectionRecord
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

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

    suspend fun getPendingCollections(): List<CollectionRecord>
    suspend fun saveCollection(record: CollectionRecord): Result<Unit>
    suspend fun updateSyncStatus(id: String, status: SyncStatus): Result<Unit>
    suspend fun resetSyncStatus(oldStatus: SyncStatus, newStatus: SyncStatus): Result<Unit>
    suspend fun updatePhotoUrls(id: String, photoUrl: String, idCardUrl: String): Result<Unit>
    suspend fun updatePersonPhotoUrls(id: String, persons: List<PersonDetail>): Result<Unit>
    suspend fun uploadPhoto(byteArray: ByteArray, fileName: String): Result<String>
    suspend fun syncFromRemote(): Result<Unit>
}
