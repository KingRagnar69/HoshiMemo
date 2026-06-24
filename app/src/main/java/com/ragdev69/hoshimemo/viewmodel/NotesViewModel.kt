package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ragdev69.hoshimemo.data.local.HoshiDatabase
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.data.repository.FirebaseRepository
import com.ragdev69.hoshimemo.data.repository.LocalRepository
import com.ragdev69.hoshimemo.data.repository.toEntity
import com.ragdev69.hoshimemo.utils.PrefsManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val firebaseRepo = FirebaseRepository()
    private val localRepo    = LocalRepository(
        HoshiDatabase.getInstance(app).noteDao(),
        HoshiDatabase.getInstance(app).todoDao()
    )
    private val prefs        = PrefsManager(app)

    val sortBy: StateFlow<String> = prefs.sortByFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "updatedAt")

    val isGridLayout: StateFlow<Boolean> = prefs.gridLayoutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Emits the current uid (or null when signed out), re-emits on auth change.
    private val authStateFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        awaitClose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    // ── Notes stream: Room when offline, Firestore when signed in ─────────────
    val notes: StateFlow<List<Note>> = combine(
        authStateFlow.flatMapLatest { uid ->
            if (uid != null) firebaseRepo.getActiveNotes() else localRepo.getActiveNotes()
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
            if (uid != null) firebaseRepo.getDeletedNotes() else localRepo.getDeletedNotes()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading  = MutableStateFlow(false)
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

    // ── Called once right after a successful Google sign-in ──────────────────
    /**
     * Uploads all Room notes that haven't been synced to Firestore yet,
     * then marks them as synced locally so we don't duplicate on next login.
     */
    fun syncLocalNotesToFirebase() = viewModelScope.launch {
        val pending = localRepo.getUnsyncedNotes()
        pending.forEach { note ->
            try {
                val cloudId = firebaseRepo.saveNote(note)
                localRepo.markSynced(cloudId)
            } catch (_: Exception) { /* retry on next login */ }
        }
        if (pending.isNotEmpty()) {
            _message.emit("${pending.size} local note${if (pending.size > 1) "s" else ""} synced ✓")
        }
    }

    // ── CRUD — routes to the right repo depending on auth state ──────────────

    private val isSignedIn get() = FirebaseAuth.getInstance().currentUser != null

    fun saveNote(note: Note, onSuccess: (String) -> Unit = {}) = viewModelScope.launch {
        _isLoading.value = true
        try {
            val id = if (isSignedIn) {
                firebaseRepo.saveNote(note)
            } else {
                localRepo.saveNote(note)
            }
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
            if (isSignedIn) firebaseRepo.moveToTrash(noteId) else localRepo.moveToTrash(noteId)
            _message.emit("Moved to trash")
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    fun moveMultipleToTrash(noteIds: List<String>) = viewModelScope.launch {
        try {
            if (isSignedIn) {
                firebaseRepo.moveMultipleToTrash(noteIds)
            } else {
                noteIds.forEach { localRepo.moveToTrash(it) }
            }
            _message.emit("${noteIds.size} note${if (noteIds.size > 1) "s" else ""} moved to trash")
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    fun restoreNote(noteId: String) = viewModelScope.launch {
        try {
            if (isSignedIn) firebaseRepo.restoreNote(noteId) else localRepo.restoreNote(noteId)
            _message.emit("Note restored ✓")
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    fun deletePermanently(noteId: String) = viewModelScope.launch {
        try {
            if (isSignedIn) firebaseRepo.deleteNotePermanently(noteId)
            else            localRepo.deleteNotePermanently(noteId)
            _message.emit("Deleted permanently")
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    fun emptyTrash() = viewModelScope.launch {
        trashedNotes.value.forEach {
            try {
                if (isSignedIn) firebaseRepo.deleteNotePermanently(it.id)
                else            localRepo.deleteNotePermanently(it.id)
            } catch (_: Exception) {}
        }
        _message.emit("Trash emptied")
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortBy(sortBy: String)     = viewModelScope.launch { prefs.setSortBy(sortBy) }
    fun setGridLayout(isGrid: Boolean) = viewModelScope.launch { prefs.setGridLayout(isGrid) }
}