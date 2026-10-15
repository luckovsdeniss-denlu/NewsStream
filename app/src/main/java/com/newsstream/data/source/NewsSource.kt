package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Topic

interface NewsSource {
    val name: String

    /** Whether this is a news outlet or a social platform; used to group sources in settings. */
    val kind: ContentKind get() = ContentKind.NEWS

    /** The topic this source mostly covers; used to group sources in settings. */
    val category: Topic get() = Topic.GENERAL

    /** ISO 639-1 language of the content, e.g. "en" or "ru". */
    val language: String get() = "en"

    /** Fetches the latest articles. Blocking; call from a background dispatcher. */
    fun fetch(): List<Article>
}
