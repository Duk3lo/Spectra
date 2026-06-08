# 🌌 Spectra Audio Engine

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java 25">
  <img src="https://img.shields.io/badge/Engine-OpenAL-blue.svg" alt="OpenAL">
  <img src="https://img.shields.io/badge/Modules-Core%20%7C%20Minecraft%20%7C%20Hytale-green.svg" alt="Modules">
  <img src="https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg" alt="MPL 2.0 License">
</p>

<p align="center">
  <b>Spectra</b> is a high-performance, modular audio engine built on <b>OpenAL</b>. It is designed to work both as a standalone desktop application and as an embedded engine for platforms such as <b>Minecraft</b> and <b>Hytale</b>.
</p>

---

## ✨ Key Features

- 🔊 **Core audio engine** powered by OpenAL through LWJGL
- ⚡ **Real-time FFT analysis**
- 🥁 **Beat detection** for Kick, Snare, and Hat
- 🌐 **Built-in web server** with SSE support
- 🖥️ **Modern desktop UI** using Java Swing
- 🧩 **Modular architecture** for multiple platforms

---

## 🛠️ Requirements

Before running Spectra, make sure you have:

- **Java 25**
- **Gradle** or the **Gradle Wrapper**
- Native library access enabled

Since Spectra uses LWJGL native bindings, run Java with:

```bash
--enable-native-access=ALL-UNNAMED
```

---

## 🚀 Build & Run

### Build all modules

```bash
./gradlew build
```

The generated artifacts will be available in:

```text
spectra-core/build/libs/
spectra-minecraft/build/libs/
spectra-hytale/build/libs/
```

### Run the desktop UI

```bash
./gradlew :spectra-core:runMain
```

This starts:

- the audio engine
- the FFT analyzer
- the web server
- the Swing dashboard

---

## 🌐 Web Visualizer

Spectra includes a local web server that exposes real-time audio data.

### Default URL

```text
http://localhost:8080
```

### Features

- Live spectrum visualization
- Real-time beat detection
- Server-Sent Events (SSE) streaming
- Browser-based monitoring

---

## 🎛️ Control Panel

From the desktop UI, you can adjust in real time:

- FFT precision
- Smoothing factor
- Detection thresholds
- Analysis sensitivity

---

## 📦 Technical Stack

- **LWJGL** — OpenAL and STB
- **JTransforms** — High-performance FFT processing
- **JLayer** — MP3 decoding support
- **Java Swing** — Desktop user interface
- **Gradle Kotlin DSL** — Modern build configuration

---

## 📄 License

This project is licensed under the **MIT License**.

You are free to use, modify, and distribute it as long as the original license notice is preserved.
