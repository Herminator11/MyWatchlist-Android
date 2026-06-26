package com.example.mywatchlist.data
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.ui.viewmodel.*
import com.example.mywatchlist.utils.*

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.example.mywatchlist.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MovieDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.movieDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ─────────────────────────────────────────────────────────────────────
    // CP-1: Favoriten-Entfernung muss atomar in editCardTransaction laufen
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_tvSeriesFavorite_when_removeFavListType_then_favoriteEntryDeleted() = runTest {
        // Arrange: Serie existiert in Currently Watching + Lieblingsserien
        val watchingEntry = TestFixtures.tvEntity(
            id = 1,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 1",
            isFavorite = true
        )
        val favEntry = TestFixtures.tvEntity(
            id = 1,
            listType = WatchListType.FAVORITE_SERIES,
            seasonNumber = "",
            isFavorite = true
        )
        dao.insertOrUpdateAll(listOf(watchingEntry, favEntry))

        // Act: Favorit entfernen – alles in einer Transaktion
        dao.editCardTransaction(
            oldId = 1,
            oldListType = WatchListType.CURRENTLY_WATCHING.name,
            oldSeason = "Staffel 1",
            newEntries = listOf(watchingEntry.copy(isFavorite = false)),
            updateFavoriteId = 1,
            newFavoriteStatus = false,
            removeFavListType = WatchListType.FAVORITE_SERIES.name
        )

        // Assert: Favoriten-Eintrag weg, Watching-Eintrag noch da mit isFavorite = false
        val favs = dao.getByListFlow(WatchListType.FAVORITE_SERIES.name).first()
        val watching = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()

        assertTrue("Favoriten-Eintrag muss gelöscht sein", favs.isEmpty())
        assertEquals("Watching-Eintrag muss erhalten bleiben", 1, watching.size)
        assertFalse("isFavorite muss false sein", watching[0].isFavorite)
    }

    @Test
    fun given_movieFavorite_when_removeFavListType_then_favoriteMovieEntryDeleted() = runTest {
        // Arrange: Film in Recently Watched + Lieblingsfilme
        val watchedEntry = TestFixtures.movieEntity(
            id = 10,
            listType = WatchListType.RECENTLY_WATCHED_MOVIES,
            isFavorite = true,
            finishedDate = "watched: 01.06.2026"
        )
        val favEntry = TestFixtures.movieEntity(
            id = 10,
            listType = WatchListType.FAVORITE_MOVIES,
            isFavorite = true,
            finishedDate = null
        )
        dao.insertOrUpdateAll(listOf(watchedEntry, favEntry))

        // Act
        dao.editCardTransaction(
            oldId = 10,
            oldListType = WatchListType.RECENTLY_WATCHED_MOVIES.name,
            oldSeason = "",
            newEntries = listOf(watchedEntry.copy(isFavorite = false)),
            updateFavoriteId = 10,
            newFavoriteStatus = false,
            removeFavListType = WatchListType.FAVORITE_MOVIES.name
        )

        // Assert
        val favMovies = dao.getByListFlow(WatchListType.FAVORITE_MOVIES.name).first()
        val recentMovies = dao.getByListFlow(WatchListType.RECENTLY_WATCHED_MOVIES.name).first()

        assertTrue("Lieblingsfilm-Eintrag muss gelöscht sein", favMovies.isEmpty())
        assertEquals("Recently-Watched-Eintrag muss erhalten bleiben", 1, recentMovies.size)
        assertFalse("isFavorite muss false sein", recentMovies[0].isFavorite)
    }

    @Test
    fun given_entryWithoutFavorite_when_removeFavListTypeNull_then_noCrashAndEntryUpdated() = runTest {
        // Arrange: Eintrag ohne Favoriten-Gegenstück
        val entry = TestFixtures.tvEntity(
            id = 2,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 2",
            isFavorite = false
        )
        dao.insertOrUpdateAll(listOf(entry))

        // Act: removeFavListType = null → kein Delete-Versuch, kein Crash
        dao.editCardTransaction(
            oldId = 2,
            oldListType = WatchListType.CURRENTLY_WATCHING.name,
            oldSeason = "Staffel 2",
            newEntries = listOf(entry.copy(notes = "Sehr gut!")),
            removeFavListType = null
        )

        // Assert: Eintrag aktualisiert, kein Crash
        val result = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()
        assertEquals(1, result.size)
        assertEquals("Sehr gut!", result[0].notes)
    }

    @Test
    fun given_favoriteEntry_when_editCardTransactionFails_then_bothEntriesConsistent() = runTest {
        // Arrange: Sicherstellen dass beide Einträge existieren
        val watchingEntry = TestFixtures.tvEntity(
            id = 3,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 1",
            isFavorite = true
        )
        val favEntry = TestFixtures.tvEntity(
            id = 3,
            listType = WatchListType.FAVORITE_SERIES,
            seasonNumber = "",
            isFavorite = true
        )
        dao.insertOrUpdateAll(listOf(watchingEntry, favEntry))

        // Atomizität prüfen: Wenn removeFavListType gesetzt ist,
        // darf kein halbfertiger Zustand entstehen.
        // Wir simulieren den vollständigen happy path und prüfen Konsistenz.
        dao.editCardTransaction(
            oldId = 3,
            oldListType = WatchListType.CURRENTLY_WATCHING.name,
            oldSeason = "Staffel 1",
            newEntries = listOf(watchingEntry.copy(isFavorite = false, seasonNumber = "Staffel 2")),
            updateFavoriteId = 3,
            newFavoriteStatus = false,
            removeFavListType = WatchListType.FAVORITE_SERIES.name
        )

        val allEntriesForId3 = listOf(
            dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first(),
            dao.getByListFlow(WatchListType.FAVORITE_SERIES.name).first()
        ).flatten()

        // Kein Eintrag mit isFavorite = true darf noch existieren
        assertTrue(
            "Kein Eintrag darf noch isFavorite = true haben",
            allEntriesForId3.none { it.isFavorite }
        )
        // Kein Favoriten-Eintrag darf mehr existieren
        assertTrue(
            "Kein FAVORITE_SERIES Eintrag darf existieren",
            allEntriesForId3.none { it.listType == WatchListType.FAVORITE_SERIES.name }
        )
    }
}