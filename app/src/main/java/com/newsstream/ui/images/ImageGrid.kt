package com.newsstream.ui.images

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.newsstream.data.Article
import com.newsstream.ui.ViewSize
import com.newsstream.ui.icon

/**
 * A grid of the images attached to the given stories (already filtered by the selected topics
 * and sources). Only images the feeds supply are used, never page lookups, so tiles appear
 * straight from the prefetched cache.
 */
@Composable
fun ImageGrid(
    articles: List<Article>,
    isLoading: Boolean,
    notLoadedYet: Boolean,
    onLoad: () -> Unit,
    idleMessage: String = "No news loaded yet.",
    idleActionLabel: String = "Load news",
    onArticleClick: (Article) -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
    size: ViewSize = ViewSize.DEFAULT,
) {
    val withImages = remember(articles) { articles.filter { it.gridImageUrl != null } }
    val gridState = rememberSaveable(resetKey, size, saver = LazyGridState.Saver) { LazyGridState() }
    val listState = rememberSaveable(resetKey, size, saver = LazyListState.Saver) { LazyListState() }

    Box(modifier.fillMaxSize()) {
        when {
            // Full screen: one whole picture per screen, with its caption.
            withImages.isNotEmpty() && size == ViewSize.FULL -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(withImages, key = { it.id }) { article ->
                    FullImage(article, Modifier.fillParentMaxHeight(), onClick = { onArticleClick(article) })
                }
            }
            withImages.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Fixed(size.imageColumns),
                state = gridState,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(withImages, key = { it.id }) { article ->
                    ImageTile(article, size, onClick = { onArticleClick(article) })
                }
            }
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            notLoadedYet -> Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(idleMessage, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onLoad) { Text(idleActionLabel) }
            }
            else -> Text(
                "No images for the selected topics yet.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }
    }
}

@Composable
private fun ImageTile(article: Article, size: ViewSize, onClick: () -> Unit) {
    val context = LocalContext.current
    val url = article.gridImageUrl ?: return
    // The smallest size uses the same request as the prefetch, so a warmed image is a
    // memory-cache hit; larger tiles decode a bigger version (from the disk cache when warmed).
    val request = remember(url, size) { thumbnailRequest(context, url, THUMBNAIL_PX * 5 / size.imageColumns) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = request,
            contentDescription = article.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (size == ViewSize.FIFTH) return@Box
        // Headline over a dark gradient so it stays readable on any picture.
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(start = 5.dp, end = 5.dp, top = 14.dp, bottom = 4.dp),
        ) {
            Text(
                article.title,
                style = if (size == ViewSize.HALF) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = if (size == ViewSize.HALF) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One picture filling the screen height, shown whole (not cropped), with source and headline. */
@Composable
private fun FullImage(article: Article, modifier: Modifier, onClick: () -> Unit) {
    val url = article.imageUrl ?: article.gridImageUrl ?: return
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = url,
            contentDescription = article.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .padding(start = 14.dp, end = 14.dp, top = 32.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                article.source.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                article.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
