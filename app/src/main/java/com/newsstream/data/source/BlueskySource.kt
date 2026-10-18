package com.newsstream.data.source

import android.net.Uri
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Engagement
import com.newsstream.data.TopicClassifier
import org.json.JSONObject

/** Bluesky's "Discover" (What's Hot) feed via the public AppView API; no login needed. */
class BlueskySource : NewsSource {
    override val name = "Bluesky"
    override val kind = ContentKind.SOCIAL

    override fun fetch(): List<Article> {
        val url = Uri.parse("https://public.api.bsky.app/xrpc/app.bsky.feed.getFeed").buildUpon()
            .appendQueryParameter("feed", DISCOVER_FEED)
            .appendQueryParameter("limit", "90")
            .build()
            .toString()
        val feed = JSONObject(httpGet(url)).getJSONArray("feed")
        return (0 until feed.length()).mapNotNull { i ->
            val post = feed.getJSONObject(i).getJSONObject("post")
            // Skip anything carrying moderation labels (e.g. adult content).
            if ((post.optJSONArray("labels")?.length() ?: 0) > 0) return@mapNotNull null
            val record = post.getJSONObject("record")
            val text = record.optString("text").trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val handle = post.getJSONObject("author").getString("handle")
            val postId = post.getString("uri").substringAfterLast('/')
            Article(
                url = "https://bsky.app/profile/$handle/post/$postId",
                title = text,
                summary = "",
                body = emptyList(),
                source = name,
                publishedAt = parseFeedDate(record.optString("createdAt")),
                kind = ContentKind.SOCIAL,
                author = "@$handle",
                engagement = Engagement(
                    likes = post.optInt("likeCount"),
                    comments = post.optInt("replyCount"),
                    shares = post.optInt("repostCount"),
                ),
                topic = TopicClassifier.classify(text),
                imageUrl = embedImage(post.optJSONObject("embed"), "fullsize"),
                thumbnailUrl = embedImage(post.optJSONObject("embed"), "thumb"),
            )
        }
    }

    // Image posts carry thumbnails in embed.images; link cards in embed.external.
    private fun embedImage(embed: JSONObject?, size: String): String? {
        if (embed == null) return null
        embed.optJSONArray("images")?.optJSONObject(0)?.let { image ->
            (image.optStringOrNull(size) ?: image.optStringOrNull("thumb"))?.let { return it }
        }
        embed.optJSONObject("external")?.optStringOrNull("thumb")?.let { return it }
        return embedImage(embed.optJSONObject("media"), size)
    }

    private companion object {
        const val DISCOVER_FEED = "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.generator/whats-hot"
    }
}
