package com.easpengren.ctextreader.data.local

import android.content.Context
import com.easpengren.ctextreader.domain.model.ReaderPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class ReaderPreferencesStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ReaderPreferences {
        val rawValue = preferences.getString(KEY_SETTINGS, null) ?: return ReaderPreferences()
        return runCatching {
            json.decodeFromString<ReaderPreferences>(rawValue)
        }.getOrDefault(ReaderPreferences())
    }

    fun save(value: ReaderPreferences) {
        preferences.edit().putString(KEY_SETTINGS, json.encodeToString(ReaderPreferences.serializer(), value)).apply()
    }

    private companion object {
        const val PREFS_NAME = "ctext_reader_preferences"
        const val KEY_SETTINGS = "reader_preferences"
    }
}