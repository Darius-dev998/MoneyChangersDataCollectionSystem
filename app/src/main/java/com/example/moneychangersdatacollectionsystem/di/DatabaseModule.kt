package com.example.moneychangersdatacollectionsystem.di

import android.content.Context
import androidx.room.Room
import com.example.moneychangersdatacollectionsystem.data.local.AppDatabase
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // For production, we would use a real passphrase from a secure source
        val passphrase = "mcdcs_secure_passphrase".toByteArray()
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    @Provides
    fun provideAgentDao(database: AppDatabase): AgentDao = database.agentDao()

    @Provides
    fun provideCollectionDao(database: AppDatabase): CollectionDao = database.collectionDao()
}
