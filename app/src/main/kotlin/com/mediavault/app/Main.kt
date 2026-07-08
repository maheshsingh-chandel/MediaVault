package com.mediavault.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.mediavault.core.duplicate.DuplicateDetectionService
import com.mediavault.core.environment.AppEnvironmentInfo
import com.mediavault.core.environment.AppPaths
import com.mediavault.core.environment.AppVersion
import com.mediavault.core.metadata.MetadataService
import com.mediavault.core.monitor.FileSystemMonitor
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.core.scanner.MediaScanner
import com.mediavault.core.thumbnail.ThumbnailService
import com.mediavault.database.DatabaseConfig
import com.mediavault.database.DatabaseInitializer
import com.mediavault.database.repository.ExposedMediaFileRepository
import com.mediavault.duplicate.DefaultDuplicateDetectionService
import com.mediavault.metadata.MediaMetadataService
import com.mediavault.monitor.WatchServiceFileSystemMonitor
import com.mediavault.scanner.DefaultMountedDriveProvider
import com.mediavault.scanner.MountedDriveProvider
import com.mediavault.scanner.WalkTreeMediaScanner
import com.mediavault.thumbnail.DefaultThumbnailService
import com.mediavault.thumbnail.ThumbnailCache
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
import org.slf4j.LoggerFactory

fun main() {
    AppPaths.ensureDirectories()
    val logger = LoggerFactory.getLogger("MediaVault")
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        logger.error("Unhandled application error", throwable)
    }
    logger.info("Starting MediaVault")

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
    val duplicateDetectionService = koin.get<DuplicateDetectionService>()
    val fileSystemMonitor = koin.get<FileSystemMonitor>()
    val databaseConfig = koin.get<DatabaseConfig>()
    val firstLaunchStore = koin.get<FirstLaunchStore>()
    val environmentInfo = AppEnvironmentInfo(
        version = AppVersion.Current,
        databasePath = databaseConfig.databasePath.toAbsolutePath().toString(),
        thumbnailDirectory = ThumbnailCache.defaultThumbnailDirectory().toAbsolutePath().toString(),
        logsDirectory = AppPaths.logsDirectory.toAbsolutePath().toString(),
        cacheDirectory = AppPaths.localCacheDirectory.toAbsolutePath().toString(),
        configurationDirectory = AppPaths.configurationDirectory.toAbsolutePath().toString(),
        dependencies = RuntimeDependencyChecker.checkAll(),
    )
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fileSystemMonitor.start()

    application {
        Window(
            onCloseRequest = {
                appScope.cancel()
                fileSystemMonitor.close()
                thumbnailService.close()
                exitApplication()
            },
            title = "MediaVault",
            icon = painterResource("mediavault-icon.svg"),
        ) {
            var progress by remember {
                mutableStateOf(scanner.progress.value)
            }
            var monitorState by remember {
                mutableStateOf(fileSystemMonitor.state.value)
            }
            var firstLaunchState by remember {
                mutableStateOf(firstLaunchStore.load())
            }
            var userMessage by remember {
                mutableStateOf<String?>(null)
            }

            LaunchedEffect(scanner) {
                scanner.progress.collectLatest {
                    progress = it
                }
            }

            LaunchedEffect(fileSystemMonitor) {
                fileSystemMonitor.state.collectLatest {
                    monitorState = it
                }
            }

            MediaVaultApp(
                initialStatistics = repository.getStatistics(),
                mediaFileRepository = repository,
                thumbnailService = thumbnailService,
                metadataService = metadataService,
                duplicateDetectionService = duplicateDetectionService,
                environmentInfo = environmentInfo,
                firstLaunchState = firstLaunchState,
                userMessage = userMessage,
                onDismissMessage = { userMessage = null },
                scanProgress = progress,
                fileSystemChangeVersion = monitorState.changeVersion,
                loadStatistics = repository::getStatistics,
                onStartScan = {
                    if (!scanner.progress.value.isScanning) {
                        userMessage = null
                        appScope.launch {
                            runCatching {
                                scanner.scanAllMountedDrives()
                                fileSystemMonitor.refreshWatchedDirectories()
                                firstLaunchStore.markComplete()
                                firstLaunchState = firstLaunchStore.load()
                            }.onFailure { throwable ->
                                logger.error("Scan failed", throwable)
                                userMessage = "MediaVault could not finish indexing. Check Settings for log file location and try again."
                            }
                        }
                    }
                },
            )
        }
    }
}

private val appModule = module {
    single { DatabaseConfig() }
    single { FirstLaunchStore() }
    single { DatabaseInitializer(get()) }
    single<MountedDriveProvider> { DefaultMountedDriveProvider() }
    single<MediaFileRepository> { ExposedMediaFileRepository(get<Database>()) }
    single<MediaScanner> { WalkTreeMediaScanner(get(), get()) }
    single<ThumbnailService> { DefaultThumbnailService() }
    single<MetadataService> { MediaMetadataService(get()) }
    single<DuplicateDetectionService> { DefaultDuplicateDetectionService(get()) }
    single<FileSystemMonitor> { WatchServiceFileSystemMonitor(get()) }
}
