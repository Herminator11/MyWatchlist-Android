package com.example.mywatchlist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    // Globaler Flow – nur noch für Hilfsfunktionen im ViewModel (getListsForMovie etc.)
    @Query("SELECT * FROM movies_table")
    fun getAllMoviesFlow(): Flow<List<MovieEntity>>

    // ── Gefilterte Flows pro Screen ───────────────────────────
    // SQL filtert direkt in der DB – kein Filtern mehr im UI-Thread

    @Query("SELECT * FROM movies_table WHERE listType = :listType")
    fun getByListFlow(listType: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies_table WHERE listType = :listType AND mediaType = :mediaType")
    fun getByListAndMediaFlow(listType: String, mediaType: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies_table WHERE listType IN (:listTypes) ORDER BY sortOrder ASC")
    fun getFavoritesFlow(listTypes: List<String>): Flow<List<MovieEntity>>

    // ── Atomare Transaktion für editCard ──────────────────────
    // Alle Schreiboperationen in einem einzigen DB-Commit →
    // genau ein Flow-Emit → eine Recomposition statt mehrerer

    @Transaction
    suspend fun editCardTransaction(
        oldId: Int,
        oldListType: String,
        oldSeason: String,
        newEntries: List<MovieEntity>,
        updateFavoriteId: Int? = null,
        newFavoriteStatus: Boolean? = null,
        removeFavListType: String? = null
    ) {
        removeSeasonFromList(oldId, oldListType, oldSeason)

        // Favoriten-Entfernung jetzt atomar innerhalb derselben Transaktion
        if (removeFavListType != null) {
            removeMovieFromList(oldId, removeFavListType)
        }

        if (updateFavoriteId != null && newFavoriteStatus != null) {
            updateFavoriteStatus(updateFavoriteId, newFavoriteStatus)
        }

        insertOrUpdateAll(newEntries)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMovie(movie: MovieEntity): Long

    @Delete
    suspend fun deleteMovie(movie: MovieEntity): Int

    @Query("DELETE FROM movies_table WHERE id = :movieId AND listType = :listType")
    suspend fun removeMovieFromList(movieId: Int, listType: String): Int

    @Query("DELETE FROM movies_table WHERE id = :movieId AND listType = :listType AND seasonNumber = :seasonNumber")
    suspend fun removeSeasonFromList(movieId: Int, listType: String, seasonNumber: String?): Int

    @Query("UPDATE movies_table SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun updateFavoriteStatus(movieId: Int, isFavorite: Boolean): Int

    @Query("UPDATE movies_table SET notes = :notes WHERE id = :movieId AND listType = :listType AND seasonNumber = :seasonNumber")
    suspend fun updateNotes(movieId: Int, listType: String, seasonNumber: String?, notes: String?): Int

    // NEU: sortOrder eines einzelnen Eintrags updaten
    @Query("UPDATE movies_table SET sortOrder = :sortOrder WHERE id = :movieId AND listType = :listType AND seasonNumber = :seasonNumber")
    suspend fun updateSortOrder(movieId: Int, listType: String, seasonNumber: String, sortOrder: Int): Int

    // NEU: Alle sortOrders einer Liste auf einmal updaten (nach Drag & Drop)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(movies: List<MovieEntity>): LongArray
}