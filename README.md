<div align="center">

# 🌊 NewsPlatform

**The whole world's news in one stream, and the statistics to make sense of it.**

200+ news sites, social feeds and YouTube channels in English and Russian, merged into one live
feed, with on-device analytics that show what's trending, who reported it first and how outlets differ.

![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-3DDC84?logo=android&logoColor=white)
![Sources](https://img.shields.io/badge/sources-200%2B-0B2347)
![Languages](https://img.shields.io/badge/languages-EN%20%7C%20RU-1E88E5)
![No tracking](https://img.shields.io/badge/analytics-100%25%20on--device-00897B)

</div>

---

## ✨ Highlights

| | |
|---|---|
| 📰 **One stream, many voices** | World news, politics, business, tech, science, climate, health, sports and culture: newest first, sorted into topics automatically. |
| 📊 **Insights** | 13 statistical analyses on everything you've loaded: bursts, trends, story clusters, source similarity, tone with margins of error and more. |
| 🔎 **Word deep-dives** | Tap any trending word to see who covers it, who had it first, how fast it spread, its momentum, tone, phrases and pictures. |
| 🖼️ **Images** | Every story's picture in a grid, prefetched and cached so it appears instantly. |
| ▶️ **YouTube** | 26 news and science channels in coloured groups, played right inside the app. |
| 💬 **Social** | Reddit, Hacker News, Lobsters, Lemmy, Mastodon, Bluesky, Telegram and VK, with likes, comments and shares. |
| 🔖 **Saved** | Bookmark anything; kept on the device and readable offline. |
| 📐 **Four sizes** | One story per screen, or 2, 3 or 5 per screen. Every size fills the card with as much text as fits. |
| 🎲 **Random 5** | Tap the shuffle button in News to swap in 5 random news sources you haven't got on yet. |
| 🌙 **Deep Ocean theme** | A dark navy design with colour-coded topics. |

## 📊 Insights

Tap the chart icon in the top bar. Everything is computed **on the phone**: no server, no account, no tracking.

| Tab | What it shows | Methods |
|---|---|---|
| **Overview** | Totals, stories per topic and source, big stories, trending words, a 24-hour timeline, most-engaged posts, languages | Counts, cross-source ranking |
| **Trends** | Words bursting right now, what's rising and fading, words that are new this period, words that travel together | z-score, 3-hour moving average, growth rate, log-likelihood G² (p < 0.01), normalised PMI |
| **Stories** | The same story told by many outlets, how evenly the news is spread, the biggest story's share, most exclusive sources | Jaccard similarity + union-find clustering, Shannon evenness |
| **Sources** | Each outlet's and topic's signature words, the most alike and most independent sources, the fastest reporters, tone by topic and source | TF-IDF, cosine similarity, median pickup delay, net tone ± 95% margin |
| **Social** | The best post on each platform, engagement by topic and time of day, whether older posts gather more | Within-platform ranks, medians, Spearman ρ |

Every trending word, big story and phrase opens a **word page** with its momentum and burst score, the
order in which sources picked it up, coverage by source and topic, related words, tone, social
engagement, a picture strip, and every matching story grouped by source.

## 🗞️ Sources

No key is needed for almost everything. All sources start switched off; pick yours in the sources sheet,
which is grouped by topic, with World split into regions.

- **World**: Reuters, AP, BBC, The Guardian, Al Jazeera, NYT, DW, France 24, Euronews, Financial Times, UN News, SCMP, The Japan Times, Kyiv Independent, The Moscow Times and more
- **Topics**: Politico, The Economist, CNBC, Ars Technica, The Verge, Wired, Nature, NASA, New Scientist, Carbon Brief, STAT News, ESPN, Variety…
- **In-depth & fact-checking**: ProPublica, Bellingcat, The Conversation, Our World in Data, Quanta, IEEE Spectrum, Aeon, PolitiFact, Snopes, Full Fact, Wikipedia
- **На русском**: BBC Русская служба, DW, Meduza, Новая газета Европа, The Insider, РБК, Коммерсантъ, Ведомости, Интерфакс, ТАСС, РИА Новости, Хабр, N+1…
- **Social**: Reddit, Hacker News, Lobsters, Lemmy, Mastodon, Bluesky, 11 Telegram channels, VK pages
- **YouTube**: BBC, Al Jazeera, DW, Reuters, Bloomberg, CNN, NASA, Veritasium, Kurzgesagt, TED, Vox, MKBHD and more
- **Optional APIs**: GNews.io headlines in 8 categories plus Russian, and The Guardian's full-text API

Add your own by listing an `RssNewsSource` (RSS 2.0, RSS 1.0 or Atom) or implementing `NewsSource` in
[`data/NewsRepository.kt`](app/src/main/java/com/newsstream/data/NewsRepository.kt).

X/Twitter, Instagram and Facebook aren't included: their APIs need paid or approved access.

## ⚡ Fast by design

- Nothing loads at launch; news loads when you ask for it, and videos only when you open YouTube.
- Up to 10 sources are fetched at once, and each one appears the moment it arrives.
- The feed renders 30 stories at a time; images are sized to the view and prefetched ahead of scrolling.
- Heavy statistics run on a background thread.

## 🔑 Optional keys

Add these as GitHub repository secrets (or Gradle properties for local builds). Everything works without them.

| Secret | Unlocks |
|---|---|
| `GUARDIAN_API_KEY` | The Guardian's full article text without the shared test key's rate limit ([get a key](https://open-platform.theguardian.com/access/)) |
| `GNEWS_API_KEY` | GNews.io headlines in 8 categories plus Russian (free plan, responses cached for 30 minutes) |
| `REDDIT_CLIENT_ID` | Reddit scores and comment counts (an "installed app" from reddit.com/prefs/apps) |
| `VK_SERVICE_TOKEN` | VK public pages (a free service token) |

## 🛠️ Build

```bash
gradle :app:assembleDebug        # or open the project in Android Studio
```

CI builds a debug APK on request: **Actions → Build APK → Run workflow**. Each build is versioned
`0.1.<run number>` and uploaded as `NewsPlatform-0.1.<run number>.apk`.

**Stack:** Kotlin · Jetpack Compose · Material 3 · Coroutines · Coil · `HttpURLConnection`, `XmlPullParser`
and `org.json` for networking and parsing. No other dependencies.

## 🗂️ Project layout

```
app/src/main/java/com/newsstream/
├── data/            Article model, topics, repository, saved stories, preferences
│   ├── source/      One class per source type: RSS, Guardian, GNews, Reddit, Mastodon, YouTube, Telegram…
│   ├── Insights.kt  Overview numbers
│   ├── Statistics.kt  The statistical analyses
│   └── TermAnalysis.kt  Word deep-dives
└── ui/              Compose screens: home, article, images, YouTube player, insights, theme
```
