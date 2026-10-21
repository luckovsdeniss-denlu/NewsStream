package com.newsstream.ui.insights

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.Insights
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.compactCount
import com.newsstream.ui.byline
import com.newsstream.ui.icon
import com.newsstream.ui.theme.AppBackground
import com.newsstream.ui.theme.panel
import java.util.Locale

/**
 * Statistics about everything loaded: volume, topics, sources, trending words and stories,
 * the last 24 hours, engagement and languages. All computed on the device.
 *
 * Bars use one hue (magnitude, not identity) and every bar is labelled with its name and value,
 * so nothing relies on colour alone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    state: NewsUiState,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onArticleClick: (Article) -> Unit,
) {
    BackHandler(onBack = onBack)
    val insights = remember(state.articles, state.sources, state.hiddenSources) {
        Insights.compute(state.articles, state.sources, state.hiddenSources)
    }
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = { Text("INSIGHTS", color = MaterialTheme.colorScheme.primary) },
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
            if (insights.storyCount == 0) {
                Column(
                    Modifier.fillMaxSize().padding(pad).padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Load some news first to see insights.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onLoad) { Text("Load news") }
                }
                return@Scaffold
            }
            Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Overview(insights)
                Section("Topics", "Stories per topic") {
                    val total = insights.storyCount
                    BarList(
                        insights.topics.map { (topic, count) ->
                            Bar(topic.label, count, "${compactCount(count)} · ${percent(count, total)}", topic.icon)
                        },
                    )
                }
                Section("Top sources", "Sources contributing the most stories") {
                    BarList(insights.topSources.map { (name, count) -> Bar(name, count, compactCount(count)) })
                }
                if (insights.bigStories.isNotEmpty()) {
                    Section("Big stories", "Phrases in headlines from 3 or more different sources") {
                        BarList(insights.bigStories.map { (phrase, sources) -> Bar(phrase, sources, "$sources sources") })
                    }
                }
                if (insights.trendingWords.isNotEmpty()) {
                    Section("Trending words", "Headline words used by the most different sources") {
                        BarList(insights.trendingWords.map { (word, sources) -> Bar(word, sources, "$sources sources") })
                    }
                }
                Section("Last 24 hours", "Stories published per hour") { HourlyColumns(insights.lastDay) }
                if (insights.mostEngaged.isNotEmpty()) {
                    Section("Most engaged posts", "Social posts with the most likes, comments and shares") {
                        insights.mostEngaged.forEach { post -> EngagedPost(post, onClick = { onArticleClick(post) }) }
                    }
                }
                Section("Languages", "Stories per language") {
                    BarList(
                        insights.languages.map { (code, count) ->
                            Bar(languageName(code), count, "${compactCount(count)} · ${percent(count, insights.storyCount)}")
                        },
                    )
                }
            }
        }
    }
}

private class Bar(val label: String, val value: Int, val valueText: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null)

private fun percent(part: Int, total: Int) =
    if (total == 0) "0%" else String.format(Locale.US, "%.0f%%", part * 100f / total)

private fun languageName(code: String) = when (code) {
    "en" -> "English"
    "ru" -> "Русский"
    else -> code
}

/** Headline numbers as a row of stat tiles. */
@Composable
private fun Overview(insights: Insights) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Stories", compactCount(insights.storyCount), Modifier.weight(1f))
        StatTile("Sources", "${insights.sourcesResponding}/${insights.sourcesSelected}", Modifier.weight(1f))
        StatTile("Topics", "${insights.topicCount}", Modifier.weight(1f))
        StatTile("With images", percent((insights.withImagesShare * 1000).toInt(), 1000), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.panel(MaterialTheme.colorScheme.primary).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Section(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().panel(MaterialTheme.colorScheme.primary).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/**
 * Horizontal bars, longest first, in a single hue: label on the left, a bar scaled to the
 * largest value, and the value written out on the right (so it never relies on bar length alone).
 */
@Composable
private fun BarList(bars: List<Bar>) {
    val max = bars.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bars.forEach { bar ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.weight(0.4f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    bar.icon?.let {
                        Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        bar.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.weight(0.4f).height(12.dp)) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(bar.value.toFloat() / max)
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    bar.valueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(0.2f),
                )
            }
        }
    }
}

/** 24 hourly columns (oldest left); tap a column to see its count. */
@Composable
private fun HourlyColumns(counts: List<Int>) {
    val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    var selected by remember { mutableStateOf<Int?>(null) }
    val shown = selected
    Text(
        if (shown == null) {
            "Peak: ${counts.maxOrNull() ?: 0} stories in one hour · tap a column for details"
        } else {
            val hoursAgo = 23 - shown
            "${counts[shown]} stories ${if (hoursAgo == 0) "in the last hour" else "$hoursAgo–${hoursAgo + 1} hours ago"}"
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
        Modifier.fillMaxWidth().height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        counts.forEachIndexed { index, count ->
            // The whole column height is the tap target, not just the bar.
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable { selected = if (selected == index) null else index },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((count.toFloat() / max).coerceAtLeast(if (count > 0) 0.03f else 0f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (selected == null || selected == index) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            },
                        ),
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Text("24h ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("12h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EngagedPost(post: Article, onClick: () -> Unit) {
    val e = post.engagement ?: return
    val total = (e.likes ?: 0) + (e.comments ?: 0) + (e.shares ?: 0)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            compactCount(total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(52.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(post.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                post.byline(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
