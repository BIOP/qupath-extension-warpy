plugins {
    `java-library`
    // To create a shadow/fat jar, including dependencies
    id("com.github.johnrengelman.shadow") version "7.0.0"
    // To manage included native libraries
    //  id 'org.bytedeco.gradle-javacpp-platform' version '1.5.6'
    `maven-publish`
}

repositories {
    // Use this only for local development!
    //  mavenLocal()

    mavenCentral()

    maven("https://maven.scijava.org/content/repositories/releases")

    maven("https://maven.scijava.org/content/repositories/snapshots")
}

ext.set("moduleName", "qupath.extension.warpy")
description = "QuPath extension to use Warpy"

group = "ch.epfl.biop"
// artifact = "qupath-extension-warpy"
version = "0.1.1-SNAPSHOT"

dependencies {
    val qupathVersion = "0.3.0" // For now

    shadow("io.github.qupath:qupath-gui-fx:${qupathVersion}")
    shadow("org.slf4j:slf4j-api:1.7.30")
    implementation("commons-io:commons-io:2.11.0")
    implementation("net.imglib2:imglib2-realtransform:3.1.1")

    testImplementation("io.github.qupath:qupath-gui-fx:${qupathVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

//processResources {
//  from ("${projectDir}/LICENSE.txt") {
//    into "licenses/"
//  }
//}

tasks.register<Copy>("copyDependencies") {
    description = "Copy dependencies into the build directory for use elsewhere"
    group = "QuPath"

    from(configurations.default)
    into("build/libs")
}

/*
 * Ensure Java 11 compatibility
 */
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    if ("sources" in project.properties)
        withSourcesJar()
    if ("javadocs" in project.properties)
        withJavadocJar()
}

/*
 * Create javadocs for all modules/packages in one place.
 * Use -PstrictJavadoc=true to fail on error with doclint (which is rather strict).
 */
//def strictJavadoc = findProperty("strictJavadoc")
//if (!strictJavadoc) {
//    tasks.withType(Javadoc) {
//        options.addStringOption("Xdoclint:none", "-quiet")
//    }
//}

/*
 * Avoid 'Entry .gitkeep is a duplicate but no duplicate handling strategy has been set.'
 * when using withSourcesJar()
 */
//tasks.withType(org.gradle.jvm.tasks.Jar) {
//    duplicatesStrategy = DuplicatesStrategy.INCLUDE
//}

tasks.test { useJUnitPlatform() }

//------------------------------------------
publishing {
    publications.create<MavenPublication>("maven") {
        repositories {
            maven {
                name = "scijavaPublic"
                url = uri("https://maven.scijava.org/content/groups/public")
            }
        }
//        from components . java
//
//                pom {
//                    name = rootProject.name
//                    description = rootProject.description
//                    url = "https://github.com/BIOP/qupath-extension-warpy"
//                    properties["inceptionYear"] = "2021"
//                    organization {
//                        name = rootProject.name
//                        url = "https://github.com/BIOP"
//                    }
//                    licenses {
//                        license {
//                            name = "GNU Lesser General Public License v3+"
//                            url = "https://www.gnu.org/licenses/lgpl.html"
//                            distribution = "repo"
//                        }
//                    }
//                    developers {
//                        developer {
//                            id = "nicokiaru"
//                            name = "Nicolas Chiaruttini"
//                            url = "https://github.com/NicoKiaru"
//                            roles.addAll("founder", "lead", "developer", "debugger", "reviewer", "support", "maintainer")
//                        }
//                    }
//                    contributors {
//                        contributor {
//                            name = "Olivier Burri"
//                            url = "https://people.epfl.ch/olivier.burri"
//                            properties["id"] = "lacan"
//                        }
//                    }
//                    mailingLists { mailingList { name = "none" } }
//                    scm {
//                        connection.set("scm:git:git://github.com/BIOP/qupath-extension-warpy")
//                        developerConnection.set("scm:git:git@github.com:BIOP/qupath-extension-warpy")
//                        tag = "HEAD" // TODO differs from version
//                        url = "https://github.com/BIOP/qupath-extension-warpy"
//                    }
//                    issueManagement {
//                        system.set("GitHub Issues")
//                        url = "https://github.com/BIOP/qupath-extension-warpy/issues"
//                    }
//                    ciManagement {
//                        system.set("GitHub Actions")
//                        url.set("https://github.com/BIOP/qupath-extension-warpy/actions/")
//                    }
//                    distributionManagement {
//                        // https://stackoverflow.com/a/21760035/1047713
//                        //                    <snapshotRepository>
//                        //                        <id>ossrh</id>
//                        //                        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
//                        //                    </snapshotRepository>
//                        //                    <repository>
//                        //                        <id>ossrh</id>
//                        //                        <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
//                        //                    </repository>
//                    }
//                    //                artifact("${rootProject.name}-${rootProject.version}-sources.jar")
//                    //                artifact("${rootProject.name}-${rootProject.version}-javadoc.jar")
//                }
    }
}