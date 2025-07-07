plugins {
    // To optionally create a shadow/fat jar that bundle up any non-core dependencies
    id("com.gradleup.shadow") version "8.3.5"
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
    // Add the maven-publish plugin
    id("maven-publish")
}

// TODO: Configure your extension here (please change the defaults!)
qupathExtension {
    name = "qupath-extension-warpy"
    group = "qupath.ext.warpy"
    version = "0.4.1-SNAPSHOT"
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

// Publishing configuration for SciJava Maven
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Warpy QuPath Extension")
                description.set("QuPath extension that supports spline transformations.")
                url.set("https://github.com/BIOP/qupath-extension-warpy")

                licenses {
                    license {
                        name.set("BSD-3-Clause")
                        url.set("https://opensource.org/licenses/BSD-3-Clause")
                    }
                }

                developers {
                    developer {
                        id.set("NicoKiaru")
                        name.set("Nicolas Chiaruttini")
                        email.set("nicolas.chiaruttini@epfl.ch")
                    }
                    developer {
                        id.set("iwbh15")
                        name.set("Peter Haub")
                        email.set("peterhaub@web.de")
                    }
                    developer {
                        id.set("lacan")
                        name.set("Olivier Burri")
                        email.set("olivier.burri@epfl.ch")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/BIOP/qupath-extension-warpy.git")
                    developerConnection.set("scm:git:ssh://github.com:BIOP/qupath-extension-warpy.git")
                    url.set("https://github.com/BIOP/qupath-extension-warpy")
                }
            }
        }
    }

    repositories {
        maven {
            name = "scijava"
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://maven.scijava.org/content/repositories/snapshots")
            } else {
                uri("https://maven.scijava.org/content/repositories/releases")
            }
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PASS")
            }
        }
    }
}
