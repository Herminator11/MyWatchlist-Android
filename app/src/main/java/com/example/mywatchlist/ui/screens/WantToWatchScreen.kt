package com.example.mywatchlist.ui.screens
import androidx.compose.ui.res.stringResource
import com.example.mywatchlist.R
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mywatchlist.data.remote.TmdbMovie
import com.example.mywatchlist.ui.components.cards.UniversalMediaCard
import com.example.mywatchlist.ui.components.common.ScreenHeader
import com.example.mywatchlist.ui.components.sheets.GlobalEditSheet
import com.example.mywatchlist.ui.components.sheets.MediaDetailDialog
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.cinemaBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WantToWatchScreen(
    modifier: Modifier = Modifier,
    libraryViewModel: LibraryViewModel
) {
    // Gefilterte + vorsortierte StateFlows direkt aus dem ViewModel
    val tvEntries by libraryViewModel.wantToWatchTv.collectAsState()
    val movieEntries by libraryViewModel.wantToWatchMovies.collectAsState()
    var selectedMovieForEdit by remember { mutableStateOf<MovieEntity?>(null) }
    var selectedMovieForMove by remember { mutableStateOf<MovieEntity?>(null) }
    var movieForDetail by remember { mutableStateOf<MovieEntity?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(
        targetValue = if (movieForDetail != null) 18.dp else 0.dp,
        label = "detail_blur"
    )

    val tabs = listOf(stringResource(R.string.tab_series), stringResource(R.string.tab_movies))
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
            .blur(blurRadius)
    ) {
        // Aktionsleiste: direkter Einstieg in die Titelsuche → "Want to Watch"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { showSearch = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AppTheme.YellowAccent,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.wtw_search_open)
                )
            }
        }

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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val isTvPage = page == 0
            // Liste kommt bereits gefiltert + alphabetisch sortiert aus dem ViewModel
            val entries = if (isTvPage) tvEntries else movieEntries

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.empty_watchlist),
                        color = AppTheme.TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
                ) {
                    item { ScreenHeader(title = stringResource(R.string.list_want_to_watch)) }
                    items(entries, key = { "${it.id}_${it.listType}_${it.seasonNumber}" }) { entity ->
                        UniversalMediaCard(
                            item = entity,
                            onMoveClick = { selectedMovieForMove = entity },
                            onClick = { movieForDetail = entity },
                            onLongClick = { selectedMovieForEdit = entity }
                        )
                    }
                }
            }
        }
    }

    // Such-Sheet: Titel suchen und direkt der "Want to Watch"-Liste hinzufügen
    if (showSearch) {
        WantToWatchSearchSheet(
            libraryViewModel = libraryViewModel,
            onDismiss = { showSearch = false }
        )
    }

    // GlobalEditSheet – Standardbearbeitung per Swipe
    selectedMovieForEdit?.let { movie ->
        GlobalEditSheet(
            movie = movie,
            libraryViewModel = libraryViewModel,
            onDismiss = { selectedMovieForEdit = null }
        )
    }

    // Detail-Popup
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

    // Move-Sheet – kontextabhängig je nach Medientyp
    selectedMovieForMove?.let { movie ->
        if (movie.mediaType == "tv") {
            MoveToWatchingSheet(
                movie = movie,
                onConfirm = { seasonNumber ->
                    libraryViewModel.moveToCurrentlyWatching(movie, seasonNumber)
                    selectedMovieForMove = null
                },
                onDismiss = { selectedMovieForMove = null }
            )
        } else {
            MoveToWatchedSheet(
                movie = movie,
                onConfirm = { date ->
                    libraryViewModel.moveToRecentlyWatched(movie, date)
                    selectedMovieForMove = null
                },
                onDismiss = { selectedMovieForMove = null }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Sheet: Titel suchen → direkt in "Want to Watch"
// Wiederverwendet SearchViewModel (TMDB) und LibraryViewModel.addNewCard.
// Da die Liste aus Room-StateFlows gespeist wird, aktualisiert sich der
// Screen nach dem Hinzufügen automatisch per Recomposition.
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WantToWatchSearchSheet(
    libraryViewModel: LibraryViewModel,
    onDismiss: () -> Unit,
    searchViewModel: SearchViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }
    // Lokal hinzugefügte IDs → sofortiges Feedback (Häkchen), bevor der
    // Room-Flow zurückspielt; isFavorite deckt bereits gespeicherte ab.
    var addedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Debounced Suche, identisch zum globalen Such-Screen
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            delay(300)
            searchViewModel.performSearch(query)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.TextTertiary)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.wtw_search_title),
                fontFamily = AppTheme.Display,
                fontSize = 24.sp,
                color = AppTheme.TextPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(stringResource(R.string.search_hint), color = AppTheme.TextSecondary, fontSize = 15.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear), tint = AppTheme.TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.YellowAccent,
                    unfocusedBorderColor = AppTheme.TextTertiary,
                    focusedContainerColor = AppTheme.DarkSurface,
                    unfocusedContainerColor = AppTheme.DarkSurface,
                    cursorColor = AppTheme.YellowAccent,
                    focusedTextColor = AppTheme.TextPrimary,
                    unfocusedTextColor = AppTheme.TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                searchViewModel.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.YellowAccent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
                query.isNotBlank() && searchViewModel.searchResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.search_no_results_for),
                            color = AppTheme.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(
                            searchViewModel.searchResults,
                            key = { it.id.toString() + (it.mediaType ?: "") }
                        ) { movie ->
                            val saved = movie.id in addedIds || libraryViewModel.isFavorite(movie.id)
                            SearchAddRow(
                                movie = movie,
                                alreadySaved = saved,
                                onAdd = {
                                    libraryViewModel.addNewCard(movie, WatchListType.WANT_TO_WATCH)
                                    addedIds = addedIds + movie.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
private fun SearchAddRow(
    movie: TmdbMovie,
    alreadySaved: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.DarkSurface2)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = movie.fullPosterPath,
            contentDescription = movie.displayTitle,
            modifier = Modifier
                .size(46.dp, 66.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = movie.displayTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listOfNotNull(
                    movie.displayDate.take(4).ifBlank { null },
                    movie.displayMediaType
                ).joinToString("  ·  "),
                fontSize = 12.sp,
                color = AppTheme.TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (alreadySaved) AppTheme.DarkSurface else AppTheme.YellowAccent)
                .let { if (!alreadySaved) it.clickable { onAdd() } else it },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (alreadySaved) Icons.Default.Check else Icons.Default.Add,
                contentDescription = if (alreadySaved)
                    stringResource(R.string.cd_already_saved)
                else
                    stringResource(R.string.wtw_search_added),
                tint = if (alreadySaved) AppTheme.TextSecondary else Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Sheet: Serie → Currently Watching
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToWatchingSheet(
    movie: MovieEntity,
    onConfirm: (seasonNumber: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.TextTertiary)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = movie.title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AppTheme.TextPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.move_to_watching_question),
                fontSize = 13.sp,
                color = AppTheme.TextSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onConfirm(movie.seasonNumber) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.YellowAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.move_start_watching), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_cancel), color = AppTheme.TextSecondary, fontSize = 15.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Sheet: Film → Recently Watched Movies
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToWatchedSheet(
    movie: MovieEntity,
    onConfirm: (date: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedDateDisplay by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.TextTertiary)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = movie.title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AppTheme.TextPrimary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.move_mark_watched),
                fontSize = 13.sp,
                color = AppTheme.TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = AppTheme.TextTertiary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.label_watched_on_caps),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.TextSecondary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedDateDisplay.isNotBlank()) selectedDateDisplay
                    else stringResource(R.string.date_pick),
                    fontSize = 16.sp,
                    color = if (selectedDateDisplay.isNotBlank()) AppTheme.TextPrimary
                    else AppTheme.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = { showDatePickerDialog = true }) {
                    Text(
                        text = if (selectedDateDisplay.isBlank()) stringResource(R.string.action_select) else stringResource(R.string.action_change),
                        color = AppTheme.YellowAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onConfirm(selectedDateDisplay) },
                enabled = selectedDateDisplay.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.YellowAccent,
                    contentColor = Color.Black,
                    disabledContainerColor = AppTheme.DarkSurface2,
                    disabledContentColor = AppTheme.TextSecondary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.move_mark_watched), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val sdf = java.text.SimpleDateFormat(
                                "dd.MM.yyyy", java.util.Locale.getDefault()
                            )
                            selectedDateDisplay = sdf.format(java.util.Date(millis))
                            selectedDateMillis = millis
                        }
                        showDatePickerDialog = false
                    }
                ) { Text(stringResource(R.string.action_confirm), color = AppTheme.YellowAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = AppTheme.TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = AppTheme.DarkSurface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = AppTheme.DarkSurface,
                    titleContentColor = AppTheme.TextPrimary,
                    headlineContentColor = AppTheme.TextPrimary,
                    weekdayContentColor = AppTheme.TextSecondary,
                    subheadContentColor = AppTheme.TextSecondary,
                    dayContentColor = AppTheme.TextPrimary,
                    selectedDayContainerColor = AppTheme.YellowAccent,
                    selectedDayContentColor = Color.Black,
                    todayContentColor = AppTheme.YellowAccent,
                    todayDateBorderColor = AppTheme.YellowAccent
                )
            )
        }
    }
}