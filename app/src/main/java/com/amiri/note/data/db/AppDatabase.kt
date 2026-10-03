package com.amiri.note.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.amiri.note.data.dao.FinanceDao
import com.amiri.note.data.dao.NoteDao
import com.amiri.note.data.dao.TaskDao
import com.amiri.note.data.dao.VaultDao
import com.amiri.note.data.dao.WeeklyReviewDao
import com.amiri.note.data.entity.FinanceRecord
import com.amiri.note.data.entity.Note
import com.amiri.note.data.entity.TaskItem
import com.amiri.note.data.entity.VaultFolder
import com.amiri.note.data.entity.VaultItem
import com.amiri.note.data.entity.WeeklyReview

@Database(
    entities = [
        Note::class,
        TaskItem::class,
        FinanceRecord::class,
        VaultItem::class,
        VaultFolder::class,
        WeeklyReview::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun financeDao(): FinanceDao
    abstract fun vaultDao(): VaultDao
    abstract fun weeklyReviewDao(): WeeklyReviewDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amiri_note.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
