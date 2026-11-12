package com.example.mywatchlist.ui.components.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mywatchlist.R
import com.example.mywatchlist.data.local.MovieEntity
import com.example.mywatchlist.ui.theme.AppTheme

// ─────────────────────────────────────────────
// POSTER-KACHEL · Raster-Ansicht (Einstellung „Kartendichte: Poster“)
// Gleiches Verhalten wie UniversalMediaCard: Tippen = Details,
// lange drücken = Bearbeiten, Aktions-Button unten rechts.
// ─────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterTile(
    item: MovieEntity,
    modifier: Modifier = Modifier,
    onFinishClick: (() -> Unit)? = null,
    onMoveClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val prefs = LocalListPrefs.current
    val title = if (item.mediaType == "tv" && item.seasonNumber.isNotBlank()) "${item.title} ${item.seasonNumber}" else item.title
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClickLabel = stringResource(R.string.a11y_edit_entry),
                onLongClick = onLongClick
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.small)
                .background(AppTheme.DarkSurface2)
        ) {
            if (item.posterPath != null) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w342${item.posterPath}",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.Info, null, tint = AppTheme.TextSecondary, modifier = Modifier.align(Alignment.Center))
            }
            val action = onFinishClick ?: onMoveClick
            if (action != null) {
                val isFinish = onFinishClick != null
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(48.dp)
                        .padding(6.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(AppTheme.YellowAccent)
                        .clickable(role = Role.Button) { action() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!isFinish && item.mediaType == "tv") Icons.Default.PlayArrow else Icons.Default.Check,
                        contentDescription = when {
                            isFinish -> stringResource(R.string.a11y_mark_finished)
                            item.mediaType == "tv" -> stringResource(R.string.move_start_watching)
                            else -> stringResource(R.string.move_mark_watched)
                        },
                        tint = AppTheme.OnAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = AppTheme.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (prefs.showYear && item.date.length >= 4) {
            Text(
                text = item.date.take(4),
                style = MaterialTheme.typography.labelMedium,
                color = AppTheme.TextSecondary
            )
        }
    }
}
