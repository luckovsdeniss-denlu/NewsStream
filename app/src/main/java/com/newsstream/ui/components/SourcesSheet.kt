package com.newsstream.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.newsstream.data.SourceInfo
import com.newsstream.ui.NewsUiState

/** Callbacks for changing which sources are shown. */
class SourceActions(
    val setVisible: (name: String, visible: Boolean) -> Unit,
    val showAll: () -> Unit,
    val hideAll: () -> Unit,
    /** Shows only sources in this language ("en", "ru"), or every source for null. */
    val showLanguage: (language: String?) -> Unit,
)

private val LANGUAGES = listOf(null to "All", "en" to "English", "ru" to "Русский")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesSheet(state: NewsUiState, actions: SourceActions, onDismiss: () -> Unit) {
    val selectedCount = state.sources.count { it.name !in state.hiddenSources }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SOURCES", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "$selectedCount of ${state.sources.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                TextButton(onClick = actions.showAll) { Text("Select all") }
                TextButton(onClick = actions.hideAll) { Text("None") }
            }
            // Language quick filter: highlighted when exactly that language's sources are on.
            val visibleLanguages = state.sources.filter { it.name !in state.hiddenSources }.map { it.language }.toSet()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LANGUAGES.forEach { (code, label) ->
                    val active = if (code == null) state.hiddenSources.isEmpty() else visibleLanguages == setOf(code)
                    FilterChip(
                        selected = active,
                        onClick = { actions.showLanguage(code) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            labelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            // One flat, alphabetical list of source names.
            val sorted = remember(state.sources) { state.sources.sortedBy { it.name.lowercase() } }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(sorted, key = { it.name }) { source ->
                    SourceRow(
                        source = source,
                        selected = source.name !in state.hiddenSources,
                        failed = source.name in state.allFailedSources,
                        onToggle = { actions.setVisible(source.name, it) },
                    )
                }
            }
        }
    }
}

/** Checkbox and name only; a source that failed to load shows its name in the error colour. */
@Composable
private fun SourceRow(
    source: SourceInfo,
    selected: Boolean,
    failed: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle(!selected) }
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        Text(
            source.name,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                failed -> MaterialTheme.colorScheme.error
                selected -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
