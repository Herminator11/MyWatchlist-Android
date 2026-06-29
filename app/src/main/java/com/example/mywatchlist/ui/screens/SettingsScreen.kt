package com.example.mywatchlist.ui.screens
import androidx.compose.ui.res.stringResource
import com.example.mywatchlist.R

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.viewmodel.ExportList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    autoHideNavBar: Boolean,
    onAutoHideNavBarChanged: (Boolean) -> Unit,
    exportCounts: Map<ExportList, Int>,
    onBuildExportText: (Set<ExportList>) -> String?
) {
    val context = LocalContext.current

    // Inhalt der noch zu schreibenden Datei (steht erst nach SAF-Auswahl fest)
    var pendingExport by remember { mutableStateOf<String?>(null) }

    // Storage Access Framework: User wählt Speicherort + Dateiname selbst.
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val text = pendingExport
        pendingExport = null
        // uri == null → User hat den Dialog abgebrochen (kein Fehler).
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(text.toByteArray())
            } ?: throw java.io.IOException("openOutputStream returned null")
            Toast.makeText(context, context.getString(R.string.export_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
        }
    }

    // Baut den Text, prüft auf Leere und startet den SAF-Dialog.
    fun startExport(selection: Set<ExportList>, fileName: String) {
        val text = onBuildExportText(selection)
        if (text == null) {
            Toast.makeText(context, context.getString(R.string.export_nothing), Toast.LENGTH_SHORT).show()
            return
        }
        pendingExport = text
        createDocument.launch(fileName)
    }

    val totalEntries = exportCounts.values.sum()
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
                            contentDescription = stringResource(R.string.action_back),
                            tint = AppTheme.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontFamily = AppTheme.Display,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
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
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))

            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.ViewDay,
                    title = stringResource(R.string.settings_navigation),
                    subtitle = stringResource(R.string.settings_autohide_navbar),
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
            SettingsSectionHeader(stringResource(R.string.settings_export_lists))

            SettingsCard {
                // Eine Export-Reihe pro Liste (deaktiviert, wenn leer).
                ExportList.values().forEachIndexed { index, list ->
                    val count = exportCounts[list] ?: 0
                    val isEmpty = count == 0
                    SettingsRow(
                        icon = Icons.Filled.FileDownload,
                        title = stringResource(list.labelRes),
                        subtitle = if (isEmpty) stringResource(R.string.settings_export_empty)
                        else stringResource(R.string.settings_export_count, count),
                        enabled = !isEmpty,
                        onClick = { startExport(setOf(list), "mywatchlist_${list.fileStem}.txt") },
                        trailing = {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = if (isEmpty) AppTheme.TextTertiary.copy(alpha = 0.4f)
                                else AppTheme.TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                    if (index < ExportList.values().lastIndex) {
                        HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Alles-auf-einmal-Export (eine Datei mit allen Listen).
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.LibraryAddCheck,
                    title = stringResource(R.string.settings_export_all),
                    subtitle = stringResource(R.string.settings_export_all_desc),
                    enabled = totalEntries > 0,
                    onClick = {
                        startExport(ExportList.values().toSet(), "mywatchlist_all_lists.txt")
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = if (totalEntries == 0) AppTheme.TextTertiary.copy(alpha = 0.4f)
                            else AppTheme.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── VERSION ───────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_version, com.example.mywatchlist.BuildConfig.VERSION_NAME),
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