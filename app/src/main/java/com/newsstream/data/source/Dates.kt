package com.newsstream.data.source

import java.text.SimpleDateFormat
import java.util.Locale

private val DATE_PATTERNS = listOf(
    "EEE, dd MMM yyyy HH:mm:ss Z", // RSS 2.0 (RFC 822)
    "EEE, dd MMM yyyy HH:mm:ss zzz",
    "dd MMM yyyy HH:mm:ss Z",
    "yyyy-MM-dd'T'HH:mm:ssX", // Atom, Dublin Core, JSON APIs (ISO 8601)
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
)

/** Parses the date formats used by news feeds and APIs; returns 0 when unrecognised. */
internal fun parseFeedDate(value: String): Long {
    val text = value.trim()
    if (text.isEmpty()) return 0L
    for (pattern in DATE_PATTERNS) {
        runCatching { SimpleDateFormat(pattern, Locale.US).parse(text)?.time }
            .getOrNull()?.let { return it }
    }
    return 0L
}
