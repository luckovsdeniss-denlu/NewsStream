package com.newsstream.ui

import android.text.format.DateUtils
import com.newsstream.data.Article
import com.newsstream.data.displaySourceName
import java.util.Locale

/** Source and author. */
fun Article.byline(): String = listOfNotNull(displaySourceName(source), author).joinToString(" · ")

/** How long ago the story was published ("5 min. ago", "3 hours ago"), or null if unknown. */
fun Article.timeAgo(): String? =
    publishedAt.takeIf { it != 0L }?.let {
        DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
    }

/** 1234 -> "1.2K", 2500000 -> "2.5M". */
fun compactCount(n: Int): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
    else -> n.toString()
}.replace(".0", "")
