package com.newsstream.data

data class Article(
    /** The article's web URL, which also identifies it across sources. */
    val url: String,
    val title: String,
    val summary: String,
    /** Full text split into paragraphs; empty when the source only provides a summary. */
    val body: List<String>,
    val source: String,
    /** Publication time in epoch milliseconds, or 0 when unknown. */
    val publishedAt: Long,
)
