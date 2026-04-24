# Wanderlog

An offline-first Android travel planner. Plan trips, build day-by-day itineraries, track expenses, manage packing lists, and import booking confirmations using AI.

## Features

- **Trip management** — create and manage trips with destination, dates, and duration
- **Itinerary builder** — day-by-day items with drag-to-reorder and swipe-to-delete
- **AI itinerary generation** — describe your trip and get a full itinerary via GPT-4o
- **File import** — share booking emails or upload PDFs/images; AI parses them into itinerary items
- **Attachments vault** — attach markdown notes and files to any trip
- **Map view** — see all stops on a map with a connecting polyline
- **Budget tracker** — log and categorise trip expenses
- **Packing list** — per-trip checklist
- **Google Places autocomplete** — search and pin locations when adding itinerary items

## Tech Stack

Kotlin · Jetpack Compose · MVVM · Clean Architecture · Hilt · Room · Retrofit · OpenAI API · Google Maps & Places

## Prerequisites

1. **Android Studio** (Hedgehog or later) on Windows with Android SDK installed
2. `local.properties` in the project root (copy from template):
   ```
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=<your Google Maps API key>
   ```
3. At runtime, open **Settings** in the app and enter your **OpenAI API key**

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Unit tests
./gradlew test

# Clean
./gradlew clean
```

Open `app/build/outputs/apk/` for the output APK.

## Releasing

Tag a commit to trigger the GitHub Actions release pipeline:

```bash
git tag v1.0.0
git push origin v1.0.0
```

See [RELEASING.md](RELEASING.md) for signing setup and CI details.

## Architecture

Three-layer Clean Architecture:

```
domain/     — models, repository interfaces, use cases (pure Kotlin)
data/       — Room DB, Retrofit/OpenAI, repository implementations
presentation/ — ViewModels + Compose screens
```

DI via Hilt. Single-activity navigation with Compose Navigation.
