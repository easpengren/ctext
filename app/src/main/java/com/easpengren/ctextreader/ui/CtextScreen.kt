package com.easpengren.ctextreader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CtextScreen(viewModel: CtextViewModel) {
    val state by viewModel.uiState.collectAsState()

    MaterialTheme {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "CText Reader",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                OutlinedTextField(
                    value = state.urnInput,
                    onValueChange = viewModel::updateUrn,
                    label = { Text("CTP URN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::checkStatus, enabled = !state.loading) {
                        Text("Check Status")
                    }
                    Button(onClick = viewModel::loadText, enabled = !state.loading) {
                        Text("Load Text")
                    }
                }
            }

            item {
                Text(text = state.statusText, style = MaterialTheme.typography.bodyMedium)
            }

            if (state.loading) {
                item { CircularProgressIndicator() }
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(state.paragraphs) { paragraph ->
                Text(text = paragraph, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
