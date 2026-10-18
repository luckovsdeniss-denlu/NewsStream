package com.newsstream.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.newsstream.data.Topic

val Topic.icon: ImageVector
    get() = when (this) {
        Topic.WORLD -> Icons.Filled.Public
        Topic.POLITICS -> Icons.Filled.AccountBalance
        Topic.BUSINESS -> Icons.Filled.AttachMoney
        Topic.TECH -> Icons.Filled.Memory
        Topic.SCIENCE -> Icons.Filled.Science
        Topic.CLIMATE -> Icons.Filled.Eco
        Topic.HEALTH -> Icons.Filled.LocalHospital
        Topic.SPORTS -> Icons.Filled.SportsSoccer
        Topic.CULTURE -> Icons.Filled.Movie
        Topic.GENERAL -> Icons.Filled.Newspaper
    }

/** A distinct, bright colour per topic, readable on the dark background. */
val Topic.color: Color
    get() = when (this) {
        Topic.WORLD -> Color(0xFF5AA9FF)
        Topic.POLITICS -> Color(0xFFB388FF)
        Topic.BUSINESS -> Color(0xFFFFC857)
        Topic.TECH -> Color(0xFF3DD5FF)
        Topic.SCIENCE -> Color(0xFF7CF29C)
        Topic.CLIMATE -> Color(0xFF4DD0A1)
        Topic.HEALTH -> Color(0xFFFF7A9A)
        Topic.SPORTS -> Color(0xFFFF9F43)
        Topic.CULTURE -> Color(0xFFF06EF0)
        Topic.GENERAL -> Color(0xFF9CCBFF)
    }

/** Colour for the social platforms group. */
val SocialColor = Color(0xFF8C9BFF)
