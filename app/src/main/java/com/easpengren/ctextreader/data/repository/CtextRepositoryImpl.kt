package com.easpengren.ctextreader.data.repository

import com.easpengren.ctextreader.data.api.CtextApiService
import com.easpengren.ctextreader.data.api.GetLinkResponseDto
import com.easpengren.ctextreader.data.api.GetTextResponseDto
import com.easpengren.ctextreader.data.api.ReadLinkResponseDto
import com.easpengren.ctextreader.data.api.StatusResponseDto
import com.easpengren.ctextreader.data.local.ReaderHistoryStore
import com.easpengren.ctextreader.domain.model.ApiResult
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
import com.easpengren.ctextreader.domain.repository.CtextRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CtextRepositoryImpl @Inject constructor(
    private val api: CtextApiService,
    private val historyStore: ReaderHistoryStore
) : CtextRepository {

    override suspend fun getStatus(apiKey: String?): ApiResult<StatusResponseDto> {
        return safeCall { api.getStatus(apiKey = apiKey) }
    }

    override suspend fun readLink(url: String, apiKey: String?): ApiResult<ReadLinkResponseDto> {
        return safeCall { api.readLink(url = url, apiKey = apiKey) }
    }

    override suspend fun getLink(urn: String, apiKey: String?): ApiResult<GetLinkResponseDto> {
        return safeCall { api.getLink(urn = urn, apiKey = apiKey) }
    }

    override suspend fun getText(urn: String, apiKey: String?): ApiResult<GetTextResponseDto> {
        return safeCall { api.getText(urn = urn, apiKey = apiKey) }
    }

    override suspend fun getHistory(): List<ReaderHistoryEntry> {
        return historyStore.loadHistory()
    }

    override suspend fun saveHistoryEntry(entry: ReaderHistoryEntry): List<ReaderHistoryEntry> {
        return historyStore.save(entry)
    }

    override suspend fun clearHistory() {
        historyStore.clear()
    }

    private suspend fun <T : Any> safeCall(block: suspend () -> T): ApiResult<T> {
        return try {
            val response = block()
            val error = extractError(response)
            if (error != null) {
                ApiResult.ApiError(code = error.first, descriptionHtml = error.second)
            } else {
                ApiResult.Success(response)
            }
        } catch (e: Exception) {
            ApiResult.TransportError(e.message ?: "Unexpected network error")
        }
    }

    private fun extractError(response: Any): Pair<String, String>? {
        return when (response) {
            is StatusResponseDto -> response.error?.let { it.code to it.description }
            is ReadLinkResponseDto -> response.error?.let { it.code to it.description }
            is GetLinkResponseDto -> response.error?.let { it.code to it.description }
            is GetTextResponseDto -> response.error?.let { it.code to it.description }
            else -> null
        }
    }
}
