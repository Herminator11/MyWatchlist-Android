package com.example.mywatchlist.ui.theme
import com.example.mywatchlist.data.AccentOption

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
    /** Fehler/Destruktiv (Toasts, Löschen). */
    val danger: Color,
    /** Scrim hinter Dialogen, falls Blur (API < 31) nicht verfügbar ist. */
    val scrim: Color,
    /** Atmosphärischer Screen-Hintergrund (oben → Mitte → unten). */
    val backdropTop: Color = Color(0xFF17141A),
    val backdropMid: Color = Color(0xFF0D0C0F),
    val backdropBottom: Color = Color(0xFF070708),
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
    danger = Color(0xFFE5484D),
    scrim = Color(0xB3070708),
)

val LocalCinemaColors = staticCompositionLocalOf { NoirGold }

// ── Akzent-Varianten: alle auf dunklem Grund getestet, Inhalt darauf = bg ──
private data class AccentSet(val accent: Color, val bright: Color, val ember: Color)

private fun AccentOption.colors(): AccentSet = when (this) {
    AccentOption.GOLD -> AccentSet(Color(0xFFE9B949), Color(0xFFFFD874), Color(0xFF7A4A12))
    AccentOption.CRIMSON -> AccentSet(Color(0xFFE5675F), Color(0xFFFF9C94), Color(0xFF6E1F1B))
    AccentOption.COPPER -> AccentSet(Color(0xFFD98A55), Color(0xFFF4B88C), Color(0xFF5E2E12))
    AccentOption.TEAL -> AccentSet(Color(0xFF45BCB2), Color(0xFF84E6DC), Color(0xFF134541))
    AccentOption.ICE -> AccentSet(Color(0xFF86B6E8), Color(0xFFBFDCF8), Color(0xFF1F3A57))
}

/** Swatch-Farbe für die Auswahl in den Einstellungen. */
fun AccentOption.swatch(): Color = colors().accent

/** Baut die aktive Palette aus Akzent + AMOLED-Schalter. */
fun cinemaPalette(accent: AccentOption, amoled: Boolean): CinemaColors {
    val a = accent.colors()
    val base = NoirGold.copy(
        accent = a.accent,
        accentBright = a.bright,
        ember = a.ember,
        hairline = a.accent.copy(alpha = 0.2f),
    )
    return if (!amoled) base else base.copy(
        bg = Color.Black,
        onAccent = Color.Black,
        surface = Color(0xFF111013),
        surface2 = Color(0xFF1E1C21),
        backdropTop = Color.Black,
        backdropMid = Color.Black,
        backdropBottom = Color.Black,
    )
}
