package com.newsstream.data.source

import android.util.Xml
import com.newsstream.data.Article
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Reads an RSS 2.0, RSS 1.0 (RDF) or Atom feed. Feeds carry headlines and summaries only,
 * not full text.
 */
class RssNewsSource(override val name: String, private val feedUrl: String) : NewsSource {

    override fun fetch(): List<Article> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(httpGet(feedUrl)))

        val articles = mutableListOf<Article>()
        var fields: MutableMap<String, String>? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (val tag = parser.name) {
                    "item", "entry" -> fields = mutableMapOf()
                    "link" -> fields?.let { readLink(parser, it) }
                    in TEXT_TAGS -> fields?.putIfAbsent(tag, parser.nextText().trim())
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" || parser.name == "entry") {
                    fields?.toArticle()?.let(articles::add)
                    fields = null
                }
            }
        }
        return articles.take(MAX_ARTICLES)
    }

    // RSS puts the URL in the element text; Atom puts it in href, possibly on several
    // <link> elements of which rel="alternate" (the default) is the article page.
    private fun readLink(parser: XmlPullParser, fields: MutableMap<String, String>) {
        val href = parser.getAttributeValue(null, "href")
        if (href == null) {
            fields.putIfAbsent("link", parser.nextText().trim())
        } else if (parser.getAttributeValue(null, "rel").let { it == null || it == "alternate" }) {
            fields.putIfAbsent("link", href.trim())
        }
    }

    private fun Map<String, String>.toArticle(): Article? {
        val title = this["title"]?.takeIf { it.isNotBlank() } ?: return null
        val link = this["link"]?.takeIf { it.isNotBlank() } ?: return null
        val date = this["pubDate"] ?: this["dc:date"] ?: this["published"] ?: this["updated"]
        return Article(
            url = link,
            title = htmlToText(title),
            summary = htmlToText(this["description"] ?: this["summary"].orEmpty()),
            body = emptyList(),
            source = name,
            publishedAt = parseFeedDate(date.orEmpty()),
        )
    }

    private companion object {
        val TEXT_TAGS = setOf("title", "description", "summary", "pubDate", "dc:date", "published", "updated")
        const val MAX_ARTICLES = 30
    }
}
