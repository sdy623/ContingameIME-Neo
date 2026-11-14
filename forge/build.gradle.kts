plugins {
    id("com.github.johnrengelman.shadow") version "8.+"
}

architectury {
    platformSetupLoomIde()
    forge()
}

val minecraftCompatVersion = rootProject.findProperty("minecraft_compat_version")?.toString()
    ?: rootProject.property("minecraft_version").toString()
sourceSets.getByName("main").java.srcDir("src/minecraft/$minecraftCompatVersion/java")
extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    sourceSets.getByName("main").kotlin.srcDir("src/minecraft/$minecraftCompatVersion/kotlin")
}
val forgeReiVersion = rootProject.findProperty("forge_rei_version")?.toString()
if (forgeReiVersion == null) {
    sourceSets.getByName("main").java.exclude("**/MixinTextFieldWidget.java")
}
loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentForge: Configuration = configurations.getByName("developmentForge")
configurations {
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentForge.extendsFrom(configurations["common"])
}

repositories {
    maven("https://maven.minecraftforge.net")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

dependencies {
    add("forge", "net.minecraftforge:forge:${rootProject.property("minecraft_version")}-${rootProject.property("forge_version")}")
    modApi("dev.architectury:architectury-forge:${rootProject.property("architectury_version")}")
    implementation("thedarkcolour:kotlinforforge:${rootProject.property("kotlinforforge_version")}")
    // Forge does not expose MixinExtras' @Local annotations on its compile classpath.
    compileOnly("io.github.llamalad7:mixinextras-common:0.4.1")
    modImplementation("io.github.llamalad7:mixinextras-forge:0.4.1")
    modImplementation("me.shedaniel.cloth:cloth-config-forge:${rootProject.property("cloth_config_version")}")
    forgeReiVersion?.let { version ->
        modCompileOnly("me.shedaniel:RoughlyEnoughItems-forge:$version")
    }

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionForge")) { isTransitive = false }
}

tasks {
    processResources {
        val kotlinForForgeVersion = rootProject.property("kotlinforforge_version").toString()
        val metadata = mapOf(
            "version" to project.version,
            "minecraft_version" to rootProject.property("minecraft_version"),
            "forge_version" to rootProject.property("forge_version"),
            "kotlinforforge_major" to kotlinForForgeVersion.substringBefore('.'),
            "architectury_version" to rootProject.property("architectury_version"),
            "cloth_config_version" to rootProject.property("cloth_config_version")
        )
        inputs.properties(metadata)

        filesMatching("META-INF/mods.toml") {
            expand(metadata)
        }

        filesMatching("kitsuneime.forge.mixins.json") {
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
        archiveClassifier.set("forge")
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
