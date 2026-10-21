package com.newsstream.ui.insights

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.TermAnalysis
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.byline
import com.newsstream.ui.compactCount
import com.newsstream.ui.icon
import com.newsstream.ui.images.thumbnailRequest
import com.newsstream.ui.theme.AppBackground
import com.newsstream.ui.timeAgo
import java.util.Locale

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
                val m = analysis.momentum
                Section("Momentum", "Last 6 hours compared with the 18 hours before") {
                    Text(
                        m.trend.label.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${m.recent} stories in the last 6 hours (${perHour(m.recentPerHour)}) · " +
                            "${m.earlier} in the 18 hours before (${perHour(m.earlierPerHour)})",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                analysis.firstSeen?.let { first ->
                    Section("First reported", "The earliest loaded headline with “$term”") {
                        StoryRow(first, showSource = true, onClick = { onArticleClick(first) })
                    }
                }
                if (analysis.pickup.size > 1) {
                    val withinHour = analysis.pickup.count { it.afterFirstMs < 3_600_000L }
                    Section(
                        "Spread",
                        "How fast sources picked it up · $withinHour of ${analysis.pickup.size} within the first hour, " +
                            "all within ${duration(analysis.pickup.last().afterFirstMs)}",
                    ) {
                        analysis.pickup.forEachIndexed { index, pick ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    if (index == 0) "first" else "+${duration(pick.afterFirstMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(72.dp),
                                )
                                Text(pick.source, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
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
                if (analysis.contexts.isNotEmpty()) {
                    Section("In context", "Phrases “$term” appears in · tap to analyse") {
                        BarList(
                            analysis.contexts.map { (phrase, count) ->
                                Bar(phrase, count, "$count stories", onClick = { onTermClick(phrase) })
                            },
                        )
                    }
                }
                if (analysis.relatedWords.isNotEmpty()) {
                    Section("Related words", "Words that appear in the same headlines · tap to analyse") {
                        BarList(
                            analysis.relatedWords.map { (word, count) ->
                                Bar(word, count, "$count stories", onClick = { onTermClick(word) })
                            },
                        )
                    }
                }
                val tone = analysis.tone
                Section("Tone", "Rough word-based reading of headlines and summaries") {
                    val n = analysis.stories.size
                    BarList(
                        listOf(
                            Bar("Positive", tone.positive, "${tone.positive} · ${percent(tone.positive, n)}"),
                            Bar("Neutral", tone.neutral, "${tone.neutral} · ${percent(tone.neutral, n)}"),
                            Bar("Negative", tone.negative, "${tone.negative} · ${percent(tone.negative, n)}"),
                        ),
                    )
                }
                if (analysis.kinds.size > 1) {
                    Section("News vs social", "Articles compared with social posts") {
                        BarList(
                            analysis.kinds.map { (kind, count) ->
                                Bar(kindName(kind), count, "$count · ${percent(count, analysis.stories.size)}")
                            },
                        )
                    }
                }
                analysis.engagement?.let { e ->
                    Section("Social engagement", "Totals over ${e.posts} social posts mentioning “$term”") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile("Likes", compactCount(e.likes), Modifier.weight(1f))
                            StatTile("Comments", compactCount(e.comments), Modifier.weight(1f))
                            StatTile("Shares", compactCount(e.shares), Modifier.weight(1f))
                        }
                        Text("Top post", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StoryRow(e.top, showSource = true, onClick = { onArticleClick(e.top) })
                    }
                }
                if (analysis.withImages.isNotEmpty()) {
                    Section("Pictures", "${analysis.withImages.size} stories with images · tap to open") {
                        val context = LocalContext.current
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(analysis.withImages, key = { it.id }) { story ->
                                AsyncImage(
                                    model = thumbnailRequest(context, story.gridImageUrl!!),
                                    contentDescription = story.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onArticleClick(story) },
                                )
                            }
                        }
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
                if (analysis.textMentions.isNotEmpty()) {
                    Section(
                        "Also mentioned in text",
                        "${analysis.textMentions.size} more stories mention “$term” in the summary or text, not the headline",
                    ) {
                        analysis.textMentions.take(10).forEach { story ->
                            StoryRow(story, showSource = true, onClick = { onArticleClick(story) })
                        }
                        if (analysis.textMentions.size > 10) {
                            Text(
                                "+${analysis.textMentions.size - 10} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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

private fun perHour(rate: Float) = String.format(Locale.US, "%.1f/h", rate)

/** 45 min, 3 h 10 min, 2 d 4 h. */
private fun duration(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 48 * 60 -> if (minutes % 60 == 0L) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60} min"
        else -> "${minutes / (24 * 60)} d ${minutes / 60 % 24} h"
    }
}

private fun kindName(kind: ContentKind) = when (kind) {
    ContentKind.NEWS -> "News articles"
    ContentKind.SOCIAL -> "Social posts"
    ContentKind.VIDEO -> "Videos"
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
