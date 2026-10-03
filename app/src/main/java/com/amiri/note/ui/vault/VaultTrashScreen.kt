package com.amiri.note.ui.vault

import androidx.compose.foundation.combinedClickable
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
import com.amiri.note.ui.common.EmptyHint
import com.amiri.note.ui.common.SectionCard

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VaultTrashScreen(vm: VaultViewModel, onBack: () -> Unit) {
    val trash by vm.trash.collectAsState()
    val selection by vm.selection.collectAsState()
    val selecting = selection.isNotEmpty()
    var confirmEmpty by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    VaultToastEffect(vm, snackbar)

    Scaffold(
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selection.size} selected") },
                    navigationIcon = { IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Default.Close, "Clear") } },
                    actions = {
                        IconButton(onClick = { vm.restoreSelectedFromTrash() }) { Icon(Icons.Default.Restore, "Restore") }
                        IconButton(onClick = { vm.deleteSelectedPermanently() }) { Icon(Icons.Default.DeleteForever, "Delete") }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Trash") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                    actions = {
                        if (trash.isNotEmpty())
                            TextButton(onClick = { confirmEmpty = true }) { Text("Empty") }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        if (trash.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize()) { EmptyHint("Trash is empty.") }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trash, key = { it.id }) { item ->
                    SectionCard(Modifier.combinedClickable(
                        onClick = { vm.toggleSelect(item.id) },
                        onLongClick = { vm.toggleSelect(item.id) }
                    )) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (selection.contains(item.id)) Icons.Default.CheckCircle else Icons.Default.Delete,
                                contentDescription = null,
                                tint = if (selection.contains(item.id)) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.displayName, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                                Text(fmtBytes(item.sizeBytes), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("Empty Trash?") },
            text = { Text("This permanently deletes all items in Trash. This can't be undone.") },
            confirmButton = { TextButton(onClick = { vm.emptyTrash(); confirmEmpty = false }) { Text("Empty") } },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancel") } }
        )
    }
}
