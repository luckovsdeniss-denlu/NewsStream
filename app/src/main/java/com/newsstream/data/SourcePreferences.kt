package com.newsstream.data

import android.content.Context

/**
 * Remembers which sources the user switched off. Hidden sources are stored (rather than
 * selected ones) so sources added in future versions show up by default.
 */
class SourcePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("newsstream", Context.MODE_PRIVATE)

    var hiddenSources: Set<String>
        get() = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet(KEY_HIDDEN, value).apply()

    private companion object {
        const val KEY_HIDDEN = "hidden_sources"
    }
}
