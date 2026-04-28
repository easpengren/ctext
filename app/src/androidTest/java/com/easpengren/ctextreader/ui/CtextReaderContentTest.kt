package com.easpengren.ctextreader.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.easpengren.ctextreader.data.api.SearchTextBookDto
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
import org.junit.Rule
import org.junit.Test

class CtextReaderContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersReaderAndHistorySections() {
        composeRule.setContent {
            CtextReaderContent(
                state = CtextUiState(
                    statusText = "Loaded",
                    currentUrn = "ctp:analects/xue-er",
                    title = "學而",
                    paragraphs = listOf("子曰：學而時習之。"),
                    subsections = listOf("ctp:analects/wei-zheng"),
                    searchResults = listOf(SearchTextBookDto(title = "論語", urn = "ctp:analects")),
                    history = listOf(
                        ReaderHistoryEntry(
                            urn = "ctp:analects/xue-er",
                            title = "學而",
                            savedAtEpochMillis = 1714262400000L
                        )
                    )
                ),
                onUrnChange = {},
                onUrlChange = {},
                onSearchQueryChange = {},
                onCheckStatus = {},
                onLoadUrn = {},
                onLoadUrl = {},
                onSearchTexts = {},
                onOpenSearchResult = {},
                onSetInterfaceLanguage = {},
                onSetSimplifiedCharacters = {},
                onOpenExternalLink = {},
                onOpenSubsection = {},
                onOpenHistoryEntry = {},
                onNavigateBack = {},
                onClearHistory = {}
            )
        }

        composeRule.onNodeWithText("Reader Inputs").assertIsDisplayed()
        composeRule.onNodeWithText("Search Titles").assertIsDisplayed()
        composeRule.onNodeWithText("Search Results").assertIsDisplayed()
        composeRule.onNodeWithText("Subsections").assertIsDisplayed()
        composeRule.onNodeWithText("History").assertIsDisplayed()
        composeRule.onNodeWithText("學而").assertIsDisplayed()
    }
}