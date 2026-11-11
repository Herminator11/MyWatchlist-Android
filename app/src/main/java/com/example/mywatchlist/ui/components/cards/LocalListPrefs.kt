package com.example.mywatchlist.ui.components.cards

import androidx.compose.runtime.compositionLocalOf
import com.example.mywatchlist.data.ListPrefs

/** Aktive Listen-Einstellungen (Dichte, Metadaten, Sortierung) für alle Karten. */
val LocalListPrefs = compositionLocalOf { ListPrefs() }
