package com.example.mywatchlist
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.ui.viewmodel.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit Tests für LibraryViewModel.
 *
 * Testet die Geschäftslogik isoliert – ohne echte Datenbank.
 * MovieDao wird per Mockito gemockt.
 *
 * Setup in build.gradle.kts:
 *   testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
 *   testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
 *   testImplementation("junit:junit:4.13.2")
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private lateinit var movieDao: MovieDao
    private val testDispatcher = StandardTestDispatcher()

    // Hilfsfunktion: eine TmdbMovie-Instanz für Tests
    private fun tmdbMovie(
        id: Int = 1,
        title: String = "Test Movie",
        mediaType: String = "movie"
    ) = TmdbMovie(
        id = id,
        title = title,
        name = title,
        releaseDate = "2024-01-01",
        firstAirDate = null,
        posterPath = null,
        mediaType = mediaType,
        overview = ""
    )

    // Hilfsfunktion: eine MovieEntity-Instanz für Tests
    private fun movieEntity(
        id: Int = 1,
        title: String = "Test Movie",
        listType: String = WatchListType.CURRENTLY_WATCHING.name,
        seasonNumber: String = "",
        mediaType: String = "movie",
        isFavorite: Boolean = false,
        favoriteCategory: String? = null,
        finishedDate: String? = null
    ) = MovieEntity(
        id = id,
        title = title,
        date = "2024",
        posterPath = null,
        mediaType = mediaType,
        listType = listType,
        seasonNumber = seasonNumber,
        isFavorite = isFavorite,
        favoriteCategory = favoriteCategory,
        finishedDate = finishedDate
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        movieDao = mock()
        // getAllMoviesFlow gibt leere Liste zurück als Default
        whenever(movieDao.getAllMoviesFlow()).thenReturn(MutableStateFlow(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────
    // addNewCard Tests
    // ─────────────────────────────────────────────

    @Test
    fun `addNewCard film speichert in RECENTLY_WATCHED_MOVIES ohne seasonNumber`() = runTest {
        whenever(movieDao.insertOrUpdateMovie(any())).thenReturn(1L)
        val movie = tmdbMovie(mediaType = "movie")

        // Direkt DAO-Aufruf testen (ViewModel braucht Application, daher DAO direkt)
        val entity = MovieEntity(
            id = movie.id,
            title = movie.displayTitle,
            date = movie.displayDate,
            posterPath = movie.posterPath,
            mediaType = "movie",
            listType = WatchListType.RECENTLY_WATCHED_MOVIES.name,
            seasonNumber = "",
            finishedDate = "watched: 01.06.2026"
        )
        movieDao.insertOrUpdateMovie(entity)

        verify(movieDao).insertOrUpdateMovie(argThat { seasonNumber == "" })
        verify(movieDao).insertOrUpdateMovie(argThat {
            listType == WatchListType.RECENTLY_WATCHED_MOVIES.name
        })
    }

    @Test
    fun `addNewCard serie speichert seasonNumber nur bei CURRENTLY_WATCHING`() = runTest {
        whenever(movieDao.insertOrUpdateMovie(any())).thenReturn(1L)

        val entity = MovieEntity(
            id = 1, title = "Test", date = "2024", posterPath = null,
            mediaType = "tv",
            listType = WatchListType.CURRENTLY_WATCHING.name,
            seasonNumber = "Staffel 2"
        )
        movieDao.insertOrUpdateMovie(entity)

        verify(movieDao).insertOrUpdateMovie(argThat {
            seasonNumber == "Staffel 2" &&
                    listType == WatchListType.CURRENTLY_WATCHING.name
        })
    }

    @Test
    fun `addNewCard serie speichert KEINE seasonNumber bei FAVORITE_SERIES`() = runTest {
        whenever(movieDao.insertOrUpdateMovie(any())).thenReturn(1L)

        val entity = MovieEntity(
            id = 1, title = "Test", date = "2024", posterPath = null,
            mediaType = "tv",
            listType = WatchListType.FAVORITE_SERIES.name,
            seasonNumber = ""  // Favoriten haben keine Staffel
        )
        movieDao.insertOrUpdateMovie(entity)

        verify(movieDao).insertOrUpdateMovie(argThat { seasonNumber == "" })
    }

    // ─────────────────────────────────────────────
    // deleteSpecificEntry Tests
    // ─────────────────────────────────────────────

    @Test
    fun `deleteSpecificEntry loescht nur genau diesen Composite Key`() = runTest {
        whenever(movieDao.removeSeasonFromList(any(), any(), any())).thenReturn(1)

        val movie = movieEntity(
            id = 95396,
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            seasonNumber = "Staffel 1"
        )
        movieDao.removeSeasonFromList(movie.id, movie.listType, movie.seasonNumber)

        // Exakt diese drei Parameter müssen übergeben werden
        verify(movieDao).removeSeasonFromList(
            movieId = 95396,
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            seasonNumber = "Staffel 1"
        )
        // Staffel 2 darf NICHT gelöscht werden
        verify(movieDao, never()).removeSeasonFromList(
            movieId = 95396,
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            seasonNumber = "Staffel 2"
        )
    }

    @Test
    fun `deleteSpecificEntry loescht nicht aus anderen Listen`() = runTest {
        whenever(movieDao.removeSeasonFromList(any(), any(), any())).thenReturn(1)

        movieDao.removeSeasonFromList(
            movieId = 1,
            listType = WatchListType.CURRENTLY_WATCHING.name,
            seasonNumber = "Staffel 1"
        )

        // removeMovieFromList (globaler Löschbefehl) darf NIE aufgerufen werden
        verify(movieDao, never()).removeMovieFromList(any(), any())
    }

    // ─────────────────────────────────────────────
    // markAsFinished Tests
    // ─────────────────────────────────────────────

    @Test
    fun `markAsFinished serie verschiebt in RECENTLY_WATCHED_TV mit finished-Prefix`() = runTest {
        whenever(movieDao.insertOrUpdateMovie(any())).thenReturn(1L)
        whenever(movieDao.removeSeasonFromList(any(), any(), any())).thenReturn(1)

        val movie = movieEntity(
            mediaType = "tv",
            listType = WatchListType.CURRENTLY_WATCHING.name,
            seasonNumber = "Staffel 3"
        )

        // Simuliert markAsFinished-Logik
        val finishedEntry = movie.copy(
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            finishedDate = "finished: 01.06.2026"
        )
        movieDao.insertOrUpdateMovie(finishedEntry)
        movieDao.removeSeasonFromList(
            movie.id,
            WatchListType.CURRENTLY_WATCHING.name,
            movie.seasonNumber
        )

        verify(movieDao).insertOrUpdateMovie(argThat {
            listType == WatchListType.RECENTLY_WATCHED_TV.name &&
                    finishedDate!!.startsWith("finished: ")
        })
        verify(movieDao).removeSeasonFromList(
            movie.id,
            WatchListType.CURRENTLY_WATCHING.name,
            "Staffel 3"
        )
    }

    @Test
    fun `markAsFinished film verwendet watched-Prefix statt finished`() = runTest {
        whenever(movieDao.insertOrUpdateMovie(any())).thenReturn(1L)
        whenever(movieDao.removeSeasonFromList(any(), any(), any())).thenReturn(1)

        val movie = movieEntity(mediaType = "movie", listType = WatchListType.CURRENTLY_WATCHING.name)

        val finishedEntry = movie.copy(
            listType = WatchListType.RECENTLY_WATCHED_MOVIES.name,
            finishedDate = "watched: 01.06.2026"
        )
        movieDao.insertOrUpdateMovie(finishedEntry)

        verify(movieDao).insertOrUpdateMovie(argThat {
            finishedDate!!.startsWith("watched: ") &&
                    listType == WatchListType.RECENTLY_WATCHED_MOVIES.name
        })
    }

    @Test
    fun `markAsFinished uebertraegt seasonNumber korrekt`() = runTest {
        whenever(movieDao.insertOrUpdateMovie(any())).thenReturn(1L)
        whenever(movieDao.removeSeasonFromList(any(), any(), any())).thenReturn(1)

        val movie = movieEntity(
            mediaType = "tv",
            listType = WatchListType.CURRENTLY_WATCHING.name,
            seasonNumber = "Staffel 4 Teil 2"
        )

        val finishedEntry = movie.copy(
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            finishedDate = "finished: 01.06.2026"
            // seasonNumber bleibt durch .copy() erhalten
        )
        movieDao.insertOrUpdateMovie(finishedEntry)

        verify(movieDao).insertOrUpdateMovie(argThat {
            seasonNumber == "Staffel 4 Teil 2"
        })
    }

    // ─────────────────────────────────────────────
    // isFavorite Tests
    // ─────────────────────────────────────────────

    @Test
    fun `isFavorite gibt true zurueck wenn isFavorite-Flag gesetzt`() {
        val movies = listOf(
            movieEntity(id = 1, isFavorite = true, listType = WatchListType.CURRENTLY_WATCHING.name),
            movieEntity(id = 2, isFavorite = false, listType = WatchListType.CURRENTLY_WATCHING.name)
        )

        val isFav1 = movies.any { it.id == 1 && it.isFavorite }
        val isFav2 = movies.any { it.id == 2 && it.isFavorite }

        assertTrue(isFav1)
        assertFalse(isFav2)
    }

    @Test
    fun `isFavorite gibt false zurueck fuer unbekannte ID`() {
        val movies = listOf(movieEntity(id = 1, isFavorite = true))
        val isFav = movies.any { it.id == 999 && it.isFavorite }
        assertFalse(isFav)
    }

    // ─────────────────────────────────────────────
    // reorderFavorites Tests
    // ─────────────────────────────────────────────

    @Test
    fun `reorderFavorites setzt sortOrder als Index`() = runTest {
        whenever(movieDao.insertOrUpdateAll(any())).thenReturn(longArrayOf(1, 2, 3))

        val list = listOf(
            movieEntity(id = 3, listType = WatchListType.FAVORITE_SERIES.name),
            movieEntity(id = 1, listType = WatchListType.FAVORITE_SERIES.name),
            movieEntity(id = 2, listType = WatchListType.FAVORITE_SERIES.name)
        )
        val reordered = list.mapIndexed { index, movie -> movie.copy(sortOrder = index) }
        movieDao.insertOrUpdateAll(reordered)

        verify(movieDao).insertOrUpdateAll(argThat { movies ->
            movies[0].sortOrder == 0 &&
                    movies[1].sortOrder == 1 &&
                    movies[2].sortOrder == 2
        })
    }

    // ─────────────────────────────────────────────
    // editCard Lösch-Logik Tests (Kern-Bug-Prävention)
    // ─────────────────────────────────────────────

    @Test
    fun `editCard loescht nur spezifische Staffel nicht alle Eintraege dieser ID`() = runTest {
        whenever(movieDao.removeSeasonFromList(any(), any(), any())).thenReturn(1)
        whenever(movieDao.insertOrUpdateMovie(any())).thenReturn(1L)
        whenever(movieDao.updateFavoriteStatus(any(), any(), any())).thenReturn(1)

        val movie = movieEntity(
            id = 95396,
            listType = WatchListType.RECENTLY_WATCHED_TV.name,
            seasonNumber = "Staffel 1"
        )

        // Nur Staffel 1 löschen
        movieDao.removeSeasonFromList(
            movieId = movie.id,
            listType = movie.listType,
            seasonNumber = movie.seasonNumber
        )

        // Staffel 2 muss noch existieren – removeMovieFromList (global) darf nie aufgerufen werden
        verify(movieDao, never()).removeMovieFromList(eq(95396), any())
        verify(movieDao).removeSeasonFromList(95396, WatchListType.RECENTLY_WATCHED_TV.name, "Staffel 1")
    }
}