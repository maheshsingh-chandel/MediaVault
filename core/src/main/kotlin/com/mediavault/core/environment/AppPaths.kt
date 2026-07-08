package com.mediavault.core.environment

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

object AppPaths {
    val appDataDirectory: Path = Path(
        System.getenv("APPDATA") ?: Path(System.getProperty("user.home"), "AppData", "Roaming").toString(),
        "MediaVault",
    )

    val localCacheDirectory: Path = Path(
        System.getenv("LOCALAPPDATA") ?: Path(System.getProperty("user.home"), "AppData", "Local").toString(),
        "MediaVault",
        "cache",
    )

    val databasePath: Path = appDataDirectory.resolve("mediavault.db")
    val thumbnailDirectory: Path = appDataDirectory.resolve("thumbnails")
    val logsDirectory: Path = appDataDirectory.resolve("logs")
    val configurationDirectory: Path = appDataDirectory.resolve("config")

    fun ensureDirectories() {
        appDataDirectory.createDirectories()
        localCacheDirectory.createDirectories()
        thumbnailDirectory.createDirectories()
        logsDirectory.createDirectories()
        configurationDirectory.createDirectories()
    }
}
