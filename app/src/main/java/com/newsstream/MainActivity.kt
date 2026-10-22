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
import com.newsstream.data.ContentKind
import com.newsstream.ui.NewsViewModel
import com.newsstream.ui.article.ArticleScreen
import com.newsstream.ui.components.SourceActions
import com.newsstream.ui.home.HomeScreen
import com.newsstream.ui.insights.InsightsScreen
import com.newsstream.ui.insights.TermScreen
import com.newsstream.ui.youtube.VideoPlayerScreen
import com.newsstream.ui.theme.NewsStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsStreamTheme {
                val newsViewModel: NewsViewModel = viewModel()
                val state by newsViewModel.state.collectAsStateWithLifecycle()
                var openArticleId by rememberSaveable { mutableStateOf<String?>(null) }
                var showInsights by rememberSaveable { mutableStateOf(false) }
                // Word or phrase being analysed from Insights.
                var insightsTerm by rememberSaveable { mutableStateOf<String?>(null) }
                // Saved stories open even when they are no longer in the live feed.
                val openArticle = state.articles.firstOrNull { it.id == openArticleId }
                    ?: state.saved.firstOrNull { it.id == openArticleId }

                if (openArticle != null && openArticle.kind == ContentKind.VIDEO) {
                    // More from the same channel first, then the newest from other channels.
                    val others = state.articles.filter { it.kind == ContentKind.VIDEO && it.id != openArticle.id }
                    VideoPlayerScreen(
                        video = openArticle,
                        saved = openArticle.id in state.savedIds,
                        upNext = (others.filter { it.source == openArticle.source } + others.filter { it.source != openArticle.source })
                            .take(10),
                        onToggleSave = { newsViewModel.toggleSaved(openArticle) },
                        onPlay = { openArticleId = it.id },
                        onBack = { openArticleId = null },
                    )
                } else if (openArticle != null) {
                    ArticleScreen(
                        article = openArticle,
                        saved = openArticle.id in state.savedIds,
                        onToggleSave = { newsViewModel.toggleSaved(openArticle) },
                        onBack = { openArticleId = null },
                    )
                } else if (insightsTerm != null) {
                    TermScreen(
                        state = state,
                        term = insightsTerm!!,
                        onBack = { insightsTerm = null },
                        onArticleClick = { openArticleId = it.id },
                        onTermClick = { insightsTerm = it },
                    )
                } else if (showInsights) {
                    InsightsScreen(
                        state = state,
                        onBack = { showInsights = false },
                        onLoad = newsViewModel::refresh,
                        onArticleClick = { openArticleId = it.id },
                        onTermClick = { insightsTerm = it },
                    )
                } else {
                    HomeScreen(
                        state = state,
                        onRefresh = newsViewModel::refresh,
                        onArticleClick = { openArticleId = it.id },
                        sourceActions = SourceActions(
                            setVisible = newsViewModel::setSourceVisible,
                            setManyVisible = newsViewModel::setSourcesVisible,
                            hideAll = newsViewModel::hideAllSources,
                            setGroupExpanded = newsViewModel::setGroupExpanded,
                        ),
                        onToggleSave = newsViewModel::toggleSaved,
                        onViewSizeChange = newsViewModel::setViewSize,
                        onRefreshVideos = newsViewModel::refreshVideos,
                        onTurnOnChannels = newsViewModel::turnOnChannels,
                        onOpenInsights = { showInsights = true },
                        onRandomSources = { newsViewModel.pickRandomNewsSources() },
                    )
                }
            }
        }
    }
}
