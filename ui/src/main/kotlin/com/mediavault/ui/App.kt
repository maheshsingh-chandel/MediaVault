package com.mediavault.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mediavault.core.model.MediaStatistics
import com.mediavault.ui.screen.DashboardScreen
import com.mediavault.ui.screen.LibraryScreen
import com.mediavault.ui.screen.SettingsScreen

enum class AppScreen {
    DASHBOARD,
    LIBRARY,
    SETTINGS,
}

@Composable
fun MediaVaultApp(
    statistics: MediaStatistics,
) {
    var selectedScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppScaffold(
                selectedScreen = selectedScreen,
                onScreenSelected = { selectedScreen = it },
            ) {
                when (selectedScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(statistics = statistics)
                    AppScreen.LIBRARY -> LibraryScreen()
                    AppScreen.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
