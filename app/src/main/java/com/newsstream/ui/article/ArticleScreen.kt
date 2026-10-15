package com.newsstream.ui.article

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.source.PageImageFinder
import com.newsstream.ui.EngagementRow
import com.newsstream.ui.byline
import com.newsstream.ui.publishedTime
import com.newsstream.ui.relativeTime
import com.newsstream.ui.theme.AppBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(article: Article, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current
    val social = article.kind == ContentKind.SOCIAL
    val accent = if (social) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = { Text(article.source.uppercase(), color = accent) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = accent)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    article.title,
                    style = if (social && article.summary.isEmpty()) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                )
                Text(article.byline().uppercase(), style = MaterialTheme.typography.labelMedium, color = accent)
                PublishedBlock(article)
                article.engagement?.let { EngagementRow(it) }
                ArticleImage(article)
                Box(Modifier.fillMaxWidth().height(1.dp).background(accent.copy(alpha = 0.5f)))
                val paragraphs = article.body.ifEmpty { listOf(article.summary).filter { it.isNotBlank() } }
                paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { uriHandler.openUri(article.url) }) {
                        Text(
                            when {
                                social && article.discussionUrl == null -> "OPEN POST >>"
                                article.body.isEmpty() -> "FULL STORY >>"
                                else -> "SOURCE >>"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                        )
                    }
                    article.discussionUrl?.takeIf { it != article.url }?.let { thread ->
                        OutlinedButton(onClick = { uriHandler.openUri(thread) }) {
                            Text("THREAD >>", style = MaterialTheme.typography.labelLarge, color = accent)
                        }
                    }
                }
            }
        }
    }
}

/** The publish date and time in a panel of its own. */
@Composable
private fun PublishedBlock(article: Article) {
    val full = article.publishedTime(LocalContext.current, full = true)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column {
            Text("PUBLISHED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(full ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
            article.relativeTime()?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * A button that loads the article's image on demand: the feed's image if it has one,
 * otherwise the lead image the article page declares for link previews.
 */
@Composable
private fun ArticleImage(article: Article) {
    var shown by rememberSaveable(article.id) { mutableStateOf(false) }
    var resolvedUrl by rememberSaveable(article.id) { mutableStateOf(article.imageUrl) }
    var searching by rememberSaveable(article.id) { mutableStateOf(false) }
    var notFound by rememberSaveable(article.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            shown = !shown
            if (shown && resolvedUrl == null && !notFound && !searching) {
                searching = true
                scope.launch {
                    val found = withContext(Dispatchers.IO) { runCatching { PageImageFinder.find(article.url) }.getOrNull() }
                    resolvedUrl = found
                    notFound = found == null
                    searching = false
                }
            }
        },
    ) {
        Icon(
            if (shown) Icons.Filled.HideImage else Icons.Filled.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            if (shown) "HIDE IMAGE" else "SHOW IMAGE",
            Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (!shown) return
    val url = resolvedUrl
    when {
        searching -> CircularProgressIndicator(Modifier.size(28.dp))
        url == null -> Text(
            "No image available for this story.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> SubcomposeAsyncImage(
            model = url,
            contentDescription = article.title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(RoundedCornerShape(14.dp)),
            loading = {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
            },
            error = {
                Text(
                    "Couldn't load the image.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            },
        )
    }
}
