package com.mediavault.database

import com.mediavault.database.table.MediaFilesTable
import kotlin.io.path.createDirectories
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class DatabaseInitializer(
    private val config: DatabaseConfig,
) {
    fun initialize(): Database {
        config.databasePath.parent?.createDirectories()

        val database = Database.connect(
            url = config.jdbcUrl,
            driver = "org.sqlite.JDBC",
        )

        transaction(database) {
            SchemaUtils.create(MediaFilesTable)
        }

        return database
    }
}
