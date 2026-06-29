package com.example.mywatchlist.utils
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.utils.*

import org.junit.Assert.*
import org.junit.Test

/**
 * TP-2: buildExportContent() ist jetzt eine reine Funktion ohne
 * Context/File-I/O und ohne MovieEntity-Abhängigkeit – läuft als
 * JVM-Unit-Test ohne Emulator.
 *
 * Ablage: src/test/java/com/example/mywatchlist/utils/ExportUtilsTest.kt
 */
class ExportUtilsTest {

    @Test
    fun given_emptyLists_when_buildExportContent_then_onlyHeaderPresent() {
        val content = buildExportContent(
            tvList = emptyList(),
            movieList = emptyList(),
            exportDate = "14.06.2026 10:00"
        )

        assertTrue(content.startsWith("MyWatchlist – History Export"))
        assertTrue(content.contains("Exportiert am: 14.06.2026 10:00"))
        assertFalse("Ohne Einträge darf kein SERIEN-Abschnitt entstehen", content.contains("SERIEN"))
        assertFalse("Ohne Einträge darf kein FILME-Abschnitt entstehen", content.contains("FILME"))
    }

    @Test
    fun given_singleTvEntryWithSeason_when_buildExportContent_then_groupedByYearWithSeasonAndDate() {
        val tvList = listOf(
            ExportEntry(
                title = "Breaking Bad",
                seasonNumber = "Staffel 5",
                finishedDate = "finished: 14.06.2026",
                date = "2008-01-20"
            )
        )

        val content = buildExportContent(tvList = tvList, movieList = emptyList(), exportDate = "14.06.2026 10:00")

        assertTrue(content.contains("SERIEN (1)"))
        assertTrue("Muss nach Jahr aus finishedDate gruppiert werden", content.contains("  2026"))
        assertTrue(
            "Zeile muss Titel, Staffel und Datum (Präfix entfernt) enthalten",
            content.contains("• Breaking Bad – Staffel 5  [14.06.2026]")
        )
    }

    @Test
    fun given_singleMovieEntryWithoutSeason_when_buildExportContent_then_noSeasonSuffix() {
        val movieList = listOf(
            ExportEntry(
                title = "Inception",
                seasonNumber = "",
                finishedDate = "watched: 01.06.2026",
                date = "2010-07-16"
            )
        )

        val content = buildExportContent(tvList = emptyList(), movieList = movieList, exportDate = "14.06.2026 10:00")

        assertTrue(content.contains("FILME (1)"))
        assertFalse("Ohne TV-Einträge darf kein SERIEN-Abschnitt entstehen", content.contains("SERIEN"))
        assertTrue("watched:-Präfix muss entfernt werden", content.contains("• Inception  [01.06.2026]"))
    }

    @Test
    fun given_nullFinishedDate_when_buildExportContent_then_groupedByDateAndNoDateSuffix() {
        val movieList = listOf(
            ExportEntry(title = "Old Movie", seasonNumber = "", finishedDate = null, date = "2010-07-16")
        )

        val content = buildExportContent(tvList = emptyList(), movieList = movieList, exportDate = "14.06.2026 10:00")

        assertTrue("Jahr muss aus date extrahiert werden, wenn finishedDate null ist", content.contains("  2010"))
        assertTrue("Ohne finishedDate darf keine Datumsklammer angezeigt werden", content.contains("• Old Movie\n"))
    }

    @Test
    fun given_noRecognizableYear_when_buildExportContent_then_groupedAsUnbekannt() {
        val movieList = listOf(
            ExportEntry(title = "Mystery Movie", seasonNumber = "", finishedDate = null, date = "n/a")
        )

        val content = buildExportContent(tvList = emptyList(), movieList = movieList, exportDate = "14.06.2026 10:00")

        assertTrue("Ohne erkennbares Jahr muss 'Unbekannt' als Gruppe genutzt werden", content.contains("  Unbekannt"))
        assertTrue(content.contains("Mystery Movie"))
    }

    @Test
    fun given_entriesFromDifferentYears_when_buildExportContent_then_groupedSeparately() {
        val tvList = listOf(
            ExportEntry(title = "Show A", seasonNumber = "", finishedDate = "finished: 01.01.2024", date = "2020-01-01"),
            ExportEntry(title = "Show B", seasonNumber = "", finishedDate = "finished: 15.06.2026", date = "2020-01-01")
        )

        val content = buildExportContent(tvList = tvList, movieList = emptyList(), exportDate = "14.06.2026 10:00")

        assertTrue(content.contains("  2024"))
        assertTrue(content.contains("  2026"))
        assertTrue(content.contains("Show A"))
        assertTrue(content.contains("Show B"))
    }

    // ── Flexibler Listen-Export (buildListExportContent) ──────

    @Test
    fun given_listSections_when_buildListExportContent_then_headerSectionsAndSortedTitles() {
        val content = buildListExportContent(
            documentTitle = "MyWatchlist Export",
            sections = listOf(
                ExportSection(
                    name = "Want to Watch",
                    entries = listOf(
                        ExportEntry("Zodiac", "", null, "2007-03-02"),
                        ExportEntry("Arrival", "", null, "2016-11-11")
                    )
                ),
                ExportSection(name = "Favorite Series", entries = emptyList())
            ),
            exportDate = "29.06.2026 12:00"
        )

        assertTrue(content.startsWith("MyWatchlist Export"))
        assertTrue(content.contains("Exported: 29.06.2026 12:00"))
        assertTrue(content.contains("WANT TO WATCH (2)"))
        assertTrue("Erscheinungsjahr muss aus date stammen", content.contains("• Arrival (2016)"))
        // alphabetisch sortiert → Arrival vor Zodiac
        assertTrue(content.indexOf("Arrival") < content.indexOf("Zodiac"))
        // leere Sektion wird mit Platzhalter dargestellt
        assertTrue(content.contains("FAVORITE SERIES (0)"))
        assertTrue(content.contains("(empty)"))
    }

    @Test
    fun given_tvEntryWithSeasonAndWatchedDate_when_buildListExportContent_then_includesBoth() {
        val content = buildListExportContent(
            documentTitle = "MyWatchlist Export",
            sections = listOf(
                ExportSection(
                    name = "Favorite Series",
                    entries = listOf(
                        ExportEntry("The Wire", "Season 1", "finished: 10.06.2026", "2002-06-02")
                    )
                )
            ),
            exportDate = "29.06.2026 12:00"
        )

        assertTrue(
            content.contains("• The Wire (2002) – Season 1  [10.06.2026]")
        )
    }

    @Test
    fun given_currentExportTimestamp_when_called_then_matchesExpectedFormat() {
        val timestamp = currentExportTimestamp()
        val pattern = Regex("""\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}""")

        assertTrue("Format muss dd.MM.yyyy HH:mm sein, war: $timestamp", pattern.matches(timestamp))
    }
}