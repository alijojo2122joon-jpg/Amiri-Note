package com.amiri.note.ui.vault

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
import androidx.compose.ui.unit.dp
import com.amiri.note.data.entity.VaultFolder
import com.amiri.note.ui.common.EmptyHint
import com.amiri.note.ui.common.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultFoldersScreen(vm: VaultViewModel, onBack: () -> Unit, onOpenFolder: (Long) -> Unit) {
    val folders by vm.folders.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<VaultFolder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Folders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCreate = true },
                icon = { Icon(Icons.Default.CreateNewFolder, null) }, text = { Text("New folder") })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        if (folders.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize()) {
                EmptyHint("No folders yet. Create folders like Personal, Work, Important.")
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(folders, key = { it.id }) { folder ->
                    var menu by remember { mutableStateOf(false) }
                    SectionCard(Modifier.clickable { onOpenFolder(folder.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(folder.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Options") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; renaming = folder })
                                    DropdownMenuItem(text = { Text("Delete (items go back to root)") }, onClick = { menu = false; vm.deleteFolder(folder.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        FolderNameDialog("New folder", "", onDismiss = { showCreate = false }) {
            vm.createFolder(it); showCreate = false
        }
    }
    renaming?.let { f ->
        FolderNameDialog("Rename folder", f.name, onDismiss = { renaming = null }) {
            vm.renameFolder(f.id, it); renaming = null
        }
    }
}

@Composable
private fun FolderNameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it },
                label = { Text("Folder name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
