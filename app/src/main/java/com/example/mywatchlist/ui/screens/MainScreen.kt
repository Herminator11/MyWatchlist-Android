package com.example.mywatchlist.ui.screens
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                    text = "Watchlist",
                    fontFamily = AppTheme.Display,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Normal,
                    color = AppTheme.TextPrimary
                )
            }
        },
        actions = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Einstellungen",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() },
                tint = AppTheme.TextSecondary
            )
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
    val label: String
) {
    object WantToWatch : BottomNavItem(
        route = "want_to_watch",
        activeIcon = Icons.Filled.Bookmark,
        inactiveIcon = Icons.Outlined.BookmarkBorder,
        label = "Watchlist"
    )

    object CurrentlyWatching : BottomNavItem(
        route = "currently_watching",
        activeIcon = Icons.Filled.LiveTv,
        inactiveIcon = Icons.Outlined.LiveTv,
        label = "Watching"
    )

    object RecentlyWatched : BottomNavItem(
        route = "recently_watched",
        activeIcon = Icons.Filled.History,
        inactiveIcon = Icons.Outlined.History,
        label = "History"
    )

    object Favorites : BottomNavItem(
        route = "favorites",
        activeIcon = Icons.Filled.Favorite,
        inactiveIcon = Icons.Outlined.FavoriteBorder,
        label = "Favorites"
    )

    object Search : BottomNavItem(
        route = "search",
        activeIcon = Icons.Filled.Search,
        inactiveIcon = Icons.Outlined.Search,
        label = "Search"
    )
}

const val ROUTE_SETTINGS = "settings"

@Composable
fun MainScreenApp() {
    val rootNavController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val autoHideNavBar by settingsViewModel.autoHideNavBar.collectAsState()
    val recentlyWatchedTv by libraryViewModel.recentlyWatchedTv.collectAsState()
    val recentlyWatchedMovies by libraryViewModel.recentlyWatchedMovies.collectAsState()
    val historyIsEmpty = recentlyWatchedTv.isEmpty() && recentlyWatchedMovies.isEmpty()

    val context = androidx.compose.ui.platform.LocalContext.current

    NavHost(
        navController = rootNavController,
        startDestination = "main"
    ) {
        composable("main") {
            MainContent(
                onOpenSettings = { rootNavController.navigate(ROUTE_SETTINGS) }
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onBack = { rootNavController.popBackStack() },
                autoHideNavBar = autoHideNavBar,
                onAutoHideNavBarChanged = { settingsViewModel.setAutoHideNavBar(it) },
                historyIsEmpty = historyIsEmpty,
                onExportHistory = {
                    val file = libraryViewModel.buildExportFile(context)
                    if (file != null) {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val shareIntent = android.content.Intent(
                            android.content.Intent.ACTION_SEND
                        ).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            putExtra(
                                android.content.Intent.EXTRA_SUBJECT,
                                "MyWatchlist History Export"
                            )
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(
                                shareIntent,
                                "History teilen"
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun MainContent(onOpenSettings: () -> Unit) {
    val navController = rememberNavController()
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val settingsViewModel: SettingsViewModel = viewModel()

    val autoHideNavBar by settingsViewModel.autoHideNavBar.collectAsState()

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
                startDestination = BottomNavItem.CurrentlyWatching.route,
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
                FloatingBottomNavigation(navController = navController, hazeState = hazeState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBottomNavigation(navController: NavController, hazeState: HazeState) {
    val items = listOf(
        BottomNavItem.WantToWatch,
        BottomNavItem.CurrentlyWatching,
        BottomNavItem.RecentlyWatched,
        BottomNavItem.Favorites,
        BottomNavItem.Search
    )
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

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
                .height(64.dp)
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
                    .padding(horizontal = 16.dp),
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
                        animationSpec = tween(durationMillis = 250), // 250ms Dauer für weichen Übergang
                        label = "pillColor"
                    )

                    // Physikalischer Bounce-Effekt für das Icon
                    val iconScale by animateFloatAsState(
                        targetValue = when {
                            isPressed -> 0.8f    // Icon schrumpft auf 80%
                            isSelected -> 1.15f  // Icon wird 15% größer
                            else -> 1.0f         // Normalzustand
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy, // Mittlere Federung
                            stiffness = Spring.StiffnessLow                 // Weiche Bewegung
                        ),
                        label = "iconScale"
                    )

                    LaunchedEffect(tooltipState.isVisible) {
                        if (tooltipState.isVisible) {
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
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (currentRoute != item.route) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(
                                    spacingBetweenTooltipAndAnchor = 24.dp
                                ),
                                tooltip = { PlainTooltip { Text(item.label) } },
                                state = tooltipState
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) AppTheme.YellowAccent else AppTheme.TextSecondary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .scale(iconScale)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
