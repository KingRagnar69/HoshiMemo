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

    val todos: StateFlow<List<TodoItem>> = repo.getTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTodos: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> !t.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTodos: StateFlow<List<TodoItem>> = todos
        .map { it.filter { t -> t.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message

    fun addTodo(title: String, priority: Int = 0) = viewModelScope.launch {
        try {
            repo.saveTodo(TodoItem(title = title, priority = priority))
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