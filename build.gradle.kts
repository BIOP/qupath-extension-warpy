plugins {
    // To optionally create a shadow/fat jar that bundle up any non-core dependencies
    // id("com.gradleup.shadow") version "8.3.5"
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
    // Add the maven-publish plugin
    id("maven-publish")
}

// TODO: Configure your extension here (please change the defaults!)
qupathExtension {
    name = "qupath-extension-warpy"
    group = "qupath.ext.warpy"
    version = "0.4.2"
    description = "Warpy - QuPath extension that supports spline transformations."
    automaticModule = "qupath.ext.warpy"
}

// TODO: Define your dependencies here
dependencies {
    implementation(libs.bundles.qupath)
    implementation(libs.qupath.fxtras)
    implementation("commons-io:commons-io:2.15.0")
    implementation("net.imglib2:imglib2-realtransform:3.1.2")
}

publishing {
    repositories {
        maven {
            name = "SciJava"
            val releasesRepoUrl = uri("https://maven.scijava.org/content/repositories/releases")
            val snapshotsRepoUrl = uri("https://maven.scijava.org/content/repositories/snapshots")
            // Use gradle -Prelease publish
            url = if (project.hasProperty("release")) releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PASS")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                licenses {
                    license {
                        name.set("BSD-3-Clause")
                        url.set("https://opensource.org/licenses/BSD-3-Clause")
                    }
                }
            }
        }
    }
}
