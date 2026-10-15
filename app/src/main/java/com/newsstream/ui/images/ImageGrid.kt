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
    onArticleClick: (Article) -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
) {
    val withImages = remember(articles) { articles.filter { it.gridImageUrl != null } }
    val gridState = rememberSaveable(resetKey, saver = LazyGridState.Saver) { LazyGridState() }

    Box(modifier.fillMaxSize()) {
        when {
            withImages.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                state = gridState,
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(withImages, key = { it.id }) { article ->
                    ImageTile(article, onClick = { onArticleClick(article) })
                }
            }
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            notLoadedYet -> Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("No news loaded yet.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onLoad) { Text("Load news") }
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
private fun ImageTile(article: Article, onClick: () -> Unit) {
    val context = LocalContext.current
    val url = article.gridImageUrl ?: return
    // Same request as the prefetch, so a warmed image is a memory-cache hit.
    val request = remember(url) { thumbnailRequest(context, url) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = request,
            contentDescription = article.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            article.topic.icon,
            contentDescription = article.topic.label,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(4.dp)
                .size(14.dp),
        )
        // Headline over a dark gradient so it stays readable on any picture.
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
        ) {
            Text(
                article.source.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                article.title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
