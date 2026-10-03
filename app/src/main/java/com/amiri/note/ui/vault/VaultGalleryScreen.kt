package com.amiri.note.ui.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.amiri.note.data.entity.VaultItem
import com.amiri.note.data.entity.VaultKind
import com.amiri.note.ui.common.EmptyHint

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultGalleryScreen(
    kind: Int,
    vm: VaultViewModel,
    onBack: () -> Unit,
    onOpenItem: (Long) -> Unit,
    onAddFromDevice: () -> Unit
) {
    val items by (if (kind == VaultKind.PHOTO) vm.photos else vm.videos).collectAsState()
    val selection by vm.selection.collectAsState()
    val busy by vm.busy.collectAsState()
    val selecting = selection.isNotEmpty()
    val snackbar = remember { SnackbarHostState() }
    var showMove by remember { mutableStateOf(false) }
    VaultToastEffect(vm, snackbar)

    val isPhoto = kind == VaultKind.PHOTO

    // "From files": classic file browser (originals are removed too where Android allows it).
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        vm.externalActive = false
        if (uris.isNotEmpty()) vm.importUris(uris, folderId = 0, deleteSource = true)
    }
    fun launchFilePicker() {
        vm.externalActive = true
        try { filePicker.launch(arrayOf(if (isPhoto) "image/*" else "video/*")) }
        catch (t: Throwable) { vm.externalActive = false }
    }

    val title = if (isPhoto) "Photos" else "Videos"

    Scaffold(
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selection.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Default.Close, "Clear") }
                    },
                    actions = {
                        IconButton(onClick = { vm.selectAll(items.map { it.id }) }) {
                            Icon(Icons.Default.SelectAll, "Select all")
                        }
                        IconButton(onClick = { showMove = true }) { Icon(Icons.Default.Folder, "Move to folder") }
                        IconButton(onClick = { vm.moveSelectedToTrash() }) { Icon(Icons.Default.Delete, "Move to trash") }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                    actions = {
                        IconButton(onClick = { launchFilePicker() }) {
                            Icon(Icons.Default.FolderOpen, "Add from files")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!selecting) {
                ExtendedFloatingActionButton(
                    onClick = onAddFromDevice,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (items.isEmpty()) {
                EmptyHint("No $title yet.\nTap Add to pick from your phone. They are encrypted into the vault and removed from your gallery.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        GalleryCell(
                            item = item,
                            vm = vm,
                            selected = selection.contains(item.id),
                            selecting = selecting,
                            onClick = {
                                if (selecting) vm.toggleSelect(item.id) else onOpenItem(item.id)
                            },
                            onLongClick = { vm.toggleSelect(item.id) }
                        )
                    }
                }
            }
            if (busy) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showMove) {
        MoveToFolderDialog(vm, onDismiss = { showMove = false }) { folderId ->
            vm.moveSelectedToFolder(folderId); showMove = false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GalleryCell(
    item: VaultItem,
    vm: VaultViewModel,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        VaultThumb(item, vm, Modifier.fillMaxSize())
        if (selecting) {
            Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.TopEnd) {
                Icon(
                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }
    }
}
