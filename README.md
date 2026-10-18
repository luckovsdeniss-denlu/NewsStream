# NewsStream

Android app skeleton (Kotlin, Jetpack Compose, Material 3).

The home screen shows live headlines and social posts merged from many sources, newest first, with a News / Images / Saved switch and a Topics selector. Images shows a grid of the pictures attached to
stories in the selected topics; thumbnails are prefetched as each source arrives and cached in memory and on
disk, so the grid appears instantly. Bookmark any story to keep it in Saved (stored on the device, available offline). Tap a story to read it; SHOW IMAGE loads its picture on demand. The sources button in the top bar picks which sources to show
(saved across restarts; applies to every screen). Stories are grouped into World, Politics,
Business, Technology, Science, Climate, Health, Sports, Culture and General (`data/Topic.kt`: source sections and
feed categories first, then headline keywords, then each source's default topic).

## Loading
Nothing is fetched at launch: the app opens empty and loads news when you tap Load news or refresh.
Sources are fetched at most 10 at a time and each one appears as soon as it arrives (the header shows
progress). The feed renders 30 compact stories at a time and adds the next 30 as you scroll. Switched-off sources
are not downloaded.

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
- **GNews.io** (optional, needs a free key): top headlines in 8 categories, with images. Add the key as the
  `GNEWS_API_KEY` repository secret. The free plan allows ~100 requests/day, so each category is a separate
  source (switch off the ones you don't need) and responses are cached on the device for 30 minutes.
- In-depth (RSS, no key): ProPublica, Bellingcat, The Conversation, Our World in Data, Visual Capitalist,
  Quanta Magazine, IEEE Spectrum, NASA Earth Observatory, Aeon, Smithsonian Magazine, Atlas Obscura, the
  fact-checkers PolitiFact, Snopes and Full Fact, N+1 (Russian science), and Wikipedia's daily "In the news" items
  and featured article (REST API, no key).
- Russian-language news (RSS, no key): BBC Русская служба, DW на русском, Euronews на русском, Meduza,
  Новая газета Европа, The Insider, РБК, Коммерсантъ, Ведомости, Интерфакс, ТАСС, РИА Новости, Лента.ру,
  Фонтанка, Хабр, Google Новости; plus GNews Russian headlines when a GNews key is set. The sources sheet has an
  All / English / Русский quick filter, and topic keywords cover Russian too.
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
CI builds a debug APK on request: Actions → Build APK → Run workflow (see `.github/workflows/build-apk.yml`).
Locally: open in Android Studio, or run `gradle :app:assembleDebug`.

## Versioning
Every CI build gets a new version automatically: it builds `versionName = 0.1.<run number>` and
`versionCode = <run number>`, and uploads `NewsStream-0.1.<run number>.apk`. To bump major/minor, change
`baseVersion` in `app/build.gradle.kts` and the `0.1` prefix in the workflow.

## Roadmap
- Images, favourites, offline caching
