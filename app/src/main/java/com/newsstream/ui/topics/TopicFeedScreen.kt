package com.newsstream.ui.topics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.Topic
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.components.ArticleFeed
import com.newsstream.ui.icon
import com.newsstream.ui.theme.AppBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicFeedScreen(
    topic: Topic,
    state: NewsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val articles = remember(state.articles, topic) { state.articles.filter { it.topic == topic } }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(topic.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(topic.label.uppercase(), color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "${articles.size} STORIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            bottomBar = bottomBar,
        ) { pad ->
            ArticleFeed(
                articles = articles,
                isLoading = state.isLoading,
                failedSources = emptyList(),
                onRefresh = onRefresh,
                onArticleClick = onArticleClick,
                modifier = Modifier.padding(pad),
                showTopic = false,
                emptyMessage = "No ${topic.label.lowercase()} stories right now.",
                pageResetKey = topic,
            )
        }
    }
}
