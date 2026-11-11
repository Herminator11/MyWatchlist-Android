package com.example.mywatchlist
import com.example.mywatchlist.ui.components.cards.LocalContentPrefs
import androidx.compose.runtime.CompositionLocalProvider
import com.example.mywatchlist.ui.components.cards.LocalListPrefs
import com.example.mywatchlist.ui.viewmodel.SettingsViewModel
import com.example.mywatchlist.ui.theme.cinemaPalette
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import com.example.mywatchlist.ui.screens.*

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mywatchlist.ui.theme.MyWatchlistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-Edge auf allen API-Levels (ab Android 15 ohnehin erzwungen):
        // transparente, dunkle Systemleisten über dem Noir-Hintergrund.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val appearance by settingsViewModel.appearance.collectAsStateWithLifecycle()
            val listPrefsState by settingsViewModel.listPrefs.collectAsStateWithLifecycle()
            val contentPrefs by settingsViewModel.contentPrefs.collectAsStateWithLifecycle()
            // Bis die Einstellungen geladen sind (wenige ms): neutraler Noir-Grund
            val listPrefs = listPrefsState
            val prefs = appearance?.takeIf { listPrefs != null } ?: run {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0B0B0D)))
                return@setContent
            }
            val palette = remember(prefs.accent, prefs.amoled) { cinemaPalette(prefs.accent, prefs.amoled) }
            MyWatchlistTheme(
                colors = palette,
                forceReduceMotion = prefs.reduceMotion,
                hapticsEnabled = prefs.haptics,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(
                        LocalListPrefs provides listPrefs!!,
                        LocalContentPrefs provides contentPrefs,
                    ) {
                        MainScreenApp()
                    }
                }
            }
        }
    }
}