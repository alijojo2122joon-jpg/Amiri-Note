package com.amiri.note.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A note. Body may contain the secret phrase; detection happens in the UI layer,
 * the phrase itself is never persisted as a flag here.
 */
@Entity(
    tableName = "notes",
    indices = [Index("category"), Index("isPinned"), Index("isArchived"), Index("updatedAt")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    /** JSON-encoded checklist items: see ChecklistCodec */
    val checklist: String = "",
    val category: String = "General",
    val priority: Int = 0,          // 0 none, 1 low, 2 medium, 3 high
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** optional reminder date-time, epoch millis, 0 = none */
    val dueAt: Long = 0
)
