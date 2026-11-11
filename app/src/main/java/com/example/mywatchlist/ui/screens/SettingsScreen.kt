package com.example.mywatchlist.ui.screens
import com.example.mywatchlist.data.ListPrefs
import com.example.mywatchlist.data.CardDensity
import com.example.mywatchlist.data.ListSort
import com.example.mywatchlist.data.MediaTab
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Check
import com.example.mywatchlist.data.AppearancePrefs
import com.example.mywatchlist.data.AccentOption
import com.example.mywatchlist.ui.theme.swatch
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Animation
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.example.mywatchlist.R

import com.dokar.sonner.ToastType
import com.dokar.sonner.rememberToasterState
import com.example.mywatchlist.ui.components.common.CinemaToaster
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
import androidx.compose.material.icons.filled.SwapVert
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
    onOpenNavigation: () -> Unit = {},
    appearance: AppearancePrefs = AppearancePrefs(),
    onAppearanceChange: ((AppearancePrefs) -> AppearancePrefs) -> Unit = {},
    listPrefs: ListPrefs = ListPrefs(),
    onListPrefsChange: ((ListPrefs) -> ListPrefs) -> Unit = {},
    exportCounts: Map<ExportList, Int>,
    onBuildExportText: (Set<ExportList>) -> String?
) {
    val context = LocalContext.current
    // Ressourcen über LocalResources → reagiert korrekt auf Konfigurationswechsel
    val resources = LocalResources.current
    // Themed Toasts (compose-sonner) statt System-Toast.makeText
    val toaster = rememberToasterState()

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
            toaster.show(resources.getString(R.string.export_saved), type = ToastType.Success)
        } catch (e: Exception) {
            toaster.show(resources.getString(R.string.export_failed), type = ToastType.Error)
        }
    }

    // Baut den Text, prüft auf Leere und startet den SAF-Dialog.
    fun startExport(selection: Set<ExportList>, fileName: String) {
        val text = onBuildExportText(selection)
        if (text == null) {
            toaster.show(resources.getString(R.string.export_nothing), type = ToastType.Info)
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
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsRow(
                    icon = Icons.Filled.SwapVert,
                    title = stringResource(R.string.settings_nav_customize),
                    subtitle = stringResource(R.string.settings_nav_customize_desc),
                    onClick = onOpenNavigation,
                    trailing = { SettingsChevron() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Look: Akzent, AMOLED, Labels, Haptik, Animationen ──
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_accent),
                    subtitle = stringResource(appearance.accent.labelRes()),
                    trailing = {}
                )
                AccentPicker(
                    selected = appearance.accent,
                    onSelect = { accent -> onAppearanceChange { it.copy(accent = accent) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Filled.DarkMode,
                    title = stringResource(R.string.settings_amoled),
                    subtitle = stringResource(R.string.settings_amoled_desc),
                    checked = appearance.amoled,
                    onChange = { v -> onAppearanceChange { it.copy(amoled = v) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Filled.Label,
                    title = stringResource(R.string.settings_nav_labels),
                    subtitle = stringResource(R.string.settings_nav_labels_desc),
                    checked = appearance.navLabels,
                    onChange = { v -> onAppearanceChange { it.copy(navLabels = v) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Filled.Vibration,
                    title = stringResource(R.string.settings_haptics),
                    subtitle = stringResource(R.string.settings_haptics_desc),
                    checked = appearance.haptics,
                    onChange = { v -> onAppearanceChange { it.copy(haptics = v) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Filled.Animation,
                    title = stringResource(R.string.settings_reduce_motion),
                    subtitle = stringResource(R.string.settings_reduce_motion_desc),
                    checked = appearance.reduceMotion,
                    onChange = { v -> onAppearanceChange { it.copy(reduceMotion = v) } }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SEKTION: LISTEN ───────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_section_lists))
            SettingsCard {
                SettingsChoiceRow(
                    icon = Icons.Filled.ViewAgenda,
                    title = stringResource(R.string.settings_density),
                    options = listOf(
                        CardDensity.COMFORTABLE to stringResource(R.string.density_comfortable),
                        CardDensity.COMPACT to stringResource(R.string.density_compact),
                        CardDensity.POSTER to stringResource(R.string.density_poster),
                    ),
                    selected = listPrefs.density,
                    onSelect = { v -> onListPrefsChange { it.copy(density = v) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Filled.CalendarMonth,
                    title = stringResource(R.string.settings_show_year),
                    subtitle = null,
                    checked = listPrefs.showYear,
                    onChange = { v -> onListPrefsChange { it.copy(showYear = v) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Filled.Sell,
                    title = stringResource(R.string.settings_show_type),
                    subtitle = null,
                    checked = listPrefs.showTypeTag,
                    onChange = { v -> onListPrefsChange { it.copy(showTypeTag = v) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                val sortOptions = listOf(
                    ListSort.DEFAULT to stringResource(R.string.sort_default),
                    ListSort.TITLE to stringResource(R.string.sort_title),
                    ListSort.YEAR_NEWEST to stringResource(R.string.sort_year_newest),
                    ListSort.YEAR_OLDEST to stringResource(R.string.sort_year_oldest),
                )
                SettingsChoiceRow(
                    icon = Icons.Filled.Sort,
                    title = stringResource(R.string.settings_sort_watchlist),
                    options = sortOptions,
                    selected = listPrefs.watchlistSort,
                    onSelect = { v -> onListPrefsChange { it.copy(watchlistSort = v) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsChoiceRow(
                    icon = Icons.Filled.Sort,
                    title = stringResource(R.string.settings_sort_watching),
                    options = sortOptions,
                    selected = listPrefs.watchingSort,
                    onSelect = { v -> onListPrefsChange { it.copy(watchingSort = v) } }
                )
                HorizontalDivider(color = AppTheme.DarkSurface2, thickness = 1.dp)
                SettingsChoiceRow(
                    icon = Icons.Filled.Tab,
                    title = stringResource(R.string.settings_watchlist_tab),
                    options = listOf(
                        MediaTab.SERIES to stringResource(R.string.tab_series),
                        MediaTab.MOVIES to stringResource(R.string.tab_movies),
                    ),
                    selected = listPrefs.watchlistStartTab,
                    onSelect = { v -> onListPrefsChange { it.copy(watchlistStartTab = v) } }
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

    CinemaToaster(state = toaster)
}

// ── Hilfs-Composables ────────────────────────────────────────

@Composable
internal fun SettingsSectionHeader(title: String) {
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
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.DarkSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsRow(
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

@Composable
internal fun SettingsChevron(enabled: Boolean = true) {
    Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = if (enabled) AppTheme.TextTertiary else AppTheme.TextTertiary.copy(alpha = 0.4f),
        modifier = Modifier.size(20.dp)
    )
}

@Composable
internal fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = { onChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null, // Zeile ist der Klickbereich → ein Fokusziel für TalkBack
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppTheme.OnAccent,
                    checkedTrackColor = AppTheme.YellowAccent,
                    uncheckedThumbColor = AppTheme.TextSecondary,
                    uncheckedTrackColor = AppTheme.DarkSurface2
                )
            )
        }
    )
}

private fun AccentOption.labelRes(): Int = when (this) {
    AccentOption.GOLD -> R.string.accent_gold
    AccentOption.CRIMSON -> R.string.accent_crimson
    AccentOption.COPPER -> R.string.accent_copper
    AccentOption.TEAL -> R.string.accent_teal
    AccentOption.ICE -> R.string.accent_ice
}

/** Fünf Farbmedaillons; Auswahl mit Ring. 48-dp-Ziele, Radio-Semantik. */
@Composable
private fun AccentPicker(selected: AccentOption, onSelect: (AccentOption) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 12.dp, bottom = 12.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AccentOption.entries.forEach { option ->
            val isSelected = option == selected
            val name = stringResource(option.labelRes())
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) }
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 36.dp else 30.dp)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) AppTheme.TextPrimary else androidx.compose.ui.graphics.Color.Transparent,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .background(option.swatch(), CircleShape)
                )
            }
        }
    }
}

/** Zeile mit aktuellem Wert rechts; Antippen öffnet ein Auswahlmenü. */
@Composable
internal fun <T> SettingsChoiceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second.orEmpty()
    Box {
        SettingsRow(
            icon = icon,
            title = title,
            subtitle = current,
            onClick = { open = true },
            trailing = { SettingsChevron() }
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = AppTheme.DarkSurface2,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            color = if (value == selected) AppTheme.YellowAccent else AppTheme.TextPrimary,
                            fontWeight = if (value == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        if (value == selected) Icon(Icons.Filled.Check, null, tint = AppTheme.YellowAccent)
                    },
                    onClick = { onSelect(value); open = false }
                )
            }
        }
    }
}
