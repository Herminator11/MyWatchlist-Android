package com.example.mywatchlist

import com.example.mywatchlist.data.local.MovieEntity
import com.example.mywatchlist.widget.WidgetData
import com.example.mywatchlist.widget.WidgetList
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetDataTest {
    private fun e(id: Int, title: String, list: String, type: String = "movie", finished: String? = null, sort: Int = Int.MAX_VALUE, season: String = "") =
        MovieEntity(id, title, "2020-01-01", null, type, list, season, finishedDate = finished, sortOrder = sort)

    private val all = listOf(
        e(1, "zeta", "WANT_TO_WATCH"), e(2, "Alpha", "WANT_TO_WATCH"),
        e(3, "Dark", "RECENTLY_WATCHED_TV", "tv", finished = "05.07.2026", season = "Season 3"),
        e(4, "Oppenheimer", "RECENTLY_WATCHED_MOVIES", finished = "14.09.2026"),
        e(5, "Old", "RECENTLY_WATCHED_MOVIES", finished = "20.11.2025"),
        e(6, "Movie fav", "FAVORITE_MOVIES", sort = 0),
        e(7, "Series fav 2", "FAVORITE_SERIES", "tv", sort = 1),
        e(8, "Series fav 1", "FAVORITE_SERIES", "tv", sort = 0),
        e(9, "Severance", "CURRENTLY_WATCHING", "tv", season = "Season 2"),
    )

    @Test fun `watchlist is alphabetical`() =
        assertEquals(listOf("Alpha", "zeta"), WidgetData.select(WidgetList.WATCHLIST, all).map { it.title })

    @Test fun `history shows most recently watched first`() =
        assertEquals(listOf("Oppenheimer", "Dark", "Old"), WidgetData.select(WidgetList.HISTORY, all).map { it.title })

    @Test fun `favorites keep manual order, series first`() =
        assertEquals(listOf("Series fav 1", "Series fav 2", "Movie fav"), WidgetData.select(WidgetList.FAVORITES, all).map { it.title })

    @Test fun `titles and subtitles match the app cards`() {
        val dark = all.first { it.id == 3 }
        assertEquals("Dark · Season 3", WidgetData.title(dark))
        assertEquals("05.07.2026", WidgetData.subtitle(dark))
        assertEquals("2020", WidgetData.subtitle(all.first { it.id == 1 }))
    }

    @Test fun `unknown stored list falls back to watching`() =
        assertEquals(WidgetList.WATCHING, WidgetList.fromName("bogus"))
}
