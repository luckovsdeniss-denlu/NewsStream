package com.newsstream.data

/** Statistics computed on the device from the loaded stories, for the Insights screen. */
data class Insights(
    val storyCount: Int,
    val sourcesResponding: Int,
    val sourcesSelected: Int,
    val topicCount: Int,
    val withImagesShare: Float,
    /** Stories per topic, most first. */
    val topics: List<Pair<Topic, Int>>,
    /** Sources with the most stories (display names). */
    val topSources: List<Pair<String, Int>>,
    /** Headline words used by the most different sources: word to number of sources. */
    val trendingWords: List<Pair<String, Int>>,
    /** Two-word phrases picked up by several sources: phrase to number of sources. */
    val bigStories: List<Pair<String, Int>>,
    /** Stories published in each of the last 24 hours, oldest hour first. */
    val lastDay: List<Int>,
    /** Social posts with the most engagement (likes + comments + shares). */
    val mostEngaged: List<Article>,
    /** Stories per language code. */
    val languages: List<Pair<String, Int>>,
) {
    companion object {
        private const val HOUR = 3_600_000L
        private val WORD = Regex("\\p{L}[\\p{L}'’-]*")

        fun compute(
            articles: List<Article>,
            sources: List<SourceInfo>,
            hiddenSources: Set<String>,
            now: Long = System.currentTimeMillis(),
        ): Insights {
            val stories = articles.filter { it.kind != ContentKind.VIDEO }
            val languageOf = sources.associate { it.name to it.language }

            // Words per story (headline only), deduplicated within a headline.
            val wordsByStory = stories.map { story ->
                story to WORD.findAll(story.title.lowercase()).map { it.value.trim('\'', '’', '-') }
                    .filter { it.length >= 4 && it !in STOPWORDS }
                    .toList()
            }
            // Rank words and phrases by how many different sources use them, so one prolific
            // source can't dominate; ties go to the more frequent term.
            fun rankBySources(terms: Map<String, Pair<MutableSet<String>, Int>>, minSources: Int) =
                terms.filterValues { it.first.size >= minSources }
                    .entries
                    .sortedByDescending { it.value.first.size * 1_000_000L + it.value.second }
                    .map { it.key to it.value.first.size }

            val wordSources = HashMap<String, Pair<MutableSet<String>, Int>>()
            val phraseSources = HashMap<String, Pair<MutableSet<String>, Int>>()
            for ((story, words) in wordsByStory) {
                for (word in words.toSet()) {
                    val entry = wordSources.getOrPut(word) { mutableSetOf<String>() to 0 }
                    entry.first += story.source
                    wordSources[word] = entry.first to entry.second + 1
                }
                for (phrase in words.zipWithNext { a, b -> "$a $b" }.toSet()) {
                    val entry = phraseSources.getOrPut(phrase) { mutableSetOf<String>() to 0 }
                    entry.first += story.source
                    phraseSources[phrase] = entry.first to entry.second + 1
                }
            }

            val lastDay = IntArray(24)
            for (story in stories) {
                val hoursAgo = ((now - story.publishedAt) / HOUR).toInt()
                if (story.publishedAt > 0 && hoursAgo in 0..23) lastDay[23 - hoursAgo]++
            }

            return Insights(
                storyCount = stories.size,
                sourcesResponding = stories.map { it.source }.toSet().size,
                sourcesSelected = sources.count { it.kind != ContentKind.VIDEO && it.name !in hiddenSources },
                topicCount = stories.map { it.topic }.toSet().size,
                withImagesShare = if (stories.isEmpty()) 0f else stories.count { it.gridImageUrl != null }.toFloat() / stories.size,
                topics = stories.groupingBy { it.topic }.eachCount().toList().sortedByDescending { it.second },
                topSources = stories.groupingBy { displaySourceName(it.source) }.eachCount().toList()
                    .sortedByDescending { it.second }.take(10),
                trendingWords = rankBySources(wordSources, minSources = 2).take(15),
                bigStories = rankBySources(phraseSources, minSources = 3).take(8),
                lastDay = lastDay.toList(),
                mostEngaged = stories.filter { it.engagement != null }
                    .sortedByDescending { it.engagement!!.let { e -> (e.likes ?: 0) + (e.comments ?: 0) + (e.shares ?: 0) } }
                    .take(5),
                languages = stories.groupingBy { languageOf[it.source] ?: "en" }.eachCount().toList()
                    .sortedByDescending { it.second },
            )
        }

        /** Common English and Russian words that say nothing about a story's subject. */
        private val STOPWORDS = setOf(
            // English
            "this", "that", "with", "from", "have", "will", "your", "what", "when", "where", "which", "they",
            "their", "there", "been", "were", "after", "about", "into", "over", "more", "than", "said", "says",
            "says", "just", "also", "could", "would", "should", "first", "last", "year", "years", "week",
            "weeks", "today", "news", "new", "amid", "here", "still", "back", "make", "makes", "made", "take",
            "takes", "gets", "being", "while", "under", "against", "between", "before", "these", "those",
            "some", "many", "most", "other", "only", "very", "much", "does", "dont", "didn't", "doesn't",
            "time", "people", "world", "says", "live", "video", "watch", "update", "updates", "report",
            "reports", "how", "why", "who", "best", "know", "need", "want", "like", "every", "around",
            "through", "during", "next", "says", "could", "says", "says", "says", "ahead", "calls", "call",
            // Russian
            "этот", "этого", "этой", "того", "чтобы", "когда", "после", "более", "также", "может", "могут",
            "будет", "будут", "были", "было", "была", "есть", "свой", "своих", "свои", "своей", "которые",
            "который", "которая", "которое", "очень", "сейчас", "теперь", "только", "даже", "почему",
            "зачем", "через", "из-за", "перед", "между", "против", "около", "году", "года", "время",
            "заявил", "заявила", "заявили", "сообщил", "сообщили", "рассказал", "назвал", "новые",
            "новый", "новая", "нового", "всех", "всего", "всё", "будто", "день", "дней", "раза",
        )
    }
}
