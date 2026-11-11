package com.example.mywatchlist

import com.example.mywatchlist.data.TmdbLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TmdbLanguageTest {
    @Test fun `fixed languages ignore the device`() =
        assertEquals("de-DE", TmdbLanguage.DE.resolve(Locale.JAPAN))

    @Test fun `system uses device language and country`() =
        assertEquals("de-AT", TmdbLanguage.SYSTEM.resolve(Locale("de", "AT")))

    @Test fun `system without country sends language only`() =
        assertEquals("fr", TmdbLanguage.SYSTEM.resolve(Locale("fr")))

    @Test fun `system with unusable locale falls back to english`() =
        assertEquals("en-US", TmdbLanguage.SYSTEM.resolve(Locale.ROOT))
}
