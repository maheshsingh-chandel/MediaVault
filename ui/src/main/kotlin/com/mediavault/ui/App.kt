package com.mediavault.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mediavault.core.duplicate.DuplicateDetectionService
import com.mediavault.core.environment.AppEnvironmentInfo
import com.mediavault.core.environment.FirstLaunchState
import com.mediavault.core.metadata.MetadataService
import com.mediavault.core.model.MediaStatistics
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.core.scanner.ScanProgress
import com.mediavault.core.thumbnail.ThumbnailService
import com.mediavault.ui.screen.DashboardScreen
import com.mediavault.ui.screen.DuplicatesScreen
import com.mediavault.ui.screen.LibraryScreen
import com.mediavault.ui.screen.SettingsScreen
import com.mediavault.ui.screen.AboutScreen
import com.mediavault.ui.screen.WelcomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppScreen {
    DASHBOARD,
    LIBRARY,
    DUPLICATES,
    SETTINGS,
    ABOUT,
}

@Composable
fun MediaVaultApp(
    initialStatistics: MediaStatistics,
    mediaFileRepository: MediaFileRepository,
    thumbnailService: ThumbnailService,
    metadataService: MetadataService,
    duplicateDetectionService: DuplicateDetectionService,
    environmentInfo: AppEnvironmentInfo,
    firstLaunchState: FirstLaunchState,
    userMessage: String?,
    onDismissMessage: () -> Unit,
    scanProgress: ScanProgress,
    fileSystemChangeVersion: Long,
    loadStatistics: () -> MediaStatistics,
    onStartScan: () -> Unit,
) {
    var selectedScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var statistics by remember { mutableStateOf(initialStatistics) }
    var showWelcome by remember(firstLaunchState.isFirstLaunch) { mutableStateOf(firstLaunchState.isFirstLaunch) }

    LaunchedEffect(scanProgress.discoveredMediaFiles, scanProgress.isScanning, fileSystemChangeVersion) {
        statistics = withContext(Dispatchers.IO) {
            loadStatistics()
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (showWelcome) {
                WelcomeScreen(
                    onStartIndexing = {
                        showWelcome = false
                        onStartScan()
                    },
                    onSkip = { showWelcome = false },
                )
            } else {
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
                        AppScreen.LIBRARY -> LibraryScreen(
                            repository = mediaFileRepository,
                            thumbnailService = thumbnailService,
                            metadataService = metadataService,
                            refreshVersion = fileSystemChangeVersion,
                        )
                        AppScreen.DUPLICATES -> DuplicatesScreen(
                            repository = mediaFileRepository,
                            duplicateDetectionService = duplicateDetectionService,
                            refreshVersion = fileSystemChangeVersion,
                        )
                        AppScreen.SETTINGS -> SettingsScreen(environmentInfo = environmentInfo)
                        AppScreen.ABOUT -> AboutScreen(environmentInfo = environmentInfo)
                    }
                }
            }

            userMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = onDismissMessage,
                    confirmButton = {
                        TextButton(onClick = onDismissMessage) {
                            Text("OK")
                        }
                    },
                    title = { Text("Something needs attention") },
                    text = {
                        Text(text = message)
                    },
                )
            }
        }
    }
}
