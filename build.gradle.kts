plugins {
    kotlin("jvm") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.11.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
}

allprojects {
    group = "com.mediavault"
    version = "1.0.0"
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }
}
