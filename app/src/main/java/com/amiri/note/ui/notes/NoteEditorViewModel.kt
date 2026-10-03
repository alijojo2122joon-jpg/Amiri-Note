package com.amiri.note.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiri.note.data.entity.Note
import com.amiri.note.data.repo.AppRepository
import com.amiri.note.security.SecurePrefs
import com.amiri.note.util.ChecklistCodec
import com.amiri.note.util.ChecklistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorState(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val checklist: List<ChecklistItem> = emptyList(),
    val category: String = "General",
    val priority: Int = 0,
    val isPinned: Boolean = false,
    val dueAt: Long = 0,
    val loaded: Boolean = false
)

class NoteEditorViewModel(
    private val repo: AppRepository,
    private val prefs: SecurePrefs
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    /** Emits true exactly once when the secret phrase is detected in the note. */
    private val _secretTriggered = MutableStateFlow(false)
    val secretTriggered: StateFlow<Boolean> = _secretTriggered.asStateFlow()

    private var currentId: Long = 0
    private var dirty = false

    fun load(id: Long) {
        if (_state.value.loaded && currentId == id) return
        currentId = id
        if (id <= 0) {
            _state.value = EditorState(loaded = true)
            return
        }
        viewModelScope.launch {
            val n = repo.getNote(id)
            if (n != null) {
                _state.value = EditorState(
                    id = n.id, title = n.title, body = n.body,
                    checklist = ChecklistCodec.decode(n.checklist),
                    category = n.category, priority = n.priority,
                    isPinned = n.isPinned, dueAt = n.dueAt, loaded = true
                )
            } else {
                _state.value = EditorState(loaded = true)
            }
        }
    }

    fun onTitle(t: String) { _state.value = _state.value.copy(title = t); dirty = true; autosave() }
    fun onBody(b: String) {
        _state.value = _state.value.copy(body = b); dirty = true
        detectInBody(b)
        autosave()
    }
    fun onCategory(c: String) { _state.value = _state.value.copy(category = c); dirty = true; autosave() }
    fun onPriority(p: Int) { _state.value = _state.value.copy(priority = p); dirty = true; autosave() }
    fun onDue(ts: Long) { _state.value = _state.value.copy(dueAt = ts); dirty = true; autosave() }
    fun togglePin() { _state.value = _state.value.copy(isPinned = !_state.value.isPinned); dirty = true; autosave() }

    fun addChecklistItem(text: String) {
        if (text.isBlank()) return
        val list = _state.value.checklist + ChecklistItem(text.trim(), false)
        _state.value = _state.value.copy(checklist = list); dirty = true; autosave()
    }
    fun toggleChecklistItem(index: Int) {
        val list = _state.value.checklist.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(done = !list[index].done)
            _state.value = _state.value.copy(checklist = list); dirty = true; autosave()
        }
    }
    fun removeChecklistItem(index: Int) {
        val list = _state.value.checklist.toMutableList()
        if (index in list.indices) { list.removeAt(index)
            _state.value = _state.value.copy(checklist = list); dirty = true; autosave() }
    }

    /**
     * Detect the secret phrase anywhere in the body. We test a sliding set of
     * token windows against the hashed phrase so the phrase is never stored
     * in cleartext in the app.
     */
    private fun detectInBody(body: String) {
        val words = body.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty() || words.size > 4000) return
        // The phrase is short. Test windows of length 1..6 with fast SHA-256.
        for (w in 1..minOf(6, words.size)) {
            for (start in 0..(words.size - w)) {
                val candidate = words.subList(start, start + w).joinToString(" ")
                if (prefs.matchesPhrase(candidate)) {
                    _secretTriggered.value = true
                    return
                }
            }
        }
    }

    fun consumeSecret() { _secretTriggered.value = false }

    private var autosaveJob: kotlinx.coroutines.Job? = null
    private fun autosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(600) // debounce
            persist()
        }
    }

    fun persistNow() { viewModelScope.launch { persist() } }

    private suspend fun persist() {
        val s = _state.value
        // don't create an empty note
        if (s.id == 0L && s.title.isBlank() && s.body.isBlank() && s.checklist.isEmpty()) return
        val note = Note(
            id = s.id, title = s.title, body = s.body,
            checklist = ChecklistCodec.encode(s.checklist),
            category = s.category.ifBlank { "General" },
            priority = s.priority, isPinned = s.isPinned, dueAt = s.dueAt
        )
        val newId = repo.saveNote(note)
        if (s.id == 0L) {
            currentId = newId
            _state.value = s.copy(id = newId)
        }
        dirty = false
    }

    fun deleteCurrent(onDone: () -> Unit) {
        viewModelScope.launch {
            if (_state.value.id > 0) repo.deleteNote(_state.value.id)
            onDone()
        }
    }
}
