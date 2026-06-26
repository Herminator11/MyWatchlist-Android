package com.example.mywatchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywatchlist.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    autoHideNavBar: Boolean,
    onAutoHideNavBarChanged: (Boolean) -> Unit,
    onExportHistory: () -> Unit,
    historyIsEmpty: Boolean
) {
    Scaffold(
        containerColor = AppTheme.DarkBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.DarkBg
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = AppTheme.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = "Einstellungen",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.TextPrimary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── SEKTION: DARSTELLUNG ─────────────────────────
            SettingsSectionHeader("Darstellung")

            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.ViewDay,
                    title = "Navigation",
                    subtitle = "Nav-Bar beim Scrollen ausblenden",
                    trailing = {
                        Switch(
                            checked = autoHideNavBar,
                            onCheckedChange = onAutoHideNavBarChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppTheme.DarkBg,
                                checkedTrackColor = AppTheme.YellowAccent,
                                uncheckedThumbColor = AppTheme.TextSecondary,
                                uncheckedTrackColor = AppTheme.DarkSurface2
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SEKTION: DATEN ────────────────────────────────
            SettingsSectionHeader("Daten")

            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.FileDownload,
                    title = "History exportieren",
                    subtitle = if (historyIsEmpty) "Noch keine Einträge vorhanden"
                    else "Recently Watched als .txt-Datei teilen",
                    enabled = !historyIsEmpty,
                    onClick = onExportHistory,
                    trailing = {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = if (historyIsEmpty) AppTheme.TextTertiary.copy(alpha = 0.4f)
                            else AppTheme.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── VERSION ───────────────────────────────────────
            Text(
                text = "MyWatchlist · Version 1.0",
                fontSize = 12.sp,
                color = AppTheme.TextTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// ── Hilfs-Composables ────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.TextSecondary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.DarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled)
                    Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.YellowAccent.copy(alpha = alpha),
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.TextPrimary.copy(alpha = alpha)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppTheme.TextSecondary.copy(alpha = alpha)
                )
            }
        }
        trailing()
    }
}