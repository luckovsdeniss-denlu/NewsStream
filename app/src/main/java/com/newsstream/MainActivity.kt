package com.newsstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.newsstream.data.SampleArticles
import com.newsstream.ui.article.ArticleScreen
import com.newsstream.ui.home.HomeScreen
import com.newsstream.ui.theme.NewsStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsStreamTheme {
                var openArticleId by rememberSaveable { mutableStateOf<String?>(null) }
                val openArticle = SampleArticles.all.firstOrNull { it.id == openArticleId }
                if (openArticle == null) {
                    HomeScreen(SampleArticles.all, onArticleClick = { openArticleId = it.id })
                } else {
                    ArticleScreen(openArticle, onBack = { openArticleId = null })
                }
            }
        }
    }
}
