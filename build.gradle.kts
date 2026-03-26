plugins {
    id("java")
    application // Para poder usar 'gradle run'
}

group = "org.astral.audio"
version = "1.0-SNAPSHOT"

// Lógica de detección de sistema (Imprescindible para los drivers nativos)
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

repositories {
    mavenCentral()
}

dependencies {
    val lwjglVersion = "3.3.4"

    // 1. El BOM asegura que todas las piezas de LWJGL sean de la misma versión
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    // 2. Las librerías de código (Interfaces de Java)
    implementation("org.lwjgl:lwjgl")        // Núcleo
    implementation("org.lwjgl:lwjgl-openal") // Audio
    implementation("org.lwjgl:lwjgl-stb")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
}

// Esto configura CUALQUIER tarea que ejecute Java (incluyendo 'gradle run')
tasks.withType<JavaExec> {
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        "-Dorg.lwjgl.util.NoChecks=true",
        "-Dorg.lwjgl.system.allocator=system",
        "-XX:+IgnoreUnrecognizedVMOptions"
    )
}


tasks.register<Exec>("runExternal") {
    group = "application"
    dependsOn("classes")
    val classpath = sourceSets["main"].runtimeClasspath.asPath
    val mainClass = "org.astral.audio.Main"
    val jvmArguments = listOf(
        "java",
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "-Dorg.lwjgl.util.NoChecks=true",
        "-Dorg.lwjgl.system.allocator=system",
        "-cp", classpath,
        mainClass
    )

    commandLine("gnome-terminal", "--", *jvmArguments.toTypedArray())
}
application {
    mainClass.set("org.astral.audio.Main")
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        "-Dorg.lwjgl.util.NoChecks=true",
        "-Dorg.lwjgl.system.allocator=system" // <--- Añadido
    )
}