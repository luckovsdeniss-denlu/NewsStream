package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.Topic

/**
 * Google News top stories. Its feed titles read "Headline - Publisher", so the publisher is
 * split off into the author line.
 */
class GoogleNewsSource(
    override val name: String = "Google News",
    private val edition: String = "hl=en-US&gl=US&ceid=US:en",
    override val language: String = "en",
) : NewsSource {
    override val category = Topic.GENERAL

    override fun fetch(): List<Article> =
        RssNewsSource(name, "https://news.google.com/rss?$edition", Topic.GENERAL, language = language).fetch().map { article ->
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
