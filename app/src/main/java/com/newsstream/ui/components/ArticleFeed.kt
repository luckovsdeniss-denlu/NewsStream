package com.newsstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.newsstream.ui.images.PrefetchAhead
import com.newsstream.ui.images.pixelsFor
import com.newsstream.ui.images.sizedRequest
import com.newsstream.ui.ViewSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.Topic
import com.newsstream.ui.EngagementRow
import com.newsstream.ui.byline
import com.newsstream.ui.icon
import com.newsstream.ui.timeAgo
import com.newsstream.ui.theme.panel

/** Article list with loading, empty and partial-failure states, shared by the feed and topic screens. */
@Composable
fun ArticleFeed(
    articles: List<Article>,
    isLoading: Boolean,
    failedSources: List<String>,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    modifier: Modifier = Modifier,
    showTopic: Boolean = true,
    emptyMessage: String = "Couldn't load any news. Check your connection.",
    /** Label of the empty-state button, or null for no button. */
    emptyActionLabel: String? = "Try again",
    /** When set, tapping a card's topic tag calls this (e.g. to toggle a topic filter). */
    onTopicClick: ((Topic) -> Unit)? = null,
    selectedTopics: Set<Topic> = emptySet(),
    /** Changing this (e.g. when filters change) goes back to the first page. */
    pageResetKey: Any? = null,
    savedIds: Set<String> = emptySet(),
    onToggleSave: ((Article) -> Unit)? = null,
    size: ViewSize = ViewSize.DEFAULT,
) {
    // Show articles a page at a time; the next page is added when the user nears the end.
    var shownCount by rememberSaveable(pageResetKey) { mutableIntStateOf(PAGE_SIZE) }
    val shown = remember(articles, shownCount) { articles.take(shownCount) }
    // Back to the top when the filters change.
    val listState = rememberSaveable(pageResetKey, saver = LazyListState.Saver) { LazyListState() }
    val nearEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, articles.size, shownCount) {
        if (nearEnd && shownCount < articles.size) shownCount += PAGE_SIZE
    }

    // Load the images of the stories just below the screen before they scroll into view.
    if (size != ViewSize.FIFTH) {
        val imageUrls = remember(shown, size) { shown.map { cardImageUrl(it, size) } }
        PrefetchAhead(
            firstHidden = { listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size },
            urls = imageUrls,
            px = pixelsFor(size),
        )
    }

    Box(modifier.fillMaxSize()) {
        when {
            articles.isNotEmpty() -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (failedSources.isNotEmpty()) {
                    item {
                        Text(
                            "Couldn't reach: ${failedSources.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                items(shown, key = { it.id }) { article ->
                    ArticleCard(
                        size = size,
                        // Each card takes its share of the screen: 1, 2, 3 or 5 per screen.
                        modifier = Modifier.fillParentMaxHeight(size.cardFraction),
                        article = article,
                        showTopic = showTopic,
                        onClick = { onArticleClick(article) },
                        onTopicClick = onTopicClick,
                        topicSelected = article.topic in selectedTopics,
                        saved = article.id in savedIds,
                        onToggleSave = onToggleSave?.let { toggle -> { toggle(article) } },
                    )
                }
                item(key = "footer") {
                    Text(
                        if (shown.size < articles.size) {
                            "Showing ${shown.size} of ${articles.size} · loading more…"
                        } else {
                            "That's everything · ${articles.size} stories"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> EmptyState(emptyMessage, emptyActionLabel, onRefresh, Modifier.align(Alignment.Center))
        }
        if (isLoading && articles.isNotEmpty()) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

private const val PAGE_SIZE = 30

/** Start loading the next page when this many items from the end are visible. */
private const val LOAD_MORE_THRESHOLD = 3

@Composable
private fun EmptyState(message: String, actionLabel: String?, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        if (actionLabel != null) {
            OutlinedButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(actionLabel, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** The image a story card shows at [size]: the full image for big cards, the thumbnail otherwise. */
private fun cardImageUrl(article: Article, size: ViewSize): String? = when (size) {
    ViewSize.FULL, ViewSize.HALF -> article.imageUrl ?: article.gridImageUrl
    else -> article.gridImageUrl
}

/** A story card in the chosen [size]: from a full-screen story to a compact two-line row. */
@Composable
fun ArticleCard(
    article: Article,
    showTopic: Boolean,
    onClick: () -> Unit,
    onTopicClick: ((Topic) -> Unit)? = null,
    topicSelected: Boolean = false,
    saved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    size: ViewSize = ViewSize.DEFAULT,
    modifier: Modifier = Modifier,
) {
    val social = article.kind == ContentKind.SOCIAL
    val accent = if (social) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val header: @Composable () -> Unit = {
        CardHeader(article, accent, showTopic, topicSelected, onTopicClick, saved, onToggleSave)
    }
    val base = modifier.fillMaxWidth().panel(accent).clickable(onClick = onClick)
    when (size) {
        ViewSize.FULL, ViewSize.HALF -> Column(base) {
            val image = article.imageUrl ?: article.gridImageUrl
            if (image != null) {
                val context = LocalContext.current
                AsyncImage(
                    model = remember(image, size) { sizedRequest(context, image, pixelsFor(size), article.gridImageUrl) },
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(12.dp).then(if (image == null) Modifier.weight(1f) else Modifier),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                header()
                Text(
                    article.title,
                    style = if (size == ViewSize.FULL) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    maxLines = if (image == null) 8 else if (size == ViewSize.FULL) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.summary.isNotBlank()) {
                    Text(
                        article.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = when {
                            image == null -> 12
                            size == ViewSize.FULL -> 5
                            else -> 2
                        },
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (size == ViewSize.FULL) article.engagement?.let { EngagementRow(it) }
            }
        }
        ViewSize.THIRD -> Row(base.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            article.gridImageUrl?.let { url ->
                val context = LocalContext.current
                AsyncImage(
                    model = remember(url) { sizedRequest(context, url, pixelsFor(ViewSize.THIRD)) },
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                header()
                Text(
                    article.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.summary.isNotBlank()) {
                    Text(
                        article.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        ViewSize.FIFTH -> Column(
            base.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            header()
            Text(
                article.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (social && article.summary.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Topic icon, source and time on one line, with the bookmark at the end. */
@Composable
private fun CardHeader(
    article: Article,
    accent: Color,
    showTopic: Boolean,
    topicSelected: Boolean,
    onTopicClick: ((Topic) -> Unit)?,
    saved: Boolean,
    onToggleSave: (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showTopic) {
            Icon(
                article.topic.icon,
                contentDescription = article.topic.label,
                tint = if (topicSelected) MaterialTheme.colorScheme.onPrimaryContainer else accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (topicSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .then(if (onTopicClick != null) Modifier.clickable { onTopicClick(article.topic) } else Modifier)
                    .padding(1.dp)
                    .size(14.dp),
            )
        }
        Text(
            article.byline().uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        article.timeAgo()?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onToggleSave != null) SaveButton(saved, onToggleSave, Modifier.padding(start = 2.dp))
    }
}

@Composable
fun TopicTag(topic: Topic, selected: Boolean = false, onClick: ((Topic) -> Unit)? = null) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .then(if (onClick != null) Modifier.clickable { onClick(topic) } else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(topic.icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        Text(topic.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = content)
    }
}

/** Bookmark toggle: filled when the story is saved. */
@Composable
fun SaveButton(saved: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Icon(
        if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
        contentDescription = if (saved) "Remove from saved" else "Save",
        tint = MaterialTheme.colorScheme.secondary,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(2.dp)
            .size(22.dp),
    )
}
