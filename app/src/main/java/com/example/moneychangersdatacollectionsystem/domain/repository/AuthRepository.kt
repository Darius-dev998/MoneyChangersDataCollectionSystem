package com.example.moneychangersdatacollectionsystem.domain.repository

import com.example.moneychangersdatacollectionsystem.domain.model.Agent
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<Agent?>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun getSession(): String?
}
