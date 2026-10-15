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
import com.newsstream.data.ContentKind
import com.newsstream.data.SourceInfo
import com.newsstream.ui.NewsUiState

/** Callbacks for changing which sources are shown. */
class SourceActions(
    val setVisible: (name: String, visible: Boolean) -> Unit,
    val showAll: () -> Unit,
    val hideAll: () -> Unit,
    val showOnly: (name: String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesSheet(state: NewsUiState, actions: SourceActions, onDismiss: () -> Unit) {
    val counts = remember(state.allArticles) { state.allArticles.groupingBy { it.source }.eachCount() }
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
            LazyColumn(Modifier.fillMaxWidth()) {
                for (kind in ContentKind.entries) {
                    val group = state.sources.filter { it.kind == kind }
                    if (group.isEmpty()) continue
                    item(key = "header-$kind") {
                        Text(
                            if (kind == ContentKind.NEWS) "NEWS" else "SOCIAL",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(group, key = { it.name }) { source ->
                        SourceRow(
                            source = source,
                            selected = source.name !in state.hiddenSources,
                            count = counts[source.name] ?: 0,
                            failed = source.name in state.allFailedSources,
                            onToggle = { actions.setVisible(source.name, it) },
                            onOnly = { actions.showOnly(source.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    source: SourceInfo,
    selected: Boolean,
    count: Int,
    failed: Boolean,
    onToggle: (Boolean) -> Unit,
    onOnly: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle(!selected) }
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
        Column(Modifier.weight(1f)) {
            Text(
                source.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (failed) "Couldn't load" else "$count stories",
                style = MaterialTheme.typography.labelSmall,
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onOnly) { Text("Only") }
    }
}
