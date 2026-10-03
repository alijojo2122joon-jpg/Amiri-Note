package com.amiri.note.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amiri.note.data.entity.Note
import com.amiri.note.data.entity.TaskItem
import com.amiri.note.data.entity.TaskStatus
import com.amiri.note.ui.VMFactory
import com.amiri.note.ui.common.*
import com.amiri.note.ui.theme.FailRed
import com.amiri.note.ui.theme.PendingGray
import com.amiri.note.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onOpenNote: (Long) -> Unit,
    onSeeAllNotes: () -> Unit,
    vm: HomeViewModel = viewModel(factory = VMFactory(LocalContextApp()))
) {
    val state by vm.state.collectAsState()
    var quick by remember { mutableStateOf("") }
    var newTask by remember { mutableStateOf("") }

    val total = state.counts.total
    val done = state.counts.completed
    val progress = if (total == 0) 0f else done.toFloat() / total
    val balance = state.totals.income - state.totals.expense

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(greeting() + ", Amir",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text(todayLong(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            SectionCard {
                SectionHeader("Today's Progress")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { ProgressBar(progress) }
                    Spacer(Modifier.width(12.dp))
                    Text("${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("$done / $total tasks completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            SectionCard {
                SectionHeader("Quick Note")
                OutlinedTextField(
                    value = quick,
                    onValueChange = { quick = it },
                    placeholder = { Text("Write something…") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (quick.isNotBlank()) {
                            IconButton(onClick = { vm.quickNote(quick); quick = "" }) {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        vm.quickNote(quick); quick = ""
                    }),
                    maxLines = 3
                )
            }
        }

        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Today's Tasks", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                if (state.tasks.isEmpty()) {
                    EmptyHint("No tasks yet. Add one below.")
                } else {
                    state.tasks.forEach { task ->
                        TaskRow(task = task, onClick = { vm.toggleTask(task) })
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = newTask,
                    onValueChange = { newTask = it },
                    placeholder = { Text("Add a task…") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { vm.addTask(newTask); newTask = "" }) {
                            Icon(Icons.Default.Add, contentDescription = "Add task")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.addTask(newTask); newTask = "" }),
                    singleLine = true
                )
            }
        }

        item {
            SectionCard {
                SectionHeader("Today's Money")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatPill("Income", fmt(state.totals.income), SuccessGreen)
                    StatPill("Expenses", fmt(state.totals.expense), FailRed)
                    StatPill("Balance", fmt(balance))
                }
                Spacer(Modifier.height(4.dp))
                Text("Currency: ${state.currency}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Recent Notes", Modifier.weight(1f))
                TextButton(onClick = onSeeAllNotes) { Text("See all") }
            }
        }
        if (state.recentNotes.isEmpty()) {
            item { SectionCard { EmptyHint("Your notes will appear here.") } }
        } else {
            items(state.recentNotes, key = { it.id }) { note ->
                RecentNoteRow(note = note, onClick = { onOpenNote(note.id) })
            }
        }

        item { Spacer(Modifier.height(72.dp)) } // room above bottom nav
    }
}

@Composable
private fun TaskRow(task: TaskItem, onClick: () -> Unit) {
    val (icon, tint) = statusVisual(task.status)
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(task.title, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RecentNoteRow(note: Note, onClick: () -> Unit) {
    SectionCard(Modifier.clickable { onClick() }) {
        val title = note.title.ifBlank { note.body.take(40).ifBlank { "Untitled" } }
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        if (note.body.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(note.body, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

fun statusVisual(status: Int): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (status) {
    TaskStatus.COMPLETED -> Icons.Default.CheckCircle to SuccessGreen
    TaskStatus.FAILED -> Icons.Default.Cancel to FailRed
    else -> Icons.Default.RadioButtonUnchecked to PendingGray
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Hello" }
}
private fun todayLong(): String =
    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(Locale.US, "%.2f", v)

/** Helper to grab Application for the default VMFactory param. */
@Composable
fun LocalContextApp(): android.app.Application =
    androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
