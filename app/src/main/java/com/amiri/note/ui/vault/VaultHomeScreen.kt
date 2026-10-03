package com.amiri.note.ui.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amiri.note.ui.common.EmptyHint
import com.amiri.note.ui.common.glass

/** Vault landing page: the private menu. No "Vault" label exists in the main app nav. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    vm: VaultViewModel,
    onOpenGallery: (kind: Int) -> Unit,
    onOpenFiles: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenTrash: () -> Unit,
    onLock: () -> Unit
) {
    val photos by vm.photos.collectAsState()
    val videos by vm.videos.collectAsState()
    val files by vm.files.collectAsState()
    val trash by vm.trash.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock vault")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                item { VaultTile("Photos", Icons.Default.Photo, "${photos.size} items") { onOpenGallery(0) } }
                item { VaultTile("Videos", Icons.Default.Videocam, "${videos.size} items") { onOpenGallery(1) } }
                item { VaultTile("Files", Icons.Default.Folder, "${files.size} items") { onOpenFiles() } }
                item { VaultTile("Folders", Icons.Default.CreateNewFolder, "Organize") { onOpenFolders() } }
                item { VaultTile("Apps", Icons.Default.Apps, "Manage") { onOpenApps() } }
                item { VaultTile("Trash", Icons.Default.Delete, "${trash.size} items") { onOpenTrash() } }
            }
        }
    }
}

@Composable
private fun VaultTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                      subtitle: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f)
            .glass(RoundedCornerShape(24.dp), strong = true)
            .clickable { onClick() }
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
