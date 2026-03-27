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

/**
 * AuthRepositoryImpl — Authentication and agent profile management.
 *
 * FIX: Minor cleanup — removed unused `user` variable.
 * The result of `auth.currentUserOrNull()` was assigned to `user` but only
 * used for the null check. Simplified to a direct null-check expression.
 *
 * The ghost-officer prevention pattern (fetch by email, sign out on failure)
 * is correct and retained unchanged.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val agentDao: AgentDao
) : AuthRepository {

    override val currentUser: Flow<Agent?> = agentDao.getLoggedInAgent().map { it?.toDomain() }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // Verify the session was established
            supabaseClient.auth.currentUserOrNull()
                ?: throw Exception("Login failed: session not established after sign-in")

            // Fetch agent profile from our agents table (NOT Supabase auth.users).
            // This ensures only provisioned agents can log in — "ghost officer" prevention.
            val agentProfile = try {
                supabaseClient.postgrest.from("agents")
                    .select { filter { eq("email", email) } }
                    .decodeSingle<Agent>()
            } catch (e: Exception) {
                Log.e("AuthRepo", "Agent profile not found for $email: ${e.message}")
                // Sign out immediately — don't leave a Supabase session for an unregistered user
                supabaseClient.auth.signOut()
                throw Exception(
                    "Unauthorized: Your agent profile was not found. " +
                            "Please contact your administrator."
                )
            }

            agentDao.insertAgent(agentProfile.toEntity())
            Log.d("AuthRepo", "Login successful: ${agentProfile.badgeNumber}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AuthRepo", "Login failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            agentDao.clearAgents()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Logout error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getSession(): String? {
        return supabaseClient.auth.currentAccessTokenOrNull()
    }
}