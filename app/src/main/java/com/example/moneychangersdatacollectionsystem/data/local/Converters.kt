package com.example.moneychangersdatacollectionsystem.data.local

import androidx.room.TypeConverter
import com.example.moneychangersdatacollectionsystem.domain.model.PersonDetail
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromPersonDetailList(value: List<PersonDetail>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toPersonDetailList(value: String): List<PersonDetail> {
        return Json.decodeFromString(value)
    }
}
