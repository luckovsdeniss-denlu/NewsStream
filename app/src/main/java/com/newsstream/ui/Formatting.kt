package com.newsstream.ui

import android.content.Context
import android.text.format.DateUtils
import com.newsstream.data.Article
import java.util.Locale

/** Source and author; the publish time is shown separately (see [relativeTime], [publishedTime]). */
fun Article.byline(): String = listOfNotNull(source, author).joinToString(" · ")

/** "5 minutes ago", or null when the source gave no date. */
fun Article.relativeTime(): String? =
    publishedAt.takeIf { it != 0L }?.let {
        DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }

/**
 * Clock time for today's stories ("14:32"), date and time otherwise ("Oct 14, 09:05"), in the
 * device's locale and 12/24-hour setting. [full] adds the weekday and year.
 */
fun Article.publishedTime(context: Context, full: Boolean = false): String? {
    if (publishedAt == 0L) return null
    var flags = DateUtils.FORMAT_SHOW_TIME
    if (full) {
        flags = flags or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_YEAR
    } else if (!DateUtils.isToday(publishedAt)) {
        flags = flags or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR
    }
    return DateUtils.formatDateTime(context, publishedAt, flags)
}

/** 1234 -> "1.2K", 2500000 -> "2.5M". */
fun compactCount(n: Int): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
    else -> n.toString()
}.replace(".0", "")
