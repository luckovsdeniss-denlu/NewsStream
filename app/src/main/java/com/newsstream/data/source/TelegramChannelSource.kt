package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Topic
import com.newsstream.data.TopicClassifier

/**
 * A public Telegram channel's latest posts, read from its web preview page
 * (https://t.me/s/<channel>), which needs no account or key. The page is HTML meant for browsers,
 * so this parses its message blocks; if Telegram changes that markup the source shows as failed.
 */
class TelegramChannelSource(
    override val name: String,
    private val channel: String,
    override val category: Topic = Topic.GENERAL,
    override val language: String = "ru",
) : NewsSource {
    override val kind = ContentKind.SOCIAL

    override fun fetch(): List<Article> {
        val html = httpGet("https://t.me/s/$channel")
        return html.split("tgme_widget_message_wrap").drop(1).mapNotNull { block ->
            val post = POST.find(block)?.groupValues?.get(1) ?: return@mapNotNull null
            val text = TEXT.find(block)?.groupValues?.get(1)?.let(::htmlToText).orEmpty()
            val image = (PHOTO.find(block) ?: VIDEO_THUMB.find(block))?.groupValues?.get(1)
            if (text.isBlank() && image == null) return@mapNotNull null
            Article(
                url = "https://t.me/$post",
                title = text.ifBlank { name },
                summary = "",
                body = emptyList(),
                source = name,
                publishedAt = parseFeedDate(TIME.find(block)?.groupValues?.get(1).orEmpty()),
                kind = ContentKind.SOCIAL,
                topic = TopicClassifier.classify(text, fallback = category),
                imageUrl = image,
            )
        }
    }

    private companion object {
        val POST = Regex("data-post=\"([^\"]+)\"")
        val TEXT = Regex("<div class=\"tgme_widget_message_text[^\"]*\"[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
        val TIME = Regex("<time datetime=\"([^\"]+)\"")
        val PHOTO = Regex("tgme_widget_message_photo_wrap[^>]*background-image:url\\('([^']+)'\\)")
        val VIDEO_THUMB = Regex("tgme_widget_message_video_thumb[^>]*background-image:url\\('([^']+)'\\)")
    }
}
