plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.0"
}


group = "org.astral.spectyle"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "Hytale"
        url = uri("https://maven.hytale.com/release")
    }
}

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val lwjglNatives = when {
    osName.contains("win") -> "natives-windows"
    osName.contains("linux") -> {
        if (osArch.contains("arm") || osArch.contains("aarch64")) "natives-linux-arm64" else "natives-linux"
    }
    osName.contains("mac") -> {
        if (osArch.contains("arm") || osArch.contains("aarch64")) "natives-macos-arm64" else "natives-macos"
    }
    else -> "natives-linux"
}

dependencies {
    val lwjglVersion = "3.4.1"

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-stb")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")

    implementation("com.github.wendykierp:JTransforms:3.1")
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("com.hypixel.hytale:Server:+")
}

val nativeAccessArgs = listOf(
    "--enable-native-access=ALL-UNNAMED",
)

tasks.withType<JavaExec> {
    jvmArgs(nativeAccessArgs)
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()

    manifest {
        attributes(
            "Main-Class" to "org.astral.spectyle.Main"
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register<JavaExec>("runMain") {
    group = "application"
    description = "Main test"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.astral.spectyle.Main")

    jvmArgs(nativeAccessArgs)
}