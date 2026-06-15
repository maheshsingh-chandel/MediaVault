plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":database"))
    implementation(project(":scanner"))
    implementation(project(":thumbnail"))
    implementation(project(":ui"))

    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

compose.desktop {
    application {
        mainClass = "com.mediavault.app.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "MediaVault"
            packageVersion = "0.1.0"
        }
    }
}
