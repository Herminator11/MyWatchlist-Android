package com.example.mywatchlist.data

// ─────────────────────────────────────────────────────────────
// DARSTELLUNGS-EINSTELLUNGEN (reine Daten, ohne Compose/Farben)
// Die Zuordnung Akzent → Farbwerte liegt in ui/theme/CinemaColors.kt.
// ─────────────────────────────────────────────────────────────
enum class AccentOption { GOLD, CRIMSON, COPPER, TEAL, ICE;
    companion object {
        fun fromName(name: String?): AccentOption = entries.firstOrNull { it.name == name } ?: GOLD
    }
}

data class AppearancePrefs(
    val accent: AccentOption = AccentOption.GOLD,
    /** Reines Schwarz statt Charcoal-Verlauf (spart Akku auf OLED). */
    val amoled: Boolean = false,
    /** Beschriftung unter den Nav-Icons statt nur Tooltip. */
    val navLabels: Boolean = false,
    val haptics: Boolean = true,
    /** Animationen reduzieren – zusätzlich zur Systemeinstellung. */
    val reduceMotion: Boolean = false,
)
