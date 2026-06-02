package com.mediavault.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mediavault.core.repository.MediaFileRepository
import com.mediavault.database.DatabaseConfig
import com.mediavault.database.DatabaseInitializer
import com.mediavault.database.repository.ExposedMediaFileRepository
import com.mediavault.ui.MediaVaultApp
import org.jetbrains.exposed.v1.jdbc.Database
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

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MediaVault",
        ) {
            MediaVaultApp(
                statistics = repository.getStatistics(),
            )
        }
    }
}

private val appModule = module {
    single { DatabaseConfig() }
    single { DatabaseInitializer(get()) }
    single<MediaFileRepository> { ExposedMediaFileRepository(get<Database>()) }
}
