package com.easpengren.ctextreader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CtextScreen(viewModel: CtextViewModel) {
    val state by viewModel.uiState.collectAsState()

    CtextReaderContent(
        state = state,
        onUrnChange = viewModel::updateUrn,
        onUrlChange = viewModel::updateUrl,
        onCheckStatus = viewModel::checkStatus,
        onLoadUrn = viewModel::loadText,
        onLoadUrl = viewModel::loadFromUrl,
        onOpenSubsection = viewModel::openSubsection,
        onOpenHistoryEntry = viewModel::openHistoryEntry,
        onNavigateBack = viewModel::navigateBack,
        onClearHistory = viewModel::clearHistory
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CtextReaderContent(
    state: CtextUiState,
    onUrnChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onCheckStatus: () -> Unit,
    onLoadUrn: () -> Unit,
    onLoadUrl: () -> Unit,
    onOpenSubsection: (String) -> Unit,
    onOpenHistoryEntry: (ReaderHistoryEntry) -> Unit,
    onNavigateBack: () -> Unit,
    onClearHistory: () -> Unit
) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CText Reader") },
                    actions = {
                        if (state.canGoBack) {
                            TextButton(onClick = onNavigateBack, enabled = !state.loading) {
                                Text("Back")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Reader Inputs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedTextField(
                                value = state.urnInput,
                                onValueChange = onUrnChange,
                                label = { Text("CTP URN") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = state.urlInput,
                                onValueChange = onUrlChange,
                                label = { Text("ctext.org URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onLoadUrn, enabled = !state.loading) {
                                    Text("Load URN")
                                }
                                OutlinedButton(onClick = onLoadUrl, enabled = !state.loading) {
                                    Text("Open URL")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onCheckStatus, enabled = !state.loading) {
                                    Text("Check Status")
                                }
                                if (state.history.isNotEmpty()) {
                                    OutlinedButton(onClick = onClearHistory, enabled = !state.loading) {
                                        Text("Clear History")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Session",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = state.statusText, style = MaterialTheme.typography.bodyMedium)
                            if (state.currentUrn.isNotBlank()) {
                                Text(text = "Current URN: ${state.currentUrn}")
                            }
                            state.directLink?.takeIf { it.isNotBlank() }?.let { link ->
                                Text(text = "Direct Link: $link", style = MaterialTheme.typography.bodySmall)
                            }
                            if (state.loading) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }

                state.error?.let { errorText ->
                    item {
                        Card {
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (state.title.isNotBlank()) {
                    item {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (state.subsections.isNotEmpty()) {
                    item {
                        Text(
                            text = "Subsections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(state.subsections) { subsectionUrn ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !state.loading) { onOpenSubsection(subsectionUrn) }
                        ) {
                            Text(
                                text = subsectionUrn,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (state.paragraphs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Text",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                items(state.paragraphs) { paragraph ->
                    Text(text = paragraph, style = MaterialTheme.typography.bodyLarge)
                }

                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (state.history.isEmpty()) {
                    item {
                        Text(
                            text = "No saved reading history yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(state.history, key = { "${it.urn}-${it.savedAtEpochMillis}" }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !state.loading) { onOpenHistoryEntry(entry) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = entry.title, fontWeight = FontWeight.SemiBold)
                            Text(text = entry.urn, style = MaterialTheme.typography.bodySmall)
                            entry.link?.takeIf { it.isNotBlank() }?.let { link ->
                                Text(text = link, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = formatHistoryTimestamp(entry.savedAtEpochMillis),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatHistoryTimestamp(value: Long): String {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(value))
}
