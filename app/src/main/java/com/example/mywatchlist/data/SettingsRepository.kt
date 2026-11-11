package com.example.mywatchlist.data
import androidx.datastore.preferences.core.stringPreferencesKey

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore-Instanz als Extension auf Context – Singleton per App-Prozess
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mywatchlist_settings"
)

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_AUTO_HIDE_NAV_BAR = booleanPreferencesKey("auto_hide_nav_bar")
        private const val DEFAULT_AUTO_HIDE = true
        private val KEY_NAV_ORDER = stringPreferencesKey("nav_order")
        private val KEY_NAV_HIDDEN = stringPreferencesKey("nav_hidden")
        private val KEY_NAV_START = stringPreferencesKey("nav_start")
    }

    // Tab-Reihenfolge, ausgeblendete Tabs und Start-Tab – beim Lesen normalisiert
    val navConfig: Flow<NavConfig> = context.dataStore.data
        .map { prefs -> NavConfig.parse(prefs[KEY_NAV_ORDER], prefs[KEY_NAV_HIDDEN], prefs[KEY_NAV_START]) }

    suspend fun setNavConfig(config: NavConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NAV_ORDER] = config.orderCsv()
            prefs[KEY_NAV_HIDDEN] = config.hiddenCsv()
            prefs[KEY_NAV_START] = config.start
        }
    }

    suspend fun resetNavConfig() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_NAV_ORDER); prefs.remove(KEY_NAV_HIDDEN); prefs.remove(KEY_NAV_START)
        }
    }

    // Flow der den aktuellen Wert liefert – emittet sofort beim Subscriben
    val autoHideNavBar: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_AUTO_HIDE_NAV_BAR] ?: DEFAULT_AUTO_HIDE }

    suspend fun setAutoHideNavBar(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_HIDE_NAV_BAR] = enabled
        }
    }
}