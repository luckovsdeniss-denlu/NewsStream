package com.newsstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.newsstream.data.NewsRepository
import com.newsstream.data.SampleNewsRepository
import com.newsstream.ui.detail.DetailScreen
import com.newsstream.ui.home.HomeScreen
import com.newsstream.ui.theme.NewsStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsStreamTheme {
                val repo: NewsRepository = remember { SampleNewsRepository() }
                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") { HomeScreen(repo) { nav.navigate("article/$it") } }
                    composable("article/{id}") { entry ->
                        DetailScreen(repo, entry.arguments?.getString("id").orEmpty()) { nav.popBackStack() }
                    }
                }
            }
        }
    }
}
