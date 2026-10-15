# NewsStream

Android app skeleton (Kotlin, Jetpack Compose, Material 3).

The home screen shows live headlines and social posts merged from many sources, newest first, with ALL / NEWS / SOCIAL filters; tap one to read it. The sources button in the top bar picks which sources to show
(saved across restarts; applies to every screen). The **Topics** tab groups everything into World, Politics,
Business, Technology, Science, Climate, Health, Sports, Culture and General (`data/Topic.kt`: source sections and
feed categories first, then headline keywords, then each source's default topic).

## News sources
Configured in `data/NewsRepository.kt`:
- **The Guardian** Content API (full article text). Uses the public `test` key by default, which is
  rate-limited. For a real key, register at https://open-platform.theguardian.com/access/ and add it as the
  `GUARDIAN_API_KEY` repository secret (CI) or Gradle property (local builds).
- RSS/Atom feeds (headline + summary, link to the full story), no key needed:
  - World: BBC News, NPR, Al Jazeera, The New York Times, Sky News, Deutsche Welle, France 24, CBS News,
    The Independent, Euronews, UN News, South China Morning Post, Times of India, The Japan Times,
    ABC News (Australia), Google News
  - Politics: Politico, The Hill
  - Business: CNBC, The Economist, Fortune
  - Technology: Ars Technica, TechCrunch, The Verge, Wired, MIT Technology Review, Engadget
  - Science: NASA, ScienceDaily, New Scientist, Space.com, Phys.org
  - Climate: Carbon Brief, Inside Climate News
  - Health: WHO, STAT News
  - Sports: ESPN, BBC Sport, Sky Sports
  - Culture: Variety, Rolling Stone
- Social (posts with author and likes/comments/shares), no key needed:
  - Reddit (r/worldnews, r/news, r/technology). Reddit blocks anonymous API access, so by default these use the
    public subreddit feeds (headlines only). For scores and comment counts, create an "installed app" at
    https://www.reddit.com/prefs/apps and add its client ID as the `REDDIT_CLIENT_ID` repository secret.
  - Hacker News front page via the Algolia HN API
  - Lobsters (lobste.rs) hottest stories
  - Lemmy (lemmy.world) hot posts via its public API
  - Mastodon trending posts and trending news links (mastodon.social)
  - Bluesky "Discover" feed via the public AppView API

X/Twitter, Instagram and Facebook are not included: their APIs require paid or approved access.

Add a source by implementing `NewsSource` (or adding another `RssNewsSource` for any RSS 2.0, RSS 1.0 or Atom feed) and listing it in `NewsRepository`.

## Build
CI builds a debug APK on every push (see `.github/workflows/build-apk.yml`).
Locally: open in Android Studio, or run `gradle :app:assembleDebug`.

## Versioning
Every push to `main` gets a new version automatically: CI builds `versionName = 0.1.<run number>` and
`versionCode = <run number>`, and uploads `NewsStream-0.1.<run number>.apk`. To bump major/minor, change
`baseVersion` in `app/build.gradle.kts` and the `0.1` prefix in the workflow.

## Roadmap
- Images, favourites, offline caching
