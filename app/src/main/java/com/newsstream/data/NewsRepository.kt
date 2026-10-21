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
import com.newsstream.data.source.TelegramChannelSource
import com.newsstream.data.source.VkWallSource
import com.newsstream.data.source.WikipediaSource
import com.newsstream.data.source.YouTubeChannelSource
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
        RssNewsSource("The Register", "https://www.theregister.com/headlines.atom", Topic.TECH),
        RssNewsSource("Tom's Hardware", "https://www.tomshardware.com/feeds/all", Topic.TECH),
        RssNewsSource("ZDNet", "https://www.zdnet.com/news/rss.xml", Topic.TECH),
        RssNewsSource("VentureBeat", "https://venturebeat.com/feed/", Topic.TECH),
        RssNewsSource("Popular Science", "https://www.popsci.com/feed/", Topic.TECH),
        // Science
        RssNewsSource("NASA", "https://www.nasa.gov/news-release/feed/", Topic.SCIENCE),
        RssNewsSource("ScienceDaily", "https://www.sciencedaily.com/rss/all.xml", Topic.SCIENCE),
        RssNewsSource("New Scientist", "https://www.newscientist.com/feed/home/", Topic.SCIENCE),
        RssNewsSource("Space.com", "https://www.space.com/feeds/all", Topic.SCIENCE),
        RssNewsSource("Phys.org", "https://phys.org/rss-feed/", Topic.SCIENCE),
        RssNewsSource("Nature", "https://www.nature.com/nature.rss", Topic.SCIENCE),
        RssNewsSource("Science (AAAS)", "https://www.science.org/rss/news_current.xml", Topic.SCIENCE),
        RssNewsSource(
            "Scientific American",
            "https://www.scientificamerican.com/platform/syndication/rss/",
            Topic.SCIENCE,
        ),
        RssNewsSource("Live Science", "https://www.livescience.com/feeds/all", Topic.SCIENCE),
        RssNewsSource("Universe Today", "https://www.universetoday.com/feed/", Topic.SCIENCE),
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
        // More per topic
        RssNewsSource("Foreign Policy", "https://foreignpolicy.com/feed/", Topic.WORLD),
        RssNewsSource("The Diplomat", "https://thediplomat.com/feed/", Topic.WORLD),
        RssNewsSource("Kyiv Independent", "https://kyivindependent.com/rss/", Topic.WORLD),
        RssNewsSource("BBC Politics", "https://feeds.bbci.co.uk/news/politics/rss.xml", Topic.POLITICS),
        RssNewsSource("NPR Politics", "https://feeds.npr.org/1014/rss.xml", Topic.POLITICS),
        RssNewsSource("Guardian Politics", "https://www.theguardian.com/politics/rss", Topic.POLITICS),
        RssNewsSource("BBC Business", "https://feeds.bbci.co.uk/news/business/rss.xml", Topic.BUSINESS),
        RssNewsSource("NPR Business", "https://feeds.npr.org/1006/rss.xml", Topic.BUSINESS),
        RssNewsSource("Guardian Business", "https://www.theguardian.com/business/rss", Topic.BUSINESS),
        RssNewsSource("BBC Technology", "https://feeds.bbci.co.uk/news/technology/rss.xml", Topic.TECH),
        RssNewsSource("9to5Google", "https://9to5google.com/feed/", Topic.TECH),
        RssNewsSource("Android Authority", "https://www.androidauthority.com/feed/", Topic.TECH),
        RssNewsSource(
            "BBC Science",
            "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
            Topic.SCIENCE,
        ),
        RssNewsSource("NPR Science", "https://feeds.npr.org/1007/rss.xml", Topic.SCIENCE),
        RssNewsSource("SciTechDaily", "https://scitechdaily.com/feed/", Topic.SCIENCE),
        RssNewsSource("Guardian Environment", "https://www.theguardian.com/environment/rss", Topic.CLIMATE),
        RssNewsSource("Grist", "https://grist.org/feed/", Topic.CLIMATE),
        RssNewsSource("Yale Environment 360", "https://e360.yale.edu/feed.xml", Topic.CLIMATE),
        RssNewsSource("BBC Health", "https://feeds.bbci.co.uk/news/health/rss.xml", Topic.HEALTH),
        RssNewsSource("NPR Health", "https://feeds.npr.org/1128/rss.xml", Topic.HEALTH),
        RssNewsSource("Medical Xpress", "https://medicalxpress.com/rss-feed/", Topic.HEALTH),
        RssNewsSource("Guardian Sport", "https://www.theguardian.com/uk/sport/rss", Topic.SPORTS),
        RssNewsSource("Yahoo Sports", "https://sports.yahoo.com/rss/", Topic.SPORTS),
        RssNewsSource("CBS Sports", "https://www.cbssports.com/rss/headlines/", Topic.SPORTS),
        RssNewsSource(
            "BBC Arts",
            "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml",
            Topic.CULTURE,
        ),
        RssNewsSource("The Hollywood Reporter", "https://www.hollywoodreporter.com/feed/", Topic.CULTURE),
        RssNewsSource("Guardian Culture", "https://www.theguardian.com/culture/rss", Topic.CULTURE),
        RssNewsSource("NBC News", "https://feeds.nbcnews.com/nbcnews/public/news", Topic.GENERAL),
        RssNewsSource("ABC News (US)", "https://abcnews.go.com/abcnews/topstories", Topic.GENERAL),
        RssNewsSource("Yahoo News", "https://news.yahoo.com/rss/", Topic.GENERAL),
        // Researched additions (feed addresses from feed directories and publisher pages)
        GoogleNewsSource("Reuters", query = "when:24h site:reuters.com", category = Topic.WORLD),
        GoogleNewsSource("AP News", query = "when:24h site:apnews.com", category = Topic.WORLD),
        RssNewsSource("The Moscow Times", "https://www.themoscowtimes.com/rss/news", Topic.WORLD),
        RssNewsSource("Meduza in English", "https://meduza.io/rss/en/all", Topic.WORLD),
        RssNewsSource("Financial Times", "https://www.ft.com/world?format=rss", Topic.WORLD),
        RssNewsSource("Euractiv", "https://www.euractiv.com/?feed=mcfeed", Topic.POLITICS),
        RssNewsSource("Bloomberg Markets", "https://feeds.bloomberg.com/markets/news.rss", Topic.BUSINESS),
        RssNewsSource("MarketWatch", "https://www.marketwatch.com/rss/topstories", Topic.BUSINESS),
        RssNewsSource("Yahoo Finance", "https://finance.yahoo.com/news/rssindex", Topic.BUSINESS),
        RssNewsSource("Science News", "https://www.sciencenews.org/feed", Topic.SCIENCE),
        RssNewsSource("Electrek", "https://electrek.co/feed/", Topic.CLIMATE),
        RssNewsSource("KFF Health News", "https://kffhealthnews.org/feed/", Topic.HEALTH),
        RssNewsSource("The Athletic", "https://www.nytimes.com/athletic/rss/news/", Topic.SPORTS),
        RssNewsSource("BBC Football", "https://feeds.bbci.co.uk/sport/football/rss.xml", Topic.SPORTS),
        RssNewsSource("Sky Sports Football", "https://www.skysports.com/rss/11095", Topic.SPORTS),
        // Curiosity: ideas, science writing, art and culture
        RssNewsSource("Colossal", "https://www.thisiscolossal.com/feed/", Topic.CULTURE),
        RssNewsSource("kottke.org", "https://feeds.kottke.org/main", Topic.GENERAL),
        RssNewsSource("Knowable Magazine", "https://knowablemagazine.org/rss", Topic.SCIENCE),
        RssNewsSource("Open Culture", "https://www.openculture.com/rss", Topic.CULTURE),
        RssNewsSource("Rest of World", "https://restofworld.org/feed/latest", Topic.TECH),
        RssNewsSource("Noema", "https://www.noemamag.com/feed/", Topic.GENERAL),
        RssNewsSource("Nautilus", "https://nautil.us/feed/", Topic.SCIENCE),
        RssNewsSource("Big Think", "https://bigthink.com/feed/", Topic.SCIENCE),
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
        // YouTube channels (public channel feeds, no key)
        YouTubeChannelSource("BBC News", "UC16niRr50-MSBwiO3YDb3RA", Topic.WORLD),
        YouTubeChannelSource("Al Jazeera English", "UCNye-wNBqNL5ZzHSJj3l8Bg", Topic.WORLD),
        YouTubeChannelSource("DW News", "UCknLrEdhRCp1aegoMqRaCZg", Topic.WORLD),
        YouTubeChannelSource("FRANCE 24 English", "UCQfwfsi5VrQ8yKZ-UWmAEFg", Topic.WORLD),
        YouTubeChannelSource("Sky News", "UCoMdktPbSTixAyNGwb-UYkQ", Topic.WORLD),
        YouTubeChannelSource("Reuters", "UChqUTb7kYRX8-EiaN3XFrSQ", Topic.WORLD),
        YouTubeChannelSource("Associated Press", "UC52X5wxOL_s5yw0dQk7NtgA", Topic.WORLD),
        YouTubeChannelSource("CNN", "UCupvZG-5ko_eiXAupbDfxWw", Topic.POLITICS),
        YouTubeChannelSource("NBC News", "UCeY0bbntWzzVIaj2z3QigXg", Topic.POLITICS),
        YouTubeChannelSource("ABC News", "UCBi2mrWuNuyYy4gbM6fU18Q", Topic.POLITICS),
        YouTubeChannelSource("CBS News", "UC8p1vwvWtl6T73JiExfWs1g", Topic.POLITICS),
        YouTubeChannelSource("Bloomberg Television", "UCIALMKvObZNtJ6AmdCLP7Lg", Topic.BUSINESS),
        YouTubeChannelSource("CNBC", "UCvJJ_dzjViJCoLf5uKUTwoA", Topic.BUSINESS),
        YouTubeChannelSource("The Verge", "UCddiUEpeqJcYeBxX1IVBKvQ", Topic.TECH),
        YouTubeChannelSource("Marques Brownlee", "UCBJycsmduvYEL83R_U4JriQ", Topic.TECH),
        YouTubeChannelSource("Linus Tech Tips", "UCXuqSBlHAE6Xw-yeJA0Tunw", Topic.TECH),
        YouTubeChannelSource("NASA", "UCLA_DiR1FfKNvjuUpBHmylQ", Topic.SCIENCE),
        YouTubeChannelSource("Veritasium", "UCHnyfMqiRRG1u-2MsSQLbXA", Topic.SCIENCE),
        YouTubeChannelSource("Kurzgesagt", "UCsXVk37bltHxD1rDPwtNM8Q", Topic.SCIENCE),
        YouTubeChannelSource("TED", "UCAuUUnT6oDeKwE6v1NGQxug", Topic.SCIENCE),
        YouTubeChannelSource("Vox", "UCLXo7UDZvByw2ixzpQCufnA", Topic.GENERAL),
        YouTubeChannelSource("The Guardian", "UCHpw8xwDNhU9gdohEcJu4aA", Topic.GENERAL),
        YouTubeChannelSource("World Health Organization", "UC07-dOwgza1IguKA86jqxNA", Topic.HEALTH),
        YouTubeChannelSource("ESPN", "UCiWLfSweyRNmLpgEHekhoAg", Topic.SPORTS),
        YouTubeChannelSource("Дождь", "UCdubelOloxR3wzwJG9x8YqQ", Topic.GENERAL, language = "ru"),
        YouTubeChannelSource("Настоящее время", "UCBG57608Hukev3d0d-gvLhQ", Topic.WORLD, language = "ru"),
        // Telegram public channels (web preview, no key)
        TelegramChannelSource("Meduza (Telegram)", "meduzalive"),
        TelegramChannelSource("Дождь (Telegram)", "tvrain"),
        TelegramChannelSource("Медиазона (Telegram)", "mediazzzona"),
        TelegramChannelSource("Настоящее время (Telegram)", "currenttime", Topic.WORLD),
        TelegramChannelSource("BBC Русская служба (Telegram)", "bbcrussian", Topic.WORLD),
        TelegramChannelSource("РБК (Telegram)", "rbc_news", Topic.BUSINESS),
        TelegramChannelSource("Коммерсантъ (Telegram)", "kommersant"),
        TelegramChannelSource("Интерфакс (Telegram)", "interfaxonline"),
        TelegramChannelSource("ТАСС (Telegram)", "tass_agency"),
        TelegramChannelSource("РИА Новости (Telegram)", "rian_ru"),
        TelegramChannelSource("NEXTA (Telegram)", "nexta_live", Topic.WORLD),
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
    ) + gnewsSources(ResponseCache(File(cacheDir, "api_responses"))) + vkSources()

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

        /** VK public pages, only when a VK_SERVICE_TOKEN was configured at build time. */
        fun vkSources(): List<NewsSource> {
            val token = BuildConfig.VK_SERVICE_TOKEN
            if (token.isBlank()) return emptyList()
            return listOf(
                VkWallSource("РИА Новости (VK)", "ria", token),
                VkWallSource("РБК (VK)", "rbc", token, Topic.BUSINESS),
                VkWallSource("Лента.ру (VK)", "lentaru", token),
                VkWallSource("Коммерсантъ (VK)", "kommersant_ru", token),
                VkWallSource("ТАСС (VK)", "tassagency", token),
            )
        }

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
