package com.example.mywatchlist.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.mywatchlist.R

// ─────────────────────────────────────────────────────────────
// CINEMA AESTHETIC – Schrift- & Farbtokens für das Detail-Popup
//
// Konzept: "Vintage-Kinoplakat trifft technisches Datenblatt".
// Ein dramatischer Display-Serif (DM Serif Display) für Titel,
// dazu eine Monospace (Space Mono) für die Metadaten-Anzeige –
// wie ein Ticket-Stub / Filmstreifen-Datenblatt.
// ─────────────────────────────────────────────────────────────

// Display-Serif – hoher Kontrast, theatralisch, plakativ
val DisplaySerif = FontFamily(
    Font(R.font.dm_serif_display_regular, FontWeight.Normal),
    Font(R.font.dm_serif_display_italic, FontWeight.Normal, FontStyle.Italic)
)

// Monospace – technisches "Spec-Sheet"-Gefühl für Zahlen & Labels
val SpecMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold)
)

// Body-Grotesk – leicht schmal, modern, gut lesbar in dichten Listen.
// Distinktiv statt System/Inter/Roboto, passt zum Kino-Look.
val BodySans = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold)
)

// ─────────────────────────────────────────────────────────────
// Erweiterte, committete Palette: tiefes Noir + warmes Gold.
// Baut auf AppTheme.YellowAccent auf, vertieft es aber zu einem
// filmischen Charcoal-zu-Schwarz mit Bernstein-Akzent.
// ─────────────────────────────────────────────────────────────
object CinemaTheme {
    val Ink          = Color(0xFF0B0B0D) // fast schwarz, Plakatgrund
    val Charcoal     = Color(0xFF161519) // warmes Anthrazit
    val Smoke        = Color(0xFF242228) // erhöhte Fläche
    val Gold         = Color(0xFFE9B949) // gedämpftes Bernsteingold
    val GoldBright   = Color(0xFFFFD874) // Glanzlicht
    val Ember        = Color(0xFF7A4A12) // dunkler Goldschatten (für Chips)
    val Parchment    = Color(0xFFF4ECDD) // warmweißer Text
    val ParchmentDim = Color(0xFFA89F8E) // gedämpfter Sekundärtext
    val Hairline     = Color(0x33E9B949) // goldene Trennlinie, transparent
}
