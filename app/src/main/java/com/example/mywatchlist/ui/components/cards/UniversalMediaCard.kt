package com.example.mywatchlist.ui.components.cards
import com.example.mywatchlist.data.local.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.mywatchlist.ui.theme.AppTheme

// ─────────────────────────────────────────────
// 1. UNIVERSELLE MEDIAKARTE
// onClick      → Detail-Popup öffnen
// onLongClick  → Bearbeiten (ersetzt die alte Swipe-Geste)
// ─────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalMediaCard(
    item: MovieEntity,
    modifier: Modifier = Modifier,
    onFinishClick: (() -> Unit)? = null,
    onMoveClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showFavoriteBadge: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .then(
                if (onClick != null || onLongClick != null)
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick
                    )
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // POSTER
            if (item.posterPath != null) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500${item.posterPath}",
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(75.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(75.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(AppTheme.DarkSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AppTheme.TextSecondary)
                }
            }

            // INFO
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val titleText = if (item.mediaType == "tv" && item.seasonNumber.isNotBlank()) {
                    "${item.title} ${item.seasonNumber}"
                } else {
                    item.title
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = titleText,
                        fontFamily = AppTheme.Body,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                val displayDate = item.finishedDate
                    ?: if (item.date.length >= 4) item.date.take(4) else item.date

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate,
                        fontFamily = AppTheme.Mono,
                        fontSize = 11.sp,
                        color = AppTheme.TextSecondary,
                        fontWeight = FontWeight.Normal
                    )
                    Text("\u00B7", color = AppTheme.TextTertiary, fontSize = 12.sp)
                    Surface(shape = RoundedCornerShape(6.dp), color = AppTheme.DarkSurface2) {
                        Text(
                            text = if (item.mediaType == "tv") "SERIES" else "FILM",
                            fontFamily = AppTheme.Mono,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.TextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    if (showFavoriteBadge && item.mediaType == "tv" && !item.favoriteCategory.isNullOrBlank()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = AppTheme.YellowAccent.copy(alpha = 0.14f)) {
                            Text(
                                text = item.favoriteCategory,
                                fontFamily = AppTheme.Mono,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.GoldBright,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // DRAG HANDLE, FINISH BUTTON oder MOVE BUTTON – gegenseitig exklusiv
            when {
                dragHandle != null -> dragHandle()
                onFinishClick != null -> {
                    Box(
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.YellowAccent)
                            .clickable { onFinishClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark as finished",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                onMoveClick != null -> {
                    Box(
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.YellowAccent)
                            .clickable { onMoveClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.mediaType == "tv")
                                Icons.Default.PlayArrow else Icons.Default.Check,
                            contentDescription = if (item.mediaType == "tv")
                                "Anfangen" else "Gesehen",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}