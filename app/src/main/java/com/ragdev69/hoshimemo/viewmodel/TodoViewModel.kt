package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ragdev69.hoshimemo.data.model.TodoItem
import com.ragdev69.hoshimemo.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TodoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FirebaseRepository()

    // SharedPreferences used to persist dismissed timer-expired dialog IDs
    // across app kills. ViewModel in-memory state would reset on process death.
    private val prefs = app.getSharedPreferences("hoshi_todo_prefs", android.content.Context.MODE_PRIVATE)
    private val KEY_EXPIRED = "expired_dismissed_ids"

    private fun loadDismissedIds(): Set<String> =
        prefs.getStringSet(KEY_EXPIRED, emptySet()) ?: emptySet()

    private fun saveDismissedIds(ids: Set<String>) =
        prefs.edit().putStringSet(KEY_EXPIRED, ids).apply()

    // Re-subscribes to todos whenever auth state changes (login / logout).
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todos: StateFlow<List<TodoItem>> = repo.authStateFlow
        .flatMapLatest { uid ->
            if (uid != null) repo.getTodos()
            else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTodos: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> !t.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTodos: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message

    // IDs of todos whose timer-expired dialog has already been shown AND answered.
    // FIX: loaded from SharedPreferences so it survives app kills — previously
    // this was pure in-memory state that reset on process death, causing the
    // "did you complete it?" dialog to reappear every time the app was reopened.
    private val _expiredDialogDismissed = MutableStateFlow<Set<String>>(loadDismissedIds())
    val expiredDialogDismissed: StateFlow<Set<String>> = _expiredDialogDismissed

    fun markExpiredDialogDismissed(todoId: String) {
        val updated = _expiredDialogDismissed.value + todoId
        _expiredDialogDismissed.value = updated
        saveDismissedIds(updated)   // FIX: persist so it survives app kill
    }

    /**
     * @param timerSeconds  total countdown in seconds; 0 means no timer
     */
    fun addTodo(title: String, priority: Int = 0, timerSeconds: Long = 0L) =
        viewModelScope.launch {
            try {
                val startedAt = if (timerSeconds > 0L) System.currentTimeMillis() else 0L
                repo.saveTodo(
                    TodoItem(
                        title          = title,
                        priority       = priority,
                        timerSeconds   = timerSeconds,
                        timerStartedAt = startedAt
                    )
                )
            } catch (e: Exception) {
                _message.emit(e.message ?: "Failed to add task")
            }
        }

    // IDs of todos that were JUST marked complete — used to trigger the
    // completion animation in TodoCard. Stored in ViewModel so it survives
    // the list transition (pending → completed). Cleared after 800ms so
    // the animation only plays once.
    private val _justCompletedIds = MutableStateFlow<Set<String>>(emptySet())
    val justCompletedIds: StateFlow<Set<String>> = _justCompletedIds

    fun toggleTodo(todoId: String, completed: Boolean) = viewModelScope.launch {
        try {
            repo.toggleTodo(todoId, completed)
            if (completed) {
                // Mark as just-completed to trigger animation in the card
                _justCompletedIds.value = _justCompletedIds.value + todoId
                // Clear it after the animation window (600ms burst + buffer)
                kotlinx.coroutines.delay(800)
                _justCompletedIds.value = _justCompletedIds.value - todoId
            }
        }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    fun deleteTodo(todoId: String) = viewModelScope.launch {
        try { repo.deleteTodo(todoId) }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }
}