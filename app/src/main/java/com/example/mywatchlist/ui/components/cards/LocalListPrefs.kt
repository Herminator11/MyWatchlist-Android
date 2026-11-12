package com.example.mywatchlist.ui.components.cards
import com.example.mywatchlist.data.ContentPrefs

import androidx.compose.runtime.compositionLocalOf
import com.example.mywatchlist.data.ListPrefs

/** Aktive Listen-Einstellungen (Dichte, Metadaten, Sortierung) für alle Karten. */
val LocalListPrefs = compositionLocalOf { ListPrefs() }

/** Aktive Inhalts-Einstellungen (TMDB-Sprache, Originaltitel). */
val LocalContentPrefs = compositionLocalOf { ContentPrefs() }
