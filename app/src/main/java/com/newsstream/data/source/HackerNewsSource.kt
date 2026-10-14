package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Engagement
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.json.JSONObject

/** Hacker News front page via the Algolia HN Search API: https://hn.algolia.com/api */
class HackerNewsSource : NewsSource {
    override val name = "Hacker News"

    override fun fetch(): List<Article> {
        val hits = JSONObject(httpGet("https://hn.algolia.com/api/v1/search?tags=front_page&hitsPerPage=30"))
            .getJSONArray("hits")
        return (0 until hits.length()).mapNotNull { i ->
            val hit = hits.getJSONObject(i)
            val title = hit.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val thread = "https://news.ycombinator.com/item?id=${hit.getString("objectID")}"
            Article(
                url = hit.optStringOrNull("url") ?: thread,
                title = title,
                summary = htmlToText(hit.optString("story_text")),
                body = emptyList(),
                source = name,
                publishedAt = hit.optLong("created_at_i") * 1000,
                kind = ContentKind.SOCIAL,
                author = hit.optStringOrNull("author"),
                engagement = Engagement(likes = hit.optInt("points"), comments = hit.optInt("num_comments")),
                discussionUrl = thread,
                topic = TopicClassifier.classify(title, fallback = Topic.TECH),
            )
        }
    }
}
