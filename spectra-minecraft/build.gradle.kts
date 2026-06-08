plugins {
    id("com.gradleup.shadow") version "9.2.0"
}

dependencies {
    implementation(project(":spectra-core"))
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

tasks.shadowJar {
    archiveBaseName.set("Spectra-Plugin")
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("org/astral/spectra/Main.class")
    exclude("org/astral/spectra/ui/**")
    exclude("musics/**")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}