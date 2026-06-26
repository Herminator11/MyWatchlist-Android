package com.example.mywatchlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mywatchlist.components.cards.UniversalMediaCard
import com.example.mywatchlist.components.sheets.GlobalEditSheet
import com.example.mywatchlist.components.sheets.MediaDetailDialog
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.cinemaBackground
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    libraryViewModel: LibraryViewModel
) {
    val allSavedMovies by libraryViewModel.allMovies.collectAsState()
    var selectedMovieForEdit by remember { mutableStateOf<MovieEntity?>(null) }
    var movieForDetail by remember { mutableStateOf<MovieEntity?>(null) }
    val blurRadius by animateDpAsState(
        targetValue = if (movieForDetail != null) 18.dp else 0.dp,
        label = "detail_blur"
    )

    val tabs = listOf("Lieblingsserien", "Lieblingsfilme")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val categories = listOf("Alle", "Realserie", "Animated", "Anime")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
            .blur(blurRadius)
    ) {
        // TAB-LEISTE
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = AppTheme.DarkSurface,
            contentColor = AppTheme.TextPrimary,
            indicator = { tabPositions ->
                // Korrekter Import: androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = AppTheme.YellowAccent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
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

        // FILTER-CHIPS (nur Serien-Tab)
        if (pagerState.currentPage == 0) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.YellowAccent,
                            selectedLabelColor = Color.Black,
                            containerColor = AppTheme.DarkSurface2,
                            labelColor = AppTheme.TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = AppTheme.TextTertiary,
                            selectedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // PAGER
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val listType = if (page == 0)
                WatchListType.FAVORITE_SERIES.name
            else
                WatchListType.FAVORITE_MOVIES.name

            val rawList = if (page == 0) {
                allSavedMovies.filter {
                    it.listType == listType &&
                            (selectedCategory == "Alle" || it.favoriteCategory == selectedCategory)
                }
            } else {
                allSavedMovies.filter { it.listType == listType }
            }.sortedBy { it.sortOrder }

            // Lokale veränderliche Liste für sofortiges UI-Feedback
            var displayList by remember(rawList) { mutableStateOf(rawList) }

            // LazyListState separat halten – korrekte API für 2.4.0
            val lazyListState = rememberLazyListState()

            val reorderableState = rememberReorderableLazyListState(
                lazyListState = lazyListState
            ) { from, to ->
                // onMove: sofortiges UI-Update
                displayList = displayList.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
                // Reihenfolge nach jedem Move persistieren
                libraryViewModel.reorderFavorites(displayList)
            }

            if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Titel in dieser Kategorie.", color = AppTheme.TextSecondary)
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                ) {
                    items(
                        items = displayList,
                        key = { "${it.id}_${it.listType}_${it.seasonNumber}" }
                    ) { movieEntity ->
                        ReorderableItem(
                            state = reorderableState,
                            key = "${movieEntity.id}_${movieEntity.listType}_${movieEntity.seasonNumber}"
                        ) { isDragging ->
                            val bgColor = if (isDragging)
                                AppTheme.DarkSurface2 else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgColor, RoundedCornerShape(16.dp))
                            ) {
                                UniversalMediaCard(
                                    item = movieEntity.copy(finishedDate = null),
                                    showFavoriteBadge = true,
                                    onClick = { movieForDetail = movieEntity },
                                    onLongClick = { selectedMovieForEdit = movieEntity },
                                    dragHandle = {
                                        Box(
                                            modifier = Modifier
                                                .draggableHandle()
                                                .padding(end = 14.dp)
                                                .size(38.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Reihenfolge ändern",
                                                tint = AppTheme.TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                )
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

    movieForDetail?.let { movie ->
        MediaDetailDialog(
            movie = movie,
            lists = libraryViewModel.getListsForEntity(movie),
            onDismiss = { movieForDetail = null },
            onEdit = {
                movieForDetail = null
                selectedMovieForEdit = movie
            }
        )
    }
}