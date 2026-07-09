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
import com.example.mywatchlist.utils.currentExportTimestamp
import com.example.mywatchlist.utils.ExportEntry
import com.example.mywatchlist.utils.ExportSection
import com.example.mywatchlist.utils.buildListExportContent

enum class WatchListType(@androidx.annotation.StringRes val labelRes: Int) {
    WANT_TO_WATCH(com.example.mywatchlist.R.string.list_want_to_watch),
    CURRENTLY_WATCHING(com.example.mywatchlist.R.string.list_currently_watching),
    RECENTLY_WATCHED_TV(com.example.mywatchlist.R.string.list_recently_watched),
    RECENTLY_WATCHED_MOVIES(com.example.mywatchlist.R.string.list_recently_watched_movies),
    FAVORITE_MOVIES(com.example.mywatchlist.R.string.list_favorite_movies),
    FAVORITE_SERIES(com.example.mywatchlist.R.string.list_favorite_series)
}

/**
 * Exportierbare Listen für den Settings-Export. Jeder Eintrag kennt sein
 * UI-Label (für Auswahl-Reihen) und seinen Dateinamens-Stamm.
 */
enum class ExportList(
    @androidx.annotation.StringRes val labelRes: Int,
    val fileStem: String
) {
    WANT_TO_WATCH(com.example.mywatchlist.R.string.list_want_to_watch, "want_to_watch"),
    FAVORITE_SERIES(com.example.mywatchlist.R.string.list_favorite_series, "favorite_series"),
    FAVORITE_MOVIES(com.example.mywatchlist.R.string.list_favorite_movies, "favorite_movies"),
    RECENTLY_WATCHED(com.example.mywatchlist.R.string.list_recently_watched, "recently_watched")
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

    // Identität = id + mediaType. TMDB vergibt IDs nur je Medientyp eindeutig,
    // deshalb darf hier nicht allein nach id gefiltert werden – sonst zeigt eine
    // Film-Karte die Listen einer gleich-nummerierten Serie (und umgekehrt).
    fun getListsForMovie(movie: TmdbMovie): Set<WatchListType> {
        return allMoviesState.value
            .filter { it.id == movie.id && it.mediaType == movie.mediaType }
            .mapNotNull { entity ->
                try { WatchListType.valueOf(entity.listType) } catch (e: Exception) { null }
            }
            .toSet()
    }

    fun getListsForEntity(entity: MovieEntity): Set<WatchListType> {
        return getListsForMovie(entity.toTmdbMovie())
    }

    // Einzige Wahrheit für "ist Favorit": die Mitgliedschaft in einer der
    // FAVORITE_*-Listen – NICHT das denormalisierte isFavorite-Flag, das auf
    // manchen Schreibpfaden veralten kann. Zusätzlich mediaType-scoped.
    fun isFavorite(movieId: Int, mediaType: String): Boolean {
        return allMoviesState.value.any {
            it.id == movieId && it.mediaType == mediaType &&
                (it.listType == WatchListType.FAVORITE_SERIES.name ||
                    it.listType == WatchListType.FAVORITE_MOVIES.name)
        }
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
                val mediaType = movie.mediaType ?: "movie"
                val addingToFavorites = listType == WatchListType.FAVORITE_SERIES ||
                        listType == WatchListType.FAVORITE_MOVIES
                val entity = MovieEntity(
                    id = movie.id,
                    title = movie.displayTitle,
                    date = movie.displayDate,
                    posterPath = movie.posterPath,
                    mediaType = mediaType,
                    listType = listType.name,
                    seasonNumber = if (isTvList) seasonNumber else "",
                    finishedDate = finishedDate,
                    favoriteCategory = favoriteCategory,
                    // Flag spiegelt die Favoriten-Mitgliedschaft (id + mediaType);
                    // beim Anlegen einer Favoritenzeile selbst direkt true.
                    isFavorite = addingToFavorites || isFavorite(movie.id, mediaType),
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
                    removeFavListType = removeFavListType,  // ← NEU
                    favoriteMediaType = if (isFavorite != movie.isFavorite) movie.mediaType else null
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
                movieDao.updateFavoriteStatus(movie.id, movie.mediaType, false)
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

    // ── Flexibler Listen-Export ───────────────────────────────
    // Welche listType-Werte zu welcher exportierbaren Liste gehören.
    private fun listTypesFor(list: ExportList): Set<String> = when (list) {
        ExportList.WANT_TO_WATCH   -> setOf(WatchListType.WANT_TO_WATCH.name)
        ExportList.FAVORITE_SERIES -> setOf(WatchListType.FAVORITE_SERIES.name)
        ExportList.FAVORITE_MOVIES -> setOf(WatchListType.FAVORITE_MOVIES.name)
        ExportList.RECENTLY_WATCHED -> setOf(
            WatchListType.RECENTLY_WATCHED_TV.name,
            WatchListType.RECENTLY_WATCHED_MOVIES.name
        )
    }

    // Einträge einer exportierbaren Liste – direkt aus dem global-hot
    // allMoviesState (Eagerly) gefiltert. Wichtig: NICHT aus den
    // gefilterten Screen-Flows lesen, denn die sind Lazily und liefern
    // ohne aktiven Collector ihren Initialwert (leere Liste) – genau das
    // ließ "Recently Watched" im Export-Screen leer erscheinen.
    private fun entriesFor(list: ExportList): List<MovieEntity> {
        val types = listTypesFor(list)
        return allMoviesState.value.filter { it.listType in types }
    }

    /**
     * Anzahl Einträge je exportierbarer Liste – reaktiv aus allMoviesState,
     * damit die Settings-Reihen automatisch aktivieren/deaktivieren.
     */
    val exportCountsState: StateFlow<Map<ExportList, Int>> =
        allMoviesState
            .map { movies ->
                ExportList.values().associateWith { list ->
                    val types = listTypesFor(list)
                    movies.count { it.listType in types }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    /**
     * Baut den .txt-Inhalt für die ausgewählten Listen. Gibt null zurück,
     * wenn keine der gewählten Listen Einträge enthält (→ "nichts zu
     * exportieren"). Reihenfolge folgt der Enum-Deklaration.
     */
    fun buildExportText(context: android.content.Context, selection: Set<ExportList>): String? {
        val sections = ExportList.values()
            .filter { it in selection }
            .map { list ->
                ExportSection(
                    name = context.getString(list.labelRes),
                    entries = entriesFor(list).map {
                        ExportEntry(it.title, it.seasonNumber, it.finishedDate, it.date)
                    }
                )
            }

        if (sections.all { it.entries.isEmpty() }) return null

        return buildListExportContent(
            documentTitle = context.getString(com.example.mywatchlist.R.string.export_doc_title),
            sections = sections,
            exportDate = currentExportTimestamp()
        )
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