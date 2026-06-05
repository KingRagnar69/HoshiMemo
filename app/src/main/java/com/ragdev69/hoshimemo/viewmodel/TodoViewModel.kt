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

    // IDs of todos whose timer-expired dialog has already been shown AND answered
    // (either "yes done" or "not yet"). Stored in ViewModel so it survives
    // navigation — remember{} would reset on every recomposition.
    private val _expiredDialogDismissed = MutableStateFlow<Set<String>>(emptySet())
    val expiredDialogDismissed: StateFlow<Set<String>> = _expiredDialogDismissed

    fun markExpiredDialogDismissed(todoId: String) {
        _expiredDialogDismissed.value = _expiredDialogDismissed.value + todoId
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

    fun toggleTodo(todoId: String, completed: Boolean) = viewModelScope.launch {
        try { repo.toggleTodo(todoId, completed) }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    fun deleteTodo(todoId: String) = viewModelScope.launch {
        try { repo.deleteTodo(todoId) }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }
}