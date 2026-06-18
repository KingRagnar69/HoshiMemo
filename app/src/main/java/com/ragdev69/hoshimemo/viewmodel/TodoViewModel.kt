package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ragdev69.hoshimemo.data.model.TodoItem
import com.ragdev69.hoshimemo.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TodoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo  = FirebaseRepository()
    private val prefs = app.getSharedPreferences("hoshi_todo_prefs", android.content.Context.MODE_PRIVATE)
    private val KEY_EXPIRED = "expired_dismissed_ids"

    private fun loadDismissedIds(): Set<String> =
        prefs.getStringSet(KEY_EXPIRED, emptySet()) ?: emptySet()
    private fun saveDismissedIds(ids: Set<String>) =
        prefs.edit().putStringSet(KEY_EXPIRED, ids).apply()

    // ── Todos ─────────────────────────────────────────────────
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todos: StateFlow<List<TodoItem>> = repo.authStateFlow
        .flatMapLatest { uid ->
            if (uid != null) repo.getTodos() else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ungroupedPending: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.groupId.isEmpty() && !t.isCompleted && !t.isFailed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ungroupedCompleted: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.groupId.isEmpty() && t.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ungroupedFailed: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.groupId.isEmpty() && t.isFailed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTodos: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> !t.isCompleted && !t.isFailed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTodos: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Groups ────────────────────────────────────────────────
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val groups: StateFlow<List<Map<String, String>>> = repo.authStateFlow
        .flatMapLatest { uid ->
            if (uid != null) repo.getGroups() else flowOf(emptyList())
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createGroup(name: String) = viewModelScope.launch {
        try { repo.saveGroup("", name) }
        catch (e: Exception) { _message.emit(e.message ?: "Failed to create group") }
    }

    fun deleteGroup(groupId: String) = viewModelScope.launch {
        try {
            todos.value.filter { it.groupId == groupId }.forEach { repo.deleteTodo(it.id) }
            val taskIds = todos.value.filter { it.groupId == groupId }.map { it.id }.toSet()
            val updated = _expiredDialogDismissed.value - taskIds
            _expiredDialogDismissed.value = updated
            saveDismissedIds(updated)
            repo.deleteGroup(groupId)
        }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    fun getTasksForGroup(groupId: String): List<TodoItem> =
        todos.value.filter { it.groupId == groupId }.sortedBy { it.orderInGroup }

    val groupTasksMap: StateFlow<Map<String, List<TodoItem>>> = todos
        .map { list ->
            list.filter { it.groupId.isNotEmpty() }
                .groupBy { it.groupId }
                .mapValues { (_, v) -> v.sortedBy { it.orderInGroup } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ── Messages ──────────────────────────────────────────────
    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message

    // ── Expired dialog dismissed set (persisted) — used only for
    // ungrouped tasks now; grouped tasks auto-advance with no dialog.
    private val _expiredDialogDismissed = MutableStateFlow<Set<String>>(loadDismissedIds())
    val expiredDialogDismissed: StateFlow<Set<String>> = _expiredDialogDismissed

    fun markExpiredDialogDismissed(todoId: String) {
        val updated = _expiredDialogDismissed.value + todoId
        _expiredDialogDismissed.value = updated
        saveDismissedIds(updated)
    }

    // ── Animation IDs (ViewModel-scoped, survives list transition) ──
    private val _justCompletedIds = MutableStateFlow<Set<String>>(emptySet())
    val justCompletedIds: StateFlow<Set<String>> = _justCompletedIds

    private val _justFailedIds = MutableStateFlow<Set<String>>(emptySet())
    val justFailedIds: StateFlow<Set<String>> = _justFailedIds

    fun flashCompleted(todoId: String) = viewModelScope.launch {
        _justCompletedIds.value = _justCompletedIds.value + todoId
        kotlinx.coroutines.delay(700)
        _justCompletedIds.value = _justCompletedIds.value - todoId
    }

    fun flashFailed(todoId: String) = viewModelScope.launch {
        _justFailedIds.value = _justFailedIds.value + todoId
        kotlinx.coroutines.delay(700)
        _justFailedIds.value = _justFailedIds.value - todoId
    }

    // ── Add todo ──────────────────────────────────────────────
    fun addTodo(
        title: String,
        priority: Int = 0,
        timerSeconds: Long = 0L,
        groupId: String = "",
        groupName: String = "",
        orderInGroup: Int = 0
    ) = viewModelScope.launch {
        try {
            repo.saveTodo(
                TodoItem(
                    title          = title,
                    priority       = priority,
                    timerSeconds   = timerSeconds,
                    timerStartedAt = 0L,
                    groupId        = groupId,
                    groupName      = groupName,
                    orderInGroup   = orderInGroup
                )
            )
        } catch (e: Exception) {
            _message.emit(e.message ?: "Failed to add task")
        }
    }

    // ── Complete / Fail — fire-and-forget Firestore writes. ────
    // The UI (GroupTimerEngine) advances the local sequence immediately and
    // does NOT wait for these to return, so network latency never blocks
    // the next task from starting.
    fun completeTodo(todoId: String) {
        flashCompleted(todoId)
        viewModelScope.launch {
            try { repo.toggleTodo(todoId, true) }
            catch (e: Exception) { _message.emit(e.message ?: "Error") }
        }
    }

    fun failTodo(todoId: String) {
        flashFailed(todoId)
        viewModelScope.launch {
            try { repo.markFailed(todoId) }
            catch (e: Exception) { _message.emit(e.message ?: "Error") }
        }
    }

    fun toggleTodo(todoId: String, completed: Boolean) {
        if (completed) flashCompleted(todoId)
        viewModelScope.launch {
            try { repo.toggleTodo(todoId, completed) }
            catch (e: Exception) { _message.emit(e.message ?: "Error") }
        }
    }

    // ── Reset todo ────────────────────────────────────────────
    fun resetTodo(todoId: String) = viewModelScope.launch {
        try {
            repo.resetTodo(todoId)
            val updated = _expiredDialogDismissed.value - todoId
            _expiredDialogDismissed.value = updated
            saveDismissedIds(updated)
        }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // Reset all todos in a group — does NOT auto-start the timer.
    // User taps START on the group to begin the sequence again.
    fun resetGroup(groupId: String) = viewModelScope.launch {
        try {
            val tasks = todos.value.filter { it.groupId == groupId }
            tasks.forEach { repo.resetTodo(it.id) }
            val taskIds = tasks.map { it.id }.toSet()
            val updated = _expiredDialogDismissed.value - taskIds
            _expiredDialogDismissed.value = updated
            saveDismissedIds(updated)
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // ── Manual timer start (fire-and-forget) ───────────────────
    fun startTimer(todoId: String) {
        viewModelScope.launch {
            try { repo.startTimer(todoId) }
            catch (e: Exception) { _message.emit(e.message ?: "Error") }
        }
    }

    fun deleteTodo(todoId: String) = viewModelScope.launch {
        try { repo.deleteTodo(todoId) }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }
}