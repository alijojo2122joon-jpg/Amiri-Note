package com.amiri.note.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiri.note.backup.BackupManager
import com.amiri.note.data.db.AppDatabase
import com.amiri.note.data.repo.AppRepository
import com.amiri.note.security.SecurePrefs
import com.amiri.note.security.VaultManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StorageInfo(
    val dbBytes: Long = 0,
    val vaultBytes: Long = 0,
    val notesCount: Int = 0
)

data class SettingsState(
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = true,
    val hasPin: Boolean = false,
    val defaultCurrency: String = "AFN",
    val storage: StorageInfo = StorageInfo(),
    val message: String? = null
)

class SettingsViewModel(
    private val repo: AppRepository,
    private val prefs: SecurePrefs,
    private val vault: VaultManager,
    private val db: AppDatabase
) : ViewModel() {

    private val backup = BackupManager(db, prefs)

    private val _state = MutableStateFlow(
        SettingsState(
            appLockEnabled = prefs.appLockEnabled,
            biometricEnabled = prefs.biometricEnabled,
            hasPin = prefs.hasPin(),
            defaultCurrency = prefs.defaultCurrency
        )
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init { refreshStorage() }

    fun refreshStorage() {
        viewModelScope.launch {
            val vaultBytes = vault.vaultBytesOnDisk()
            val notes = db.noteDao().count()
            _state.value = _state.value.copy(
                storage = StorageInfo(
                    dbBytes = backup.databaseFileBytes(),
                    vaultBytes = vaultBytes,
                    notesCount = notes
                )
            )
        }
    }

    fun setAppLock(enabled: Boolean) {
        if (enabled && !prefs.hasPin()) {
            _state.value = _state.value.copy(message = "Set a PIN first to enable App Lock.")
            return
        }
        prefs.appLockEnabled = enabled
        _state.value = _state.value.copy(appLockEnabled = enabled)
    }

    fun setBiometric(enabled: Boolean) {
        prefs.biometricEnabled = enabled
        _state.value = _state.value.copy(biometricEnabled = enabled)
    }

    fun setDefaultCurrency(c: String) {
        prefs.defaultCurrency = c.ifBlank { "AFN" }
        _state.value = _state.value.copy(defaultCurrency = prefs.defaultCurrency)
    }

    /** Set or change the PIN. If a PIN exists, [old] must verify. */
    fun changePin(old: String?, new: String): Boolean {
        if (prefs.hasPin()) {
            if (old == null || !prefs.verifyPin(old)) {
                _state.value = _state.value.copy(message = "Current PIN is incorrect.")
                return false
            }
        }
        if (new.length < 4) {
            _state.value = _state.value.copy(message = "PIN must be at least 4 digits.")
            return false
        }
        prefs.setPin(new)
        _state.value = _state.value.copy(hasPin = true, message = "PIN updated.")
        return true
    }

    fun changeSecretPhrase(newPhrase: String): Boolean {
        if (newPhrase.isBlank()) {
            _state.value = _state.value.copy(message = "Phrase cannot be empty.")
            return false
        }
        prefs.setPhrase(newPhrase)
        _state.value = _state.value.copy(message = "Secret phrase updated.")
        return true
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    // Backup delegates — the actual file I/O is driven from the screen via SAF.
    val backupManager: BackupManager get() = backup
}
