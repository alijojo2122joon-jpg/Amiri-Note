package com.amiri.note.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Task status: 0 = pending (○), 1 = completed (✓), 2 = failed (×) */
object TaskStatus {
    const val PENDING = 0
    const val COMPLETED = 1
    const val FAILED = 2
}

@Entity(
    tableName = "tasks",
    indices = [Index("dayKey"), Index("status"), Index("sortOrder")]
)
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val status: Int = TaskStatus.PENDING,
    /** yyyy-MM-dd of the day this task belongs to */
    val dayKey: String = "",
    val priority: Int = 0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
