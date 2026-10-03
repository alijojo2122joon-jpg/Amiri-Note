package com.amiri.note.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiri.note.data.dao.DayTaskCounts
import com.amiri.note.data.dao.FinanceTotals
import com.amiri.note.data.entity.FinanceRecord
import com.amiri.note.data.entity.TaskItem
import com.amiri.note.data.repo.AppRepository
import com.amiri.note.security.SecurePrefs
import com.amiri.note.util.DateKeys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DayDetail(
    val tasks: List<TaskItem> = emptyList(),
    val finance: List<FinanceRecord> = emptyList(),
    val counts: DayTaskCounts = DayTaskCounts(0, 0, 0, 0),
    val totals: FinanceTotals = FinanceTotals(0.0, 0.0, 0.0, 0.0)
)

data class CalendarState(
    val year: Int,
    val month0: Int,              // 0-based
    val selectedDayKey: String,
    val daysWithData: Set<String> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repo: AppRepository,
    private val prefs: SecurePrefs
) : ViewModel() {

    private val now = Calendar.getInstance()
    private val _state = MutableStateFlow(
        CalendarState(now.get(Calendar.YEAR), now.get(Calendar.MONTH), DateKeys.dayKey())
    )
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private val selected = MutableStateFlow(DateKeys.dayKey())

    val detail: StateFlow<DayDetail> =
        selected.flatMapLatest { dayKey ->
            combine(
                repo.tasksForDay(dayKey),
                repo.financeForDay(dayKey),
                repo.taskCounts(dayKey).map { it ?: DayTaskCounts(0, 0, 0, 0) },
                repo.dayTotals(dayKey, prefs.defaultCurrency).map { it ?: FinanceTotals(0.0, 0.0, 0.0, 0.0) }
            ) { tasks, finance, counts, totals ->
                DayDetail(tasks, finance, counts, totals)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayDetail())

    init { refreshMonthIndicators() }

    fun selectDay(dayKey: String) {
        selected.value = dayKey
        _state.value = _state.value.copy(selectedDayKey = dayKey)
    }

    fun prevMonth() {
        val c = Calendar.getInstance().apply { set(_state.value.year, _state.value.month0, 1); add(Calendar.MONTH, -1) }
        _state.value = _state.value.copy(year = c.get(Calendar.YEAR), month0 = c.get(Calendar.MONTH))
        refreshMonthIndicators()
    }

    fun nextMonth() {
        val c = Calendar.getInstance().apply { set(_state.value.year, _state.value.month0, 1); add(Calendar.MONTH, 1) }
        _state.value = _state.value.copy(year = c.get(Calendar.YEAR), month0 = c.get(Calendar.MONTH))
        refreshMonthIndicators()
    }

    private fun refreshMonthIndicators() {
        viewModelScope.launch {
            val s = _state.value
            val start = DateKeys.dayKey(s.year, s.month0, 1)
            val cal = Calendar.getInstance().apply { set(s.year, s.month0, 1) }
            val last = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val end = DateKeys.dayKey(s.year, s.month0, last)
            val days = repo.daysWithTasks(start, end).toSet()
            _state.value = _state.value.copy(daysWithData = days)
        }
    }
}
