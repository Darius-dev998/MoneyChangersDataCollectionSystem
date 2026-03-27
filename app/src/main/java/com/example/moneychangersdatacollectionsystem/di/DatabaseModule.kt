package com.example.moneychangersdatacollectionsystem.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneychangersdatacollectionsystem.data.local.AppDatabase
import com.example.moneychangersdatacollectionsystem.data.local.AppDatabase.Companion.MIGRATION_5_6
import com.example.moneychangersdatacollectionsystem.data.local.DatabasePassphraseManager
import com.example.moneychangersdatacollectionsystem.data.local.dao.AgentDao
import com.example.moneychangersdatacollectionsystem.data.local.dao.CollectionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val TAG = "DatabaseModule"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphraseManager: DatabasePassphraseManager
    ): AppDatabase {
        val passphrase = passphraseManager.getPassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(MIGRATION_5_6)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.i(TAG, "Database created: ${AppDatabase.DATABASE_NAME}")
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    
                    // Use explicit arrayOf<Any?>() to resolve ambiguity in SQLCipher/Room
                    try {
                        db.query("PRAGMA synchronous = FULL", arrayOf<Any?>()).close()
                        db.query("PRAGMA wal_autocheckpoint = 1000", arrayOf<Any?>()).close()
                        db.query("PRAGMA cache_size = -8000", arrayOf<Any?>()).close()
                        db.query("PRAGMA foreign_keys = ON", arrayOf<Any?>()).close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting PRAGMAs: ${e.message}")
                    }
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideAgentDao(database: AppDatabase): AgentDao = database.agentDao()

    @Provides
    @Singleton
    fun provideCollectionDao(database: AppDatabase): CollectionDao = database.collectionDao()
}
