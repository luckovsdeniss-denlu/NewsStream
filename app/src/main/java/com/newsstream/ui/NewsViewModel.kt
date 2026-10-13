package com.newsstream.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newsstream.data.Article
import com.newsstream.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val isLoading: Boolean = false,
    val articles: List<Article> = emptyList(),
    val failedSources: List<String> = emptyList(),
)

class NewsViewModel(private val repository: NewsRepository = NewsRepository()) : ViewModel() {
    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.latest()
            _state.update {
                // Keep showing the previous articles if every source failed this time.
                NewsUiState(
                    articles = result.articles.ifEmpty { it.articles },
                    failedSources = result.failedSources,
                )
            }
        }
    }
}
