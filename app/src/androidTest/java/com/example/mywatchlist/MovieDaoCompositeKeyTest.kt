package com.example.mywatchlist

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mywatchlist.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MS-2: Composite Key REPLACE-Verhalten.
 * Primärschlüssel von movies_table ist (id, listType, seasonNumber).
 * insertOrUpdateAll/insertOrUpdateMovie nutzen OnConflictStrategy.REPLACE.
 *
 * Diese Tests prüfen:
 * 1. Identischer Composite Key → REPLACE (Update, kein Duplikat)
 * 2. Unterschiedlicher Composite Key (z.B. andere seasonNumber/listType) → eigene Zeile
 *
 * Ablage: src/androidTest/java/com/example/mywatchlist/MovieDaoCompositeKeyTest.kt
 */
@RunWith(AndroidJUnit4::class)
class MovieDaoCompositeKeyTest {

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
    // Fall 1: Identischer Composite Key (id, listType, seasonNumber) → REPLACE
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_existingEntry_when_insertWithSameCompositeKey_then_entryReplacedNotDuplicated() = runTest {
        // Arrange: ursprünglicher Eintrag
        val original = TestFixtures.tvEntity(
            id = 1,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 1",
            notes = "Alte Notiz"
        )
        dao.insertOrUpdateMovie(original)

        // Act: gleicher Composite Key (id=1, CURRENTLY_WATCHING, "Staffel 1"), andere Notiz
        val updated = original.copy(notes = "Neue Notiz")
        dao.insertOrUpdateMovie(updated)

        // Assert: genau ein Eintrag, mit aktualisierten Daten
        val result = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()
        assertEquals("Es darf kein Duplikat entstehen", 1, result.size)
        assertEquals("Notiz muss überschrieben sein", "Neue Notiz", result[0].notes)
    }

    @Test
    fun given_existingEntry_when_insertOrUpdateAllWithSameCompositeKey_then_entryReplaced() = runTest {
        // Arrange
        val original = TestFixtures.movieEntity(
            id = 50,
            listType = WatchListType.WANT_TO_WATCH,
            isFavorite = false
        )
        dao.insertOrUpdateAll(listOf(original))

        // Act: gleicher Composite Key (id=50, WANT_TO_WATCH, seasonNumber=""), isFavorite geändert
        val updated = original.copy(isFavorite = true, favoriteCategory = "Klassiker")
        dao.insertOrUpdateAll(listOf(updated))

        // Assert
        val result = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()
        assertEquals("Es darf kein Duplikat entstehen", 1, result.size)
        assertTrue("isFavorite muss überschrieben sein", result[0].isFavorite)
        assertEquals("Klassiker", result[0].favoriteCategory)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fall 2: Unterschiedlicher Composite Key → separate Zeile, kein Überschreiben
    // ─────────────────────────────────────────────────────────────────────

    @Test
    fun given_existingEntry_when_insertWithDifferentSeasonNumber_then_separateRowCreated() = runTest {
        // Arrange: Serie in Staffel 1
        val season1 = TestFixtures.tvEntity(
            id = 2,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 1"
        )
        dao.insertOrUpdateMovie(season1)

        // Act: gleiche id, gleicher listType, ABER andere seasonNumber → eigener Composite Key
        val season2 = TestFixtures.tvEntity(
            id = 2,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 2"
        )
        dao.insertOrUpdateMovie(season2)

        // Assert: beide Einträge existieren parallel
        val result = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()
        assertEquals("Beide Staffeln müssen als separate Einträge existieren", 2, result.size)
        val seasons = result.map { it.seasonNumber }.toSet()
        assertTrue(seasons.contains("Staffel 1"))
        assertTrue(seasons.contains("Staffel 2"))
    }

    @Test
    fun given_movieInOneList_when_insertSameIdDifferentListType_then_separateRowCreated() = runTest {
        // Arrange: Film in Want to Watch
        val wantToWatch = TestFixtures.movieEntity(
            id = 100,
            listType = WatchListType.WANT_TO_WATCH
        )
        dao.insertOrUpdateMovie(wantToWatch)

        // Act: gleiche id, aber FAVORITE_MOVIES als listType → eigener Composite Key
        val favorite = TestFixtures.movieEntity(
            id = 100,
            listType = WatchListType.FAVORITE_MOVIES,
            isFavorite = true
        )
        dao.insertOrUpdateMovie(favorite)

        // Assert: beide Einträge existieren unabhängig voneinander
        val wantToWatchResult = dao.getByListFlow(WatchListType.WANT_TO_WATCH.name).first()
        val favoritesResult = dao.getByListFlow(WatchListType.FAVORITE_MOVIES.name).first()

        assertEquals("Want-to-Watch-Eintrag muss erhalten bleiben", 1, wantToWatchResult.size)
        assertEquals("Favoriten-Eintrag muss separat existieren", 1, favoritesResult.size)
        assertFalse("Want-to-Watch-Eintrag bleibt unverändert", wantToWatchResult[0].isFavorite)
        assertTrue("Favoriten-Eintrag ist favorisiert", favoritesResult[0].isFavorite)
    }

    @Test
    fun given_mixedBatch_when_insertOrUpdateAll_then_onlyMatchingCompositeKeysReplaced() = runTest {
        // Arrange: zwei bestehende Einträge mit unterschiedlichen Composite Keys
        val entryA = TestFixtures.tvEntity(
            id = 3,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 1",
            notes = "A-alt"
        )
        val entryB = TestFixtures.tvEntity(
            id = 3,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 2",
            notes = "B-alt"
        )
        dao.insertOrUpdateAll(listOf(entryA, entryB))

        // Act: Batch-Update – nur entryA wird per identischem Composite Key überschrieben,
        // entryC ist komplett neu (andere seasonNumber)
        val entryAUpdated = entryA.copy(notes = "A-neu")
        val entryC = TestFixtures.tvEntity(
            id = 3,
            listType = WatchListType.CURRENTLY_WATCHING,
            seasonNumber = "Staffel 3",
            notes = "C-neu"
        )
        dao.insertOrUpdateAll(listOf(entryAUpdated, entryC))

        // Assert: 3 Einträge total – A überschrieben, B unverändert, C neu
        val result = dao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name).first()
        assertEquals("Es müssen 3 Einträge existieren (A, B, C)", 3, result.size)

        val byNote = result.associateBy { it.notes }
        assertNotNull("A-neu muss existieren (Replace)", byNote["A-neu"])
        assertNull("A-alt darf nicht mehr existieren", byNote["A-alt"])
        assertNotNull("B-alt muss unverändert bestehen bleiben", byNote["B-alt"])
        assertNotNull("C-neu muss neu hinzugekommen sein", byNote["C-neu"])
    }
}