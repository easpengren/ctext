package com.easpengren.ctextreader.data.local

import android.content.Context
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Singleton
class ReaderHistoryStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadHistory(): List<ReaderHistoryEntry> {
        val rawValue = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ReaderHistoryEntry.serializer()), rawValue)
        }.getOrDefault(emptyList())
    }

    fun save(entry: ReaderHistoryEntry): List<ReaderHistoryEntry> {
        val updated = (listOf(entry) + loadHistory().filterNot { it.urn == entry.urn }).take(MAX_ENTRIES)
        val encoded = json.encodeToString(ListSerializer(ReaderHistoryEntry.serializer()), updated)
        preferences.edit().putString(KEY_HISTORY, encoded).apply()
        return updated
    }

    fun clear() {
        preferences.edit().remove(KEY_HISTORY).apply()
    }

    private companion object {
        const val PREFS_NAME = "ctext_reader_history"
        const val KEY_HISTORY = "reader_history"
        const val MAX_ENTRIES = 25
    }
}