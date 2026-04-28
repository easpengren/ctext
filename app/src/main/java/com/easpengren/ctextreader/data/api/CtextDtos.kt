package com.easpengren.ctextreader.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CtextErrorDto(
    val code: String,
    val description: String
)

@Serializable
data class GetTextResponseDto(
    val title: String? = null,
    val fulltext: List<String>? = null,
    val subsections: List<String>? = null,
    val error: CtextErrorDto? = null
)

@Serializable
data class ReadLinkResponseDto(
    val urn: String? = null,
    val error: CtextErrorDto? = null
)

@Serializable
data class GetLinkResponseDto(
    val link: String? = null,
    val error: CtextErrorDto? = null
)

@Serializable
data class StatusResponseDto(
    @SerialName("status") val status: String? = null,
    val authenticated: Boolean? = null,
    val error: CtextErrorDto? = null
)
