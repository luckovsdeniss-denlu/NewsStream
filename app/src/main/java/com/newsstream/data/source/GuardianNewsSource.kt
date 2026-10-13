package com.newsstream.data.source

import android.net.Uri
import com.newsstream.data.Article
import org.json.JSONObject

/** The Guardian Open Platform content API: https://open-platform.theguardian.com */
class GuardianNewsSource(private val apiKey: String) : NewsSource {
    override val name = "The Guardian"

    override fun fetch(): List<Article> {
        val url = Uri.parse("https://content.guardianapis.com/search").buildUpon()
            .appendQueryParameter("api-key", apiKey)
            .appendQueryParameter("order-by", "newest")
            .appendQueryParameter("page-size", "20")
            .appendQueryParameter("type", "article")
            .appendQueryParameter("show-fields", "trailText,bodyText")
            .build()
            .toString()
        val results = JSONObject(httpGet(url)).getJSONObject("response").getJSONArray("results")
        return (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            val fields = item.optJSONObject("fields")
            Article(
                url = item.getString("webUrl"),
                title = item.getString("webTitle"),
                summary = htmlToText(fields?.optString("trailText").orEmpty()),
                body = fields?.optString("bodyText").orEmpty().toParagraphs(),
                source = name,
                publishedAt = parseFeedDate(item.optString("webPublicationDate")),
            )
        }
    }

    // bodyText has no paragraph markup, so break it into readable chunks of a few sentences.
    private fun String.toParagraphs(): List<String> {
        if (isBlank()) return emptyList()
        val sentences = trim().split(Regex("(?<=[.!?][\"”’]?)\\s+(?=[A-Z“\"‘])"))
        return sentences.chunked(3).map { it.joinToString(" ") }
    }
}
