# NewsStream

Android news streaming platform: browse news by category and watch live/on-demand video (HLS) inside articles.

**Stack:** Kotlin, Jetpack Compose (Material 3), Navigation Compose, Media3 ExoPlayer, Coil.

## Structure
- `data/` – models and `NewsRepository` (currently `SampleNewsRepository` with demo content)
- `ui/home` – category tabs + article feed
- `ui/detail` – article page with video player

## Build
CI builds a debug APK on every push (see `.github/workflows/build-apk.yml`).
Locally: open in Android Studio, or run `gradle :app:assembleDebug`.

## Versioning
Every push to `main` gets a new version automatically: CI builds `versionName = 0.1.<run number>` and
`versionCode = <run number>`, and uploads `NewsStream-0.1.<run number>.apk`. To bump major/minor, change
`baseVersion` in `app/build.gradle.kts` and the `0.1` prefix in the workflow.

## Roadmap
- Real backend / news API via Retrofit
- Dependency injection (Hilt), offline cache (Room)
- Search, bookmarks, push notifications, live-stream schedule
