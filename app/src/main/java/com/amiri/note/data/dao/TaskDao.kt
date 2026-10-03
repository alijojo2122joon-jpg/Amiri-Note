package com.amiri.note.data.dao

import androidx.room.*
import com.amiri.note.data.entity.TaskItem
import kotlinx.coroutines.flow.Flow

/** Aggregate counts for a single day. */
data class DayTaskCounts(
    val total: Int,
    val completed: Int,
    val failed: Int,
    val pending: Int
)

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE dayKey = :dayKey ORDER BY sortOrder, createdAt")
    fun observeForDay(dayKey: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE dayKey = :dayKey ORDER BY sortOrder, createdAt")
    suspend fun getForDay(dayKey: String): List<TaskItem>

    @Query("""
        SELECT
            COUNT(*) AS total,
            SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS completed,
            SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS failed,
            SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS pending
        FROM tasks WHERE dayKey = :dayKey
    """)
    fun observeCounts(dayKey: String): Flow<DayTaskCounts?>

    @Query("""
        SELECT
            COUNT(*) AS total,
            SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS completed,
            SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS failed,
            SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS pending
        FROM tasks WHERE dayKey BETWEEN :start AND :end
    """)
    suspend fun countsInRange(start: String, end: String): DayTaskCounts?

    @Query("SELECT DISTINCT dayKey FROM tasks WHERE dayKey BETWEEN :start AND :end")
    suspend fun daysWithTasks(start: String, end: String): List<String>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :q || '%' ORDER BY dayKey DESC")
    fun search(q: String): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskItem): Long

    @Update suspend fun update(task: TaskItem)
    @Delete suspend fun delete(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskItem>
}
