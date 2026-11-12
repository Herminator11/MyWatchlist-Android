package com.example.mywatchlist

import com.example.mywatchlist.data.backup.BackupCodec
import com.example.mywatchlist.data.local.MovieEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val entries = listOf(
        MovieEntity(1, "Severance", "2022-02-18", "/p.jpg", "tv", "CURRENTLY_WATCHING", "Season 2", notes = "Ümläute & \"quotes\""),
        MovieEntity(2, "Dune", "2021-10-22", null, "movie", "WANT_TO_WATCH"),
        MovieEntity(3, "Frieren", "2023-09-29", null, "tv", "FAVORITE_SERIES", favoriteCategory = "Anime", isFavorite = true, sortOrder = 0),
        MovieEntity(4, "Dark", "2017-12-01", null, "tv", "RECENTLY_WATCHED_TV", "Season 3", finishedDate = "05.07.2026"),
    )

    @Test fun `round trip keeps every field`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(entries, "2026-11-11T12:00:00"))
        assertEquals(entries, decoded.entries)
        assertEquals(0, decoded.skipped)
        assertEquals("2026-11-11T12:00:00", decoded.exportedAt)
    }

    @Test fun `rejects foreign or broken files`() {
        assertThrows(BackupCodec.InvalidBackupException::class.java) { BackupCodec.decode("not json {") }
        assertThrows(BackupCodec.InvalidBackupException::class.java) { BackupCodec.decode("""{"app":"Other","format":1,"entries":[]}""") }
        assertThrows(BackupCodec.InvalidBackupException::class.java) { BackupCodec.decode("""{"app":"MyWatchlist","format":99,"entries":[]}""") }
        assertThrows(BackupCodec.InvalidBackupException::class.java) { BackupCodec.decode("""{"app":"MyWatchlist","format":0,"entries":[]}""") }
        assertThrows(BackupCodec.InvalidBackupException::class.java) { BackupCodec.decode("""{"app":"MyWatchlist","format":1}""") }
    }

    @Test fun `invalid entries are skipped, missing optional fields get defaults`() {
        val json = """{"app":"MyWatchlist","format":1,"entries":[
            {"id":1,"title":"Ok","media_type":"movie","list_type":"WANT_TO_WATCH"},
            {"id":2,"title":"","media_type":"movie","list_type":"WANT_TO_WATCH"},
            {"id":3,"title":"Bad list","media_type":"movie","list_type":"TRASH"},
            {"title":"No id","media_type":"tv","list_type":"FAVORITE_SERIES"},
            {"id":1,"title":"Duplicate","media_type":"movie","list_type":"WANT_TO_WATCH"}
        ]}"""
        val d = BackupCodec.decode(json)
        assertEquals(1, d.entries.size)
        assertEquals(4, d.skipped)
        val e = d.entries.single()
        assertEquals("", e.seasonNumber)
        assertEquals("", e.date)
        assertEquals(Int.MAX_VALUE, e.sortOrder)
    }

    @Test fun `missing only returns entries not already stored`() {
        val existing = listOf(entries[0], entries[2].copy(title = "changed locally"))
        val missing = BackupCodec.missing(entries, existing)
        assertEquals(listOf(entries[1], entries[3]), missing)
        assertTrue(BackupCodec.missing(entries, entries).isEmpty())
    }
}
