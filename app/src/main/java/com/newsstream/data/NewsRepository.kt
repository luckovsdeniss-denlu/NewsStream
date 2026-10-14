package com.newsstream.data

import com.newsstream.BuildConfig
import com.newsstream.data.source.BlueskySource
import com.newsstream.data.source.GuardianNewsSource
import com.newsstream.data.source.HackerNewsSource
import com.newsstream.data.source.MastodonTrendingLinksSource
import com.newsstream.data.source.MastodonTrendingPostsSource
import com.newsstream.data.source.NewsSource
import com.newsstream.data.source.RedditSource
import com.newsstream.data.source.RssNewsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class NewsResult(val articles: List<Article>, val failedSources: List<String>)

class NewsRepository(
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
        // Business
        RssNewsSource("CNBC", "https://www.cnbc.com/id/100003114/device/rss/rss.html", Topic.BUSINESS),
        // Technology and science
        RssNewsSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", Topic.TECH),
        RssNewsSource("TechCrunch", "https://techcrunch.com/feed/", Topic.TECH),
        RssNewsSource("The Verge", "https://www.theverge.com/rss/index.xml", Topic.TECH),
        RssNewsSource("NASA", "https://www.nasa.gov/news-release/feed/", Topic.SCIENCE),
        // Social
        RedditSource("worldnews", Topic.WORLD),
        RedditSource("news"),
        RedditSource("technology", Topic.TECH),
        HackerNewsSource(),
        MastodonTrendingPostsSource(),
        MastodonTrendingLinksSource(),
        BlueskySource(),
    ),
) {
    /** Fetches every source in parallel; a failing source is reported rather than failing the rest. */
    suspend fun latest(): NewsResult = withContext(Dispatchers.IO) {
        val results = coroutineScope {
            sources.map { source -> async { source to runCatching { source.fetch() } } }.awaitAll()
        }
        NewsResult(
            articles = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) }
                .distinctBy { it.id }
                .sortedByDescending { it.publishedAt },
            failedSources = results.filter { (_, result) -> result.isFailure }.map { (source, _) -> source.name },
        )
    }
}
