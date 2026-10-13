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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.ui.EngagementRow
import com.newsstream.ui.byline
import com.newsstream.ui.theme.HudBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(article: Article, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current
    val social = article.kind == ContentKind.SOCIAL
    val accent = if (social) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    HudBackground {
        Scaffold(
            containerColor = Color.Transparent,
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
                Text(
                    "▸ " + article.byline().uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                )
                article.engagement?.let { EngagementRow(it) }
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
