plugins {
    // To optionally create a shadow/fat jar that bundle up any non-core dependencies
    id("com.gradleup.shadow") version "8.3.5"
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
}

// TODO: Configure your extension here (please change the defaults!)
qupathExtension {
    name = "qupath-extension-warpy"
    group = "io.github.BIOP"
    version = "0.4.1-SNAPSHOT"
    description = "Warpy - QuPath extension that supports spline transformations."
    automaticModule = "io.github.BIOP.extension.warpy"
}

// TODO: Define your dependencies here
dependencies {
    implementation(libs.bundles.qupath)
    implementation(libs.qupath.fxtras)
    implementation("commons-io:commons-io:2.15.0")
    implementation("net.imglib2:imglib2-realtransform:3.1.2")

}