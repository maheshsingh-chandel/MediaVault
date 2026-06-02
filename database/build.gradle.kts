plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))

    implementation("org.jetbrains.exposed:exposed-core:1.3.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("org.xerial:sqlite-jdbc:3.53.0.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
