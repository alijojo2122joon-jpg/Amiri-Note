package com.amiri.note.ui.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shows the ViewModel's one-shot messages ("Added 3 items", errors…) as snackbars. */
@Composable
fun VaultToastEffect(vm: VaultViewModel, snackbar: SnackbarHostState) {
    val toast by vm.toast.collectAsState()
    LaunchedEffect(toast) {
        toast?.let {
            vm.clearToast()
            snackbar.showSnackbar(it)
        }
    }
}

/** Lets the user choose a destination folder for the selected items. */
@Composable
fun MoveToFolderDialog(
    vm: VaultViewModel,
    onDismiss: () -> Unit,
    onPick: (folderId: Long) -> Unit
) {
    val folders by vm.folders.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            LazyColumn {
                item {
                    FolderPickRow("No folder (root)") { onPick(0L) }
                }
                items(folders, key = { it.id }) { f ->
                    FolderPickRow(f.name) { onPick(f.id) }
                }
                if (folders.isEmpty()) {
                    item {
                        Text("You have no folders yet. Create one from Folders on the Private screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FolderPickRow(name: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}
