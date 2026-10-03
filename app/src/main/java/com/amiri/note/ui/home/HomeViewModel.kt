package com.amiri.note.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiri.note.data.dao.DayTaskCounts
import com.amiri.note.data.dao.FinanceTotals
import com.amiri.note.data.entity.Note
import com.amiri.note.data.entity.TaskItem
import com.amiri.note.data.entity.TaskStatus
import com.amiri.note.data.repo.AppRepository
import com.amiri.note.util.DateKeys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val dayKey: String = DateKeys.dayKey(),
    val counts: DayTaskCounts = DayTaskCounts(0, 0, 0, 0),
    val tasks: List<TaskItem> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val totals: FinanceTotals = FinanceTotals(0.0, 0.0, 0.0, 0.0),
    val currency: String = "AFN"
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: AppRepository) : ViewModel() {

    private val today = DateKeys.dayKey()
    private val currencyFlow = MutableStateFlow("AFN")

    val state: StateFlow<HomeState> =
        combine(
            repo.taskCounts(today),
            repo.tasksForDay(today),
            repo.recentNotes(5),
            repo.dayTotals(today, "AFN"),
            currencyFlow
        ) { counts, tasks, notes, totals, currency ->
            HomeState(
                dayKey = today,
                counts = counts ?: DayTaskCounts(0, 0, 0, 0),
                tasks = tasks,
                recentNotes = notes,
                totals = totals ?: FinanceTotals(0.0, 0.0, 0.0, 0.0),
                currency = currency
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    fun setCurrency(c: String) { currencyFlow.value = c }

    fun quickNote(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.saveNote(Note(title = "", body = text.trim()))
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            val next = when (task.status) {
                TaskStatus.PENDING -> TaskStatus.COMPLETED
                TaskStatus.COMPLETED -> TaskStatus.FAILED
                else -> TaskStatus.PENDING
            }
            repo.saveTask(task.copy(status = next))
        }
    }

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.saveTask(TaskItem(title = title.trim(), dayKey = today))
        }
    }
}
