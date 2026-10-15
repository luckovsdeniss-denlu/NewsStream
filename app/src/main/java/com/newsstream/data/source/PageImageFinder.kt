package com.newsstream.data.source

import java.net.URL

/**
 * Finds a web page's lead image from its Open Graph / Twitter card meta tags, which news sites
 * set for link previews. Only the start of the page is downloaded. Blocking; call off the main thread.
 */
object PageImageFinder {
    private const val MAX_BYTES = 256 * 1024

    private val META_TAG = Regex("<meta\\s[^>]*>", RegexOption.IGNORE_CASE)
    private val IMAGE_KEY = Regex(
        "(?:property|name)\\s*=\\s*[\"'](?:og:image(?::secure_url|:url)?|twitter:image(?::src)?)[\"']",
        RegexOption.IGNORE_CASE,
    )
    private val CONTENT = Regex("content\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

    fun find(pageUrl: String): String? {
        val html = httpGetPrefix(pageUrl, MAX_BYTES)
        val image = META_TAG.findAll(html)
            .map { it.value }
            .filter { IMAGE_KEY.containsMatchIn(it) }
            .firstNotNullOfOrNull { CONTENT.find(it)?.groupValues?.get(1) }
            ?: return null
        // Resolve relative URLs against the page.
        return runCatching { URL(URL(pageUrl), image.replace("&amp;", "&")).toString() }.getOrNull()
    }
}
