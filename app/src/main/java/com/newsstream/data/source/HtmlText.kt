package com.newsstream.data.source

import android.text.Html

internal fun htmlToText(html: String): String =
    Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().replace('￼', ' ').trim()
