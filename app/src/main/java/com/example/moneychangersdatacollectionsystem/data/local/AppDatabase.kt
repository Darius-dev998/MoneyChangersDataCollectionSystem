package com.example.moneychangersdatacollectionsystem.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.moneychangersdatacollectionsystem.data.local.dao.AgentDao
import com.example.moneychangersdatacollectionsystem.data.local.dao.CollectionDao
import com.example.moneychangersdatacollectionsystem.data.local.entity.AgentEntity
import com.example.moneychangersdatacollectionsystem.data.local.entity.CollectionEntity
import com.example.moneychangersdatacollectionsystem.data.local.entity.CollectionFtsEntity

@Database(
    entities = [AgentEntity::class, CollectionEntity::class, CollectionFtsEntity::class],
    version = 5, // Incremented to version 5 for FTS4 support
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        const val DATABASE_NAME = "mcdcs_db"
    }
}
