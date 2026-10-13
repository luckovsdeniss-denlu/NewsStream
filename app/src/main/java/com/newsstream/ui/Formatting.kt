package com.newsstream.ui

import android.text.format.DateUtils
import com.newsstream.data.Article

fun Article.byline(): String =
    if (publishedAt == 0L) {
        source
    } else {
        val time = DateUtils.getRelativeTimeSpanString(
            publishedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        )
        "$source · $time"
    }
