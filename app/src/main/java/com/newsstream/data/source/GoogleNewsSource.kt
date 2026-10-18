package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.Topic
import java.net.URLEncoder

/**
 * Google News: top stories, or the results of a search [query] such as
 * "when:24h site:reuters.com" — which gives a feed for outlets that no longer publish their own
 * (Reuters, AP). Its feed titles read "Headline - Publisher", so the publisher is split off into
 * the author line.
 */
class GoogleNewsSource(
    override val name: String = "Google News",
    private val edition: String = "hl=en-US&gl=US&ceid=US:en",
    override val language: String = "en",
    private val query: String? = null,
    override val category: Topic = Topic.GENERAL,
) : NewsSource {
    private val feedUrl = if (query == null) {
        "https://news.google.com/rss?$edition"
    } else {
        "https://news.google.com/rss/search?q=${URLEncoder.encode(query, "UTF-8")}&$edition"
    }

    override fun fetch(): List<Article> =
        RssNewsSource(name, feedUrl, category, language = language).fetch().map { article ->
            val split = article.title.lastIndexOf(" - ")
            if (split <= 0) {
                article
            } else {
                article.copy(
                    title = article.title.substring(0, split),
                    author = article.title.substring(split + 3),
                    // Google's summary just repeats the headline and publisher.
                    summary = "",
                )
            }
        }
}
