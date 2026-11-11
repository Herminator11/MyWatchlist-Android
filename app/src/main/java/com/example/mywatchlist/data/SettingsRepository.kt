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
        private val KEY_ACCENT = stringPreferencesKey("accent")
        private val KEY_AMOLED = booleanPreferencesKey("amoled")
        private val KEY_NAV_LABELS = booleanPreferencesKey("nav_labels")
        private val KEY_HAPTICS = booleanPreferencesKey("haptics")
        private val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        private val KEY_DENSITY = stringPreferencesKey("list_density")
        private val KEY_SHOW_YEAR = booleanPreferencesKey("list_show_year")
        private val KEY_SHOW_TYPE = booleanPreferencesKey("list_show_type")
        private val KEY_SORT_WATCHLIST = stringPreferencesKey("sort_watchlist")
        private val KEY_SORT_WATCHING = stringPreferencesKey("sort_watching")
        private val KEY_WATCHLIST_TAB = stringPreferencesKey("watchlist_start_tab")
    }

    val listPrefs: Flow<ListPrefs> = context.dataStore.data.map { p ->
        val d = ListPrefs()
        ListPrefs(
            density = enumOrDefault(p[KEY_DENSITY], d.density),
            showYear = p[KEY_SHOW_YEAR] ?: d.showYear,
            showTypeTag = p[KEY_SHOW_TYPE] ?: d.showTypeTag,
            watchlistSort = enumOrDefault(p[KEY_SORT_WATCHLIST], d.watchlistSort),
            watchingSort = enumOrDefault(p[KEY_SORT_WATCHING], d.watchingSort),
            watchlistStartTab = enumOrDefault(p[KEY_WATCHLIST_TAB], d.watchlistStartTab),
        )
    }

    suspend fun setListPrefs(prefs: ListPrefs) {
        context.dataStore.edit { p ->
            p[KEY_DENSITY] = prefs.density.name
            p[KEY_SHOW_YEAR] = prefs.showYear
            p[KEY_SHOW_TYPE] = prefs.showTypeTag
            p[KEY_SORT_WATCHLIST] = prefs.watchlistSort.name
            p[KEY_SORT_WATCHING] = prefs.watchingSort.name
            p[KEY_WATCHLIST_TAB] = prefs.watchlistStartTab.name
        }
    }

    val appearance: Flow<AppearancePrefs> = context.dataStore.data.map { p ->
        val d = AppearancePrefs()
        AppearancePrefs(
            accent = AccentOption.fromName(p[KEY_ACCENT]),
            amoled = p[KEY_AMOLED] ?: d.amoled,
            navLabels = p[KEY_NAV_LABELS] ?: d.navLabels,
            haptics = p[KEY_HAPTICS] ?: d.haptics,
            reduceMotion = p[KEY_REDUCE_MOTION] ?: d.reduceMotion,
        )
    }

    suspend fun setAppearance(prefs: AppearancePrefs) {
        context.dataStore.edit { p ->
            p[KEY_ACCENT] = prefs.accent.name
            p[KEY_AMOLED] = prefs.amoled
            p[KEY_NAV_LABELS] = prefs.navLabels
            p[KEY_HAPTICS] = prefs.haptics
            p[KEY_REDUCE_MOTION] = prefs.reduceMotion
        }
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