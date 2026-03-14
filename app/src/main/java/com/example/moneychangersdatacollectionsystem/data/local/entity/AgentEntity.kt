package com.example.moneychangersdatacollectionsystem.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.moneychangersdatacollectionsystem.domain.model.Agent

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val badgeNumber: String,
    val role: String
)

fun AgentEntity.toDomain() = Agent(
    id = id,
    email = email,
    fullName = fullName,
    badgeNumber = badgeNumber,
    role = role
)

fun Agent.toEntity() = AgentEntity(
    id = id,
    email = email,
    fullName = fullName,
    badgeNumber = badgeNumber,
    role = role
)
