plugins {
    id("com.gradleup.shadow") version "9.2.0"
}

dependencies {
    implementation(project(":spectra-core"))
    compileOnly("com.hypixel.hytale:Server:+")
}

tasks.shadowJar {
    archiveBaseName.set("Spectra-Hytale")
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("org/astral/spectra/Main.class")
    exclude("org/astral/spectra/ui/**")
    exclude("musics/**")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}