package com.newsstream.data.source

import java.io.File

/**
 * Saves API responses to disk so rate-limited sources can reuse a recent response instead of
 * spending a request, and fall back to the last good one when a request fails.
 */
class ResponseCache(private val dir: File) {

    /** The cached body for [key] if it is younger than [maxAgeMs], else null. */
    fun read(key: String, maxAgeMs: Long = Long.MAX_VALUE): String? {
        val file = fileFor(key)
        if (!file.exists() || System.currentTimeMillis() - file.lastModified() > maxAgeMs) return null
        return runCatching { file.readText() }.getOrNull()
    }

    fun write(key: String, body: String) {
        runCatching {
            dir.mkdirs()
            fileFor(key).writeText(body)
        }
    }

    private fun fileFor(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")
}
