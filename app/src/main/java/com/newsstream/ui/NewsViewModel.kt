package com.newsstream.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newsstream.data.Article
import com.newsstream.data.NewsRepository
import com.newsstream.data.SourceInfo
import com.newsstream.data.SavedArticles
import com.newsstream.data.SourcePreferences
import com.newsstream.ui.images.thumbnailRequest
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    /** Sources currently being fetched. */
    val loadingSources: Int = 0,
    /** Sources requested in the current round of loading, for progress display. */
    val loadingTotal: Int = 0,
    /** Everything fetched, including articles from hidden sources. */
    val allArticles: List<Article> = emptyList(),
    val allFailedSources: List<String> = emptyList(),
    val sources: List<SourceInfo> = emptyList(),
    val hiddenSources: Set<String> = emptySet(),
    /** Stories the user saved, newest-saved first. */
    val saved: List<Article> = emptyList(),
) {
    val savedIds: Set<String> by lazy { saved.map { it.id }.toSet() }

    val isLoading: Boolean get() = loadingSources > 0

    /** Nothing has been fetched yet: the app doesn't load news until the user asks. */
    val notLoadedYet: Boolean get() = !isLoading && allArticles.isEmpty() && allFailedSources.isEmpty()

    /** Articles from the sources the user has selected. */
    val articles: List<Article> by lazy { allArticles.filter { it.source !in hiddenSources } }
    val failedSources: List<String> by lazy { allFailedSources.filter { it !in hiddenSources } }
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NewsRepository(application.cacheDir)
    private val preferences = SourcePreferences(application)
    private val savedStore = SavedArticles(application)

    // Only touched on the main thread (viewModelScope collects there).
    private val articlesBySource = mutableMapOf<String, List<Article>>()
    private val failed = mutableSetOf<String>()
    private val inFlight = mutableSetOf<String>()
    private var roundTotal = 0

    private val _state = MutableStateFlow(
        NewsUiState(
            sources = repository.sourceInfo,
            hiddenSources = preferences.hiddenSources,
            saved = savedStore.load(),
        ),
    )
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    /**
     * Fetches every selected source. Nothing is fetched at launch; this runs when the user taps
     * Load news or refresh. Switched-off sources are not downloaded.
     */
    fun refresh() = load(visibleSourceNames())

    /** Saves the story, or removes it if it is already saved. */
    fun toggleSaved(article: Article) {
        val current = _state.value.saved
        val updated = if (current.any { it.id == article.id }) {
            current.filterNot { it.id == article.id }
        } else {
            listOf(article) + current
        }
        _state.update { it.copy(saved = updated) }
        // Always write the latest list, so quick successive taps can't leave an older one on disk.
        viewModelScope.launch(Dispatchers.IO) { runCatching { savedStore.store(_state.value.saved) } }
    }

    fun setSourceVisible(name: String, visible: Boolean) {
        setHidden(if (visible) _state.value.hiddenSources - name else _state.value.hiddenSources + name)
        if (visible) loadIfMissing(listOf(name))
    }

    fun showAllSources() {
        setHidden(emptySet())
        loadIfMissing(visibleSourceNames())
    }

    fun hideAllSources() = setHidden(_state.value.sources.map { it.name }.toSet())

    fun showLanguage(language: String?) {
        val sources = _state.value.sources
        setHidden(if (language == null) emptySet() else sources.filter { it.language != language }.map { it.name }.toSet())
        loadIfMissing(visibleSourceNames())
    }

    private fun visibleSourceNames() =
        _state.value.sources.map { it.name }.filter { it !in _state.value.hiddenSources }

    /** Fetches sources switched on for the first time (e.g. hidden since launch). */
    private fun loadIfMissing(names: List<String>) = load(names.filter { it !in articlesBySource })

    private fun load(names: List<String>) {
        val toFetch = names.filter { inFlight.add(it) }
        if (toFetch.isEmpty()) return
        // A new round starts when nothing else is loading; otherwise extend the current one.
        roundTotal = if (inFlight.size == toFetch.size) toFetch.size else roundTotal + toFetch.size
        publish()
        viewModelScope.launch {
            repository.fetch(toFetch.toSet()).collect { result ->
                inFlight -= result.name
                result.articles
                    .onSuccess {
                        articlesBySource[result.name] = it
                        failed -= result.name
                        prefetchImages(it)
                    }
                    // Keep showing a source's previous articles if a refresh fails.
                    .onFailure { failed += result.name }
                publish()
            }
        }
    }

    /**
     * Warms the image cache with a source's newest thumbnails as soon as it arrives, so the
     * Images view shows them instantly instead of loading on scroll.
     */
    private fun prefetchImages(articles: List<Article>) {
        val context = getApplication<Application>()
        articles.asSequence()
            .mapNotNull { it.gridImageUrl }
            .take(PREFETCH_PER_SOURCE)
            .forEach { context.imageLoader.enqueue(thumbnailRequest(context, it)) }
    }

    private fun publish() {
        val merged = articlesBySource.values.flatten()
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAt }
        _state.update {
            it.copy(
                loadingSources = inFlight.size,
                loadingTotal = roundTotal,
                allArticles = merged,
                allFailedSources = failed.toList(),
            )
        }
    }

    private fun setHidden(hidden: Set<String>) {
        preferences.hiddenSources = hidden
        _state.update { it.copy(hiddenSources = hidden) }
    }

    private companion object {
        const val PREFETCH_PER_SOURCE = 8
    }
}
