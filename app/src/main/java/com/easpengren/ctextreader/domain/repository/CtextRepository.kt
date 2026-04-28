package com.easpengren.ctextreader.domain.repository

import com.easpengren.ctextreader.data.api.GetLinkResponseDto
import com.easpengren.ctextreader.data.api.GetTextResponseDto
import com.easpengren.ctextreader.data.api.ReadLinkResponseDto
import com.easpengren.ctextreader.data.api.SearchTextsResponseDto
import com.easpengren.ctextreader.data.api.StatusResponseDto
import com.easpengren.ctextreader.domain.model.ApiResult
import com.easpengren.ctextreader.domain.model.InterfaceLanguage
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
import com.easpengren.ctextreader.domain.model.ReaderPreferences

interface CtextRepository {
    suspend fun getStatus(
        language: InterfaceLanguage = InterfaceLanguage.ENGLISH,
        simplified: Boolean = false,
        apiKey: String? = null
    ): ApiResult<StatusResponseDto>
    suspend fun readLink(
        url: String,
        language: InterfaceLanguage = InterfaceLanguage.ENGLISH,
        simplified: Boolean = false,
        apiKey: String? = null
    ): ApiResult<ReadLinkResponseDto>
    suspend fun getLink(
        urn: String,
        language: InterfaceLanguage = InterfaceLanguage.ENGLISH,
        simplified: Boolean = false,
        apiKey: String? = null
    ): ApiResult<GetLinkResponseDto>
    suspend fun getText(
        urn: String,
        language: InterfaceLanguage = InterfaceLanguage.ENGLISH,
        simplified: Boolean = false,
        apiKey: String? = null
    ): ApiResult<GetTextResponseDto>
    suspend fun searchTexts(
        query: String,
        language: InterfaceLanguage = InterfaceLanguage.ENGLISH,
        simplified: Boolean = false,
        apiKey: String? = null
    ): ApiResult<SearchTextsResponseDto>
    suspend fun getHistory(): List<ReaderHistoryEntry>
    suspend fun saveHistoryEntry(entry: ReaderHistoryEntry): List<ReaderHistoryEntry>
    suspend fun clearHistory()
    suspend fun getPreferences(): ReaderPreferences
    suspend fun savePreferences(preferences: ReaderPreferences): ReaderPreferences
}
