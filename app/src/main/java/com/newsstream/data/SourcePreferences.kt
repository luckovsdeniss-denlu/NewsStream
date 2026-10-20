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

    /** The chosen story/image size (a ViewSize name), or null for the default. */
    var viewSize: String?
        get() = prefs.getString(KEY_VIEW_SIZE, null)
        set(value) = prefs.edit().putString(KEY_VIEW_SIZE, value).apply()

    private companion object {
        const val KEY_VIEW_SIZE = "view_size"
        const val KEY_SELECTED = "selected_sources"
        const val KEY_EXPANDED = "expanded_source_groups"
    }
}
