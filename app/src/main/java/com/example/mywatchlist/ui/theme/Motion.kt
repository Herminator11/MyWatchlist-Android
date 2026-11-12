package com.example.mywatchlist.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf

// ─────────────────────────────────────────────────────────────
// MOTION-TOKENS · ein Spring-Schema für die ganze App
//
// spatial  → Bewegung, Größe, Position (leicht federnd, „physisch“)
// bouncy   → kleine Signatur-Momente (Nav-Icon), deutlich federnd
// effects  → Alpha, Blur (ohne Überschwingen)
// colorFade → Farbwechsel: 250-ms-Tween (Design-Token, siehe CLAUDE.md)
//
// Bei reduzierter Animation liefern alle Specs `snap()`.
// ─────────────────────────────────────────────────────────────
val LocalReduceMotion = compositionLocalOf { false }

/** Haptisches Feedback app-weit an/aus (Einstellung). */
val LocalHapticsEnabled = compositionLocalOf { true }

object CinemaMotion {
    fun <T> spatial(reduce: Boolean = false): FiniteAnimationSpec<T> =
        if (reduce) snap() else spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)

    fun <T> bouncy(reduce: Boolean = false): FiniteAnimationSpec<T> =
        if (reduce) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    fun <T> effects(reduce: Boolean = false): FiniteAnimationSpec<T> =
        if (reduce) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> colorFade(reduce: Boolean = false): FiniteAnimationSpec<T> =
        if (reduce) snap() else tween(durationMillis = 250)
}
