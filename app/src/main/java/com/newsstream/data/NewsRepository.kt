package com.newsstream.data

import com.newsstream.BuildConfig
import com.newsstream.data.source.GuardianNewsSource
import com.newsstream.data.source.NewsSource
import com.newsstream.data.source.RssNewsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class NewsResult(val articles: List<Article>, val failedSources: List<String>)

class NewsRepository(
    private val sources: List<NewsSource> = listOf(
        GuardianNewsSource(BuildConfig.GUARDIAN_API_KEY),
        RssNewsSource("BBC News", "https://feeds.bbci.co.uk/news/world/rss.xml"),
        RssNewsSource("NPR", "https://feeds.npr.org/1001/rss.xml"),
        RssNewsSource("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml"),
    ),
) {
    /** Fetches every source in parallel; a failing source is reported rather than failing the rest. */
    suspend fun latest(): NewsResult = withContext(Dispatchers.IO) {
        val results = coroutineScope {
            sources.map { source -> async { source to runCatching { source.fetch() } } }.awaitAll()
        }
        NewsResult(
            articles = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) }
                .distinctBy { it.url }
                .sortedByDescending { it.publishedAt },
            failedSources = results.filter { (_, result) -> result.isFailure }.map { (source, _) -> source.name },
        )
    }
}
