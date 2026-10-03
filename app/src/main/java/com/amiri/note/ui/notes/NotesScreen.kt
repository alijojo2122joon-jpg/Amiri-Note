package com.amiri.note.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amiri.note.data.entity.Note
import com.amiri.note.ui.VMFactory
import com.amiri.note.ui.common.EmptyHint
import com.amiri.note.ui.common.SectionCard
import com.amiri.note.ui.home.LocalContextApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    vm: NotesViewModel = viewModel(factory = VMFactory(LocalContextApp()))
) {
    val state by vm.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) {
                Icon(Icons.Default.Add, contentDescription = "New note")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search notes…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.showArchived) "Archived" else "All notes",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::toggleArchived) {
                    Text(if (state.showArchived) "Show active" else "Show archived")
                }
            }
            if (state.notes.isEmpty()) {
                EmptyHint("No notes found.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenNote(note.id) },
                            onPin = { vm.togglePin(note) },
                            onArchive = { vm.toggleArchive(note) },
                            onDuplicate = { vm.duplicate(note) },
                            onDelete = { vm.delete(note) }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    SectionCard(Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val title = note.title.ifBlank { note.body.take(40).ifBlank { "Untitled" } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(Icons.Default.PushPin, contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                if (note.body.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(note.body, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Spacer(Modifier.height(6.dp))
                AssistChipRow(note)
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                        onClick = { menu = false; onPin() })
                    DropdownMenuItem(text = { Text(if (note.isArchived) "Unarchive" else "Archive") },
                        onClick = { menu = false; onArchive() })
                    DropdownMenuItem(text = { Text("Duplicate") },
                        onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") },
                        onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun AssistChipRow(note: Note) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small) {
            Text(note.category, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        if (note.priority > 0) {
            val label = when (note.priority) { 3 -> "High"; 2 -> "Medium"; else -> "Low" }
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
    }
}
