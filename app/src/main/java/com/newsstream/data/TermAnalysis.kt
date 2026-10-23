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
    val momentum: Momentum,
    /** When each source first ran the term, in the order they picked it up. */
    val pickup: List<Pickup>,
    /** Longer phrases built around the term in headlines: phrase to number of stories. */
    val contexts: List<Pair<String, Int>>,
    /** Stories that mention the term in their text but not in the headline. */
    val textMentions: List<Article>,
    /** Matching stories per content kind (news articles vs social posts). */
    val kinds: List<Pair<ContentKind, Int>>,
    /** Engagement summed over the matching social posts; null when there are none. */
    val engagement: EngagementTotals?,
    /** A rough, word-based read of the headlines' tone. */
    val tone: Tone,
    /** Matching stories that have a picture, newest first. */
    val withImages: List<Article>,
    /** How unusual the last 3 hours are for this term (z-score); null with nothing recent. */
    val burstZ: Float?,
) {
    val firstSeen: Article? get() = stories.firstOrNull { it.publishedAt > 0 }
    val latest: Article? get() = stories.lastOrNull { it.publishedAt > 0 }

    enum class Trend(val label: String) { RISING("Rising"), STEADY("Steady"), FADING("Fading"), QUIET("Quiet") }

    /** Stories per hour in the last 6 hours compared with the 18 hours before. */
    data class Momentum(val recent: Int, val earlier: Int, val trend: Trend) {
        val recentPerHour: Float get() = recent / 6f
        val earlierPerHour: Float get() = earlier / 18f
    }

    data class Pickup(val source: String, val firstAt: Long, val afterFirstMs: Long)

    data class EngagementTotals(val posts: Int, val likes: Int, val comments: Int, val shares: Int, val top: Article)

    data class Tone(val positive: Int, val neutral: Int, val negative: Int) {
        private val n: Int get() = positive + neutral + negative

        /** Positive minus negative share of stories, -1 to 1. */
        val net: Float get() = if (n == 0) 0f else (positive - negative).toFloat() / n

        /** 95% margin of error of [net]. */
        val margin: Float get() = if (n == 0) 0f else
            (1.96 * kotlin.math.sqrt(maxOf((positive + negative).toFloat() / n - net * net, 0f) / n.toDouble())).toFloat()
    }

    companion object {
        private const val HOUR = 3_600_000L

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
            val headlineWords = all.associateWith { Insights.headlineWords(it.title) }
            val matches = all.filter { story -> headlineWords.getValue(story).windowed(termWords.size).any { it == termWords } }
            val stories = matches.sortedWith(compareBy<Article> { it.publishedAt == 0L }.thenBy { it.publishedAt })

            val related = HashMap<String, Int>()
            val contexts = HashMap<String, Int>()
            for (story in matches) {
                val words = headlineWords.getValue(story)
                for (word in words.toSet()) if (word !in termWords) related[word] = (related[word] ?: 0) + 1
                // The term with one word either side, e.g. "election" -> "presidential election".
                val found = mutableSetOf<String>()
                words.windowed(termWords.size).forEachIndexed { i, window ->
                    if (window != termWords) return@forEachIndexed
                    words.getOrNull(i - 1)?.let { found += "$it $term" }
                    words.getOrNull(i + termWords.size)?.let { found += "$term $it" }
                }
                for (phrase in found) contexts[phrase] = (contexts[phrase] ?: 0) + 1
            }

            val recent = stories.count { it.publishedAt > 0 && now - it.publishedAt in 0 until 6 * HOUR }
            val earlier = stories.count { it.publishedAt > 0 && now - it.publishedAt in 6 * HOUR until 24 * HOUR }
            val recentRate = recent / 6f
            val earlierRate = earlier / 18f
            val trend = when {
                recent == 0 && earlier == 0 -> Trend.QUIET
                recentRate > earlierRate * 1.5f -> Trend.RISING
                recentRate < earlierRate / 1.5f -> Trend.FADING
                else -> Trend.STEADY
            }

            val dated = stories.filter { it.publishedAt > 0 }
            val firstAt = dated.firstOrNull()?.publishedAt ?: 0L
            val pickup = dated.groupBy { displaySourceName(it.source) }
                .map { (source, list) -> list.minOf { it.publishedAt }.let { Pickup(source, it, it - firstAt) } }
                .sortedBy { it.firstAt }

            val needle = term.lowercase()
            val matchedIds = matches.map { it.id }.toSet()
            val textMentions = all.filter { story ->
                story.id !in matchedIds && (story.summary.lowercase().contains(needle) ||
                    story.body.any { it.lowercase().contains(needle) })
            }.sortedByDescending { it.publishedAt }

            val posts = stories.filter { it.engagement != null }
            fun total(a: Article) = a.engagement!!.let { (it.likes ?: 0) + (it.comments ?: 0) + (it.shares ?: 0) }
            val engagement = posts.maxByOrNull(::total)?.let { top ->
                EngagementTotals(
                    posts = posts.size,
                    likes = posts.sumOf { it.engagement!!.likes ?: 0 },
                    comments = posts.sumOf { it.engagement!!.comments ?: 0 },
                    shares = posts.sumOf { it.engagement!!.shares ?: 0 },
                    top = top,
                )
            }

            var positive = 0
            var negative = 0
            for (story in stories) {
                val score = ToneLexicon.score(story)
                if (score > 0) positive++ else if (score < 0) negative++
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
                momentum = Momentum(recent, earlier, trend),
                pickup = pickup,
                contexts = contexts.filterValues { it >= 2 }.toList().sortedByDescending { it.second }.take(8),
                textMentions = textMentions,
                kinds = stories.groupingBy { it.kind }.eachCount().toList().sortedByDescending { it.second },
                engagement = engagement,
                tone = Tone(positive, stories.size - positive - negative, negative),
                withImages = stories.filter { it.gridImageUrl != null }.reversed(),
                burstZ = Statistics.burstScore(stories.map { it.publishedAt }, now),
            )
        }
    }
}
