package com.newsstream.ui.images

import android.content.Context
import coil.request.ImageRequest

/**
 * Pixel size grid tiles are decoded at. Prefetching and display use the same size so a
 * prefetched image is a memory-cache hit, and small decodes keep scrolling smooth.
 */
const val THUMBNAIL_PX = 240

fun thumbnailRequest(context: Context, url: String, sizePx: Int = THUMBNAIL_PX): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(sizePx)
        .build()
