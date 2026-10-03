package com.amiri.note.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amiri.note.AmiriApp
import com.amiri.note.ui.home.HomeViewModel
import com.amiri.note.ui.notes.NotesViewModel
import com.amiri.note.ui.notes.NoteEditorViewModel
import com.amiri.note.ui.calendar.CalendarViewModel
import com.amiri.note.ui.stats.StatsViewModel
import com.amiri.note.ui.settings.SettingsViewModel
import com.amiri.note.ui.vault.VaultViewModel

/** Builds ViewModels with the app singletons injected. */
class VMFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val a = AmiriApp.from(app)
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(a.repository) as T
            modelClass.isAssignableFrom(NotesViewModel::class.java) ->
                NotesViewModel(a.repository) as T
            modelClass.isAssignableFrom(NoteEditorViewModel::class.java) ->
                NoteEditorViewModel(a.repository, a.securePrefs) as T
            modelClass.isAssignableFrom(CalendarViewModel::class.java) ->
                CalendarViewModel(a.repository, a.securePrefs) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(a.repository, a.securePrefs) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(a.repository, a.securePrefs, a.vaultManager, a.database) as T
            modelClass.isAssignableFrom(VaultViewModel::class.java) ->
                VaultViewModel(a.vaultManager, a.database.vaultDao(), a.securePrefs) as T
            else -> throw IllegalArgumentException("Unknown VM: ${modelClass.name}")
        }
    }
}
