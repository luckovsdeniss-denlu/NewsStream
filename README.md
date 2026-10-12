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

## Roadmap
- Real backend / news API via Retrofit
- Dependency injection (Hilt), offline cache (Room)
- Search, bookmarks, push notifications, live-stream schedule
