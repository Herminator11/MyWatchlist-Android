package com.example.mywatchlist.screenshots

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import com.example.mywatchlist.data.local.AppDatabase
import com.example.mywatchlist.data.local.MovieEntity
import com.example.mywatchlist.ui.components.sheets.MediaDetailDialog
import com.example.mywatchlist.ui.screens.CurrentlyWatchingScreen
import com.example.mywatchlist.ui.screens.FavoritesScreen
import com.example.mywatchlist.ui.screens.FloatingBottomNavigation
import com.example.mywatchlist.ui.screens.MovieTrackerTopBar
import com.example.mywatchlist.ui.screens.RecentlyWatchedScreen
import com.example.mywatchlist.ui.screens.SearchScreen
import com.example.mywatchlist.ui.screens.SettingsScreen
import com.example.mywatchlist.ui.screens.WantToWatchScreen
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.MyWatchlistTheme
import com.example.mywatchlist.ui.viewmodel.ExportList
import com.example.mywatchlist.ui.viewmodel.LibraryViewModel
import com.example.mywatchlist.ui.viewmodel.WatchListType
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// ─────────────────────────────────────────────────────────────
// A/B-SCREENSHOTS (JVM, Robolectric + Roborazzi)
//
// • Läuft komplett auf dem Mac – kein Gerät, keine echte Datenbank:
//   Room ist IN-MEMORY und mit festen Beispieldaten gefüllt.
// • Poster kommen nicht aus dem Netz, sondern als generierte Verläufe
//   (deterministisch, offline).
// • Diese Datei ist auf Variante A und B identisch, damit beide Seiten
//   des Vergleichs unter exakt denselben Bedingungen entstehen.
//
// Aufnahme:  ./gradlew recordRoborazziV2 -Pab.out=<ordner>   (Variante B)
//            ./gradlew recordRoborazziDebug -Pab.out=<ordner> (Variante A)
// ─────────────────────────────────────────────────────────────

private val PosterHues = listOf(
    0xFF3B1F2B.toInt() to 0xFFB8574A.toInt(),
    0xFF14213D.toInt() to 0xFF4F7CAC.toInt(),
    0xFF1F2D1C.toInt() to 0xFF7FA35B.toInt(),
    0xFF2A1A3D.toInt() to 0xFF9C6ADE.toInt(),
    0xFF3D2A12.toInt() to 0xFFE0A33B.toInt(),
    0xFF102A2A.toInt() to 0xFF3FA7A0.toInt(),
)

/** Coil-Interceptor: liefert sofort einen Verlauf statt eines Netzwerk-Bildes. */
private class FakePosterInterceptor(private val context: Context) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        // nur Dateiname → gleiche Farbe in A und B, egal ob w185 oder w500 geladen wird
        val seed = chain.request.data.toString().substringAfterLast('/').hashCode()
        val (a, b) = PosterHues[Math.floorMod(seed, PosterHues.size)]
        val drawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply {
            setSize(500, 750)
        }
        return SuccessResult(drawable = drawable, request = chain.request, dataSource = DataSource.MEMORY)
    }
}

private fun entity(
    id: Int, title: String, list: WatchListType, type: String, year: String,
    season: String = "", finished: String? = null, fav: Boolean = false,
    category: String? = null, sort: Int = Int.MAX_VALUE,
) = MovieEntity(
    id = id, title = title, date = "$year-01-01", posterPath = "/poster_$id.jpg",
    mediaType = type, listType = list.name, seasonNumber = season,
    finishedDate = finished, favoriteCategory = category, isFavorite = fav,
    notes = null, sortOrder = sort,
)

private val SampleData = listOf(
    // Am Schauen
    entity(1, "Severance", WatchListType.CURRENTLY_WATCHING, "tv", "2022", season = "Season 2"),
    entity(2, "The Bear", WatchListType.CURRENTLY_WATCHING, "tv", "2022", season = "Season 3"),
    entity(3, "Shōgun", WatchListType.CURRENTLY_WATCHING, "tv", "2024", season = "Season 1"),
    entity(4, "Arcane: League of Legends", WatchListType.CURRENTLY_WATCHING, "tv", "2021", season = "Season 2"),
    // Watchlist
    entity(10, "Dune: Part Two", WatchListType.WANT_TO_WATCH, "movie", "2024"),
    entity(11, "Past Lives", WatchListType.WANT_TO_WATCH, "movie", "2023"),
    entity(12, "Perfect Days", WatchListType.WANT_TO_WATCH, "movie", "2023"),
    entity(13, "The Zone of Interest", WatchListType.WANT_TO_WATCH, "movie", "2023"),
    entity(14, "Andor", WatchListType.WANT_TO_WATCH, "tv", "2022", season = "Season 1"),
    entity(15, "Slow Horses", WatchListType.WANT_TO_WATCH, "tv", "2022", season = "Season 1"),
    // Verlauf
    entity(20, "Oppenheimer", WatchListType.RECENTLY_WATCHED_MOVIES, "movie", "2023", finished = "14.09.2026"),
    entity(21, "Anatomy of a Fall", WatchListType.RECENTLY_WATCHED_MOVIES, "movie", "2023", finished = "02.08.2026"),
    entity(22, "Poor Things", WatchListType.RECENTLY_WATCHED_MOVIES, "movie", "2023", finished = "20.11.2025"),
    entity(23, "Dark", WatchListType.RECENTLY_WATCHED_TV, "tv", "2017", season = "Season 3", finished = "05.07.2026"),
    entity(24, "Fleabag", WatchListType.RECENTLY_WATCHED_TV, "tv", "2016", season = "Season 2", finished = "11.03.2026"),
    // Favoriten
    entity(30, "Better Call Saul", WatchListType.FAVORITE_SERIES, "tv", "2015", fav = true, category = "Live-Action", sort = 0),
    entity(31, "Frieren", WatchListType.FAVORITE_SERIES, "tv", "2023", fav = true, category = "Anime", sort = 1),
    entity(32, "BoJack Horseman", WatchListType.FAVORITE_SERIES, "tv", "2014", fav = true, category = "Animated", sort = 2),
    entity(33, "Twin Peaks", WatchListType.FAVORITE_SERIES, "tv", "1990", fav = true, category = "Live-Action", sort = 3),
    entity(34, "In the Mood for Love", WatchListType.FAVORITE_MOVIES, "movie", "2000", fav = true, sort = 0),
)

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class AbScreenshotBase(private val suffix: String) {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: AppDatabase
    private lateinit var vm: LibraryViewModel

    private val outDir: String = System.getProperty("ab.out") ?: "build/ab-screenshots"

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking { db.movieDao().insertOrUpdateAll(SampleData) }
        vm = LibraryViewModel(app, db.movieDao())
        Coil.setImageLoader(
            ImageLoader.Builder(app)
                .components { add(FakePosterInterceptor(app)) }
                .dispatcher(Dispatchers.Unconfined)
                .build()
        )
    }

    @After
    fun tearDown() = db.close()

    /** Nachbau der App-Hülle (TopBar + Inhalt + Glas-Navigation) für einen Tab. */
    @Composable
    private fun Shell(route: String, content: @Composable () -> Unit) {
        val nav = rememberNavController()
        val haze = remember { HazeState() }
        Scaffold(
            containerColor = AppTheme.DarkBg,
            topBar = { MovieTrackerTopBar(onProfileClick = {}) }
        ) { pad ->
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = nav,
                    startDestination = route,
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(state = haze)
                        .padding(top = pad.calculateTopPadding())
                ) { composable(route) { content() } }
                Box(Modifier.align(Alignment.BottomCenter)) {
                    FloatingBottomNavigation(navController = nav, hazeState = haze)
                }
            }
        }
    }

    private fun shoot(name: String, waitForText: String? = null, content: @Composable () -> Unit) {
        composeRule.setContent { MyWatchlistTheme { content() } }
        if (waitForText != null) {
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText(waitForText, substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        captureScreenRoboImage("$outDir/${name}_$suffix.png")
    }

    @Test fun watching() = shoot("watching", "Severance") {
        Shell("currently_watching") { CurrentlyWatchingScreen(libraryViewModel = vm) }
    }

    @Test fun watchlist() = shoot("watchlist", "Andor") {
        Shell("want_to_watch") { WantToWatchScreen(libraryViewModel = vm) }
    }

    @Test fun history() = shoot("history", "Dark") {
        Shell("recently_watched") { RecentlyWatchedScreen(libraryViewModel = vm) }
    }

    @Test fun favorites() = shoot("favorites", "Frieren") {
        Shell("favorites") { FavoritesScreen(libraryViewModel = vm) }
    }

    @Test fun search() = shoot("search") {
        Shell("search") { SearchScreen(libraryViewModel = vm) }
    }

    @Test fun settings() = shoot("settings") {
        SettingsScreen(
            onBack = {},
            autoHideNavBar = true,
            onAutoHideNavBarChanged = {},
            exportCounts = ExportList.values().associateWith { 4 },
            onBuildExportText = { null }
        )
    }

    @Test fun detail() = shoot("detail", "Severance") {
        Shell("currently_watching") { CurrentlyWatchingScreen(libraryViewModel = vm) }
        MediaDetailDialog(
            movie = SampleData.first(),
            lists = setOf(WatchListType.CURRENTLY_WATCHING),
            onDismiss = {},
            onEdit = {}
        )
    }
}

@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", fontScale = 1.0f)
class AbScreenshotFont100 : AbScreenshotBase("fs100")

@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi", fontScale = 1.5f)
class AbScreenshotFont150 : AbScreenshotBase("fs150")
