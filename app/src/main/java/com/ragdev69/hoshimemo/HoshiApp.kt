package com.ragdev69.hoshimemo

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.Locale

class HoshiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // ── Enable Firestore offline persistence ──────────────
        // This is the PRIMARY fix for notes not appearing immediately.
        // With persistence enabled, writes are committed to local cache
        // instantly and synced to the server in the background.
        // Login also feels faster because cached data loads before the
        // network round-trip completes.
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    override fun attachBaseContext(base: Context) {
        val savedLang = base
            .getSharedPreferences("hoshi_locale", Context.MODE_PRIVATE)
            .getString("lang", "en") ?: "en"
        super.attachBaseContext(wrapLocale(base, savedLang))
    }

    companion object {
        fun wrapLocale(context: Context, langCode: String): Context {
            val locale = Locale(langCode)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocales(LocaleList(locale))
            return context.createConfigurationContext(config)
        }

        fun persistLocale(context: Context, langCode: String) {
            context.getSharedPreferences("hoshi_locale", Context.MODE_PRIVATE)
                .edit()
                .putString("lang", langCode)
                .apply()
        }
    }
}