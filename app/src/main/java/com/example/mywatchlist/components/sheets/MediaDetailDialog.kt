package com.example.mywatchlist.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.mywatchlist.MovieApi
import com.example.mywatchlist.MovieEntity
import com.example.mywatchlist.WatchListType
import com.example.mywatchlist.ui.theme.AppTheme

// ─────────────────────────────────────────────
// Ladezustand der TMDB-Detaildaten
// ─────────────────────────────────────────────
private sealed interface DetailState {
    data object Loading : DetailState
    data class Tv(val seasons: Int?, val episodes: Int?) : DetailState
    data class Movie(val runtimeMinutes: Int?) : DetailState
    data object Error : DetailState
}

// ─────────────────────────────────────────────
// DETAIL-POPUP
// Erscheint zentriert über dem (unscharfen) Hintergrund.
// Zeigt: zugewiesene Listen, Staffel-/Folgenzahl (Serie),
// Release-Datum.
// ─────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaDetailDialog(
    movie: MovieEntity,
    lists: Set<WatchListType>,
    onDismiss: () -> Unit
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
                DetailState.Tv(d.numberOfSeasons, d.numberOfEpisodes)
            } else {
                val d = MovieApi.service.getMovieDetails(movie.id)
                DetailState.Movie(d.runtime)
            }
        } catch (e: Exception) {
            DetailState.Error
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = AppTheme.DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // ── HEADER: Poster + Titel + Schließen ──────────
                Row(verticalAlignment = Alignment.Top) {
                    if (movie.posterPath != null) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/w500${movie.posterPath}",
                            contentDescription = movie.title,
                            modifier = Modifier
                                .size(92.dp, 138.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(92.dp, 138.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.DarkSurface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = AppTheme.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = movie.title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.TextPrimary,
                                lineHeight = 24.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Schließen",
                                    tint = AppTheme.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppTheme.DarkSurface2
                        ) {
                            Text(
                                text = if (isTv) "TV Series" else "Movie",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppTheme.TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = AppTheme.TextTertiary.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(16.dp))

                // ── LISTEN ───────────────────────────────────────
                DetailSectionLabel("Auf folgenden Listen")
                Spacer(modifier = Modifier.height(8.dp))
                if (lists.isEmpty()) {
                    Text(
                        text = "Auf keiner Liste",
                        fontSize = 14.sp,
                        color = AppTheme.TextSecondary
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lists.forEach { listType ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF3D3500)
                            ) {
                                Text(
                                    text = listType.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppTheme.YellowAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppTheme.TextTertiary.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(16.dp))

                // ── INFO-ZEILEN ─────────────────────────────────
                if (isTv) {
                    val tv = detailState as? DetailState.Tv
                    DetailRow(
                        label = "Staffeln",
                        value = tv?.seasons?.toString(),
                        loading = detailState is DetailState.Loading
                    )
                    DetailRow(
                        label = "Folgen",
                        value = tv?.episodes?.toString(),
                        loading = detailState is DetailState.Loading
                    )
                } else {
                    val mv = detailState as? DetailState.Movie
                    DetailRow(
                        label = "Laufzeit",
                        value = mv?.runtimeMinutes?.let { "$it Min." },
                        loading = detailState is DetailState.Loading
                    )
                }

                DetailRow(
                    label = "Release",
                    value = formatReleaseDate(movie.date),
                    loading = false
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// HILFS-COMPOSABLES
// ─────────────────────────────────────────────
@Composable
private fun DetailSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.TextSecondary,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun DetailRow(label: String, value: String?, loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = AppTheme.TextSecondary
        )
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = AppTheme.YellowAccent
            )
            else -> Text(
                text = value ?: "–",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.TextPrimary
            )
        }
    }
}

// "2008-01-20" → "20.01.2008"; nur Jahr oder leer → Rohwert/–
private fun formatReleaseDate(raw: String): String? {
    if (raw.isBlank()) return null
    return runCatching {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        parser.parse(raw)?.let { formatter.format(it) }
    }.getOrNull() ?: raw.take(4).ifBlank { null }
}
