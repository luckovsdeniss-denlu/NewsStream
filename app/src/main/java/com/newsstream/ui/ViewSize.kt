package com.newsstream.ui

/**
 * How large stories and images are drawn, from one per screen to five per screen. One setting
 * applies to the news list, the image grid and Saved.
 */
enum class ViewSize(
    val label: String,
    /** Share of the screen height one news card takes. */
    val cardFraction: Float,
    /** Image grid columns (1 = one full-screen picture at a time). */
    val imageColumns: Int,
) {
    FULL("Full screen", 1f, 1),
    HALF("×2 smaller", 1f / 2, 2),
    THIRD("×3 smaller", 1f / 3, 3),
    FIFTH("×5 smaller", 1f / 5, 5),
    ;

    companion object {
        val DEFAULT = FIFTH

        fun fromName(name: String?): ViewSize = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
