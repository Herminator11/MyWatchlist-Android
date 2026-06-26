package com.example.mywatchlist.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.mywatchlist.R

// ─────────────────────────────────────────────────────────────
// CINEMA AESTHETIC – Schrift- & Farbtokens
//
// Konzept: "Filmplakat / Ticket trifft technisches Datenblatt".
// Eine hohe, kondensierte Versal-Display (Bebas Neue) für Titel,
// dazu eine Monospace (Space Mono) für die Metadaten-Anzeige –
// wie ein Ticket-Stub / Filmstreifen-Datenblatt.
// ─────────────────────────────────────────────────────────────

// Display – Bebas Neue: hohe, kondensierte Versalien, plakativ.
// Einzelschnitt → alle genutzten Gewichte zeigen denselben Schnitt
// (verhindert künstliches Fett/Synthese).
val DisplayFont = FontFamily(
    Font(R.font.bebas_neue_regular, FontWeight.Normal),
    Font(R.font.bebas_neue_regular, FontWeight.Medium),
    Font(R.font.bebas_neue_regular, FontWeight.SemiBold),
    Font(R.font.bebas_neue_regular, FontWeight.Bold)
)

// Rückwärtskompatibler Alias (frühere Referenzen nutzten DisplaySerif)
val DisplaySerif = DisplayFont

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
