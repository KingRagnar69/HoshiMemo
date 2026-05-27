package com.ragdev69.hoshimemo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.data.model.TodoItem
import com.ragdev69.hoshimemo.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    private val userId: String    get() = auth.currentUser?.uid ?: "anonymous"

    // ── Collection References ────────────────────────────────
    private fun notesCol()   = db.collection("users").document(userId).collection("notes")
    private fun todosCol()   = db.collection("users").document(userId).collection("todos")
    private fun profileDoc() = db.collection("users").document(userId)

    // ── Auth ─────────────────────────────────────────────────
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAccount(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = auth.signOut()

    // ── Profile ──────────────────────────────────────────────
    suspend fun saveProfile(profile: UserProfile) {
        profileDoc().set(profile).await()
    }

    fun getProfile(): Flow<UserProfile?> = callbackFlow {
        val listener = profileDoc().addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(UserProfile::class.java))
        }
        awaitClose { listener.remove() }
    }

    // ── Notes ────────────────────────────────────────────────
    fun getActiveNotes(sortBy: String = "updatedAt"): Flow<List<Note>> = callbackFlow {
        val listener = notesCol()
            .whereEqualTo("isDeleted", false)
            .orderBy(sortBy, Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(
                    snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList()
                )
            }
        awaitClose { listener.remove() }
    }

    fun getDeletedNotes(): Flow<List<Note>> = callbackFlow {
        val listener = notesCol()
            .whereEqualTo("isDeleted", true)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(
                    snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList()
                )
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveNote(note: Note): String {
        val data = mutableMapOf<String, Any>(
            "title"     to note.title,
            "content"   to note.content,
            "color"     to note.color,
            "isDeleted" to note.isDeleted,
            "userId"    to userId,
            "isPinned"  to note.isPinned,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        return if (note.id.isEmpty()) {
            data["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
            notesCol().add(data).await().id
        } else {
            notesCol().document(note.id).set(data).await()
            note.id
        }
    }

    suspend fun moveToTrash(noteId: String) {
        notesCol().document(noteId).update(
            "isDeleted", true,
            "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
        ).await()
    }

    suspend fun restoreNote(noteId: String) {
        notesCol().document(noteId).update(
            "isDeleted", false,
            "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
        ).await()
    }

    suspend fun deleteNotePermanently(noteId: String) {
        notesCol().document(noteId).delete().await()
    }

    // ── Todos ────────────────────────────────────────────────
    fun getTodos(): Flow<List<TodoItem>> = callbackFlow {
        val listener = todosCol()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(
                    snap?.documents?.mapNotNull { it.toObject(TodoItem::class.java) } ?: emptyList()
                )
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveTodo(todo: TodoItem): String {
        val data = mapOf(
            "title"       to todo.title,
            "isCompleted" to todo.isCompleted,
            "userId"      to userId,
            "priority"    to todo.priority,
            "createdAt"   to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        return if (todo.id.isEmpty()) {
            todosCol().add(data).await().id
        } else {
            todosCol().document(todo.id).set(data).await()
            todo.id
        }
    }

    suspend fun toggleTodo(todoId: String, completed: Boolean) {
        todosCol().document(todoId).update("isCompleted", completed).await()
    }

    suspend fun deleteTodo(todoId: String) {
        todosCol().document(todoId).delete().await()
    }
}