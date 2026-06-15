package com.mediavault.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mediavault.core.metadata.MetadataService
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.core.scanner.MediaScanner
import com.mediavault.core.thumbnail.ThumbnailService
import com.mediavault.database.DatabaseConfig
import com.mediavault.database.DatabaseInitializer
import com.mediavault.database.repository.ExposedMediaFileRepository
import com.mediavault.metadata.MediaMetadataService
import com.mediavault.scanner.DefaultMountedDriveProvider
import com.mediavault.scanner.MountedDriveProvider
import com.mediavault.scanner.WalkTreeMediaScanner
import com.mediavault.thumbnail.DefaultThumbnailService
import com.mediavault.ui.MediaVaultApp
import org.jetbrains.exposed.v1.jdbc.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() {
    val koin = startKoin {
        modules(appModule)
    }.koin

    val databaseInitializer = koin.get<DatabaseInitializer>()
    val database = databaseInitializer.initialize()
    koin.declare(database)

    val repository = koin.get<MediaFileRepository>()
    val scanner = koin.get<MediaScanner>()
    val thumbnailService = koin.get<ThumbnailService>()
    val metadataService = koin.get<MetadataService>()
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    application {
        Window(
            onCloseRequest = {
                appScope.cancel()
                thumbnailService.close()
                exitApplication()
            },
            title = "MediaVault",
        ) {
            var progress by remember {
                mutableStateOf(scanner.progress.value)
            }

            LaunchedEffect(scanner) {
                scanner.progress.collectLatest {
                    progress = it
                }
            }

            MediaVaultApp(
                initialStatistics = repository.getStatistics(),
                mediaFileRepository = repository,
                thumbnailService = thumbnailService,
                metadataService = metadataService,
                scanProgress = progress,
                loadStatistics = repository::getStatistics,
                onStartScan = {
                    if (!scanner.progress.value.isScanning) {
                        appScope.launch {
                            scanner.scanAllMountedDrives()
                        }
                    }
                },
            )
        }
    }
}

private val appModule = module {
    single { DatabaseConfig() }
    single { DatabaseInitializer(get()) }
    single<MountedDriveProvider> { DefaultMountedDriveProvider() }
    single<MediaFileRepository> { ExposedMediaFileRepository(get<Database>()) }
    single<MediaScanner> { WalkTreeMediaScanner(get(), get()) }
    single<ThumbnailService> { DefaultThumbnailService() }
    single<MetadataService> { MediaMetadataService(get()) }
}
