# NewsStream

Android app skeleton (Kotlin, Jetpack Compose, Material 3).

The home screen shows live headlines merged from several news sources, newest first; tap one to read it.

## News sources
Configured in `data/NewsRepository.kt`:
- **The Guardian** Content API (full article text). Uses the public `test` key by default, which is
  rate-limited. For a real key, register at https://open-platform.theguardian.com/access/ and add it as the
  `GUARDIAN_API_KEY` repository secret (CI) or Gradle property (local builds).
- RSS/Atom feeds (headline + summary, link to the full story), no key needed:
  - World: BBC News, NPR, Al Jazeera, The New York Times, Sky News, Deutsche Welle, France 24, CBS News
  - Business: CNBC
  - Technology and science: Ars Technica, TechCrunch, The Verge, NASA

Add a source by implementing `NewsSource` (or adding another `RssNewsSource` for any RSS 2.0, RSS 1.0 or Atom feed) and listing it in `NewsRepository`.

## Build
CI builds a debug APK on every push (see `.github/workflows/build-apk.yml`).
Locally: open in Android Studio, or run `gradle :app:assembleDebug`.

## Versioning
Every push to `main` gets a new version automatically: CI builds `versionName = 0.1.<run number>` and
`versionCode = <run number>`, and uploads `NewsStream-0.1.<run number>.apk`. To bump major/minor, change
`baseVersion` in `app/build.gradle.kts` and the `0.1` prefix in the workflow.

## Roadmap
- Images, categories, offline caching
