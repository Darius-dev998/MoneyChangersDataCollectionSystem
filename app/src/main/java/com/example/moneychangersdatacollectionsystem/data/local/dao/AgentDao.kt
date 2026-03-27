package com.example.moneychangersdatacollectionsystem.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.moneychangersdatacollectionsystem.data.local.entity.AgentEntity
import kotlinx.coroutines.flow.Flow

/**
 * AgentDao — Data access for authenticated agent records.
 *
 * FIXES APPLIED:
 *  #1  getLoggedInAgent() ORDER BY added.
 *      Original used LIMIT 1 with no ordering — non-deterministic which
 *      agent is returned if multiple rows exist (data corruption scenario).
 *      ORDER BY id ensures consistent, reproducible results.
 *
 *  #2  updateAgent() added.
 *      Without an update method, the only way to change agent data is
 *      insertAgent() with OnConflictStrategy.REPLACE which does a DELETE
 *      + INSERT, potentially invalidating foreign key references and
 *      re-triggering any active Flow collectors.
 *
 *  #3  getAgentCount() added.
 *      Useful for determining whether any agent is logged in without
 *      collecting a Flow — avoids coroutine/Flow overhead for simple
 *      boolean checks.
 */
@Dao
interface AgentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)

    // FIX #2: Update without DELETE + re-INSERT
    @Update
    suspend fun updateAgent(agent: AgentEntity)

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: String): AgentEntity?

    // FIX #1: ORDER BY id ensures deterministic result if multiple rows exist.
    // In normal operation there should only ever be one row (the logged-in agent).
    @Query("SELECT * FROM agents ORDER BY id LIMIT 1")
    fun getLoggedInAgent(): Flow<AgentEntity?>

    // FIX #3: One-shot count check — avoids Flow overhead for login state checks.
    @Query("SELECT COUNT(*) FROM agents")
    suspend fun getAgentCount(): Int

    @Query("DELETE FROM agents")
    suspend fun clearAgents()
}