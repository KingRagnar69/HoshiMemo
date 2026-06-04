package com.ragdev69.hoshimemo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ragdev69.hoshimemo.data.model.Note
import com.ragdev69.hoshimemo.data.model.TodoItem
import com.ragdev69.hoshimemo.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    private val userIdOrNull: String?
        get() = auth.currentUser?.uid

    private fun notesCol() = db.collection("users").document(userId).collection("notes")
    private fun todosCol() = db.collection("users").document(userId).collection("todos")

    // ── Auth ──────────────────────────────────────────────────
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

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result     = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = auth.signOut()

    // ── Profile ───────────────────────────────────────────────
    suspend fun profileExists(uid: String): Boolean {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.exists()
        } catch (e: Exception) {
            true
        }
    }

    suspend fun saveProfile(profile: UserProfile, explicitUid: String? = null) {
        val uid  = explicitUid ?: userId
        val data = mapOf(
            "uid"           to uid,
            "name"          to profile.name,
            "email"         to profile.email,
            "age"           to profile.age,
            "bio"           to profile.bio,
            "avatarInitial" to profile.avatarInitial
        )
        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .await()
    }

    fun getProfile(): Flow<UserProfile?> {
        val uid = userIdOrNull ?: return emptyFlow()
        return callbackFlow {
            val listener = db.collection("users").document(uid)
                .addSnapshotListener { snap, err ->
                    if (err != null) { close(err); return@addSnapshotListener }
                    trySend(snap?.toObject(UserProfile::class.java))
                }
            awaitClose { listener.remove() }
        }
    }

    // ── Notes ─────────────────────────────────────────────────
    fun getActiveNotes(): Flow<List<Note>> {
        val uid = userIdOrNull ?: return emptyFlow()
        return callbackFlow {
            val listener = db.collection("users").document(uid).collection("notes")
                .whereEqualTo("isDeleted", false)
                .addSnapshotListener { snap, err ->
                    if (err != null) { close(err); return@addSnapshotListener }
                    trySend(
                        snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList()
                    )
                }
            awaitClose { listener.remove() }
        }
    }

    fun getDeletedNotes(): Flow<List<Note>> {
        val uid = userIdOrNull ?: return emptyFlow()
        return callbackFlow {
            val listener = db.collection("users").document(uid).collection("notes")
                .whereEqualTo("isDeleted", true)
                .addSnapshotListener { snap, err ->
                    if (err != null) { close(err); return@addSnapshotListener }
                    trySend(
                        snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList()
                    )
                }
            awaitClose { listener.remove() }
        }
    }

    // FIX (new note delay): don't .await() on writes — with offline persistence
    // enabled Firestore commits to local cache instantly and fires the snapshot
    // listener before the server responds. Using .await() was blocking until the
    // server round-trip completed (~1-2s) before returning, which is why the note
    // felt delayed. Now we fire-and-forget the write; the listener picks it up
    // from cache immediately.
    suspend fun saveNote(note: Note): String {
        return if (note.id.isEmpty()) {
            // Generate a local ID immediately so we can return it without waiting
            val docRef = notesCol().document()
            val data = mapOf<String, Any>(
                "title"     to note.title,
                "content"   to note.content,
                "color"     to note.color,
                "isDeleted" to note.isDeleted,
                "userId"    to userId,
                "isPinned"  to note.isPinned,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // set() without await — fires instantly to local cache
            docRef.set(data)
            docRef.id
        } else {
            val data = mapOf<String, Any>(
                "title"     to note.title,
                "content"   to note.content,
                "color"     to note.color,
                "isDeleted" to note.isDeleted,
                "isPinned"  to note.isPinned,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // update() without await — fires instantly to local cache
            notesCol().document(note.id).update(data)
            note.id
        }
    }

    suspend fun moveToTrash(noteId: String) {
        notesCol().document(noteId).update(
            "isDeleted", true,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    suspend fun restoreNote(noteId: String) {
        notesCol().document(noteId).update(
            "isDeleted", false,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    suspend fun deleteNotePermanently(noteId: String) {
        notesCol().document(noteId).delete().await()
    }

    // ── Todos ─────────────────────────────────────────────────
    fun getTodos(): Flow<List<TodoItem>> {
        val uid = userIdOrNull ?: return emptyFlow()
        return callbackFlow {
            val listener = db.collection("users").document(uid).collection("todos")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) { close(err); return@addSnapshotListener }
                    trySend(
                        snap?.documents?.mapNotNull { it.toObject(TodoItem::class.java) } ?: emptyList()
                    )
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveTodo(todo: TodoItem): String {
        val data = mapOf(
            "title"       to todo.title,
            "isCompleted" to todo.isCompleted,
            "userId"      to userId,
            "priority"    to todo.priority,
            "createdAt"   to FieldValue.serverTimestamp()
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