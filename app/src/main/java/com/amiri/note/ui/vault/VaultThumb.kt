package com.amiri.note.ui.vault

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import com.amiri.note.data.entity.VaultItem
import com.amiri.note.data.entity.VaultKind

/** Decrypts and shows an item's thumbnail; falls back to an icon. */
@Composable
fun VaultThumb(item: VaultItem, vm: VaultViewModel, modifier: Modifier = Modifier) {
    var bitmap by remember(item.id) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(item.id, item.thumbPath) {
        val bytes = vm.loadThumb(item)
        if (bytes != null) {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap = bmp?.asImageBitmap()
        }
    }

    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bm = bitmap
        if (bm != null) {
            Image(bitmap = bm, contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (item.kind == VaultKind.VIDEO) {
                Icon(Icons.Default.PlayCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            Icon(
                when (item.kind) {
                    VaultKind.VIDEO -> Icons.Default.PlayCircle
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
