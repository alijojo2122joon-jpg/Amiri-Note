package com.amiri.note.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amiri.note.ui.VMFactory
import com.amiri.note.ui.common.SectionCard
import com.amiri.note.ui.common.SectionHeader
import com.amiri.note.ui.home.LocalContextApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    onSecretTriggered: () -> Unit,
    vm: NoteEditorViewModel = viewModel(factory = VMFactory(LocalContextApp()))
) {
    LaunchedEffect(noteId) { vm.load(noteId) }
    val state by vm.state.collectAsState()
    val secret by vm.secretTriggered.collectAsState()

    // Secret entrance: when the phrase appears, route to the vault flow silently.
    LaunchedEffect(secret) {
        if (secret) {
            vm.consumeSecret()
            onSecretTriggered()
        }
    }

    var newCheck by remember { mutableStateOf("") }
    var showMeta by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { vm.persistNow(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.togglePin() }) {
                        Icon(if (state.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (state.isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showMeta = !showMeta }) {
                        Icon(Icons.Default.Tune, contentDescription = "Details")
                    }
                    IconButton(onClick = { vm.deleteCurrent(onBack) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicField(
                value = state.title,
                onValueChange = vm::onTitle,
                placeholder = "Title",
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (showMeta) {
                SectionCard {
                    SectionHeader("Details")
                    OutlinedTextField(
                        value = state.category,
                        onValueChange = vm::onCategory,
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Priority", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "None", 1 to "Low", 2 to "Medium", 3 to "High").forEach { (p, label) ->
                            FilterChip(
                                selected = state.priority == p,
                                onClick = { vm.onPriority(p) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            BasicField(
                value = state.body,
                onValueChange = vm::onBody,
                placeholder = "Start writing…",
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 6
            )

            SectionCard {
                SectionHeader("Checklist")
                state.checklist.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Checkbox(checked = item.done, onCheckedChange = { vm.toggleChecklistItem(index) })
                        Text(item.text, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { vm.removeChecklistItem(index) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove",
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value = newCheck,
                    onValueChange = { newCheck = it },
                    placeholder = { Text("Add checklist item…") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { vm.addChecklistItem(newCheck); newCheck = "" }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.addChecklistItem(newCheck); newCheck = "" }),
                    singleLine = true
                )
            }

            Text("Saved automatically",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun BasicField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = textStyle) },
        textStyle = textStyle,
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}
