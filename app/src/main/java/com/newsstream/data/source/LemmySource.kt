package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Engagement
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.json.JSONObject

/** Hot posts on a Lemmy instance (an open, federated Reddit alternative) via its public API. */
class LemmySource(private val instance: String = "lemmy.world") : NewsSource {
    override val name = "Lemmy"
    override val kind = ContentKind.SOCIAL
    override val category = Topic.GENERAL

    override fun fetch(): List<Article> {
        val posts = JSONObject(httpGet("https://$instance/api/v3/post/list?sort=Hot&type_=All&limit=30"))
            .getJSONArray("posts")
        return (0 until posts.length()).mapNotNull { i ->
            val view = posts.getJSONObject(i)
            val post = view.getJSONObject("post")
            val community = view.optJSONObject("community")
            if (post.optBoolean("nsfw") || community?.optBoolean("nsfw") == true) return@mapNotNull null
            if (post.optBoolean("removed") || post.optBoolean("deleted")) return@mapNotNull null
            val title = post.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val thread = post.optStringOrNull("ap_id") ?: "https://$instance/post/${post.optLong("id")}"
            val summary = post.optString("body").take(600).trim()
            val counts = view.optJSONObject("counts")
            Article(
                url = post.optStringOrNull("url") ?: thread,
                title = title,
                summary = summary,
                body = emptyList(),
                source = name,
                publishedAt = parseFeedDate(post.optString("published")),
                kind = ContentKind.SOCIAL,
                author = view.optJSONObject("creator")?.optStringOrNull("name")?.let { "@$it" },
                engagement = counts?.let { Engagement(likes = it.optInt("score"), comments = it.optInt("comments")) },
                discussionUrl = thread,
                topic = TopicClassifier.classify(title, summary, community?.optString("title").orEmpty()),
            )
        }
    }
}
