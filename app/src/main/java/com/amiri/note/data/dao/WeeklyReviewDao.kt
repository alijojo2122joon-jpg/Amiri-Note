package com.amiri.note.data.dao

import androidx.room.*
import com.amiri.note.data.entity.WeeklyReview
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReviewDao {
    @Query("SELECT * FROM weekly_reviews WHERE weekKey = :weekKey")
    fun observe(weekKey: String): Flow<WeeklyReview?>

    @Query("SELECT * FROM weekly_reviews WHERE weekKey = :weekKey")
    suspend fun get(weekKey: String): WeeklyReview?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: WeeklyReview)

    @Query("SELECT * FROM weekly_reviews")
    suspend fun getAll(): List<WeeklyReview>
}
