plugins {
    id("com.gradleup.shadow") version "9.2.0"
}

dependencies {
    val lwjglVersion = "3.4.1"

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("javazoom:jlayer:1.0.1")
    implementation("com.github.wendykierp:JTransforms:3.1")

    listOf(
        "natives-windows",
        "natives-linux",
        "natives-linux-arm64",
        "natives-macos",
        "natives-macos-arm64",
        "natives-windows-arm64"
    ).forEach { n ->
        runtimeOnly("org.lwjgl:lwjgl::$n")
        runtimeOnly("org.lwjgl:lwjgl-openal::$n")
        runtimeOnly("org.lwjgl:lwjgl-stb::$n")
    }

    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks.shadowJar {
    archiveClassifier.set("standalone")
    mergeServiceFiles()

    manifest {
        attributes("Main-Class" to "org.astral.spectra.Main")
    }
}

tasks.register<JavaExec>("runMain") {
    group = "application"
    description = "Ejecuta la UI y el Main del Core"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.astral.spectra.Main")
}