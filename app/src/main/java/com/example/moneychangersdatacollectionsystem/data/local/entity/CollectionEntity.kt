package com.example.moneychangersdatacollectionsystem.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.moneychangersdatacollectionsystem.domain.model.CollectionRecord
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus

/**
 * CollectionEntity — Room entity for money changer registration records.
 *
 * FIXES APPLIED:
 *  #1  latitude and longitude changed to Double? (nullable).
 *      Non-nullable with no default means GPS must always succeed before
 *      a record can be saved — no graceful handling of GPS failure or
 *      offline indoor capture. Null = "GPS not yet acquired."
 *
 *  #2  photoUrl and idCardPhotoUrl changed to String? (nullable).
 *      Photos are captured on-device and uploaded to Supabase Storage later
 *      by the SyncWorker. Before upload, the URL is unknown. Using empty
 *      string "" as a sentinel caused Coil to attempt loading an empty URL
 *      and Supabase to attempt invalid storage requests. Null = "not yet uploaded."
 *
 *  #3  Index(value = ["id"]) REMOVED — redundant.
 *      @PrimaryKey already creates an implicit index on the primary key column.
 *      Declaring it again wastes database space and confuses schema readers.
 */
@Entity(
    tableName = "collections",
    indices = [
        // FIX #3: Index on "id" removed — @PrimaryKey already indexes it.
        Index(value = ["businessName"]),
        Index(value = ["county"]),
        Index(value = ["category"]),
        Index(value = ["agentId"]),
        Index(value = ["syncStatus"]),          // Added — SyncWorker queries by status
        Index(value = ["timestamp"]),            // Added — ordered queries need this
        Index(value = ["maritalStatus"]),
        Index(value = ["educationStatus"]),
        Index(value = ["originCounty"]),
        Index(value = ["gender"]),
        Index(value = ["idType"]),
        Index(value = ["ownerOperatorStatus"]),
        Index(value = ["hasBankAccount"]),
        Index(value = ["moneyRemittance"])
    ]
)
data class CollectionEntity(
    @PrimaryKey val id: String,
    val agentId: String,
    val county: String,
    val category: String,
    val umbrellaType: String? = null,
    val businessName: String,
    val personCount: Int? = null,
    val gender: String? = null,
    val idType: String,
    val idNumber: String,
    val contactNumber: String,

    // FIX #1: Nullable — GPS may not be available at capture time.
    // Null means "GPS was not acquired for this record."
    val latitude: Double? = null,
    val longitude: Double? = null,

    // FIX #2: Nullable — photo not yet uploaded to Supabase Storage.
    // Null means "photo captured on device but remote URL not yet assigned."
    // SyncWorker sets this to the Supabase Storage URL after upload.
    val photoUrl: String? = null,
    val idCardPhotoUrl: String? = null,

    val persons: List<PersonDetail>,

    // Profile Fields
    val originCounty: String,
    val moneyRemittance: Boolean?,
    val educationStatus: String,
    val emergencyContactName: String,
    val emergencyContactNumber: String,
    val emergencyContactAddress: String,
    val hasBankAccount: Boolean?,
    val ownerOperatorStatus: String,
    val maritalStatus: String,
    val physicalAddress: String,

    // Profile for non-Forex
    val personName: String,
    val nationality: String,

    val syncStatus: SyncStatus,
    val timestamp: Long,

    // Retry tracking for sync failures
    val syncRetryCount: Int = 0
)

fun CollectionEntity.toDomain() = CollectionRecord(
    id = id,
    agentId = agentId,
    county = county,
    category = category,
    umbrellaType = umbrellaType,
    businessName = businessName,
    personCount = personCount,
    gender = gender,
    idType = idType,
    idNumber = idNumber,
    contactNumber = contactNumber,
    latitude = latitude,
    longitude = longitude,
    photoUrl = photoUrl,
    idCardPhotoUrl = idCardPhotoUrl,
    persons = persons,
    originCounty = originCounty,
    moneyRemittance = moneyRemittance,
    educationStatus = educationStatus,
    emergencyContactName = emergencyContactName,
    emergencyContactNumber = emergencyContactNumber,
    emergencyContactAddress = emergencyContactAddress,
    hasBankAccount = hasBankAccount,
    ownerOperatorStatus = ownerOperatorStatus,
    maritalStatus = maritalStatus,
    physicalAddress = physicalAddress,
    personName = personName,
    nationality = nationality,
    syncStatus = syncStatus,
    timestamp = timestamp
)

fun CollectionRecord.toEntity() = CollectionEntity(
    id = id,
    agentId = agentId,
    county = county,
    category = category,
    umbrellaType = umbrellaType,
    businessName = businessName,
    personCount = personCount,
    gender = gender,
    idType = idType,
    idNumber = idNumber,
    contactNumber = contactNumber,
    latitude = latitude,
    longitude = longitude,
    photoUrl = photoUrl,
    idCardPhotoUrl = idCardPhotoUrl,
    persons = persons,
    originCounty = originCounty,
    moneyRemittance = moneyRemittance,
    educationStatus = educationStatus,
    emergencyContactName = emergencyContactName,
    emergencyContactNumber = emergencyContactNumber,
    emergencyContactAddress = emergencyContactAddress,
    hasBankAccount = hasBankAccount,
    ownerOperatorStatus = ownerOperatorStatus,
    maritalStatus = maritalStatus,
    physicalAddress = physicalAddress,
    personName = personName,
    nationality = nationality,
    syncStatus = syncStatus,
    timestamp = timestamp
)