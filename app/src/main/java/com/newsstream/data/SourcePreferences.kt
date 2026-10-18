package com.newsstream.data

import android.content.Context

/**
 * Remembers which sources the user selected and which source groups they unfolded. Nothing is
 * selected at first: the user picks their sources, and sources added later also start off.
 */
class SourcePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("newsstream", Context.MODE_PRIVATE)

    var selectedSources: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet(KEY_SELECTED, value).apply()

    /**
     * Labels of the source groups the user unfolded in the sources sheet. Expanded groups are
     * stored (rather than folded ones) so every group, including ones added later, starts folded.
     */
    var expandedGroups: Set<String>
        get() = prefs.getStringSet(KEY_EXPANDED, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet(KEY_EXPANDED, value).apply()

    private companion object {
        const val KEY_SELECTED = "selected_sources"
        const val KEY_EXPANDED = "expanded_source_groups"
    }
}
