package com.easpengren.ctextreader.data.repository

import com.easpengren.ctextreader.data.api.CtextApiService
import com.easpengren.ctextreader.data.api.GetLinkResponseDto
import com.easpengren.ctextreader.data.api.GetTextResponseDto
import com.easpengren.ctextreader.data.api.ReadLinkResponseDto
import com.easpengren.ctextreader.data.api.SearchTextsResponseDto
import com.easpengren.ctextreader.data.api.StatusResponseDto
import com.easpengren.ctextreader.data.local.ReaderHistoryStore
import com.easpengren.ctextreader.data.local.ReaderPreferencesStore
import com.easpengren.ctextreader.domain.model.ApiResult
import com.easpengren.ctextreader.domain.model.InterfaceLanguage
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
import com.easpengren.ctextreader.domain.model.ReaderPreferences
import com.easpengren.ctextreader.domain.repository.CtextRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CtextRepositoryImpl @Inject constructor(
    private val api: CtextApiService,
    private val historyStore: ReaderHistoryStore,
    private val preferencesStore: ReaderPreferencesStore
) : CtextRepository {

    override suspend fun getStatus(
        language: InterfaceLanguage,
        simplified: Boolean,
        apiKey: String?
    ): ApiResult<StatusResponseDto> {
        return safeCall {
            api.getStatus(
                lang = language.apiValue,
                remap = simplified.toRemapValue(),
                apiKey = apiKey
            )
        }
    }

    override suspend fun readLink(
        url: String,
        language: InterfaceLanguage,
        simplified: Boolean,
        apiKey: String?
    ): ApiResult<ReadLinkResponseDto> {
        return safeCall {
            api.readLink(
                url = url,
                lang = language.apiValue,
                remap = simplified.toRemapValue(),
                apiKey = apiKey
            )
        }
    }

    override suspend fun getLink(
        urn: String,
        language: InterfaceLanguage,
        simplified: Boolean,
        apiKey: String?
    ): ApiResult<GetLinkResponseDto> {
        return safeCall {
            api.getLink(
                urn = urn,
                lang = language.apiValue,
                remap = simplified.toRemapValue(),
                apiKey = apiKey
            )
        }
    }

    override suspend fun getText(
        urn: String,
        language: InterfaceLanguage,
        simplified: Boolean,
        apiKey: String?
    ): ApiResult<GetTextResponseDto> {
        return safeCall {
            api.getText(
                urn = urn,
                lang = language.apiValue,
                remap = simplified.toRemapValue(),
                apiKey = apiKey
            )
        }
    }

    override suspend fun searchTexts(
        query: String,
        language: InterfaceLanguage,
        simplified: Boolean,
        apiKey: String?
    ): ApiResult<SearchTextsResponseDto> {
        return safeCall {
            api.searchTexts(
                title = query,
                lang = language.apiValue,
                remap = simplified.toRemapValue(),
                apiKey = apiKey
            )
        }
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

    override suspend fun getPreferences(): ReaderPreferences {
        return preferencesStore.load()
    }

    override suspend fun savePreferences(preferences: ReaderPreferences): ReaderPreferences {
        preferencesStore.save(preferences)
        return preferencesStore.load()
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
            is SearchTextsResponseDto -> response.error?.let { it.code to it.description }
            else -> null
        }
    }

    private fun Boolean.toRemapValue(): String? = if (this) "gb" else null
}
