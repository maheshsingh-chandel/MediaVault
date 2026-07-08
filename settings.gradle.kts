pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MediaVault"

include(":app")
include(":core")
include(":database")
include(":metadata")
include(":monitor")
include(":scanner")
include(":thumbnail")
include(":ui")
