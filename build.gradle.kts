plugins {
    java
    id("com.github.johnrengelman.shadow").version("6.0.0")
}

group = "me.moros"
version = "1.6.1"

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.org/repository/maven-public")
}

dependencies {
    implementation("org.bstats", "bstats-bukkit-lite", "1.7")
    compileOnly("org.spigotmc:spigot-api:1.16.3-R0.1-SNAPSHOT")
    compileOnly("com.github.ProjectKorra:ProjectKorra:v1.9.0")
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
