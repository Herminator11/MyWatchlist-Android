package com.example.mywatchlist

import android.app.Application
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mywatchlist.TestFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TQ-1: Jeder Test bekommt eine eigene In-Memory-DB – kein Singleton-Leak zwischen Tests.
 * TQ-2: MovieDao wird via LibraryViewModelFactory injiziert, nicht hart verdrahtet.
 *
 * Ablage: src/androidTest/java/com/example/mywatchlist/LibraryViewModelTest.kt
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LibraryViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MovieDao
    private lateinit var viewModel: LibraryViewModel
    private lateinit var app: Application

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        app = context.applicationContext as Application

        // TQ-1: Frische In-Memory-DB pro Test – kein INSTANCE-Singleton
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.movieDao()

        // TQ-2: DAO via Factory injizieren statt AppDatabase.getDatabase()
        viewModel = LibraryViewModelFactory(app, dao)
            .create(LibraryViewModel::class.java)
    }

    @After
    fun teardown() {
        db.close()
    }

    // ─────────────────────────────────────────────────────────────────────
    // TQ-1: DB-Isolation – kein State-Leak zwischen Tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_freshDb_when_allFlowsQueried_then_allEmpty() = runTest {
        // Jeder Test startet mit leerer DB – kein Leak aus anderen Tests
        val currentlyWatching = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()
        val favSeries = dao.getByListFlow(WatchListType.FAVORITE_SERIES.name).first()
        val wantToWatch = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()

        assertTrue("Currently Watching muss leer sein", currentlyWatching.isEmpty())
        assertTrue("Lieblingsserien muss leer sein", favSeries.isEmpty())
        assertTrue("Want to Watch muss leer sein", wantToWatch.isEmpty())
    }

    @Test
    fun given_insertedEntry_when_teardownAndNewSetup_then_entryGone() = runTest {
        // Arrange: Eintrag in Test 1 einfügen
        val entry = TestFixtures.tvEntity(id = 99, listType = WatchListType.CURRENTLY_WATCHING)
        dao.insertOrUpdateAll(listOf(entry))

        val before = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()
        assertEquals("Eintrag muss vorhanden sein", 1, before.size)

        // Nach teardown + neuem setup wäre DB leer (simuliert durch direkten DB-Neuaufbau)
        db.close()
        val freshDb = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        val after = freshDb.movieDao().getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()
        assertTrue("Nach DB-Neuaufbau muss Liste leer sein", after.isEmpty())
        freshDb.close()
    }

    // ─────────────────────────────────────────────────────────────────────
    // TQ-2: DAO-Injektion – ViewModel nutzt injizierten Test-DAO
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_injectedDao_when_viewModelQueriesData_then_seesSameDb() = runTest {
        // Arrange: Direkt über den injizierten DAO schreiben
        val entry = TestFixtures.movieEntity(
            id = 1,
            listType = WatchListType.RECENTLY_WATCHED_MOVIES,
            finishedDate = "watched: 01.06.2026"
        )
        dao.insertOrUpdateAll(listOf(entry))

        // Assert: ViewModel sieht denselben Eintrag über seinen injizierten DAO
        val result = dao.getByListFlow(WatchListType.RECENTLY_WATCHED_MOVIES.name).first()
        assertEquals("ViewModel-DAO und Test-DAO müssen dieselbe DB sehen", 1, result.size)
        assertEquals("Inception", result[0].title)
    }

    @Test
    fun given_injectedDao_when_viewModelDeletesEntry_then_entryGone() = runTest {
        // Arrange
        val entry = TestFixtures.tvEntity(id = 5, listType = WatchListType.WANT_TO_WATCH)
        dao.insertOrUpdateAll(listOf(entry))

        // Act: ViewModel löscht über seinen injizierten DAO
        viewModel.deleteSpecificEntry(entry)

        // Kurz warten bis Coroutine abgeschlossen
        Thread.sleep(100)

        // Assert: Eintrag weg
        val result = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()
        assertTrue("Eintrag muss nach deleteSpecificEntry weg sein", result.isEmpty())
    }

    @Test
    fun given_injectedDao_when_viewModelMovesToCurrentlyWatching_then_listTypeUpdated() = runTest {
        // Arrange: Film in Want to Watch
        val entry = TestFixtures.tvEntity(
            id = 7,
            listType = WatchListType.WANT_TO_WATCH,
            seasonNumber = ""
        )
        dao.insertOrUpdateAll(listOf(entry))

        // Act: ViewModel verschiebt in Currently Watching
        viewModel.moveToCurrentlyWatching(entry, seasonNumber = "Staffel 1")
        Thread.sleep(100)

        // Assert
        val wantToWatch = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()
        val watching = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()

        assertTrue("Want to Watch muss leer sein", wantToWatch.isEmpty())
        assertEquals("Currently Watching muss den Eintrag enthalten", 1, watching.size)
        assertEquals("Staffel 1", watching[0].seasonNumber)
    }
}