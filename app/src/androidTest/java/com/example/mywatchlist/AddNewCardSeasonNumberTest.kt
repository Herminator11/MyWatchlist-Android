package com.example.mywatchlist
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.ui.viewmodel.*
import com.example.mywatchlist.utils.*

import android.app.Application
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mywatchlist.TestFixtures
import com.example.mywatchlist.utils.toTmdbMovie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MS-4: seasonNumber-Branch in addNewCard() für WANT_TO_WATCH.
 *
 * Hintergrund: isTvList in addNewCard() prüft NUR den listType, nicht den
 * mediaType. WANT_TO_WATCH ist immer Teil von isTvList – egal ob Film oder
 * Serie. Im Normalfall ist seasonNumber beim Hinzufügen zu "Want to Watch"
 * leer ("") und der Branch hat keine sichtbare Auswirkung.
 *
 * Die letzten beiden Tests prüfen einen Folgefehler-Pfad: moveToCurrentlyWatching()
 * hat oldSeason = "" hartkodiert. Falls ein WANT_TO_WATCH-Eintrag jemals eine
 * nicht-leere seasonNumber hätte, würde der alte Eintrag beim Verschieben
 * NICHT entfernt → verwaister Duplikat-Eintrag.
 *
 * Ablage: src/androidTest/java/com/example/mywatchlist/AddNewCardSeasonNumberTest.kt
 */
@RunWith(AndroidJUnit4::class)
class AddNewCardSeasonNumberTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MovieDao
    private lateinit var viewModel: LibraryViewModel
    private lateinit var app: Application

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        app = context.applicationContext as Application

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.movieDao()

        viewModel = LibraryViewModelFactory(app, dao)
            .create(LibraryViewModel::class.java)
    }

    @After
    fun teardown() {
        db.close()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Normalfall: seasonNumber = "" beim Hinzufügen zu WANT_TO_WATCH
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_tvShow_when_addedToWantToWatch_then_seasonNumberIsEmpty() = runTest {
        val tvShow = TestFixtures.tvEntity(id = 1, listType = WatchListType.WANT_TO_WATCH)
            .toTmdbMovie()

        viewModel.addNewCard(tvShow, WatchListType.WANT_TO_WATCH)
        Thread.sleep(150)

        val result = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()
        assertEquals(1, result.size)
        assertEquals("Serie in Want to Watch muss seasonNumber = \"\" haben", "", result[0].seasonNumber)
        assertEquals("tv", result[0].mediaType)
    }

    @Test
    fun given_movie_when_addedToWantToWatch_then_seasonNumberIsEmpty() = runTest {
        val movie = TestFixtures.movieEntity(id = 100, listType = WatchListType.WANT_TO_WATCH)
            .toTmdbMovie()

        viewModel.addNewCard(movie, WatchListType.WANT_TO_WATCH)
        Thread.sleep(150)

        val result = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()
        assertEquals(1, result.size)
        assertEquals("Film in Want to Watch muss seasonNumber = \"\" haben", "", result[0].seasonNumber)
        assertEquals("movie", result[0].mediaType)
    }

    @Test
    fun given_tvAndMovieInWantToWatch_when_filteredByMediaType_then_correctlySeparated() = runTest {
        val tvShow = TestFixtures.tvEntity(id = 2, listType = WatchListType.WANT_TO_WATCH).toTmdbMovie()
        val movie = TestFixtures.movieEntity(id = 200, listType = WatchListType.WANT_TO_WATCH).toTmdbMovie()

        viewModel.addNewCard(tvShow, WatchListType.WANT_TO_WATCH)
        viewModel.addNewCard(movie, WatchListType.WANT_TO_WATCH)
        Thread.sleep(150)

        val tvResults = dao.getByListAndMediaFlow(WatchListType.WANT_TO_WATCH.name, "tv").first()
        val movieResults = dao.getByListAndMediaFlow(WatchListType.WANT_TO_WATCH.name, "movie").first()

        assertEquals("Nur die Serie darf im TV-Filter sein", 1, tvResults.size)
        assertEquals("Nur der Film darf im Movie-Filter sein", 1, movieResults.size)
        assertEquals(2, tvResults[0].id)
        assertEquals(200, movieResults[0].id)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Normalfall: moveToCurrentlyWatching aus WANT_TO_WATCH (seasonNumber = "")
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_wantToWatchTvEntry_when_movedToCurrentlyWatching_then_oldEntryRemovedAndNewEntryHasSeason() = runTest {
        // Arrange: Serie regulär in Want to Watch (seasonNumber = "")
        val entry = TestFixtures.tvEntity(
            id = 3,
            listType = WatchListType.WANT_TO_WATCH,
            seasonNumber = ""
        )
        dao.insertOrUpdateAll(listOf(entry))

        // Act
        viewModel.moveToCurrentlyWatching(entry, seasonNumber = "Staffel 1")
        Thread.sleep(150)

        // Assert: alter Eintrag weg, neuer Eintrag mit Staffel korrekt
        val wantToWatch = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()
        val watching = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()

        assertTrue("Want to Watch muss leer sein nach dem Verschieben", wantToWatch.isEmpty())
        assertEquals(1, watching.size)
        assertEquals("Staffel 1", watching[0].seasonNumber)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Grenzfall: Was passiert wenn ein WANT_TO_WATCH-Eintrag ausnahmsweise
    // eine nicht-leere seasonNumber hat (z.B. durch Altdaten oder zukünftige
    // UI-Änderung)? moveToCurrentlyWatching() hat oldSeason = "" hartkodiert.
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_wantToWatchEntryWithNonEmptySeasonNumber_when_movedToCurrentlyWatching_then_checkForStaleEntry() = runTest {
        // Arrange: untypischer Fall – WANT_TO_WATCH-Eintrag mit seasonNumber != ""
        val entry = TestFixtures.tvEntity(
            id = 4,
            listType = WatchListType.WANT_TO_WATCH,
            seasonNumber = "Staffel 1"  // untypisch, aber technisch durch isTvList-Branch möglich
        )
        dao.insertOrUpdateAll(listOf(entry))

        // Act
        viewModel.moveToCurrentlyWatching(entry, seasonNumber = "Staffel 1")
        Thread.sleep(150)

        // Assert
        val wantToWatch = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()
        val watching = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()

        // Neuer Eintrag in Currently Watching muss da sein
        assertEquals("Currently Watching muss den neuen Eintrag haben", 1, watching.size)

        // Dieser Assert deckt den potenziellen Bug auf:
        // oldSeason = "" in moveToCurrentlyWatching() entfernt nur Einträge mit
        // seasonNumber = "". Ein WANT_TO_WATCH-Eintrag mit seasonNumber = "Staffel 1"
        // würde NICHT gefunden und bliebe als verwaister Duplikat-Eintrag zurück.
        assertTrue(
            "Want to Watch sollte nach dem Verschieben leer sein – " +
                    "falls dieser Test fehlschlägt, liegt ein verwaister Eintrag vor " +
                    "(oldSeason=\"\" in moveToCurrentlyWatching matcht seasonNumber=\"Staffel 1\" nicht)",
            wantToWatch.isEmpty()
        )
    }
}