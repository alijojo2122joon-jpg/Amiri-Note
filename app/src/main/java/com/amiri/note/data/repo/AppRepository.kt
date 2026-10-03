package com.amiri.note.data.repo

import com.amiri.note.data.dao.DayTaskCounts
import com.amiri.note.data.dao.FinanceDao
import com.amiri.note.data.dao.FinanceTotals
import com.amiri.note.data.dao.NoteDao
import com.amiri.note.data.dao.TaskDao
import com.amiri.note.data.dao.WeeklyReviewDao
import com.amiri.note.data.db.AppDatabase
import com.amiri.note.data.entity.FinanceRecord
import com.amiri.note.data.entity.Note
import com.amiri.note.data.entity.TaskItem
import com.amiri.note.data.entity.WeeklyReview
import com.amiri.note.util.DateKeys
import kotlinx.coroutines.flow.Flow

/** Repository for the non-vault (normal app) data. */
class AppRepository(db: AppDatabase) {

    private val noteDao: NoteDao = db.noteDao()
    private val taskDao: TaskDao = db.taskDao()
    private val financeDao: FinanceDao = db.financeDao()
    private val reviewDao: WeeklyReviewDao = db.weeklyReviewDao()

    // Notes
    fun activeNotes(): Flow<List<Note>> = noteDao.observeActive()
    fun archivedNotes(): Flow<List<Note>> = noteDao.observeArchived()
    fun recentNotes(limit: Int = 5): Flow<List<Note>> = noteDao.observeRecent(limit)
    fun searchNotes(q: String): Flow<List<Note>> = noteDao.search(q)
    fun categories(): Flow<List<String>> = noteDao.observeCategories()
    suspend fun getNote(id: Long): Note? = noteDao.getById(id)
    suspend fun saveNote(note: Note): Long =
        noteDao.upsert(note.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteNote(id: Long) = noteDao.deleteById(id)
    suspend fun duplicateNote(id: Long): Long? {
        val n = noteDao.getById(id) ?: return null
        return noteDao.upsert(
            n.copy(id = 0, title = n.title + " (copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis())
        )
    }

    // Tasks
    fun tasksForDay(dayKey: String): Flow<List<TaskItem>> = taskDao.observeForDay(dayKey)
    fun taskCounts(dayKey: String): Flow<DayTaskCounts?> = taskDao.observeCounts(dayKey)
    fun searchTasks(q: String): Flow<List<TaskItem>> = taskDao.search(q)
    suspend fun saveTask(task: TaskItem): Long =
        taskDao.upsert(task.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteTask(id: Long) = taskDao.deleteById(id)
    suspend fun duplicateTask(task: TaskItem): Long =
        taskDao.upsert(task.copy(id = 0, createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()))
    suspend fun rangeTaskCounts(start: String, end: String): DayTaskCounts? =
        taskDao.countsInRange(start, end)
    suspend fun daysWithTasks(start: String, end: String): List<String> =
        taskDao.daysWithTasks(start, end)

    // Finance
    fun financeForDay(dayKey: String): Flow<List<FinanceRecord>> = financeDao.observeForDay(dayKey)
    fun recentFinance(limit: Int = 20): Flow<List<FinanceRecord>> = financeDao.observeRecent(limit)
    fun dayTotals(dayKey: String, currency: String): Flow<FinanceTotals?> =
        financeDao.observeDayTotals(dayKey, currency)
    fun currencies(): Flow<List<String>> = financeDao.observeCurrencies()
    fun searchFinance(q: String): Flow<List<FinanceRecord>> = financeDao.search(q)
    suspend fun saveFinance(r: FinanceRecord): Long = financeDao.upsert(r)
    suspend fun deleteFinance(id: Long) = financeDao.deleteById(id)
    suspend fun rangeTotals(start: String, end: String, currency: String): FinanceTotals? =
        financeDao.totalsInRange(start, end, currency)

    // Weekly review
    fun weeklyReview(weekKey: String): Flow<WeeklyReview?> = reviewDao.observe(weekKey)
    suspend fun saveWeeklyReview(weekKey: String, text: String) =
        reviewDao.upsert(WeeklyReview(weekKey, text, System.currentTimeMillis()))

    fun todayKey(): String = DateKeys.dayKey()
}
