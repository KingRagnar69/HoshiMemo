package com.ragdev69.hoshimemo.utils

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)

object LocaleHelper {
    val supportedLanguages = listOf(
        Language("en", "English",    "English",           "🇺🇸"),
        Language("ja", "Japanese",   "日本語",              "🇯🇵"),
        Language("ko", "Korean",     "한국어",               "🇰🇷"),
        Language("zh", "Chinese",    "中文",               "🇨🇳"),
        Language("hi", "Hindi",      "हिन्दी",               "🇮🇳"),
        Language("es", "Spanish",    "Español",            "🇪🇸"),
        Language("fr", "French",     "Français",           "🇫🇷"),
        Language("de", "German",     "Deutsch",            "🇩🇪"),
        Language("pt", "Portuguese", "Português",          "🇧🇷"),
        Language("ar", "Arabic",     "العربية",             "🇸🇦"),
        Language("ru", "Russian",    "Русский",            "🇷🇺"),
        Language("id", "Indonesian", "Bahasa Indonesia",   "🇮🇩"),
        Language("tr", "Turkish",    "Türkçe",             "🇹🇷"),
        Language("it", "Italian",    "Italiano",           "🇮🇹"),
        Language("th", "Thai",       "ภาษาไทย",            "🇹🇭"),
        Language("vi", "Vietnamese", "Tiếng Việt",         "🇻🇳"),
        Language("bn", "Bengali",    "বাংলা",               "🇧🇩"),
        Language("nl", "Dutch",      "Nederlands",         "🇳🇱"),
        Language("pl", "Polish",     "Polski",             "🇵🇱"),
        Language("sv", "Swedish",    "Svenska",            "🇸🇪")
    )
}