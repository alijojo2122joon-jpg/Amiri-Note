package com.amiri.note.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import com.amiri.note.util.CrashLogger
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amiri.note.ui.VMFactory
import com.amiri.note.ui.common.SectionCard
import com.amiri.note.ui.common.SectionHeader
import com.amiri.note.ui.home.LocalContextApp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel(factory = VMFactory(LocalContextApp()))
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var showPinDialog by remember { mutableStateOf(false) }
    var showPhraseDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var crashText by remember { mutableStateOf(CrashLogger.read(context)) }
    var showCrash by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    // Export backup via SAF create-document
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val json = vm.backupManager.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                snackbar.showSnackbar("Backup saved.")
            } catch (e: Exception) {
                snackbar.showSnackbar("Backup failed: ${e.message}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    val n = vm.backupManager.importJson(json)
                    vm.refreshStorage()
                    snackbar.showSnackbar("Restored $n items.")
                }
            } catch (e: Exception) {
                snackbar.showSnackbar("Restore failed: ${e.message}")
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

            item {
                SectionCard {
                    SectionHeader("Security")
                    ToggleRow("App Lock", "Require PIN / fingerprint to open the app",
                        state.appLockEnabled) { vm.setAppLock(it) }
                    Divider(Modifier.padding(vertical = 8.dp))
                    ToggleRow("Biometric unlock", "Use fingerprint where available",
                        state.biometricEnabled) { vm.setBiometric(it) }
                    Divider(Modifier.padding(vertical = 8.dp))
                    ClickRow(if (state.hasPin) "Change PIN" else "Set PIN") { showPinDialog = true }
                    Divider(Modifier.padding(vertical = 8.dp))
                    ClickRow("Change secret phrase") { showPhraseDialog = true }
                }
            }

            item {
                SectionCard {
                    SectionHeader("Finance")
                    ClickRow("Default currency: ${state.defaultCurrency}") { showCurrencyDialog = true }
                }
            }

            item {
                SectionCard {
                    SectionHeader("Backup (offline)")
                    ClickRow("Export backup to file") {
                        exportLauncher.launch("AmiriNote-backup-${System.currentTimeMillis()}.json")
                    }
                    Divider(Modifier.padding(vertical = 8.dp))
                    ClickRow("Import backup from file") {
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Vault files are stored encrypted and are not included in this JSON backup.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                SectionCard {
                    SectionHeader("Storage")
                    StorageRow("Notes & data (database)", fmtBytes(state.storage.dbBytes))
                    StorageRow("Vault (encrypted)", fmtBytes(state.storage.vaultBytes))
                    StorageRow("Notes count", state.storage.notesCount.toString())
                    Divider(Modifier.padding(vertical = 8.dp))
                    StorageRow("Total on disk",
                        fmtBytes(state.storage.dbBytes + state.storage.vaultBytes))
                }
            }

            item {
                SectionCard {
                    SectionHeader("Diagnostics")
                    if (crashText != null) {
                        ClickRow("View last error report") { showCrash = true }
                    } else {
                        Text("No errors recorded. If the app ever closes unexpectedly, the details appear here.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                SectionCard {
                    SectionHeader("About")
                    Text("Amiri Note", style = MaterialTheme.typography.titleMedium)
                    Text("Version 1.1 · 100% offline · no internet permission",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showCrash && crashText != null) {
        AlertDialog(
            onDismissRequest = { showCrash = false },
            title = { Text("Last error report") },
            text = {
                Box(Modifier.heightIn(max = 380.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        item {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(crashText!!, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(crashText!!))
                }) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = {
                    CrashLogger.clear(context); crashText = null; showCrash = false
                }) { Text("Clear & close") }
            }
        )
    }

    if (showPinDialog) {
        PinChangeDialog(
            needsOld = state.hasPin,
            onDismiss = { showPinDialog = false },
            onConfirm = { old, new -> if (vm.changePin(old, new)) showPinDialog = false }
        )
    }
    if (showPhraseDialog) {
        TextInputDialog(
            title = "Change secret phrase",
            label = "New secret phrase",
            onDismiss = { showPhraseDialog = false },
            onConfirm = { if (vm.changeSecretPhrase(it)) showPhraseDialog = false }
        )
    }
    if (showCurrencyDialog) {
        TextInputDialog(
            title = "Default currency",
            label = "Currency code (e.g. AFN, USD, EUR)",
            initial = state.defaultCurrency,
            onDismiss = { showCurrencyDialog = false },
            onConfirm = { vm.setDefaultCurrency(it.uppercase()); showCurrencyDialog = false }
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ClickRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickableRow(onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable { onClick() }

@Composable
private fun StorageRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PinChangeDialog(needsOld: Boolean, onDismiss: () -> Unit, onConfirm: (String?, String) -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (needsOld) "Change PIN" else "Set PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needsOld) {
                    OutlinedTextField(value = old, onValueChange = { old = it.filter(Char::isDigit) },
                        label = { Text("Current PIN") }, singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                }
                OutlinedTextField(value = new, onValueChange = { new = it.filter(Char::isDigit) },
                    label = { Text("New PIN (min 4 digits)") }, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(if (needsOld) old else null, new) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TextInputDialog(
    title: String, label: String, initial: String = "",
    onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it },
                label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun fmtBytes(b: Long): String {
    if (b < 1024) return "$b B"
    val kb = b / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
