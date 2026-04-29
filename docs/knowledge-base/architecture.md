# Architecture

## Stack

- Kotlin
- Jetpack Compose
- MVVM + Clean Architecture
- Hilt for dependency injection
- Room for local persistence
- Retrofit + OpenAI API
- Google Maps Compose + Places SDK

## Layering

### Domain

- Location: `domain/model`, `domain/repository`, `domain/usecase`
- Contains pure Kotlin models, repository interfaces, and use cases.

### Data

- Location: `data/local`, `data/remote`, `data/repository`
- Handles Room entities and DAOs, remote APIs, and repository implementations.

### Presentation

- Location: `presentation/`
- Compose screens and ViewModels grouped by feature area.

## Important Infrastructure

- Dependency injection modules live under `core/di/`.
- Shared utilities live under `core/util/`.
- `ApproximateCurrencyConverter` supports display-currency conversion in budget screens.
- `MainActivity` hosts a single-activity Compose navigation setup.

## Navigation Model

- Navigation is defined through `NavGraph.kt` and `Screen.kt`.
- Primary user journey:
  - Trip list
  - Trip form
  - Itinerary
  - Feature screens from itinerary: map, budget, packing, AI generate, Ask About Trip, attachments, sync

## Notable UI Structure

- The itinerary screen is the central workflow surface.
- The itinerary uses bottom sheets for manual item entry, place search, and import.
- Active accommodation is shown in a dedicated section above regular day items.
