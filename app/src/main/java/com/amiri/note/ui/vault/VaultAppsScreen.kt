package com.amiri.note.ui.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.amiri.note.ui.common.EmptyHint
import com.amiri.note.ui.common.SectionCard
import com.amiri.note.ui.common.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LaunchableApp(val label: String, val packageName: String, val icon: ImageBitmap?)

/**
 * Private app list.
 *
 * Android does not allow a normal app to hide other apps from the launcher (no
 * public API exists for that). So this screen does what Android officially
 * supports: you ADD apps to a private, PIN-protected list inside the vault and
 * open them from here. To really hide an app from the home screen, use your
 * phone's own feature (on realme: Settings → Privacy → App Hiding / Private Safe).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultAppsScreen(vm: VaultViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val saved by vm.hiddenApps.collectAsState()
    var allApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showPicker by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<LaunchableApp?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        allApps = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
        loading = false
    }

    // Only show apps that are still installed.
    val mine = remember(allApps, saved) { allApps.filter { it.packageName in saved } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apps") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showPicker = true }) { Icon(Icons.Default.Add, "Add apps") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add") }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SectionCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Add the apps you want to keep private and open them from here, behind your PIN. " +
                            "Android doesn't allow any app to remove other apps from the home screen — " +
                            "for that, use your phone's own App Hiding feature in Settings → Privacy.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                mine.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyHint("No apps added yet.\nTap Add to choose apps for your private list.")
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(88.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(mine, key = { it.packageName }) { app ->
                        Column(
                            Modifier
                                .glass(RoundedCornerShape(20.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (!launchApp(context, app.packageName))
                                            menuFor = app
                                    },
                                    onLongClick = { menuFor = app }
                                )
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppIcon(app, 48)
                            Spacer(Modifier.height(6.dp))
                            Text(app.label, style = MaterialTheme.typography.labelMedium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center)
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Long-press menu
    menuFor?.let { app ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(app.label) },
            text = {
                Column {
                    TextButton(onClick = {
                        if (!launchApp(context, app.packageName)) { /* ignored; no launch intent */ }
                        menuFor = null
                    }) { Text("Open") }
                    TextButton(onClick = { openAppInfo(context, app.packageName); menuFor = null }) {
                        Text("App info (disable / uninstall)")
                    }
                    TextButton(onClick = { vm.removeHiddenApp(app.packageName); menuFor = null }) {
                        Text("Remove from private list")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { menuFor = null }) { Text("Close") } }
        )
    }

    if (showPicker) {
        AppPickerSheet(
            apps = allApps,
            initiallySelected = saved,
            onDismiss = { showPicker = false },
            onSave = { vm.setHiddenApps(it); showPicker = false }
        )
    }
}

@Composable
private fun AppIcon(app: LaunchableApp, sizeDp: Int) {
    val icon = app.icon
    if (icon != null) {
        Image(bitmap = icon, contentDescription = app.label,
            modifier = Modifier.size(sizeDp.dp).clip(RoundedCornerShape(12.dp)))
    } else {
        Icon(Icons.Default.Android, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    apps: List<LaunchableApp>,
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(initiallySelected) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.fillMaxHeight(0.92f).padding(horizontal = 16.dp)) {
            Text("Add apps", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search apps…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (apps.isEmpty()) {
                EmptyHint("No launchable apps found.")
            }
            LazyColumn(Modifier.weight(1f)) {
                items(filtered, key = { it.packageName }) { app ->
                    val on = app.packageName in selected
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                selected = if (on) selected - app.packageName else selected + app.packageName
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(app, 40)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(app.packageName, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Checkbox(checked = on, onCheckedChange = {
                            selected = if (on) selected - app.packageName else selected + app.packageName
                        })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(selected) }) { Text("Save (${selected.size})") }
            }
        }
    }
}

private fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    return pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
        val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
        if (pkg == context.packageName) return@mapNotNull null
        val label = try { ri.loadLabel(pm)?.toString() } catch (_: Throwable) { null } ?: pkg
        val icon = try { ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap() } catch (_: Throwable) { null }
        LaunchableApp(label, pkg, icon)
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

private fun launchApp(context: Context, packageName: String): Boolean {
    return try {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        true
    } catch (_: Throwable) { false }
}

private fun openAppInfo(context: Context, packageName: String) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Throwable) { }
}
