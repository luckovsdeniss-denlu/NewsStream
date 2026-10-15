package com.newsstream.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.Topic
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.components.ArticleFeed
import com.newsstream.ui.components.SourceActions
import com.newsstream.ui.components.SourcesSheet
import com.newsstream.ui.icon
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
                        IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
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
                        emptyMessage = if (state.allArticles.isNotEmpty() && state.articles.isEmpty()) {
                            "No sources selected. Tap the sources button to choose some."
                        } else if (state.articles.isEmpty()) {
                            "Couldn't load any news. Check your connection."
                        } else {
                            "No stories match these filters."
                        },
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
 * One fixed row: a News / Images switch and the topics selector (multiple choice; the menu
 * stays open while ticking). Topics apply to both views.
 */
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
    ) {
        ModeSwitch(mode, onModeSelected, Modifier.weight(1f))
        Box(Modifier.weight(1f)) {
            var open by remember { mutableStateOf(false) }
            val value = when (selectedTopics.size) {
                0 -> "All"
                1 -> selectedTopics.first().label
                else -> "${selectedTopics.first().label} +${selectedTopics.size - 1}"
            }
            SelectorButton("TOPICS", value, active = selectedTopics.isNotEmpty(), onClick = { open = true })
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text("All topics") },
                    onClick = {
                        onClearTopics()
                        open = false
                    },
                    leadingIcon = { CheckMark(selectedTopics.isEmpty()) },
                )
                HorizontalDivider()
                Topic.entries.forEach { topic ->
                    val count = topicCounts[topic] ?: 0
                    DropdownMenuItem(
                        text = { Text("${topic.label} ($count)") },
                        onClick = { onToggleTopic(topic) },
                        leadingIcon = { Checkbox(checked = topic in selectedTopics, onCheckedChange = null) },
                        trailingIcon = { Icon(topic.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        enabled = count > 0 || topic in selectedTopics,
                    )
                }
            }
        }
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

@Composable
private fun SelectorButton(title: String, value: String, active: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CheckMark(checked: Boolean) {
    if (checked) {
        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
    } else {
        Spacer(Modifier.size(24.dp))
    }
}
