package com.newsstream.data

import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Statistical analyses of the loaded stories for the Insights tabs: bursts and trends in words,
 * distinctive vocabulary, source similarity, story clusters, diversity, exclusives and speed,
 * tone with confidence margins, and social engagement. Heavier than [Insights], so it is computed
 * off the main thread.
 */
data class Statistics(
    /** (1) Words with a sudden burst in the last 3 hours compared with the 21 before. */
    val spikes: List<Spike>,
    /** (3) Words mentioned at a much higher (or lower) rate in the last 6 hours than the 18 before. */
    val rising: List<Growth>,
    val fading: List<Growth>,
    /** (5) Words significantly over-used in the last 6 hours (log-likelihood keyness). */
    val keywords: List<Keyness>,
    /** (6) Word pairs that appear together far more often than chance (normalised PMI). */
    val wordPairs: List<WordPair>,
    /** (4) Each source's most distinctive words (TF-IDF), largest sources first. */
    val sourceWords: List<Pair<String, List<String>>>,
    /** (4) Each topic's most distinctive words (TF-IDF). */
    val topicWords: List<Pair<Topic, List<String>>>,
    /** (7) Source pairs whose vocabulary is most alike (cosine similarity of TF-IDF vectors). */
    val similarSources: List<SourcePair>,
    /** (7) Sources least like any other: average similarity, lowest first. */
    val uniqueSources: List<Pair<String, Float>>,
    /** (8) Evenness of stories across topics, 0 (one topic) to 1 (perfectly even). */
    val topicDiversity: Float,
    /** (8) Evenness across distinct stories (clusters), 0 (one story dominates) to 1. */
    val storyDiversity: Float,
    /** (8) Share of all stories that belong to the biggest story. */
    val topStoryShare: Float,
    /** (10) Stories covered by several outlets: near-duplicate headlines grouped together. */
    val clusters: List<StoryCluster>,
    /** (9) Share of each source's stories that no other loaded source has, most exclusive first. */
    val exclusives: List<Pair<String, Float>>,
    /** (9) Median delay of each source behind the first report on shared stories, fastest first. */
    val fastestSources: List<SourceLag>,
    /** (11) Net tone per topic and per source with a 95% margin. */
    val toneByTopic: List<ToneStat>,
    val toneBySource: List<ToneStat>,
    /** (12) Social posts ranked within their own platform, best first. */
    val topPercentile: List<PostRank>,
    /** (13) Median engagement per topic and per time of day. */
    val engagementByTopic: List<Pair<Topic, MedianStat>>,
    val engagementByTimeOfDay: List<Pair<String, MedianStat>>,
    /** (13) Spearman correlation between a post's age and its engagement; null with too few posts. */
    val ageCorrelation: Float?,
    val socialPostCount: Int,
) {
    data class Spike(val word: String, val recent: Int, val baseline: Float, val z: Float)
    data class Growth(val word: String, val recent: Int, val earlier: Int, val change: Float)
    data class Keyness(val word: String, val recent: Int, val earlier: Int, val g2: Float)
    data class WordPair(val first: String, val second: String, val together: Int, val npmi: Float)
    data class SourcePair(val first: String, val second: String, val similarity: Float)
    data class StoryCluster(val stories: List<Article>, val sources: List<String>)
    data class SourceLag(val source: String, val medianMs: Long, val shared: Int)
    data class ToneStat(val label: String, val stories: Int, val net: Float, val margin: Float)
    data class PostRank(val post: Article, val platform: String, val rank: Int, val of: Int, val total: Int)
    data class MedianStat(val median: Int, val posts: Int)

    companion object {
        private const val HOUR = 3_600_000L

        /** Average of each hour with its neighbours (a centred 3-hour window). */
        fun movingAverage(counts: List<Int>, window: Int = 3): List<Float> = counts.indices.map { i ->
            val from = max(0, i - window / 2)
            val to = minOf(counts.lastIndex, i + window / 2)
            (from..to).sumOf { counts[it] }.toFloat() / (to - from + 1)
        }

        /**
         * How unusual the last 3 hours are for these story times: (recent - mean) / spread over
         * the seven 3-hour blocks before, with a Poisson floor on the spread so tiny counts don't
         * produce huge scores. Null when there's nothing recent.
         */
        fun burstScore(times: List<Long>, now: Long): Float? {
            val bins = IntArray(8)
            for (t in times) {
                val hoursAgo = (now - t) / HOUR
                if (t > 0 && hoursAgo in 0L..23L) bins[7 - (hoursAgo / 3).toInt()]++
            }
            if (bins[7] == 0) return null
            val base = bins.take(7)
            val mean = base.average().toFloat()
            val variance = base.sumOf { ((it - mean) * (it - mean)).toDouble() }.toFloat() / 7
            return (bins[7] - mean) / sqrt(maxOf(variance, mean, 0.5f))
        }

        fun compute(articles: List<Article>, now: Long = System.currentTimeMillis()): Statistics {
            val stories = articles.filter { it.kind != ContentKind.VIDEO }
            val headline = stories.associateWith { Insights.headlineWords(it.title).toSet() }

            // --- Words over time (1, 3, 5) ---
            val timesByWord = HashMap<String, MutableList<Long>>()
            val recentCount = HashMap<String, Int>()
            val earlierCount = HashMap<String, Int>()
            for (story in stories) {
                val age = now - story.publishedAt
                if (story.publishedAt <= 0 || age !in 0 until 24 * HOUR) continue
                val recent = age < 6 * HOUR
                for (word in headline.getValue(story)) {
                    timesByWord.getOrPut(word) { mutableListOf() } += story.publishedAt
                    if (recent) recentCount[word] = (recentCount[word] ?: 0) + 1
                    else earlierCount[word] = (earlierCount[word] ?: 0) + 1
                }
            }
            val spikes = timesByWord.mapNotNull { (word, times) ->
                val recent = times.count { now - it < 3 * HOUR }
                val z = burstScore(times, now) ?: return@mapNotNull null
                if (recent < 3 || z < 2f) return@mapNotNull null
                Spike(word, recent, (times.size - recent) / 7f, z)
            }.sortedByDescending { it.z }.take(8)

            val growth = (recentCount.keys + earlierCount.keys).map { word ->
                val r = recentCount[word] ?: 0
                val e = earlierCount[word] ?: 0
                // Rates per hour, with half a mention added so a zero doesn't divide.
                Growth(word, r, e, ((r + 0.5f) / 6f) / ((e + 0.5f) / 18f) - 1f)
            }
            val rising = growth.filter { it.recent >= 3 && it.change >= 0.5f }.sortedByDescending { it.change }.take(10)
            val fading = growth.filter { it.earlier >= 4 && it.change <= -0.33f }.sortedBy { it.change }.take(10)

            val recentTotal = recentCount.values.sum().toDouble()
            val earlierTotal = earlierCount.values.sum().toDouble()
            val keywords = if (recentTotal == 0.0 || earlierTotal == 0.0) emptyList() else growth.mapNotNull { g ->
                val a = g.recent.toDouble()
                val b = g.earlier.toDouble()
                if (g.recent < 3 || a / recentTotal <= b / earlierTotal) return@mapNotNull null
                val e1 = recentTotal * (a + b) / (recentTotal + earlierTotal)
                val e2 = earlierTotal * (a + b) / (recentTotal + earlierTotal)
                val g2 = 2 * ((if (a > 0) a * ln(a / e1) else 0.0) + (if (b > 0) b * ln(b / e2) else 0.0))
                // 6.63 is the 1% significance threshold (chi-squared, one degree of freedom).
                if (g2 >= 6.63) Keyness(g.word, g.recent, g.earlier, g2.toFloat()) else null
            }.sortedByDescending { it.g2 }.take(10)

            // --- Word pairs (6) ---
            val docs = stories.size.toDouble()
            val wordDocs = HashMap<String, Int>()
            for (words in headline.values) for (w in words) wordDocs[w] = (wordDocs[w] ?: 0) + 1
            val pairCount = HashMap<String, Int>()
            val pairSources = HashMap<String, MutableSet<String>>()
            for (story in stories) {
                val words = headline.getValue(story).filter { (wordDocs[it] ?: 0) >= 3 }.sorted().take(20)
                for (i in words.indices) for (j in i + 1 until words.size) {
                    val key = words[i] + " " + words[j]
                    pairCount[key] = (pairCount[key] ?: 0) + 1
                    pairSources.getOrPut(key) { mutableSetOf() } += story.source
                }
            }
            val wordPairs = pairCount.mapNotNull { (key, together) ->
                if (together < 3 || (pairSources[key]?.size ?: 0) < 2) return@mapNotNull null
                val (a, b) = key.split(' ')
                val pxy = together / docs
                val pmi = ln(pxy / ((wordDocs.getValue(a) / docs) * (wordDocs.getValue(b) / docs)))
                WordPair(a, b, together, (pmi / -ln(pxy)).toFloat())
            }.sortedWith(compareByDescending<WordPair> { it.npmi }.thenByDescending { it.together }).take(10)

            // --- Distinctive words and similarity (4, 7) ---
            fun textWords(a: Article) = Insights.headlineWords(a.title + " " + a.summary.take(400))
            val bySource = stories.groupBy { displaySourceName(it.source) }.filterValues { it.size >= 5 }
            val sourceVectors = tfIdf(bySource.mapValues { (_, list) -> list.flatMap(::textWords) })
            val sourceWords = bySource.entries.sortedByDescending { it.value.size }.take(12).mapNotNull { (name, _) ->
                sourceVectors[name]?.let { v -> name to v.entries.sortedByDescending { it.value }.take(5).map { it.key } }
            }.filter { it.second.isNotEmpty() }
            val byTopic = stories.groupBy { it.topic }.filterValues { it.size >= 5 }
            val topicVectors = tfIdf(byTopic.mapValues { (_, list) -> list.flatMap(::textWords) })
            val topicWords = byTopic.entries.sortedByDescending { it.value.size }.mapNotNull { (topic, _) ->
                topicVectors[topic]?.let { v -> topic to v.entries.sortedByDescending { it.value }.take(6).map { it.key } }
            }.filter { it.second.isNotEmpty() }

            val names = sourceVectors.keys.toList()
            val norms = names.associateWith { n -> sqrt(sourceVectors.getValue(n).values.sumOf { (it * it).toDouble() }) }
            val pairs = mutableListOf<SourcePair>()
            val similaritySum = HashMap<String, Double>()
            for (i in names.indices) for (j in i + 1 until names.size) {
                val a = sourceVectors.getValue(names[i])
                val b = sourceVectors.getValue(names[j])
                val (small, large) = if (a.size <= b.size) a to b else b to a
                val dot = small.entries.sumOf { (w, x) -> (x * (large[w] ?: 0f)).toDouble() }
                val norm = norms.getValue(names[i]) * norms.getValue(names[j])
                val sim = if (norm == 0.0) 0.0 else dot / norm
                pairs += SourcePair(names[i], names[j], sim.toFloat())
                similaritySum[names[i]] = (similaritySum[names[i]] ?: 0.0) + sim
                similaritySum[names[j]] = (similaritySum[names[j]] ?: 0.0) + sim
            }
            val similarSources = pairs.sortedByDescending { it.similarity }.take(10)
            val uniqueSources = if (names.size < 3) emptyList() else names
                .map { it to ((similaritySum[it] ?: 0.0) / (names.size - 1)).toFloat() }
                .sortedBy { it.second }.take(6)

            // --- Story clusters (10), diversity (8), exclusives and speed (9) ---
            val clusters = clusterStories(stories, headline)
            val inCluster = clusters.flatMap { it.stories }.map { it.id }.toSet()
            val topicDiversity = evenness(stories.groupingBy { it.topic }.eachCount().values, Topic.entries.size)
            val units = clusters.map { it.stories.size } + List(stories.size - inCluster.size) { 1 }
            val storyDiversity = evenness(units, units.size)
            val topStoryShare = if (stories.isEmpty()) 0f else (clusters.maxOfOrNull { it.stories.size } ?: 1).toFloat() / stories.size

            val exclusives = stories.groupBy { displaySourceName(it.source) }.filterValues { it.size >= 5 }
                .map { (name, list) -> name to list.count { it.id !in inCluster }.toFloat() / list.size }
                .sortedByDescending { it.second }.take(10)

            val lags = HashMap<String, MutableList<Long>>()
            for (cluster in clusters) {
                val dated = cluster.stories.filter { it.publishedAt > 0 }
                if (dated.map { it.source }.toSet().size < 3) continue
                val first = dated.minOf { it.publishedAt }
                dated.groupBy { displaySourceName(it.source) }.forEach { (name, list) ->
                    lags.getOrPut(name) { mutableListOf() } += list.minOf { it.publishedAt } - first
                }
            }
            val fastestSources = lags.filterValues { it.size >= 2 }
                .map { (name, list) -> SourceLag(name, median(list.map { it.toDouble() }).toLong(), list.size) }
                .sortedBy { it.medianMs }.take(10)

            // --- Tone (11) ---
            val tone = stories.associateWith { ToneLexicon.score(it).coerceIn(-1, 1) }
            fun toneStat(label: String, list: List<Article>): ToneStat {
                val n = list.size
                val pos = list.count { tone.getValue(it) > 0 }
                val neg = list.count { tone.getValue(it) < 0 }
                val net = (pos - neg).toFloat() / n
                // Each story scores +1, 0 or -1; margin is 1.96 standard errors of their mean.
                val variance = (pos + neg).toFloat() / n - net * net
                return ToneStat(label, n, net, (1.96 * sqrt(max(variance, 0f) / n.toDouble())).toFloat())
            }
            val toneByTopic = stories.groupBy { it.topic }.filterValues { it.size >= 10 }
                .map { (topic, list) -> toneStat(topic.label, list) }.sortedByDescending { it.net }
            val toneBySource = stories.groupBy { displaySourceName(it.source) }.filterValues { it.size >= 8 }
                .entries.sortedByDescending { it.value.size }.take(12)
                .map { (name, list) -> toneStat(name, list) }.sortedByDescending { it.net }

            // --- Social engagement (12, 13) ---
            val posts = stories.filter { it.engagement != null }
            fun total(a: Article) = a.engagement!!.let { (it.likes ?: 0) + (it.comments ?: 0) + (it.shares ?: 0) }
            val topPercentile = posts.groupBy { displaySourceName(it.source) }.filterValues { it.size >= 5 }
                .flatMap { (platform, list) ->
                    list.sortedByDescending(::total).take(2).mapIndexed { i, post ->
                        PostRank(post, platform, i + 1, list.size, total(post))
                    }
                }
                .sortedWith(compareBy<PostRank> { it.rank.toFloat() / it.of }.thenByDescending { it.total })
                .take(8)
            val engagementByTopic = posts.groupBy { it.topic }.filterValues { it.size >= 5 }
                .map { (topic, list) -> topic to MedianStat(median(list.map { total(it).toDouble() }).toInt(), list.size) }
                .sortedByDescending { it.second.median }
            val calendar = Calendar.getInstance()
            val dayParts = listOf("Night (0–6)", "Morning (6–12)", "Afternoon (12–18)", "Evening (18–24)")
            val engagementByTimeOfDay = posts.filter { it.publishedAt > 0 }
                .groupBy { post ->
                    calendar.timeInMillis = post.publishedAt
                    dayParts[calendar.get(Calendar.HOUR_OF_DAY) / 6]
                }
                .filterValues { it.size >= 3 }
                .map { (part, list) -> part to MedianStat(median(list.map { total(it).toDouble() }).toInt(), list.size) }
                .sortedBy { dayParts.indexOf(it.first) }
            val dated = posts.filter { it.publishedAt > 0 }
            val ageCorrelation = if (dated.size < 8) null else spearman(
                dated.map { (now - it.publishedAt).toDouble() },
                dated.map { total(it).toDouble() },
            )

            return Statistics(
                spikes = spikes,
                rising = rising,
                fading = fading,
                keywords = keywords,
                wordPairs = wordPairs,
                sourceWords = sourceWords,
                topicWords = topicWords,
                similarSources = similarSources,
                uniqueSources = uniqueSources,
                topicDiversity = topicDiversity,
                storyDiversity = storyDiversity,
                topStoryShare = topStoryShare,
                clusters = clusters.filter { it.sources.size >= 2 }.take(10),
                exclusives = exclusives,
                fastestSources = fastestSources,
                toneByTopic = toneByTopic,
                toneBySource = toneBySource,
                topPercentile = topPercentile,
                engagementByTopic = engagementByTopic,
                engagementByTimeOfDay = engagementByTimeOfDay,
                ageCorrelation = ageCorrelation,
                socialPostCount = posts.size,
            )
        }

        /**
         * TF-IDF weights per document: how often a word is used in this document, times how rare
         * it is across documents. Words used only once in a document are dropped as noise.
         */
        private fun <K> tfIdf(documents: Map<K, List<String>>): Map<K, Map<String, Float>> {
            if (documents.size < 3) return emptyMap()
            val df = HashMap<String, Int>()
            val counts = documents.mapValues { (_, words) -> words.groupingBy { it }.eachCount() }
            for (c in counts.values) for (w in c.keys) df[w] = (df[w] ?: 0) + 1
            val n = documents.size.toDouble()
            return counts.mapValues { (key, c) ->
                val length = documents.getValue(key).size.toFloat()
                c.filterValues { it >= 2 }.mapValues { (w, count) ->
                    (count / length) * ln(n / df.getValue(w)).toFloat()
                }.filterValues { it > 0f }
            }
        }

        /**
         * Groups stories whose headlines share most of their meaningful words (Jaccard similarity
         * of at least 0.3 on 2+ shared words), linking chains of similar headlines into one story.
         * Only words used by 2–60 headlines are compared, which keeps it fast and ignores words
         * too common to identify a story. Clusters come back with the most sources first.
         */
        private fun clusterStories(stories: List<Article>, words: Map<Article, Set<String>>): List<StoryCluster> {
            val usable = stories.filter { words.getValue(it).size >= 3 }
            val df = HashMap<String, MutableList<Int>>()
            usable.forEachIndexed { i, story -> for (w in words.getValue(story)) df.getOrPut(w) { mutableListOf() } += i }
            val shared = HashMap<Long, Int>()
            for (postings in df.values) {
                if (postings.size !in 2..60) continue
                for (x in postings.indices) for (y in x + 1 until postings.size) {
                    val key = postings[x].toLong() * usable.size + postings[y]
                    shared[key] = (shared[key] ?: 0) + 1
                }
            }
            val parent = IntArray(usable.size) { it }
            fun find(i: Int): Int {
                var r = i
                while (parent[r] != r) r = parent[r]
                var c = i
                while (parent[c] != r) { val next = parent[c]; parent[c] = r; c = next }
                return r
            }
            for ((key, common) in shared) {
                if (common < 2) continue
                val i = (key / usable.size).toInt()
                val j = (key % usable.size).toInt()
                val union = words.getValue(usable[i]).size + words.getValue(usable[j]).size - common
                if (common.toFloat() / union >= 0.3f) parent[find(i)] = find(j)
            }
            return usable.indices.groupBy(::find).values
                .filter { it.size >= 2 }
                .map { group ->
                    val list = group.map { usable[it] }
                        .sortedWith(compareBy<Article> { it.publishedAt == 0L }.thenBy { it.publishedAt })
                    StoryCluster(list, list.map { displaySourceName(it.source) }.distinct())
                }
                .sortedWith(compareByDescending<StoryCluster> { it.sources.size }.thenByDescending { it.stories.size })
        }

        /** Shannon entropy of the counts divided by its maximum for [categories]: 0 to 1. */
        private fun evenness(counts: Collection<Int>, categories: Int): Float {
            val total = counts.sum().toDouble()
            if (total == 0.0 || categories < 2) return 0f
            val h = counts.filter { it > 0 }.sumOf { val p = it / total; -p * ln(p) }
            return (h / ln(categories.toDouble())).toFloat()
        }

        private fun median(values: List<Double>): Double {
            if (values.isEmpty()) return 0.0
            val sorted = values.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
        }

        /** Spearman rank correlation (ties get their average rank). */
        private fun spearman(x: List<Double>, y: List<Double>): Float {
            fun ranks(v: List<Double>): DoubleArray {
                val order = v.indices.sortedBy { v[it] }
                val r = DoubleArray(v.size)
                var i = 0
                while (i < order.size) {
                    var j = i
                    while (j + 1 < order.size && v[order[j + 1]] == v[order[i]]) j++
                    val avg = (i + j) / 2.0 + 1
                    for (k in i..j) r[order[k]] = avg
                    i = j + 1
                }
                return r
            }
            val rx = ranks(x)
            val ry = ranks(y)
            val mx = rx.average()
            val my = ry.average()
            var num = 0.0
            var dx = 0.0
            var dy = 0.0
            for (k in rx.indices) {
                num += (rx[k] - mx) * (ry[k] - my)
                dx += (rx[k] - mx) * (rx[k] - mx)
                dy += (ry[k] - my) * (ry[k] - my)
            }
            return if (dx == 0.0 || dy == 0.0) 0f else (num / sqrt(dx * dy)).toFloat()
        }

        /** Describes a correlation's strength in words. */
        fun describeCorrelation(r: Float): String = when (abs(r)) {
            in 0f..0.1f -> "no real link"
            in 0.1f..0.3f -> "a weak link"
            in 0.3f..0.5f -> "a moderate link"
            else -> "a strong link"
        }
    }
}
