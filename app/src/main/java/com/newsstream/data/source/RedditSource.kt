package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Engagement
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.json.JSONObject

/** Hot posts of a subreddit via Reddit's public JSON listing. */
class RedditSource(private val subreddit: String, private val defaultTopic: Topic = Topic.GENERAL) : NewsSource {
    override val name = "Reddit r/$subreddit"

    override fun fetch(): List<Article> {
        val children = JSONObject(httpGet("https://www.reddit.com/r/$subreddit/hot.json?limit=30&raw_json=1"))
            .getJSONObject("data").getJSONArray("children")
        return (0 until children.length()).mapNotNull { i ->
            val post = children.getJSONObject(i).getJSONObject("data")
            if (post.optBoolean("stickied") || post.optBoolean("over_18")) return@mapNotNull null
            val thread = "https://www.reddit.com" + post.getString("permalink")
            val title = post.getString("title")
            val summary = post.optString("selftext").take(600).trim()
            Article(
                url = if (post.optBoolean("is_self")) thread else post.optStringOrNull("url") ?: thread,
                title = title,
                summary = summary,
                body = emptyList(),
                source = name,
                publishedAt = (post.optDouble("created_utc", 0.0) * 1000).toLong(),
                kind = ContentKind.SOCIAL,
                author = post.optStringOrNull("author")?.let { "u/$it" },
                engagement = Engagement(likes = post.optInt("score"), comments = post.optInt("num_comments")),
                discussionUrl = thread,
                topic = TopicClassifier.classify(title, summary, post.optString("link_flair_text"), defaultTopic),
            )
        }
    }
}
