package com.example.mywatchlist.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mywatchlist.ui.theme.AppTheme

// ─────────────────────────────────────────────
// LEER-ZUSTAND · Icon-Medaillon + Nachricht + optionaler Hinweis
// Ersetzt den bisherigen zentrierten Grautext auf allen Listen.
// ─────────────────────────────────────────────
@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(AppTheme.DarkSurface, CircleShape)
                    .border(1.dp, AppTheme.Hairline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppTheme.YellowAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            if (hint != null) {
                Text(
                    text = hint.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
