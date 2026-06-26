package com.example.mywatchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywatchlist.components.cards.SwipeableMediaCard
import com.example.mywatchlist.components.cards.UniversalMediaCard
import com.example.mywatchlist.components.common.ScreenHeader
import com.example.mywatchlist.components.sheets.GlobalEditSheet
import com.example.mywatchlist.ui.theme.AppTheme
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
                        text = "Noch nichts auf der Merkliste.",
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
                    item { ScreenHeader(title = "Watchlist") }
                    items(entries, key = { "${it.id}_${it.listType}_${it.seasonNumber}" }) { entity ->
                        SwipeableMediaCard(
                            item = entity,
                            onSwipe = { selectedMovieForEdit = entity }
                        ) {
                            UniversalMediaCard(
                                item = entity,
                                onMoveClick = { selectedMovieForMove = entity }
                            )
                        }
                    }
                }
            }
        }
    }

    // GlobalEditSheet – Standardbearbeitung per Swipe
    selectedMovieForEdit?.let { movie ->
        GlobalEditSheet(
            movie = movie,
            libraryViewModel = libraryViewModel,
            onDismiss = { selectedMovieForEdit = null }
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
                text = "Serie in \"Currently Watching\" verschieben?",
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
                Text("Anfangen zu schauen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abbrechen", color = AppTheme.TextSecondary, fontSize = 15.sp)
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
                text = "Als gesehen markieren",
                fontSize = 13.sp,
                color = AppTheme.TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = AppTheme.TextTertiary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GESCHAUT AM",
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
                    else "Datum auswählen",
                    fontSize = 16.sp,
                    color = if (selectedDateDisplay.isNotBlank()) AppTheme.TextPrimary
                    else AppTheme.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = { showDatePickerDialog = true }) {
                    Text(
                        text = if (selectedDateDisplay.isBlank()) "Auswählen" else "Ändern",
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
                Text("Als gesehen markieren", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                ) { Text("Bestätigen", color = AppTheme.YellowAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Abbrechen", color = AppTheme.TextSecondary)
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