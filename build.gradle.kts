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
version = "0.2.1"

dependencies {
    val qupathVersion = "0.3.0" // For now

    shadow("io.github.qupath:qupath-gui-fx:$qupathVersion")
    shadow("org.slf4j:slf4j-api:1.7.30")
    implementation("commons-io:commons-io:2.11.0")
    implementation("net.imglib2:imglib2-realtransform:3.1.2")

    testImplementation("io.github.qupath:qupath-gui-fx:$qupathVersion")
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

// Skip fat jar publication
components.java.withVariantsFromConfiguration(configurations.shadowRuntimeElements.get()) { skip() }

publishing {
    publications.create<MavenPublication>("maven") {
        from(components.java)
    }
    repositories {
        maven {
            name = "scijava"
            //credentials(PasswordCredentials::class)
            url = if (version.toString().endsWith("SNAPSHOT"))
                 uri("https://maven.scijava.org/content/repositories/snapshots")
            else uri("https://maven.scijava.org/content/repositories/releases")
        }
    }
}

val SoftwareComponentContainer.java
    get() = components.getByName("java") as AdhocComponentWithVariants