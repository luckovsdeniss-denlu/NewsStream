package com.newsstream.ui.topics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.Topic
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.icon
import com.newsstream.ui.theme.AppBackground
import com.newsstream.ui.theme.panel

private data class TopicSummary(val topic: Topic, val count: Int, val latest: Article?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
    state: NewsUiState,
    onTopicClick: (Topic) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    // Busiest topics first; articles are already sorted newest first.
    val summaries = remember(state.articles) {
        val byTopic = state.articles.groupBy { it.topic }
        Topic.entries
            .map { topic -> byTopic[topic].orEmpty().let { TopicSummary(topic, it.size, it.firstOrNull()) } }
            .sortedByDescending { it.count }
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("TOPICS", color = MaterialTheme.colorScheme.primary)
                            Text(
                                "${state.articles.size} STORIES IN ${summaries.count { it.count > 0 }} TOPICS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            bottomBar = bottomBar,
        ) { pad ->
            if (state.articles.isEmpty() && state.isLoading) {
                Column(Modifier.fillMaxSize().padding(pad), Arrangement.Center, Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(summaries, key = { it.topic }) { summary ->
                        TopicTile(summary, onClick = { onTopicClick(summary.topic) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicTile(summary: TopicSummary, onClick: () -> Unit) {
    val empty = summary.count == 0
    val accent = if (empty) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .panel(accent)
            .clickable(enabled = !empty, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(summary.topic.icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Text(summary.topic.label, style = MaterialTheme.typography.titleMedium, color = accent)
        }
        Text(
            if (empty) "No stories yet" else "${summary.count} stories",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(2.dp))
        summary.latest?.let {
            Text(
                it.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
