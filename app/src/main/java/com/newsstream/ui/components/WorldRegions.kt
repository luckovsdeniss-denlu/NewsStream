package com.newsstream.ui.components

import com.newsstream.data.SourceInfo

/**
 * Sub-groups of the World sources in the sources sheet, in display order. Russian-language
 * sources get their own sub-group; anything not listed here falls under "Global".
 */
internal val WORLD_REGIONS = listOf(
    "Global & wires",
    "Americas",
    "Europe",
    "Russia & Ukraine",
    "Asia & Pacific",
    "На русском",
)

private val REGION_OF = mapOf(
    // Americas
    "NPR" to "Americas",
    "The New York Times" to "Americas",
    "CBS News" to "Americas",
    // Europe
    "Sky News" to "Europe",
    "Deutsche Welle" to "Europe",
    "France 24" to "Europe",
    "The Independent" to "Europe",
    "Euronews" to "Europe",
    "Financial Times" to "Europe",
    // Russia & Ukraine (in English)
    "Kyiv Independent" to "Russia & Ukraine",
    "The Moscow Times" to "Russia & Ukraine",
    "Meduza in English" to "Russia & Ukraine",
    // Asia & Pacific
    "South China Morning Post" to "Asia & Pacific",
    "Times of India" to "Asia & Pacific",
    "The Japan Times" to "Asia & Pacific",
    "ABC News (Australia)" to "Asia & Pacific",
    "The Diplomat" to "Asia & Pacific",
)

internal fun worldRegion(source: SourceInfo): String = when {
    source.language == "ru" -> "На русском"
    else -> REGION_OF[source.name] ?: "Global & wires"
}
