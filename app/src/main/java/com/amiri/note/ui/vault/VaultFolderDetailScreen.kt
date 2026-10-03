package com.amiri.note.ui.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amiri.note.ui.common.EmptyHint

/** Contents of one vault folder (photos, videos and files together). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultFolderDetailScreen(
    folderId: Long,
    vm: VaultViewModel,
    onBack: () -> Unit,
    onOpenItem: (Long) -> Unit
) {
    val folders by vm.folders.collectAsState()
    val name = folders.firstOrNull { it.id == folderId }?.name ?: "Folder"
    val items by remember(folderId) { vm.itemsInFolder(folderId) }.collectAsState(initial = emptyList())
    val selection by vm.selection.collectAsState()
    val busy by vm.busy.collectAsState()
    val selecting = selection.isNotEmpty()
    val snackbar = remember { SnackbarHostState() }
    var showMove by remember { mutableStateOf(false) }
    VaultToastEffect(vm, snackbar)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.externalActive = false
        if (uris.isNotEmpty()) vm.importUris(uris, folderId, true)
    }

    Scaffold(
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selection.size} selected") },
                    navigationIcon = { IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Default.Close, "Clear") } },
                    actions = {
                        IconButton(onClick = { vm.selectAll(items.map { it.id }) }) { Icon(Icons.Default.SelectAll, "All") }
                        IconButton(onClick = { showMove = true }) { Icon(Icons.Default.Folder, "Move") }
                        IconButton(onClick = { vm.moveSelectedToTrash() }) { Icon(Icons.Default.Delete, "Trash") }
                    }
                )
            } else {
                TopAppBar(title = { Text(name, maxLines = 1) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } })
            }
        },
        floatingActionButton = {
            if (!selecting) ExtendedFloatingActionButton(
                onClick = {
                    vm.externalActive = true
                    try { picker.launch(arrayOf("*/*")) } catch (t: Throwable) { vm.externalActive = false }
                },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add") })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (items.isEmpty()) {
                EmptyHint("This folder is empty.\nTap Add, or select items in Photos / Videos / Files and use “Move to folder”.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        GalleryCell(
                            item = item, vm = vm,
                            selected = selection.contains(item.id), selecting = selecting,
                            onClick = { if (selecting) vm.toggleSelect(item.id) else onOpenItem(item.id) },
                            onLongClick = { vm.toggleSelect(item.id) }
                        )
                    }
                }
            }
            if (busy) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }

    if (showMove) {
        MoveToFolderDialog(vm, onDismiss = { showMove = false }) { target ->
            vm.moveSelectedToFolder(target); showMove = false
        }
    }
}
