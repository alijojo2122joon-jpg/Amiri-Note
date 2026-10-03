package com.amiri.note.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** User-authored weekly review text, keyed by ISO week (e.g. "2026-W40"). */
@Entity(tableName = "weekly_reviews")
data class WeeklyReview(
    @PrimaryKey val weekKey: String,
    val text: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
