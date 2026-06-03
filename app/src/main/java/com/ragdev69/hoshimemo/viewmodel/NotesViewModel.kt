package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.data.repository.FirebaseRepository
import com.ragdev69.hoshimemo.utils.PrefsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FirebaseRepository()
    private val prefs = PrefsManager(app)

    val sortBy: StateFlow<String> = prefs.sortByFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "updatedAt")

    val isGridLayout: StateFlow<Boolean> = prefs.gridLayoutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // FIX (crash + immediate appearance):
    // Previously called repo.getActiveNotes(sortBy) which passed sortBy into
    // a Firestore orderBy() — that crashes without a composite index and also
    // causes notes to temporarily disappear when serverTimestamp is pending.
    // Now we fetch all active notes once (no Firestore orderBy) and sort
    // entirely client-side using combine(). This is instant, works offline,
    // and never crashes regardless of what sort key is selected.
    val notes: StateFlow<List<Note>> = combine(
        repo.getActiveNotes(),
        sortBy
    ) { noteList, sort ->
        when (sort) {
            "createdAt" -> noteList.sortedByDescending { it.createdAt }
            "title"     -> noteList.sortedBy { it.title.lowercase() }
            else        -> noteList.sortedByDescending { it.updatedAt } // "updatedAt" default
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = repo.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredNotes: StateFlow<List<Note>> = combine(notes, _searchQuery) { notes, query ->
        if (query.isBlank()) notes
        else notes.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setSortBy(sortBy: String) = viewModelScope.launch {
        prefs.setSortBy(sortBy)
    }

    fun setGridLayout(isGrid: Boolean) = viewModelScope.launch {
        prefs.setGridLayout(isGrid)
    }

    fun saveNote(note: Note, onSuccess: (String) -> Unit = {}) = viewModelScope.launch {
        _isLoading.value = true
        try {
            val id = repo.saveNote(note)
            _isLoading.value = false
            _message.emit("Note saved ✓")
            onSuccess(id)
        } catch (e: Exception) {
            _isLoading.value = false
            _message.emit(e.message ?: "Failed to save note")
        }
    }

    fun moveToTrash(noteId: String) = viewModelScope.launch {
        try {
            repo.moveToTrash(noteId)
            _message.emit("Moved to trash")
        } catch (e: Exception) {
            _message.emit(e.message ?: "Error")
        }
    }

    fun restoreNote(noteId: String) = viewModelScope.launch {
        try {
            repo.restoreNote(noteId)
            _message.emit("Note restored ✓")
        } catch (e: Exception) {
            _message.emit(e.message ?: "Error")
        }
    }

    fun deletePermanently(noteId: String) = viewModelScope.launch {
        try {
            repo.deleteNotePermanently(noteId)
            _message.emit("Deleted permanently")
        } catch (e: Exception) {
            _message.emit(e.message ?: "Error")
        }
    }

    fun emptyTrash() = viewModelScope.launch {
        trashedNotes.value.forEach {
            try { repo.deleteNotePermanently(it.id) } catch (_: Exception) {}
        }
        _message.emit("Trash emptied")
    }
}