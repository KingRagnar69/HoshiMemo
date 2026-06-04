package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.data.repository.FirebaseRepository
import com.ragdev69.hoshimemo.utils.PrefsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FirebaseRepository()
    private val prefs = PrefsManager(app)

    val sortBy: StateFlow<String> = prefs.sortByFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "updatedAt")

    val isGridLayout: StateFlow<Boolean> = prefs.gridLayoutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // FIX: watch Firebase Auth state changes and re-subscribe to Firestore
    // whenever the user logs in or out. Previously getActiveNotes() was called
    // once at ViewModel init — if the user wasn't logged in yet it returned
    // emptyFlow() and never re-subscribed after login, so notes only appeared
    // after an app restart.
    private val authStateFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        awaitClose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    val notes: StateFlow<List<Note>> = combine(
        authStateFlow.flatMapLatest { uid ->
            if (uid != null) repo.getActiveNotes() else flowOf(emptyList())
        },
        sortBy
    ) { noteList, sort ->
        when (sort) {
            "createdAt" -> noteList.sortedByDescending { it.createdAt }
            "title"     -> noteList.sortedBy { it.title.lowercase() }
            else        -> noteList.sortedByDescending { it.updatedAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = authStateFlow
        .flatMapLatest { uid ->
            if (uid != null) repo.getDeletedNotes() else flowOf(emptyList())
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