package com.example.mywatchlist.utils
import com.example.mywatchlist.utils.*

import org.junit.Assert.*
import org.junit.Test

/**
 * TP-3: parseSortDate() ist jetzt eine reine String-Funktion ohne
 * Android/Room-Abhängigkeit – läuft als JVM-Unit-Test ohne Emulator.
 *
 * Ablage: src/test/java/com/example/mywatchlist/utils/DateSortUtilsTest.kt
 */
class DateSortUtilsTest {

    @Test
    fun given_finishedDateWithWatchedPrefix_when_parsed_then_returnsSortableFormat() {
        val result = parseSortDate("watched: 01.06.2026", "2026-01-01")
        assertEquals("2026-06-01", result)
    }

    @Test
    fun given_finishedDateWithFinishedPrefix_when_parsed_then_returnsSortableFormat() {
        val result = parseSortDate("finished: 14.06.2026", "2026-01-01")
        assertEquals("2026-06-14", result)
    }

    @Test
    fun given_nullFinishedDate_when_dateIsIsoFormat_then_returnsDateUnchanged() {
        // entity.date ist im TMDB-Format yyyy-MM-dd, enthält keine Punkte -> kein Match -> Fallback
        val result = parseSortDate(null, "2010-07-16")
        assertEquals("2010-07-16", result)
    }

    @Test
    fun given_finishedDateWithoutRecognizablePattern_when_parsed_then_returnsRawUnchanged() {
        val result = parseSortDate("unbekannt", "2010-07-16")
        assertEquals("unbekannt", result)
    }

    @Test
    fun given_singleDigitDayAndMonth_when_zeroPadded_then_parsedCorrectly() {
        val result = parseSortDate("watched: 01.02.2026", "2026-01-01")
        assertEquals("2026-02-01", result)
    }

    @Test
    fun given_multipleEntries_when_sortedDescendingByParseSortDate_then_mostRecentFirst() {
        val entries = listOf(
            "watched: 01.01.2024" to "2024-01-01",
            "watched: 15.06.2026" to "2026-01-01",
            "finished: 20.12.2025" to "2025-01-01"
        )

        val sorted = entries.sortedByDescending { (finishedDate, date) ->
            parseSortDate(finishedDate, date)
        }

        // Erwartete Reihenfolge: 15.06.2026, 20.12.2025, 01.01.2024
        assertEquals("watched: 15.06.2026", sorted[0].first)
        assertEquals("finished: 20.12.2025", sorted[1].first)
        assertEquals("watched: 01.01.2024", sorted[2].first)
    }
}