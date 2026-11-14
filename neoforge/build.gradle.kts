plugins {
    id("com.github.johnrengelman.shadow") version "8.+"
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

val minecraftCompatVersion = rootProject.findProperty("minecraft_compat_version")?.toString()
    ?: rootProject.property("minecraft_version").toString()
sourceSets.getByName("main").java.srcDir("src/minecraft/$minecraftCompatVersion/java")
extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    sourceSets.getByName("main").kotlin.srcDir("src/minecraft/$minecraftCompatVersion/kotlin")
}
loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentNeoForge: Configuration = configurations.getByName("developmentNeoForge")
configurations {
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentNeoForge.extendsFrom(configurations["common"])
    // Avoid duplicate fml_loader modules from raw vs transformed loader jars.
    runtimeClasspath.get().exclude(group = "net.neoforged.fancymodloader", module = "loader")
    developmentNeoForge.exclude(group = "net.neoforged.fancymodloader", module = "loader")
}

repositories {
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

dependencies {
    //NeoForge
    neoForge("net.neoforged:neoforge:${rootProject.property("neoforge_version")}")
    //Architectury API
    modApi("dev.architectury:architectury-neoforge:${rootProject.property("architectury_version")}")
    //Kotlin for NeoForge
    implementation("thedarkcolour:kotlinforforge-neoforge:${rootProject.property("kotlinforforge_version")}")
    //Cloth Config
    modImplementation("me.shedaniel.cloth:cloth-config-neoforge:${rootProject.property("cloth_config_version")}")
    //REI (compile-only for conditional mixin support)
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-neoforge:${rootProject.property("rei_version")}")

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionNeoForge")) { isTransitive = false }
}

tasks {
    processResources {
        val metadata = mapOf(
            "version" to project.version,
            "minecraft_version" to rootProject.property("minecraft_version"),
            "neoforge_version" to rootProject.property("neoforge_version"),
            "architectury_version" to rootProject.property("architectury_version"),
            "cloth_config_version" to rootProject.property("cloth_config_version")
        )
        inputs.properties(metadata)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(metadata)
        }

        filesMatching("kitsuneime.neoforge.mixins.json") {
            expand("java_version" to (rootProject.findProperty("java_version") ?: 21))
        }
    }

    shadowJar {
        exclude("architectury.common.json")
        exclude("fabric.mod.json")
        configurations = listOf(project.configurations["shadowCommon"])
        archiveClassifier.set("dev-shadow")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    remapJar {
        injectAccessWidener.set(true)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
        dependsOn(shadowJar)
        archiveClassifier.set("neoforge")
    }

    jar {
        archiveClassifier.set("dev")
    }

    sourcesJar {
        val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
        dependsOn(commonSources)
        from(commonSources.archiveFile.map { zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
