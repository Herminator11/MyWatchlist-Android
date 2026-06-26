package com.example.mywatchlist

import com.example.mywatchlist.utils.toTmdbMovie
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für MovieEntity – Datenmodell und Composite Key Logik.
 * Keine Abhängigkeiten, läuft als reine JVM Tests.
 */
class MovieEntityTest {

    private fun baseEntity(
        id: Int = 1,
        listType: String = WatchListType.CURRENTLY_WATCHING.name,
        seasonNumber: String = ""
    ) = MovieEntity(
        id = id,
        title = "Daredevil",
        date = "2024",
        posterPath = null,
        mediaType = "tv",
        listType = listType,
        seasonNumber = seasonNumber
    )

    // ─────────────────────────────────────────────
    // Composite Key Tests
    // ─────────────────────────────────────────────

    @Test
    fun `zwei Staffeln derselben Serie haben unterschiedliche Keys`() {
        val staffel1 = baseEntity(
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            seasonNumber = "Staffel 1"
        )
        val staffel2 = baseEntity(
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            seasonNumber = "Staffel 2"
        )

        // Gleiche ID aber verschiedene seasonNumber → unterschiedliche Einträge
        assertEquals(staffel1.id, staffel2.id)
        assertNotEquals(staffel1.seasonNumber, staffel2.seasonNumber)
    }

    @Test
    fun `gleiche Serie in zwei verschiedenen Listen hat unterschiedliche Keys`() {
        val currentlyWatching = baseEntity(
            listType = WatchListType.CURRENTLY_WATCHING.name,
            seasonNumber = "Staffel 3"
        )
        val recentlyWatched = baseEntity(
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            seasonNumber = "Staffel 3"
        )

        assertEquals(currentlyWatching.id, recentlyWatched.id)
        assertNotEquals(currentlyWatching.listType, recentlyWatched.listType)
    }

    @Test
    fun `Favorit hat leere seasonNumber`() {
        val favorit = baseEntity(
            listType = WatchListType.FAVORITE_SERIES.name,
            seasonNumber = ""
        )
        assertEquals("", favorit.seasonNumber)
    }

    @Test
    fun `copy behaelt seasonNumber bei`() {
        val original = baseEntity(seasonNumber = "Staffel 4 Teil 2")
        val kopie = original.copy(listType = WatchListType.RECENTLY_WATCHED_TV.name)

        assertEquals("Staffel 4 Teil 2", kopie.seasonNumber)
    }

    @Test
    fun `copy mit finishedDate aendert keine anderen Felder`() {
        val original = baseEntity(seasonNumber = "Staffel 2")
        val finished = original.copy(finishedDate = "finished: 01.06.2026")

        assertEquals(original.id, finished.id)
        assertEquals(original.seasonNumber, finished.seasonNumber)
        assertEquals(original.listType, finished.listType)
        assertEquals("finished: 01.06.2026", finished.finishedDate)
    }

    // ─────────────────────────────────────────────
    // Default-Werte Tests
    // ─────────────────────────────────────────────

    @Test
    fun `neue Entity hat sortOrder MAX_VALUE als Default`() {
        val entity = baseEntity()
        assertEquals(Int.MAX_VALUE, entity.sortOrder)
    }

    @Test
    fun `neue Entity ist nicht favorisiert per Default`() {
        val entity = baseEntity()
        assertFalse(entity.isFavorite)
    }

    @Test
    fun `neue Entity hat keine Notizen per Default`() {
        val entity = baseEntity()
        assertNull(entity.notes)
    }

    @Test
    fun `neue Entity hat keine favoriteCategory per Default`() {
        val entity = baseEntity()
        assertNull(entity.favoriteCategory)
    }

    // ─────────────────────────────────────────────
    // toTmdbMovie Extension Tests
    // ─────────────────────────────────────────────

    @Test
    fun `toTmdbMovie uebertraegt alle relevanten Felder korrekt`() {
        val entity = MovieEntity(
            id = 42,
            title = "Breaking Bad",
            date = "2008",
            posterPath = "/poster.jpg",
            mediaType = "tv",
            listType = WatchListType.CURRENTLY_WATCHING.name,
            seasonNumber = "Staffel 1"
        )
        val tmdb = entity.toTmdbMovie()

        assertEquals(42, tmdb.id)
        assertEquals("Breaking Bad", tmdb.displayTitle)
        assertEquals("/poster.jpg", tmdb.posterPath)
        assertEquals("tv", tmdb.mediaType)
    }

    @Test
    fun `toTmdbMovie setzt title und name identisch`() {
        val entity = baseEntity().copy(title = "Attack on Titan")
        val tmdb = entity.toTmdbMovie()

        assertEquals(tmdb.title, tmdb.name)
        assertEquals("Attack on Titan", tmdb.title)
    }
}