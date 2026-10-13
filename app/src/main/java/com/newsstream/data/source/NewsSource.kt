package com.newsstream.data.source

import com.newsstream.data.Article

interface NewsSource {
    val name: String

    /** Fetches the latest articles. Blocking; call from a background dispatcher. */
    fun fetch(): List<Article>
}
