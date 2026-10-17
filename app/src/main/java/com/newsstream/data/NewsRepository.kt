package com.newsstream.data

import com.newsstream.BuildConfig
import com.newsstream.data.source.BlueskySource
import com.newsstream.data.source.GNewsSource
import com.newsstream.data.source.GoogleNewsSource
import com.newsstream.data.source.GuardianNewsSource
import com.newsstream.data.source.HackerNewsSource
import com.newsstream.data.source.LemmySource
import com.newsstream.data.source.LobstersSource
import com.newsstream.data.source.MastodonTrendingLinksSource
import com.newsstream.data.source.MastodonTrendingPostsSource
import com.newsstream.data.source.NewsSource
import com.newsstream.data.source.RedditSource
import com.newsstream.data.source.ResponseCache
import com.newsstream.data.source.RssNewsSource
import com.newsstream.data.source.WikipediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

data class SourceResult(val name: String, val articles: Result<List<Article>>)

data class SourceInfo(val name: String, val kind: ContentKind, val category: Topic, val language: String)

class NewsRepository(cacheDir: File) {
    private val sources: List<NewsSource> = listOf(
        // World
        GuardianNewsSource(BuildConfig.GUARDIAN_API_KEY),
        RssNewsSource("BBC News", "https://feeds.bbci.co.uk/news/world/rss.xml"),
        RssNewsSource("NPR", "https://feeds.npr.org/1001/rss.xml"),
        RssNewsSource("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml"),
        RssNewsSource("The New York Times", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml"),
        RssNewsSource("Sky News", "https://feeds.skynews.com/feeds/rss/world.xml"),
        RssNewsSource("Deutsche Welle", "https://rss.dw.com/rdf/rss-en-all"),
        RssNewsSource("France 24", "https://www.france24.com/en/rss"),
        RssNewsSource("CBS News", "https://www.cbsnews.com/latest/rss/world"),
        RssNewsSource("The Independent", "https://www.independent.co.uk/news/world/rss"),
        RssNewsSource("Euronews", "https://www.euronews.com/rss"),
        RssNewsSource("UN News", "https://news.un.org/feed/subscribe/en/news/all/rss.xml"),
        RssNewsSource("South China Morning Post", "https://www.scmp.com/rss/91/feed"),
        RssNewsSource("Times of India", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms"),
        RssNewsSource("The Japan Times", "https://www.japantimes.co.jp/feed/"),
        RssNewsSource("ABC News (Australia)", "https://www.abc.net.au/news/feed/51120/rss.xml"),
        GoogleNewsSource(),
        // Politics
        RssNewsSource("Politico", "https://rss.politico.com/politics-news.xml", Topic.POLITICS),
        RssNewsSource("The Hill", "https://thehill.com/feed/", Topic.POLITICS),
        // Business
        RssNewsSource("CNBC", "https://www.cnbc.com/id/100003114/device/rss/rss.html", Topic.BUSINESS),
        RssNewsSource("The Economist", "https://www.economist.com/latest/rss.xml", Topic.BUSINESS),
        RssNewsSource("Fortune", "https://fortune.com/feed/", Topic.BUSINESS),
        // Technology
        RssNewsSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", Topic.TECH),
        RssNewsSource("TechCrunch", "https://techcrunch.com/feed/", Topic.TECH),
        RssNewsSource("The Verge", "https://www.theverge.com/rss/index.xml", Topic.TECH),
        RssNewsSource("Wired", "https://www.wired.com/feed/rss", Topic.TECH),
        RssNewsSource("MIT Technology Review", "https://www.technologyreview.com/feed/", Topic.TECH),
        RssNewsSource("Engadget", "https://www.engadget.com/rss.xml", Topic.TECH),
        // Science
        RssNewsSource("NASA", "https://www.nasa.gov/news-release/feed/", Topic.SCIENCE),
        RssNewsSource("ScienceDaily", "https://www.sciencedaily.com/rss/all.xml", Topic.SCIENCE),
        RssNewsSource("New Scientist", "https://www.newscientist.com/feed/home/", Topic.SCIENCE),
        RssNewsSource("Space.com", "https://www.space.com/feeds/all", Topic.SCIENCE),
        RssNewsSource("Phys.org", "https://phys.org/rss-feed/", Topic.SCIENCE),
        // Climate
        RssNewsSource("Carbon Brief", "https://www.carbonbrief.org/feed/", Topic.CLIMATE),
        RssNewsSource("Inside Climate News", "https://insideclimatenews.org/feed/", Topic.CLIMATE),
        // Health
        RssNewsSource("WHO", "https://www.who.int/rss-feeds/news-english.xml", Topic.HEALTH),
        RssNewsSource("STAT News", "https://www.statnews.com/feed/", Topic.HEALTH),
        // Sports
        RssNewsSource("ESPN", "https://www.espn.com/espn/rss/news", Topic.SPORTS),
        RssNewsSource("BBC Sport", "https://feeds.bbci.co.uk/sport/rss.xml", Topic.SPORTS),
        RssNewsSource("Sky Sports", "https://www.skysports.com/rss/12040", Topic.SPORTS),
        // Culture
        RssNewsSource("Variety", "https://variety.com/feed/", Topic.CULTURE),
        RssNewsSource("Rolling Stone", "https://www.rollingstone.com/feed/", Topic.CULTURE),
        // Russian-language news
        RssNewsSource("BBC Русская служба", "https://feeds.bbci.co.uk/russian/rss.xml", language = "ru"),
        RssNewsSource("DW на русском", "https://rss.dw.com/rdf/rss-ru-all", language = "ru"),
        RssNewsSource("Euronews на русском", "https://ru.euronews.com/rss", language = "ru"),
        RssNewsSource("Meduza", "https://meduza.io/rss/all", language = "ru"),
        RssNewsSource("Новая газета Европа", "https://novayagazeta.eu/feed/rss", language = "ru"),
        RssNewsSource("The Insider", "https://theins.ru/feed", Topic.POLITICS, language = "ru"),
        RssNewsSource("РБК", "https://rssexport.rbc.ru/rbcnews/news/30/full.rss", Topic.BUSINESS, language = "ru"),
        RssNewsSource("Коммерсантъ", "https://www.kommersant.ru/RSS/main.xml", language = "ru"),
        RssNewsSource("Ведомости", "https://www.vedomosti.ru/rss/news", Topic.BUSINESS, language = "ru"),
        RssNewsSource("Интерфакс", "https://www.interfax.ru/rss.asp", language = "ru"),
        RssNewsSource("ТАСС", "https://tass.ru/rss/v2.xml", language = "ru"),
        RssNewsSource("РИА Новости", "https://ria.ru/export/rss2/archive/index.xml", language = "ru"),
        RssNewsSource("Лента.ру", "https://lenta.ru/rss", language = "ru"),
        RssNewsSource("Фонтанка", "https://www.fontanka.ru/fontanka.rss", language = "ru"),
        RssNewsSource("Хабр", "https://habr.com/ru/rss/articles/?fl=ru", Topic.TECH, language = "ru"),
        GoogleNewsSource("Google Новости", "hl=ru&gl=RU&ceid=RU:ru", language = "ru"),
        // In-depth: investigations, explainers, data, science and fact-checks
        RssNewsSource("ProPublica", "https://www.propublica.org/feeds/propublica/main", Topic.POLITICS),
        RssNewsSource("Bellingcat", "https://www.bellingcat.com/feed/"),
        RssNewsSource("The Conversation", "https://theconversation.com/global/articles.atom", Topic.GENERAL),
        RssNewsSource("Our World in Data", "https://ourworldindata.org/atom.xml", Topic.GENERAL),
        RssNewsSource("Visual Capitalist", "https://www.visualcapitalist.com/feed/", Topic.BUSINESS),
        RssNewsSource("Quanta Magazine", "https://www.quantamagazine.org/feed/", Topic.SCIENCE),
        RssNewsSource("IEEE Spectrum", "https://spectrum.ieee.org/feeds/feed.rss", Topic.TECH),
        RssNewsSource(
            "NASA Earth Observatory",
            "https://earthobservatory.nasa.gov/feeds/earth-observatory.rss",
            Topic.CLIMATE,
        ),
        RssNewsSource("Aeon", "https://aeon.co/feed.rss", Topic.CULTURE),
        RssNewsSource("Smithsonian Magazine", "https://www.smithsonianmag.com/rss/latest_articles/", Topic.SCIENCE),
        RssNewsSource("Atlas Obscura", "https://www.atlasobscura.com/feeds/latest", Topic.CULTURE),
        RssNewsSource("PolitiFact", "https://www.politifact.com/rss/all/", Topic.POLITICS),
        RssNewsSource("Snopes", "https://www.snopes.com/feed/", Topic.GENERAL),
        RssNewsSource("Full Fact", "https://fullfact.org/feed/", Topic.POLITICS),
        WikipediaSource(),
        RssNewsSource("N+1", "https://nplus1.ru/rss", Topic.SCIENCE, language = "ru"),
        // Social
        RedditSource("worldnews", Topic.WORLD),
        RedditSource("news"),
        RedditSource("technology", Topic.TECH),
        HackerNewsSource(),
        LobstersSource(),
        LemmySource(),
        MastodonTrendingPostsSource(),
        MastodonTrendingLinksSource(),
        BlueskySource(),
    ) + gnewsSources(ResponseCache(File(cacheDir, "api_responses")))

    /** Every configured source, in display order. */
    val sourceInfo: List<SourceInfo> = sources.map { SourceInfo(it.name, it.kind, it.category, it.language) }

    /**
     * Fetches the named sources, emitting each one's result as soon as it arrives so the UI can
     * show articles without waiting for the slowest source. At most [MAX_PARALLEL_REQUESTS]
     * requests run at once; a failing source is reported rather than failing the rest.
     */
    fun fetch(names: Set<String>): Flow<SourceResult> = channelFlow {
        val permits = Semaphore(MAX_PARALLEL_REQUESTS)
        sources.filter { it.name in names }.forEach { source ->
            launch {
                permits.withPermit { send(SourceResult(source.name, runCatching { source.fetch() })) }
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val MAX_PARALLEL_REQUESTS = 10

        /** GNews categories, only when a GNEWS_API_KEY was configured at build time. */
        fun gnewsSources(cache: ResponseCache): List<NewsSource> {
            val key = BuildConfig.GNEWS_API_KEY
            if (key.isBlank()) return emptyList()
            return listOf(
                Triple("general", "Top stories", Topic.GENERAL),
                Triple("world", "World", Topic.WORLD),
                Triple("business", "Business", Topic.BUSINESS),
                Triple("technology", "Technology", Topic.TECH),
                Triple("science", "Science", Topic.SCIENCE),
                Triple("health", "Health", Topic.HEALTH),
                Triple("sports", "Sports", Topic.SPORTS),
                Triple("entertainment", "Entertainment", Topic.CULTURE),
            ).map { (category, label, topic) -> GNewsSource(category, label, topic, key, cache) } +
                GNewsSource("general", "Русский", Topic.GENERAL, key, cache, language = "ru")
        }
    }
}
