package com.easpengren.ctextreader.domain.repository

import com.easpengren.ctextreader.data.api.GetLinkResponseDto
import com.easpengren.ctextreader.data.api.GetTextResponseDto
import com.easpengren.ctextreader.data.api.ReadLinkResponseDto
import com.easpengren.ctextreader.data.api.StatusResponseDto
import com.easpengren.ctextreader.domain.model.ApiResult

interface CtextRepository {
    suspend fun getStatus(apiKey: String? = null): ApiResult<StatusResponseDto>
    suspend fun readLink(url: String, apiKey: String? = null): ApiResult<ReadLinkResponseDto>
    suspend fun getLink(urn: String, apiKey: String? = null): ApiResult<GetLinkResponseDto>
    suspend fun getText(urn: String, apiKey: String? = null): ApiResult<GetTextResponseDto>
}
