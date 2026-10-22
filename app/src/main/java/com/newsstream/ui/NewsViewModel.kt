package com.newsstream.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newsstream.data.Article
import com.newsstream.data.ContentKind
import com.newsstream.data.NewsRepository
import com.newsstream.data.SourceInfo
import com.newsstream.data.SavedArticles
import com.newsstream.data.SourcePreferences
import com.newsstream.data.Topic
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
    /** Source groups unfolded in the sources sheet (all others are folded); saved automatically. */
    val expandedGroups: Set<String> = emptySet(),
    /** Size stories and images are shown at; saved automatically. */
    val viewSize: ViewSize = ViewSize.DEFAULT,
) {
    val savedIds: Set<String> by lazy { saved.map { it.id }.toSet() }

    val isLoading: Boolean get() = loadingSources > 0

    /** The user hasn't selected any source yet (the starting state). */
    val noSourcesSelected: Boolean get() = sources.isNotEmpty() && sources.all { it.name in hiddenSources }

    /** No videos have been fetched yet: they load only when asked for in the YouTube tab. */
    val videosNotLoadedYet: Boolean
        get() = !isLoading && allArticles.none { it.kind == ContentKind.VIDEO } &&
            sources.none { it.kind == ContentKind.VIDEO && it.name in allFailedSources }

    /** Nothing has been fetched yet: the app doesn't load news until the user asks. */
    val notLoadedYet: Boolean
        get() = !isLoading && allArticles.none { it.kind != ContentKind.VIDEO } &&
            sources.none { it.kind != ContentKind.VIDEO && it.name in allFailedSources }

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
    private val videoSourceNames = repository.sourceInfo.filter { it.kind == ContentKind.VIDEO }.map { it.name }.toSet()
    private var videosRequested = false
    private var roundTotal = 0

    private val _state = MutableStateFlow(
        NewsUiState(
            sources = repository.sourceInfo,
            // Every source not explicitly selected is off, so a fresh install starts with none.
            hiddenSources = repository.sourceInfo.map { it.name }.toSet() - preferences.selectedSources,
            expandedGroups = preferences.expandedGroups,
            viewSize = ViewSize.fromName(preferences.viewSize),
            saved = savedStore.load(),
        ),
    )
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    /**
     * Fetches every selected source. Nothing is fetched at launch; this runs when the user taps
     * Load news or refresh. Switched-off sources are not downloaded.
     */
    fun refresh() = load(visibleSourceNames().filter { it !in videoSourceNames })

    /** Fetches the selected YouTube channels. Videos load only from the YouTube tab, on request. */
    fun refreshVideos() {
        videosRequested = true
        load(visibleSourceNames().filter { it in videoSourceNames })
    }

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

    /** Switches the given YouTube channels on and loads their videos. */
    fun turnOnChannels(names: List<String>) {
        setHidden(_state.value.hiddenSources - names.toSet())
        videosRequested = true
        load(names.filter { it !in articlesBySource })
    }

    fun setSourceVisible(name: String, visible: Boolean) {
        setHidden(if (visible) _state.value.hiddenSources - name else _state.value.hiddenSources + name)
        if (visible) loadIfMissing(listOf(name))
    }

    /**
     * Replaces the news-source selection with [count] random news sources (from [topic]'s sources
     * when a topic is chosen) and loads them. Social and YouTube choices are left as they are.
     * Returns the names picked.
     */
    fun pickRandomNewsSources(count: Int = 5, topic: Topic? = null): List<String> {
        val news = _state.value.sources.filter { it.kind == ContentKind.NEWS }
        val pool = news.filter { topic == null || it.category == topic }.ifEmpty { news }
        val picked = pool.shuffled().take(count).map { it.name }
        setHidden(_state.value.hiddenSources + news.map { it.name } - picked.toSet())
        loadIfMissing(picked)
        return picked
    }

    fun setSourcesVisible(names: List<String>, visible: Boolean) {
        setHidden(if (visible) _state.value.hiddenSources - names.toSet() else _state.value.hiddenSources + names)
        if (visible) loadIfMissing(names)
    }

    fun hideAllSources() = setHidden(_state.value.sources.map { it.name }.toSet())

    /** Folds or unfolds a group in the sources sheet and saves the choice right away. */
    fun setGroupExpanded(label: String, expanded: Boolean) {
        val updated = if (expanded) _state.value.expandedGroups + label else _state.value.expandedGroups - label
        preferences.expandedGroups = updated
        _state.update { it.copy(expandedGroups = updated) }
    }

    fun setViewSize(size: ViewSize) {
        preferences.viewSize = size.name
        _state.update { it.copy(viewSize = size) }
    }

    private fun visibleSourceNames() =
        _state.value.sources.map { it.name }.filter { it !in _state.value.hiddenSources }

    /**
     * Fetches sources switched on for the first time (e.g. hidden since launch). YouTube channels
     * are skipped until the user has loaded videos in the YouTube tab.
     */
    private fun loadIfMissing(names: List<String>) =
        load(names.filter { it !in articlesBySource && (videosRequested || it !in videoSourceNames) })

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
        preferences.selectedSources = _state.value.sources.map { it.name }.toSet() - hidden
        _state.update { it.copy(hiddenSources = hidden) }
    }

    private companion object {
        const val PREFETCH_PER_SOURCE = 8
    }
}
