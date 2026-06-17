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

    // Ungrouped only — shown in main list
    val ungroupedPending: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.groupId.isEmpty() && !t.isCompleted && !t.isFailed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ungroupedCompleted: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.groupId.isEmpty() && t.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ungroupedFailed: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.groupId.isEmpty() && t.isFailed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // For progress bar — all ungrouped
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
            // Delete all todos in the group first
            todos.value.filter { it.groupId == groupId }.forEach {
                repo.deleteTodo(it.id)
            }
            // Clear their IDs from dismissed set
            val taskIds = todos.value.filter { it.groupId == groupId }.map { it.id }.toSet()
            val updated = _expiredDialogDismissed.value - taskIds
            _expiredDialogDismissed.value = updated
            saveDismissedIds(updated)
            // Then delete the group document
            repo.deleteGroup(groupId)
        }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    fun getTasksForGroup(groupId: String): List<TodoItem> =
        todos.value.filter { it.groupId == groupId }.sortedBy { it.orderInGroup }

    // All group tasks as a single map — collect this in the UI instead of
    // calling getTasksForGroup() inside items{} which creates a new StateFlow
    // on every recomposition and crashes with multiple groups.
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

    // ── Expired dialog dismissed set (persisted) ──────────────
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
            // Timer is NOT started on creation — user taps Start manually
            repo.saveTodo(
                TodoItem(
                    title        = title,
                    priority     = priority,
                    timerSeconds = timerSeconds,
                    timerStartedAt = 0L,           // not started yet
                    groupId      = groupId,
                    groupName    = groupName,
                    orderInGroup = orderInGroup
                )
            )
        } catch (e: Exception) {
            _message.emit(e.message ?: "Failed to add task")
        }
    }

    // ── Complete (swipe right) ────────────────────────────────
    fun completeTodo(todoId: String) = viewModelScope.launch {
        try {
            // Only animate if not already completed
            val alreadyDone = todos.value.find { it.id == todoId }?.isCompleted == true
            repo.toggleTodo(todoId, true)
            if (!alreadyDone) {
                _justCompletedIds.value = _justCompletedIds.value + todoId
                kotlinx.coroutines.delay(800)
                _justCompletedIds.value = _justCompletedIds.value - todoId
                autoAdvanceGroup(todoId)
            }
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // ── Fail (swipe left) ─────────────────────────────────────
    fun failTodo(todoId: String) = viewModelScope.launch {
        try {
            val alreadyFailed = todos.value.find { it.id == todoId }?.isFailed == true
            repo.markFailed(todoId)
            if (!alreadyFailed) {
                _justFailedIds.value = _justFailedIds.value + todoId
                kotlinx.coroutines.delay(800)
                _justFailedIds.value = _justFailedIds.value - todoId
                autoAdvanceGroup(todoId)
            }
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // Keep toggleTodo for timer-expired dialog "yes" button
    fun toggleTodo(todoId: String, completed: Boolean) = viewModelScope.launch {
        try {
            val alreadyDone = todos.value.find { it.id == todoId }?.isCompleted == true
            repo.toggleTodo(todoId, completed)
            if (completed && !alreadyDone) {
                _justCompletedIds.value = _justCompletedIds.value + todoId
                kotlinx.coroutines.delay(800)
                _justCompletedIds.value = _justCompletedIds.value - todoId
                autoAdvanceGroup(todoId)
            }
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // ── Reset todo ────────────────────────────────────────────
    fun resetTodo(todoId: String) = viewModelScope.launch {
        try {
            repo.resetTodo(todoId)
            // Clear from dismissed set so the timer-expired dialog can show again
            val updated = _expiredDialogDismissed.value - todoId
            _expiredDialogDismissed.value = updated
            saveDismissedIds(updated)
        }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // ── Reset all todos in a group ────────────────────────────
    fun resetGroup(groupId: String) = viewModelScope.launch {
        try {
            val tasks = todos.value.filter { it.groupId == groupId }
            tasks.forEach { repo.resetTodo(it.id) }
            // Clear all group task IDs from dismissed set
            val taskIds = tasks.map { it.id }.toSet()
            val updated = _expiredDialogDismissed.value - taskIds
            _expiredDialogDismissed.value = updated
            saveDismissedIds(updated)
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // ── Manual timer start ────────────────────────────────────
    fun startTimer(todoId: String) = viewModelScope.launch {
        try { repo.startTimer(todoId) }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // Start the first pending task in a group (called from group START button)
    fun startGroupTimer(groupId: String) = viewModelScope.launch {
        try {
            val first = todos.value
                .filter { it.groupId == groupId && !it.isCompleted && !it.isFailed }
                .minByOrNull { it.orderInGroup } ?: return@launch
            if (first.timerSeconds > 0L && first.timerStartedAt == 0L) {
                repo.startTimer(first.id)
            }
        } catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }

    // ── Auto-advance group ────────────────────────────────────
    // Called after a task is marked complete/failed. Waits for Firestore to
    // confirm the update (by polling todos.value), then starts the next task.
    private suspend fun autoAdvanceGroup(finishedTodoId: String) {
        val finished = todos.value.find { it.id == finishedTodoId } ?: return
        if (finished.groupId.isEmpty()) return
        val groupId = finished.groupId

        // Poll until Firestore snapshot confirms the finished task is done
        // (avoids the race where todos.value still shows old state)
        var attempts = 0
        while (attempts < 10) {
            val confirmedDone = todos.value.find { it.id == finishedTodoId }
                ?.let { it.isCompleted || it.isFailed } == true
            if (confirmedDone) break
            kotlinx.coroutines.delay(300)
            attempts++
        }

        // Find next pending task
        val nextTask = todos.value
            .filter { it.groupId == groupId && !it.isCompleted && !it.isFailed && it.id != finishedTodoId }
            .minByOrNull { it.orderInGroup } ?: return

        // Start its timer if it has one and hasn't been started yet
        if (nextTask.timerSeconds > 0L && nextTask.timerStartedAt == 0L) {
            repo.startTimer(nextTask.id)
        }
    }

    fun deleteTodo(todoId: String) = viewModelScope.launch {
        try { repo.deleteTodo(todoId) }
        catch (e: Exception) { _message.emit(e.message ?: "Error") }
    }
}