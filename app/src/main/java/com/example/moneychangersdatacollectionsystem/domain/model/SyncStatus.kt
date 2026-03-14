package com.example.moneychangersdatacollectionsystem.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}
