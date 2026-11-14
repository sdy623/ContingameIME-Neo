plugins {
    `maven-publish`
}

val minecraftVersion = rootProject.property("minecraft_version").toString()
val compatibilityVersion = rootProject.findProperty("minecraft_compat_version")?.toString() ?: minecraftVersion
require(compatibilityVersion in setOf("1.20.1", "1.20.6", "1.21.1", "1.21.6", "1.21.9", "1.21.10", "1.21.11")) {
    "No Minecraft compatibility sources for $minecraftVersion (API family $compatibilityVersion). " +
        "Supported API families: 1.20.1, 1.20.6, 1.21.1, 1.21.6, 1.21.9, 1.21.10, 1.21.11."
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    sourceSets.getByName("main").kotlin.srcDir("src/minecraft/$compatibilityVersion/kotlin")
}

architectury {
    val enabled_platforms: String by rootProject
    common(enabled_platforms.split(","))
}

loom {
    val versionedAccessWidener = file("src/main/resources/kitsuneime.$compatibilityVersion.accesswidener")
    accessWidenerPath.set(
        if (versionedAccessWidener.exists()) versionedAccessWidener
        else file("src/main/resources/kitsuneime.accesswidener")
    )
}

dependencies {
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    //Kotlin
    modImplementation("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_language_kotlin_version")}")
    //Architectury API
    modApi("dev.architectury:architectury:${rootProject.property("architectury_version")}")
    //Cloth Config
    modImplementation("me.shedaniel.cloth:cloth-config:${rootProject.property("cloth_config_version")}") {
        exclude("net.fabricmc.fabric-api")
    }
    implementation(kotlin("stdlib-jdk8"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = rootProject.property("archives_base_name").toString()
            from(components.getByName("java"))
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
    }
}
