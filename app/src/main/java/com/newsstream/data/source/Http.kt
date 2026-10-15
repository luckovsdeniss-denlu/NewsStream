package com.newsstream.data.source

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val DEFAULT_USER_AGENT = "NewsStream/1.0 (Android)"

internal fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String =
    request(url, "GET", headers, body = null)

/** POSTs an application/x-www-form-urlencoded body. */
internal fun httpPostForm(url: String, form: String, headers: Map<String, String> = emptyMap()): String =
    request(url, "POST", headers + ("Content-Type" to "application/x-www-form-urlencoded"), body = form)

private fun request(url: String, method: String, headers: Map<String, String>, body: String?): String {
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
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
