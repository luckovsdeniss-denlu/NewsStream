package com.newsstream.data.source

import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Topic

/**
 * A YouTube channel's latest videos from its public Atom feed
 * (https://www.youtube.com/feeds/videos.xml?channel_id=…): no API key needed. Each video becomes
 * an article of kind VIDEO with its thumbnail, linking to the video on YouTube.
 */
class YouTubeChannelSource(
    channel: String,
    private val channelId: String,
    override val category: Topic,
    override val language: String = "en",
) : NewsSource {
    override val kind = ContentKind.VIDEO

    // Suffixed so a channel never clashes with a news feed of the same name (e.g. "BBC News");
    // the UI drops the suffix when showing it (see displaySourceName).
    override val name = channel + YOUTUBE_SUFFIX

    override fun fetch(): List<Article> =
        RssNewsSource(
            name,
            "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId",
            category,
            language = language,
        ).fetch().map { video ->
            video.copy(
                kind = ContentKind.VIDEO,
                // The feed's author is the channel itself; the source name already says that.
                author = null,
                // Feed thumbnails are 480x360 with letterbox bars; the 16:9 "mqdefault" fits cards.
                thumbnailUrl = video.thumbnailUrl?.replace("hqdefault.jpg", "mqdefault.jpg"),
            )
        }
}

internal const val YOUTUBE_SUFFIX = " (YouTube)"
