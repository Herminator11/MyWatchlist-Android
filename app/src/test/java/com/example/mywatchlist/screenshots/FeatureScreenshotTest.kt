package com.example.mywatchlist.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mywatchlist.data.NavConfig
import com.example.mywatchlist.data.AccentOption
import com.example.mywatchlist.data.AppearancePrefs
import com.example.mywatchlist.ui.screens.SettingsScreen
import com.example.mywatchlist.ui.theme.cinemaPalette
import com.example.mywatchlist.ui.viewmodel.ExportList
import com.example.mywatchlist.ui.screens.BottomNavItem
import com.example.mywatchlist.ui.screens.FloatingBottomNavigation
import com.example.mywatchlist.ui.screens.NavigationSettingsScreen
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.MyWatchlistTheme
import com.example.mywatchlist.ui.theme.cinemaBackground
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import dev.chrisbanes.haze.HazeState
import org.junit.Rule
import org.junit.Before
import org.junit.After
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.CompositionLocalProvider
import com.example.mywatchlist.data.CardDensity
import com.example.mywatchlist.data.ListPrefs
import com.example.mywatchlist.data.ListSort
import com.example.mywatchlist.data.local.AppDatabase
import com.example.mywatchlist.ui.components.cards.LocalListPrefs
import com.example.mywatchlist.ui.screens.CurrentlyWatchingScreen
import com.example.mywatchlist.ui.screens.WantToWatchScreen
import com.example.mywatchlist.ui.viewmodel.LibraryViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots neuer Features (nur Variante B). Kein Gerät, keine echten Daten.
 * ./gradlew recordRoborazziV2 --tests '*FeatureScreenshot*' -Pab.out=<ordner>
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class FeatureScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val outDir: String = System.getProperty("ab.out") ?: "build/feature-screenshots"

    private val custom = NavConfig.Default
        .move(4, 0)                       // Suche nach vorne
        .setHidden("recently_watched", true)
        .withStart("favorites")

    private fun shoot(name: String) {
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        captureScreenRoboImage("$outDir/$name.png")
    }

    @Test
    fun navigationSettings() {
        composeRule.setContent {
            MyWatchlistTheme {
                var config by remember { mutableStateOf(custom) }
                NavigationSettingsScreen(
                    config = config,
                    onBack = {},
                    onChange = { config = it(config) },
                    onReset = { config = NavConfig.Default }
                )
            }
        }
        shoot("feature_nav_settings")
    }

    @Test
    fun customNavigationBar() {
        composeRule.setContent {
            MyWatchlistTheme {
                val nav = rememberNavController()
                Box(Modifier.fillMaxSize().cinemaBackground()) {
                    NavHost(nav, startDestination = custom.start) {
                        custom.order.forEach { r -> composable(r) { Box(Modifier.fillMaxSize()) } }
                    }
                    Box(Modifier.align(Alignment.BottomCenter)) {
                        FloatingBottomNavigation(
                            navController = nav,
                            hazeState = remember { HazeState() },
                            items = custom.visible.mapNotNull(BottomNavItem::fromRoute)
                        )
                    }
                }
            }
        }
        shoot("feature_nav_custom_bar")
    }

    @Test
    fun settingsTealAccent() {
        val prefs = AppearancePrefs(accent = AccentOption.TEAL)
        composeRule.setContent {
            MyWatchlistTheme(colors = cinemaPalette(prefs.accent, prefs.amoled)) {
                SettingsScreen(
                    onBack = {}, autoHideNavBar = true, onAutoHideNavBarChanged = {},
                    appearance = prefs,
                    exportCounts = ExportList.values().associateWith { 4 },
                    onBuildExportText = { null }
                )
            }
        }
        shoot("feature_settings_teal")
    }

    @Test
    fun crimsonAmoledWithLabels() {
        composeRule.setContent {
            MyWatchlistTheme(colors = cinemaPalette(AccentOption.CRIMSON, amoled = true)) {
                val nav = rememberNavController()
                Box(Modifier.fillMaxSize().cinemaBackground()) {
                    NavHost(nav, startDestination = "currently_watching") {
                        NavConfig.Default.order.forEach { r -> composable(r) { Box(Modifier.fillMaxSize()) } }
                    }
                    Box(Modifier.align(Alignment.BottomCenter)) {
                        FloatingBottomNavigation(navController = nav, hazeState = remember { HazeState() }, showLabels = true)
                    }
                }
            }
        }
        shoot("feature_nav_labels_crimson_amoled")
    }

    private lateinit var db: AppDatabase
    private lateinit var vm: LibraryViewModel

    @Before
    fun seed() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        runBlocking { db.movieDao().insertOrUpdateAll(SampleData) }
        vm = LibraryViewModel(app, db.movieDao())
        Coil.setImageLoader(ImageLoader.Builder(app).components { add(FakePosterInterceptor(app)) }.dispatcher(Dispatchers.Unconfined).build())
    }

    @After
    fun closeDb() = db.close()

    private fun waitFor(text: String) = composeRule.waitUntil(10_000) {
        composeRule.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun watchingPosterGrid() {
        composeRule.setContent {
            MyWatchlistTheme {
                CompositionLocalProvider(LocalListPrefs provides ListPrefs(density = CardDensity.POSTER, watchingSort = ListSort.TITLE)) {
                    CurrentlyWatchingScreen(libraryViewModel = vm)
                }
            }
        }
        waitFor("Severance")
        shoot("feature_lists_poster_grid")
    }

    @Test
    fun watchlistCompactNewestMovies() {
        composeRule.setContent {
            MyWatchlistTheme {
                CompositionLocalProvider(
                    LocalListPrefs provides ListPrefs(
                        density = CardDensity.COMPACT, showTypeTag = false,
                        watchlistSort = ListSort.YEAR_NEWEST, watchlistStartTab = com.example.mywatchlist.data.MediaTab.MOVIES
                    )
                ) { WantToWatchScreen(libraryViewModel = vm) }
            }
        }
        waitFor("Dune")
        shoot("feature_lists_compact")
    }
}
