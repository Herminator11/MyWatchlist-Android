package com.example.mywatchlist.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalTextStyle

// Committetes, dunkles Farbschema – kein Material-You-Dynamic-Color
// mehr, damit der Gold/Noir-Look überall konsistent greift (Ripples,
// Cursor, Auswahlfarben, Standard-Material-Komponenten).
private val CinemaColorScheme = darkColorScheme(
    primary = AppTheme.YellowAccent,
    onPrimary = AppTheme.DarkBg,
    secondary = AppTheme.GoldBright,
    onSecondary = AppTheme.DarkBg,
    background = AppTheme.DarkBg,
    onBackground = AppTheme.TextPrimary,
    surface = AppTheme.DarkSurface,
    onSurface = AppTheme.TextPrimary,
    surfaceVariant = AppTheme.DarkSurface2,
    onSurfaceVariant = AppTheme.TextSecondary,
    outline = AppTheme.TextTertiary
)

@Composable
fun MyWatchlistTheme(
    // Parameter bleiben aus Kompatibilitätsgründen erhalten, werden
    // aber ignoriert: die App ist bewusst durchgehend dunkel.
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CinemaColorScheme,
        typography = Typography
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
