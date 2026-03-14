package com.example.moneychangersdatacollectionsystem.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Agent(
    val id: String,
    val email: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("badge_number")
    val badgeNumber: String,
    val role: String
)
