package com.newsstream.ui.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.newsstream.ui.images.pixelsFor
import com.newsstream.ui.images.sizedRequest
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.SourceInfo
import com.newsstream.data.Topic
import com.newsstream.data.displaySourceName
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.ViewSize
import com.newsstream.ui.YouTubeColor
import com.newsstream.ui.color
import com.newsstream.ui.icon
import com.newsstream.ui.timeAgo

/** A channel group in the YouTube tab, with its own colour and icon. */
private data class ChannelGroup(val label: String, val color: Color, val icon: ImageVector)

private val RussianGroup = ChannelGroup("Русский", Color(0xFFFF8A65), Icons.Filled.Translate)

/** The group a channel is listed under: Russian-language channels together, others by topic. */
private fun channelGroup(source: SourceInfo): ChannelGroup {
    if (source.language == "ru") return RussianGroup
    val topic = source.category
    val label = when (topic) {
        Topic.POLITICS -> "US news"
        Topic.GENERAL -> "Explainers"
        else -> topic.label
    }
    return ChannelGroup(label, topic.color, topic.icon)
}

/**
 * Latest videos from the selected YouTube channels, newest first, filtered by channel group.
 * Tapping a video opens it in YouTube.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YouTubeFeed(
    state: NewsUiState,
    size: ViewSize,
    onLoad: () -> Unit,
    onChooseChannels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val channels = remember(state.sources) { state.sources.filter { it.kind == ContentKind.VIDEO } }
    val groupOf = remember(channels) { channels.associate { it.name to channelGroup(it) } }
    // Groups are kept by label so the selection survives rotation.
    // Groups in a fixed order, only those with a selected channel.
    val selectedChannels = channels.filter { it.name !in state.hiddenSources }
    val groups = remember(selectedChannels) {
        selectedChannels.map { channelGroup(it) }.distinct()
    }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    val videos = remember(state.articles, group) {
        state.articles.filter { it.kind == ContentKind.VIDEO && (group == null || groupOf[it.source]?.label == group) }
    }
    val gridState = rememberSaveable(group, size, saver = LazyGridState.Saver) { LazyGridState() }

    Column(modifier.fillMaxSize()) {
        if (groups.size > 1) {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GroupChip(ChannelGroup("All", YouTubeColor, Icons.Filled.SmartDisplay), group == null) { group = null }
                groups.forEach { g -> GroupChip(g, group == g.label) { group = g.label } }
            }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                selectedChannels.isEmpty() -> EmptyMessage(
                    "No YouTube channels selected yet.",
                    "Choose channels",
                    onChooseChannels,
                    Modifier.align(Alignment.Center),
                )
                videos.isNotEmpty() -> LazyVerticalGrid(
                    columns = GridCells.Fixed(size.imageColumns),
                    state = gridState,
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(videos, key = { it.id }) { video -> VideoTile(video, size, groupOf[video.source]) }
                }
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.videosNotLoadedYet -> EmptyMessage(
                    "No videos loaded yet.",
                    "Load videos",
                    onLoad,
                    Modifier.align(Alignment.Center),
                )
                else -> EmptyMessage("No videos in this group yet.", null, onLoad, Modifier.align(Alignment.Center))
            }
        }
    }
}

/** A group filter chip in the group's colour: tinted when selected, outlined when not. */
@Composable
private fun GroupChip(group: ChannelGroup, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) group.color.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.dp, group.color.copy(alpha = if (selected) 0.7f else 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(group.icon, contentDescription = null, tint = group.color, modifier = Modifier.size(14.dp))
        Text(
            group.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) group.color else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A 16:9 thumbnail with a play badge, then the title and channel (text hidden at the smallest size). */
@Composable
private fun VideoTile(video: Article, size: ViewSize, group: ChannelGroup?) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { uriHandler.openUri(video.url) },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black),
        ) {
            val context = LocalContext.current
            val url = if (size == ViewSize.FULL) video.imageUrl ?: video.gridImageUrl else video.gridImageUrl
            AsyncImage(
                model = remember(url, size) { url?.let { sizedRequest(context, it, pixelsFor(size), video.gridImageUrl) } },
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Category badge in the group's colour (icon only at the smallest size).
            if (group != null) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(group.icon, contentDescription = group.label, tint = group.color, modifier = Modifier.size(12.dp))
                    if (size != ViewSize.FIFTH) {
                        Text(group.label, style = MaterialTheme.typography.labelSmall, color = group.color)
                    }
                }
            }
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (size == ViewSize.FIFTH) 18.dp else 40.dp),
            )
        }
        if (size != ViewSize.FIFTH) {
            Text(
                video.title,
                style = if (size == ViewSize.FULL) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                maxLines = if (size == ViewSize.THIRD) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(displaySourceName(video.source), video.timeAgo()).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = group?.color ?: YouTubeColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyMessage(message: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (actionLabel != null) OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}
