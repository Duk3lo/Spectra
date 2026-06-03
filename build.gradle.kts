plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.0"
}

group = "org.astral.spectra"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    /*maven {
    name = "Hytale"
    url = uri("https://maven.hytale.com/release")
    }*/
}

dependencies {
//Audio
val lwjglVersion = "3.4.1"

implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
implementation("org.lwjgl:lwjgl")
implementation("org.lwjgl:lwjgl-openal")
implementation("org.lwjgl:lwjgl-stb")
implementation("javazoom:jlayer:1.0.1")

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

implementation("com.github.wendykierp:JTransforms:3.1")
compileOnly("org.jetbrains:annotations:24.1.0")

//Hytale
//compileOnly("com.hypixel.hytale:Server:+")

//Minecraft
compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
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

sourceSets {
main {
    java {
        exclude("org/astral/spectra/hytale/**")
    }
}
}