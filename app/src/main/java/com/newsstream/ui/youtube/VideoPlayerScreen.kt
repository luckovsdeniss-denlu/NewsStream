package com.newsstream.ui.youtube

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.newsstream.data.Article
import com.newsstream.data.displaySourceName
import com.newsstream.ui.YouTubeColor
import com.newsstream.ui.images.thumbnailRequest
import com.newsstream.ui.theme.AppBackground
import com.newsstream.ui.timeAgo

/**
 * Plays a YouTube video inside the app with YouTube's embedded player, then shows its title,
 * description and more videos to play next. The player's fullscreen button switches to landscape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: Article,
    saved: Boolean,
    upNext: List<Article>,
    onToggleSave: () -> Unit,
    onPlay: (Article) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current
    // The player's own fullscreen view, while it is shown.
    var fullscreen by remember { mutableStateOf<FullscreenView?>(null) }

    Box(Modifier.fillMaxSize()) {
        AppBackground {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                displaySourceName(video.source).uppercase(),
                                color = YouTubeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = YouTubeColor)
                            }
                        },
                        actions = {
                            IconButton(onClick = { uriHandler.openUri(video.url) }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in YouTube", tint = YouTubeColor)
                            }
                            IconButton(onClick = onToggleSave) {
                                Icon(
                                    if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = if (saved) "Remove from saved" else "Save",
                                    tint = YouTubeColor,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                },
            ) { pad ->
                Column(Modifier.fillMaxSize().padding(pad)) {
                    val videoId = youTubeVideoId(video.url)
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                        if (videoId != null) {
                            // A new player for each video, so switching videos starts cleanly.
                            key(videoId) {
                                YouTubePlayer(videoId, onFullscreen = { fullscreen = it })
                            }
                        } else {
                            Text(
                                "This video can't be played here.",
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                    val scroll = key(video.id) { rememberScrollState() }
                    Column(
                        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(video.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            listOfNotNull(displaySourceName(video.source), video.timeAgo()).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = YouTubeColor,
                        )
                        Text(
                            "Won't play? Some channels only allow their videos on YouTube: use the open button above.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (video.summary.isNotBlank()) {
                            Text(video.summary, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (upNext.isNotEmpty()) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(YouTubeColor.copy(alpha = 0.4f)))
                            Text("UP NEXT", style = MaterialTheme.typography.labelLarge, color = YouTubeColor)
                            upNext.forEach { next -> UpNextRow(next, onClick = { onPlay(next) }) }
                        }
                    }
                }
            }
        }
        fullscreen?.let { full ->
            BackHandler { full.exit() }
            AndroidView(
                factory = { full.view.also { (it.parent as? ViewGroup)?.removeView(it) } },
                modifier = Modifier.fillMaxSize().background(Color.Black),
            )
        }
    }
}

/** The view the player shows in fullscreen, and how to leave it. */
private class FullscreenView(val view: View, val exit: () -> Unit)

/**
 * YouTube's IFrame player in a WebView. The page is given an https origin, which YouTube requires
 * to identify the embedding app; playback pauses when the app goes to the background.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubePlayer(videoId: String, onFullscreen: (FullscreenView?) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycle, webView) {
        val view = webView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = object : WebChromeClient() {
                    private var callback: CustomViewCallback? = null

                    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                        this.callback = callback
                        activity?.enterFullscreen()
                        onFullscreen(FullscreenView(view) { onHideCustomView() })
                    }

                    override fun onHideCustomView() {
                        onFullscreen(null)
                        activity?.exitFullscreen()
                        callback?.onCustomViewHidden()
                        callback = null
                    }
                }
                loadDataWithBaseURL(PLAYER_ORIGIN, playerHtml(videoId), "text/html", "utf-8", null)
                webView = this
            }
        },
        onRelease = { view ->
            onFullscreen(null)
            activity?.exitFullscreen()
            view.destroy()
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private const val PLAYER_ORIGIN = "https://com.newsstream"

private fun playerHtml(videoId: String) = """
    <!DOCTYPE html>
    <html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>html,body{margin:0;padding:0;height:100%;background:#000;overflow:hidden}iframe{border:0;width:100%;height:100%}</style>
    </head><body>
    <iframe src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&origin=$PLAYER_ORIGIN"
        referrerpolicy="strict-origin-when-cross-origin"
        allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe>
    </body></html>
""".trimIndent()

private val VIDEO_ID = Regex("(?:v=|/shorts/|youtu\\.be/|/embed/|/live/)([A-Za-z0-9_-]{11})")

/** The 11-character video ID from a YouTube watch, shorts, live or short-link URL. */
fun youTubeVideoId(url: String): String? = VIDEO_ID.find(url)?.groupValues?.get(1)

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Activity.enterFullscreen() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

private fun Activity.exitFullscreen() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
}

/** Another video: thumbnail, title and channel; tap to play it. */
@Composable
private fun UpNextRow(video: Article, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = remember(video.gridImageUrl) { video.gridImageUrl?.let { thumbnailRequest(context, it) } },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(128.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp)).background(Color.Black),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(video.title, style = MaterialTheme.typography.labelLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
