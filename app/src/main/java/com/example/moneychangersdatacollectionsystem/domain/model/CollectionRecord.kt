package com.example.moneychangersdatacollectionsystem.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonDetail(
    val id: String = "", // PRS-XXXX
    val name: String = "",
    val contact: String = "",
    @SerialName("origin_county")
    val originCounty: String = "",
    @SerialName("money_remittance")
    val moneyRemittance: Boolean? = null,
    @SerialName("education_status")
    val educationStatus: String = "",
    @SerialName("emergency_contact_name")
    val emergencyContactName: String = "",
    @SerialName("emergency_contact_number")
    val emergencyContactNumber: String = "",
    @SerialName("emergency_contact_address")
    val emergencyContactAddress: String = "",
    @SerialName("has_bank_account")
    val hasBankAccount: Boolean? = null,
    @SerialName("owner_operator_status")
    val ownerOperatorStatus: String = "",
    @SerialName("marital_status")
    val maritalStatus: String = "",
    @SerialName("physical_address")
    val physicalAddress: String = "",
    @SerialName("nationality")
    val nationality: String = "",
    @SerialName("id_type")
    val idType: String = "",
    @SerialName("id_number")
    val idNumber: String = "",
    @SerialName("id_card_photo_url")
    val idCardPhotoUrl: String = ""
)

@Serializable
data class CollectionRecord(
    val id: String, // Category specific: FRX-XXXX, UMB-XXXX, etc.
    @SerialName("agent_id")
    val agentId: String = "",
    val county: String = "",
    val category: String = "",
    @SerialName("umbrella_type")
    val umbrellaType: String? = null,
    @SerialName("business_name")
    val businessName: String = "",
    @SerialName("person_count")
    val personCount: Int? = null,
    val gender: String? = null,
    @SerialName("id_type")
    val idType: String = "",
    @SerialName("id_number")
    val idNumber: String = "",
    @SerialName("contact_number")
    val contactNumber: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("photo_url")
    val photoUrl: String = "", 
    @SerialName("id_card_photo_url")
    val idCardPhotoUrl: String = "",
    @SerialName("persons")
    val persons: List<PersonDetail> = emptyList(),
    
    // Global fields for non-Forex categories
    @SerialName("origin_county")
    val originCounty: String = "",
    @SerialName("money_remittance")
    val moneyRemittance: Boolean? = null,
    @SerialName("education_status")
    val educationStatus: String = "",
    @SerialName("emergency_contact_name")
    val emergencyContactName: String = "",
    @SerialName("emergency_contact_number")
    val emergencyContactNumber: String = "",
    @SerialName("emergency_contact_address")
    val emergencyContactAddress: String = "",
    @SerialName("has_bank_account")
    val hasBankAccount: Boolean? = null,
    @SerialName("owner_operator_status")
    val ownerOperatorStatus: String = "",
    @SerialName("marital_status")
    val maritalStatus: String = "",
    @SerialName("physical_address")
    val physicalAddress: String = "",
    
    // Profile for non-Forex
    @SerialName("person_name")
    val personName: String = "",
    @SerialName("nationality")
    val nationality: String = "",

    @SerialName("sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

val CollectionRecord.shortId: String
    get() = id 

val CollectionRecord.shortAgentId: String
    get() = if (agentId.length > 8) "AGT-${agentId.take(8).uppercase()}" else agentId

@Serializable
enum class BusinessCategory(val displayName: String, val code: String) {
    FOREX_BUREAU("Forex Bureau", "FRX"),
    STORE_SHOP("Store/Shop", "STR"),
    UMBRELLA("Umbrella", "UMB"),
    WITHOUT_UMBRELLA("Without Umbrella", "WOU"),
    KIOSK_BOOTH("Kiosk/Booth", "KSK")
}
