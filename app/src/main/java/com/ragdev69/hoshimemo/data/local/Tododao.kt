package com.ragdev69.hoshimemo.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    // ── Todos ─────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM todo_items ORDER BY createdAt DESC")
    fun observeTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    suspend fun getTodoById(id: String): TodoEntity?

    @Upsert
    suspend fun upsert(todo: TodoEntity)

    @Query("UPDATE todo_items SET isCompleted = :completed, isFailed = 0 WHERE id = :id")
    suspend fun toggleCompleted(id: String, completed: Boolean)

    @Query("UPDATE todo_items SET isFailed = 1 WHERE id = :id")
    suspend fun markFailed(id: String)

    @Query("UPDATE todo_items SET isCompleted = 0, isFailed = 0, timerStartedAt = 0 WHERE id = :id")
    suspend fun resetTodo(id: String)

    @Query("UPDATE todo_items SET timerStartedAt = :startedAt WHERE id = :id")
    suspend fun startTimer(id: String, startedAt: Long)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteTodo(id: String)

    @Query("DELETE FROM todo_items WHERE groupId = :groupId")
    suspend fun deleteTodosInGroup(groupId: String)

    @Query("SELECT * FROM todo_items WHERE isSynced = 0")
    suspend fun getUnsyncedTodos(): List<TodoEntity>

    @Query("UPDATE todo_items SET isSynced = 1 WHERE id = :id")
    suspend fun markTodoSynced(id: String)

    // ── Groups ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM todo_groups ORDER BY createdAt ASC")
    fun observeGroups(): Flow<List<TodoGroupEntity>>

    @Upsert
    suspend fun upsertGroup(group: TodoGroupEntity)

    @Query("DELETE FROM todo_groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("SELECT * FROM todo_groups WHERE isSynced = 0")
    suspend fun getUnsyncedGroups(): List<TodoGroupEntity>

    @Query("UPDATE todo_groups SET isSynced = 1 WHERE groupId = :groupId")
    suspend fun markGroupSynced(groupId: String)
}