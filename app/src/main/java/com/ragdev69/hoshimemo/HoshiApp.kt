package com.ragdev69.hoshimemo

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

class HoshiApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("hoshi_locale", Context.MODE_PRIVATE)
        val langCode = prefs.getString("lang", "en") ?: "en"
        super.attachBaseContext(applyLocale(base, langCode))
    }

    companion object {
        fun applyLocale(context: Context, langCode: String): Context {
            val locale = Locale(langCode)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocales(LocaleList(locale))
            return context.createConfigurationContext(config)
        }

        fun saveLocale(context: Context, langCode: String) {
            context.getSharedPreferences("hoshi_locale", Context.MODE_PRIVATE)
                .edit()
                .putString("lang", langCode)
                .apply()
        }
    }
}