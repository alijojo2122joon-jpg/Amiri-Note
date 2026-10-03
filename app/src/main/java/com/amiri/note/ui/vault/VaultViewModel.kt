package com.amiri.note.ui.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiri.note.data.dao.VaultDao
import com.amiri.note.data.entity.VaultFolder
import com.amiri.note.data.entity.VaultItem
import com.amiri.note.data.entity.VaultKind
import com.amiri.note.security.SecurePrefs
import com.amiri.note.security.VaultManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Shared state for the whole vault session (unlock status + selection). */
class VaultViewModel(
    private val vault: VaultManager,
    private val dao: VaultDao,
    private val prefs: SecurePrefs
) : ViewModel() {

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun clearToast() { _toast.value = null }

    val folders: StateFlow<List<VaultFolder>> =
        dao.observeFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photos: StateFlow<List<VaultItem>> =
        dao.observeByKind(VaultKind.PHOTO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val videos: StateFlow<List<VaultItem>> =
        dao.observeByKind(VaultKind.VIDEO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val files: StateFlow<List<VaultItem>> =
        dao.observeByKind(VaultKind.FILE).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trash: StateFlow<List<VaultItem>> =
        dao.observeTrash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Unlock ----
    fun hasPin(): Boolean = prefs.hasPin()
    fun biometricEnabled(): Boolean = prefs.biometricEnabled
    fun verifyPin(pin: String): Boolean = prefs.verifyPin(pin)
    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        prefs.setPin(pin); prefs.vaultInitialized = true
        return true
    }
    fun unlock() { _unlocked.value = true }
    fun lock() { _unlocked.value = false; clearSelection() }

    /** True while a system picker / save dialog is open, so auto-lock doesn't fire. */
    @Volatile var externalActive: Boolean = false

    // ---- Private app list ----
    private val _apps = MutableStateFlow(prefs.hiddenApps)
    val hiddenApps: StateFlow<Set<String>> = _apps.asStateFlow()
    fun setHiddenApps(pkgs: Set<String>) { prefs.hiddenApps = pkgs; _apps.value = pkgs }
    fun removeHiddenApp(pkg: String) = setHiddenApps(_apps.value - pkg)

    // ---- Selection ----
    fun toggleSelect(id: Long) {
        _selection.value = _selection.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }
    fun selectAll(ids: List<Long>) { _selection.value = ids.toSet() }
    fun clearSelection() { _selection.value = emptySet() }
    val selectionActive: Boolean get() = _selection.value.isNotEmpty()

    // ---- Import ----
    /**
     * Encrypts [uris] into the vault. [onDone] receives the Uris that were
     * imported successfully (so the caller can then remove those originals).
     */
    fun importUris(
        uris: List<Uri>, folderId: Long, deleteSource: Boolean,
        onDone: (List<Uri>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _busy.value = true
            var ok = 0; var fail = 0; var removed = 0; var lastError = ""
            val imported = mutableListOf<Uri>()
            for (u in uris) {
                try {
                    val r = vault.importUri(u, folderId, deleteSource)
                    ok++; imported += u; if (r.sourceDeleted) removed++
                } catch (t: Throwable) { fail++; lastError = t.message ?: t.javaClass.simpleName }
            }
            _busy.value = false
            _toast.value = when {
                ok == 0 -> "Could not add: $lastError"
                fail > 0 -> "Added $ok, $fail failed ($lastError)"
                deleteSource && removed == ok -> "Hidden $ok item(s); originals removed"
                deleteSource -> "Added $ok; originals could not be removed ($removed removed)"
                else -> "Added $ok item(s) to the vault"
            }
            onDone(imported)
        }
    }

    // ---- Operations ----
    fun moveSelectedToTrash() {
        val ids = _selection.value.toList(); if (ids.isEmpty()) return
        viewModelScope.launch {
            dao.moveToTrash(ids, System.currentTimeMillis())
            clearSelection(); _toast.value = "Moved to Trash"
        }
    }
    fun restoreSelectedFromTrash() {
        val ids = _selection.value.toList(); if (ids.isEmpty()) return
        viewModelScope.launch { dao.restoreFromTrash(ids); clearSelection(); _toast.value = "Restored" }
    }
    fun deleteSelectedPermanently() {
        val ids = _selection.value.toList(); if (ids.isEmpty()) return
        viewModelScope.launch { vault.permanentlyDelete(ids); clearSelection(); _toast.value = "Deleted permanently" }
    }
    fun emptyTrash() {
        viewModelScope.launch {
            val ids = trash.value.map { it.id }
            vault.permanentlyDelete(ids); _toast.value = "Trash emptied"
        }
    }
    fun moveSelectedToFolder(folderId: Long) {
        val ids = _selection.value.toList(); if (ids.isEmpty()) return
        viewModelScope.launch { dao.moveToFolder(ids, folderId); clearSelection(); _toast.value = "Moved" }
    }
    fun renameItem(id: Long, newName: String) {
        viewModelScope.launch {
            dao.getItem(id)?.let { dao.updateItem(it.copy(displayName = newName)) }
        }
    }

    // ---- Folders ----
    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { dao.insertFolder(VaultFolder(name = name.trim())) }
    }
    fun renameFolder(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { dao.getAllFolders().find { it.id == id }?.let { dao.updateFolder(it.copy(name = name.trim())) } }
    }
    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            // move its items back to root first
            val items = dao.getAllItems().filter { it.folderId == id }.map { it.id }
            if (items.isNotEmpty()) dao.moveToFolder(items, 0)
            dao.deleteFolder(id)
        }
    }

    fun itemsInFolder(folderId: Long): Flow<List<VaultItem>> = dao.observeInFolder(folderId)

    // ---- Restore to device ----
    suspend fun decryptToTemp(item: VaultItem) = vault.decryptToTemp(item)
    suspend fun loadThumb(item: VaultItem) = vault.loadThumbBytes(item)
    fun restoreToUri(item: VaultItem, dest: Uri, removeAfter: Boolean) {
        viewModelScope.launch {
            try {
                vault.restoreToUri(item, dest, removeAfter)
                _toast.value = "Restored to device"
            } catch (t: Throwable) { _toast.value = "Restore failed: ${t.message}" }
        }
    }

    suspend fun getItem(id: Long): VaultItem? = dao.getItem(id)
}
