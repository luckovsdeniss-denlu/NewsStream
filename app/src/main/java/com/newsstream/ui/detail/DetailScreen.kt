package com.newsstream.ui.detail

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.newsstream.data.Article
import com.newsstream.data.NewsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(repo: NewsRepository, id: String, onBack: () -> Unit) {
    val article by produceState<Article?>(null, id) { value = repo.article(id) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(article?.source.orEmpty()) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { pad ->
        val a = article
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            if (a == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                a.streamUrl?.let { VideoPlayer(it) }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(a.title, style = MaterialTheme.typography.headlineSmall)
                    Text(a.publishedAt, style = MaterialTheme.typography.labelMedium)
                    Text(a.summary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun VideoPlayer(url: String) {
    val context: Context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { PlayerView(it).apply { this.player = player } },
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
    )
}
