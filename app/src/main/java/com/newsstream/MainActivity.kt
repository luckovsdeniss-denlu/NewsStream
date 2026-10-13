package com.newsstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.newsstream.ui.home.HomeScreen
import com.newsstream.ui.theme.NewsStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsStreamTheme { HomeScreen() }
        }
    }
}
