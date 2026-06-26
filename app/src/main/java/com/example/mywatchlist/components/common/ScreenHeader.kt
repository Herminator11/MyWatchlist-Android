package com.example.mywatchlist.components.common

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywatchlist.ui.theme.AppTheme

// ─────────────────────────────────────────────
// SCREEN HEADER  ·  Display-Serif + orchestrierter Auftritt
// Erscheint auf jedem Screen → der "Page-Load"-Moment der App.
// ─────────────────────────────────────────────
@Composable
fun ScreenHeader(title: String) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(420, easing = EaseOutCubic),
        label = "header_alpha"
    )
    val lift = with(LocalDensity.current) { 14.dp.toPx() }
    val translation by animateFloatAsState(
        targetValue = if (shown) 0f else lift,
        animationSpec = tween(480, easing = EaseOutCubic),
        label = "header_y"
    )
    // Akzentbalken "wächst" beim Eintreten in die Höhe
    val barScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(560, easing = EaseOutCubic),
        label = "header_bar"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 4.dp, bottom = 12.dp)
            .graphicsLayer {
                this.alpha = alpha
                translationY = translation
            }
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 30.dp)
                .graphicsLayer {
                    scaleY = barScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(AppTheme.GoldBright, AppTheme.YellowAccent)
                    )
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontFamily = AppTheme.Display,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                color = AppTheme.TextPrimary,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            // dezente Mono-Subline für den "Datenblatt"-Charakter
            Text(
                text = "·".repeat(3) + "  MY WATCHLIST",
                fontFamily = AppTheme.Mono,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = AppTheme.TextSecondary
            )
        }
    }
}
