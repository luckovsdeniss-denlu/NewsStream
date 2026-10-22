package com.newsstream.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.newsstream.data.ContentKind
import com.newsstream.data.SourceInfo
import com.newsstream.data.Topic
import com.newsstream.ui.SocialColor
import com.newsstream.ui.YouTubeColor
import com.newsstream.data.displaySourceName
import com.newsstream.ui.color
import com.newsstream.ui.icon
import com.newsstream.ui.NewsUiState

/** Callbacks for changing which sources are shown. */
class SourceActions(
    val setVisible: (name: String, visible: Boolean) -> Unit,
    /** Switches several sources on or off at once (e.g. a whole sub-group). */
    val setManyVisible: (names: List<String>, visible: Boolean) -> Unit,
    val hideAll: () -> Unit,
    /** Unfolds or folds a source group; the choice is saved automatically. */
    val setGroupExpanded: (label: String, expanded: Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SourcesSheet(state: NewsUiState, actions: SourceActions, onDismiss: () -> Unit) {
    val selectedCount = state.sources.count { it.name !in state.hiddenSources }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        // Full screen height regardless of how many groups are open.
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SOURCES", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "$selectedCount of ${state.sources.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                TextButton(onClick = actions.hideAll, enabled = selectedCount > 0) { Text("Deselect") }
            }
            // Sources grouped by the topic they cover (social platforms last), each group in its
            // topic's colour. Chips wrap so the whole list is visible at once (scrolling only if a
            // very small screen can't fit it).
            val groups = remember(state.sources) {
                Topic.entries.map { topic ->
                    SourceGroup(
                        label = topic.label,
                        icon = topic.icon,
                        color = topic.color,
                        sources = state.sources.filter { it.kind == ContentKind.NEWS && it.category == topic },
                    )
                } + SourceGroup(
                    label = "Social",
                    icon = Icons.Filled.Forum,
                    color = SocialColor,
                    sources = state.sources.filter { it.kind == ContentKind.SOCIAL },
                ) + SourceGroup(
                    label = "YouTube",
                    icon = Icons.Filled.SmartDisplay,
                    color = YouTubeColor,
                    sources = state.sources.filter { it.kind == ContentKind.VIDEO },
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.filter { it.sources.isNotEmpty() }.forEach { group ->
                    // Groups start folded; ones the user opened are remembered across restarts.
                    val expanded = group.label in state.expandedGroups
                    val onCount = group.sources.count { it.name !in state.hiddenSources }
                    // Tap the header to fold the group away or open it again.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { actions.setGroupExpanded(group.label, !expanded) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(group.icon, contentDescription = null, tint = group.color, modifier = Modifier.size(14.dp))
                        Text(group.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = group.color)
                        Text(
                            "$onCount/${group.sources.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                            contentDescription = if (expanded) "Collapse ${group.label}" else "Expand ${group.label}",
                            tint = group.color,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        if (group.label == Topic.WORLD.label) {
                            // World is large, so it is split into regions (and Russian-language).
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val byRegion = group.sources.groupBy(::worldRegion)
                                WORLD_REGIONS.forEach { region ->
                                    val sources = byRegion[region].orEmpty()
                                    if (sources.isNotEmpty()) {
                                        SubgroupHeader(region, sources, group.color, state.hiddenSources, actions)
                                        SourceChips(sources, group.color, state, actions)
                                    }
                                }
                            }
                        } else {
                            SourceChips(group.sources, group.color, state, actions)
                        }
                    }
                }
            }
        }
    }
}

/** A group's (or sub-group's) sources as wrapping chips, alphabetically. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceChips(sources: List<SourceInfo>, color: Color, state: NewsUiState, actions: SourceActions) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        sources.sortedBy { it.name.lowercase() }.forEach { source ->
            SourceChip(
                source = source,
                color = color,
                selected = source.name !in state.hiddenSources,
                failed = source.name in state.allFailedSources,
                onToggle = { actions.setVisible(source.name, it) },
            )
        }
    }
}

/**
 * A sub-group's name and how many of its sources are on; tapping it switches the whole sub-group
 * on (or off, when all are already on).
 */
@Composable
private fun SubgroupHeader(
    label: String,
    sources: List<SourceInfo>,
    color: Color,
    hiddenSources: Set<String>,
    actions: SourceActions,
) {
    val onCount = sources.count { it.name !in hiddenSources }
    val allOn = onCount == sources.size
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { actions.setManyVisible(sources.map { it.name }, !allOn) }
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(width = 3.dp, height = 12.dp).background(color.copy(alpha = 0.7f)))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text(
            "$onCount/${sources.size} · ${if (allOn) "tap to turn off" else "tap to turn all on"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private class SourceGroup(val label: String, val icon: ImageVector, val color: Color, val sources: List<SourceInfo>)

/**
 * A compact toggle in its group's colour: tinted fill when selected, coloured outline when not.
 * A source that failed to load shows its name in red.
 */
@Composable
private fun SourceChip(
    source: SourceInfo,
    color: Color,
    selected: Boolean,
    failed: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        displaySourceName(source.name),
        style = MaterialTheme.typography.labelMedium,
        color = when {
            failed -> MaterialTheme.colorScheme.error
            selected -> color
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = if (selected) 0.6f else 0.3f), shape)
            .clickable { onToggle(!selected) }
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}
