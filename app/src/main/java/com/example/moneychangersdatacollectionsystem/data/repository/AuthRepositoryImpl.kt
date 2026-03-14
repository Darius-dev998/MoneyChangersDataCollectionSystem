package com.example.moneychangersdatacollectionsystem.data.repository

import android.util.Log
import com.example.moneychangersdatacollectionsystem.data.local.dao.AgentDao
import com.example.moneychangersdatacollectionsystem.data.local.entity.toDomain
import com.example.moneychangersdatacollectionsystem.data.local.entity.toEntity
import com.example.moneychangersdatacollectionsystem.domain.model.Agent
import com.example.moneychangersdatacollectionsystem.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val agentDao: AgentDao
) : AuthRepository {

    private val auth = supabaseClient.auth
    private val postgrest = supabaseClient.postgrest

    override val currentUser: Flow<Agent?> = agentDao.getLoggedInAgent().map { it?.toDomain() }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = auth.currentUserOrNull() ?: throw Exception("Login failed: User not found in Auth")

            // FETCH BY EMAIL: This fixes the AGT-001 vs UUID issue
            val agentResponse = try {
                postgrest.from("agents")
                    .select {
                        filter {
                            eq("email", email)
                        }
                    }
                    .decodeSingle<Agent>()
            } catch (e: Exception) {
                Log.e("AuthRepo", "Agent profile not found for email $email. Error: ${e.message}")
                // Fallback: Use the Auth user info
                Agent(
                    id = user.id,
                    email = email,
                    fullName = "Field Officer",
                    badgeNumber = "Pending",
                    role = "OFFICER"
                )
            }

            // Save to local database
            agentDao.insertAgent(agentResponse.toEntity())

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Login failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            agentDao.clearAgents()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSession(): String? {
        return auth.currentAccessTokenOrNull()
    }
}
