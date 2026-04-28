package com.easpengren.ctextreader.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReaderPreferences(
    val interfaceLanguage: InterfaceLanguage = InterfaceLanguage.ENGLISH,
    val useSimplifiedCharacters: Boolean = false
)

@Serializable
enum class InterfaceLanguage(val apiValue: String) {
    ENGLISH("en"),
    CHINESE("zh")
}