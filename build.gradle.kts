plugins {
    java
    id("com.github.johnrengelman.shadow").version("8.1.1")
}

group = "me.moros"
version = "1.7.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.bstats", "bstats-bukkit", "3.0.2")
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly("com.github.ProjectKorra:ProjectKorra:v1.11.1")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        dependencies {
            relocate("org.bstats", "me.moros.hyperion.bstats")
        }
        minimize()
    }
    build {
        dependsOn(shadowJar)
    }
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    named<Copy>("processResources") {
        expand("pluginVersion" to project.version)
    }
}
