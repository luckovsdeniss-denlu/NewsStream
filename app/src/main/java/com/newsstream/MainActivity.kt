package com.newsstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.newsstream.ui.NewsViewModel
import com.newsstream.ui.article.ArticleScreen
import com.newsstream.ui.components.SourceActions
import com.newsstream.ui.home.HomeScreen
import com.newsstream.ui.theme.NewsStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsStreamTheme {
                val newsViewModel: NewsViewModel = viewModel()
                val state by newsViewModel.state.collectAsStateWithLifecycle()
                var openArticleId by rememberSaveable { mutableStateOf<String?>(null) }
                val openArticle = state.articles.firstOrNull { it.id == openArticleId }

                if (openArticle != null) {
                    ArticleScreen(openArticle, onBack = { openArticleId = null })
                } else {
                    HomeScreen(
                        state = state,
                        onRefresh = newsViewModel::refresh,
                        onArticleClick = { openArticleId = it.id },
                        sourceActions = SourceActions(
                            setVisible = newsViewModel::setSourceVisible,
                            showAll = newsViewModel::showAllSources,
                            hideAll = newsViewModel::hideAllSources,
                            showLanguage = newsViewModel::showLanguage,
                        ),
                    )
                }
            }
        }
    }
}
