package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Wikipedia's daily featured feed (https://en.wikipedia.org/api/rest_v1/): the "In the news"
 * items on the main page, each linked to its Wikipedia article, plus Today's featured article.
 */
class WikipediaSource(private val lang: String = "en") : NewsSource {
    override val name = "Wikipedia"
    override val category = Topic.GENERAL
    override val language = lang

    override fun fetch(): List<Article> {
        val today = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val path = String.format(
            Locale.US, "%04d/%02d/%02d",
            today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH),
        )
        val feed = JSONObject(
            httpGet(
                "https://$lang.wikipedia.org/api/rest_v1/feed/featured/$path",
                // Wikimedia asks API clients to identify themselves.
                mapOf("Api-User-Agent" to "NewsStream/1.0 (Android news reader)"),
            ),
        )
        // The feed is per day, without times: date items at the start of the day.
        val startOfDay = today.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val news = feed.optJSONArray("news")
        val inTheNews = (0 until (news?.length() ?: 0)).mapNotNull { i ->
            val item = news!!.getJSONObject(i)
            val story = htmlToText(item.optString("story")).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            // The first linked page is the story's main subject.
            val page = item.optJSONArray("links")?.optJSONObject(0) ?: return@mapNotNull null
            pageArticle(page, title = story, author = "In the news", publishedAt = startOfDay)
        }
        val featured = feed.optJSONObject("tfa")?.let { page ->
            pageArticle(page, title = null, author = "Today's featured article", publishedAt = startOfDay)
        }
        return inTheNews + listOfNotNull(featured)
    }

    private fun pageArticle(page: JSONObject, title: String?, author: String, publishedAt: Long): Article? {
        val url = page.optJSONObject("content_urls")?.optJSONObject("desktop")?.optStringOrNull("page") ?: return null
        val pageTitle = page.optJSONObject("titles")?.optStringOrNull("normalized") ?: page.optString("title")
        val extract = page.optString("extract").trim()
        val headline = title ?: pageTitle
        return Article(
            url = url,
            title = headline,
            summary = extract,
            body = extract.split("\n").filter { it.isNotBlank() },
            source = name,
            publishedAt = publishedAt,
            author = author,
            topic = TopicClassifier.classify(headline, extract),
            imageUrl = page.optJSONObject("originalimage")?.optStringOrNull("source")
                ?: page.optJSONObject("thumbnail")?.optStringOrNull("source"),
            thumbnailUrl = page.optJSONObject("thumbnail")?.optStringOrNull("source"),
        )
    }
}
