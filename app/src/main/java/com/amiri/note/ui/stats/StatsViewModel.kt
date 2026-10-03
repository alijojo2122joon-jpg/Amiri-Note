package com.amiri.note.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiri.note.data.dao.DayTaskCounts
import com.amiri.note.data.dao.FinanceTotals
import com.amiri.note.data.entity.FinanceRecord
import com.amiri.note.data.entity.WeeklyReview
import com.amiri.note.data.repo.AppRepository
import com.amiri.note.security.SecurePrefs
import com.amiri.note.util.DateKeys
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DayBar(val label: String, val dayKey: String, val percent: Int)

data class StatsState(
    val currency: String = "AFN",
    val weekDays: List<DayBar> = emptyList(),
    val weekCounts: DayTaskCounts = DayTaskCounts(0, 0, 0, 0),
    val weekTotals: FinanceTotals = FinanceTotals(0.0, 0.0, 0.0, 0.0),
    val monthTotals: FinanceTotals = FinanceTotals(0.0, 0.0, 0.0, 0.0),
    val monthCounts: DayTaskCounts = DayTaskCounts(0, 0, 0, 0),
    val weekKey: String = DateKeys.weekKey(),
    val review: String = "",
    val recentFinance: List<FinanceRecord> = emptyList()
)

class StatsViewModel(
    private val repo: AppRepository,
    private val prefs: SecurePrefs
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState(currency = prefs.defaultCurrency))
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repo.weeklyReview(DateKeys.weekKey()).collect { r ->
                _state.value = _state.value.copy(review = r?.text ?: "")
            }
        }
        viewModelScope.launch {
            repo.recentFinance(15).collect { list ->
                _state.value = _state.value.copy(recentFinance = list)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val cur = prefs.defaultCurrency
            val days = DateKeys.weekDays()
            val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val bars = days.mapIndexed { i, dk ->
                val c = repo.rangeTaskCounts(dk, dk)
                val pct = if (c == null || c.total == 0) 0 else (c.completed * 100 / c.total)
                DayBar(labels[i], dk, pct)
            }
            val weekCounts = repo.rangeTaskCounts(days.first(), days.last()) ?: DayTaskCounts(0,0,0,0)
            val weekTotals = repo.rangeTotals(days.first(), days.last(), cur) ?: FinanceTotals(0.0,0.0,0.0,0.0)
            val monthTotals = repo.rangeTotals(DateKeys.startOfMonthKey(), DateKeys.endOfMonthKey(), cur)
                ?: FinanceTotals(0.0,0.0,0.0,0.0)
            val monthCounts = repo.rangeTaskCounts(DateKeys.startOfMonthKey(), DateKeys.endOfMonthKey())
                ?: DayTaskCounts(0,0,0,0)
            _state.value = _state.value.copy(
                currency = cur,
                weekDays = bars,
                weekCounts = weekCounts,
                weekTotals = weekTotals,
                monthTotals = monthTotals,
                monthCounts = monthCounts
            )
        }
    }

    fun saveReview(text: String) {
        viewModelScope.launch { repo.saveWeeklyReview(DateKeys.weekKey(), text) }
    }

    fun addFinance(type: Int, amount: Double, currency: String, description: String, category: String) {
        viewModelScope.launch {
            repo.saveFinance(
                FinanceRecord(
                    type = type, amount = amount, currency = currency,
                    description = description, category = category.ifBlank { "General" },
                    dayKey = DateKeys.dayKey()
                )
            )
            refresh()
        }
    }

    fun deleteFinance(id: Long) {
        viewModelScope.launch { repo.deleteFinance(id); refresh() }
    }
}
