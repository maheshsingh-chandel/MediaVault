package com.mediavault.database

import java.nio.file.Path
import kotlin.io.path.Path

data class DatabaseConfig(
    val databasePath: Path = defaultDatabasePath(),
) {
    val jdbcUrl: String = "jdbc:sqlite:${databasePath.toAbsolutePath()}"
}

private fun defaultDatabasePath(): Path {
    val home = System.getProperty("user.home")
    return Path(home, ".mediavault", "mediavault.db")
}
