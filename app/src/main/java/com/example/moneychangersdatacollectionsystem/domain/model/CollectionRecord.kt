package com.example.moneychangersdatacollectionsystem.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PersonDetail — Individual person within a Forex Bureau registration.
 *
 * FIX: idCardPhotoUrl changed from String = "" to String? = null.
 * Empty string caused Coil to attempt loading an empty URL (failed silently)
 * and SyncWorker to attempt uploading/checking an empty path string.
 * Null correctly represents "photo not yet captured or uploaded."
 */
@Serializable
data class PersonDetail(
    val id: String = "",
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
    // FIX: Was String = "". Null means "photo not yet captured/uploaded."
    @SerialName("id_card_photo_url")
    val idCardPhotoUrl: String? = null
)

/**
 * CollectionRecord — Domain model for a money changer registration record.
 *
 * FIXES APPLIED:
 *  #1  latitude/longitude changed from Double = 0.0 to Double? = null.
 *      (0.0, 0.0) is a real GPS coordinate in the Gulf of Guinea — approx.
 *      600km off Liberia's coast in the Atlantic Ocean. Every record saved
 *      without a GPS fix was being mapped to the ocean. Null = "not acquired."
 *
 *  #2  photoUrl/idCardPhotoUrl changed from String = "" to String? = null.
 *      Before Supabase Storage upload, the URL is unknown. Empty string caused
 *      Coil and SyncWorker to attempt requests with an empty path. Null is
 *      the correct sentinel for "photo captured but not yet uploaded."
 */
@Serializable
data class CollectionRecord(
    val id: String,
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

    // FIX #1: Nullable. Null = GPS was not acquired for this record.
    val latitude: Double? = null,
    val longitude: Double? = null,

    // FIX #2: Nullable. Null = photo captured on device, not yet uploaded.
    @SerialName("photo_url")
    val photoUrl: String? = null,
    @SerialName("id_card_photo_url")
    val idCardPhotoUrl: String? = null,

    @SerialName("persons")
    val persons: List<PersonDetail> = emptyList(),

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
    @SerialName("person_name")
    val personName: String = "",
    @SerialName("nationality")
    val nationality: String = "",
    @SerialName("sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    @SerialName("timestamp")
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