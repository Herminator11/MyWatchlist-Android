package com.example.mywatchlist.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// ─────────────────────────────────────────────────────────────
// DESIGN TOKENS  ·  "Cinema Noir / Goldakzent" (siehe CLAUDE.md)
//
// Zentrale Fassade für die GANZE App. Die Namen bleiben bewusst
// erhalten (DarkBg, YellowAccent …), damit kein Aufrufer angefasst
// werden muss. Die Werte kommen jetzt aus LocalCinemaColors und sind
// damit zur Laufzeit austauschbar.
// ─────────────────────────────────────────────────────────────
object AppTheme {
    // — Flächen —
    val DarkBg: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.bg
    val DarkSurface: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.surface
    val DarkSurface2: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.surface2

    // — Akzent —
    val YellowAccent: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.accent
    val GoldBright: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.accentBright
    val Ember: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.ember
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.onAccent

    // — Text —
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.textSecondary
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.textTertiary

    val Hairline: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.hairline
    val Scrim: Color @Composable @ReadOnlyComposable get() = LocalCinemaColors.current.scrim

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
