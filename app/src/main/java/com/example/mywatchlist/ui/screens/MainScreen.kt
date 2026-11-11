package com.example.mywatchlist.ui.screens
import com.example.mywatchlist.data.ListPrefs
import com.example.mywatchlist.data.AppearancePrefs
import com.example.mywatchlist.ui.theme.LocalHapticsEnabled
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mywatchlist.ui.theme.CinemaMotion
import com.example.mywatchlist.ui.theme.LocalReduceMotion
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.example.mywatchlist.R
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Text
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalView
import com.example.mywatchlist.ui.theme.AppTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale

// ─────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieTrackerTopBar(onProfileClick: () -> Unit) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = AppTheme.DarkBg
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(AppTheme.GoldBright, AppTheme.YellowAccent)
                            )
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.app_wordmark),
                    fontFamily = AppTheme.Display,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Normal,
                    color = AppTheme.TextPrimary
                )
            }
        },
        actions = {
            // IconButton statt klickbarem Icon → 48-dp-Touch-Ziel + Button-Rolle
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    modifier = Modifier.size(26.dp),
                    tint = AppTheme.TextSecondary
                )
            }
        }
    )
}

// ─────────────────────────────────────────────
// NAV ITEMS
// ─────────────────────────────────────────────
sealed class BottomNavItem(
    val route: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    @androidx.annotation.StringRes val labelRes: Int
) {
    object WantToWatch : BottomNavItem(
        route = "want_to_watch",
        activeIcon = Icons.Filled.Bookmark,
        inactiveIcon = Icons.Outlined.BookmarkBorder,
        labelRes = R.string.header_watchlist
    )

    object CurrentlyWatching : BottomNavItem(
        route = "currently_watching",
        activeIcon = Icons.Filled.LiveTv,
        inactiveIcon = Icons.Outlined.LiveTv,
        labelRes = R.string.header_watching
    )

    object RecentlyWatched : BottomNavItem(
        route = "recently_watched",
        activeIcon = Icons.Filled.History,
        inactiveIcon = Icons.Outlined.History,
        labelRes = R.string.header_history
    )

    object Favorites : BottomNavItem(
        route = "favorites",
        activeIcon = Icons.Filled.Favorite,
        inactiveIcon = Icons.Outlined.FavoriteBorder,
        labelRes = R.string.header_favorites
    )

    object Search : BottomNavItem(
        route = "search",
        activeIcon = Icons.Filled.Search,
        inactiveIcon = Icons.Outlined.Search,
        labelRes = R.string.nav_search
    )

    companion object {
        /** Alle Tabs in der Standard-Reihenfolge. */
        val all: List<BottomNavItem> by lazy { listOf(WantToWatch, CurrentlyWatching, RecentlyWatched, Favorites, Search) }

        fun fromRoute(route: String): BottomNavItem? = all.firstOrNull { it.route == route }
    }
}

const val ROUTE_SETTINGS = "settings"
const val ROUTE_SETTINGS_NAVIGATION = "settings/navigation"

@Composable
fun MainScreenApp(
    openTabRequest: String? = null,
    onOpenTabHandled: () -> Unit = {},
) {
    val rootNavController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val autoHideNavBar by settingsViewModel.autoHideNavBar.collectAsStateWithLifecycle()
    val appearance by settingsViewModel.appearance.collectAsStateWithLifecycle()
    val listPrefs by settingsViewModel.listPrefs.collectAsStateWithLifecycle()
    val contentPrefs by settingsViewModel.contentPrefs.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Widget-Tipp während Einstellungen offen sind → zurück zur Hauptansicht
    LaunchedEffect(openTabRequest) {
        if (openTabRequest != null) rootNavController.popBackStack("main", inclusive = false)
    }

    NavHost(
        navController = rootNavController,
        startDestination = "main"
    ) {
        composable("main") {
            MainContent(
                onOpenSettings = { rootNavController.navigate(ROUTE_SETTINGS) },
                openTabRequest = openTabRequest,
                onOpenTabHandled = onOpenTabHandled
            )
        }
        composable(ROUTE_SETTINGS) {
            // Export-Zähler reaktiv aus den gefilterten StateFlows
            val exportCounts by libraryViewModel.exportCountsState.collectAsStateWithLifecycle()
            SettingsScreen(
                onBack = { rootNavController.popBackStack() },
                autoHideNavBar = autoHideNavBar,
                onAutoHideNavBarChanged = { settingsViewModel.setAutoHideNavBar(it) },
                onOpenNavigation = { rootNavController.navigate(ROUTE_SETTINGS_NAVIGATION) },
                appearance = appearance ?: AppearancePrefs(),
                onAppearanceChange = { settingsViewModel.updateAppearance(it) },
                listPrefs = listPrefs ?: ListPrefs(),
                onListPrefsChange = { settingsViewModel.updateListPrefs(it) },
                contentPrefs = contentPrefs,
                onContentPrefsChange = { settingsViewModel.updateContentPrefs(it) },
                onBuildBackup = { libraryViewModel.buildBackupJson() },
                onCountMissing = { libraryViewModel.countMissingFromBackup(it) },
                onRestoreMissing = { libraryViewModel.restoreMissingFromBackup(it) },
                exportCounts = exportCounts,
                onBuildExportText = { selection ->
                    libraryViewModel.buildExportText(context, selection)
                }
            )
        }
        composable(ROUTE_SETTINGS_NAVIGATION) {
            val navConfig by settingsViewModel.navConfig.collectAsStateWithLifecycle()
            navConfig?.let { config ->
                NavigationSettingsScreen(
                    config = config,
                    onBack = { rootNavController.popBackStack() },
                    onChange = { transform -> settingsViewModel.updateNavConfig(transform) },
                    onReset = { settingsViewModel.resetNavConfig() }
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    onOpenSettings: () -> Unit,
    openTabRequest: String? = null,
    onOpenTabHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val settingsViewModel: SettingsViewModel = viewModel()

    val autoHideNavBar by settingsViewModel.autoHideNavBar.collectAsStateWithLifecycle()
    val navConfig by settingsViewModel.navConfig.collectAsStateWithLifecycle()
    val appearance by settingsViewModel.appearance.collectAsStateWithLifecycle()

    // App-weite Undo-Snackbar fürs Löschen aus Listen
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(libraryViewModel) {
        libraryViewModel.events.collect { event ->
            when (event) {
                is LibraryViewModel.LibraryEvent.Deleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.snackbar_deleted, event.movie.title),
                        actionLabel = resources.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        libraryViewModel.undoDelete(event.movie)
                    }
                }
            }
        }
    }

    val bottomBarHeight = 120.dp
    val bottomBarHeightPx = with(LocalDensity.current) { bottomBarHeight.toPx() }
    val hazeState = remember { HazeState() }
    val bottomBarOffsetHeightPx = remember { mutableFloatStateOf(0f) }

    // Wenn autoHideNavBar deaktiviert wird, Nav-Bar sofort wieder einblenden
    LaunchedEffect(autoHideNavBar) {
        if (!autoHideNavBar) bottomBarOffsetHeightPx.floatValue = 0f
    }

    val nestedScrollConnection = remember(autoHideNavBar, bottomBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!autoHideNavBar) return Offset.Zero  // Sticky: nicht verschieben
                val delta = available.y
                val newOffset = bottomBarOffsetHeightPx.floatValue - delta
                bottomBarOffsetHeightPx.floatValue = newOffset.coerceIn(0f, bottomBarHeightPx)
                return Offset.Zero
            }
        }
    }

    // Bis die gespeicherte Tab-Konfiguration geladen ist (wenige ms), nur den
    // Hintergrund zeigen – so startet die App direkt im gewählten Start-Tab.
    val config = navConfig ?: run {
        Box(Modifier.fillMaxSize().background(AppTheme.DarkBg))
        return
    }

    // Vom Widget angeforderten Tab öffnen (alle Routen sind im Graph, auch ausgeblendete)
    LaunchedEffect(openTabRequest) {
        val route = openTabRequest ?: return@LaunchedEffect
        if (BottomNavItem.fromRoute(route) != null) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        onOpenTabHandled()
    }

    Scaffold(
        containerColor = AppTheme.DarkBg,
        topBar = {
            MovieTrackerTopBar(onProfileClick = onOpenSettings)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            NavHost(
                navController = navController,
                startDestination = config.start,
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState)
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                composable(BottomNavItem.WantToWatch.route) { WantToWatchScreen(libraryViewModel = libraryViewModel) }
                composable(BottomNavItem.CurrentlyWatching.route) { CurrentlyWatchingScreen(libraryViewModel = libraryViewModel) }
                composable(BottomNavItem.RecentlyWatched.route) { RecentlyWatchedScreen(libraryViewModel = libraryViewModel) }
                composable(BottomNavItem.Favorites.route) { FavoritesScreen(libraryViewModel = libraryViewModel) }
                composable(BottomNavItem.Search.route) { SearchScreen(libraryViewModel = libraryViewModel) }
            }

            // DIE FLOATING-EBENE
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(x = 0, y = bottomBarOffsetHeightPx.floatValue.roundToInt()) }
            ) {
                FloatingBottomNavigation(
                    navController = navController,
                    hazeState = hazeState,
                    items = config.visible.mapNotNull(BottomNavItem::fromRoute),
                    showLabels = appearance?.navLabels == true
                )
            }

            // Undo-Snackbar über der Floating-Navigation
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp, start = 12.dp, end = 12.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AppTheme.DarkSurface,
                    contentColor = AppTheme.TextPrimary,
                    actionColor = AppTheme.YellowAccent,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBottomNavigation(
    navController: NavController,
    hazeState: HazeState,
    items: List<BottomNavItem> = BottomNavItem.all,
    showLabels: Boolean = false,
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val hapticsEnabled = LocalHapticsEnabled.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars) // Verhindert Konflikte mit Android-Gesten
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showLabels) 72.dp else 64.dp)
                .hazeChild(
                    state = hazeState,
                    shape = RoundedCornerShape(32.dp),
                    style = HazeStyle(
                        blurRadius = 15.dp,
                        tint = AppTheme.DarkSurface.copy(alpha = 0.6f)
                    )
                ),
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val tooltipState = rememberTooltipState()

                    // Wir überwachen, ob der Nutzer den Button gerade physisch gedrückt hält
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    // Animierte Farbe für die Hintergrundkapsel (sanftes Ein-/Ausblenden)
                    val animatedBackgroundColor by animateColorAsState(
                        targetValue = if (isSelected) AppTheme.YellowAccent.copy(alpha = 0.16f) else Color.Transparent,
                        animationSpec = CinemaMotion.effects(reduceMotion),
                        label = "pillColor"
                    )

                    // Physikalischer Bounce-Effekt für das Icon
                    val iconScale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.8f    // Icon schrumpft auf 80%
                            isSelected -> 1.15f  // Icon wird 15% größer
                            else -> 1.0f         // Normalzustand
                        },
                        animationSpec = CinemaMotion.bouncy(reduceMotion), // Signatur-Bounce
                        label = "iconScale"
                    )

                    LaunchedEffect(tooltipState.isVisible) {
                        if (tooltipState.isVisible && hapticsEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    // Animierte Farbe
                                    color = animatedBackgroundColor,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                // HIER übergeben wir die interactionSource für die isPressed-Erkennung
                                // selectable + Role.Tab → TalkBack sagt "Tab, ausgewählt"
                                .selectable(
                                    selected = isSelected,
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab
                                ) {
                                    if (currentRoute != item.route) {
                                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                .padding(
                                    horizontal = if (showLabels) 6.dp else 14.dp,
                                    vertical = if (showLabels) 5.dp else 12.dp // 24 + 2×12 = 48 dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(
                                    spacingBetweenTooltipAndAnchor = 24.dp
                                ),
                                tooltip = { PlainTooltip { Text(stringResource(item.labelRes)) } },
                                state = tooltipState
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                        contentDescription = stringResource(item.labelRes),
                                        tint = if (isSelected) AppTheme.YellowAccent else AppTheme.TextSecondary,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .scale(iconScale)
                                    )
                                    // Optionale Beschriftung (Einstellung „Nav-Labels“)
                                    if (showLabels) {
                                        Text(
                                            text = stringResource(item.labelRes),
                                            fontFamily = AppTheme.Body,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            color = if (isSelected) AppTheme.YellowAccent else AppTheme.TextSecondary,
                                            modifier = Modifier.clearAndSetSemantics { } // Icon trägt schon das Label
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
