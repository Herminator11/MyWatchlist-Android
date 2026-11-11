package com.example.mywatchlist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
// TYPO-SKALA · Plakat (Bebas Neue) · Lesetext (Barlow) · Datenblatt (Space Mono)
//
// display/headline → Bebas Neue (Screen- und Dialogtitel)
// title/body       → Barlow     (Karten, Listen, Fließtext)
// label            → Space Mono (Tags, Daten, Metazeilen) – nie unter 11 sp
// ─────────────────────────────────────────────────────────────
val CinemaTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontSize = 44.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = DisplayFont, fontSize = 36.sp, lineHeight = 38.sp),
    displaySmall = TextStyle(fontFamily = DisplayFont, fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineLarge = TextStyle(fontFamily = DisplayFont, fontSize = 28.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontSize = 24.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontSize = 22.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.sp),
    titleSmall = TextStyle(fontFamily = BodySans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = BodySans, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = BodySans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = BodySans, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = SpecMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = SpecMono, fontSize = 12.sp, lineHeight = 15.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = SpecMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp),
)
