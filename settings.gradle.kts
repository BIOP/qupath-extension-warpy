pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.scijava.org/content/repositories/releases")
        }
    }
}

plugins {
    id("io.github.qupath.qupath-extension-settings") version "0.2.1"
}

qupath {
    version = "0.6.0"
}