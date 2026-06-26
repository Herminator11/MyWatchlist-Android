package com.example.mywatchlist

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
import com.example.mywatchlist.utils.parseSortDate
import com.example.mywatchlist.utils.buildExportContent
import com.example.mywatchlist.utils.currentExportTimestamp
import com.example.mywatchlist.utils.ExportEntry

enum class WatchListType(val displayName: String) {
    WANT_TO_WATCH("Want to Watch"),
    CURRENTLY_WATCHING("Currently Watching"),
    RECENTLY_WATCHED_TV("Recently Watched"),
    RECENTLY_WATCHED_MOVIES("Recently Watched Movies"),
    FAVORITE_MOVIES("Lieblingsfilme"),
    FAVORITE_SERIES("Lieblingsserien")
}

class LibraryViewModel(
    application: Application,
    private val movieDao: MovieDao = AppDatabase.getDatabase(application).movieDao()
) : AndroidViewModel(application) {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
        errorMsg: String = "Ein Fehler ist aufgetreten",
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
        launchIO("Eintrag konnte nicht hinzugefügt werden") {
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
        launchIO("Eintrag konnte nicht als gesehen markiert werden") {
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

    fun deleteSpecificEntry(movie: MovieEntity) {
        launchIO("Eintrag konnte nicht gelöscht werden") {
                movieDao.removeSeasonFromList(
                    movieId = movie.id,
                    listType = movie.listType,
                    seasonNumber = movie.seasonNumber
                )
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
        launchIO("Änderungen konnten nicht gespeichert werden") {
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
        launchIO("Kategorie konnte nicht aktualisiert werden") {
                movieDao.insertOrUpdateMovie(movie.copy(favoriteCategory = newCategory))
        }
    }

    fun removeFromFavorites(movie: MovieEntity) {
        launchIO("Favorit konnte nicht entfernt werden") {
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
        launchIO("Notiz konnte nicht gespeichert werden") {
                movieDao.updateNotes(movie.id, movie.listType, movie.seasonNumber, notes)
        }
    }

    // ── Drag & Drop: nur sortOrder updaten, nicht gesamte Zeile ersetzen ──

    fun reorderFavorites(reorderedList: List<MovieEntity>) {
        launchIO("Reihenfolge konnte nicht gespeichert werden") {
            reorderedList.forEachIndexed { index, movie ->
                    movieDao.updateSortOrder(movie.id, movie.listType, movie.seasonNumber, index)
            }
        }
    }

    // ── Want to Watch → Currently Watching ───────────────────

    fun moveToCurrentlyWatching(movie: MovieEntity, seasonNumber: String) {
        launchIO("Eintrag konnte nicht verschoben werden") {
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
        launchIO("Eintrag konnte nicht verschoben werden") {
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