package com.newsstream.data

enum class ContentKind { NEWS, SOCIAL }

/** Social engagement counts; null means the platform doesn't report that metric. */
data class Engagement(val likes: Int? = null, val comments: Int? = null, val shares: Int? = null)

data class Article(
    /** The article's (or post's) web URL. */
    val url: String,
    /** Headline; for social posts without a headline, the post text itself. */
    val title: String,
    val summary: String,
    /** Full text split into paragraphs; empty when the source only provides a summary. */
    val body: List<String>,
    val source: String,
    /** Publication time in epoch milliseconds, or 0 when unknown. */
    val publishedAt: Long,
    val kind: ContentKind = ContentKind.NEWS,
    val author: String? = null,
    val engagement: Engagement? = null,
    /** Comment thread, when it lives somewhere other than [url] (e.g. a Reddit link post). */
    val discussionUrl: String? = null,
    val topic: Topic = Topic.GENERAL,
    /** Lead image, when the source provides one; otherwise found on demand from the page. */
    val imageUrl: String? = null,
) {
    /** Unique across sources: two platforms may share the same link. */
    val id: String get() = "$source|$url"
}
