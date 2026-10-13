package com.newsstream.ui.article

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.newsstream.ui.byline
import com.newsstream.ui.theme.CyberBackground
import com.newsstream.ui.theme.NeonBorderBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(article: Article, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current
    CyberBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(article.source.uppercase(), color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                            )
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
                Text(article.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "▸ " + article.byline().uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Box(Modifier.fillMaxWidth().height(2.dp).background(NeonBorderBrush))
                val paragraphs = article.body.ifEmpty { listOf(article.summary).filter { it.isNotBlank() } }
                paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                OutlinedButton(
                    onClick = { uriHandler.openUri(article.url) },
                    border = BorderStroke(1.dp, NeonBorderBrush),
                ) {
                    Text(
                        if (article.body.isEmpty()) "READ FULL STORY >>" else "OPEN SOURCE >>",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
