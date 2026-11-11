package com.example.mywatchlist.ui.viewmodel
import com.example.mywatchlist.data.ListPrefs
import com.example.mywatchlist.data.AppearancePrefs
import com.example.mywatchlist.data.NavConfig
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

    // null bis DataStore gelesen ist → die Navigation wird erst dann aufgebaut,
    // damit der Start-Tab nicht kurz auf den Default springt.
    val navConfig: StateFlow<NavConfig?> = repository.navConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun updateNavConfig(transform: (NavConfig) -> NavConfig) {
        val current = navConfig.value ?: return
        val next = transform(current)
        if (next != current) viewModelScope.launch { repository.setNavConfig(next) }
    }

    // Darstellung: null bis geladen → kein kurzes Aufblitzen der Standardfarbe
    val appearance: StateFlow<AppearancePrefs?> = repository.appearance
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun updateAppearance(transform: (AppearancePrefs) -> AppearancePrefs) {
        val current = appearance.value ?: return
        val next = transform(current)
        if (next != current) viewModelScope.launch { repository.setAppearance(next) }
    }

    // null bis geladen → z. B. der Watchlist-Start-Tab stimmt schon beim ersten Frame
    val listPrefs: StateFlow<ListPrefs?> = repository.listPrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun updateListPrefs(transform: (ListPrefs) -> ListPrefs) {
        val current = listPrefs.value ?: return
        val next = transform(current)
        if (next != current) viewModelScope.launch { repository.setListPrefs(next) }
    }

    fun resetNavConfig() {
        viewModelScope.launch { repository.resetNavConfig() }
    }

    fun setAutoHideNavBar(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoHideNavBar(enabled)
        }
    }
}