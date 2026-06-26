package com.example.mywatchlist.ui.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mywatchlist.R
import com.example.mywatchlist.ui.theme.AppTheme

// ─────────────────────────────────────────────
@Composable
fun SeasonPicker(
    seasonNumber: String,
    onSeasonChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = seasonNumber,
        onValueChange = onSeasonChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(stringResource(R.string.season_hint), color = AppTheme.TextTertiary, fontSize = 13.sp)
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
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
}