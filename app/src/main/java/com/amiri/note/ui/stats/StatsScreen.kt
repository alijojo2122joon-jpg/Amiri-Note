package com.amiri.note.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amiri.note.data.entity.FinanceType
import com.amiri.note.ui.VMFactory
import com.amiri.note.ui.common.ProgressBar
import com.amiri.note.ui.common.SectionCard
import com.amiri.note.ui.common.SectionHeader
import com.amiri.note.ui.home.LocalContextApp
import com.amiri.note.ui.home.fmt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    vm: StatsViewModel = viewModel(factory = VMFactory(LocalContextApp()))
) {
    val state by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var reviewText by remember(state.review) { mutableStateOf(state.review) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add money") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Statistics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

            item {
                SectionCard {
                    SectionHeader("This week — daily completion")
                    state.weekDays.forEach { bar ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(bar.label, modifier = Modifier.width(44.dp),
                                style = MaterialTheme.typography.bodyMedium)
                            Box(Modifier.weight(1f)) { ProgressBar(bar.percent / 100f) }
                            Spacer(Modifier.width(10.dp))
                            Text("${bar.percent}%", modifier = Modifier.width(44.dp),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                val wc = state.weekCounts
                val pct = if (wc.total == 0) 0f else wc.completed.toFloat() / wc.total
                SectionCard {
                    SectionHeader("Weekly totals")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("Total", wc.total.toString())
                        Stat("Done", wc.completed.toString())
                        Stat("Failed", wc.failed.toString())
                        Stat("Pending", wc.pending.toString())
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Completion: ${(pct * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(pct)
                    Spacer(Modifier.height(12.dp))
                    val wBalance = state.weekTotals.income - state.weekTotals.expense
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("Income", fmt(state.weekTotals.income))
                        Stat("Expense", fmt(state.weekTotals.expense))
                        Stat("Balance", fmt(wBalance))
                    }
                }
            }

            item {
                val mc = state.monthCounts
                val mBalance = state.monthTotals.income - state.monthTotals.expense
                SectionCard {
                    SectionHeader("This month")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("Tasks", mc.total.toString())
                        Stat("Done", mc.completed.toString())
                        Stat("Income", fmt(state.monthTotals.income))
                        Stat("Balance", fmt(mBalance))
                    }
                }
            }

            item {
                SectionCard {
                    SectionHeader("My weekly review — ${state.weekKey}")
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        placeholder = { Text("Write about your week…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.saveReview(reviewText) },
                        modifier = Modifier.align(Alignment.End)) { Text("Save review") }
                }
            }

            item { SectionHeader("Recent money records") }
            if (state.recentFinance.isEmpty()) {
                item { SectionCard { Text("No records yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            } else {
                item {
                    SectionCard {
                        state.recentFinance.forEach { f ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(f.description.ifBlank { labelFor(f.type) },
                                        style = MaterialTheme.typography.bodyLarge)
                                    Text("${labelFor(f.type)} · ${f.dayKey}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${fmt(f.amount)} ${f.currency}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorFor(f.type))
                                IconButton(onClick = { vm.deleteFinance(f.id) }) {
                                    Icon(Icons.Default.Delete, "Delete",
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }

    if (showAdd) {
        AddFinanceDialog(
            defaultCurrency = state.currency,
            onDismiss = { showAdd = false },
            onAdd = { type, amount, currency, desc, cat ->
                vm.addFinance(type, amount, currency, desc, cat)
                showAdd = false
            }
        )
    }
}

@Composable
private fun RowScope.Stat(label: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFinanceDialog(
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onAdd: (type: Int, amount: Double, currency: String, desc: String, cat: String) -> Unit
) {
    var type by remember { mutableStateOf(FinanceType.INCOME) }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var desc by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add money record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        FinanceType.INCOME to "Income",
                        FinanceType.EXPENSE to "Expense",
                        FinanceType.DEBT to "Debt",
                        FinanceType.CREDIT to "Credit"
                    ).forEach { (t, l) ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(l) })
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = currency, onValueChange = { currency = it.uppercase().take(5) },
                    label = { Text("Currency") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cat, onValueChange = { cat = it },
                    label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0) onAdd(type, a, currency.ifBlank { defaultCurrency }, desc, cat)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun labelFor(t: Int) = when (t) {
    FinanceType.INCOME -> "Income"; FinanceType.EXPENSE -> "Expense"
    FinanceType.DEBT -> "Debt"; else -> "Credit"
}
@Composable
private fun colorFor(t: Int) = when (t) {
    FinanceType.INCOME, FinanceType.CREDIT -> com.amiri.note.ui.theme.SuccessGreen
    else -> com.amiri.note.ui.theme.FailRed
}
