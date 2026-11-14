plugins {
    id("com.github.johnrengelman.shadow") version "8.+"
}

architectury {
    platformSetupLoomIde()
    fabric()
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
val shadowCommon: Configuration by configurations.creating // Don't use shadow from the shadow plugin because we don't want IDEA to index this.
val developmentFabric: Configuration = configurations.getByName("developmentFabric")
configurations {
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentFabric.extendsFrom(configurations["common"])
}

repositories {
    maven("https://maven.fabricmc.net")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://maven.ladysnake.org/releases") {
        mavenContent {
            includeGroup("io.github.ladysnake")
            includeGroup("org.ladysnake")
            includeGroupByRegex("dev\\.onyxstudios.*")
        }
    }
}

dependencies {
    //Fabric
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_version")}")
    //Cloth Api? "The Cloth API has largely been replaced by the Architectury API."
    //Architectury API
    modApi("dev.architectury:architectury-fabric:${rootProject.property("architectury_version")}")
    //REI
    modImplementation("me.shedaniel:RoughlyEnoughItems-fabric:${rootProject.property("rei_version")}")
    //Stain
    rootProject.findProperty("satin_version")?.toString()?.let { satinVersion ->
        modCompileOnly("io.github.ladysnake:satin:$satinVersion")
    }
    //Kotlin
    modImplementation("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_language_kotlin_version")}")
    //Cloth Config
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${rootProject.property("cloth_config_version")}") {
        exclude("net.fabricmc.fabric-api")
    }

    common(project(":common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", configuration = "transformProductionFabric")) { isTransitive = false }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("minecraft_version", rootProject.property("minecraft_version"))

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "minecraft_version" to rootProject.property("minecraft_version")
            )
        }

        filesMatching("kitsuneime.fabric.mixins.json") {
            expand("java_version" to (rootProject.findProperty("java_version") ?: 21))
        }
    }

    shadowJar {
        exclude("architectury.common.json")
        configurations = listOf(project.configurations["shadowCommon"])
        archiveClassifier.set("dev-shadow")
    }

    remapJar {
        injectAccessWidener.set(true)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
        dependsOn(shadowJar)
        archiveClassifier.set("fabric")
    }

    jar {
        archiveClassifier.set("dev")
    }

    sourcesJar {
        val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
        dependsOn(commonSources)
        from(commonSources.archiveFile.map { zipTree(it) })
    }
}
