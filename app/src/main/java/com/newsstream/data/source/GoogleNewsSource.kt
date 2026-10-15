package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.Topic

/**
 * Google News top stories. Its feed titles read "Headline - Publisher", so the publisher is
 * split off into the author line.
 */
class GoogleNewsSource(private val edition: String = "hl=en-US&gl=US&ceid=US:en") : NewsSource {
    override val name = "Google News"
    override val category = Topic.GENERAL

    override fun fetch(): List<Article> =
        RssNewsSource(name, "https://news.google.com/rss?$edition", Topic.GENERAL).fetch().map { article ->
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
