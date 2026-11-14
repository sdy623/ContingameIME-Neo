pluginManagement {
    val kotlinPluginVersion = providers.gradleProperty("kotlin_version").getOrElse("2.2.21")
    repositories {
        maven {
            name = "Shedaniel Maven"
            setUrl("https://maven.shedaniel.me/")
        }
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinPluginVersion
    }
}

include("common")

// Only create platform projects selected by the target profile. A single-loader
// build then resolves only that loader's artifacts while preserving default projects.
val enabledPlatforms = providers.gradleProperty("enabled_platforms")
    .getOrElse("fabric,neoforge")
    .split(",")
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toSet()
for (platform in enabledPlatforms) {
    require(platform in setOf("fabric", "forge", "neoforge")) { "Unknown loader module: $platform" }
    include(platform)
}

rootProject.name = "ContingameIME-Neo"
