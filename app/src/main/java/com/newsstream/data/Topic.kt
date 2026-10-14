package com.newsstream.data

enum class Topic(val label: String) {
    WORLD("World"),
    POLITICS("Politics"),
    BUSINESS("Business"),
    TECH("Technology"),
    SCIENCE("Science"),
    CLIMATE("Climate"),
    HEALTH("Health"),
    SPORTS("Sports"),
    CULTURE("Culture"),
    GENERAL("General"),
}

/**
 * Assigns a [Topic] by keyword matching. Category hints from the source (feed categories,
 * section names) count most, then the title, then the summary; with no match at all the
 * source's own fallback topic is used.
 */
object TopicClassifier {

    private val KEYWORDS: Map<Topic, List<String>> = mapOf(
        Topic.WORLD to listOf(
            "world", "war", "ukraine", "russia", "gaza", "israel", "iran", "china", "military", "troops",
            "refugee", "united nations", "nato", "ceasefire", "embassy", "border", "protest", "conflict",
            "missile", "invasion", "hostage", "africa", "middle east", "europe", "asia",
        ),
        Topic.POLITICS to listOf(
            "politic", "election", "parliament", "senate", "congress", "president", "prime minister",
            "minister", "government", "vote", "voter", "campaign", "democrat", "republican", "labour",
            "conservative", "lawmaker", "white house", "kremlin", "legislation", "supreme court", "policy",
            "diplomat", "sanction", "governor", "mayor",
        ),
        Topic.BUSINESS to listOf(
            "business", "market", "stock", "shares", "economy", "economic", "inflation", "interest rate",
            "bank", "earnings", "profit", "revenue", "investor", "trade", "tariff", "ipo", "ceo",
            "wall street", "finance", "financial", "crypto", "bitcoin", "recession", "gdp", "merger",
            "acquisition", "layoff", "retail", "oil price",
        ),
        Topic.TECH to listOf(
            "tech", "ai", "artificial intelligence", "software", "app", "apps", "apple", "google",
            "microsoft", "openai", "chip", "semiconductor", "smartphone", "iphone", "android", "cyber",
            "hacker", "hacked", "data breach", "robot", "computer", "internet", "linux", "programming",
            "nvidia", "gpu", "startup", "silicon valley", "algorithm", "developer", "open source", "browser",
        ),
        Topic.SCIENCE to listOf(
            "science", "scientist", "nasa", "space", "planet", "astronom", "research", "physics",
            "telescope", "mars", "moon", "rocket", "spacex", "dinosaur", "fossil", "species", "quantum",
            "genome", "galaxy", "asteroid", "biolog", "chemist", "satellite", "orbit",
        ),
        Topic.CLIMATE to listOf(
            "climate", "emission", "carbon", "global warming", "wildfire", "flood", "hurricane", "storm",
            "drought", "heatwave", "renewable", "solar power", "wind power", "environment", "pollution",
            "fossil fuel", "biodiversity", "deforestation", "glacier", "sea level",
        ),
        Topic.HEALTH to listOf(
            "health", "hospital", "disease", "virus", "vaccine", "covid", "cancer", "medical", "medicine",
            "doctor", "patient", "outbreak", "pandemic", "fda", "mental health", "obesity", "nhs",
            "diabetes", "infection", "surgery", "drug",
        ),
        Topic.SPORTS to listOf(
            "sport", "football", "soccer", "nba", "nfl", "tennis", "cricket", "olympic", "world cup",
            "premier league", "championship", "tournament", "coach", "formula 1", "f1", "golf", "rugby",
            "baseball", "basketball", "athlete", "striker", "quarterback", "grand prix", "uefa", "fifa",
        ),
        Topic.CULTURE to listOf(
            "culture", "film", "movie", "music", "album", "singer", "actor", "actress", "celebrity",
            "tv series", "netflix", "hollywood", "box office", "festival", "oscar", "grammy",
            "video game", "gaming", "book", "novel", "artist", "museum", "fashion", "theatre", "theater",
            "television", "streaming",
        ),
    )

    // Short keywords ("ai", "app") must match whole words; longer ones may match as prefixes
    // so "election" also catches "elections".
    private val PATTERNS: Map<Topic, List<Regex>> = KEYWORDS.mapValues { (_, words) ->
        words.map { word ->
            val escaped = Regex.escape(word)
            Regex(if (word.length <= 3) "\\b$escaped\\b" else "\\b$escaped")
        }
    }

    fun classify(title: String, summary: String = "", hints: String = "", fallback: Topic = Topic.GENERAL): Topic {
        val t = title.lowercase()
        val s = summary.lowercase()
        val h = hints.lowercase()
        val scores = PATTERNS.mapValues { (_, patterns) ->
            patterns.sumOf { p ->
                (if (p.containsMatchIn(h)) 3 else 0) +
                    (if (p.containsMatchIn(t)) 2 else 0) +
                    (if (p.containsMatchIn(s)) 1 else 0)
            }
        }
        val best = scores.maxByOrNull { it.value } ?: return fallback
        return if (best.value > 0) best.key else fallback
    }
}
