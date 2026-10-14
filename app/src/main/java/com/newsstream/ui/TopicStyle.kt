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
