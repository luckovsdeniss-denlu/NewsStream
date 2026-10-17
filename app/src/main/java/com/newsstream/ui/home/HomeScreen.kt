package com.newsstream.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.Topic
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.components.ArticleFeed
import com.newsstream.ui.components.SourceActions
import com.newsstream.ui.components.SourcesSheet
import com.newsstream.ui.images.ImageGrid
import com.newsstream.ui.theme.AppBackground

private enum class Mode(val label: String) {
    NEWS("News"),
    IMAGES("Images"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: NewsUiState,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    sourceActions: SourceActions,
) {
    var mode by rememberSaveable { mutableStateOf(Mode.NEWS) }
    var showSources by rememberSaveable { mutableStateOf(false) }
    if (showSources) {
        SourcesSheet(state, sourceActions, onDismiss = { showSources = false })
    }
    val sourcesFiltered = state.hiddenSources.isNotEmpty()
    // Selected topics, saved as comma-separated enum names so they survive rotation.
    var topicNames by rememberSaveable { mutableStateOf("") }
    val selectedTopics = remember(topicNames) {
        topicNames.split(',').mapNotNull { name -> Topic.entries.firstOrNull { it.name == name } }.toSet()
    }
    val toggleTopic: (Topic) -> Unit = { topic ->
        val updated = if (topic in selectedTopics) selectedTopics - topic else selectedTopics + topic
        topicNames = updated.joinToString(",") { it.name }
    }

    val topicCounts = remember(state.articles) { state.articles.groupingBy { it.topic }.eachCount() }
    val visible = if (selectedTopics.isEmpty()) state.articles else state.articles.filter { it.topic in selectedTopics }

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
                                buildString {
                                    append("LIVE · ")
                                    if (visible.size != state.articles.size) append("${visible.size} OF ")
                                    append("${state.articles.size} STORIES")
                                    if (state.isLoading) {
                                        append(" · LOADING ${state.loadingTotal - state.loadingSources}/${state.loadingTotal}")
                                    } else if (sourcesFiltered) {
                                        val shown = state.sources.count { it.name !in state.hiddenSources }
                                        append(" · $shown/${state.sources.size} SOURCES")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSources = true }) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = "Choose sources",
                                // Highlighted while some sources are switched off.
                                tint = if (sourcesFiltered) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                        UpdateButton(isLoading = state.isLoading, onClick = onRefresh)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                FilterBar(
                    mode = mode,
                    onModeSelected = { mode = it },
                    topicCounts = topicCounts,
                    selectedTopics = selectedTopics,
                    onToggleTopic = toggleTopic,
                    onClearTopics = { topicNames = "" },
                )
                if (mode == Mode.IMAGES) {
                    ImageGrid(
                        articles = visible,
                        isLoading = state.isLoading,
                        notLoadedYet = state.notLoadedYet,
                        onLoad = onRefresh,
                        onArticleClick = onArticleClick,
                        resetKey = "$topicNames|${state.hiddenSources.sorted()}",
                    )
                } else {
                    ArticleFeed(
                        articles = visible,
                        isLoading = state.isLoading,
                        failedSources = state.failedSources,
                        onRefresh = onRefresh,
                        onArticleClick = onArticleClick,
                        emptyMessage = if (state.notLoadedYet) {
                            "No news loaded yet."
                        } else if (state.allArticles.isNotEmpty() && state.articles.isEmpty()) {
                            "No sources selected. Tap the sources button to choose some."
                        } else if (state.articles.isEmpty()) {
                            "Couldn't load any news. Check your connection."
                        } else {
                            "No stories match these filters."
                        },
                        emptyActionLabel = if (state.notLoadedYet) "Load news" else "Try again",
                        onTopicClick = toggleTopic,
                        selectedTopics = selectedTopics,
                        pageResetKey = "$topicNames|${state.hiddenSources.sorted()}",
                    )
                }
            }
        }
    }
}

/**
 * The News / Images switch, then every topic as a flat, borderless label wrapping over as
 * many lines as needed (no menus, no sideways scrolling). Tapping labels selects several
 * topics; "All" clears the selection. Topics apply to both views.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(
    mode: Mode,
    onModeSelected: (Mode) -> Unit,
    topicCounts: Map<Topic, Int>,
    selectedTopics: Set<Topic>,
    onToggleTopic: (Topic) -> Unit,
    onClearTopics: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        ModeSwitch(mode, onModeSelected, Modifier.fillMaxWidth())
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FlatTopic("All", count = null, selected = selectedTopics.isEmpty(), enabled = true, onClick = onClearTopics)
            Topic.entries.forEach { topic ->
                val count = topicCounts[topic] ?: 0
                FlatTopic(
                    label = topic.label,
                    count = count.takeIf { it > 0 },
                    selected = topic in selectedTopics,
                    enabled = count > 0 || topic in selectedTopics || topicCounts.isEmpty(),
                    onClick = { onToggleTopic(topic) },
                )
            }
        }
    }
}

/** A borderless text label; selected ones get a soft filled background. */
@Composable
private fun FlatTopic(label: String, count: Int?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
        if (count != null) {
            Text("$count", style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

/** Refresh as a labelled pill whose sync arrows spin while sources are loading. */
@Composable
private fun UpdateButton(isLoading: Boolean, onClick: () -> Unit) {
    val rotation by rememberInfiniteTransition(label = "update").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "spin",
    )
    Row(
        Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Filled.Sync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp).rotate(if (isLoading) -rotation else 0f),
        )
        Text(
            if (isLoading) "Updating" else "Update",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Two-segment switch between the story list and the image grid. */
@Composable
private fun ModeSwitch(selected: Mode, onSelect: (Mode) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .height(48.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(3.dp),
    ) {
        Mode.entries.forEach { option ->
            val active = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (option == Mode.NEWS) Icons.Filled.Newspaper else Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
