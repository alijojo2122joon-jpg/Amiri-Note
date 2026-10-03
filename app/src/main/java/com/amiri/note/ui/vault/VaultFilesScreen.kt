package com.amiri.note.ui.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.amiri.note.data.entity.VaultItem
import com.amiri.note.ui.common.EmptyHint
import com.amiri.note.ui.common.SectionCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VaultFilesScreen(
    vm: VaultViewModel,
    onBack: () -> Unit,
    onOpenItem: (Long) -> Unit
) {
    val files by vm.files.collectAsState()
    val selection by vm.selection.collectAsState()
    val busy by vm.busy.collectAsState()
    val selecting = selection.isNotEmpty()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        vm.externalActive = false
        if (uris.isNotEmpty()) vm.importUris(uris, 0, true)
    }
    val snackbar = remember { SnackbarHostState() }
    var showMove by remember { mutableStateOf(false) }
    VaultToastEffect(vm, snackbar)

    Scaffold(
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${selection.size} selected") },
                    navigationIcon = { IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Default.Close, "Clear") } },
                    actions = {
                        IconButton(onClick = { vm.selectAll(files.map { it.id }) }) { Icon(Icons.Default.SelectAll, "All") }
                        IconButton(onClick = { showMove = true }) { Icon(Icons.Default.Folder, "Move to folder") }
                        IconButton(onClick = { vm.moveSelectedToTrash() }) { Icon(Icons.Default.Delete, "Trash") }
                    }
                )
            } else {
                TopAppBar(title = { Text("Files") },
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
            if (files.isEmpty()) {
                EmptyHint("No files yet. Add PDFs, documents, audio, archives — all encrypted.")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(files, key = { it.id }) { item ->
                        FileRow(item, selection.contains(item.id), selecting,
                            onClick = { if (selecting) vm.toggleSelect(item.id) else onOpenItem(item.id) },
                            onLong = { vm.toggleSelect(item.id) })
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
            if (busy) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }

    if (showMove) {
        MoveToFolderDialog(vm, onDismiss = { showMove = false }) { folderId ->
            vm.moveSelectedToFolder(folderId); showMove = false
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun FileRow(item: VaultItem, selected: Boolean, selecting: Boolean,
                    onClick: () -> Unit, onLong: () -> Unit) {
    SectionCard(Modifier.combinedClickable(onClick = onClick, onLongClick = onLong)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (selecting && selected) Icons.Default.CheckCircle else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (selecting && selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.displayName, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text("${fmtBytes(item.sizeBytes)} · ${item.mimeType} · ${fmtDate(item.addedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

internal fun fmtBytes(b: Long): String {
    if (b < 1024) return "$b B"
    val kb = b / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
internal fun fmtDate(ts: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ts))
