package com.amiri.note.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amiri.note.data.entity.FinanceType
import com.amiri.note.ui.VMFactory
import com.amiri.note.ui.common.SectionCard
import com.amiri.note.ui.common.SectionHeader
import com.amiri.note.ui.home.LocalContextApp
import com.amiri.note.ui.home.fmt
import com.amiri.note.ui.home.statusVisual
import com.amiri.note.util.DateKeys
import java.util.Calendar

@Composable
fun CalendarScreen(
    vm: CalendarViewModel = viewModel(factory = VMFactory(LocalContextApp()))
) {
    val state by vm.state.collectAsState()
    val detail by vm.detail.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionCard {
                MonthHeader(state.year, state.month0, vm::prevMonth, vm::nextMonth)
                Spacer(Modifier.height(8.dp))
                WeekdayLabels()
                MonthGrid(
                    year = state.year,
                    month0 = state.month0,
                    selectedKey = state.selectedDayKey,
                    daysWithData = state.daysWithData,
                    onSelect = vm::selectDay
                )
            }
        }

        item {
            SectionCard {
                SectionHeader("Day summary — ${state.selectedDayKey}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Tasks", detail.counts.total.toString())
                    MiniStat("Done", detail.counts.completed.toString())
                    MiniStat("Failed", detail.counts.failed.toString())
                    MiniStat("Pending", detail.counts.pending.toString())
                }
                Spacer(Modifier.height(8.dp))
                val balance = detail.totals.income - detail.totals.expense
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Income", fmt(detail.totals.income))
                    MiniStat("Expense", fmt(detail.totals.expense))
                    MiniStat("Balance", fmt(balance))
                }
            }
        }

        item { SectionHeader("Tasks") }
        if (detail.tasks.isEmpty()) {
            item { SectionCard { Text("No tasks this day.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            item {
                SectionCard {
                    detail.tasks.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)) {
                            val (icon, tint) = statusVisual(t.status)
                            Icon(icon, contentDescription = null, tint = tint)
                            Spacer(Modifier.width(10.dp))
                            Text(t.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        item { SectionHeader("Finance") }
        if (detail.finance.isEmpty()) {
            item { SectionCard { Text("No records this day.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            item {
                SectionCard {
                    detail.finance.forEach { f ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(f.description.ifBlank { typeLabel(f.type) },
                                    style = MaterialTheme.typography.bodyLarge)
                                Text("${typeLabel(f.type)} · ${f.category}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${fmt(f.amount)} ${f.currency}",
                                style = MaterialTheme.typography.titleMedium,
                                color = typeColor(f.type))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun MonthHeader(year: Int, month0: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, "Previous") }
        Text(monthName(month0) + " " + year,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f))
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Next") }
    }
}

@Composable
private fun WeekdayLabels() {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(Modifier.fillMaxWidth()) {
        labels.forEach {
            Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthGrid(
    year: Int, month0: Int, selectedKey: String,
    daysWithData: Set<String>, onSelect: (String) -> Unit
) {
    val cal = Calendar.getInstance().apply { set(year, month0, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Monday-based offset
    val firstDow = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) // Mon=0..Sun=6
    val cells = firstDow + daysInMonth
    val rows = (cells + 6) / 7

    Column {
        var day = 1
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    if (index < firstDow || day > daysInMonth) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val key = DateKeys.dayKey(year, month0, day)
                        DayCell(
                            day = day,
                            selected = key == selectedKey,
                            hasData = daysWithData.contains(key),
                            onClick = { onSelect(key) },
                            modifier = Modifier.weight(1f)
                        )
                        day++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, selected: Boolean, hasData: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                if (hasData && !selected) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}

@Composable
private fun RowScope.MiniStat(label: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun typeLabel(t: Int) = when (t) {
    FinanceType.INCOME -> "Income"; FinanceType.EXPENSE -> "Expense"
    FinanceType.DEBT -> "Debt"; else -> "Credit"
}
@Composable
private fun typeColor(t: Int) = when (t) {
    FinanceType.INCOME, FinanceType.CREDIT -> com.amiri.note.ui.theme.SuccessGreen
    else -> com.amiri.note.ui.theme.FailRed
}
private fun monthName(m0: Int) = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December")[m0]
