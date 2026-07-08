package com.mediavault.database

import com.mediavault.core.environment.AppPaths
import java.nio.file.Path

data class DatabaseConfig(
    val databasePath: Path = AppPaths.databasePath,
) {
    val jdbcUrl: String = "jdbc:sqlite:${databasePath.toAbsolutePath()}"
}
