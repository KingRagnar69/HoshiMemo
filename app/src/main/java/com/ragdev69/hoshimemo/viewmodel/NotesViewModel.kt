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

    // FIX — Problem 1 (notes missing after login):
    // The old code called repo.getActiveNotes() once at ViewModel init time.
    // If the user wasn't logged in yet, userIdOrNull was null → emptyFlow()
    // was returned and the Firestore listener was never attached. Notes only
    // appeared after a restart because that re-ran getActiveNotes() with a
    // valid uid.
    //
    // Fix: flatMapLatest on authStateFlow. Every time the user logs in or out,
    // authStateFlow emits the new uid (or null). When uid is non-null we attach
    // a fresh Firestore listener; when null we emit an empty list. This means
    // notes appear the moment authentication completes — no restart needed.
    //
    // FIX — Problem 2 (sort not working / note delay):
    // Sorting is done client-side with combine() so all three sort keys work
    // without Firestore composite indexes. The write path in saveNote() no
    // longer awaits server confirmation, so the snapshot listener fires from
    // the local cache instantly.
    private val rawNotes: Flow<List<Note>> = repo.authStateFlow
        .flatMapLatest { uid ->
            if (uid != null) repo.getActiveNotes(uid)
            else flowOf(emptyList())
        }

    val notes: StateFlow<List<Note>> = combine(rawNotes, sortBy) { noteList, sort ->
        when (sort) {
            "createdAt" -> noteList.sortedByDescending { it.createdAt }
            "title"     -> noteList.sortedBy { it.title.lowercase() }
            else        -> noteList.sortedByDescending { it.updatedAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = repo.authStateFlow
        .flatMapLatest { uid ->
            if (uid != null) repo.getDeletedNotes(uid)
            else flowOf(emptyList())
        }
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