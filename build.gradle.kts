plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.3.0"
}

group = "com.andreih"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.typesafe:config:1.4.3")
    implementation("ai.koog:koog-agents:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0-RC")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}