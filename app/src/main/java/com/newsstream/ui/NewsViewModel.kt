package com.newsstream.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newsstream.data.Article
import com.newsstream.data.NewsRepository
import com.newsstream.data.SourceInfo
import com.newsstream.data.SourcePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val isLoading: Boolean = false,
    /** Everything fetched, including articles from hidden sources. */
    val allArticles: List<Article> = emptyList(),
    val allFailedSources: List<String> = emptyList(),
    val sources: List<SourceInfo> = emptyList(),
    val hiddenSources: Set<String> = emptySet(),
) {
    /** Articles from the sources the user has selected. */
    val articles: List<Article> by lazy { allArticles.filter { it.source !in hiddenSources } }
    val failedSources: List<String> by lazy { allFailedSources.filter { it !in hiddenSources } }
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NewsRepository()
    private val preferences = SourcePreferences(application)

    private val _state = MutableStateFlow(
        NewsUiState(sources = repository.sourceInfo, hiddenSources = preferences.hiddenSources),
    )
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
                it.copy(
                    isLoading = false,
                    // Keep showing the previous articles if every source failed this time.
                    allArticles = result.articles.ifEmpty { it.allArticles },
                    allFailedSources = result.failedSources,
                )
            }
        }
    }

    fun setSourceVisible(name: String, visible: Boolean) =
        setHidden(if (visible) _state.value.hiddenSources - name else _state.value.hiddenSources + name)

    fun showAllSources() = setHidden(emptySet())

    fun hideAllSources() = setHidden(_state.value.sources.map { it.name }.toSet())

    fun showOnlySource(name: String) = setHidden(_state.value.sources.map { it.name }.toSet() - name)

    private fun setHidden(hidden: Set<String>) {
        preferences.hiddenSources = hidden
        _state.update { it.copy(hiddenSources = hidden) }
    }
}
