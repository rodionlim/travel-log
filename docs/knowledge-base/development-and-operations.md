# Development and Operations

## Local Setup

- Android Studio and the Android SDK must be installed on the machine running Gradle.
- `local.properties` must define:
  - `sdk.dir`
  - `MAPS_API_KEY`
- AI features also require an OpenAI API key entered at runtime through the app's Settings screen.

## Build Commands

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
./gradlew connectedAndroidTest
./gradlew clean
```

## Validation Expectations

- Use `./gradlew :app:compileDebugKotlin` for fast Kotlin compile validation on local feature changes.
- Use `./gradlew test --stacktrace` for broader unit-test validation.
- CI also runs unit tests and debug APK assembly through GitHub Actions.

## CI Pipeline

- Workflow file: `.github/workflows/android.yml`
- Workflow name: `Android CI`
- Main CI responsibilities:
  - run unit tests
  - build the debug APK
  - upload the debug APK artifact

## Release Flow

- Tag pushes like `v1.0.0` trigger the release path.
- The release workflow builds the release APK and can sign it when keystore secrets are configured.
- Release docs and signing details live in [Releasing](../RELEASING.md).

## Practical Caveats

- The manifest `MAPS_API_KEY` is required for the map screen even if the runtime settings key is present.
- The OpenAI parsing model matters most for image/PDF import fidelity.
- Heavy image uploads and aggressive PDF rasterization increase token usage compared with text-only flows.
