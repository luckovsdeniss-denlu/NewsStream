package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Engagement
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.json.JSONObject
import java.io.IOException

/**
 * Posts from a public VK page via the official API method wall.get
 * (https://dev.vk.com/method/wall.get), authorised with an app's service token.
 */
class VkWallSource(
    override val name: String,
    /** The page's short address, e.g. "ria" for vk.com/ria. */
    private val domain: String,
    private val serviceToken: String,
    override val category: Topic = Topic.GENERAL,
    override val language: String = "ru",
) : NewsSource {
    override val kind = ContentKind.SOCIAL

    override fun fetch(): List<Article> {
        val json = JSONObject(
            httpGet("https://api.vk.com/method/wall.get?domain=$domain&count=30&v=5.199&access_token=$serviceToken"),
        )
        // VK reports problems (bad token, unknown page) as an "error" object with HTTP 200.
        json.optJSONObject("error")?.let { throw IOException("VK: ${it.optString("error_msg")}") }
        val items = json.getJSONObject("response").getJSONArray("items")
        return (0 until items.length()).mapNotNull { i ->
            val post = items.getJSONObject(i)
            if (post.optInt("marked_as_ads") == 1) return@mapNotNull null
            val text = post.optString("text").trim()
            val attachments = post.optJSONArray("attachments")
            var image: String? = null
            var link: JSONObject? = null
            for (a in 0 until (attachments?.length() ?: 0)) {
                val attachment = attachments!!.getJSONObject(a)
                when (attachment.optString("type")) {
                    "photo" -> if (image == null) image = largestPhoto(attachment.optJSONObject("photo"))
                    "link" -> if (link == null) link = attachment.optJSONObject("link")
                }
            }
            if (image == null) image = largestPhoto(link?.optJSONObject("photo"))
            val title = text.ifBlank { link?.optString("title").orEmpty() }
            if (title.isBlank()) return@mapNotNull null
            Article(
                url = "https://vk.com/wall${post.optLong("owner_id")}_${post.optLong("id")}",
                title = title,
                summary = "",
                body = emptyList(),
                source = name,
                publishedAt = post.optLong("date") * 1000,
                kind = ContentKind.SOCIAL,
                engagement = Engagement(
                    likes = post.optJSONObject("likes")?.optInt("count"),
                    comments = post.optJSONObject("comments")?.optInt("count"),
                    shares = post.optJSONObject("reposts")?.optInt("count"),
                ),
                topic = TopicClassifier.classify(title, fallback = category),
                imageUrl = image,
            )
        }
    }

    /** The widest size of a VK photo object. */
    private fun largestPhoto(photo: JSONObject?): String? {
        val sizes = photo?.optJSONArray("sizes") ?: return null
        return (0 until sizes.length()).map { sizes.getJSONObject(it) }
            .maxByOrNull { it.optInt("width") }
            ?.optStringOrNull("url")
    }
}
