package com.example.moneychangersdatacollectionsystem.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.moneychangersdatacollectionsystem.domain.model.CollectionRecord
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus

@Entity(
    tableName = "collections",
    indices = [
        Index(value = ["id"]),
        Index(value = ["businessName"]),
        Index(value = ["county"]),
        Index(value = ["category"]),
        Index(value = ["agentId"]),
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
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String, 
    val idCardPhotoUrl: String, 
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
    val timestamp: Long
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
