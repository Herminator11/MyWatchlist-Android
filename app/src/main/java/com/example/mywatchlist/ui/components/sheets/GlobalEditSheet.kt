package com.example.mywatchlist.ui.components.sheets
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
fun GlobalEditSheet(
    movie: MovieEntity,
    libraryViewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTv = movie.mediaType == "tv"

    val relevantListTypes = WatchListType.values().filter { listType ->
        if (isTv) {
            listType != WatchListType.RECENTLY_WATCHED_MOVIES &&
                    listType != WatchListType.FAVORITE_MOVIES
        } else {
            listType != WatchListType.CURRENTLY_WATCHING &&
                    listType != WatchListType.RECENTLY_WATCHED_TV &&
                    listType != WatchListType.FAVORITE_SERIES
        }
        // WANT_TO_WATCH ist für alle Medientypen immer sichtbar (kein Ausschluss nötig)
    }.toSet()

    var selectedLists by remember {
        mutableStateOf(libraryViewModel.getListsForEntity(movie).filter { it in relevantListTypes }.toSet())
    }
    var seasonNumber by remember { mutableStateOf(movie.seasonNumber) }
    var notes by remember { mutableStateOf(movie.notes ?: "") }

    // Kategorie: vorhandenen Wert vorbelegen
    val seriesCategories = listOf("Realserie", "Animated", "Anime")
    var selectedCategory by remember { mutableStateOf(movie.favoriteCategory) }

    val showSeasonPicker = isTv && selectedLists.any {
        it == WatchListType.CURRENTLY_WATCHING ||
                it == WatchListType.RECENTLY_WATCHED_TV ||
                it == WatchListType.WANT_TO_WATCH
    }
    val showCategoryPicker = isTv && selectedLists.contains(WatchListType.FAVORITE_SERIES)

    val isRecentlyWatched = movie.listType == WatchListType.RECENTLY_WATCHED_TV.name ||
            movie.listType == WatchListType.RECENTLY_WATCHED_MOVIES.name

    val parsedDate = remember(movie.finishedDate) {
        movie.finishedDate
            ?.replace("finished: ", "")
            ?.replace("watched: ", "")
            ?.trim()
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateDisplay by remember { mutableStateOf(parsedDate ?: "") }
    var selectedDateMillis by remember {
        mutableStateOf(
            parsedDate?.let {
                runCatching {
                    java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        .parse(it)?.time
                }.getOrNull()
            }
        )
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            // ── HEADER: Poster + Titel + Herz ───────────────
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
                        text = movie.title,
                        fontFamily = AppTheme.Display,
                        fontWeight = FontWeight.Normal,
                        fontSize = 21.sp,
                        color = AppTheme.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isTv) "SERIES" else "FILM",
                        fontFamily = AppTheme.Mono,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = AppTheme.TextSecondary
                    )
                }

            }

            SheetDivider()

            // ── LISTEN-ZUWEISUNG ─────────────────────────────
            SheetSectionLabel("Listen")
            relevantListTypes.forEach { listType ->
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
                        color = AppTheme.TextPrimary
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

            // ── STAFFELPICKER ────────────────────────────────
            if (showSeasonPicker) {
                SheetDivider()
                SheetSectionLabel("Staffel")
                SeasonPicker(
                    seasonNumber = seasonNumber,
                    onSeasonChange = { seasonNumber = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // ── DATUM (nur bei Recently Watched) ────────────
            if (isRecentlyWatched) {
                SheetDivider()
                SheetSectionLabel(if (movie.mediaType == "tv") "Beendet am" else "Geschaut am")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (selectedDateDisplay.isNotBlank()) selectedDateDisplay
                        else "Kein Datum gesetzt",
                        fontSize = 16.sp,
                        color = if (selectedDateDisplay.isNotBlank()) AppTheme.TextPrimary
                        else AppTheme.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Ändern", color = AppTheme.YellowAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            SheetDivider()

            // ── NOTIZEN ──────────────────────────────────────
            SheetSectionLabel("Notizen")
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text("Notiz hinzufügen...", color = AppTheme.TextTertiary, fontSize = 14.sp)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.YellowAccent,
                    unfocusedBorderColor = AppTheme.TextTertiary,
                    focusedContainerColor = AppTheme.DarkSurface2,
                    unfocusedContainerColor = AppTheme.DarkSurface2,
                    cursorColor = AppTheme.YellowAccent,
                    focusedTextColor = AppTheme.TextPrimary,
                    unfocusedTextColor = AppTheme.TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── SPEICHERN-BUTTON ─────────────────────────────
            Button(
                onClick = {
                    val finalSeason = if (showSeasonPicker) seasonNumber else ""
                    val prefix = if (movie.mediaType == "tv") "finished: " else "watched: "
                    val finalFinishedDate = if (isRecentlyWatched && selectedDateDisplay.isNotBlank())
                        "$prefix$selectedDateDisplay" else movie.finishedDate
                    val finalCategory = if (showCategoryPicker) selectedCategory
                    else movie.favoriteCategory

                    if (selectedLists.isEmpty()) {
                        libraryViewModel.deleteSpecificEntry(movie)
                    } else {
                        libraryViewModel.editCard(
                            movie = movie,
                            newLists = selectedLists,
                            seasonNumber = finalSeason,
                            finishedDate = finalFinishedDate,
                            notes = notes.ifBlank { null },
                            isFavorite = movie.isFavorite,
                            favoriteCategory = finalCategory
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.YellowAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    // ── DATE PICKER DIALOG ───────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
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
                        showDatePicker = false
                    }
                ) { Text("Bestätigen", color = AppTheme.YellowAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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

// ─────────────────────────────────────────────
// HILFSFUNKTIONEN FÜR SHEETS
// ─────────────────────────────────────────────
@Composable
fun SheetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        color = AppTheme.TextTertiary
    )
}

@Composable
fun SheetSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        fontFamily = AppTheme.Mono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.YellowAccent,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
    )
}