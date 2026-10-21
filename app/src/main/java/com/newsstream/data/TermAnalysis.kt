package com.newsstream.data

/** Everything loaded about one trending word or phrase, for the term detail screen. */
data class TermAnalysis(
    val term: String,
    /** Stories whose headline contains the term, oldest first (who had it first). */
    val stories: List<Article>,
    /** Sources covering the term (display names) with their matching stories, most stories first. */
    val sources: List<Pair<String, List<Article>>>,
    val topics: List<Pair<Topic, Int>>,
    /** Matching stories per hour over the last 24 hours, oldest hour first. */
    val lastDay: List<Int>,
    /** Other headline words that appear most often next to the term: word to number of stories. */
    val relatedWords: List<Pair<String, Int>>,
    val languages: List<Pair<String, Int>>,
    /** Share of all loaded stories that mention the term. */
    val shareOfAll: Float,
) {
    val firstSeen: Article? get() = stories.firstOrNull { it.publishedAt > 0 }
    val latest: Article? get() = stories.lastOrNull { it.publishedAt > 0 }

    companion object {
        fun compute(
            articles: List<Article>,
            sources: List<SourceInfo>,
            term: String,
            now: Long = System.currentTimeMillis(),
        ): TermAnalysis {
            val all = articles.filter { it.kind != ContentKind.VIDEO }
            val termWords = term.split(' ')
            val languageOf = sources.associate { it.name to it.language }

            // Same tokenisation as the Insights counts, so the numbers match.
            val matches = all.mapNotNull { story ->
                val words = Insights.headlineWords(story.title)
                if (words.windowed(termWords.size).any { it == termWords }) story to words else null
            }
            val stories = matches.map { it.first }
                .sortedWith(compareBy<Article> { it.publishedAt == 0L }.thenBy { it.publishedAt })

            val related = HashMap<String, Int>()
            for ((_, words) in matches) {
                for (word in words.toSet()) if (word !in termWords) related[word] = (related[word] ?: 0) + 1
            }

            return TermAnalysis(
                term = term,
                stories = stories,
                sources = stories.groupBy { displaySourceName(it.source) }.toList()
                    .sortedWith(compareByDescending<Pair<String, List<Article>>> { it.second.size }.thenBy { it.first }),
                topics = stories.groupingBy { it.topic }.eachCount().toList().sortedByDescending { it.second },
                lastDay = Insights.hourlyCounts(stories, now),
                relatedWords = related.filterValues { it >= 2 }.toList().sortedByDescending { it.second }.take(12),
                languages = stories.groupingBy { languageOf[it.source] ?: "en" }.eachCount().toList()
                    .sortedByDescending { it.second },
                shareOfAll = if (all.isEmpty()) 0f else stories.size.toFloat() / all.size,
            )
        }
    }
}
