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