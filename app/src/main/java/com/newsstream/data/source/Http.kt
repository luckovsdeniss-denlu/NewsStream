package com.newsstream.data.source

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal fun httpGet(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", "NewsStream/1.0 (Android)")
        val code = connection.responseCode
        if (code !in 200..299) throw IOException("HTTP $code from $url")
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
