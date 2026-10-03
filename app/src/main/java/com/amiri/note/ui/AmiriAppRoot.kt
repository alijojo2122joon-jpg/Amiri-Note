package com.amiri.note.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.amiri.note.ui.calendar.CalendarScreen
import com.amiri.note.ui.home.HomeScreen
import com.amiri.note.ui.home.LocalContextApp
import com.amiri.note.ui.notes.NoteEditorScreen
import com.amiri.note.ui.notes.NotesScreen
import com.amiri.note.ui.settings.SettingsScreen
import com.amiri.note.ui.stats.StatsScreen
import com.amiri.note.ui.vault.*

private data class NavItem(val route: String, val label: String, val sel: ImageVector, val unsel: ImageVector)

private val bottomItems = listOf(
    NavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(Routes.NOTES, "Notes", Icons.Filled.Description, Icons.Outlined.Description),
    NavItem(Routes.CALENDAR, "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    NavItem(Routes.STATS, "Statistics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    NavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

// The bottom bar shows only on the five top-level routes; every other route
// (note editor + the whole vault flow) is full-screen.

@Composable
fun AmiriAppRoot() {
    val navController = rememberNavController()
    // One VaultViewModel shared across the whole vault flow.
    val vaultVm: VaultViewModel = viewModel(factory = VMFactory(LocalContextApp()))

    // Auto-lock: leaving the app for more than 30 s while the vault is open locks it.
    // (System pickers set vaultVm.externalActive so adding files never triggers this.)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var stoppedAt = 0L
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP ->
                    stoppedAt = if (vaultVm.externalActive) 0L else SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> {
                    if (stoppedAt > 0L && vaultVm.unlocked.value &&
                        SystemClock.elapsedRealtime() - stoppedAt > 30_000L
                    ) {
                        vaultVm.lock()
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    }
                    stoppedAt = 0L
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = bottomItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color(0x24FFFFFF),
                    tonalElevation = 0.dp
                ) {
                    bottomItems.forEach { item ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) item.sel else item.unsel, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(pad).consumeWindowInsets(pad)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenNote = { id -> navController.navigate("${Routes.NOTE_EDITOR}?id=$id") },
                    onSeeAllNotes = { navController.navigate(Routes.NOTES) { launchSingleTop = true } }
                )
            }
            composable(Routes.NOTES) {
                NotesScreen(
                    onOpenNote = { id -> navController.navigate("${Routes.NOTE_EDITOR}?id=$id") },
                    onNewNote = { navController.navigate("${Routes.NOTE_EDITOR}?id=0") }
                )
            }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(
                route = "${Routes.NOTE_EDITOR}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                NoteEditorScreen(
                    noteId = id,
                    onBack = { navController.popBackStack() },
                    onSecretTriggered = {
                        // Secret entrance: go to the vault gate. Reset unlock state.
                        vaultVm.lock()
                        navController.navigate(Routes.VAULT_UNLOCK)
                    }
                )
            }

            // ---- Vault flow (shared vaultVm) ----
            composable(Routes.VAULT_UNLOCK) {
                VaultUnlockScreen(
                    vm = vaultVm,
                    onUnlocked = {
                        navController.navigate(Routes.VAULT_HOME) {
                            popUpTo(Routes.VAULT_UNLOCK) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.VAULT_HOME) {
                // Guard: if somehow reached while locked, bounce back.
                LaunchedEffect(Unit) { if (!vaultVm.unlocked.value) navController.popBackStack() }
                VaultHomeScreen(
                    vm = vaultVm,
                    onOpenGallery = { kind -> navController.navigate("${Routes.VAULT_GALLERY}/$kind") },
                    onOpenFiles = { navController.navigate(Routes.VAULT_FILES) },
                    onOpenFolders = { navController.navigate(Routes.VAULT_FOLDERS) },
                    onOpenApps = { navController.navigate(Routes.VAULT_APPS) },
                    onOpenTrash = { navController.navigate(Routes.VAULT_TRASH) },
                    onLock = {
                        vaultVm.lock()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "${Routes.VAULT_GALLERY}/{kind}",
                arguments = listOf(navArgument("kind") { type = NavType.IntType })
            ) { entry ->
                val kind = entry.arguments?.getInt("kind") ?: 0
                VaultGalleryScreen(
                    kind = kind, vm = vaultVm,
                    onBack = { vaultVm.clearSelection(); navController.popBackStack() },
                    onOpenItem = { id -> navController.navigate("${Routes.VAULT_VIEWER}/$id") },
                    onAddFromDevice = { navController.navigate("${Routes.VAULT_PICK}/$kind") }
                )
            }
            composable(
                route = "${Routes.VAULT_PICK}/{kind}",
                arguments = listOf(navArgument("kind") { type = NavType.IntType })
            ) { entry ->
                val kind = entry.arguments?.getInt("kind") ?: 0
                VaultDevicePickerScreen(kind = kind, vm = vaultVm, onBack = { navController.popBackStack() })
            }
            composable(Routes.VAULT_FILES) {
                VaultFilesScreen(vm = vaultVm,
                    onBack = { vaultVm.clearSelection(); navController.popBackStack() },
                    onOpenItem = { id -> navController.navigate("${Routes.VAULT_VIEWER}/$id") })
            }
            composable(Routes.VAULT_FOLDERS) {
                VaultFoldersScreen(vm = vaultVm, onBack = { navController.popBackStack() },
                    onOpenFolder = { id -> navController.navigate("${Routes.VAULT_FOLDER}/$id") })
            }
            composable(
                route = "${Routes.VAULT_FOLDER}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                VaultFolderDetailScreen(folderId = id, vm = vaultVm,
                    onBack = { vaultVm.clearSelection(); navController.popBackStack() },
                    onOpenItem = { itemId -> navController.navigate("${Routes.VAULT_VIEWER}/$itemId") })
            }
            composable(Routes.VAULT_TRASH) {
                VaultTrashScreen(vm = vaultVm,
                    onBack = { vaultVm.clearSelection(); navController.popBackStack() })
            }
            composable(Routes.VAULT_APPS) {
                VaultAppsScreen(vm = vaultVm, onBack = { navController.popBackStack() })
            }
            composable(
                route = "${Routes.VAULT_VIEWER}/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                VaultViewerScreen(itemId = id, vm = vaultVm, onBack = { navController.popBackStack() })
            }
        }
    }
}
