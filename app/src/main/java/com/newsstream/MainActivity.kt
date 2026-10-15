package com.newsstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.newsstream.data.Topic
import com.newsstream.ui.NewsViewModel
import com.newsstream.ui.article.ArticleScreen
import com.newsstream.ui.components.AppNavBar
import com.newsstream.ui.components.SourceActions
import com.newsstream.ui.components.Tab
import com.newsstream.ui.home.HomeScreen
import com.newsstream.ui.theme.NewsStreamTheme
import com.newsstream.ui.topics.TopicFeedScreen
import com.newsstream.ui.topics.TopicsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsStreamTheme {
                val newsViewModel: NewsViewModel = viewModel()
                val state by newsViewModel.state.collectAsStateWithLifecycle()
                var tab by rememberSaveable { mutableStateOf(Tab.FEED) }
                var openTopic by rememberSaveable { mutableStateOf<Topic?>(null) }
                var openArticleId by rememberSaveable { mutableStateOf<String?>(null) }
                val openArticle = state.articles.firstOrNull { it.id == openArticleId }

                val navBar: @Composable () -> Unit = {
                    AppNavBar(
                        selected = tab,
                        onSelect = {
                            // Tapping Topics again returns to the topic grid.
                            if (it == Tab.TOPICS) openTopic = null
                            tab = it
                        },
                    )
                }
                val topic = openTopic
                when {
                    openArticle != null -> ArticleScreen(openArticle, onBack = { openArticleId = null })
                    tab == Tab.FEED -> HomeScreen(
                        state = state,
                        onRefresh = newsViewModel::refresh,
                        onArticleClick = { openArticleId = it.id },
                        sourceActions = SourceActions(
                            setVisible = newsViewModel::setSourceVisible,
                            showAll = newsViewModel::showAllSources,
                            hideAll = newsViewModel::hideAllSources,
                            showOnly = newsViewModel::showOnlySource,
                        ),
                        bottomBar = navBar,
                    )
                    topic != null -> TopicFeedScreen(
                        topic = topic,
                        state = state,
                        onBack = { openTopic = null },
                        onRefresh = newsViewModel::refresh,
                        onArticleClick = { openArticleId = it.id },
                        bottomBar = navBar,
                    )
                    else -> TopicsScreen(state = state, onTopicClick = { openTopic = it }, bottomBar = navBar)
                }
            }
        }
    }
}
