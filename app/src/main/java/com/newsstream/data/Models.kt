package com.newsstream.data

enum class Category(val label: String) {
    ALL("Top"), WORLD("World"), TECH("Tech"), SPORT("Sport"), BUSINESS("Business")
}

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val category: Category,
    val imageUrl: String,
    val source: String,
    val publishedAt: String,
    /** HLS/MP4 stream URL; null for text-only articles. */
    val streamUrl: String? = null,
)
