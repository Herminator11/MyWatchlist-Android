package com.example.mywatchlist.widget

import com.example.mywatchlist.data.NavRoutes
import com.example.mywatchlist.data.local.MovieEntity
import com.example.mywatchlist.utils.parseSortDate

// ─────────────────────────────────────────────────────────────
// WIDGET-DATEN · welche Liste, welche Einträge, in welcher Reihenfolge
// Reine Logik (unit-testbar); liest nur – das Widget schreibt nie.
// ─────────────────────────────────────────────────────────────
enum class WidgetList(val route: String) {
    WATCHING(NavRoutes.CURRENTLY_WATCHING),
    WATCHLIST(NavRoutes.WANT_TO_WATCH),
    HISTORY(NavRoutes.RECENTLY_WATCHED),
    FAVORITES(NavRoutes.FAVORITES);

    companion object {
        val Default = WATCHING
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: Default
    }
}

object WidgetData {
    fun select(list: WidgetList, all: List<MovieEntity>): List<MovieEntity> = when (list) {
        // wie in der App: Reihenfolge wie gespeichert
        WidgetList.WATCHING -> all.filter { it.listType == "CURRENTLY_WATCHING" }
        WidgetList.WATCHLIST -> all.filter { it.listType == "WANT_TO_WATCH" }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        // zuletzt Gesehenes zuerst
        WidgetList.HISTORY -> all.filter { it.listType == "RECENTLY_WATCHED_TV" || it.listType == "RECENTLY_WATCHED_MOVIES" }
            .sortedByDescending { parseSortDate(it.finishedDate, it.date) }
        // manuelle Favoriten-Reihenfolge, Serien vor Filmen
        WidgetList.FAVORITES -> all.filter { it.listType == "FAVORITE_SERIES" || it.listType == "FAVORITE_MOVIES" }
            .sortedWith(compareBy<MovieEntity> { it.listType != "FAVORITE_SERIES" }.thenBy { it.sortOrder })
    }

    /** Anzeigetitel inkl. Staffel, wie auf den Karten in der App. */
    fun title(e: MovieEntity): String =
        if (e.mediaType == "tv" && e.seasonNumber.isNotBlank()) "${e.title} · ${e.seasonNumber}" else e.title

    /** Zweite Zeile: Gesehen-Datum im Verlauf, sonst Erscheinungsjahr. */
    fun subtitle(e: MovieEntity): String = e.finishedDate ?: e.date.take(4)
}
