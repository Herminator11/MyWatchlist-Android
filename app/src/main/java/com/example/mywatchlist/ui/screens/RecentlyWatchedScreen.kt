package com.example.mywatchlist.ui.screens
import androidx.compose.ui.res.stringResource
import com.example.mywatchlist.R
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.mywatchlist.ui.components.cards.UniversalMediaCard
import com.example.mywatchlist.ui.components.common.ScreenHeader
import com.example.mywatchlist.ui.components.sheets.GlobalEditSheet
import com.example.mywatchlist.ui.components.sheets.MediaDetailDialog
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.cinemaBackground
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
    var movieForDetail by remember { mutableStateOf<MovieEntity?>(null) }
    // Lokaler Filter, um einen bereits gesehenen Titel schnell zu finden
    var filterQuery by remember { mutableStateOf("") }
    val blurRadius by animateDpAsState(
        targetValue = if (movieForDetail != null) 18.dp else 0.dp,
        label = "detail_blur"
    )

    val tabs = listOf(stringResource(R.string.tab_series), stringResource(R.string.tab_movies))
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Scroll-Zustand je Tab herausgehoben, damit die Filterleiste weiß,
    // ob die aktuell sichtbare Liste ganz oben steht.
    val tvListState = rememberLazyListState()
    val movieListState = rememberLazyListState()
    val currentListState = if (pagerState.currentPage == 0) tvListState else movieListState
    // Leiste nur zeigen, wenn man ganz oben ist – oder während gefiltert wird,
    // damit das Feld beim Tippen nicht unter einem wegscrollt.
    val showFilterBar by remember {
        derivedStateOf {
            filterQuery.isNotEmpty() ||
                (currentListState.firstVisibleItemIndex == 0 &&
                    currentListState.firstVisibleItemScrollOffset == 0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
            .blur(blurRadius)
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

        // Titel-Filter (nur Recently Watched) – blendet sich beim Scrollen aus
        // und erscheint nur, wenn die Liste ganz oben steht.
        AnimatedVisibility(
            visible = showFilterBar,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FilterBar(
                query = filterQuery,
                onQueryChange = { filterQuery = it },
                onClear = { filterQuery = "" }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // Liste kommt bereits sortiert aus dem ViewModel
            val allEntries = if (page == 0) tvEntries else movieEntries
            // Lokaler Filter nach Titel (case-insensitive)
            val entries = remember(allEntries, filterQuery) {
                if (filterQuery.isBlank()) allEntries
                else allEntries.filter { it.title.contains(filterQuery.trim(), ignoreCase = true) }
            }

            // Gruppierung nach Jahr – findet nur noch die Jahreszahl, kein Regex mehr
            val grouped: Map<String, List<MovieEntity>> = remember(entries) {
                entries.groupBy { entity ->
                    val raw = entity.finishedDate ?: entity.date
                    Regex("""\d{4}""").find(raw)?.value ?: "Unbekannt"
                }
            }

            if (grouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (filterQuery.isBlank()) stringResource(R.string.empty_history)
                        else stringResource(R.string.recently_no_match),
                        color = AppTheme.TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    state = if (page == 0) tvListState else movieListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                ) {
                    item { ScreenHeader(title = stringResource(R.string.list_recently_watched)) }

                    grouped.forEach { (year, yearEntries) ->
                        item(key = "header_${page}_$year") {
                            YearHeader(year = year)
                        }
                        items(yearEntries, key = { "${it.id}_${it.listType}_${it.seasonNumber}" }) { movieEntity ->
                            UniversalMediaCard(
                                item = movieEntity,
                                onClick = { movieForDetail = movieEntity },
                                onLongClick = { selectedMovieForEdit = movieEntity }
                            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        placeholder = {
            Text(stringResource(R.string.recently_filter_hint), color = AppTheme.TextSecondary, fontSize = 14.sp)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear), tint = AppTheme.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppTheme.YellowAccent,
            unfocusedBorderColor = AppTheme.TextTertiary,
            focusedContainerColor = AppTheme.DarkSurface,
            unfocusedContainerColor = AppTheme.DarkSurface,
            cursorColor = AppTheme.YellowAccent,
            focusedTextColor = AppTheme.TextPrimary,
            unfocusedTextColor = AppTheme.TextPrimary
        )
    )
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