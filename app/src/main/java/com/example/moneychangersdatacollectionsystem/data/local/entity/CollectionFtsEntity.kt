package com.example.moneychangersdatacollectionsystem.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * CollectionFtsEntity — FTS4 full-text search index over CollectionEntity.
 *
 * FIX: `id` field REMOVED.
 *
 * With `contentEntity = CollectionEntity::class`, Room automatically links
 * FTS rows to their corresponding content rows via SQLite's implicit rowid.
 * Room generates the correct CREATE VIRTUAL TABLE statement that maps the
 * FTS docid to the content table's rowid — no explicit id field is needed
 * or wanted here.
 *
 * Declaring `id` as an explicit field in a contentEntity FTS table creates
 * column name ambiguity: SQLite's FTS machinery and Room's generated SQL
 * both try to reference the content table's `id` column, producing either
 * a compile-time Room schema error or incorrect runtime query results.
 *
 * The fields listed here are the COLUMNS TO INDEX for full-text search.
 * They must exactly match column names in CollectionEntity.
 * The primary key (id) is handled by Room/SQLite automatically via rowid.
 *
 * INDEXED FIELDS (chosen for search relevance):
 *   businessName    — primary search term for operators
 *   physicalAddress — location-based search
 *   county          — geographic filter
 *   agentId         — audit/agent search
 *   originCounty    — demographic search
 *   personName      — individual name search
 *   nationality     — demographic search
 */
@Fts4(contentEntity = CollectionEntity::class)
@Entity(tableName = "collections_fts")
data class CollectionFtsEntity(
    val businessName: String,
    val physicalAddress: String,
    val county: String,
    val agentId: String,
    val originCounty: String,
    val personName: String,
    val nationality: String
)