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

private val FRACTION_BEYOND_MILLIS = Regex("(\\.\\d{3})\\d+")
private val ISO_WITHOUT_ZONE = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?")

/** Parses the date formats used by news feeds and APIs; returns 0 when unrecognised. */
internal fun parseFeedDate(value: String): Long {
    var text = value.trim()
    if (text.isEmpty()) return 0L
    // Some APIs send microseconds ("…:05.123456Z"); keep milliseconds only.
    text = text.replace(FRACTION_BEYOND_MILLIS, "$1")
    // ISO timestamps without a zone are UTC.
    if (ISO_WITHOUT_ZONE.matches(text)) text += "Z"
    for (pattern in DATE_PATTERNS) {
        runCatching { SimpleDateFormat(pattern, Locale.US).parse(text)?.time }
            .getOrNull()?.let { return it }
    }
    return 0L
}
