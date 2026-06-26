package com.example.mywatchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.mywatchlist.components.cards.SwipeableMediaCard
import com.example.mywatchlist.components.cards.UniversalMediaCard
import com.example.mywatchlist.components.common.ScreenHeader
import com.example.mywatchlist.components.sheets.GlobalEditSheet
import com.example.mywatchlist.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentlyWatchingScreen(
    modifier: Modifier = Modifier,
    libraryViewModel: LibraryViewModel
) {
    val allSavedMovies by libraryViewModel.allMovies.collectAsState()
    val currentlyWatchingList = allSavedMovies.filter { it.listType == "CURRENTLY_WATCHING" }
    var selectedMovieForEdit by remember { mutableStateOf<MovieEntity?>(null) }
    var movieForDatePicker by remember { mutableStateOf<MovieEntity?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.DarkBg)
    ) {
        if (currentlyWatchingList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Deine Liste ist noch leer.\nSuche nach Titeln, um sie hinzuzufügen!",
                    color = AppTheme.TextSecondary,
                    textAlign = TextAlign.Center
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
                item { ScreenHeader(title = "Watching") }

                items(items = currentlyWatchingList, key = { "${it.id}_${it.listType}_${it.seasonNumber}" }) { movieEntity ->
                    SwipeableMediaCard(
                        item = movieEntity,
                        onSwipe = { selectedMovieForEdit = movieEntity }
                    ) {
                        UniversalMediaCard(
                            item = movieEntity,
                            // Done-Button öffnet DatePicker (Story 3.2)
                            onFinishClick = { movieForDatePicker = movieEntity }
                        )
                    }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishedDatePickerDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        onConfirm(sdf.format(java.util.Date(millis)))
                    }
                }
            ) {
                Text("Bestätigen", color = AppTheme.YellowAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = AppTheme.TextSecondary)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = AppTheme.DarkSurface
        )
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