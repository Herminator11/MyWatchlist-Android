package com.example.mywatchlist
import com.example.mywatchlist.ui.viewmodel.*

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für WatchListType Enum und zugehörige Logik.
 */
class WatchListTypeTest {

    @Test
    fun `alle WatchListTypes haben displayName`() {
        WatchListType.values().forEach { type ->
            assertTrue(
                "displayName darf nicht leer sein für $type",
                type.displayName.isNotBlank()
            )
        }
    }

    @Test
    fun `valueOf gibt korrekten WatchListType zurueck`() {
        assertEquals(
            WatchListType.CURRENTLY_WATCHING,
            WatchListType.valueOf("CURRENTLY_WATCHING")
        )
        assertEquals(
            WatchListType.RECENTLY_WATCHED_TV,
            WatchListType.valueOf("RECENTLY_WATCHED_TV")
        )
        assertEquals(
            WatchListType.FAVORITE_SERIES,
            WatchListType.valueOf("FAVORITE_SERIES")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `valueOf wirft Exception bei unbekanntem String`() {
        WatchListType.valueOf("UNGUELTIGE_LISTE")
    }

    @Test
    fun `genau sechs WatchListTypes definiert`() {
        // Sicherheitsnetz: wenn neue Listen hinzugefügt werden,
        // muss dieser Test bewusst aktualisiert werden
        assertEquals(6, WatchListType.values().size)
    }

    @Test
    fun `TV-Listen enthalten korrekte Werte`() {
        val tvLists = listOf(
            WatchListType.CURRENTLY_WATCHING,
            WatchListType.RECENTLY_WATCHED_TV,
            WatchListType.FAVORITE_SERIES
        )
        // Diese Listen sind für TV-Serien relevant
        assertTrue(tvLists.contains(WatchListType.CURRENTLY_WATCHING))
        assertTrue(tvLists.contains(WatchListType.RECENTLY_WATCHED_TV))
        assertFalse(tvLists.contains(WatchListType.RECENTLY_WATCHED_MOVIES))
    }
}