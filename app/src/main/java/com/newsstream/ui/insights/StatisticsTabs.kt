package com.newsstream.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.Insights
import com.newsstream.data.Statistics
import com.newsstream.ui.compactCount
import com.newsstream.ui.icon
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private fun f1(value: Float) = String.format(Locale.US, "%.1f", value)

private fun signedPercent(value: Float): String {
    val p = (value * 100).roundToInt()
    return if (p > 0) "+$p%" else if (p < 0) "−${-p}%" else "0%"
}

/** Words over time: bursts, the hourly chart with its moving average, growth, keyness and pairs. */
@Composable
internal fun TrendsTab(stats: Statistics, insights: Insights, onTermClick: (String) -> Unit) {
    Section("Bursting now", "Words in far more headlines in the last 3 hours than in the 21 before (z-score of 2 or more) · tap to analyse") {
        if (stats.spikes.isEmpty()) {
            EmptyNote("No unusual bursts right now.")
        } else {
            BarList(
                stats.spikes.map {
                    Bar(it.word, (it.z * 10).roundToInt(), "z ${f1(it.z)} · ${it.recent} vs ~${f1(it.baseline)}", onClick = { onTermClick(it.word) })
                },
            )
        }
    }
    Section("Last 24 hours", "Stories published per hour") { HourlyColumns(insights.lastDay) }
    Section("Rising", "Mention rate in the last 6 hours compared with the 18 before · tap to analyse") {
        if (stats.rising.isEmpty()) {
            EmptyNote("Nothing is clearly rising yet.")
        } else {
            BarList(
                stats.rising.map {
                    Bar(it.word, (it.change * 100).roundToInt(), "${signedPercent(it.change)} · ${it.recent} vs ${it.earlier}", onClick = { onTermClick(it.word) })
                },
            )
        }
    }
    Section("Fading", "Words mentioned much less in the last 6 hours than the 18 before · tap to analyse") {
        if (stats.fading.isEmpty()) {
            EmptyNote("Nothing is clearly fading.")
        } else {
            BarList(
                stats.fading.map {
                    Bar(it.word, (-it.change * 100).roundToInt(), "${signedPercent(it.change)} · ${it.recent} vs ${it.earlier}", onClick = { onTermClick(it.word) })
                },
            )
        }
    }
    Section("New this period", "Words used significantly more in the last 6 hours (log-likelihood G², p < 0.01) · tap to analyse") {
        if (stats.keywords.isEmpty()) {
            EmptyNote("Needs stories from both the last 6 hours and the 18 before.")
        } else {
            BarList(
                stats.keywords.map {
                    Bar(it.word, it.g2.roundToInt(), "G² ${f1(it.g2)} · ${it.recent} vs ${it.earlier}", onClick = { onTermClick(it.word) })
                },
            )
        }
    }
    Section("Word pairs", "Words that appear together far more than chance (normalised PMI, 3+ headlines, 2+ sources)") {
        if (stats.wordPairs.isEmpty()) {
            EmptyNote("Not enough repeated pairs yet.")
        } else {
            BarList(
                stats.wordPairs.map {
                    Bar("${it.first} + ${it.second}", (it.npmi * 100).roundToInt(), "${it.together}× · ${f1(it.npmi)}")
                },
            )
        }
    }
}

/** Story clusters, diversity and exclusives. */
@Composable
internal fun StoriesTab(stats: Statistics, onArticleClick: (Article) -> Unit) {
    Section("Diversity", "How evenly the news is spread (Shannon evenness: 100% = perfectly even)") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Topics", "${(stats.topicDiversity * 100).roundToInt()}%", Modifier.weight(1f))
            StatTile("Stories", "${(stats.storyDiversity * 100).roundToInt()}%", Modifier.weight(1f))
            StatTile("Biggest story", "${(stats.topStoryShare * 100).roundToInt()}%", Modifier.weight(1f))
        }
        Text(
            when {
                stats.topStoryShare >= 0.1f -> "One story dominates: ${(stats.topStoryShare * 100).roundToInt()}% of everything loaded."
                stats.storyDiversity >= 0.9f -> "Coverage is broad: no single story dominates."
                else -> "A few stories get most of the coverage."
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Section("Stories across outlets", "Near-identical headlines from different sources grouped into one story · tap to open the first report") {
        if (stats.clusters.isEmpty()) {
            EmptyNote("No story is covered by more than one loaded source yet.")
        } else {
            stats.clusters.forEach { cluster ->
                val first = cluster.stories.first()
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onArticleClick(first) }.padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "${cluster.sources.size} outlets · ${cluster.stories.size} stories",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(first.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        cluster.sources.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    Section("Most exclusive sources", "Share of a source's stories that no other loaded source covers (sources with 5+ stories)") {
        if (stats.exclusives.isEmpty()) {
            EmptyNote("Needs sources with at least 5 stories.")
        } else {
            BarList(stats.exclusives.map { (name, share) -> Bar(name, (share * 100).roundToInt(), "${(share * 100).roundToInt()}% only here") })
        }
    }
}

/** Distinctive vocabulary, similarity, speed and tone per source. */
@Composable
internal fun SourcesTab(stats: Statistics) {
    Section("Distinctive words", "What each source writes about more than the others (TF-IDF over headlines and summaries)") {
        if (stats.sourceWords.isEmpty()) {
            EmptyNote("Needs at least 3 sources with 5+ stories.")
        } else {
            stats.sourceWords.forEach { (name, words) -> WordsRow(name, words) }
        }
    }
    if (stats.topicWords.isNotEmpty()) {
        Section("Words by topic", "Each topic's most distinctive words (TF-IDF)") {
            stats.topicWords.forEach { (topic, words) -> WordsRow(topic.label, words) }
        }
    }
    Section("Most alike", "Source pairs with the most similar vocabulary (cosine similarity of TF-IDF)") {
        if (stats.similarSources.isEmpty()) {
            EmptyNote("Needs at least 3 sources with 5+ stories.")
        } else {
            BarList(
                stats.similarSources.map {
                    Bar("${it.first} ↔ ${it.second}", (it.similarity * 100).roundToInt(), "${(it.similarity * 100).roundToInt()}% alike")
                },
            )
        }
    }
    if (stats.uniqueSources.isNotEmpty()) {
        Section("Most independent", "Sources least like the others: average similarity to every other source") {
            BarList(
                stats.uniqueSources.map { (name, sim) ->
                    Bar(name, (sim * 100).roundToInt(), "${(sim * 100).roundToInt()}% alike on average")
                },
            )
        }
    }
    Section("Fastest sources", "Median delay behind the first report, on stories that 3+ sources covered") {
        if (stats.fastestSources.isEmpty()) {
            EmptyNote("Needs more stories shared between sources.")
        } else {
            BarList(
                stats.fastestSources.map {
                    val minutes = (it.medianMs / 60_000).toInt()
                    Bar(it.source, minutes, (if (minutes == 0) "first" else "+${duration(it.medianMs)}") + " · ${it.shared} stories")
                },
            )
        }
    }
    Section("Tone by topic", "Positive minus negative stories, with the 95% margin of error · rough, word-based") {
        if (stats.toneByTopic.isEmpty()) EmptyNote("Needs topics with 10+ stories.") else ToneList(stats.toneByTopic)
    }
    Section("Tone by source", "Same measure per source (sources with 8+ stories)") {
        if (stats.toneBySource.isEmpty()) EmptyNote("Needs sources with 8+ stories.") else ToneList(stats.toneBySource)
    }
}

/** Social engagement: platform ranks, medians by topic and time, and the age correlation. */
@Composable
internal fun SocialTab(stats: Statistics, onArticleClick: (Article) -> Unit) {
    if (stats.socialPostCount == 0) {
        Section("Social", "Engagement statistics") {
            EmptyNote("Turn on some social sources (Reddit, Mastodon, Bluesky…) and load them to see engagement statistics.")
        }
        return
    }
    Section("Best of each platform", "Posts ranked within their own source, so small and large platforms compare fairly · tap to open") {
        if (stats.topPercentile.isEmpty()) {
            EmptyNote("Needs sources with at least 5 posts.")
        } else {
            stats.topPercentile.forEach { rank ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onArticleClick(rank.post) }.padding(vertical = 4.dp),
                ) {
                    Text(
                        "#${rank.rank} of ${rank.of} on ${rank.platform} · ${compactCount(rank.total)} interactions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(rank.post.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    if (stats.engagementByTopic.isNotEmpty()) {
        Section("Engagement by topic", "Median likes + comments + shares per post (topics with 5+ posts)") {
            BarList(
                stats.engagementByTopic.map { (topic, m) ->
                    Bar(topic.label, m.median, "${compactCount(m.median)} · ${m.posts} posts", topic.icon)
                },
            )
        }
    }
    if (stats.engagementByTimeOfDay.isNotEmpty()) {
        Section("Engagement by time of day", "Median interactions by when the post was published (your local time)") {
            BarList(stats.engagementByTimeOfDay.map { (part, m) -> Bar(part, m.median, "${compactCount(m.median)} · ${m.posts} posts") })
        }
    }
    stats.ageCorrelation?.let { rho ->
        Section("Age and engagement", "Spearman rank correlation between a post's age and its interactions") {
            Text("ρ = ${String.format(Locale.US, "%.2f", rho)}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "${Statistics.describeCorrelation(rho).replaceFirstChar { it.uppercase() }}" +
                    when {
                        abs(rho) < 0.1f -> ": a post's age says little about how much engagement it has."
                        rho > 0 -> ": older posts have gathered more engagement."
                        else -> ": newer posts are getting more engagement."
                    },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WordsRow(label: String, words: List<String>) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(words.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Net tone per row as a bar growing left (more negative) or right (more positive) from a centre
 * line, with a whisker for the margin of error and the value written out.
 */
@Composable
private fun ToneList(rows: List<Statistics.ToneStat>) {
    val scale = rows.maxOf { abs(it.net) + it.margin }.coerceAtLeast(0.05f)
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.32f),
                )
                Box(Modifier.weight(0.38f).height(14.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val mid = size.width / 2
                        val half = size.width / 2
                        val end = mid + half * (row.net / scale)
                        val left = minOf(mid, end)
                        drawRoundRect(
                            barColor,
                            topLeft = Offset(left, size.height * 0.2f),
                            size = Size(abs(end - mid), size.height * 0.6f),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                        // Margin of error around the value.
                        val lo = mid + half * ((row.net - row.margin) / scale)
                        val hi = mid + half * ((row.net + row.margin) / scale)
                        drawLine(axisColor, Offset(lo, size.height / 2), Offset(hi, size.height / 2), strokeWidth = 1.5.dp.toPx())
                        drawLine(axisColor, Offset(mid, 0f), Offset(mid, size.height), strokeWidth = 1.dp.toPx())
                    }
                }
                Text(
                    "${signedPercent(row.net)} ±${(row.margin * 100).roundToInt()} · ${row.stories}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(0.3f),
                )
            }
        }
        Text(
            "Bar: net tone (right = more positive) · line: 95% margin · last number: stories",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
