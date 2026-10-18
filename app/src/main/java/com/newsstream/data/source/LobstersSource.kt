package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Engagement
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.json.JSONArray
import org.json.JSONObject

/** Lobsters, a link-sharing community focused on computing: https://lobste.rs */
class LobstersSource : NewsSource {
    override val name = "Lobsters"
    override val kind = ContentKind.SOCIAL
    override val category = Topic.TECH

    override fun fetch(): List<Article> {
        val stories = JSONArray(httpGet("https://lobste.rs/hottest.json"))
        return (0 until stories.length()).mapNotNull { i ->
            val story = stories.getJSONObject(i)
            val title = story.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val thread = story.optStringOrNull("comments_url") ?: return@mapNotNull null
            // Older API versions return the submitter as an object, newer ones as a name.
            val submitter = (story.opt("submitter_user") as? JSONObject)?.optString("username")
                ?: story.optStringOrNull("submitter_user")
            val tags = story.optJSONArray("tags")?.let { t -> (0 until t.length()).joinToString(" ") { t.getString(it) } }
            Article(
                url = story.optStringOrNull("url") ?: thread,
                title = title,
                summary = htmlToText(story.optString("description")),
                body = emptyList(),
                source = name,
                publishedAt = parseFeedDate(story.optString("created_at")),
                kind = ContentKind.SOCIAL,
                author = submitter,
                engagement = Engagement(likes = story.optInt("score"), comments = story.optInt("comment_count")),
                discussionUrl = thread,
                topic = TopicClassifier.classify(title, hints = tags.orEmpty(), fallback = Topic.TECH),
            )
        }
    }
}
