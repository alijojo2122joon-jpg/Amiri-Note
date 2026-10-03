package com.amiri.note.data.dao

import androidx.room.*
import com.amiri.note.data.entity.FinanceRecord
import kotlinx.coroutines.flow.Flow

/** Sum of amounts grouped by type for a period. */
data class FinanceTotals(
    val income: Double,
    val expense: Double,
    val debt: Double,
    val credit: Double
)

@Dao
interface FinanceDao {

    @Query("SELECT * FROM finance WHERE dayKey = :dayKey ORDER BY createdAt DESC")
    fun observeForDay(dayKey: String): Flow<List<FinanceRecord>>

    @Query("SELECT * FROM finance ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FinanceRecord>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 0 THEN amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) AS expense,
            COALESCE(SUM(CASE WHEN type = 2 THEN amount ELSE 0 END), 0) AS debt,
            COALESCE(SUM(CASE WHEN type = 3 THEN amount ELSE 0 END), 0) AS credit
        FROM finance WHERE dayKey = :dayKey AND currency = :currency
    """)
    fun observeDayTotals(dayKey: String, currency: String): Flow<FinanceTotals?>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 0 THEN amount ELSE 0 END), 0) AS income,
            COALESCE(SUM(CASE WHEN type = 1 THEN amount ELSE 0 END), 0) AS expense,
            COALESCE(SUM(CASE WHEN type = 2 THEN amount ELSE 0 END), 0) AS debt,
            COALESCE(SUM(CASE WHEN type = 3 THEN amount ELSE 0 END), 0) AS credit
        FROM finance WHERE dayKey BETWEEN :start AND :end AND currency = :currency
    """)
    suspend fun totalsInRange(start: String, end: String, currency: String): FinanceTotals?

    @Query("SELECT DISTINCT currency FROM finance ORDER BY currency")
    fun observeCurrencies(): Flow<List<String>>

    @Query("SELECT * FROM finance WHERE description LIKE '%' || :q || '%' OR category LIKE '%' || :q || '%' ORDER BY createdAt DESC")
    fun search(q: String): Flow<List<FinanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: FinanceRecord): Long

    @Update suspend fun update(record: FinanceRecord)
    @Delete suspend fun delete(record: FinanceRecord)

    @Query("DELETE FROM finance WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM finance")
    suspend fun getAll(): List<FinanceRecord>
}
