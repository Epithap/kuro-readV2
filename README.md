<p align="center">
  <img src="app_icon.png" alt="KuroRead V2 Logo" width="120" style="border-radius: 24px; box-shadow: 0 8px 24px rgba(0,0,0,0.6);" />
</p>

<h1 align="center">📖 KuroRead V2</h1>

<p align="center">
  <strong>100% Pure Native Kotlin & Jetpack Compose Light Novel & Web Novel Reader for Android.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2014%2B-E53935?style=for-the-badge&logo=android" alt="Platform" />
  <img src="https://img.shields.io/badge/Version-v1.0.0-blue?style=for-the-badge" alt="Version" />
  <img src="https://img.shields.io/badge/Language-Kotlin%201.9-orange?style=for-the-badge&logo=kotlin" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-green?style=for-the-badge&logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/License-MIT-purple?style=for-the-badge" alt="License" />
</p>

---

## 🚀 Overview

**KuroRead V2** is a complete, 100% pure native Kotlin and Jetpack Compose rebuild of the KuroRead Light Novel app for Android. Built with ZERO WebViews, KuroRead V2 features a state-based architecture, modern dark matte editorial aesthetic, native JSoup web scraping engine for multi-source novel updates, and a highly customizable reader interface.

---

## 📱 Download APK

- 📥 **[Download KuroRead V2 v1.0.0 via GitHub Releases](https://github.com/Epithap/kuro-readV2/releases/latest)** *(Pure Native Kotlin & Jetpack Compose Release)*
- 📦 **[Direct APK File (`KuroReadV2-v1.0.0.apk`)](KuroReadV2-v1.0.0.apk)**

---

## ✨ Key Features

- **⚡ 100% Native Jetpack Compose UI**:
  - Built from the ground up using modern Material 3 and Jetpack Compose (`com.kuroread.app.v2`).
  - Zero WebViews — pure native performance, instant rendering, and fluid transitions.
- **🎨 Dark Matte Editorial Design System**:
  - Minimalist matte dark palette (`#161616` background, `#212123` containers, `#D92D3A` crimson accents).
  - High-contrast typography with custom Serif & Sans-Serif options designed for zero strain reading.
- **📚 Interactive HomeScreen & Carousel**:
  - Modern horizontal swipeable recommendation carousel (`WeeklyCarousel`) with animated pager dots.
  - Real-time search filter for titles, authors, and genres.
- **📖 Feature-Rich Native Reader (`ReaderScreen`)**:
  - Adjustable font sizes (`A-` / `A+` controls).
  - Background Theme Mode Picker: **Dark** (`#161616`), **AMOLED Black** (`#000000`), and **Warm Sepia** (`#241F1C`).
  - Typography Toggle (**Serif** vs **Sans-Serif**).
  - Chapter progress bar with instant Previous/Next chapter navigation buttons.
- **🕷️ Mihon-Style Live Web Scraping Engine (`WebNovelScraper.kt`)**:
  - Powered by JSoup for live web scraping from **SakuraNovel** (WP REST API), **NovelRingan**, and **Meionovel**.
  - Direct JSON & object mapping for instant chapter fetching.
- **🗂️ Collection, History & Profile Screens**:
  - Dedicated bottom navigation tabs for shelf management, automatic reading history tracking, and local stats.

---

## 🏗️ Architecture & Project Structure

```
KuroReadV2/
├── 📱 KuroReadV2-v1.0.0.apk           # Compiled native release APK
├── 🖼️ app_icon.png                     # KuroRead V2 App Icon
├── 📄 README.md                        # Documentation
└── 🤖 android_studio_project/         # Native Android Project
    ├── app/
    │   ├── build.gradle.kts           # namespace = "com.kuroread.app.v2"
    │   └── src/main/
    │       ├── AndroidManifest.xml
    │       └── java/com/kuroread/app/v2/
    │           ├── MainActivity.kt        # Entry point with State-based Navigation
    │           ├── model/
    │           │   └── Novel.kt           # Novel, Chapter & Status data models
    │           ├── data/
    │           │   └── WebNovelScraper.kt # JSoup multi-source live scraper engine
    │           └── ui/
    │               ├── theme/
    │               │   └── Color.kt       # Matte Dark color palette & Reader themes
    │               ├── components/
    │               │   ├── WeeklyCarousel.kt # Swipeable recommendation pager
    │               │   └── NovelListItem.kt  # Novel list card
    │               └── screens/
    │                   ├── HomeScreen.kt         # Explore & search screen
    │                   ├── NovelDetailScreen.kt  # Metadata & chapter list
    │                   ├── ReaderScreen.kt       # Native reading engine
    │                   ├── CollectionScreen.kt   # Shelf & bookmarks
    │                   ├── HistoryScreen.kt      # Reading history
    │                   └── ProfileScreen.kt      # Reader stats & settings
    └── build.gradle.kts
```

---

## 🛠️ Build & Installation

### Requirements
- Android Studio Jellyfish / Ladybug or newer
- JDK 17
- Android SDK 34 (Minimum SDK 24)

### Building via Gradle Command Line
Navigate to `android_studio_project` and run:

```bash
cd android_studio_project
./gradlew :app:assembleDebug
```

The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9
- **UI Framework**: Jetpack Compose (Compose BOM `2024.06.00`, Material 3, Material Icons Extended)
- **Image Loading**: Coil Compose (`2.6.0`)
- **HTML Parser & Scraper**: JSoup (`1.17.2`)
- **Design Aesthetic**: Solid Editorial Dark Matte

---

## 📄 License

Distributed under the MIT License.
