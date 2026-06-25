package com.ragdev69.hoshimemo.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.ragdev69.hoshimemo.R
import com.ragdev69.hoshimemo.ui.theme.SecretTheme

// ── Sound IDs loaded into SoundPool ──────────────────────────────────────────
private data class LoadedSounds(
    val nav: Int,
    val write: Int,
    val del: Int,
    val todoStart: Int,
    val todoComplete: Int
)

object SoundManager {

    private var soundPool: SoundPool? = null
    private var gothSounds:  LoadedSounds? = null
    private var dragSounds:  LoadedSounds? = null
    private var scifiSounds: LoadedSounds? = null
    private var defaultTodoStart:    Int = 0
    private var defaultTodoComplete: Int = 0

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        val sp = soundPool ?: return

        gothSounds = LoadedSounds(
            nav          = sp.load(context, R.raw.goth_nav, 1),
            write        = sp.load(context, R.raw.goth_write, 1),
            del          = sp.load(context, R.raw.goth_del, 1),
            todoStart    = sp.load(context, R.raw.goth_todo_one, 1),
            todoComplete = sp.load(context, R.raw.goth_todo_two, 1)
        )
        dragSounds = LoadedSounds(
            nav          = sp.load(context, R.raw.drag_nav, 1),
            write        = sp.load(context, R.raw.drag_write, 1),
            del          = sp.load(context, R.raw.drag_del, 1),
            todoStart    = sp.load(context, R.raw.drag_todo_one, 1),
            todoComplete = sp.load(context, R.raw.drag_todo_two, 1)
        )
        scifiSounds = LoadedSounds(
            nav          = sp.load(context, R.raw.scifi_nav, 1),
            write        = sp.load(context, R.raw.scifi_write, 1),
            del          = sp.load(context, R.raw.scifi_del, 1),
            todoStart    = sp.load(context, R.raw.scifi_todo_one, 1),
            todoComplete = sp.load(context, R.raw.scifi_todo_two, 1)
        )
        defaultTodoStart    = sp.load(context, R.raw.todo_one, 1)
        defaultTodoComplete = sp.load(context, R.raw.todo_two, 1)
    }

    fun release() {
        soundPool?.release()
        soundPool     = null
        isInitialized = false
    }

    // ── Public play functions ─────────────────────────────────────────────────

    fun playNav(theme: SecretTheme, muted: Boolean) {
        if (muted || theme == SecretTheme.NONE) return
        play(soundsFor(theme)?.nav)
    }

    fun playWrite(theme: SecretTheme, muted: Boolean) {
        if (muted || theme == SecretTheme.NONE) return
        play(soundsFor(theme)?.write)
    }

    fun playDel(theme: SecretTheme, muted: Boolean) {
        if (muted || theme == SecretTheme.NONE) return
        play(soundsFor(theme)?.del)
    }

    fun playTodoStart(theme: SecretTheme, muted: Boolean) {
        if (muted) return
        val id = if (theme == SecretTheme.NONE) defaultTodoStart else soundsFor(theme)?.todoStart
        play(id)
    }

    fun playTodoComplete(theme: SecretTheme, muted: Boolean) {
        if (muted) return
        val id = if (theme == SecretTheme.NONE) defaultTodoComplete else soundsFor(theme)?.todoComplete
        play(id)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun soundsFor(theme: SecretTheme): LoadedSounds? = when (theme) {
        SecretTheme.GOTHIC        -> gothSounds
        SecretTheme.DRAGON        -> dragSounds
        SecretTheme.SCIFI_FANTASY -> scifiSounds
        SecretTheme.NONE          -> null
    }

    private fun play(soundId: Int?) {
        val id = soundId ?: return
        if (id == 0) return
        soundPool?.play(id, 1f, 1f, 1, 0, 1f)
    }
}