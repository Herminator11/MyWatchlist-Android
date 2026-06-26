package com.example.mywatchlist.ui.components.sheets
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mywatchlist.ui.components.common.SeasonPicker
import com.example.mywatchlist.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToListSheet(
    movie: TmdbMovie,
    libraryViewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTv = (movie.mediaType ?: "movie") == "tv"

    // Listen-Auswahl (Mehrfachauswahl wie GlobalEditSheet)
    var selectedLists by remember { mutableStateOf<Set<WatchListType>>(emptySet()) }

    // Staffel (nur bei TV + relevante Liste)
    var seasonNumber by remember { mutableStateOf("") }

    // Datum (nur bei Recently Watched)
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedDateDisplay by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    // Kategorie (nur bei TV + FAVORITE_SERIES)
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val seriesCategories = listOf("Realserie", "Animated", "Anime")

    // Sichtbarkeits-Flags – reagieren reaktiv auf Checkbox-Änderungen
    val showSeasonPicker = isTv && selectedLists.any {
        it == WatchListType.CURRENTLY_WATCHING ||
                it == WatchListType.RECENTLY_WATCHED_TV ||
                it == WatchListType.WANT_TO_WATCH
    }
    val showDatePicker = selectedLists.any {
        it == WatchListType.RECENTLY_WATCHED_TV || it == WatchListType.RECENTLY_WATCHED_MOVIES
    }
    val showCategoryPicker = isTv && selectedLists.contains(WatchListType.FAVORITE_SERIES)

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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            // ── HEADER ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (movie.posterPath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w500${movie.posterPath}",
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp, 68.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = movie.displayTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = AppTheme.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isTv) "TV Series" else "Movie",
                        fontSize = 13.sp,
                        color = AppTheme.TextSecondary
                    )
                }
            }

            SheetDivider()

            // ── LISTEN-ZUWEISUNG (Checkboxen) ───────────────
            SheetSectionLabel("Hinzufügen zu")
            val relevantLists = WatchListType.values().filter { listType ->
                if (isTv) {
                    listType != WatchListType.RECENTLY_WATCHED_MOVIES &&
                            listType != WatchListType.FAVORITE_MOVIES
                } else {
                    listType != WatchListType.CURRENTLY_WATCHING &&
                            listType != WatchListType.RECENTLY_WATCHED_TV &&
                            listType != WatchListType.FAVORITE_SERIES
                }
                // WANT_TO_WATCH ist für alle Medientypen immer sichtbar (kein Ausschluss nötig)
            }
            relevantLists.forEach { listType ->
                val isChecked = selectedLists.contains(listType)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedLists = if (isChecked)
                                selectedLists - listType
                            else
                                selectedLists + listType
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            selectedLists = if (checked)
                                selectedLists + listType
                            else
                                selectedLists - listType
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AppTheme.YellowAccent,
                            checkmarkColor = Color.Black,
                            uncheckedColor = AppTheme.TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = listType.displayName,
                        fontSize = 15.sp,
                        color = AppTheme.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── KATEGORIE (TV + FAVORITE_SERIES) ────────────
            if (showCategoryPicker) {
                SheetDivider()
                SheetSectionLabel("Kategorie")
                seriesCategories.forEach { category ->
                    val isChecked = selectedCategory == category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { if (it) selectedCategory = category },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppTheme.YellowAccent,
                                checkmarkColor = Color.Black,
                                uncheckedColor = AppTheme.TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = category,
                                fontSize = 15.sp,
                                color = AppTheme.TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            val desc = when (category) {
                                "Realserie" -> "Live-Action Serien"
                                "Animated"  -> "Westliche Animationsserien"
                                "Anime"     -> "Japanische Animationsserien"
                                else        -> ""
                            }
                            if (desc.isNotBlank()) {
                                Text(text = desc, fontSize = 12.sp, color = AppTheme.TextSecondary)
                            }
                        }
                    }
                }
            }

            // ── STAFFEL (TV + Currently/Recently Watched TV) ─
            if (showSeasonPicker) {
                SheetDivider()
                SheetSectionLabel("Staffel")
                SeasonPicker(
                    seasonNumber = seasonNumber,
                    onSeasonChange = { seasonNumber = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // ── DATUM (Recently Watched) ─────────────────────
            if (showDatePicker) {
                SheetDivider()
                val dateLabel = if (selectedLists.contains(WatchListType.RECENTLY_WATCHED_TV))
                    "Beendet am" else "Geschaut am"
                SheetSectionLabel(dateLabel)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── HINZUFÜGEN-BUTTON ────────────────────────────
            Button(
                onClick = {
                    val finalSeason = if (showSeasonPicker) seasonNumber else ""
                    val prefix = if (selectedLists.contains(WatchListType.RECENTLY_WATCHED_TV))
                        "finished: " else "watched: "
                    val finalDate = if (showDatePicker && selectedDateDisplay.isNotBlank())
                        "$prefix$selectedDateDisplay" else null

                    selectedLists.forEach { listType ->
                        libraryViewModel.addNewCard(
                            movie = movie,
                            listType = listType,
                            seasonNumber = finalSeason,
                            finishedDate = finalDate,
                            favoriteCategory = if (listType == WatchListType.FAVORITE_SERIES)
                                selectedCategory else null
                        )
                    }
                    onDismiss()
                },
                enabled = selectedLists.isNotEmpty() &&
                        (!showCategoryPicker || selectedCategory != null),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.YellowAccent,
                    contentColor = Color.Black,
                    disabledContainerColor = AppTheme.DarkSurface2,
                    disabledContentColor = AppTheme.TextSecondary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Hinzufügen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    // ── DATE PICKER DIALOG ───────────────────────────────────
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