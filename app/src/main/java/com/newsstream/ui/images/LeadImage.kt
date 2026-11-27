package com.newsstream.ui.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.newsstream.data.Article
import com.newsstream.data.source.PageImageFinder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What is known about a story's picture. */
sealed interface LeadImage {
    data object Searching : LeadImage
    data object None : LeadImage
    data class Found(val url: String) : LeadImage
}

/** Pictures found on article pages, remembered for the rest of the session ("" = page has none). */
private val pageImages = ConcurrentHashMap<String, String>()

/**
 * The story's picture: the feed's own image when it has one, otherwise the lead image the article
 * page declares for link previews, looked up in the background and remembered.
 */
@Composable
fun rememberLeadImage(article: Article): State<LeadImage> {
    val own = article.imageUrl ?: article.gridImageUrl
    val known = own ?: pageImages[article.url]
    val initial = when {
        known == null -> LeadImage.Searching
        known.isEmpty() -> LeadImage.None
        else -> LeadImage.Found(known)
    }
    return produceState(initial, article.url) {
        if (value != LeadImage.Searching) return@produceState
        val found = withContext(Dispatchers.IO) { runCatching { PageImageFinder.find(article.url) }.getOrNull() }
        pageImages[article.url] = found.orEmpty()
        value = if (found.isNullOrEmpty()) LeadImage.None else LeadImage.Found(found)
    }
}
