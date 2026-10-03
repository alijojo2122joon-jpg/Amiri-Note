package com.amiri.note.ui.vault

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.amiri.note.data.entity.VaultItem
import com.amiri.note.data.entity.VaultKind
import com.amiri.note.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
@Composable
fun VaultViewerScreen(itemId: Long, vm: VaultViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var item by remember { mutableStateOf<VaultItem?>(null) }
    var tempFile by remember { mutableStateOf<File?>(null) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showRename by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    VaultToastEffect(vm, snackbar)

    LaunchedEffect(itemId) {
        loading = true
        try {
            val it = vm.getItem(itemId)
            item = it
            if (it != null) {
                val f = vm.decryptToTemp(it)
                tempFile = f
                if (it.kind == VaultKind.PHOTO) {
                    bitmap = withContext(Dispatchers.IO) {
                        ImageUtil.decodeSampled(2400) { f.inputStream() }?.asImageBitmap()
                    }
                    if (bitmap == null) error = "This image could not be displayed."
                }
            } else error = "Item not found."
        } catch (t: Throwable) {
            error = "Could not open: ${t.message ?: t.javaClass.simpleName}"
        }
        loading = false
    }

    // Never leave decrypted plaintext behind.
    DisposableEffect(tempFile) {
        val f = tempFile
        onDispose { f?.let { runCatching { it.delete() } } }
    }

    // Restore (export a decrypted copy to a user-chosen location) via SAF.
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(item?.mimeType ?: "*/*")
    ) { uri: Uri? ->
        vm.externalActive = false
        val it = item
        if (uri != null && it != null) vm.restoreToUri(it, uri, removeAfter = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.displayName ?: "", maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showRename = true }, enabled = item != null) {
                        Icon(Icons.Default.Edit, "Rename")
                    }
                    IconButton(onClick = {
                        vm.externalActive = true
                        try { restoreLauncher.launch(item?.displayName ?: "restored") }
                        catch (t: Throwable) { vm.externalActive = false }
                    }, enabled = item != null) { Icon(Icons.Default.Download, "Restore to device") }
                    IconButton(onClick = {
                        item?.let { vm.toggleSelect(it.id); vm.moveSelectedToTrash(); onBack() }
                    }, enabled = item != null) { Icon(Icons.Default.Delete, "Move to trash") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
            val it = item
            when {
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                loading || it == null -> CircularProgressIndicator()
                it.kind == VaultKind.PHOTO && bitmap != null -> ZoomableImage(bitmap!!, it.displayName)
                it.kind == VaultKind.VIDEO && tempFile != null -> VideoPlayer(tempFile!!)
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.InsertDriveFile, null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(it.displayName, textAlign = TextAlign.Center)
                        Text("${fmtBytes(it.sizeBytes)} · ${it.mimeType}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = {
                            val f = tempFile ?: return@Button
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context, context.packageName + ".fileprovider", f)
                                val view = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, it.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(view, "Open with"))
                            } catch (e: ActivityNotFoundException) {
                                error = "No app on this phone can open this file type."
                            } catch (t: Throwable) {
                                error = "Could not open: ${t.message}"
                            }
                        }) { Text("Open with…") }
                        Spacer(Modifier.height(8.dp))
                        Text("Or use the download icon to save a copy to your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    if (showRename) {
        var name by remember { mutableStateOf(item?.displayName ?: "") }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        item?.let { vm.renameItem(it.id, name.trim()); item = it.copy(displayName = name.trim()) }
                    }
                    showRename = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ZoomableImage(bitmap: ImageBitmap, description: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    Image(
        bitmap = bitmap,
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                    else { offsetX += pan.x; offsetY += pan.y }
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
    )
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPlayer(file: File) {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
        modifier = Modifier.fillMaxSize()
    )
}
