package com.mediavault.app

import com.mediavault.core.environment.AppPaths
import com.mediavault.core.environment.FirstLaunchState
import java.util.Properties
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

class FirstLaunchStore {
    private val file = AppPaths.configurationDirectory.resolve("app.properties")

    fun load(): FirstLaunchState {
        if (file.notExists()) {
            return FirstLaunchState(isFirstLaunch = true)
        }

        val properties = Properties()
        file.inputStream().use(properties::load)
        return FirstLaunchState(
            isFirstLaunch = properties.getProperty(FIRST_LAUNCH_COMPLETE) != "true",
        )
    }

    fun markComplete() {
        AppPaths.configurationDirectory.createDirectories()
        val properties = Properties()
        if (file.notExists().not()) {
            file.inputStream().use(properties::load)
        }
        properties.setProperty(FIRST_LAUNCH_COMPLETE, "true")
        file.outputStream().use { properties.store(it, "MediaVault configuration") }
    }

    private companion object {
        const val FIRST_LAUNCH_COMPLETE = "firstLaunchComplete"
    }
}
