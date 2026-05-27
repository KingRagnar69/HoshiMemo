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

    val notes: StateFlow<List<Note>> = sortBy
        .flatMapLatest { repo.getActiveNotes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            _message.emit("Note saved ✓")
            _isLoading.value = false   // ← clear loading BEFORE navigating back
            onSuccess(id)
        } catch (e: Exception) {
            _message.emit(e.message ?: "Failed to save note")
            _isLoading.value = false
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