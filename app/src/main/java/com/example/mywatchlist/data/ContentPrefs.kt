package com.example.mywatchlist.data

import java.util.Locale

// ─────────────────────────────────────────────────────────────
// INHALT / TMDB · Sprache der Titel, Beschreibungen und Genres
// Standard = Englisch (bisheriges Verhalten).
// ─────────────────────────────────────────────────────────────
enum class TmdbLanguage(val code: String?) {
    EN("en-US"), DE("de-DE"), ES("es-ES"), FR("fr-FR"), IT("it-IT"), JA("ja-JP"),
    /** Sprache des Geräts. */
    SYSTEM(null);

    /** TMDB-Sprachcode, z. B. "de-DE". SYSTEM nutzt die Geräte-Locale. */
    fun resolve(device: Locale): String = code ?: run {
        val lang = device.language.takeIf { it.length == 2 } ?: return "en-US"
        val country = device.country.takeIf { it.length == 2 }
        if (country != null) "$lang-$country" else lang
    }
}

data class ContentPrefs(
    val tmdbLanguage: TmdbLanguage = TmdbLanguage.EN,
    /** In der Suche den Originaltitel unter dem Titel zeigen, wenn er abweicht. */
    val showOriginalTitles: Boolean = false,
)
