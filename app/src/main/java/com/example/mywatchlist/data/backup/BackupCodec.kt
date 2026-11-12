package com.example.mywatchlist.data.backup

import com.example.mywatchlist.data.local.MovieEntity
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────
// JSON-BACKUP · Format v1
//
// Grundsatz: Ein Restore darf NIE vorhandene Daten verändern.
// Darum:  • Einlesen über DTOs mit nullable Feldern (Gson umgeht
//           Kotlins Null-Sicherheit) und strikte Validierung pro Eintrag
//         • Wiederherstellen nur als „fehlende Einträge hinzufügen“
//           (siehe MovieDao.insertMissing, OnConflict IGNORE)
// ─────────────────────────────────────────────────────────────
object BackupCodec {
    const val APP_ID = "MyWatchlist"
    const val FORMAT = 1

    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    private val VALID_LISTS = setOf(
        "WANT_TO_WATCH", "CURRENTLY_WATCHING", "RECENTLY_WATCHED_TV",
        "RECENTLY_WATCHED_MOVIES", "FAVORITE_MOVIES", "FAVORITE_SERIES"
    )
    private val VALID_TYPES = setOf("movie", "tv")

    data class BackupFile(
        val app: String?,
        val format: Int?,
        @SerializedName("exported_at") val exportedAt: String?,
        val entries: List<BackupEntry>?,
    )

    data class BackupEntry(
        val id: Int?, val title: String?, val date: String?,
        @SerializedName("poster_path") val posterPath: String?,
        @SerializedName("media_type") val mediaType: String?,
        @SerializedName("list_type") val listType: String?,
        @SerializedName("season_number") val seasonNumber: String?,
        @SerializedName("finished_date") val finishedDate: String?,
        @SerializedName("favorite_category") val favoriteCategory: String?,
        @SerializedName("is_favorite") val isFavorite: Boolean?,
        val notes: String?,
        @SerializedName("sort_order") val sortOrder: Int?,
    )

    /** Ergebnis des Einlesens: gültige Einträge + Anzahl übersprungener. */
    data class Decoded(val exportedAt: String?, val entries: List<MovieEntity>, val skipped: Int)

    class InvalidBackupException(message: String) : Exception(message)

    fun encode(entries: List<MovieEntity>, exportedAt: String): String = gson.toJson(
        BackupFile(
            app = APP_ID, format = FORMAT, exportedAt = exportedAt,
            entries = entries.map {
                BackupEntry(
                    it.id, it.title, it.date, it.posterPath, it.mediaType, it.listType, it.seasonNumber,
                    it.finishedDate, it.favoriteCategory, it.isFavorite, it.notes, it.sortOrder
                )
            }
        )
    )

    fun decode(json: String): Decoded {
        val file = try {
            gson.fromJson(json, BackupFile::class.java)
        } catch (e: JsonParseException) {
            throw InvalidBackupException("not valid JSON")
        } ?: throw InvalidBackupException("empty file")
        if (file.app != APP_ID) throw InvalidBackupException("not a MyWatchlist backup")
        if (file.format == null || file.format < 1 || file.format > FORMAT) throw InvalidBackupException("unsupported format ${file.format}")
        val raw = file.entries ?: throw InvalidBackupException("no entries")

        val valid = raw.mapNotNull { it?.toEntityOrNull() }
            .distinctBy { Triple(it.id, it.listType, it.seasonNumber) }
        return Decoded(file.exportedAt, valid, skipped = raw.size - valid.size)
    }

    private fun BackupEntry.toEntityOrNull(): MovieEntity? {
        if (id == null || title.isNullOrBlank() || listType !in VALID_LISTS || mediaType !in VALID_TYPES) return null
        return MovieEntity(
            id = id,
            title = title,
            date = date ?: "",
            posterPath = posterPath,
            mediaType = mediaType!!,
            listType = listType!!,
            seasonNumber = seasonNumber ?: "",
            finishedDate = finishedDate,
            favoriteCategory = favoriteCategory,
            isFavorite = isFavorite ?: false,
            notes = notes,
            sortOrder = sortOrder ?: Int.MAX_VALUE,
        )
    }

    /** Welche Einträge fehlen in der aktuellen Datenbank? (Schlüssel = Primärschlüssel) */
    fun missing(backup: List<MovieEntity>, existing: List<MovieEntity>): List<MovieEntity> {
        val keys = existing.mapTo(HashSet()) { Triple(it.id, it.listType, it.seasonNumber) }
        return backup.filter { Triple(it.id, it.listType, it.seasonNumber) !in keys }
    }
}
