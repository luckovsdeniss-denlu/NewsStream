package com.newsstream.data.source

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

private const val DEFAULT_USER_AGENT = "NewsStream/1.0 (Android)"

internal fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String =
    request(url, "GET", headers, body = null).text()

/** Raw response bytes, e.g. for XML whose own declaration names the encoding. */
internal fun httpGetBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray =
    request(url, "GET", headers, body = null).bytes

internal class HttpResponse(val bytes: ByteArray, private val contentType: String?) {
    /** Decodes using the charset from Content-Type (e.g. windows-1251), defaulting to UTF-8. */
    fun text(): String {
        val charset = contentType?.let { CHARSET.find(it)?.groupValues?.get(1) }
            ?.let { runCatching { Charset.forName(it.trim('"')) }.getOrNull() }
            ?: Charsets.UTF_8
        return String(bytes, charset)
    }

    private companion object {
        val CHARSET = Regex("charset=([^;\\s]+)", RegexOption.IGNORE_CASE)
    }
}

/** GETs at most [maxBytes] of the response, e.g. just a web page's <head>. */
internal fun httpGetPrefix(url: String, maxBytes: Int): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
        val code = connection.responseCode
        if (code !in 200..299) throw IOException("HTTP $code from $url")
        val bytes = connection.inputStream.use { input ->
            val buffer = ByteArray(maxBytes)
            var total = 0
            while (total < maxBytes) {
                val read = input.read(buffer, total, maxBytes - total)
                if (read < 0) break
                total += read
            }
            buffer.copyOf(total)
        }
        return String(bytes)
    } finally {
        connection.disconnect()
    }
}

/** POSTs an application/x-www-form-urlencoded body. */
internal fun httpPostForm(url: String, form: String, headers: Map<String, String> = emptyMap()): String =
    request(url, "POST", headers + ("Content-Type" to "application/x-www-form-urlencoded"), body = form).text()

private fun request(url: String, method: String, headers: Map<String, String>, body: String?): HttpResponse {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = connection.responseCode
        if (code !in 200..299) throw IOException("HTTP $code from $url")
        val bytes = connection.inputStream.use { it.readBytes() }
        return HttpResponse(bytes, connection.contentType)
    } finally {
        connection.disconnect()
    }
}
