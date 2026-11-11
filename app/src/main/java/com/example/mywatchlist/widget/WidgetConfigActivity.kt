package com.example.mywatchlist.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.example.mywatchlist.R
import com.example.mywatchlist.data.SettingsRepository
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.MyWatchlistTheme
import com.example.mywatchlist.ui.theme.cinemaBackground
import com.example.mywatchlist.ui.theme.cinemaPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Auswahl beim Hinzufügen (und Neu-Konfigurieren) eines Widgets:
 * welche Liste soll es zeigen? Gespeichert pro Widget-Instanz im Glance-State.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Abbrechen (Zurück) = Widget wird nicht angelegt
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        lifecycleScope.launch {
            val appearance = SettingsRepository(this@WidgetConfigActivity).appearance.first()
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            val current = WidgetList.fromName(
                getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)[WatchlistWidget.LIST_KEY]
            )
            setContent {
                MyWatchlistTheme(colors = cinemaPalette(appearance.accent, appearance.amoled)) {
                    WidgetConfigScreen(initial = current) { choice ->
                        lifecycleScope.launch {
                            updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                                prefs[WatchlistWidget.LIST_KEY] = choice.name
                            }
                            WatchlistWidget().update(this@WidgetConfigActivity, glanceId)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetConfigScreen(initial: WidgetList, onPick: (WidgetList) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .cinemaBackground()
            .safeDrawingPadding()
            .padding(20.dp)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.widget_config_title), style = MaterialTheme.typography.displaySmall, color = AppTheme.TextPrimary)
        Text(
            stringResource(R.string.widget_config_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.TextSecondary,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        WidgetList.entries.forEach { option ->
            val isSel = option == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = AppTheme.DarkSurface,
                border = BorderStroke(1.dp, if (isSel) AppTheme.YellowAccent else AppTheme.DarkSurface2),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSel, role = Role.RadioButton) { selected = option; onPick(option) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 18.dp)
                ) {
                    Icon(
                        imageVector = when (option) {
                            WidgetList.WATCHING -> Icons.Outlined.LiveTv
                            WidgetList.WATCHLIST -> Icons.Outlined.BookmarkBorder
                            WidgetList.HISTORY -> Icons.Outlined.History
                            WidgetList.FAVORITES -> Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = null,
                        tint = if (isSel) AppTheme.YellowAccent else AppTheme.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(stringResource(option.labelRes()), style = MaterialTheme.typography.titleMedium, color = AppTheme.TextPrimary)
                }
            }
        }
    }
}
