package com.newsstream.ui.images

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import com.newsstream.ui.ViewSize
import kotlinx.coroutines.flow.distinctUntilChanged

/** Size of the small version every image has: prefetched as sources arrive, used for ×5 tiles. */
const val THUMBNAIL_PX = 240

/** Pixel size images are requested at for each view size, so each is downloaded and decoded once. */
fun pixelsFor(size: ViewSize): Int = when (size) {
    ViewSize.FULL -> 1080
    ViewSize.HALF -> 720
    ViewSize.THIRD -> 360
    ViewSize.FIFTH -> THUMBNAIL_PX
}

private fun cacheKey(url: String, px: Int) = "$px|$url"

/**
 * An image request at a fixed size with a stable memory-cache key, so prefetching and display
 * share one cached bitmap. Larger sizes show the small version of [placeholderUrl] (usually
 * already in memory) instantly while the sharper one loads.
 */
fun sizedRequest(context: Context, url: String, px: Int, placeholderUrl: String? = url): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(px)
        .memoryCacheKey(cacheKey(url, px))
        .apply {
            if (placeholderUrl != null && px != THUMBNAIL_PX) {
                placeholderMemoryCacheKey(cacheKey(placeholderUrl, THUMBNAIL_PX))
            }
        }
        .build()

fun thumbnailRequest(context: Context, url: String): ImageRequest = sizedRequest(context, url, THUMBNAIL_PX)

/**
 * Starts loading the images of the next [ahead] items below the screen while the user scrolls,
 * so they are ready by the time they appear. [firstHidden] is the index of the first item not
 * yet on screen; [urls] are the items' image URLs in list order.
 */
@Composable
fun PrefetchAhead(firstHidden: () -> Int, urls: List<String?>, px: Int, ahead: Int = 12) {
    val context = LocalContext.current
    val requested = remember(urls, px) { HashSet<String>() }
    LaunchedEffect(urls, px) {
        snapshotFlow(firstHidden).distinctUntilChanged().collect { start ->
            for (i in start.coerceAtLeast(0) until minOf(start + ahead, urls.size)) {
                val url = urls[i] ?: continue
                if (requested.add(url)) context.imageLoader.enqueue(sizedRequest(context, url, px))
            }
        }
    }
}
