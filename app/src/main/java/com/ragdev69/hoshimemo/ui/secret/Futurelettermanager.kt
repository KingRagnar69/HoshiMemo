package com.ragdev69.hoshimemo.ui.secret

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.letterDataStore by preferencesDataStore(name = "future_letters")

data class FutureLetter(
    val id: String,
    val title: String,
    val content: String,
    val writtenAt: Long,
    val unlockAt: Long,
    val isOpened: Boolean = false
)

// ── JSON helpers (no external serialization lib needed) ───────
private fun FutureLetter.toJson(): JSONObject = JSONObject().apply {
    put("id",        id)
    put("title",     title)
    put("content",   content)
    put("writtenAt", writtenAt)
    put("unlockAt",  unlockAt)
    put("isOpened",  isOpened)
}

private fun JSONObject.toLetter(): FutureLetter = FutureLetter(
    id        = getString("id"),
    title     = getString("title"),
    content   = getString("content"),
    writtenAt = getLong("writtenAt"),
    unlockAt  = getLong("unlockAt"),
    isOpened  = optBoolean("isOpened", false)
)

private fun List<FutureLetter>.toJsonString(): String =
    JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }.toString()

private fun String.toLetterList(): List<FutureLetter> {
    val arr = runCatching { JSONArray(this) }.getOrDefault(JSONArray())
    return (0 until arr.length()).mapNotNull {
        runCatching { arr.getJSONObject(it).toLetter() }.getOrNull()
    }
}

// ── Manager ───────────────────────────────────────────────────
object FutureLetterManager {

    private val KEY_LETTERS = stringPreferencesKey("letters_json")

    fun getLettersFlow(context: Context): Flow<List<FutureLetter>> =
        context.letterDataStore.data.map { prefs ->
            prefs[KEY_LETTERS]?.toLetterList() ?: emptyList()
        }

    suspend fun saveLetter(context: Context, letter: FutureLetter) {
        context.letterDataStore.edit { prefs ->
            val existing = prefs[KEY_LETTERS]?.toLetterList() ?: emptyList()
            val updated  = existing.filterNot { it.id == letter.id } + letter
            prefs[KEY_LETTERS] = updated.toJsonString()
        }
        // Schedule the "it's unlocked" notification for this letter's unlockAt time.
        LetterAlarmScheduler.schedule(context.applicationContext, letter)
    }

    suspend fun markOpened(context: Context, letterId: String) {
        context.letterDataStore.edit { prefs ->
            val existing = prefs[KEY_LETTERS]?.toLetterList() ?: emptyList()
            val updated  = existing.map { if (it.id == letterId) it.copy(isOpened = true) else it }
            prefs[KEY_LETTERS] = updated.toJsonString()
        }
    }

    suspend fun deleteLetter(context: Context, letterId: String) {
        context.letterDataStore.edit { prefs ->
            val existing = prefs[KEY_LETTERS]?.toLetterList() ?: emptyList()
            prefs[KEY_LETTERS] = existing.filterNot { it.id == letterId }.toJsonString()
        }
        LetterAlarmScheduler.cancel(context.applicationContext, letterId)
    }
}