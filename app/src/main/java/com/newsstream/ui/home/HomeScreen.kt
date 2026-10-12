package com.newsstream.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.newsstream.data.Article
import com.newsstream.data.Category
import com.newsstream.data.NewsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repo: NewsRepository, onOpen: (String) -> Unit) {
    var category by remember { mutableStateOf(Category.ALL) }
    var articles by remember { mutableStateOf<List<Article>?>(null) }
    LaunchedEffect(category) {
        articles = null
        articles = repo.articles(category)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("NewsStream") }) }) { pad ->
        Column(Modifier.padding(pad)) {
            ScrollableTabRow(selectedTabIndex = category.ordinal, edgePadding = 8.dp) {
                Category.entries.forEach {
                    Tab(selected = it == category, onClick = { category = it }, text = { Text(it.label) })
                }
            }
            val list = articles
            if (list == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(list, key = { it.id }) { ArticleCard(it) { onOpen(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box {
            AsyncImage(
                model = article.imageUrl, contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop,
            )
            if (article.streamUrl != null) {
                Icon(Icons.Default.PlayCircle, "Video", Modifier.align(Alignment.Center).size(56.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(article.title, style = MaterialTheme.typography.titleMedium)
            Text("${article.source} · ${article.publishedAt}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
