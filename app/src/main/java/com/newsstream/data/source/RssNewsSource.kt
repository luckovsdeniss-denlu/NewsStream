package com.newsstream.data.source

import android.util.Xml
import com.newsstream.data.Article
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * Reads an RSS 2.0, RSS 1.0 (RDF) or Atom feed. Feeds carry headlines and summaries only,
 * not full text.
 */
class RssNewsSource(
    override val name: String,
    private val feedUrl: String,
    /** Topic for items that no keyword or category places elsewhere. */
    private val defaultTopic: Topic = Topic.WORLD,
    private val headers: Map<String, String> = emptyMap(),
    override val language: String = "en",
) : NewsSource {
    override val category = defaultTopic

    override fun fetch(): List<Article> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        // Feed bytes plus a null encoding let the parser honour the XML declaration
        // (some Russian feeds are windows-1251, not UTF-8).
        parser.setInput(ByteArrayInputStream(httpGetBytes(feedUrl, headers)), null)

        val articles = mutableListOf<Article>()
        var fields: MutableMap<String, String>? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (val tag = parser.name) {
                    "item", "entry" -> fields = mutableMapOf()
                    "link" -> fields?.let { readLink(parser, it) }
                    "media:content", "media:thumbnail", "enclosure" -> fields?.let { readImage(parser, it) }
                    // RSS: <category>Tech</category>; Atom: <category term="Tech"/>
                    "category" -> fields?.let {
                        val category = parser.getAttributeValue(null, "term") ?: parser.nextText()
                        it["category"] = listOfNotNull(it["category"], category.trim()).joinToString(" ")
                    }
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
        } else {
            when (parser.getAttributeValue(null, "rel")) {
                null, "alternate" -> fields.putIfAbsent("link", href.trim())
                "enclosure" -> if (parser.getAttributeValue(null, "type").orEmpty().startsWith("image")) {
                    fields.putIfAbsent("image", href.trim())
                }
            }
        }
    }

    // <media:content url=… medium="image">, <media:thumbnail url=…>, <enclosure url=… type="image/jpeg">
    private fun readImage(parser: XmlPullParser, fields: MutableMap<String, String>) {
        val url = parser.getAttributeValue(null, "url")?.takeIf { it.isNotBlank() } ?: return
        val type = parser.getAttributeValue(null, "type").orEmpty()
        val medium = parser.getAttributeValue(null, "medium").orEmpty()
        val isImage = parser.name == "media:thumbnail" || medium == "image" || type.startsWith("image") ||
            (type.isEmpty() && medium.isEmpty() && IMAGE_EXTENSION.containsMatchIn(url))
        if (!isImage) return
        fields.putIfAbsent(if (parser.name == "media:thumbnail") "thumb" else "image", url.trim())
    }

    private fun Map<String, String>.toArticle(): Article? {
        val title = this["title"]?.takeIf { it.isNotBlank() } ?: return null
        val link = this["link"]?.takeIf { it.isNotBlank() } ?: return null
        val date = this["pubDate"] ?: this["dc:date"] ?: this["published"] ?: this["updated"]
        val cleanTitle = htmlToText(title)
        // YouTube feeds put the video description in <media:description>.
        val rawSummary = this["description"] ?: this["summary"] ?: this["media:description"].orEmpty()
        val summary = htmlToText(rawSummary)
        // Many feeds embed the lead image as an <img> in the description instead.
        val image = this["image"] ?: INLINE_IMAGE.find(rawSummary)?.groupValues?.get(1) ?: this["thumb"]
        return Article(
            url = link,
            title = cleanTitle,
            summary = summary,
            body = emptyList(),
            source = name,
            publishedAt = parseFeedDate(date.orEmpty()),
            author = (this["dc:creator"] ?: this["name"])?.takeIf { it.isNotBlank() },
            imageUrl = image?.replace("&amp;", "&"),
            thumbnailUrl = this["thumb"]?.replace("&amp;", "&"),
            topic = TopicClassifier.classify(cleanTitle, summary, this["category"].orEmpty(), defaultTopic),
        )
    }

    private companion object {
        // "name" is the author's name inside an Atom <author> element.
        val TEXT_TAGS = setOf(
            "title", "description", "summary", "pubDate", "dc:date", "published", "updated", "dc:creator", "name",
            "media:description",
        )
        const val MAX_ARTICLES = 30
        val INLINE_IMAGE = Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val IMAGE_EXTENSION = Regex("\\.(jpe?g|png|webp|gif)(\\?|$)", RegexOption.IGNORE_CASE)
    }
}
