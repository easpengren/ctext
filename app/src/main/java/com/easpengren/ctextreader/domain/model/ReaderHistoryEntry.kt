package com.easpengren.ctextreader.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReaderHistoryEntry(
    val urn: String,
    val title: String,
    val link: String? = null,
    val sourceUrl: String? = null,
    val savedAtEpochMillis: Long
)