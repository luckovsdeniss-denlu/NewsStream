package com.newsstream

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class NewsStreamApp : Application(), ImageLoaderFactory {
    /**
     * One shared image loader for the whole app: a large memory cache so images already on
     * screen or prefetched appear instantly, and a disk cache that survives restarts. Feed
     * images rarely change, so cached copies are used regardless of the servers' cache headers.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(300L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(120)
        .build()
}
