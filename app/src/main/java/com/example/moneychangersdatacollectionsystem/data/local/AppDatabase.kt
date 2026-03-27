package com.example.moneychangersdatacollectionsystem.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.moneychangersdatacollectionsystem.data.local.dao.AgentDao
import com.example.moneychangersdatacollectionsystem.data.local.dao.CollectionDao
import com.example.moneychangersdatacollectionsystem.data.local.entity.AgentEntity
import com.example.moneychangersdatacollectionsystem.data.local.entity.CollectionEntity
import com.example.moneychangersdatacollectionsystem.data.local.entity.CollectionFtsEntity

/**
 * AppDatabase — Room Database Definition.
 *
 * FIXES APPLIED:
 *  #1  exportSchema changed to TRUE.
 *      At version 5 with no exported schemas, there is no history of what
 *      changed between versions 1–5. Future migrations are impossible to
 *      write correctly without the schema history.
 *      ACTION REQUIRED: Add to app/build.gradle.kts ksp{} block:
 *          arg("room.schemaLocation", "$projectDir/schemas")
 *      (This is already added in the fixed build.gradle.kts.)
 *      Commit the generated JSON files to version control.
 *
 *  #2  Version bumped 5 → 6 for schema changes in CollectionEntity:
 *        — latitude/longitude changed from Double to Double? (nullable)
 *        — photoUrl/idCardPhotoUrl changed from String to String? (nullable)
 *        — syncRetryCount INT DEFAULT 0 column added
 *      ACTION REQUIRED: Create a Migration(5, 6) object. Template below.
 *      NEVER use fallbackToDestructiveMigration in production — it silently
 *      wipes all data on every version bump.
 *
 *  #3  Both Converters and the SyncStatus enum converter are in the same
 *      Converters class — no separate registration needed. Verified.
 *
 * VERSION HISTORY:
 *  v1-5 : Pre-history (no exported schemas — lesson learned)
 *  v6   : latitude/longitude nullable, photoUrl/idCardPhotoUrl nullable,
 *         syncRetryCount added to CollectionEntity
 */
@Database(
    entities = [
        AgentEntity::class,
        CollectionEntity::class,
        CollectionFtsEntity::class
    ],
    version = 6,
    // FIX #1: exportSchema = true. Requires room.schemaLocation KSP arg.
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun agentDao(): AgentDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        const val DATABASE_NAME = "mcdcs_db"

        /**
         * FIX #2: Migration 5 → 6.
         *
         * SQLite does not support changing column nullability — the existing
         * data is already compatible (NULL can be stored in any column
         * regardless of Kotlin nullability annotation). The schema change is
         * a Kotlin/Room type annotation change only.
         *
         * The only structural SQL change is adding syncRetryCount.
         *
         * REGISTER IN DatabaseModule:
         *   .addMigrations(MIGRATION_5_6)
         */
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add syncRetryCount column with default 0 for all existing records
                db.execSQL("ALTER TABLE collections ADD COLUMN syncRetryCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Template for next migration:
        // val MIGRATION_6_7 = object : Migration(6, 7) {
        //     override fun migrate(db: SupportSQLiteDatabase) { ... }
        // }
    }
}