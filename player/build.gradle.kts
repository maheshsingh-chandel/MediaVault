plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation("uk.co.caprica:vlcj:4.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
