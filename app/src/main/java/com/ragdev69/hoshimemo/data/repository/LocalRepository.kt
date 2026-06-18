package com.ragdev69.hoshimemo.data.repository

import com.ragdev69.hoshimemo.data.local.NoteDao
import com.ragdev69.hoshimemo.data.local.NoteEntity
import com.ragdev69.hoshimemo.data.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

/**
 * Offline-first storage backed by Room.
 *
 * The ViewModel decides which repository to talk to:
 *  - user NOT signed in  →  LocalRepository only
 *  - user signed in      →  FirebaseRepository + sync pending local notes
 */
class LocalRepository(private val dao: NoteDao) {

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

    // ── Sync helpers ──────────────────────────────────────────────────────────

    suspend fun getUnsyncedNotes(): List<Note> =
        dao.getUnsyncedNotes().map { it.toNote() }

    suspend fun markSynced(noteId: String) = dao.markSynced(noteId)
}

// ── Mappers ───────────────────────────────────────────────────────────────────

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