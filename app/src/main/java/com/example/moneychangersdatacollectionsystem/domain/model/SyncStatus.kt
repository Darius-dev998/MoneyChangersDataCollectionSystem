package com.example.moneychangersdatacollectionsystem.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * SyncStatus — State machine for offline-first record synchronization.
 *
 * FIX: @SerialName added to each value.
 *
 * Without @SerialName, Kotlinx Serialization uses the Kotlin enum constant
 * name as the JSON string (e.g., "PENDING"). If the Supabase column stores
 * values in a different case or format, deserialization silently fails or
 * returns wrong values. Explicit @SerialName decouples the Kotlin constant
 * name from the stored string, making the contract explicit and safe to refactor.
 *
 * State transitions:
 *   New record created  → PENDING
 *   SyncWorker picks up → SYNCING
 *   Upload succeeds     → SYNCED
 *   Upload fails        → PENDING (reverts, increments syncRetryCount)
 *   Max retries exceeded→ FAILED  (manual intervention required)
 *   App restart         → SYNCING → PENDING (self-healing in MCDCSApplication)
 */
@Serializable
enum class SyncStatus {
    @SerialName("PENDING")
    PENDING,

    @SerialName("SYNCING")
    SYNCING,

    @SerialName("SYNCED")
    SYNCED,

    @SerialName("FAILED")
    FAILED
}