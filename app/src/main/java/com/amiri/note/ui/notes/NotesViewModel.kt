package com.amiri.note.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiri.note.data.entity.Note
import com.amiri.note.data.repo.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotesState(
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val showArchived: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repo: AppRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val showArchived = MutableStateFlow(false)

    val state: StateFlow<NotesState> =
        combine(query, showArchived) { q, arch -> q to arch }
            .flatMapLatest { (q, arch) ->
                val source = when {
                    q.isNotBlank() -> repo.searchNotes(q)
                    arch -> repo.archivedNotes()
                    else -> repo.activeNotes()
                }
                source.map { NotesState(it, q, arch) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesState())

    fun setQuery(q: String) { query.value = q }
    fun toggleArchived() { showArchived.value = !showArchived.value }

    fun togglePin(note: Note) = viewModelScope.launch {
        repo.saveNote(note.copy(isPinned = !note.isPinned))
    }
    fun toggleArchive(note: Note) = viewModelScope.launch {
        repo.saveNote(note.copy(isArchived = !note.isArchived))
    }
    fun delete(note: Note) = viewModelScope.launch { repo.deleteNote(note.id) }
    fun duplicate(note: Note) = viewModelScope.launch { repo.duplicateNote(note.id) }
}
