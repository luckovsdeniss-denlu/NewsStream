package com.newsstream.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewModule
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.newsstream.data.ContentKind
import com.newsstream.data.Topic
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.ViewSize
import com.newsstream.ui.components.ArticleFeed
import com.newsstream.ui.components.SourceActions
import com.newsstream.ui.components.SourcesSheet
import com.newsstream.ui.icon
import com.newsstream.ui.images.ImageGrid
import com.newsstream.ui.youtube.YouTubeFeed
import com.newsstream.ui.theme.AppBackground

private enum class Mode(val label: String) {
    NEWS("News"),
    IMAGES("Images"),
    YOUTUBE("YouTube"),
    SAVED("Saved"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: NewsUiState,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    sourceActions: SourceActions,
    onToggleSave: (Article) -> Unit,
    onViewSizeChange: (ViewSize) -> Unit,
    onRefreshVideos: () -> Unit,
    onTurnOnChannels: (List<String>) -> Unit,
    onOpenInsights: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(Mode.NEWS) }
    var showSources by rememberSaveable { mutableStateOf(false) }
    if (showSources) {
        SourcesSheet(state, sourceActions, onDismiss = { showSources = false })
    }
    val sourcesFiltered = state.hiddenSources.isNotEmpty()
    val chooseSources: () -> Unit = { showSources = true }
    val noSourcesMessage = "No sources selected yet. Choose the sources you want to follow."
    // The selected topic (one at a time, empty for all), saved by name so it survives rotation.
    var topicNames by rememberSaveable { mutableStateOf("") }
    val selectedTopics = remember(topicNames) {
        topicNames.split(',').mapNotNull { name -> Topic.entries.firstOrNull { it.name == name } }.toSet()
    }
    // Selecting a topic replaces the previous one; selecting it again goes back to all topics.
    val toggleTopic: (Topic) -> Unit = { topic ->
        topicNames = if (topic in selectedTopics) "" else topic.name
    }

    // Videos live in the YouTube tab only.
    val stories = remember(state.articles) { state.articles.filter { it.kind != ContentKind.VIDEO } }
    val topicCounts = remember(stories) { stories.groupingBy { it.topic }.eachCount() }
    val visible = if (selectedTopics.isEmpty()) stories else stories.filter { it.topic in selectedTopics }

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
                        IconButton(onClick = onOpenInsights) {
                            Icon(
                                Icons.Filled.Insights,
                                contentDescription = "Insights",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        SizeButton(state.viewSize, onViewSizeChange)
                        IconButton(onClick = { showSources = true }) {
                            Icon(
                                Icons.Filled.RssFeed,
                                contentDescription = "Choose sources",
                                // Highlighted while some sources are switched off.
                                tint = if (sourcesFiltered) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                        // In the YouTube tab, refresh reloads only the channels; elsewhere only news.
                        UpdateButton(
                            isLoading = state.isLoading,
                            onClick = if (mode == Mode.YOUTUBE) onRefreshVideos else onRefresh,
                        )
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
                if (mode == Mode.YOUTUBE) {
                    YouTubeFeed(
                        state = state,
                        size = state.viewSize,
                        onLoad = onRefreshVideos,
                        onChooseChannels = chooseSources,
                        onTurnOnChannels = onTurnOnChannels,
                    )
                } else if (mode == Mode.SAVED) {
                    val saved = if (selectedTopics.isEmpty()) state.saved else state.saved.filter { it.topic in selectedTopics }
                    ArticleFeed(
                        articles = saved,
                        isLoading = false,
                        failedSources = emptyList(),
                        onRefresh = onRefresh,
                        onArticleClick = onArticleClick,
                        emptyMessage = if (state.saved.isEmpty()) {
                            "No saved stories yet. Tap the bookmark on any story to save it."
                        } else {
                            "No saved stories in these topics."
                        },
                        emptyActionLabel = null,
                        onTopicClick = toggleTopic,
                        selectedTopics = selectedTopics,
                        pageResetKey = "saved|$topicNames",
                        savedIds = state.savedIds,
                        onToggleSave = onToggleSave,
                        size = state.viewSize,
                    )
                } else if (mode == Mode.IMAGES) {
                    ImageGrid(
                        articles = visible,
                        isLoading = state.isLoading,
                        notLoadedYet = state.notLoadedYet,
                        onLoad = if (state.noSourcesSelected) chooseSources else onRefresh,
                        idleMessage = if (state.noSourcesSelected) noSourcesMessage else "No news loaded yet.",
                        idleActionLabel = if (state.noSourcesSelected) "Choose sources" else "Load news",
                        onArticleClick = onArticleClick,
                        resetKey = "$topicNames|${state.hiddenSources.sorted()}",
                        size = state.viewSize,
                    )
                } else {
                    ArticleFeed(
                        articles = visible,
                        isLoading = state.isLoading,
                        failedSources = state.failedSources,
                        onRefresh = if (state.noSourcesSelected) chooseSources else onRefresh,
                        onArticleClick = onArticleClick,
                        emptyMessage = if (state.noSourcesSelected) {
                            noSourcesMessage
                        } else if (state.notLoadedYet) {
                            "No news loaded yet."
                        } else if (state.allArticles.isNotEmpty() && state.articles.isEmpty()) {
                            "No sources selected. Tap the sources button to choose some."
                        } else if (state.articles.isEmpty()) {
                            "Couldn't load any news. Check your connection."
                        } else {
                            "No stories match these filters."
                        },
                        emptyActionLabel = when {
                            state.noSourcesSelected -> "Choose sources"
                            state.notLoadedYet -> "Load news"
                            else -> "Try again"
                        },
                        onTopicClick = toggleTopic,
                        selectedTopics = selectedTopics,
                        pageResetKey = "$topicNames|${state.hiddenSources.sorted()}",
                        savedIds = state.savedIds,
                        onToggleSave = onToggleSave,
                        size = state.viewSize,
                    )
                }
            }
        }
    }
}

/** One compact row: the News / Images / Saved switch and a single-choice topic button. */
@Composable
private fun FilterBar(
    mode: Mode,
    onModeSelected: (Mode) -> Unit,
    topicCounts: Map<Topic, Int>,
    selectedTopics: Set<Topic>,
    onToggleTopic: (Topic) -> Unit,
    onClearTopics: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeSwitch(mode, onModeSelected, Modifier.weight(1f))
        // The YouTube tab filters by channel group instead of topic.
        if (mode != Mode.YOUTUBE) {
            TopicButton(selectedTopics.firstOrNull(), topicCounts, onToggleTopic, onClearTopics)
        }
    }
}

/** Compact button showing the current topic; its menu picks exactly one topic, or all. */
@Composable
private fun TopicButton(
    selected: Topic?,
    topicCounts: Map<Topic, Int>,
    onSelect: (Topic) -> Unit,
    onClear: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (selected != null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .clickable { open = true }
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                selected?.icon ?: Icons.Filled.Category,
                contentDescription = "Topic",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                selected?.label ?: "All",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("All topics") },
                leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                trailingIcon = { if (selected == null) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                onClick = {
                    onClear()
                    open = false
                },
            )
            Topic.entries.forEach { topic ->
                val count = topicCounts[topic] ?: 0
                DropdownMenuItem(
                    text = { Text(if (count > 0) "${topic.label} ($count)" else topic.label) },
                    leadingIcon = { Icon(topic.icon, contentDescription = null) },
                    trailingIcon = { if (topic == selected) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                    enabled = count > 0 || topic == selected || topicCounts.isEmpty(),
                    onClick = {
                        if (topic != selected) onSelect(topic)
                        open = false
                    },
                )
            }
        }
    }
}

/** Refresh as a single sync icon that spins while sources are loading. */
@Composable
private fun UpdateButton(isLoading: Boolean, onClick: () -> Unit) {
    val rotation by rememberInfiniteTransition(label = "update").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "spin",
    )
    IconButton(onClick = onClick, enabled = !isLoading) {
        Icon(
            Icons.Filled.Sync,
            contentDescription = if (isLoading) "Updating" else "Update",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(if (isLoading) -rotation else 0f),
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
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Top-bar button choosing how big stories and images are: full screen, ×2, ×3 or ×5 smaller. */
@Composable
private fun SizeButton(selected: ViewSize, onSelect: (ViewSize) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.ViewModule, contentDescription = "Size", tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ViewSize.entries.forEach { size ->
                DropdownMenuItem(
                    text = { Text(size.label) },
                    trailingIcon = { if (size == selected) Icon(Icons.Filled.Check, contentDescription = "Selected") },
                    onClick = {
                        onSelect(size)
                        open = false
                    },
                )
            }
        }
    }
}
