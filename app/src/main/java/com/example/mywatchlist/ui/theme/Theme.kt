package com.example.mywatchlist.ui.theme

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Material-ColorScheme wird aus der aktiven Cinema-Palette abgeleitet,
// damit Standard-Komponenten (Ripples, Cursor, DatePicker, Switches,
// Sheets) automatisch im Gold/Noir-Look erscheinen.
private fun CinemaColors.toColorScheme() = darkColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = ember,
    onPrimaryContainer = accentBright,
    secondary = accentBright,
    onSecondary = onAccent,
    background = bg,
    onBackground = textPrimary,
    surface = surface,
    onSurface = textPrimary,
    surfaceVariant = surface2,
    onSurfaceVariant = textSecondary,
    surfaceContainer = surface,
    surfaceContainerHigh = surface2,
    surfaceContainerHighest = surface2,
    surfaceContainerLow = surface,
    inverseSurface = textPrimary,
    inverseOnSurface = bg,
    outline = textTertiary,
    outlineVariant = surface2,
    scrim = scrim,
)

// Zentrale Rundungen statt verstreuter RoundedCornerShape(…)-Literale.
val CinemaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),  // Tags, Badges
    small = RoundedCornerShape(12.dp),      // Buttons, Aktions-Kacheln
    medium = RoundedCornerShape(16.dp),     // Karten, Settings-Cards
    large = RoundedCornerShape(20.dp),      // Chips, Sheets
    extraLarge = RoundedCornerShape(32.dp), // Floating-Navigation
)

@Composable
fun MyWatchlistTheme(
    colors: CinemaColors = NoirGold,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // System „Animationen entfernen“ (Entwickler-/Bedienungshilfen) respektieren.
    val reduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    CompositionLocalProvider(
        LocalCinemaColors provides colors,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = CinemaTypography,
            shapes = CinemaShapes,
        ) {
            // Body-Default app-weit: jeder Text ohne explizite Schrift
            // erbt Barlow – an EINER Stelle gesetzt statt überall.
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = AppTheme.Body)
            ) {
                content()
            }
        }
    }
}
