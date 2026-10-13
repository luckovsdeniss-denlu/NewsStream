package com.newsstream.data

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val body: List<String>,
    val source: String,
    val sourceUrl: String,
    val publishedDate: String,
)
