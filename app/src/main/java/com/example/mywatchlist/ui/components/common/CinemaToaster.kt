package com.example.mywatchlist.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterState
import com.example.mywatchlist.ui.theme.AppTheme

// ─────────────────────────────────────────────
// CINEMA TOASTER · compose-sonner im Noir/Gold-Look
// Ersetzt Toast.makeText: gestapelt, wischbar, Theme-Farben.
// Undo-Aktionen bleiben bei der Material-Snackbar.
// ─────────────────────────────────────────────
@Composable
fun CinemaToaster(state: ToasterState, modifier: Modifier = Modifier) {
    val surface = AppTheme.DarkSurface
    val text = AppTheme.TextPrimary
    val accent = AppTheme.YellowAccent
    val hairline = AppTheme.Hairline
    val danger = AppTheme.Danger
    Toaster(
        state = state,
        modifier = modifier,
        darkTheme = true,
        alignment = Alignment.BottomCenter,
        containerPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        background = { SolidColor(surface) },
        contentColor = { text },
        border = { toast ->
            BorderStroke(
                1.dp,
                when (toast.type) {
                    ToastType.Success -> accent.copy(alpha = 0.55f)
                    ToastType.Error -> danger.copy(alpha = 0.6f)
                    else -> hairline
                }
            )
        },
    )
}
