package com.newsstream.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stories the user saved, kept in full in a JSON file on the device so they stay readable
 * offline and after they drop out of the live feeds. Newest-saved first.
 */
class SavedArticles(context: Context) {
    private val file = File(context.filesDir, "saved_articles.json")

    fun load(): List<Article> = runCatching {
        val array = JSONArray(file.readText())
        (0 until array.length()).mapNotNull { runCatching { array.getJSONObject(it).toArticle() }.getOrNull() }
    }.getOrDefault(emptyList())

    @Synchronized
    fun store(articles: List<Article>) {
        val array = JSONArray()
        articles.forEach { array.put(it.toJson()) }
        // Write to a temp file first so a crash mid-write can't corrupt the saved list.
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(array.toString())
        temp.renameTo(file)
    }
}

private fun Article.toJson() = JSONObject().apply {
    put("url", url)
    put("title", title)
    put("summary", summary)
    put("body", JSONArray(body))
    put("source", source)
    put("publishedAt", publishedAt)
    put("kind", kind.name)
    put("topic", topic.name)
    author?.let { put("author", it) }
    discussionUrl?.let { put("discussionUrl", it) }
    imageUrl?.let { put("imageUrl", it) }
    thumbnailUrl?.let { put("thumbnailUrl", it) }
    engagement?.let { e ->
        put(
            "engagement",
            JSONObject().apply {
                e.likes?.let { put("likes", it) }
                e.comments?.let { put("comments", it) }
                e.shares?.let { put("shares", it) }
            },
        )
    }
}

private fun JSONObject.toArticle(): Article {
    val bodyArray = optJSONArray("body")
    return Article(
        url = getString("url"),
        title = getString("title"),
        summary = optString("summary"),
        body = (0 until (bodyArray?.length() ?: 0)).map { bodyArray!!.getString(it) },
        source = getString("source"),
        publishedAt = optLong("publishedAt"),
        kind = runCatching { ContentKind.valueOf(optString("kind")) }.getOrDefault(ContentKind.NEWS),
        topic = runCatching { Topic.valueOf(optString("topic")) }.getOrDefault(Topic.GENERAL),
        author = optNullableString("author"),
        discussionUrl = optNullableString("discussionUrl"),
        imageUrl = optNullableString("imageUrl"),
        thumbnailUrl = optNullableString("thumbnailUrl"),
        engagement = optJSONObject("engagement")?.let { e ->
            Engagement(
                likes = e.takeIf { it.has("likes") }?.optInt("likes"),
                comments = e.takeIf { it.has("comments") }?.optInt("comments"),
                shares = e.takeIf { it.has("shares") }?.optInt("shares"),
            )
        },
    )
}

private fun JSONObject.optNullableString(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null
