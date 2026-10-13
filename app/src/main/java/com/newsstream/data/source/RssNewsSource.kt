package com.newsstream.data.source

import android.util.Xml
import com.newsstream.data.Article
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

/** Reads an RSS 2.0 feed. Feeds carry headlines and summaries only, not full text. */
class RssNewsSource(override val name: String, private val feedUrl: String) : NewsSource {

    override fun fetch(): List<Article> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(httpGet(feedUrl)))

        val articles = mutableListOf<Article>()
        var fields: MutableMap<String, String>? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> fields = mutableMapOf()
                    "title", "link", "description", "pubDate" ->
                        fields?.put(parser.name, parser.nextText().trim())
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") {
                    fields?.toArticle()?.let(articles::add)
                    fields = null
                }
            }
        }
        return articles
    }

    private fun Map<String, String>.toArticle(): Article? {
        val title = this["title"]?.takeIf { it.isNotBlank() } ?: return null
        val link = this["link"]?.takeIf { it.isNotBlank() } ?: return null
        return Article(
            url = link,
            title = htmlToText(title),
            summary = htmlToText(this["description"].orEmpty()),
            body = emptyList(),
            source = name,
            publishedAt = parseRssDate(this["pubDate"].orEmpty()),
        )
    }

    private fun parseRssDate(value: String): Long {
        for (pattern in listOf("EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz")) {
            runCatching { SimpleDateFormat(pattern, Locale.US).parse(value)?.time }
                .getOrNull()?.let { return it }
        }
        return 0L
    }
}
