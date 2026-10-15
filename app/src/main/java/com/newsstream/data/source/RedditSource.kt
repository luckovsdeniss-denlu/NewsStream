package com.newsstream.data.source

import android.util.Base64
import com.newsstream.BuildConfig
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Engagement
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Hot posts of a subreddit.
 *
 * Reddit blocks anonymous requests to its JSON API, so this uses one of two routes:
 * - with a Reddit app client ID ([BuildConfig.REDDIT_CLIENT_ID]), the official OAuth API,
 *   which includes scores and comment counts;
 * - otherwise the subreddit's public Atom feed, which has headlines and links only.
 */
class RedditSource(
    private val subreddit: String,
    private val defaultTopic: Topic = Topic.GENERAL,
    private val clientId: String = BuildConfig.REDDIT_CLIENT_ID,
) : NewsSource {
    override val name = "Reddit r/$subreddit"
    override val kind = ContentKind.SOCIAL
    override val category = defaultTopic

    override fun fetch(): List<Article> = if (clientId.isNotBlank()) fetchFromApi() else fetchFromFeed()

    private fun fetchFromFeed(): List<Article> =
        RssNewsSource(name, "https://www.reddit.com/r/$subreddit/hot/.rss", defaultTopic, REDDIT_HEADERS)
            .fetch()
            .map { it.copy(kind = ContentKind.SOCIAL, discussionUrl = it.url) }

    private fun fetchFromApi(): List<Article> {
        val headers = REDDIT_HEADERS + ("Authorization" to "Bearer ${accessToken(clientId)}")
        val children = JSONObject(httpGet("https://oauth.reddit.com/r/$subreddit/hot?limit=30&raw_json=1", headers))
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

    private companion object {
        // Reddit asks API clients for a unique, descriptive User-Agent.
        val REDDIT_HEADERS = mapOf("User-Agent" to "android:com.newsstream:v1.0 (NewsStream news reader)")

        private var token: String? = null
        private var tokenExpiresAt = 0L

        /**
         * App-only OAuth token for an "installed app" client (no user login, no secret),
         * shared by all subreddit sources and refreshed shortly before it expires.
         */
        @Synchronized
        fun accessToken(clientId: String): String {
            val now = System.currentTimeMillis()
            token?.takeIf { now < tokenExpiresAt }?.let { return it }
            val credentials = Base64.encodeToString("$clientId:".toByteArray(), Base64.NO_WRAP)
            val grant = URLEncoder.encode("https://oauth.reddit.com/grants/installed_client", "UTF-8")
            val response = JSONObject(
                httpPostForm(
                    "https://www.reddit.com/api/v1/access_token",
                    "grant_type=$grant&device_id=DO_NOT_TRACK_THIS_DEVICE",
                    REDDIT_HEADERS + ("Authorization" to "Basic $credentials"),
                ),
            )
            val newToken = response.getString("access_token")
            token = newToken
            tokenExpiresAt = now + (response.optLong("expires_in", 3600) - 60) * 1000
            return newToken
        }
    }
}
