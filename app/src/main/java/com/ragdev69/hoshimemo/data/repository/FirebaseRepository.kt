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

    private fun notesCol()  = db.collection("users").document(userId).collection("notes")
    private fun todosCol()  = db.collection("users").document(userId).collection("todos")
    private fun groupsCol() = db.collection("users").document(userId).collection("todoGroups")

    // Emits uid on every login/logout — used by ViewModels to re-subscribe
    val authStateFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // ── Auth ──────────────────────────────────────────────────
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            Result.success(auth.signInWithEmailAndPassword(email, password).await().user!!)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createAccount(email: String, password: String): Result<FirebaseUser> {
        return try {
            Result.success(auth.createUserWithEmailAndPassword(email, password).await().user!!)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Result.success(auth.signInWithCredential(credential).await().user!!)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun signOut() = auth.signOut()

    // ── Profile ───────────────────────────────────────────────
    suspend fun profileExists(uid: String): Boolean {
        return try {
            db.collection("users").document(uid).get().await().exists()
        } catch (e: Exception) { true }
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
        db.collection("users").document(uid).set(data, SetOptions.merge()).await()
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
                    trySend(snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList())
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
                    trySend(snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList())
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveNote(note: Note): String {
        return if (note.id.isEmpty()) {
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
            notesCol().document(note.id).update(data)
            note.id
        }
    }

    suspend fun moveToTrash(noteId: String) {
        notesCol().document(noteId).update(
            "isDeleted", true, "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    suspend fun moveMultipleToTrash(noteIds: List<String>) {
        if (noteIds.isEmpty()) return
        val batch = db.batch()
        noteIds.forEach { id ->
            batch.update(notesCol().document(id), mapOf(
                "isDeleted" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
        }
        batch.commit().await()
    }

    suspend fun restoreNote(noteId: String) {
        notesCol().document(noteId).update(
            "isDeleted", false, "updatedAt", FieldValue.serverTimestamp()
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
                    trySend(snap?.documents?.mapNotNull { it.toObject(TodoItem::class.java) } ?: emptyList())
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun saveTodo(todo: TodoItem): String {
        val data = mapOf(
            "title"          to todo.title,
            "isCompleted"    to todo.isCompleted,
            "isFailed"       to todo.isFailed,
            "userId"         to userId,
            "priority"       to todo.priority,
            "timerSeconds"   to todo.timerSeconds,
            "timerStartedAt" to todo.timerStartedAt,
            "groupId"        to todo.groupId,
            "groupName"      to todo.groupName,
            "orderInGroup"   to todo.orderInGroup,
            "createdAt"      to FieldValue.serverTimestamp()
        )
        return if (todo.id.isEmpty()) {
            todosCol().add(data).await().id
        } else {
            todosCol().document(todo.id).set(data).await()
            todo.id
        }
    }

    suspend fun toggleTodo(todoId: String, completed: Boolean) {
        todosCol().document(todoId).update(mapOf(
            "isCompleted" to completed,
            "isFailed"    to false      // clear failed state when marking complete
        )).await()
    }

    // Reset: mark incomplete, clear failed, clear timer start
    suspend fun resetTodo(todoId: String) {
        todosCol().document(todoId).update(mapOf(
            "isCompleted"    to false,
            "isFailed"       to false,
            "timerStartedAt" to 0L
        )).await()
    }

    // Mark a todo as failed (swipe left)
    suspend fun markFailed(todoId: String) {
        todosCol().document(todoId).update("isFailed", true).await()
    }

    // Manually start the timer — sets timerStartedAt to now
    suspend fun startTimer(todoId: String) {
        todosCol().document(todoId).update(
            "timerStartedAt", System.currentTimeMillis()
        ).await()
    }

    // ── Groups ─────────────────────────────────────────────────
    suspend fun saveGroup(groupId: String, groupName: String) {
        val data = mapOf(
            "groupId"   to groupId,
            "groupName" to groupName,
            "userId"    to userId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (groupId.isEmpty()) {
            groupsCol().add(data).await()
        } else {
            groupsCol().document(groupId).set(data).await()
        }
    }

    fun getGroups(): Flow<List<Map<String, String>>> {
        val uid = userIdOrNull ?: return emptyFlow()
        return callbackFlow {
            val listener = db.collection("users").document(uid).collection("todoGroups")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) { close(err); return@addSnapshotListener }
                    val groups = snap?.documents?.map { doc ->
                        // FIX (crash on 2nd group): saveGroup() always saves groupId = ""
                        // for newly created groups, so doc.getString("groupId") returns
                        // "" (empty string, not null) for EVERY group. The elvis (?:)
                        // operator only triggers on null, not on "", so every group's
                        // resolved groupId was "". TodoScreen's items(groups, key = {
                        // it["groupId"] ?: "" }) then assigned the SAME Compose key ("")
                        // to every group — Compose throws "duplicate key" on the 2nd
                        // group and the app crashes.
                        //
                        // Fix: if the stored groupId is empty, fall back to doc.id
                        // (the real Firestore document ID, which is always unique).
                        val storedId = doc.getString("groupId") ?: ""
                        val gId = if (storedId.isEmpty()) doc.id else storedId
                        mapOf(
                            "groupId"   to gId,
                            "groupName" to (doc.getString("groupName") ?: "")
                        )
                    } ?: emptyList()
                    trySend(groups)
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun deleteGroup(groupId: String) {
        groupsCol().document(groupId).delete().await()
    }

    suspend fun deleteTodo(todoId: String) {
        todosCol().document(todoId).delete().await()
    }
}