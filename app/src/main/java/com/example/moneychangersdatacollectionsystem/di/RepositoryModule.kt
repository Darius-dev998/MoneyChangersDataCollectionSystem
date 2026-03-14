package com.example.moneychangersdatacollectionsystem.di

import com.example.moneychangersdatacollectionsystem.data.repository.AuthRepositoryImpl
import com.example.moneychangersdatacollectionsystem.data.repository.CollectionRepositoryImpl
import com.example.moneychangersdatacollectionsystem.domain.repository.AuthRepository
import com.example.moneychangersdatacollectionsystem.domain.repository.CollectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(
        collectionRepositoryImpl: CollectionRepositoryImpl
    ): CollectionRepository
}
