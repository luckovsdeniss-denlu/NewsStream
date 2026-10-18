package com.newsstream.data

import android.content.Context

/**
 * Remembers which sources the user switched off and which source groups they folded. Hidden sources are stored (rather than
 * selected ones) so sources added in future versions show up by default.
 */
class SourcePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("newsstream", Context.MODE_PRIVATE)

    var hiddenSources: Set<String>
        get() = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet(KEY_HIDDEN, value).apply()

    /** Labels of the source groups folded in the sources sheet. */
    var collapsedGroups: Set<String>
        get() = prefs.getStringSet(KEY_COLLAPSED, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet(KEY_COLLAPSED, value).apply()

    private companion object {
        const val KEY_HIDDEN = "hidden_sources"
        const val KEY_COLLAPSED = "collapsed_source_groups"
    }
}
