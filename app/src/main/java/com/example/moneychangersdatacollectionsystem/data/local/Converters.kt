package com.example.moneychangersdatacollectionsystem.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import com.example.moneychangersdatacollectionsystem.domain.model.SyncStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Converters — Room TypeConverters for complex types.
 *
 * FIXES APPLIED:
 *  #1  SyncStatus TypeConverter ADDED. [CRITICAL — compile error without it]
 *      CollectionEntity has a field `syncStatus: SyncStatus` which is an enum.
 *      Room cannot store/retrieve enum types without an explicit TypeConverter.
 *      Without this, the Room annotation processor fails to compile the DAO
 *      with: "Cannot figure out how to save this field into database."
 *
 *      The converter stores each SyncStatus as its String name:
 *        SyncStatus.PENDING  → "PENDING"
 *        SyncStatus.SYNCING  → "SYNCING"
 *        SyncStatus.SYNCED   → "SYNCED"
 *        SyncStatus.FAILED   → "FAILED"
 *
 *      The toSyncStatus() deserializer handles unrecognized values gracefully
 *      by defaulting to PENDING rather than throwing an exception. This protects
 *      against schema drift if a new status value is added in a future version.
 */
class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ── String List ──────────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return try {
            json.encodeToString(value)
        } catch (e: Exception) {
            "[]"
        }
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            Log.e("Converters", "Error decoding string list: ${e.message}")
            emptyList()
        }
    }

    // ── PersonDetail List ────────────────────────────────────────────────────

    @TypeConverter
    fun fromPersonDetailList(value: List<PersonDetail>): String {
        return try {
            json.encodeToString(value)
        } catch (e: Exception) {
            "[]"
        }
    }

    @TypeConverter
    fun toPersonDetailList(value: String): List<PersonDetail> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            Log.e("Converters", "Error decoding person list: ${e.message}")
            emptyList()
        }
    }

    // ── SyncStatus Enum ──────────────────────────────────────────────────────
    // FIX #1: Added — required for CollectionEntity.syncStatus to compile.

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return try {
            SyncStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // Unknown status value (e.g., from a future schema version).
            // Default to PENDING so the record is re-evaluated by SyncWorker
            // rather than silently dropped or causing a crash.
            Log.w("Converters", "Unknown SyncStatus value '$value', defaulting to PENDING")
            SyncStatus.PENDING
        }
    }
}