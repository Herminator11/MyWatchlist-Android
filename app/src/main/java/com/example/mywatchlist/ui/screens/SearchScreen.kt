package com.example.mywatchlist.ui.screens
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import com.example.mywatchlist.ui.components.sheets.AddToListSheet
import com.example.mywatchlist.ui.components.sheets.MediaDetailDialog
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.cinemaBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = viewModel(),
    libraryViewModel: LibraryViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var selectedMovieForAdd by remember { mutableStateOf<TmdbMovie?>(null) }
    var movieForDetail by remember { mutableStateOf<TmdbMovie?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val blurRadius by animateDpAsState(
        targetValue = if (movieForDetail != null) 18.dp else 0.dp,
        label = "detail_blur"
    )

    // Quick-Add per Swipe: Filme sofort in "Want to Watch" (mit Undo).
    // Serien öffnen das Sheet, weil eine Staffel gewählt werden muss.
    val quickAdd: (TmdbMovie) -> Unit = { movie ->
        if ((movie.mediaType ?: "movie") == "tv") {
            selectedMovieForAdd = movie
        } else {
            libraryViewModel.addNewCard(movie, WatchListType.WANT_TO_WATCH)
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "„${movie.displayTitle}“ → Want to Watch",
                    actionLabel = "Rückgängig",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    libraryViewModel.deleteSpecificEntry(
                        MovieEntity(
                            id = movie.id,
                            title = movie.displayTitle,
                            date = movie.displayDate,
                            posterPath = movie.posterPath,
                            mediaType = movie.mediaType ?: "movie",
                            listType = WatchListType.WANT_TO_WATCH.name,
                            seasonNumber = ""
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(300)
            searchViewModel.performSearch(searchQuery)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        ) {

            // HEADER
            SearchHeader()

            // SUCHLEISTE
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                focusRequester = focusRequester
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CONTENT
            AnimatedContent(
                targetState = when {
                    searchViewModel.isLoading -> SearchState.LOADING
                    searchViewModel.errorMessage != null -> SearchState.ERROR
                    searchViewModel.searchResults.isEmpty() && searchQuery.isNotBlank() -> SearchState.EMPTY
                    searchQuery.isBlank() -> SearchState.IDLE
                    else -> SearchState.RESULTS
                },
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "search_content"
            ) { state ->
                when (state) {
                    SearchState.LOADING -> LoadingState()
                    SearchState.ERROR   -> ErrorState(searchViewModel.errorMessage)
                    SearchState.EMPTY   -> EmptyState(searchQuery)
                    SearchState.IDLE    -> IdleState()
                    SearchState.RESULTS -> ResultsList(
                        results = searchViewModel.searchResults,
                        libraryViewModel = libraryViewModel,
                        onAddClick = { selectedMovieForAdd = it },
                        onCardClick = { movieForDetail = it },
                        onQuickAdd = quickAdd,
                        isLoadingMore = searchViewModel.isLoadingMore,
                        canLoadMore = searchViewModel.canLoadMore,
                        onLoadMore = { searchViewModel.loadNextPage() }
                    )
                }
            }
        }

        // Undo-Snackbar (über der Floating-Navigation)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 130.dp, start = 12.dp, end = 12.dp)
        )
    }

    // SHEET – 2-Step für neue Karten
    selectedMovieForAdd?.let { movie ->
        AddToListSheet(
            movie = movie,
            libraryViewModel = libraryViewModel,
            onDismiss = { selectedMovieForAdd = null }
        )
    }

    // DETAIL-POPUP – Tap auf eine Trefferkarte
    movieForDetail?.let { movie ->
        val transientEntity = MovieEntity(
            id = movie.id,
            title = movie.displayTitle,
            date = movie.releaseDate ?: movie.firstAirDate ?: "",
            posterPath = movie.posterPath,
            mediaType = movie.mediaType ?: "movie",
            listType = ""
        )
        MediaDetailDialog(
            movie = transientEntity,
            lists = libraryViewModel.getListsForMovie(movie),
            onDismiss = { movieForDetail = null },
            onAdd = {
                movieForDetail = null
                selectedMovieForAdd = movie
            }
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

// ── States ─────────────────────────────────────────────────────
private enum class SearchState { IDLE, LOADING, ERROR, EMPTY, RESULTS }

// ── Header ─────────────────────────────────────────────────────
@Composable
private fun SearchHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppTheme.YellowAccent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Suche",
            fontFamily = AppTheme.Display,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
            color = AppTheme.TextPrimary,
            letterSpacing = (-0.3).sp
        )
    }
}

// ── Suchleiste ─────────────────────────────────────────────────
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .focusRequester(focusRequester),
        placeholder = { Text("Filme, Serien, Anime...", color = AppTheme.TextSecondary, fontSize = 15.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Löschen", tint = AppTheme.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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
}

// ── Ergebnisliste ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsList(
    results: List<TmdbMovie>,
    libraryViewModel: LibraryViewModel,
    onAddClick: (TmdbMovie) -> Unit,
    onCardClick: (TmdbMovie) -> Unit,
    onQuickAdd: (TmdbMovie) -> Unit,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    // Infinite-Scroll: nachladen, wenn die letzten ~3 Items sichtbar werden
    LaunchedEffect(listState, canLoadMore, isLoadingMore) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last to info.totalItemsCount
        }.collect { (last, total) ->
            if (canLoadMore && !isLoadingMore && total > 0 && last >= total - 3) {
                onLoadMore()
            }
        }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
    ) {
        item {
            Text(
                text = "${results.size} Ergebnisse · zum Merken nach rechts wischen",
                color = AppTheme.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(results, key = { it.id.toString() + (it.mediaType ?: "") }) { movie ->
            val alreadySaved = libraryViewModel.isFavorite(movie.id)
            SwipeToAdd(onAdd = { onQuickAdd(movie) }) {
                SearchResultCard(
                    movie = movie,
                    alreadySaved = alreadySaved,
                    onAddClick = { onAddClick(movie) },
                    onClick = { onCardClick(movie) }
                )
            }
        }
        // Fußzeilen-Spinner beim Nachladen
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppTheme.YellowAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

// Swipe-rechts auf einer Trefferkarte → Quick-Add ("Want to Watch").
// Schnappt zurück (kein echtes Dismiss); löst nur die Aktion aus.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToAdd(onAdd: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) onAdd()
            false
        },
        positionalThreshold = { it * 0.4f }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = { AddSwipeBackground(state) },
        content = { content() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSwipeBackground(state: SwipeToDismissBoxState) {
    val active = state.targetValue == SwipeToDismissBoxValue.StartToEnd
    val bg by animateColorAsState(
        targetValue = if (active) AppTheme.YellowAccent else AppTheme.DarkSurface2,
        label = "add_swipe_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(start = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = if (active) Color.Black else AppTheme.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "Want to Watch",
            fontFamily = AppTheme.Mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (active) Color.Black else AppTheme.TextSecondary
        )
    }
}

// ── Suchkarte ──────────────────────────────────────────────────
@Composable
private fun SearchResultCard(
    movie: TmdbMovie,
    alreadySaved: Boolean,
    onAddClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = movie.fullPosterPath,
                contentDescription = movie.displayTitle,
                modifier = Modifier
                    .width(75.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = movie.displayTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (movie.displayDate.isNotBlank()) {
                        Text(text = movie.displayDate.take(4), fontSize = 12.sp, color = AppTheme.TextSecondary)
                        Text("\u00B7", color = AppTheme.TextTertiary, fontSize = 12.sp)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = AppTheme.DarkSurface2) {
                        Text(
                            text = movie.displayMediaType,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.TextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (alreadySaved) AppTheme.DarkSurface2 else AppTheme.YellowAccent)
                    .let { if (!alreadySaved) it.clickable { onAddClick() } else it },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (alreadySaved) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (alreadySaved) "Bereits gespeichert" else "Hinzufügen",
                    tint = if (alreadySaved) AppTheme.TextSecondary else Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Leerzustände ───────────────────────────────────────────────
@Composable
private fun IdleState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Entdecken", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.TextSecondary, letterSpacing = 0.8.sp)
        listOf(
            "\uD83C\uDFAC  Filme & Serien aus aller Welt",
            "\uD83D\uDCFA  Anime und Animationsserien",
            "\u2B50  Zu Listen hinzufügen & verwalten"
        ).forEach { hint ->
            Text(text = hint, fontSize = 15.sp, color = AppTheme.TextSecondary, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppTheme.YellowAccent, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Suche...", color = AppTheme.TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorState(message: String?) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\u26A0\uFE0F", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message ?: "Ein Fehler ist aufgetreten.", color = AppTheme.TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\uD83D\uDD0D", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Kein Ergebnis für", color = AppTheme.TextSecondary, fontSize = 14.sp)
            Text(text = "\u201E$query\u201C", color = AppTheme.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}