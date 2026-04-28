package com.easpengren.ctextreader.ui

import com.easpengren.ctextreader.data.api.GetLinkResponseDto
import com.easpengren.ctextreader.data.api.GetTextResponseDto
import com.easpengren.ctextreader.data.api.ReadLinkResponseDto
import com.easpengren.ctextreader.data.api.StatusResponseDto
import com.easpengren.ctextreader.domain.model.ApiResult
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
import com.easpengren.ctextreader.domain.repository.CtextRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CtextViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load from url resolves urn and updates history`() = runTest {
        val repository = FakeCtextRepository(
            readLinkResult = ApiResult.Success(ReadLinkResponseDto(urn = "ctp:analects/xue-er")),
            getTextResult = ApiResult.Success(
                GetTextResponseDto(
                    title = "學而",
                    fulltext = listOf("子曰：學而時習之。")
                )
            ),
            getLinkResult = ApiResult.Success(GetLinkResponseDto(link = "https://ctext.org/analects/xue-er"))
        )
        val viewModel = CtextViewModel(repository)

        viewModel.updateUrl("https://ctext.org/analects/xue-er")
        viewModel.loadFromUrl()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ctp:analects/xue-er", state.currentUrn)
        assertEquals("學而", state.title)
        assertEquals(1, state.history.size)
    }

    private class FakeCtextRepository(
        private val readLinkResult: ApiResult<ReadLinkResponseDto> = ApiResult.TransportError("unused"),
        private val getTextResult: ApiResult<GetTextResponseDto> = ApiResult.TransportError("unused"),
        private val getLinkResult: ApiResult<GetLinkResponseDto> = ApiResult.TransportError("unused")
    ) : CtextRepository {
        private var history = emptyList<ReaderHistoryEntry>()

        override suspend fun getStatus(apiKey: String?): ApiResult<StatusResponseDto> = ApiResult.Success(StatusResponseDto())

        override suspend fun readLink(url: String, apiKey: String?): ApiResult<ReadLinkResponseDto> = readLinkResult

        override suspend fun getLink(urn: String, apiKey: String?): ApiResult<GetLinkResponseDto> = getLinkResult

        override suspend fun getText(urn: String, apiKey: String?): ApiResult<GetTextResponseDto> = getTextResult

        override suspend fun getHistory(): List<ReaderHistoryEntry> = history

        override suspend fun saveHistoryEntry(entry: ReaderHistoryEntry): List<ReaderHistoryEntry> {
            history = listOf(entry)
            return history
        }

        override suspend fun clearHistory() {
            history = emptyList()
        }
    }
}