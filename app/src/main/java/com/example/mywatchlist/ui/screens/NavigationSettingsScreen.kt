package com.example.mywatchlist.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.example.mywatchlist.R
import com.example.mywatchlist.data.NavConfig
import com.example.mywatchlist.data.NavRoutes
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.CinemaMotion
import com.example.mywatchlist.ui.theme.LocalReduceMotion
import com.example.mywatchlist.ui.theme.LocalHapticsEnabled
import sh.calvin.reorderable.ReorderableColumn

// ─────────────────────────────────────────────
// TABS ANPASSEN · Reihenfolge (Drag & Drop), Sichtbarkeit, Start-Screen
// Jede Änderung wird sofort gespeichert und ist in der Vorschau oben
// sichtbar. Die Logik (Normalisierung) liegt testbar in NavConfig.
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSettingsScreen(
    config: NavConfig,
    onBack: () -> Unit,
    onChange: ((NavConfig) -> NavConfig) -> Unit,
    onReset: () -> Unit,
) {
    val view = LocalView.current
    val haptics = LocalHapticsEnabled.current
    val items = config.order.mapNotNull(BottomNavItem::fromRoute)

    Scaffold(
        containerColor = AppTheme.DarkBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.DarkBg),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = AppTheme.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        stringResource(R.string.nav_settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppTheme.TextPrimary
                    )
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)
        ) {
            // ── Live-Vorschau der Navigationsleiste ──
            SettingsSectionHeader(stringResource(R.string.nav_settings_preview))
            NavPreview(config)
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.nav_settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            ReorderableColumn(
                list = items,
                onSettle = { from, to -> onChange { it.move(from, to) } },
                onMove = { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { index, item, isDragging ->
                key(item.route) {
                    run { // Inhalt läuft im ReorderableScope (draggableHandle)
                        val label = stringResource(item.labelRes)
                        val isStart = config.start == item.route
                        val isHidden = item.route in config.hidden
                        val elevation by animateDpAsState(
                            if (isDragging) 10.dp else 0.dp,
                            animationSpec = CinemaMotion.spatial(LocalReduceMotion.current),
                            label = "dragElevation"
                        )
                        val border by animateColorAsState(
                            if (isStart) AppTheme.YellowAccent.copy(alpha = 0.55f) else AppTheme.DarkSurface2,
                            animationSpec = CinemaMotion.colorFade(LocalReduceMotion.current),
                            label = "startBorder"
                        )
                        val moveUp = stringResource(R.string.nav_settings_move_up, label)
                        val moveDown = stringResource(R.string.nav_settings_move_down, label)

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isDragging) AppTheme.DarkSurface2 else AppTheme.DarkSurface,
                            border = BorderStroke(1.dp, border),
                            shadowElevation = elevation,
                            modifier = Modifier
                                .fillMaxWidth()
                                // TalkBack: Verschieben ohne Drag-Geste
                                .semantics {
                                    customActions = buildList {
                                        if (index > 0) add(CustomAccessibilityAction(moveUp) { onChange { it.move(index, index - 1) }; true })
                                        if (index < items.lastIndex) add(CustomAccessibilityAction(moveDown) { onChange { it.move(index, index + 1) }; true })
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.heightIn(min = 64.dp)
                            ) {
                                // Drag-Griff
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { if (haptics) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
                                    )
                                ) {
                                    Icon(
                                        Icons.Filled.DragHandle,
                                        contentDescription = stringResource(R.string.cd_reorder),
                                        tint = AppTheme.TextSecondary
                                    )
                                }
                                // Tab-Zeile: Antippen = Start-Screen
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clickable(
                                            enabled = !isHidden,
                                            role = Role.RadioButton,
                                            onClickLabel = stringResource(R.string.nav_settings_make_start)
                                        ) { onChange { it.withStart(item.route) } }
                                        .semantics { selected = isStart }
                                ) {
                                    Icon(
                                        imageVector = if (isStart) item.activeIcon else item.inactiveIcon,
                                        contentDescription = null,
                                        tint = when {
                                            isHidden -> AppTheme.TextTertiary
                                            isStart -> AppTheme.YellowAccent
                                            else -> AppTheme.TextPrimary
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isHidden) AppTheme.TextTertiary else AppTheme.TextPrimary
                                        )
                                        if (item.route == NavRoutes.SEARCH) {
                                            Text(
                                                stringResource(R.string.nav_settings_search_locked),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppTheme.TextSecondary
                                            )
                                        }
                                    }
                                    if (isStart) StartBadge()
                                }
                                // Sichtbarkeit
                                Box(Modifier.padding(horizontal = 12.dp)) {
                                    if (item.route in NavRoutes.ALWAYS_VISIBLE) {
                                        Icon(
                                            Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = AppTheme.TextTertiary,
                                            modifier = Modifier.padding(horizontal = 14.dp).size(20.dp)
                                        )
                                    } else {
                                        val visibleLabel = stringResource(R.string.nav_settings_visible, label)
                                        Switch(
                                            checked = !isHidden,
                                            enabled = config.canHide(item.route),
                                            onCheckedChange = { show -> onChange { it.setHidden(item.route, !show) } },
                                            modifier = Modifier.semantics { contentDescription = visibleLabel },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = AppTheme.OnAccent,
                                                checkedTrackColor = AppTheme.YellowAccent,
                                                uncheckedThumbColor = AppTheme.TextSecondary,
                                                uncheckedTrackColor = AppTheme.DarkSurface2
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onReset,
                enabled = config != NavConfig.Default,
                border = BorderStroke(1.dp, AppTheme.Hairline),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = AppTheme.YellowAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.nav_settings_reset), color = AppTheme.YellowAccent)
            }
        }
    }
}

@Composable
private fun StartBadge() {
    Surface(shape = MaterialTheme.shapes.extraSmall, color = AppTheme.YellowAccent.copy(alpha = 0.16f)) {
        Text(
            stringResource(R.string.nav_settings_start),
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.GoldBright,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Mini-Abbild der Floating-Navigation in der aktuellen Konfiguration. */
@Composable
private fun NavPreview(config: NavConfig) {
    val visible = config.visible.mapNotNull(BottomNavItem::fromRoute)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = AppTheme.DarkSurface,
        border = BorderStroke(1.dp, AppTheme.Hairline),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            visible.forEach { item ->
                val isStart = item.route == config.start
                Box(
                    modifier = Modifier
                        .background(
                            if (isStart) AppTheme.YellowAccent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isStart) item.activeIcon else item.inactiveIcon,
                        contentDescription = stringResource(item.labelRes),
                        tint = if (isStart) AppTheme.YellowAccent else AppTheme.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
