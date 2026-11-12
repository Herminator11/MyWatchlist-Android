package com.example.mywatchlist.data

import com.example.mywatchlist.data.local.MovieEntity

// ─────────────────────────────────────────────────────────────
// LISTEN-EINSTELLUNGEN · Kartendichte, Metadaten, Sortierung, Start-Tab
// Reine Daten + Sortierlogik (unit-testbar). Sortiert wird nur für die
// Anzeige – an der Datenbank ändert sich nichts.
// ─────────────────────────────────────────────────────────────
enum class CardDensity { COMFORTABLE, COMPACT, POSTER }

enum class ListSort { DEFAULT, TITLE, YEAR_NEWEST, YEAR_OLDEST }

enum class MediaTab { SERIES, MOVIES }

data class ListPrefs(
    val density: CardDensity = CardDensity.COMFORTABLE,
    val showYear: Boolean = true,
    val showTypeTag: Boolean = true,
    /** Watchlist: DEFAULT = bisher (alphabetisch aus dem ViewModel). */
    val watchlistSort: ListSort = ListSort.DEFAULT,
    /** Am Schauen: DEFAULT = Reihenfolge wie gespeichert. */
    val watchingSort: ListSort = ListSort.DEFAULT,
    /** Welcher Watchlist-Tab beim Öffnen aktiv ist. */
    val watchlistStartTab: MediaTab = MediaTab.SERIES,
)

/** Erscheinungsjahr aus "yyyy-MM-dd" (oder null, wenn unbekannt). */
internal fun MovieEntity.releaseYear(): Int? = date.take(4).toIntOrNull()

fun List<MovieEntity>.sortedFor(sort: ListSort): List<MovieEntity> = when (sort) {
    ListSort.DEFAULT -> this
    ListSort.TITLE -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    // Unbekanntes Jahr immer ans Ende, bei Gleichstand alphabetisch
    ListSort.YEAR_NEWEST -> sortedWith(
        compareBy<MovieEntity> { it.releaseYear() == null }
            .thenByDescending { it.releaseYear() ?: 0 }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    )
    ListSort.YEAR_OLDEST -> sortedWith(
        compareBy<MovieEntity> { it.releaseYear() == null }
            .thenBy { it.releaseYear() ?: 0 }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    )
}

inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
