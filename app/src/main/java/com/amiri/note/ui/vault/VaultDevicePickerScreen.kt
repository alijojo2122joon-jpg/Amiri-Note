package com.amiri.note.ui.vault

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.amiri.note.data.entity.VaultKind
import com.amiri.note.ui.common.EmptyHint
import com.amiri.note.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DeviceMedia(val id: Long, val uri: Uri, val isVideo: Boolean)

private fun mainPermission(video: Boolean): String = when {
    Build.VERSION.SDK_INT >= 33 ->
        if (video) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_MEDIA_IMAGES
    else -> Manifest.permission.READ_EXTERNAL_STORAGE
}

private fun requestList(video: Boolean): Array<String> =
    if (Build.VERSION.SDK_INT >= 34)
        arrayOf(mainPermission(video), "android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
    else arrayOf(mainPermission(video))

private fun granted(ctx: Context, p: String) =
    ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

private fun hasAccess(ctx: Context, video: Boolean): Boolean =
    granted(ctx, mainPermission(video)) ||
        (Build.VERSION.SDK_INT >= 34 && granted(ctx, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"))

private fun hasFullAccess(ctx: Context, video: Boolean) = granted(ctx, mainPermission(video))

private fun queryDeviceMedia(ctx: Context, video: Boolean): List<DeviceMedia> {
    val collection = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val out = ArrayList<DeviceMedia>()
    try {
        ctx.contentResolver.query(
            collection, arrayOf(MediaStore.MediaColumns._ID), null, null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { c ->
            val idx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (c.moveToNext() && out.size < 6000) {
                val id = c.getLong(idx)
                out += DeviceMedia(id, ContentUris.withAppendedId(collection, id), video)
            }
        }
    } catch (_: Throwable) { }
    return out
}

@Composable
private fun DeviceThumb(m: DeviceMedia, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var bmp by remember(m.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(m.id) {
        bmp = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= 29)
                    ctx.contentResolver.loadThumbnail(m.uri, Size(256, 256), null).asImageBitmap()
                else if (!m.isVideo)
                    ImageUtil.decodeSampled(256) { ctx.contentResolver.openInputStream(m.uri) }?.asImageBitmap()
                else null
            } catch (_: Throwable) { null }
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (m.isVideo) Icon(Icons.Default.PlayCircle, null, tint = Color.White)
        } else {
            Icon(if (m.isVideo) Icons.Default.Videocam else Icons.Default.Photo, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * In-app chooser of the phone's own photos/videos. "Hide" encrypts the chosen
 * items into the vault and then asks Android (one system confirmation) to delete
 * the originals, so they disappear from the gallery.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultDevicePickerScreen(kind: Int, vm: VaultViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val video = kind == VaultKind.VIDEO
    val busy by vm.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    VaultToastEffect(vm, snackbar)

    var access by remember { mutableStateOf(hasAccess(context, video)) }
    var media by remember { mutableStateOf<List<DeviceMedia>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var message by remember { mutableStateOf<String?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.externalActive = false
        access = hasAccess(context, video)
        reload++
    }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        vm.externalActive = false
        message = if (res.resultCode == android.app.Activity.RESULT_OK)
            "Done — the originals were removed from your gallery."
        else "Added to the vault, but you chose to keep the originals in the gallery."
        selected = emptySet(); reload++
    }

    LaunchedEffect(access, reload) {
        if (access) {
            loading = true
            media = withContext(Dispatchers.IO) { queryDeviceMedia(context, video) }
            loading = false
        } else loading = false
    }
    LaunchedEffect(Unit) {
        if (!access) {
            vm.externalActive = true
            try { permLauncher.launch(requestList(video)) } catch (_: Throwable) { vm.externalActive = false }
        }
    }

    fun hideSelected() {
        val chosen = media.filter { it.id in selected }
        if (chosen.isEmpty()) return
        vm.importUris(chosen.map { it.uri }, folderId = 0, deleteSource = false) { done ->
            if (done.isEmpty()) return@importUris
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    val sender = MediaStore.createDeleteRequest(context.contentResolver, done).intentSender
                    vm.externalActive = true
                    deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                } catch (t: Throwable) {
                    message = "Added to the vault, but Android would not let me remove the originals: ${t.message}"
                    vm.externalActive = false
                }
            } else {
                var gone = 0
                for (u in done) { try { if (context.contentResolver.delete(u, null, null) > 0) gone++ } catch (_: Throwable) { } }
                message = "Added to the vault; $gone original(s) removed."
                selected = emptySet(); reload++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selected.isEmpty()) (if (video) "Choose videos" else "Choose photos")
                              else "${selected.size} selected") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (media.isNotEmpty()) {
                        IconButton(onClick = {
                            selected = if (selected.size == media.size) emptySet() else media.map { it.id }.toSet()
                        }) { Icon(Icons.Default.SelectAll, "Select all") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selected.isNotEmpty() && !busy) {
                ExtendedFloatingActionButton(
                    onClick = { hideSelected() },
                    icon = { Icon(Icons.Default.Lock, null) },
                    text = { Text("Hide ${selected.size}") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            if (access && Build.VERSION.SDK_INT >= 34 && !hasFullAccess(context, video)) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("You allowed only some photos.", style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"))
                        )
                    }) { Text("Allow all") }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    !access -> Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("To move photos and videos into the vault, allow access to your media.",
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            vm.externalActive = true
                            try { permLauncher.launch(requestList(video)) } catch (_: Throwable) { vm.externalActive = false }
                        }) { Text("Allow access") }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}"))
                            )
                        }) { Text("Open app settings") }
                    }
                    loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    media.isEmpty() -> EmptyHint(if (video) "No videos found on this phone." else "No photos found on this phone.")
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(media, key = { it.id }) { m ->
                            val on = m.id in selected
                            Box(
                                Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                    .combinedClickable(
                                        onClick = { selected = if (on) selected - m.id else selected + m.id },
                                        onLongClick = { selected = selected + m.id }
                                    )
                            ) {
                                DeviceThumb(m, Modifier.fillMaxSize())
                                if (on) {
                                    Box(Modifier.fillMaxSize().background(Color(0x663A6BFF)))
                                }
                                Icon(
                                    if (on) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    null,
                                    tint = if (on) Color.White else Color(0xCCFFFFFF),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                                )
                            }
                        }
                    }
                }
                if (busy) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}
