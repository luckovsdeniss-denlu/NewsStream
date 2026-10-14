package com.newsstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Topic
import com.newsstream.ui.EngagementRow
import com.newsstream.ui.byline
import com.newsstream.ui.icon
import com.newsstream.ui.theme.panel

/** Article list with loading, empty and partial-failure states, shared by the feed and topic screens. */
@Composable
fun ArticleFeed(
    articles: List<Article>,
    isLoading: Boolean,
    failedSources: List<String>,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    modifier: Modifier = Modifier,
    showTopic: Boolean = true,
    emptyMessage: String = "Couldn't load any news. Check your connection.",
    /** When set, tapping a card's topic tag calls this (e.g. to toggle a topic filter). */
    onTopicClick: ((Topic) -> Unit)? = null,
    selectedTopics: Set<Topic> = emptySet(),
) {
    Box(modifier.fillMaxSize()) {
        when {
            articles.isNotEmpty() -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (failedSources.isNotEmpty()) {
                    item {
                        Text(
                            "Couldn't reach: ${failedSources.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                items(articles, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        showTopic = showTopic,
                        onClick = { onArticleClick(article) },
                        onTopicClick = onTopicClick,
                        topicSelected = article.topic in selectedTopics,
                    )
                }
            }
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> EmptyState(emptyMessage, onRefresh, Modifier.align(Alignment.Center))
        }
        if (isLoading && articles.isNotEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EmptyState(message: String, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Try again", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ArticleCard(
    article: Article,
    showTopic: Boolean,
    onClick: () -> Unit,
    onTopicClick: ((Topic) -> Unit)? = null,
    topicSelected: Boolean = false,
) {
    val social = article.kind == ContentKind.SOCIAL
    val accent = if (social) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .panel(accent)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showTopic) TopicTag(article.topic, topicSelected, onTopicClick)
            Text(
                article.byline().uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            article.title,
            style = if (social && article.summary.isEmpty()) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
        if (article.summary.isNotBlank()) {
            Text(
                article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        article.engagement?.let { EngagementRow(it) }
    }
}

@Composable
fun TopicTag(topic: Topic, selected: Boolean = false, onClick: ((Topic) -> Unit)? = null) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .then(if (onClick != null) Modifier.clickable { onClick(topic) } else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(topic.icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        Text(topic.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = content)
    }
}
