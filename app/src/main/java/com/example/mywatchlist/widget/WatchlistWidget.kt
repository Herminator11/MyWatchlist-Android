package com.example.mywatchlist.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.mywatchlist.MainActivity
import com.example.mywatchlist.R
import com.example.mywatchlist.data.AppearancePrefs
import com.example.mywatchlist.data.SettingsRepository
import com.example.mywatchlist.data.local.AppDatabase
import com.example.mywatchlist.data.local.MovieEntity
import com.example.mywatchlist.ui.theme.CinemaColors
import com.example.mywatchlist.ui.theme.cinemaPalette
import kotlinx.coroutines.flow.first

// ─────────────────────────────────────────────────────────────
// HOMESCREEN-WIDGET · zeigt eine wählbare Liste (pro Widget-Instanz)
//
// • Liest nur aus der Datenbank – schreibt nie.
// • Folgt Akzentfarbe/AMOLED aus den Einstellungen.
// • 3 Größen: klein (Zähler + nächster Titel), mittel (Liste), groß (Liste mit Postern)
// • Tippen öffnet die App direkt im passenden Tab.
// ─────────────────────────────────────────────────────────────
class WatchlistWidget : GlanceAppWidget() {

    companion object {
        val LIST_KEY = stringPreferencesKey("widget_list")
        private val SMALL = DpSize(110.dp, 110.dp)
        private val MEDIUM = DpSize(250.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 260.dp)

        /** Alle Widgets neu zeichnen (nach DB- oder Einstellungsänderungen). */
        suspend fun refreshAll(context: Context) {
            if (GlanceAppWidgetManager(context).getGlanceIds(WatchlistWidget::class.java).isNotEmpty()) {
                WatchlistWidget().updateAll(context)
            }
        }

        fun openAppIntent(context: Context, route: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_TAB, route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.getDatabase(context).movieDao()
        val settings = SettingsRepository(context)
        // Startwerte einmal laden, danach reaktiv über Flows
        val initialAll = dao.getAllOnce()
        val initialAppearance = settings.appearance.first()

        provideContent {
            // Flow einmal anlegen – sonst startet jede Recomposition eine neue Room-Abfrage
            val allFlow = remember { dao.getAllMoviesFlow() }
            val all by allFlow.collectAsState(initialAll)
            val appearance by settings.appearance.collectAsState(initialAppearance)
            val list = WidgetList.fromName(currentState<Preferences>()[LIST_KEY])
            val entries = remember(all, list) { WidgetData.select(list, all) }
            WidgetContent(context, list, entries, appearance)
        }
    }
}

@Composable
internal fun WidgetContent(context: Context, list: WidgetList, entries: List<MovieEntity>, appearance: AppearancePrefs) {
    val c = remember(appearance) { cinemaPalette(appearance.accent, appearance.amoled) }
    val size = LocalSize.current
    val showPosters = size.height >= 200.dp
    val compact = size.width < 200.dp
    val open = actionStartActivity(WatchlistWidget.openAppIntent(context, list.route))

    // Poster nur für die sichtbaren Einträge, klein skaliert (RemoteViews-Speicherlimit)
    val posterKeys = if (showPosters) entries.take(8).map { it.posterPath } else emptyList()
    val posters by produceState<Map<String, Bitmap>>(emptyMap(), posterKeys) {
        value = loadPosters(context, posterKeys.filterNotNull())
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(ColorProvider(c.bg))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clickable(open)
    ) {
        Header(c, list, entries.size)
        Spacer(GlanceModifier.height(8.dp))
        when {
            entries.isEmpty() -> Text(
                context.getString(R.string.widget_empty),
                style = TextStyle(color = ColorProvider(c.textSecondary), fontSize = 13.sp)
            )
            compact -> {
                // klein: nur nächster Titel, groß und lesbar
                Text(
                    WidgetData.title(entries.first()),
                    maxLines = 3,
                    style = TextStyle(color = ColorProvider(c.textPrimary), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    WidgetData.subtitle(entries.first()),
                    style = TextStyle(color = ColorProvider(c.textSecondary), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                )
            }
            else -> LazyColumn {
                // Keine eigene itemId: eine Hash-Summe kann kollidieren, Glance vergibt sonst eindeutige IDs
                items(entries) { e ->
                    EntryRow(c, e, posters[e.posterPath], showPosters, open)
                }
            }
        }
    }
}

@Composable
private fun Header(c: CinemaColors, list: WidgetList, count: Int) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
        Box(GlanceModifier.width(4.dp).height(20.dp).cornerRadius(2.dp).background(ColorProvider(c.accent))) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            context.getString(list.labelRes()).uppercase(),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(c.textPrimary),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily("sans-serif-condensed")
            )
        )
        Text(
            count.toString(),
            style = TextStyle(color = ColorProvider(c.accent), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        )
    }
}

@Composable
private fun EntryRow(c: CinemaColors, e: MovieEntity, poster: Bitmap?, showPoster: Boolean, open: androidx.glance.action.Action) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(ColorProvider(c.surface))
                .padding(if (showPoster) 6.dp else 10.dp)
                .clickable(open)
        ) {
            if (showPoster) {
                if (poster != null) {
                    Image(
                        provider = ImageProvider(poster),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.size(30.dp, 45.dp).cornerRadius(6.dp)
                    )
                } else {
                    Box(GlanceModifier.size(30.dp, 45.dp).cornerRadius(6.dp).background(ColorProvider(c.surface2))) {}
                }
                Spacer(GlanceModifier.width(10.dp))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    WidgetData.title(e),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(c.textPrimary), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    WidgetData.subtitle(e),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(c.textSecondary), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )
            }
        }
        Spacer(GlanceModifier.height(6.dp))
    }
}

internal fun WidgetList.labelRes(): Int = when (this) {
    WidgetList.WATCHING -> R.string.list_currently_watching
    WidgetList.WATCHLIST -> R.string.list_want_to_watch
    WidgetList.HISTORY -> R.string.header_history
    WidgetList.FAVORITES -> R.string.header_favorites
}

private suspend fun loadPosters(context: Context, paths: List<String>): Map<String, Bitmap> {
    val loader = context.imageLoader
    return paths.distinct().mapNotNull { path ->
        val request = ImageRequest.Builder(context)
            .data("https://image.tmdb.org/t/p/w92$path")
            .size(90, 135)
            .allowHardware(false) // RemoteViews brauchen Software-Bitmaps
            .build()
        (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap(90, 135)?.let { path to it }
    }.toMap()
}
