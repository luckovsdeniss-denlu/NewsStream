package com.newsstream.data

import kotlinx.coroutines.delay

/**
 * Source of news. Replace [SampleNewsRepository] with a network-backed
 * implementation (e.g. Retrofit + a news API) without touching the UI.
 */
interface NewsRepository {
    suspend fun articles(category: Category): List<Article>
    suspend fun article(id: String): Article?
}

class SampleNewsRepository : NewsRepository {
    private val hls = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

    private val all = listOf(
        Article("1", "Live: Global leaders meet for climate summit", "Coverage of the opening session and key announcements.", Category.WORLD, "https://picsum.photos/seed/world/800/450", "NewsStream Live", "Just now", hls),
        Article("2", "New chip promises 2x battery life", "Engineers unveil an efficient mobile processor.", Category.TECH, "https://picsum.photos/seed/tech/800/450", "Tech Desk", "1h ago"),
        Article("3", "Match highlights: derby ends in dramatic draw", "Watch the best moments from last night.", Category.SPORT, "https://picsum.photos/seed/sport/800/450", "Sport Desk", "3h ago", hls),
        Article("4", "Markets rally as inflation cools", "Major indices close higher for a third day.", Category.BUSINESS, "https://picsum.photos/seed/biz/800/450", "Markets", "5h ago"),
    )

    override suspend fun articles(category: Category): List<Article> {
        delay(300)
        return if (category == Category.ALL) all else all.filter { it.category == category }
    }

    override suspend fun article(id: String): Article? = all.firstOrNull { it.id == id }
}
