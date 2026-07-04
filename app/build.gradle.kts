plugins {
    id("fabric-loom") version "1.16.2"
    id("maven-publish")
}

group = "minecrfat.tv"
version = "1.0.0"

base {
    archivesName.set("minecraft-tv")
}

loom {
    noIntermediateMappings()
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:26.2")
    mappings(files("mappings/official-named.jar"))
    modImplementation("net.fabricmc:fabric-loader:0.19.2")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.154.0+26.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

val modVersion = project.version.toString()

tasks.processResources {
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}
