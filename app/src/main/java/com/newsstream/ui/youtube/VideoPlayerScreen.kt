package com.newsstream.ui.youtube

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.JavascriptInterface
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay

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
 * YouTube's IFrame Player API in a WebView, set up the way the widely used android-youtube-player
 * library does it: the page is loaded with an https origin naming the app ("https://" + package),
 * which YouTube requires from embedding apps, and the player is created with YT.Player so its
 * ready and error events reach the app. Errors are shown over the player instead of a black box;
 * playback pauses when the app goes to the background.
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun YouTubePlayer(videoId: String, onFullscreen: (FullscreenView?) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uriHandler = LocalUriHandler.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var status by remember { mutableStateOf(PlayerStatus.LOADING) }
    var errorCode by remember { mutableStateOf(0) }

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
    // If the player never reports ready, say so rather than leaving a black box.
    LaunchedEffect(videoId) {
        delay(15_000)
        if (status == PlayerStatus.LOADING) status = PlayerStatus.TIMED_OUT
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    // Called by the page's player events (on a background thread).
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onReady() {
                                post { status = PlayerStatus.READY }
                            }

                            @JavascriptInterface
                            fun onError(code: Int) {
                                post {
                                    errorCode = code
                                    status = PlayerStatus.ERROR
                                }
                            }
                        },
                        "NewsStreamPlayer",
                    )
                    webChromeClient = object : WebChromeClient() {
                        private var callback: CustomViewCallback? = null

                        // Without a poster WebView draws a grey placeholder over the player.
                        override fun getDefaultVideoPoster(): Bitmap =
                            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

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
                    val origin = "https://" + ctx.packageName
                    loadDataWithBaseURL(origin, playerHtml(videoId, origin), "text/html", "utf-8", null)
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
        if (status == PlayerStatus.ERROR || status == PlayerStatus.TIMED_OUT) {
            Column(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (status == PlayerStatus.TIMED_OUT) {
                        "The player didn't load. Check the connection or open the video in YouTube."
                    } else {
                        playerErrorMessage(errorCode)
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(onClick = { uriHandler.openUri("https://www.youtube.com/watch?v=$videoId") }) {
                    Text("OPEN IN YOUTUBE", color = YouTubeColor)
                }
            }
        }
    }
}

private enum class PlayerStatus { LOADING, READY, ERROR, TIMED_OUT }

/** The IFrame API's error codes, in words. */
private fun playerErrorMessage(code: Int) = when (code) {
    2 -> "This video link isn't valid (error 2)."
    5 -> "This video can't be played in the app's player (error 5)."
    100 -> "This video was removed or made private (error 100)."
    101, 150 -> "The channel doesn't allow this video to play outside YouTube (error $code)."
    152, 153 -> "YouTube refused to play this video in the app (error $code)."
    else -> "YouTube couldn't play this video (error $code)."
}

/** A page with YouTube's IFrame Player API filling the screen, reporting ready and errors to the app. */
private fun playerHtml(videoId: String, origin: String) = """
    <!DOCTYPE html>
    <html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden;position:fixed}</style>
    <script defer src="https://www.youtube.com/iframe_api"></script>
    </head><body>
    <div id="player"></div>
    <script>
      function onYouTubeIframeAPIReady() {
        new YT.Player('player', {
          width: '100%',
          height: '100%',
          videoId: '$videoId',
          playerVars: { autoplay: 1, playsinline: 1, rel: 0, controls: 1, fs: 1, enablejsapi: 1, origin: '$origin' },
          events: {
            onReady: function(e) { NewsStreamPlayer.onReady(); e.target.playVideo(); },
            onError: function(e) { NewsStreamPlayer.onError(e.data); }
          }
        });
      }
    </script>
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
