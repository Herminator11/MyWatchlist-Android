package com.example.mywatchlist

import com.example.mywatchlist.data.NavConfig
import com.example.mywatchlist.data.NavRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavConfigTest {

    @Test
    fun `nothing stored gives the old fixed layout`() {
        val c = NavConfig.parse(null, null, null)
        assertEquals(NavRoutes.DEFAULT_ORDER, c.order)
        assertTrue(c.hidden.isEmpty())
        assertEquals(NavRoutes.CURRENTLY_WATCHING, c.start)
    }

    @Test
    fun `stored order round-trips`() {
        val order = listOf("search", "favorites", "currently_watching", "want_to_watch", "recently_watched")
        val c = NavConfig.parse(order.joinToString(","), "", "favorites")
        assertEquals(order, c.order)
        assertEquals("favorites", c.start)
        assertEquals(c, NavConfig.parse(c.orderCsv(), c.hiddenCsv(), c.start))
    }

    @Test
    fun `unknown and duplicate routes are dropped, missing ones appended`() {
        val c = NavConfig.parse("search, bogus ,search,favorites", null, null)
        assertEquals(
            listOf("search", "favorites", "want_to_watch", "currently_watching", "recently_watched"),
            c.order
        )
    }

    @Test
    fun `search can never be hidden`() {
        val c = NavConfig.parse(null, "search,favorites", null)
        assertEquals(setOf("favorites"), c.hidden)
        assertFalse(c.canHide(NavRoutes.SEARCH))
    }

    @Test
    fun `at least two tabs stay visible`() {
        val all = "want_to_watch,currently_watching,recently_watched,favorites"
        val c = NavConfig.parse(null, all, null)
        assertTrue("invalid hidden set is discarded", c.hidden.isEmpty())

        val three = NavConfig.Default
            .setHidden("want_to_watch", true)
            .setHidden("recently_watched", true)
            .setHidden("favorites", true)
        assertEquals(listOf("currently_watching", "search"), three.visible)
        assertFalse(three.canHide("currently_watching"))
    }

    @Test
    fun `start falls back to first visible tab when hidden`() {
        val c = NavConfig.Default.setHidden("currently_watching", true)
        assertEquals("want_to_watch", c.start)
    }

    @Test
    fun `move reorders and keeps everything else`() {
        val c = NavConfig.Default.move(4, 0)
        assertEquals("search", c.order.first())
        assertEquals(5, c.order.size)
        assertEquals(NavConfig.Default.start, c.start)
        assertEquals(c, c.move(9, 0)) // out of range → unchanged
    }
}
