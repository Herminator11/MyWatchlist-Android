package com.example.mywatchlist.ui.screens
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import com.example.mywatchlist.data.CardDensity
import com.example.mywatchlist.data.sortedFor
import com.example.mywatchlist.ui.components.cards.LocalListPrefs
import com.example.mywatchlist.ui.components.cards.PosterTile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mywatchlist.ui.theme.CinemaMotion
import com.example.mywatchlist.ui.theme.LocalReduceMotion
import com.example.mywatchlist.ui.components.common.EmptyState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LiveTv
import com.example.mywatchlist.ui.components.common.CinemaDatePickerDialog
import androidx.compose.ui.res.stringResource
import com.example.mywatchlist.R
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.mywatchlist.ui.components.cards.UniversalMediaCard
import com.example.mywatchlist.ui.components.common.ScreenHeader
import com.example.mywatchlist.ui.components.sheets.GlobalEditSheet
import com.example.mywatchlist.ui.components.sheets.MediaDetailDialog
import com.example.mywatchlist.ui.theme.AppTheme
import com.example.mywatchlist.ui.theme.cinemaBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentlyWatchingScreen(
    modifier: Modifier = Modifier,
    libraryViewModel: LibraryViewModel
) {
    val allSavedMovies by libraryViewModel.allMovies.collectAsStateWithLifecycle()
    // nur neu filtern, wenn sich die Liste ändert – nicht bei jeder Recomposition
    val listPrefs = LocalListPrefs.current
    val currentlyWatchingList = remember(allSavedMovies, listPrefs.watchingSort) {
        allSavedMovies.filter { it.listType == "CURRENTLY_WATCHING" }.sortedFor(listPrefs.watchingSort)
    }
    var selectedMovieForEdit by remember { mutableStateOf<MovieEntity?>(null) }
    var movieForDatePicker by remember { mutableStateOf<MovieEntity?>(null) }
    var movieForDetail by remember { mutableStateOf<MovieEntity?>(null) }
    val blurRadius by animateDpAsState(
        targetValue = if (movieForDetail != null) 18.dp else 0.dp,
        animationSpec = CinemaMotion.effects(LocalReduceMotion.current),
        label = "detail_blur"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .cinemaBackground()
            .blur(blurRadius)
    ) {
        if (currentlyWatchingList.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.LiveTv,
                message = stringResource(R.string.empty_watching),
                hint = stringResource(R.string.empty_hint_search)
            )
        } else if (listPrefs.density == CardDensity.POSTER) {
            // Poster-Raster (Einstellung „Kartendichte“)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ScreenHeader(title = stringResource(R.string.list_currently_watching))
                }
                gridItems(currentlyWatchingList, key = { "${it.id}_${it.listType}_${it.seasonNumber}" }) { movieEntity ->
                    PosterTile(
                        item = movieEntity,
                        modifier = Modifier.animateItem(),
                        onFinishClick = { movieForDatePicker = movieEntity },
                        onClick = { movieForDetail = movieEntity },
                        onLongClick = { selectedMovieForEdit = movieEntity }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
            ) {
                item { ScreenHeader(title = stringResource(R.string.list_currently_watching)) }

                items(items = currentlyWatchingList, key = { "${it.id}_${it.listType}_${it.seasonNumber}" }) { movieEntity ->
                    UniversalMediaCard(
                        item = movieEntity,
                        modifier = Modifier.animateItem(),
                        // Done-Button öffnet DatePicker (Story 3.2)
                        onFinishClick = { movieForDatePicker = movieEntity },
                        onClick = { movieForDetail = movieEntity },
                        onLongClick = { selectedMovieForEdit = movieEntity }
                    )
                }
            }
        }
    }

    // Globales Bearbeiten-Sheet
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

    // Date Picker (Story 3.2)
    movieForDatePicker?.let { movie ->
        FinishedDatePickerDialog(
            onConfirm = { dateString ->
                libraryViewModel.markAsFinished(movie, dateString)
                movieForDatePicker = null
            },
            onDismiss = { movieForDatePicker = null }
        )
    }
}

// ─────────────────────────────────────────────
// DATE PICKER DIALOG (Story 3.2)
// ─────────────────────────────────────────────
@Composable
private fun FinishedDatePickerDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    CinemaDatePickerDialog(
        onConfirm = { _, display -> onConfirm(display) },
        onDismiss = onDismiss
    )
}
