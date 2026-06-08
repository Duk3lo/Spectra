plugins {
    java
}

allprojects {
    group = "org.astral.spectra"
    version = "0.0.1"

    repositories {
        mavenCentral()
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "Hytale"
            url = uri("https://maven.hytale.com/release")
        }
    }
}

subprojects {
    pluginManager.apply("java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaExec> {
        jvmArgs(listOf("--enable-native-access=ALL-UNNAMED"))
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:24.1.0")
    }
}