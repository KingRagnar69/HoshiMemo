package com.ragdev69.hoshimemo.data.repository

import com.ragdev69.hoshimemo.data.local.NoteDao
import com.ragdev69.hoshimemo.data.local.NoteEntity
import com.ragdev69.hoshimemo.data.local.TodoDao
import com.ragdev69.hoshimemo.data.local.TodoEntity
import com.ragdev69.hoshimemo.data.local.TodoGroupEntity
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.data.model.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

/**
 * Offline-first storage backed by Room.
 *
 * The ViewModel decides which repository to talk to:
 *  - user NOT signed in  →  LocalRepository only
 *  - user signed in      →  FirebaseRepository + sync pending local notes/todos
 */
class LocalRepository(
    private val dao: NoteDao,
    private val todoDao: TodoDao
) {

    fun getActiveNotes(): Flow<List<Note>> =
        dao.observeActiveNotes().map { list -> list.map { it.toNote() } }

    fun getDeletedNotes(): Flow<List<Note>> =
        dao.observeDeletedNotes().map { list -> list.map { it.toNote() } }

    /** Upsert: if [note].id is blank a new UUID is generated. Returns the id. */
    suspend fun saveNote(note: Note): String {
        val id = note.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(note.toEntity(id))
        return id
    }

    suspend fun moveToTrash(noteId: String)       = dao.softDelete(noteId)
    suspend fun restoreNote(noteId: String)        = dao.restore(noteId)
    suspend fun deleteNotePermanently(noteId: String) = dao.deletePermanently(noteId)
    suspend fun emptyTrash()                       = dao.emptyTrash()

    // ── Sync helpers (notes) ────────────────────────────────────────────────

    suspend fun getUnsyncedNotes(): List<Note> =
        dao.getUnsyncedNotes().map { it.toNote() }

    suspend fun markSynced(noteId: String) = dao.markSynced(noteId)

    // ── Todos ────────────────────────────────────────────────────────────────

    fun getTodos(): Flow<List<TodoItem>> =
        todoDao.observeTodos().map { list -> list.map { it.toTodoItem() } }

    /** Upsert: if [todo].id is blank a new UUID is generated. Returns the id. */
    suspend fun saveTodo(todo: TodoItem): String {
        val id = todo.id.ifBlank { UUID.randomUUID().toString() }
        todoDao.upsert(todo.toEntity(id))
        return id
    }

    suspend fun toggleTodo(todoId: String, completed: Boolean) = todoDao.toggleCompleted(todoId, completed)
    suspend fun markFailed(todoId: String)                     = todoDao.markFailed(todoId)
    suspend fun resetTodo(todoId: String)                      = todoDao.resetTodo(todoId)
    suspend fun startTimer(todoId: String)                     = todoDao.startTimer(todoId, System.currentTimeMillis())
    suspend fun deleteTodo(todoId: String)                     = todoDao.deleteTodo(todoId)
    suspend fun deleteTodosInGroup(groupId: String)            = todoDao.deleteTodosInGroup(groupId)

    suspend fun getUnsyncedTodos(): List<TodoItem> =
        todoDao.getUnsyncedTodos().map { it.toTodoItem() }

    suspend fun markTodoSynced(todoId: String) = todoDao.markTodoSynced(todoId)

    // ── Todo groups ──────────────────────────────────────────────────────────

    fun getGroups(): Flow<List<Map<String, String>>> =
        todoDao.observeGroups().map { list ->
            list.map { mapOf("groupId" to it.groupId, "groupName" to it.groupName) }
        }

    /** Upsert: if [groupId] is blank a new UUID is generated. */
    suspend fun saveGroup(groupId: String, groupName: String, userId: String = "") {
        val id = groupId.ifBlank { UUID.randomUUID().toString() }
        todoDao.upsertGroup(
            TodoGroupEntity(
                groupId   = id,
                groupName = groupName,
                userId    = userId,
                isSynced  = false
            )
        )
    }

    suspend fun deleteGroup(groupId: String) = todoDao.deleteGroup(groupId)

    suspend fun getUnsyncedGroups(): List<TodoGroupEntity> = todoDao.getUnsyncedGroups()
    suspend fun markGroupSynced(groupId: String) = todoDao.markGroupSynced(groupId)
}

// ── Mappers (Notes) ──────────────────────────────────────────────────────────

fun NoteEntity.toNote() = Note(
    id        = id,
    title     = title,
    content   = content,
    color     = color,
    createdAt = Date(createdAt),
    updatedAt = Date(updatedAt),
    isDeleted = isDeleted,
    userId    = userId,
    isPinned  = isPinned
)

fun Note.toEntity(overrideId: String = id) = NoteEntity(
    id        = overrideId,
    title     = title,
    content   = content,
    color     = color,
    createdAt = createdAt?.time ?: System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),   // always bump on save
    isDeleted = isDeleted,
    userId    = userId,
    isPinned  = isPinned,
    isSynced  = false
)

// ── Mappers (Todos) ──────────────────────────────────────────────────────────

fun TodoEntity.toTodoItem() = TodoItem(
    id             = id,
    title          = title,
    isCompleted    = isCompleted,
    isFailed       = isFailed,
    createdAt      = Date(createdAt),
    userId         = userId,
    priority       = priority,
    timerSeconds   = timerSeconds,
    timerStartedAt = timerStartedAt,
    groupId        = groupId,
    groupName      = groupName,
    orderInGroup   = orderInGroup
)

fun TodoItem.toEntity(overrideId: String = id) = TodoEntity(
    id             = overrideId,
    title          = title,
    isCompleted    = isCompleted,
    isFailed       = isFailed,
    createdAt      = createdAt?.time ?: System.currentTimeMillis(),
    userId         = userId,
    priority       = priority,
    timerSeconds   = timerSeconds,
    timerStartedAt = timerStartedAt,
    groupId        = groupId,
    groupName      = groupName,
    orderInGroup   = orderInGroup,
    isSynced       = false
)