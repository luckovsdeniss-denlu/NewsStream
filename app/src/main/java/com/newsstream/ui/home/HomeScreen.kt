package com.newsstream.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Topic
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.components.ArticleFeed
import com.newsstream.ui.icon
import com.newsstream.ui.theme.AppBackground

private enum class Filter(val label: String, val kind: ContentKind?) {
    ALL("ALL", null),
    NEWS("NEWS", ContentKind.NEWS),
    SOCIAL("SOCIAL", ContentKind.SOCIAL),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: NewsUiState,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    // Selected topics, saved as comma-separated enum names so they survive rotation.
    var topicNames by rememberSaveable { mutableStateOf("") }
    val selectedTopics = remember(topicNames) {
        topicNames.split(',').mapNotNull { name -> Topic.entries.firstOrNull { it.name == name } }.toSet()
    }
    val toggleTopic: (Topic) -> Unit = { topic ->
        val updated = if (topic in selectedTopics) selectedTopics - topic else selectedTopics + topic
        topicNames = updated.joinToString(",") { it.name }
    }

    val byKind = filter.kind?.let { kind -> state.articles.filter { it.kind == kind } } ?: state.articles
    val topicCounts = remember(byKind) { byKind.groupingBy { it.topic }.eachCount() }
    val visible = if (selectedTopics.isEmpty()) byKind else byKind.filter { it.topic in selectedTopics }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("NEWSSTREAM", color = MaterialTheme.colorScheme.primary)
                            Text(
                                if (visible.size == state.articles.size) {
                                    "LIVE · ${state.articles.size} STORIES"
                                } else {
                                    "LIVE · ${visible.size} OF ${state.articles.size} STORIES"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            bottomBar = bottomBar,
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                FilterRow(filter, onSelect = { filter = it })
                TopicFilterRow(
                    counts = topicCounts,
                    selected = selectedTopics,
                    onToggle = toggleTopic,
                    onClear = { topicNames = "" },
                )
                ArticleFeed(
                    articles = visible,
                    isLoading = state.isLoading,
                    failedSources = state.failedSources,
                    onRefresh = onRefresh,
                    onArticleClick = onArticleClick,
                    emptyMessage = if (state.articles.isEmpty()) {
                        "Couldn't load any news. Check your connection."
                    } else {
                        "No stories match these filters."
                    },
                    onTopicClick = toggleTopic,
                    selectedTopics = selectedTopics,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(selected: Filter, onSelect: (Filter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Filter.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.label, style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    labelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

/** Multi-select topic chips with live counts; topics with no stories are hidden unless selected. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicFilterRow(
    counts: Map<Topic, Int>,
    selected: Set<Topic>,
    onToggle: (Topic) -> Unit,
    onClear: () -> Unit,
) {
    val topics = Topic.entries
        .filter { (counts[it] ?: 0) > 0 || it in selected }
        .sortedByDescending { counts[it] ?: 0 }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        labelColor = MaterialTheme.colorScheme.primary,
        iconColor = MaterialTheme.colorScheme.primary,
        containerColor = MaterialTheme.colorScheme.surface,
    )
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = onClear,
                label = { Text("All topics", style = MaterialTheme.typography.labelLarge) },
                colors = chipColors,
            )
        }
        items(topics, key = { it.name }) { topic ->
            FilterChip(
                selected = topic in selected,
                onClick = { onToggle(topic) },
                label = { Text("${topic.label} ${counts[topic] ?: 0}", style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { Icon(topic.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = chipColors,
            )
        }
    }
}
