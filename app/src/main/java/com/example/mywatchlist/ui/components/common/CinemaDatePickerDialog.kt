package com.example.mywatchlist.ui.components.common

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.mywatchlist.R
import com.example.mywatchlist.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────
// CINEMA DATE PICKER · ein gemeinsamer Dialog statt vier Kopien
// Liefert Millis + Anzeige-String im bisherigen Format "dd.MM.yyyy".
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaDatePickerDialog(
    initialSelectedDateMillis: Long? = null,
    onConfirm: (millis: Long, display: String) -> Unit,
    onDismiss: () -> Unit,
    /** Wird aufgerufen, wenn "Bestätigen" ohne gewähltes Datum gedrückt wird. */
    onConfirmWithoutSelection: () -> Unit = {},
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)
    val pickerColors = DatePickerDefaults.colors(
        containerColor = AppTheme.DarkSurface,
        titleContentColor = AppTheme.TextPrimary,
        headlineContentColor = AppTheme.TextPrimary,
        weekdayContentColor = AppTheme.TextSecondary,
        subheadContentColor = AppTheme.TextSecondary,
        dayContentColor = AppTheme.TextPrimary,
        selectedDayContainerColor = AppTheme.YellowAccent,
        selectedDayContentColor = AppTheme.OnAccent,
        todayContentColor = AppTheme.YellowAccent,
        todayDateBorderColor = AppTheme.YellowAccent
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val display = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
                        onConfirm(millis, display)
                    } else {
                        onConfirmWithoutSelection()
                    }
                }
            ) { Text(stringResource(R.string.action_confirm), color = AppTheme.YellowAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = AppTheme.TextSecondary)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = AppTheme.DarkSurface)
    ) {
        DatePicker(state = datePickerState, colors = pickerColors)
    }
}
