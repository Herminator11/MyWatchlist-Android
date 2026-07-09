package com.example.mywatchlist
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.ui.viewmodel.*
import com.example.mywatchlist.utils.*

import android.app.Application
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mywatchlist.TestFixtures
import com.example.mywatchlist.utils.toTmdbMovie
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MS-1: Error-Handling in ViewModel-Coroutinen.
 * Prüft ob launchIO() Exceptions korrekt in _error surfacet
 * statt sie still zu verschlucken.
 *
 * Ablage: src/androidTest/java/com/example/mywatchlist/ErrorHandlingTest.kt
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ErrorHandlingTest {

    private lateinit var db: AppDatabase
    private lateinit var app: Application

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        app = context.applicationContext as Application
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ─────────────────────────────────────────────────────────────────────
    // FakeDao: wirft bei bestimmten Operationen kontrolliert Exceptions
    // ─────────────────────────────────────────────────────────────────────

    inner class FakeDao(
        private val throwOnInsert: Boolean = false,
        private val throwOnDelete: Boolean = false,
        private val throwOnTransaction: Boolean = false
    ) : MovieDao by db.movieDao() {

        override suspend fun insertOrUpdateMovie(movie: MovieEntity): Long {
            if (throwOnInsert) throw RuntimeException("Simulierter DB-Fehler: Insert")
            return db.movieDao().insertOrUpdateMovie(movie)
        }

        override suspend fun removeSeasonFromList(movieId: Int, listType: String, seasonNumber: String?): Int {
            if (throwOnDelete) throw RuntimeException("Simulierter DB-Fehler: Delete")
            return db.movieDao().removeSeasonFromList(movieId, listType, seasonNumber)
        }

        override suspend fun editCardTransaction(
            oldId: Int,
            oldListType: String,
            oldSeason: String,
            newEntries: List<MovieEntity>,
            updateFavoriteId: Int?,
            newFavoriteStatus: Boolean?,
            removeFavListType: String?,
            favoriteMediaType: String?
        ) {
            if (throwOnTransaction) throw RuntimeException("Simulierter DB-Fehler: Transaction")
            db.movieDao().editCardTransaction(oldId, oldListType, oldSeason, newEntries, updateFavoriteId, newFavoriteStatus, removeFavListType, favoriteMediaType)
        }

        override fun getAllMoviesFlow(): Flow<List<MovieEntity>> = flowOf(emptyList())
        override fun getByListFlow(listType: String): Flow<List<MovieEntity>> = flowOf(emptyList())
        override fun getByListAndMediaFlow(listType: String, mediaType: String): Flow<List<MovieEntity>> = flowOf(emptyList())
        override fun getFavoritesFlow(listTypes: List<String>): Flow<List<MovieEntity>> = flowOf(emptyList())
    }

    // ─────────────────────────────────────────────────────────────────────
    // MS-1: Tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_daoThrowsOnInsert_when_addNewCard_then_errorFlowContainsMessage() = runTest {
        val viewModel = LibraryViewModelFactory(app, FakeDao(throwOnInsert = true))
            .create(LibraryViewModel::class.java)

        val tmdbMovie = TestFixtures.tvEntity(id = 1).toTmdbMovie()
        viewModel.addNewCard(tmdbMovie, WatchListType.CURRENTLY_WATCHING)

        // Kurz warten bis Coroutine abgeschlossen
        Thread.sleep(200)

        val error = viewModel.error.value
        assertNotNull("error darf nicht null sein wenn DAO wirft", error)
        assertEquals("Eintrag konnte nicht hinzugefügt werden", error)
    }

    @Test
    fun given_daoThrowsOnDelete_when_deleteSpecificEntry_then_errorFlowContainsMessage() = runTest {
        val viewModel = LibraryViewModelFactory(app, FakeDao(throwOnDelete = true))
            .create(LibraryViewModel::class.java)

        val entry = TestFixtures.tvEntity(id = 2, listType = WatchListType.WANT_TO_WATCH)
        viewModel.deleteSpecificEntry(entry)

        Thread.sleep(200)

        val error = viewModel.error.value
        assertNotNull("error darf nicht null sein wenn DAO wirft", error)
        assertEquals("Eintrag konnte nicht gelöscht werden", error)
    }

    @Test
    fun given_daoThrowsOnTransaction_when_moveToCurrentlyWatching_then_errorFlowContainsMessage() = runTest {
        val viewModel = LibraryViewModelFactory(app, FakeDao(throwOnTransaction = true))
            .create(LibraryViewModel::class.java)

        val entry = TestFixtures.tvEntity(id = 3, listType = WatchListType.WANT_TO_WATCH)
        viewModel.moveToCurrentlyWatching(entry, "Staffel 1")

        Thread.sleep(200)

        val error = viewModel.error.value
        assertNotNull("error darf nicht null sein wenn DAO wirft", error)
        assertEquals("Eintrag konnte nicht verschoben werden", error)
    }

    @Test
    fun given_errorSet_when_clearError_then_errorFlowIsNull() = runTest {
        val viewModel = LibraryViewModelFactory(app, FakeDao(throwOnInsert = true))
            .create(LibraryViewModel::class.java)

        val tmdbMovie = TestFixtures.tvEntity(id = 4).toTmdbMovie()
        viewModel.addNewCard(tmdbMovie, WatchListType.CURRENTLY_WATCHING)
        Thread.sleep(200)

        assertNotNull("Fehler muss zuerst gesetzt sein", viewModel.error.value)

        // clearError aufrufen
        viewModel.clearError()

        assertNull("Nach clearError muss error null sein", viewModel.error.value)
    }

    @Test
    fun given_noError_when_normalOperation_then_errorFlowRemainsNull() = runTest {
        // Normaler DAO ohne Fehler
        val viewModel = LibraryViewModelFactory(app, db.movieDao())
            .create(LibraryViewModel::class.java)

        val entry = TestFixtures.tvEntity(id = 5, listType = WatchListType.WANT_TO_WATCH)
        db.movieDao().insertOrUpdateAll(listOf(entry))
        viewModel.deleteSpecificEntry(entry)
        Thread.sleep(200)

        assertNull("error muss null bleiben wenn kein Fehler auftritt", viewModel.error.value)
    }
}
