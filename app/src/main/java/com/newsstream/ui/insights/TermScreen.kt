package com.newsstream.ui.insights

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.TermAnalysis
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.byline
import com.newsstream.ui.compactCount
import com.newsstream.ui.icon
import com.newsstream.ui.theme.AppBackground
import com.newsstream.ui.timeAgo

/**
 * One trending word or phrase in depth: how much it's covered, by which sources (with all their
 * stories), in which topics, over the last 24 hours, and which words travel with it.
 * Tapping a related word analyses that word instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermScreen(
    state: NewsUiState,
    term: String,
    onBack: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onTermClick: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    val analysis = remember(state.articles, state.sources, term) {
        TermAnalysis.compute(state.articles, state.sources, term)
    }
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "“$term”",
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "ANALYSIS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
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
        ) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (analysis.stories.isEmpty()) {
                    Text(
                        "No loaded headlines mention “$term” any more.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    return@Column
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Stories", compactCount(analysis.stories.size), Modifier.weight(1f))
                    StatTile("Sources", "${analysis.sources.size}", Modifier.weight(1f))
                    StatTile(
                        "Of all news",
                        percent((analysis.shareOfAll * 1000).toInt(), 1000),
                        Modifier.weight(1f),
                    )
                    StatTile("First seen", analysis.firstSeen?.timeAgo() ?: "—", Modifier.weight(1f))
                }
                analysis.firstSeen?.let { first ->
                    Section("First reported", "The earliest loaded headline with “$term”") {
                        StoryRow(first, showSource = true, onClick = { onArticleClick(first) })
                    }
                }
                Section("Coverage by source", "Stories mentioning “$term” per source") {
                    BarList(analysis.sources.map { (name, stories) -> Bar(name, stories.size, compactCount(stories.size)) })
                }
                Section("Topics", "Where “$term” shows up") {
                    BarList(
                        analysis.topics.map { (topic, count) ->
                            Bar(topic.label, count, "$count · ${percent(count, analysis.stories.size)}", topic.icon)
                        },
                    )
                }
                Section("Last 24 hours", "Stories mentioning “$term” per hour") { HourlyColumns(analysis.lastDay) }
                if (analysis.relatedWords.isNotEmpty()) {
                    Section("Related words", "Words that appear in the same headlines · tap to analyse") {
                        BarList(
                            analysis.relatedWords.map { (word, count) ->
                                Bar(word, count, "$count stories", onClick = { onTermClick(word) })
                            },
                        )
                    }
                }
                if (analysis.languages.size > 1) {
                    Section("Languages", "Stories per language") {
                        BarList(
                            analysis.languages.map { (code, count) ->
                                Bar(languageName(code), count, "$count · ${percent(count, analysis.stories.size)}")
                            },
                        )
                    }
                }
                Section("All sources", "Every story mentioning “$term”, grouped by source, earliest first") {
                    analysis.sources.forEachIndexed { index, (name, stories) ->
                        if (index > 0) Spacer(Modifier.height(6.dp))
                        Text(
                            "$name · ${stories.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        stories.forEach { story -> StoryRow(story, onClick = { onArticleClick(story) }) }
                    }
                }
            }
        }
    }
}

/** A headline with how long ago it was published; tap to open it. */
@Composable
private fun StoryRow(story: Article, showSource: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            story.timeAgo() ?: "—",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.width(64.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                story.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (showSource) {
                Text(
                    story.byline(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
