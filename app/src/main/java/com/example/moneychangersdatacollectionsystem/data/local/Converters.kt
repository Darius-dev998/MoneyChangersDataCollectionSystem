package com.example.moneychangersdatacollectionsystem.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

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
}
