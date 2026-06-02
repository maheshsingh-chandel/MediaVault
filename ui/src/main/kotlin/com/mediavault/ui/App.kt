package com.mediavault.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.core.scanner.ScanProgress
import com.mediavault.ui.screen.DashboardScreen
import com.mediavault.ui.screen.LibraryScreen
import com.mediavault.ui.screen.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppScreen {
    DASHBOARD,
    LIBRARY,
    SETTINGS,
}

@Composable
fun MediaVaultApp(
    initialStatistics: MediaStatistics,
    mediaFileRepository: MediaFileRepository,
    scanProgress: ScanProgress,
    loadStatistics: () -> MediaStatistics,
    onStartScan: () -> Unit,
) {
    var selectedScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var statistics by remember { mutableStateOf(initialStatistics) }

    LaunchedEffect(scanProgress.discoveredMediaFiles, scanProgress.isScanning) {
        statistics = withContext(Dispatchers.IO) {
            loadStatistics()
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppScaffold(
                selectedScreen = selectedScreen,
                onScreenSelected = { selectedScreen = it },
            ) {
                when (selectedScreen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        statistics = statistics,
                        scanProgress = scanProgress,
                        onStartScan = onStartScan,
                    )
                    AppScreen.LIBRARY -> LibraryScreen(repository = mediaFileRepository)
                    AppScreen.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
