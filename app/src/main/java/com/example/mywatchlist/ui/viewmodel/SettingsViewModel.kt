package com.example.mywatchlist.ui.viewmodel
import com.example.mywatchlist.data.local.*
import com.example.mywatchlist.data.remote.*
import com.example.mywatchlist.data.*

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mywatchlist.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    // StateFlow – sofort verfügbar mit Default true,
    // wird mit gespeichertem Wert aktualisiert sobald DataStore gelesen hat
    val autoHideNavBar: StateFlow<Boolean> = repository.autoHideNavBar
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    fun setAutoHideNavBar(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoHideNavBar(enabled)
        }
    }
}