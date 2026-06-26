package com.example.mywatchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.mywatchlist.components.cards.SwipeableMediaCard
import com.example.mywatchlist.components.cards.UniversalMediaCard
import com.example.mywatchlist.components.common.ScreenHeader
import com.example.mywatchlist.components.sheets.GlobalEditSheet
import com.example.mywatchlist.ui.theme.AppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyWatchedScreen(
    modifier: Modifier = Modifier,
    libraryViewModel: LibraryViewModel
) {
    // Gefilterte + vorsortierte StateFlows – keine Verarbeitung mehr im Composable
    val tvEntries by libraryViewModel.recentlyWatchedTv.collectAsState()
    val movieEntries by libraryViewModel.recentlyWatchedMovies.collectAsState()
    var selectedMovieForEdit by remember { mutableStateOf<MovieEntity?>(null) }

    val tabs = listOf("Serien", "Filme")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.DarkBg)
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = AppTheme.DarkSurface,
            contentColor = AppTheme.TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = AppTheme.YellowAccent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            color = if (pagerState.currentPage == index)
                                AppTheme.TextPrimary else AppTheme.TextSecondary
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // Liste kommt bereits sortiert aus dem ViewModel
            val entries = if (page == 0) tvEntries else movieEntries

            // Gruppierung nach Jahr – findet nur noch die Jahreszahl, kein Regex mehr
            val grouped: Map<String, List<MovieEntity>> = remember(entries) {
                entries.groupBy { entity ->
                    val raw = entity.finishedDate ?: entity.date
                    Regex("""\d{4}""").find(raw)?.value ?: "Unbekannt"
                }
            }

            if (grouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Diese Liste ist noch leer.", color = AppTheme.TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                ) {
                    item { ScreenHeader(title = "History") }

                    grouped.forEach { (year, yearEntries) ->
                        item(key = "header_${page}_$year") {
                            YearHeader(year = year)
                        }
                        items(yearEntries, key = { "${it.id}_${it.listType}_${it.seasonNumber}" }) { movieEntity ->
                            SwipeableMediaCard(
                                item = movieEntity,
                                onSwipe = { selectedMovieForEdit = movieEntity }
                            ) {
                                UniversalMediaCard(item = movieEntity)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedMovieForEdit?.let { movie ->
        GlobalEditSheet(
            movie = movie,
            libraryViewModel = libraryViewModel,
            onDismiss = { selectedMovieForEdit = null }
        )
    }
}

@Composable
private fun YearHeader(year: String) {
    Text(
        text = year,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.TextSecondary,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}