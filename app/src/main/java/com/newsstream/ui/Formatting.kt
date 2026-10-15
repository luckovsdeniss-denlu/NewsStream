package com.newsstream.ui

import com.newsstream.data.Article
import java.util.Locale

/** Source and author. */
fun Article.byline(): String = listOfNotNull(source, author).joinToString(" · ")

/** 1234 -> "1.2K", 2500000 -> "2.5M". */
fun compactCount(n: Int): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
    else -> n.toString()
}.replace(".0", "")
