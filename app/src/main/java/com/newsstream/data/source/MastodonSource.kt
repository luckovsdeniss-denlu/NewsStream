package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Engagement
import com.newsstream.data.TopicClassifier
import org.json.JSONObject

/** Trending public posts on a Mastodon server: https://docs.joinmastodon.org/methods/trends/ */
class MastodonTrendingPostsSource(private val server: String = "mastodon.social") : NewsSource {
    override val name = "Mastodon"
    override val kind = ContentKind.SOCIAL

    override fun fetch(): List<Article> {
        val statuses = org.json.JSONArray(httpGet("https://$server/api/v1/trends/statuses?limit=30"))
        return (0 until statuses.length()).mapNotNull { i ->
            val status = statuses.getJSONObject(i)
            if (status.optBoolean("sensitive")) return@mapNotNull null
            val text = htmlToText(status.optString("content")).takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val account = status.getJSONObject("account")
            Article(
                url = status.optStringOrNull("url") ?: return@mapNotNull null,
                title = text,
                summary = "",
                body = emptyList(),
                source = name,
                publishedAt = parseFeedDate(status.optString("created_at")),
                kind = ContentKind.SOCIAL,
                author = "@" + account.optString("acct"),
                engagement = Engagement(
                    likes = status.optInt("favourites_count"),
                    comments = status.optInt("replies_count"),
                    shares = status.optInt("reblogs_count"),
                ),
                topic = TopicClassifier.classify(text),
                imageUrl = firstImage(status)?.optStringOrNull("url")
                    ?: status.optJSONObject("card")?.optStringOrNull("image"),
                thumbnailUrl = firstImage(status)?.optStringOrNull("preview_url"),
            )
        }
    }
}

/** News links trending on a Mastodon server, ranked by how often they are shared. */
class MastodonTrendingLinksSource(private val server: String = "mastodon.social") : NewsSource {
    override val name = "Mastodon Trending"
    override val kind = ContentKind.SOCIAL

    override fun fetch(): List<Article> {
        val links = org.json.JSONArray(httpGet("https://$server/api/v1/trends/links?limit=20"))
        return (0 until links.length()).mapNotNull { i ->
            val link = links.getJSONObject(i)
            val title = link.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val history = link.optJSONArray("history")
            val cleanTitle = htmlToText(title)
            val summary = htmlToText(link.optString("description"))
            val shares = (0 until (history?.length() ?: 0)).sumOf { history!!.getJSONObject(it).optString("uses").toIntOrNull() ?: 0 }
            Article(
                url = link.getString("url"),
                title = cleanTitle,
                summary = summary,
                body = emptyList(),
                source = name,
                publishedAt = parseFeedDate(link.optString("published_at")),
                kind = ContentKind.SOCIAL,
                author = link.optStringOrNull("provider_name"),
                engagement = Engagement(shares = shares),
                topic = TopicClassifier.classify(cleanTitle, summary),
                imageUrl = link.optStringOrNull("image"),
            )
        }
    }
}

private fun firstImage(status: JSONObject): JSONObject? =
    status.optJSONArray("media_attachments")?.optJSONObject(0)?.takeIf { it.optString("type") == "image" }

internal fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
