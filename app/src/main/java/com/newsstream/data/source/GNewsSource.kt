package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.json.JSONObject
import java.io.IOException

/**
 * One GNews.io top-headlines category (https://gnews.io/docs/v4).
 *
 * The free plan allows about 100 requests a day, so responses are cached on disk for
 * [CACHE_MINUTES]: refreshing within that window costs nothing, and if a request fails
 * (e.g. the daily quota is used up) the last saved response is shown instead.
 */
class GNewsSource(
    /** GNews category: general, world, business, technology, science, health, sports, entertainment. */
    private val gnewsCategory: String,
    label: String,
    override val category: Topic,
    private val apiKey: String,
    private val cache: ResponseCache,
) : NewsSource {
    override val name = "GNews · $label"

    override fun fetch(): List<Article> {
        val key = "gnews_$gnewsCategory"
        val body = cache.read(key, CACHE_MINUTES * 60_000L) ?: try {
            httpGet(
                "https://gnews.io/api/v4/top-headlines?category=$gnewsCategory&lang=en&max=10&apikey=$apiKey",
            ).also { cache.write(key, it) }
        } catch (e: IOException) {
            cache.read(key) ?: throw e
        }

        val articles = JSONObject(body).getJSONArray("articles")
        return (0 until articles.length()).mapNotNull { i ->
            val item = articles.getJSONObject(i)
            val title = item.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val url = item.optStringOrNull("url") ?: return@mapNotNull null
            val summary = item.optString("description").trim()
            Article(
                url = url,
                title = title,
                summary = summary,
                // The free plan truncates content ("… [1234 chars]"), so the summary is used instead.
                body = emptyList(),
                source = name,
                publishedAt = parseFeedDate(item.optString("publishedAt")),
                author = item.optJSONObject("source")?.optStringOrNull("name"),
                topic = TopicClassifier.classify(title, summary, fallback = category),
                imageUrl = item.optStringOrNull("image"),
            )
        }
    }

    private companion object {
        const val CACHE_MINUTES = 30
    }
}
