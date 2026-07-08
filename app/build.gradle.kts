plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":database"))
    implementation(project(":duplicate"))
    implementation(project(":metadata"))
    implementation(project(":monitor"))
    implementation(project(":player"))
    implementation(project(":scanner"))
    implementation(project(":thumbnail"))
    implementation(project(":ui"))

    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("ch.qos.logback:logback-classic:1.5.21")
}

compose.desktop {
    application {
        mainClass = "com.mediavault.app.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
            )
            packageName = "MediaVault"
            packageVersion = project.version.toString()
            description = "Local media library manager for desktop"
            copyright = "Copyright (C) 2026 Mahesh Chandel"
            vendor = "Mahesh Chandel"
            licenseFile.set(project.file("../LICENSE.txt"))

            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.naming",
                "java.sql",
                "jdk.unsupported",
            )

            windows {
                menuGroup = "MediaVault"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "3c7f3a78-04fb-4c40-9b7b-67b413f36d19"
                iconFile.set(project.file("src/main/resources/mediavault-icon.ico"))
            }
        }
    }
}

tasks.register("cleanRelease") {
    group = "distribution"
    description = "Remove generated release installer artifacts."
    delete(layout.buildDirectory.dir("compose/binaries"))
    delete(layout.buildDirectory.dir("release"))
}

tasks.register("packageRelease") {
    group = "distribution"
    description = "Build Windows EXE and MSI installers with jpackage."
    dependsOn("packageExe", "packageMsi")
}

tasks.register("createInstaller") {
    group = "distribution"
    description = "Alias for packageRelease."
    dependsOn("packageRelease")
}
