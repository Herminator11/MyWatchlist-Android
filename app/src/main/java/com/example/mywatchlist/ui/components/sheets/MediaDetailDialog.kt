package com.example.mywatchlist.ui.components.sheets
import androidx.compose.ui.res.stringResource
import com.example.mywatchlist.R
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.ui.viewmodel.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mywatchlist.ui.theme.CinemaTheme
import com.example.mywatchlist.ui.theme.DisplaySerif
import com.example.mywatchlist.ui.theme.SpecMono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ─────────────────────────────────────────────
// Ladezustand der TMDB-Detaildaten
// ─────────────────────────────────────────────
private data class CastLine(val name: String, val character: String?)

private fun Credits?.toCastLines(): List<CastLine> =
    this?.cast.orEmpty()
        .filter { !it.name.isNullOrBlank() }
        .sortedBy { it.order ?: Int.MAX_VALUE }
        .take(8)
        .map { CastLine(it.name!!, it.character?.takeIf { c -> c.isNotBlank() }) }

private sealed interface DetailState {
    data object Loading : DetailState
    data class Tv(
        val seasons: Int?,
        val episodes: Int?,
        val genres: List<String>,
        val originalTitle: String?,
        // (Staffelnummer, Folgenzahl), ohne Specials (Staffel 0)
        val seasonList: List<Pair<Int, Int>>,
        val creators: List<String>,
        val cast: List<CastLine>
    ) : DetailState
    data class Movie(
        val runtimeMinutes: Int?,
        val genres: List<String>,
        val originalTitle: String?,
        val directors: List<String>,
        val cast: List<CastLine>
    ) : DetailState
    data object Error : DetailState
}

// ═════════════════════════════════════════════════════════════
// DETAIL-POPUP  ·  "Vintage-Kinoplakat / Datenblatt"
//
// Aesthetik (siehe CLAUDE.md):
//  · Typo:   DM Serif Display (Titel) + Space Mono (Daten)
//  · Farbe:  tiefes Noir-Charcoal mit warmem Goldakzent
//  · Tiefe:  geschichtete Gradienten + goldener Lichtschein
//  · Motion: orchestrierter Auftritt mit gestaffeltem Reveal
// ═════════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaDetailDialog(
    movie: MovieEntity,
    lists: Set<WatchListType>,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null
) {
    val isTv = movie.mediaType == "tv"

    // TMDB-Detaildaten erst beim Öffnen nachladen.
    val detailState by produceState<DetailState>(
        initialValue = DetailState.Loading,
        key1 = movie.id,
        key2 = movie.mediaType
    ) {
        value = try {
            if (isTv) {
                val d = MovieApi.service.getTvDetails(movie.id)
                DetailState.Tv(
                    seasons = d.numberOfSeasons,
                    episodes = d.numberOfEpisodes,
                    genres = d.genres.orEmpty().map { it.name },
                    originalTitle = d.originalName,
                    seasonList = d.seasons.orEmpty()
                        .filter { it.seasonNumber > 0 }
                        .sortedBy { it.seasonNumber }
                        .map { it.seasonNumber to (it.episodeCount ?: 0) },
                    creators = d.createdBy.orEmpty().mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } },
                    cast = d.credits.toCastLines()
                )
            } else {
                val d = MovieApi.service.getMovieDetails(movie.id)
                DetailState.Movie(
                    runtimeMinutes = d.runtime,
                    genres = d.genres.orEmpty().map { it.name },
                    originalTitle = d.originalTitle,
                    directors = d.credits?.crew.orEmpty()
                        .filter { it.job == "Director" }
                        .mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } },
                    cast = d.credits.toCastLines()
                )
            }
        } catch (e: Exception) {
            DetailState.Error
        }
    }

    val genres = when (val d = detailState) {
        is DetailState.Tv -> d.genres
        is DetailState.Movie -> d.genres
        else -> emptyList()
    }
    val cast = when (val d = detailState) {
        is DetailState.Tv -> d.cast
        is DetailState.Movie -> d.cast
        else -> emptyList()
    }
    val crew = when (val d = detailState) {
        is DetailState.Tv -> d.creators
        is DetailState.Movie -> d.directors
        else -> emptyList()
    }
    // Originaltitel nur zeigen, wenn er sich vom angezeigten Titel unterscheidet.
    val originalTitle = when (val d = detailState) {
        is DetailState.Tv -> d.originalTitle
        is DetailState.Movie -> d.originalTitle
        else -> null
    }?.takeIf { it.isNotBlank() && !it.equals(movie.title, ignoreCase = true) }

    // ── Orchestrierter Auftritt ──────────────────────────────
    var mounted by remember { mutableStateOf(false) }
    var revealed by remember { mutableIntStateOf(0) }
    LaunchedEffectReveal { mounted = true; revealed = it }

    // Interaktives Holo-Poster (Tap aufs Poster)
    var showInteractivePoster by remember { mutableStateOf(false) }

    val containerScale by animateFloatAsState(
        targetValue = if (mounted) 1f else 0.90f,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "container_scale"
    )
    val containerAlpha by animateFloatAsState(
        targetValue = if (mounted) 1f else 0f,
        animationSpec = tween(240),
        label = "container_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp)
                .graphicsLayer {
                    scaleX = containerScale
                    scaleY = containerScale
                    alpha = containerAlpha
                },
            shape = RoundedCornerShape(22.dp),
            color = CinemaTheme.Ink
        ) {
            Box(
                modifier = Modifier
                    // Tiefe: vertikaler Charcoal→Ink Verlauf …
                    .background(
                        Brush.verticalGradient(
                            listOf(CinemaTheme.Charcoal, CinemaTheme.Ink)
                        )
                    )
                    // … plus warmer Lichtschein oben links wie ein Spot.
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CinemaTheme.Gold.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            radius = 520f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    // Filmstreifen-Perforation als Kopfmotiv
                    SprocketStrip()

                    Column(modifier = Modifier.padding(22.dp)) {
                        // ── HEADER: Poster + Titel + Schließen ──
                        RevealItem(index = 1, revealed = revealed) {
                            Row(verticalAlignment = Alignment.Top) {
                                PosterFrame(
                                    posterPath = movie.posterPath,
                                    title = movie.title,
                                    onClick = { showInteractivePoster = true }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            TicketTag(text = if (isTv) stringResource(R.string.tag_series) else stringResource(R.string.tag_feature))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = movie.title,
                                                fontFamily = DisplaySerif,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 27.sp,
                                                lineHeight = 30.sp,
                                                color = CinemaTheme.Parchment,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (originalTitle != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = originalTitle,
                                                    fontFamily = SpecMono,
                                                    fontStyle = FontStyle.Italic,
                                                    fontSize = 12.sp,
                                                    color = CinemaTheme.ParchmentDim,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        CloseButton(onDismiss)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        RevealItem(index = 2, revealed = revealed) { GoldRule() }
                        Spacer(modifier = Modifier.height(16.dp))

                        // ── LISTEN ──────────────────────────────
                        RevealItem(index = 3, revealed = revealed) {
                            Column {
                                FieldLabel(stringResource(R.string.detail_filed_under))
                                Spacer(modifier = Modifier.height(10.dp))
                                if (lists.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.detail_not_on_list),
                                        fontFamily = SpecMono,
                                        fontSize = 13.sp,
                                        color = CinemaTheme.ParchmentDim
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        lists.forEach { ListChip(stringResource(it.labelRes)) }
                                    }
                                }

                                // Persönliches "Fertig geschaut"-Datum (nur aus
                                // Recently Watched). Bewusst HIER bei den
                                // Bibliotheks-Infos statt unter stringResource(R.string.spec_released), damit
                                // klar ist: das bist DU, keine Produktionsangabe.
                                // Gespeichert als "finished: …"/"watched: …" (dd.MM.yyyy).
                                val finishedDisplay = movie.finishedDate
                                    ?.replace("finished: ", "")
                                    ?.replace("watched: ", "")
                                    ?.trim()
                                    ?.takeIf { it.isNotBlank() }
                                if (finishedDisplay != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    FieldLabel(if (isTv) stringResource(R.string.detail_finished_watching) else stringResource(R.string.detail_watched_on))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = finishedDisplay,
                                        fontFamily = SpecMono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CinemaTheme.Parchment
                                    )
                                }
                            }
                        }

                        // ── GENRES ──────────────────────────────
                        if (detailState is DetailState.Loading || genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            RevealItem(index = 4, revealed = revealed) {
                                Column {
                                    FieldLabel(stringResource(R.string.detail_genres))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (detailState is DetailState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(15.dp),
                                            strokeWidth = 2.dp,
                                            color = CinemaTheme.Gold
                                        )
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            genres.forEach { ListChip(it) }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        RevealItem(index = 5, revealed = revealed) { GoldRule() }
                        Spacer(modifier = Modifier.height(6.dp))

                        // ── DATENBLATT (Spec sheet) ─────────────
                        RevealItem(index = 6, revealed = revealed) {
                            Column {
                                if (isTv) {
                                    val tv = detailState as? DetailState.Tv
                                    SpecRow(stringResource(R.string.spec_seasons), tv?.seasons?.toString(),
                                        detailState is DetailState.Loading)
                                    SpecRow(stringResource(R.string.spec_episodes), tv?.episodes?.toString(),
                                        detailState is DetailState.Loading)
                                    // Folgen pro Staffel
                                    tv?.seasonList?.forEach { (number, count) ->
                                        SeasonRow(
                                            label = stringResource(R.string.detail_season_row, number),
                                            value = stringResource(R.string.detail_episodes_count, count)
                                        )
                                    }
                                } else {
                                    val mv = detailState as? DetailState.Movie
                                    SpecRow(stringResource(R.string.spec_runtime),
                                        mv?.runtimeMinutes?.let { stringResource(R.string.spec_runtime_value, it) },
                                        detailState is DetailState.Loading)
                                }
                                SpecRow(stringResource(R.string.spec_released), formatReleaseDate(movie.date), false)
                            }
                        }

                        // ── CAST & CREW ─────────────────────────
                        if (crew.isNotEmpty() || cast.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            RevealItem(index = 7, revealed = revealed) {
                                Column {
                                    GoldRule()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (crew.isNotEmpty()) {
                                        FieldLabel(
                                            stringResource(
                                                if (isTv) R.string.detail_created_by else R.string.detail_directed_by
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = crew.joinToString(", "),
                                            fontFamily = SpecMono,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CinemaTheme.Parchment
                                        )
                                    }
                                    if (cast.isNotEmpty()) {
                                        if (crew.isNotEmpty()) Spacer(modifier = Modifier.height(16.dp))
                                        FieldLabel(stringResource(R.string.detail_cast))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        cast.forEach { line ->
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text(
                                                    text = line.name,
                                                    fontFamily = SpecMono,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = CinemaTheme.Parchment
                                                )
                                                if (line.character != null) {
                                                    Text(
                                                        text = line.character,
                                                        fontFamily = SpecMono,
                                                        fontStyle = FontStyle.Italic,
                                                        fontSize = 12.sp,
                                                        color = CinemaTheme.ParchmentDim
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── PRIMÄRAKTION: Hinzufügen ODER Bearbeiten ──
                        if (onAdd != null || onEdit != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            RevealItem(index = 8, revealed = revealed) {
                                if (onAdd != null) {
                                    AddAction(onClick = onAdd)
                                } else if (onEdit != null) {
                                    EditAction(onClick = onEdit)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── INTERAKTIVES HOLO-POSTER ─────────────────────────────
    if (showInteractivePoster && movie.posterPath != null) {
        InteractivePosterDialog(
            posterPath = movie.posterPath,
            title = movie.title,
            onDismiss = { showInteractivePoster = false }
        )
    }
}

// ═════════════════════════════════════════════════════════════
// INTERAKTIVES HOLO-POSTER
// Tap aufs Poster → große Karte, die man mit dem Finger in 3D
// kippen kann; ein Glanzlicht (Specular) wandert über die Karte.
// Loslassen → federt zurück in die Ebene.
// ═════════════════════════════════════════════════════════════
@Composable
private fun InteractivePosterDialog(
    posterPath: String,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val scope = rememberCoroutineScope()
        val densityF = LocalDensity.current.density
        val maxTilt = 18f
        val rotX = remember { Animatable(0f) }
        val rotY = remember { Animatable(0f) }

        // Sanfter Auftritt der Karte
        var mounted by remember { mutableStateOf(false) }
        LaunchedEffectReveal { mounted = it >= 0 }
        val appear by animateFloatAsState(
            targetValue = if (mounted) 1f else 0.85f,
            animationSpec = tween(260, easing = EaseOutCubic),
            label = "poster_appear"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .aspectRatio(2f / 3f)
                        .graphicsLayer {
                            rotationX = rotX.value
                            rotationY = rotY.value
                            scaleX = appear
                            scaleY = appear
                            cameraDistance = 14f * densityF
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(CinemaTheme.Smoke)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, drag ->
                                    change.consume()
                                    scope.launch {
                                        rotY.snapTo((rotY.value + drag.x * 0.12f).coerceIn(-maxTilt, maxTilt))
                                    }
                                    scope.launch {
                                        rotX.snapTo((rotX.value - drag.y * 0.12f).coerceIn(-maxTilt, maxTilt))
                                    }
                                },
                                onDragEnd = {
                                    val spec = spring<Float>(
                                        dampingRatio = 0.35f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                    scope.launch { rotX.animateTo(0f, spec) }
                                    scope.launch { rotY.animateTo(0f, spec) }
                                }
                            )
                        }
                ) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w500$posterPath",
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Glanzlicht + holografischer Schimmer, wandert mit der Neigung
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                val nx = rotY.value / maxTilt   // -1..1
                                val ny = rotX.value / maxTilt
                                val gx = (0.5f + nx * 0.5f) * size.width
                                val gy = (0.5f - ny * 0.5f) * size.height

                                // weiches Specular-Highlight
                                drawRect(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.30f),
                                            Color.White.copy(alpha = 0.0f)
                                        ),
                                        center = Offset(gx, gy),
                                        radius = size.width * 0.95f
                                    )
                                )
                                // diagonaler Gold-Schimmer (Holo)
                                drawRect(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            CinemaTheme.GoldBright.copy(alpha = 0.10f),
                                            Color.Transparent
                                        ),
                                        start = Offset(gx - size.width, gy - size.height),
                                        end = Offset(gx + size.width, gy + size.height)
                                    )
                                )
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.poster_hint),
                    fontFamily = SpecMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    color = CinemaTheme.ParchmentDim
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AUFTRITTS-CHOREOGRAFIE
// ─────────────────────────────────────────────────────────────
@Composable
private fun LaunchedEffectReveal(onTick: (Int) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onTick(0)                 // mount (Container fade/scale)
        delay(120)
        for (i in 1..8) {
            onTick(i)
            delay(75)
        }
    }
}

@Composable
private fun RevealItem(index: Int, revealed: Int, content: @Composable () -> Unit) {
    val visible = revealed >= index
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(380, easing = EaseOutCubic),
        label = "reveal_alpha_$index"
    )
    val lift = with(LocalDensity.current) { 16.dp.toPx() }
    val translation by animateFloatAsState(
        targetValue = if (visible) 0f else lift,
        animationSpec = tween(440, easing = EaseOutCubic),
        label = "reveal_y_$index"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = translation
        }
    ) { content() }
}

// ─────────────────────────────────────────────────────────────
// BAUSTEINE
// ─────────────────────────────────────────────────────────────

// Filmstreifen-Perforation am oberen Rand
@Composable
private fun SprocketStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CinemaTheme.Ink.copy(alpha = 0.6f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        repeat(14) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CinemaTheme.Ember.copy(alpha = 0.55f))
            )
        }
    }
}

@Composable
private fun PosterFrame(posterPath: String?, title: String, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(96.dp, 144.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CinemaTheme.Smoke)
            .then(
                if (posterPath != null && onClick != null)
                    Modifier.clickable { onClick() } else Modifier
            )
    ) {
        if (posterPath != null) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500$posterPath",
                contentDescription = title,
                modifier = Modifier
                    .size(96.dp, 144.dp)
                    .drawWithContent {
                        drawContent()
                        // Sanftes Vignette unten für Plakat-Tiefe
                        drawRect(
                            Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1f to CinemaTheme.Ink.copy(alpha = 0.55f)
                            )
                        )
                    },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.size(96.dp, 144.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Info, contentDescription = null, tint = CinemaTheme.ParchmentDim)
            }
        }

        // Affordanz: kleines "Vergrößern"-Badge, wenn antippbar
        if (posterPath != null && onClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(CinemaTheme.Ink.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = stringResource(R.string.poster_expand),
                    tint = CinemaTheme.GoldBright,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun TicketTag(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CinemaTheme.Gold.copy(alpha = 0.14f))
    ) {
        Text(
            text = text,
            fontFamily = SpecMono,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = CinemaTheme.GoldBright,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.action_close),
            tint = CinemaTheme.ParchmentDim,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Primäre Aktion im Popup: führt zum Bearbeiten-Sheet.
// Ersetzt die alte Swipe-zum-Bearbeiten-Geste (Gesten-Konflikt mit
// dem Pager). Gold-umrandeter "Ticket"-Button im Mono-Stil.
@Composable
private fun EditAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, CinemaTheme.Gold.copy(alpha = 0.55f)),
                RoundedCornerShape(12.dp)
            )
            .background(CinemaTheme.Gold.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = CinemaTheme.GoldBright,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.action_edit),
            fontFamily = SpecMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.5.sp,
            color = CinemaTheme.GoldBright
        )
    }
}

// Primäre Aktion auf dem Such-Screen: gefüllter Gold-Button (CTA),
// öffnet das Liste-wählen-Sheet. Stärker betont als EditAction.
@Composable
private fun AddAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CinemaTheme.Gold)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = CinemaTheme.Ink,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.action_add_to_list),
            fontFamily = SpecMono,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = CinemaTheme.Ink
        )
    }
}

@Composable
private fun GoldRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(CinemaTheme.Hairline, CinemaTheme.Gold.copy(alpha = 0.5f), CinemaTheme.Hairline)
                )
            )
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = SpecMono,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 2.5.sp,
        color = CinemaTheme.Gold
    )
}

@Composable
private fun ListChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = CinemaTheme.Gold.copy(alpha = 0.10f),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
    ) {
        Text(
            text = text,
            fontFamily = SpecMono,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = CinemaTheme.GoldBright,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// Datenblatt-Zeile: Label · gepunkteter Leader · Wert (mono)
@Composable
private fun SpecRow(label: String, value: String?, loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = SpecMono,
            fontSize = 13.sp,
            color = CinemaTheme.ParchmentDim
        )
        // Gepunkteter Leader füllt den Raum – Datenblatt-Optik
        Text(
            text = " " + "·".repeat(40) + " ",
            fontFamily = SpecMono,
            fontSize = 13.sp,
            color = CinemaTheme.Hairline,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f)
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = CinemaTheme.Gold
            )
        } else {
            Text(
                text = value ?: "—",
                fontFamily = SpecMono,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontStyle = if (value == null) FontStyle.Italic else FontStyle.Normal,
                color = if (value == null) CinemaTheme.ParchmentDim else CinemaTheme.GoldBright
            )
        }
    }
}

// Eingerückte, dezentere Zeile für eine einzelne Staffel
@Composable
private fun SeasonRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = SpecMono,
            fontSize = 12.sp,
            color = CinemaTheme.ParchmentDim
        )
        Text(
            text = " " + "·".repeat(40) + " ",
            fontFamily = SpecMono,
            fontSize = 12.sp,
            color = CinemaTheme.Hairline,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontFamily = SpecMono,
            fontSize = 13.sp,
            color = CinemaTheme.Parchment
        )
    }
}

// "2008-01-20" → "20.01.2008"; nur Jahr oder leer → Rohwert/null
private fun formatReleaseDate(raw: String): String? {
    if (raw.isBlank()) return null
    return runCatching {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        parser.parse(raw)?.let { formatter.format(it) }
    }.getOrNull() ?: raw.take(4).ifBlank { null }
}
