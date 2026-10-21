package com.newsstream.ui.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.SourceInfo
import com.newsstream.data.Topic
import com.newsstream.data.displaySourceName
import com.newsstream.ui.NewsUiState
import com.newsstream.ui.ViewSize
import com.newsstream.ui.YouTubeColor
import com.newsstream.ui.timeAgo

/** The group a channel is listed under in the YouTube tab. */
private fun channelGroup(source: SourceInfo): String = when {
    source.language == "ru" -> "Русский"
    else -> when (source.category) {
        Topic.WORLD -> "World"
        Topic.POLITICS -> "US news"
        Topic.BUSINESS -> "Business"
        Topic.TECH -> "Technology"
        Topic.SCIENCE -> "Science"
        Topic.HEALTH -> "Health"
        Topic.SPORTS -> "Sports"
        Topic.CLIMATE -> "Climate"
        Topic.CULTURE -> "Culture"
        Topic.GENERAL -> "Explainers"
    }
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
    // Groups in a fixed order, only those with a selected channel.
    val selectedChannels = channels.filter { it.name !in state.hiddenSources }
    val groups = remember(selectedChannels) {
        selectedChannels.map { channelGroup(it) }.distinct()
    }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    val videos = remember(state.articles, group) {
        state.articles.filter { it.kind == ContentKind.VIDEO && (group == null || groupOf[it.source] == group) }
    }
    val gridState = rememberSaveable(group, size, saver = LazyGridState.Saver) { LazyGridState() }

    Column(modifier.fillMaxSize()) {
        if (groups.size > 1) {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GroupChip("All", selected = group == null, onClick = { group = null })
                groups.forEach { name -> GroupChip(name, selected = group == name, onClick = { group = name }) }
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
                    items(videos, key = { it.id }) { video -> VideoTile(video, size) }
                }
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.notLoadedYet -> EmptyMessage("No videos loaded yet.", "Load news", onLoad, Modifier.align(Alignment.Center))
                else -> EmptyMessage("No videos in this group yet.", null, onLoad, Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) YouTubeColor else MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) YouTubeColor.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** A 16:9 thumbnail with a play badge, then the title and channel (text hidden at the smallest size). */
@Composable
private fun VideoTile(video: Article, size: ViewSize) {
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
            AsyncImage(
                model = if (size == ViewSize.FULL) video.imageUrl ?: video.gridImageUrl else video.gridImageUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
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
                color = YouTubeColor,
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
