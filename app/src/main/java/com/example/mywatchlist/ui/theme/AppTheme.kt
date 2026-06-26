package com.example.mywatchlist.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// ─────────────────────────────────────────────────────────────
// DESIGN TOKENS  ·  "Cinema Noir / Goldakzent" (siehe CLAUDE.md)
//
// Zentrale Quelle für die GANZE App. Alle Screens lesen aus diesen
// Tokens – ändert man sie hier, re-skinnt sich die komplette App.
// Die Namen bleiben bewusst erhalten (DarkBg, YellowAccent …),
// damit kein Aufrufer angefasst werden muss; nur die Werte werden
// vom kalten Grau-Gelb auf warmes Noir-Gold verschoben.
// ─────────────────────────────────────────────────────────────
object AppTheme {
    // — Flächen (tiefes, warmes Noir) —
    val DarkBg        = Color(0xFF0B0B0D) // App-Grund, fast schwarz
    val DarkSurface   = Color(0xFF161519) // Karten / Sheets
    val DarkSurface2  = Color(0xFF242228) // erhöhte Fläche / Badges

    // — Akzent (warmes Bernsteingold statt Neongelb) —
    val YellowAccent  = Color(0xFFE9B949)

    // — Text (warmweißes Pergament statt kühlem Weiß) —
    val TextPrimary   = Color(0xFFF4ECDD)
    val TextSecondary = Color(0xFFA89F8E)
    val TextTertiary  = Color(0xFF4A4640)

    // — Zusätzliche Akzent-Nuancen für das neue Design —
    val GoldBright    = Color(0xFFFFD874)
    val Ember         = Color(0xFF7A4A12)
    val Hairline      = Color(0x33E9B949)

    // — Schrift-Tokens (gebündelte Fonts, kein System/Inter) —
    val Display: FontFamily = DisplaySerif // Titel, Headlines
    val Body: FontFamily    = BodySans     // Fließtext, Listen
    val Mono: FontFamily    = SpecMono     // Daten, Labels, Tags
}

// Atmosphärischer App-Hintergrund: warmer Charcoal oben, der nach
// fast-schwarz unten ausläuft – Tiefe statt Solid-Fläche.
fun Modifier.cinemaBackground(): Modifier = this.background(
    Brush.verticalGradient(
        0.0f to Color(0xFF17141A),
        0.45f to Color(0xFF0D0C0F),
        1.0f to Color(0xFF070708)
    )
)
