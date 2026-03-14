package com.example.moneychangersdatacollectionsystem.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Fts4(contentEntity = CollectionEntity::class)
@Entity(tableName = "collections_fts")
data class CollectionFtsEntity(
    val id: String,
    val businessName: String,
    val physicalAddress: String,
    val county: String,
    val agentId: String,
    val originCounty: String
)
