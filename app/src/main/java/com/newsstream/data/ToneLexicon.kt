package com.newsstream.data

/**
 * A rough, word-based tone reading (English and Russian): positive minus negative word stems in
 * a story's headline and summary. Good for comparing groups of stories, not for judging one.
 */
object ToneLexicon {
    private val WORD = Regex("\\p{L}+")

    /** Positive minus negative words in the story's headline and summary. */
    fun score(story: Article): Int {
        val words = WORD.findAll((story.title + " " + story.summary).lowercase()).map { it.value }.toList()
        return words.count { matchesStem(it, POSITIVE) } - words.count { matchesStem(it, NEGATIVE) }
    }

    /** Short stems (4 letters or fewer) must match the whole word, so "war" doesn't hit "warm". */
    private fun matchesStem(word: String, stems: List<String>) =
        stems.any { if (it.length <= 4) word == it else word.startsWith(it) }

    /** Word stems that usually signal good news (English and Russian). Matched as prefixes. */
    private val POSITIVE = listOf(
        "win", "wins", "won", "success", "record", "boost", "growth", "grow", "gain", "rise", "rally",
        "improv", "breakthrough", "agree", "deal", "peace", "recover", "celebrat", "launch", "approv",
        "save", "saved", "hope", "best", "award", "help", "rescue", "advance", "surge", "strong",
        "побед", "успех", "успеш", "рекорд", "рост", "вырос", "улучш", "прорыв", "соглаш",
        "восстанов", "запуст", "одобр", "спас", "помог", "наград", "лучш", "открыт", "развит",
    )

    /** Word stems that usually signal bad news (English and Russian). Matched as prefixes. */
    private val NEGATIVE = listOf(
        "kill", "kills", "killed", "dead", "death", "dies", "died", "war", "wars", "attack", "crash", "crisis", "fall", "falls", "fell",
        "drop", "drops", "loss", "lose", "lost", "fail", "fear", "threat", "warn", "ban", "fire", "flood",
        "storm", "strike", "protest", "scandal", "fraud", "arrest", "charged", "sued", "lawsuit",
        "cut", "cuts", "layoff", "collapse", "decline", "injur", "victim", "shoot", "bomb", "sanction",
        "убит", "погиб", "смерт", "войн", "атак", "удар", "крах", "кризис", "паден", "упал", "сниж",
        "потер", "угроз", "запрет", "пожар", "наводн", "авари", "катастроф", "скандал", "мошен",
        "арест", "задерж", "обвин", "санкц", "протест", "взрыв", "ранен", "жертв",
    )
}
