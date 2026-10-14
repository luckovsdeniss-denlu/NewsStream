package com.newsstream.data.source

import android.net.Uri
import com.newsstream.data.Article
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
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
            val title = item.getString("webTitle")
            val summary = htmlToText(fields?.optString("trailText").orEmpty())
            Article(
                url = item.getString("webUrl"),
                title = title,
                summary = summary,
                body = fields?.optString("bodyText").orEmpty().toParagraphs(),
                source = name,
                publishedAt = parseFeedDate(item.optString("webPublicationDate")),
                topic = SECTION_TOPICS[item.optString("sectionId")]
                    ?: TopicClassifier.classify(title, summary, item.optString("sectionName"), Topic.WORLD),
            )
        }
    }

    private companion object {
        val SECTION_TOPICS = mapOf(
            "world" to Topic.WORLD, "uk-news" to Topic.WORLD, "us-news" to Topic.WORLD,
            "australia-news" to Topic.WORLD, "global-development" to Topic.WORLD,
            "politics" to Topic.POLITICS,
            "business" to Topic.BUSINESS, "money" to Topic.BUSINESS,
            "technology" to Topic.TECH, "games" to Topic.TECH,
            "science" to Topic.SCIENCE,
            "environment" to Topic.CLIMATE,
            "society" to Topic.HEALTH, "healthcare-network" to Topic.HEALTH,
            "sport" to Topic.SPORTS, "football" to Topic.SPORTS,
            "culture" to Topic.CULTURE, "film" to Topic.CULTURE, "music" to Topic.CULTURE,
            "books" to Topic.CULTURE, "tv-and-radio" to Topic.CULTURE, "artanddesign" to Topic.CULTURE,
            "stage" to Topic.CULTURE, "fashion" to Topic.CULTURE,
        )
    }

    // bodyText has no paragraph markup, so break it into readable chunks of a few sentences.
    private fun String.toParagraphs(): List<String> {
        if (isBlank()) return emptyList()
        val sentences = trim().split(Regex("(?<=[.!?][\"”’]?)\\s+(?=[A-Z“\"‘])"))
        return sentences.chunked(3).map { it.joinToString(" ") }
    }
}
