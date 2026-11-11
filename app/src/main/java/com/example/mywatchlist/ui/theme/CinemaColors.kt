package com.example.mywatchlist.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
// FARB-TOKENS · reaktiv über CompositionLocal
//
// Früher waren die Farben feste `val`s in einem object. Jetzt hängen
// sie an LocalCinemaColors → eine Palette kann zur Laufzeit getauscht
// werden (Basis für die geplante Akzentfarben-Einstellung), ohne dass
// ein einziger Aufrufer von AppTheme.* angepasst werden muss.
// ─────────────────────────────────────────────────────────────
@Immutable
data class CinemaColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val accent: Color,
    val accentBright: Color,
    val ember: Color,
    /** Inhalt (Icons/Text) auf Akzentflächen – ersetzt Color.Black. */
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val hairline: Color,
    /** Scrim hinter Dialogen, falls Blur (API < 31) nicht verfügbar ist. */
    val scrim: Color,
)

/** Standardpalette „Cinema Noir / Gold“. */
val NoirGold = CinemaColors(
    bg = Color(0xFF0B0B0D),
    surface = Color(0xFF161519),
    surface2 = Color(0xFF242228),
    accent = Color(0xFFE9B949),
    accentBright = Color(0xFFFFD874),
    ember = Color(0xFF7A4A12),
    onAccent = Color(0xFF0B0B0D),
    textPrimary = Color(0xFFF4ECDD),
    textSecondary = Color(0xFFA89F8E),
    // vorher #4A4640 (≈1.9:1 auf Surface) → jetzt ≈3.3:1 für Chevrons/Trenner
    textTertiary = Color(0xFF6E685E),
    hairline = Color(0x33E9B949),
    scrim = Color(0xB3070708),
)

val LocalCinemaColors = staticCompositionLocalOf { NoirGold }
