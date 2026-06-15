plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}
