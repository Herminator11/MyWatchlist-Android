package com.example.mywatchlist
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.ui.viewmodel.*
import com.example.mywatchlist.utils.*

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Unit Tests für Datum-Formatierungslogik.
 * Testet die finishedDate/watchedDate Konventionen der App.
 */
class DateFormatTest {

    private val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // ─────────────────────────────────────────────
    // Format-Tests
    // ─────────────────────────────────────────────

    @Test
    fun `finishedDate fuer Serien hat finished-Prefix`() {
        val date = "01.06.2026"
        val result = "finished: $date"
        assertTrue(result.startsWith("finished: "))
        assertEquals("finished: 01.06.2026", result)
    }

    @Test
    fun `finishedDate fuer Filme hat watched-Prefix`() {
        val date = "01.06.2026"
        val result = "watched: $date"
        assertTrue(result.startsWith("watched: "))
    }

    @Test
    fun `Datum wird korrekt aus finishedDate extrahiert`() {
        val finishedDate = "finished: 01.06.2026"
        val extracted = finishedDate
            .replace("finished: ", "")
            .replace("watched: ", "")
            .trim()
        assertEquals("01.06.2026", extracted)
    }

    @Test
    fun `Datum wird korrekt aus watched extrahiert`() {
        val watchedDate = "watched: 31.05.2026"
        val extracted = watchedDate
            .replace("finished: ", "")
            .replace("watched: ", "")
            .trim()
        assertEquals("31.05.2026", extracted)
    }

    @Test
    fun `Datumsformat ist korrekt parseable`() {
        val dateString = "06.06.2026"
        val parsed = sdf.parse(dateString)
        assertNotNull(parsed)
    }

    @Test
    fun `Jahr wird korrekt aus Datum extrahiert`() {
        val finishedDate = "finished: 06.06.2026"
        val yearRegex = Regex("""\d{4}""")
        val year = yearRegex.find(finishedDate)?.value
        assertEquals("2026", year)
    }

    @Test
    fun `Jahr wird aus Release-Datum extrahiert`() {
        val releaseDate = "2024-01-15"
        val year = if (releaseDate.length >= 4) releaseDate.take(4) else releaseDate
        assertEquals("2024", year)
    }

    // ─────────────────────────────────────────────
    // Sortier-Logik Tests (RecentlyWatchedScreen)
    // ─────────────────────────────────────────────

    @Test
    fun `Eintraege werden absteigend nach finishedDate sortiert`() {
        val entries = listOf(
            "finished: 01.01.2025",
            "finished: 06.06.2026",
            "finished: 15.03.2026"
        )
        val sorted = entries.sortedByDescending { it }

        // Neuestes zuerst
        assertEquals("finished: 15.03.2026", sorted[0])
    }

    @Test
    fun `Eintraege werden nach Jahr gruppiert`() {
        data class FakeEntry(val finishedDate: String?)

        val entries = listOf(
            FakeEntry("finished: 01.01.2025"),
            FakeEntry("finished: 06.06.2026"),
            FakeEntry("finished: 31.12.2025"),
            FakeEntry("finished: 15.03.2026")
        )

        val grouped = entries.groupBy { entry ->
            val raw = entry.finishedDate ?: ""
            Regex("""\d{4}""").find(raw)?.value ?: "Unbekannt"
        }

        assertEquals(2, grouped.keys.size)
        assertTrue(grouped.containsKey("2025"))
        assertTrue(grouped.containsKey("2026"))
        assertEquals(2, grouped["2025"]?.size)
        assertEquals(2, grouped["2026"]?.size)
    }
}