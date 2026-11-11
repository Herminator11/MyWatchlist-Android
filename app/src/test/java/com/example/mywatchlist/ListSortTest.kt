package com.example.mywatchlist

import com.example.mywatchlist.data.CardDensity
import com.example.mywatchlist.data.ListSort
import com.example.mywatchlist.data.enumOrDefault
import com.example.mywatchlist.data.local.MovieEntity
import com.example.mywatchlist.data.sortedFor
import org.junit.Assert.assertEquals
import org.junit.Test

class ListSortTest {

    private fun e(title: String, date: String) = MovieEntity(
        id = title.hashCode(), title = title, date = date, posterPath = null,
        mediaType = "movie", listType = "WANT_TO_WATCH"
    )

    private val list = listOf(e("dune", "2021-10-22"), e("Alien", "1979-05-25"), e("Unknown", ""), e("Arrival", "2016-11-11"), e("blade", "2021-01-01"))

    @Test fun `default keeps stored order`() = assertEquals(list, list.sortedFor(ListSort.DEFAULT))

    @Test fun `title sort ignores case`() =
        assertEquals(listOf("Alien", "Arrival", "blade", "dune", "Unknown"), list.sortedFor(ListSort.TITLE).map { it.title })

    @Test fun `newest first, ties alphabetical, unknown year last`() =
        assertEquals(listOf("blade", "dune", "Arrival", "Alien", "Unknown"), list.sortedFor(ListSort.YEAR_NEWEST).map { it.title })

    @Test fun `oldest first, unknown year last`() =
        assertEquals(listOf("Alien", "Arrival", "blade", "dune", "Unknown"), list.sortedFor(ListSort.YEAR_OLDEST).map { it.title })

    @Test fun `unknown enum names fall back to default`() {
        assertEquals(CardDensity.COMPACT, enumOrDefault("COMPACT", CardDensity.COMFORTABLE))
        assertEquals(CardDensity.COMFORTABLE, enumOrDefault("bogus", CardDensity.COMFORTABLE))
        assertEquals(CardDensity.COMFORTABLE, enumOrDefault<CardDensity>(null, CardDensity.COMFORTABLE))
    }
}
