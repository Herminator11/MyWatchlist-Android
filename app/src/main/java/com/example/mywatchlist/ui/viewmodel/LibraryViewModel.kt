package com.example.mywatchlist.ui.viewmodel
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mywatchlist.utils.toTmdbMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.mywatchlist.utils.parseSortDate
import com.example.mywatchlist.utils.buildExportContent
import com.example.mywatchlist.utils.currentExportTimestamp
import com.example.mywatchlist.utils.ExportEntry

enum class WatchListType(@androidx.annotation.StringRes val labelRes: Int) {
    WANT_TO_WATCH(com.example.mywatchlist.R.string.list_want_to_watch),
    CURRENTLY_WATCHING(com.example.mywatchlist.R.string.list_currently_watching),
    RECENTLY_WATCHED_TV(com.example.mywatchlist.R.string.list_recently_watched),
    RECENTLY_WATCHED_MOVIES(com.example.mywatchlist.R.string.list_recently_watched_movies),
    FAVORITE_MOVIES(com.example.mywatchlist.R.string.list_favorite_movies),
    FAVORITE_SERIES(com.example.mywatchlist.R.string.list_favorite_series)
}

class LibraryViewModel(
    application: Application,
    private val movieDao: MovieDao = AppDatabase.getDatabase(application).movieDao()
) : AndroidViewModel(application) {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Einmal-Events für die UI (z. B. Lösch-Undo-Snackbar)
    private val _events = MutableSharedFlow<LibraryEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    sealed interface LibraryEvent {
        // Trägt die komplette gelöschte Karte zum 1:1-Wiederherstellen
        data class Deleted(val movie: MovieEntity) : LibraryEvent
    }

    // ── Globaler Flow (nur für Hilfsfunktionen) ───────────────
    // Screens benutzen NICHT diesen Flow direkt –
    // sie nutzen die gefilterten StateFlows unten.
    private val allMoviesState: StateFlow<List<MovieEntity>> =
        movieDao.getAllMoviesFlow()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Rückwärtskompatibilität für Screens die noch nicht migriert sind
    val allMovies: StateFlow<List<MovieEntity>> = allMoviesState

    // ── Gefilterte StateFlows pro Screen ─────────────────────
    // Filterung + Sortierung läuft auf Dispatchers.Default,
    // nicht auf dem Main Thread. Jeder Screen subscribt nur auf
    // seinen eigenen Flow – eine Änderung in Liste A recomposiert
    // nicht Liste B oder C.

    val currentlyWatching: StateFlow<List<MovieEntity>> =
        movieDao.getByListFlow(WatchListType.CURRENTLY_WATCHING.name)
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyWatchedTv: StateFlow<List<MovieEntity>> =
        movieDao.getByListFlow(WatchListType.RECENTLY_WATCHED_TV.name)
            .map { list -> list.sortedByDescending { parseSortDate(it.finishedDate, it.date) } }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyWatchedMovies: StateFlow<List<MovieEntity>> =
        movieDao.getByListFlow(WatchListType.RECENTLY_WATCHED_MOVIES.name)
            .map { list -> list.sortedByDescending { parseSortDate(it.finishedDate, it.date) } }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val wantToWatchTv: StateFlow<List<MovieEntity>> =
        movieDao.getByListAndMediaFlow(WatchListType.WANT_TO_WATCH.name, "tv")
            .map { list -> list.sortedBy { it.title } }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val wantToWatchMovies: StateFlow<List<MovieEntity>> =
        movieDao.getByListAndMediaFlow(WatchListType.WANT_TO_WATCH.name, "movie")
            .map { list -> list.sortedBy { it.title } }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteSeries: StateFlow<List<MovieEntity>> =
        movieDao.getFavoritesFlow(listOf(WatchListType.FAVORITE_SERIES.name))
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteMovies: StateFlow<List<MovieEntity>> =
        movieDao.getFavoritesFlow(listOf(WatchListType.FAVORITE_MOVIES.name))
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Hilfsfunktionen ───────────────────────────────────────
    private fun launchIO(
        errorMsg: String = "Something went wrong",
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                _error.value = errorMsg
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun getListsForMovie(movie: TmdbMovie): Set<WatchListType> {
        return allMoviesState.value
            .filter { it.id == movie.id }
            .mapNotNull { entity ->
                try { WatchListType.valueOf(entity.listType) } catch (e: Exception) { null }
            }
            .toSet()
    }

    fun getListsForEntity(entity: MovieEntity): Set<WatchListType> {
        return getListsForMovie(entity.toTmdbMovie())
    }

    fun isFavorite(movieId: Int): Boolean {
        return allMoviesState.value.any { it.id == movieId && it.isFavorite }
    }

    // ── KERN: Einzelne Karte hinzufügen ───────────────────────

    fun addNewCard(
        movie: TmdbMovie,
        listType: WatchListType,
        seasonNumber: String = "",
        finishedDate: String? = null,
        favoriteCategory: String? = null,
        notes: String? = null
    ) {
        launchIO("Could not add entry") {
                val isTvList = listType == WatchListType.CURRENTLY_WATCHING ||
                        listType == WatchListType.RECENTLY_WATCHED_TV ||
                        listType == WatchListType.WANT_TO_WATCH
                val entity = MovieEntity(
                    id = movie.id,
                    title = movie.displayTitle,
                    date = movie.displayDate,
                    posterPath = movie.posterPath,
                    mediaType = movie.mediaType ?: "movie",
                    listType = listType.name,
                    seasonNumber = if (isTvList) seasonNumber else "",
                    finishedDate = finishedDate,
                    favoriteCategory = favoriteCategory,
                    isFavorite = isFavorite(movie.id),
                    notes = notes
                )
                movieDao.insertOrUpdateMovie(entity)
        }
    }

    // ── Als fertig markieren (mit DatePicker-Datum) ───────────

    fun markAsFinished(movie: MovieEntity, finishedDate: String) {
        launchIO("Could not mark entry as watched") {
                val newListType = if (movie.mediaType == "tv")
                    WatchListType.RECENTLY_WATCHED_TV.name
                else
                    WatchListType.RECENTLY_WATCHED_MOVIES.name

                val datePrefix = if (movie.mediaType == "tv") "finished: " else "watched: "

                movieDao.editCardTransaction(
                    oldId = movie.id,
                    oldListType = WatchListType.CURRENTLY_WATCHING.name,
                    oldSeason = movie.seasonNumber,
                    newEntries = listOf(
                        movie.copy(
                            listType = newListType,
                            finishedDate = "$datePrefix$finishedDate"
                        )
                    )
                )
        }
    }

    // ── Spezifischen Eintrag löschen ──────────────────────────

    // notify=false unterdrückt die Undo-Snackbar – z. B. wenn das
    // Löschen selbst schon ein Undo ist (Such-Quick-Add rückgängig).
    fun deleteSpecificEntry(movie: MovieEntity, notify: Boolean = true) {
        launchIO("Could not delete entry") {
                movieDao.removeSeasonFromList(
                    movieId = movie.id,
                    listType = movie.listType,
                    seasonNumber = movie.seasonNumber
                )
                // Snackbar mit "Rückgängig" auslösen (UI sammelt das Event)
                if (notify) _events.emit(LibraryEvent.Deleted(movie))
        }
    }

    // ── Löschen rückgängig machen (Undo-Snackbar) ─────────────
    // Fügt die gelöschte Karte 1:1 wieder ein – inkl. Notizen,
    // Staffel, finishedDate und sortOrder.

    fun undoDelete(movie: MovieEntity) {
        launchIO("Could not restore") {
                movieDao.insertOrUpdateMovie(movie)
        }
    }

    // ── Karte bearbeiten (GlobalEditSheet) ────────────────────
    // Nutzt jetzt editCardTransaction → ein einziger DB-Commit,
    // ein einziger Flow-Emit, eine Recomposition.

    fun editCard(
        movie: MovieEntity,
        newLists: Set<WatchListType>,
        seasonNumber: String,
        finishedDate: String?,
        notes: String?,
        isFavorite: Boolean,
        favoriteCategory: String? = null
    ) {
        launchIO("Could not save changes") {
                val newEntries = mutableListOf<MovieEntity>()
                var removeFavListType: String? = null   // ← NEU

                if (isFavorite != movie.isFavorite) {
                    val favListType = if (movie.mediaType == "tv")
                        WatchListType.FAVORITE_SERIES.name
                    else
                        WatchListType.FAVORITE_MOVIES.name

                    if (isFavorite) {
                        newEntries.add(
                            movie.copy(
                                listType = favListType,
                                seasonNumber = "",
                                isFavorite = true
                            )
                        )
                    } else {
                        removeFavListType = favListType  // ← kein separater DAO-Aufruf mehr
                    }
                }

                newLists.forEach { listType ->
                    val isTvList = listType == WatchListType.CURRENTLY_WATCHING ||
                            listType == WatchListType.RECENTLY_WATCHED_TV
                    newEntries.add(
                        movie.copy(
                            listType = listType.name,
                            seasonNumber = if (isTvList) seasonNumber else "",
                            finishedDate = finishedDate,
                            notes = notes,
                            isFavorite = isFavorite,
                            favoriteCategory = if (listType == WatchListType.FAVORITE_SERIES)
                                favoriteCategory ?: movie.favoriteCategory
                            else movie.favoriteCategory
                        )
                    )
                }

                movieDao.editCardTransaction(
                    oldId = movie.id,
                    oldListType = movie.listType,
                    oldSeason = movie.seasonNumber,
                    newEntries = newEntries,
                    updateFavoriteId = if (isFavorite != movie.isFavorite) movie.id else null,
                    newFavoriteStatus = if (isFavorite != movie.isFavorite) isFavorite else null,
                    removeFavListType = removeFavListType  // ← NEU
                )
        }
    }

    // ── Favoriten-Kategorie updaten ───────────────────────────

    fun updateFavoriteCategory(movie: MovieEntity, newCategory: String) {
        launchIO("Could not update category") {
                movieDao.insertOrUpdateMovie(movie.copy(favoriteCategory = newCategory))
        }
    }

    fun removeFromFavorites(movie: MovieEntity) {
        launchIO("Could not remove favorite") {
                val favoriteListType = if (movie.mediaType == "tv")
                    WatchListType.FAVORITE_SERIES.name
                else
                    WatchListType.FAVORITE_MOVIES.name
                movieDao.removeMovieFromList(movie.id, favoriteListType)
                movieDao.updateFavoriteStatus(movie.id, false)
        }
    }

    // ── Notizen updaten ───────────────────────────────────────

    fun updateNotes(movie: MovieEntity, notes: String?) {
        launchIO("Could not save note") {
                movieDao.updateNotes(movie.id, movie.listType, movie.seasonNumber, notes)
        }
    }

    // ── Drag & Drop: nur sortOrder updaten, nicht gesamte Zeile ersetzen ──

    fun reorderFavorites(reorderedList: List<MovieEntity>) {
        launchIO("Could not save order") {
            reorderedList.forEachIndexed { index, movie ->
                    movieDao.updateSortOrder(movie.id, movie.listType, movie.seasonNumber, index)
            }
        }
    }

    // ── Want to Watch → Currently Watching ───────────────────

    fun moveToCurrentlyWatching(movie: MovieEntity, seasonNumber: String) {
        launchIO("Could not move entry") {
                movieDao.editCardTransaction(
                    oldId = movie.id,
                    oldListType = WatchListType.WANT_TO_WATCH.name,
                    oldSeason = movie.seasonNumber,
                    newEntries = listOf(
                        movie.copy(
                            listType = WatchListType.CURRENTLY_WATCHING.name,
                            seasonNumber = seasonNumber,
                            finishedDate = null
                        )
                    )
                )
        }
    }

    // ── Want to Watch → Recently Watched Movies ───────────────

    fun moveToRecentlyWatched(movie: MovieEntity, watchedDate: String) {
        launchIO("Could not move entry") {
                movieDao.editCardTransaction(
                    oldId = movie.id,
                    oldListType = WatchListType.WANT_TO_WATCH.name,
                    oldSeason = "",
                    newEntries = listOf(
                        movie.copy(
                            listType = WatchListType.RECENTLY_WATCHED_MOVIES.name,
                            seasonNumber = "",
                            finishedDate = "watched: $watchedDate"
                        )
                    )
                )
        }
    }

    // ── History Export ────────────────────────────────────────
    // Generiert eine strukturierte .txt-Datei aus den aktuellen
    // recentlyWatchedTv und recentlyWatchedMovies StateFlows.
    // Gibt den Dateipfad zurück – null wenn beide Listen leer sind.

    fun buildExportFile(context: android.content.Context): java.io.File? {
        val tvList = recentlyWatchedTv.value
        val movieList = recentlyWatchedMovies.value

        if (tvList.isEmpty() && movieList.isEmpty()) return null

        val content = buildExportContent(
            tvList = tvList.map {
                ExportEntry(it.title, it.seasonNumber, it.finishedDate, it.date)
            },
            movieList = movieList.map {
                ExportEntry(it.title, it.seasonNumber, it.finishedDate, it.date)
            },
            exportDate = currentExportTimestamp()
        )

        // Datei in Cache schreiben
        val exportDir = java.io.File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = java.io.File(exportDir, "mywatchlist_history.txt")
        file.writeText(content)
        return file
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.AndroidViewModelFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[APPLICATION_KEY]!!
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(app) as T
            }
        }
    }
}