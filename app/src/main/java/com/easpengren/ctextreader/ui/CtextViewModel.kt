package com.easpengren.ctextreader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easpengren.ctextreader.data.api.GetTextResponseDto
import com.easpengren.ctextreader.data.api.SearchTextBookDto
import com.easpengren.ctextreader.domain.model.ApiResult
import com.easpengren.ctextreader.domain.model.InterfaceLanguage
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
import com.easpengren.ctextreader.domain.model.ReaderPreferences
import com.easpengren.ctextreader.domain.repository.CtextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CtextUiState(
    val urnInput: String = "ctp:analects/xue-er",
    val urlInput: String = "https://ctext.org/analects/xue-er",
    val searchQuery: String = "Analects",
    val statusText: String = "Idle",
    val currentUrn: String = "",
    val directLink: String? = null,
    val englishPageLink: String? = null,
    val englishReaderNotice: String? = null,
    val title: String = "",
    val paragraphs: List<String> = emptyList(),
    val subsections: List<String> = emptyList(),
    val searchResults: List<SearchTextBookDto> = emptyList(),
    val interfaceLanguage: InterfaceLanguage = InterfaceLanguage.ENGLISH,
    val useSimplifiedCharacters: Boolean = false,
    val history: List<ReaderHistoryEntry> = emptyList(),
    val canGoBack: Boolean = false,
    val error: String? = null,
    val loading: Boolean = false
)

@HiltViewModel
class CtextViewModel @Inject constructor(
    private val repository: CtextRepository
) : ViewModel() {

    private val navigationBackstack = ArrayDeque<String>()

    private val _uiState = MutableStateFlow(CtextUiState())
    val uiState: StateFlow<CtextUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val history = repository.getHistory()
            val preferences = repository.getPreferences()
            _uiState.update {
                it.copy(
                    history = history,
                    interfaceLanguage = preferences.interfaceLanguage,
                    useSimplifiedCharacters = preferences.useSimplifiedCharacters
                )
            }
        }
    }

    fun updateUrn(value: String) {
        _uiState.update { it.copy(urnInput = value) }
    }

    fun updateUrl(value: String) {
        _uiState.update { it.copy(urlInput = value) }
    }

    fun updateSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun setInterfaceLanguage(language: InterfaceLanguage) {
        persistPreferences(
            _uiState.value.copy(
                interfaceLanguage = language,
                error = null,
                statusText = "API language set to ${language.name.lowercase()}"
            )
        )
    }

    fun setSimplifiedCharacters(enabled: Boolean) {
        persistPreferences(
            _uiState.value.copy(
                useSimplifiedCharacters = enabled,
                error = null,
                statusText = if (enabled) "Simplified remap enabled" else "Traditional characters enabled"
            )
        )
    }

    fun checkStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, statusText = "Checking status...") }
            when (
                val result = repository.getStatus(
                    language = _uiState.value.interfaceLanguage,
                    simplified = _uiState.value.useSimplifiedCharacters
                )
            ) {
                is ApiResult.Success -> {
                    val status = result.data.status ?: if (result.data.authenticated == true) "authenticated" else "unknown"
                    _uiState.update { it.copy(loading = false, statusText = "Status: $status") }
                }
                is ApiResult.ApiError -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "${result.code}: ${result.descriptionHtml}",
                            statusText = "API error"
                        )
                    }
                }
                is ApiResult.TransportError -> {
                    _uiState.update { it.copy(loading = false, error = result.message, statusText = "Network error") }
                }
            }
        }
    }

    fun loadText() {
        val urn = _uiState.value.urnInput.trim()
        if (urn.isEmpty()) {
            _uiState.update { it.copy(error = "Enter a valid CTP URN") }
            return
        }

        loadTextInternal(urn = urn, pushCurrent = true, persistOnSuccess = true)
    }

    fun loadFromUrl() {
        val url = _uiState.value.urlInput.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(error = "Enter a valid ctext.org URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, statusText = "Resolving URL...") }
            when (
                val result = repository.readLink(
                    url = url,
                    language = _uiState.value.interfaceLanguage,
                    simplified = _uiState.value.useSimplifiedCharacters
                )
            ) {
                is ApiResult.Success -> {
                    val urn = result.data.urn.orEmpty()
                    if (urn.isBlank()) {
                        _uiState.update {
                            it.copy(
                                loading = false,
                                error = "The API did not return a URN for this URL.",
                                statusText = "URL resolution failed"
                            )
                        }
                    } else {
                        _uiState.update { it.copy(urnInput = urn) }
                        loadTextInternal(
                            urn = urn,
                            sourceUrl = url,
                            pushCurrent = true,
                            persistOnSuccess = true
                        )
                    }
                }
                is ApiResult.ApiError -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "${result.code}: ${result.descriptionHtml}",
                            statusText = "URL resolution failed"
                        )
                    }
                }
                is ApiResult.TransportError -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = result.message,
                            statusText = "Network error"
                        )
                    }
                }
            }
        }
    }

    fun searchTexts() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) {
            _uiState.update { it.copy(error = "Enter a title or keyword to search") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    statusText = "Searching titles...",
                    searchResults = emptyList()
                )
            }
            val candidates = buildSearchCandidates(query)
            val language = _uiState.value.interfaceLanguage
            val simplified = _uiState.value.useSimplifiedCharacters
            val mergedResults = linkedMapOf<String, SearchTextBookDto>()
            var lastError: ApiResult<*>? = null

            for (candidate in candidates) {
                when (
                    val result = repository.searchTexts(
                        query = candidate,
                        language = language,
                        simplified = simplified
                    )
                ) {
                    is ApiResult.Success -> {
                        result.data.books
                            .asSequence()
                            .filter { it.urn.isNotBlank() }
                            .forEach { book ->
                                mergedResults.putIfAbsent("${book.urn}|${book.title}", book)
                            }
                    }
                    is ApiResult.ApiError,
                    is ApiResult.TransportError -> {
                        lastError = result
                    }
                }

                if (mergedResults.size >= 50) break
            }

            if (mergedResults.isNotEmpty()) {
                val finalResults = mergedResults.values.take(50)
                _uiState.update {
                    it.copy(
                        loading = false,
                        searchResults = finalResults,
                        statusText = "Found ${finalResults.size} matching titles",
                        error = null
                    )
                }
            } else {
                when (val error = lastError) {
                    is ApiResult.ApiError -> {
                        _uiState.update {
                            it.copy(
                                loading = false,
                                error = "${error.code}: ${error.descriptionHtml}",
                                statusText = "Search failed"
                            )
                        }
                    }
                    is ApiResult.TransportError -> {
                        _uiState.update {
                            it.copy(
                                loading = false,
                                error = error.message,
                                statusText = "Network error"
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(
                                loading = false,
                                error = "No searchable titles matched your query.",
                                statusText = "No results"
                            )
                        }
                    }
                }
            }
        }
    }

    fun openSearchResult(result: SearchTextBookDto) {
        _uiState.update { it.copy(urnInput = result.urn) }
        loadTextInternal(urn = result.urn, pushCurrent = true, persistOnSuccess = true)
    }

    fun openSubsection(urn: String) {
        loadTextInternal(urn = urn, pushCurrent = true, persistOnSuccess = true)
    }

    fun openHistoryEntry(entry: ReaderHistoryEntry) {
        _uiState.update { it.copy(urlInput = entry.sourceUrl ?: it.urlInput) }
        loadTextInternal(urn = entry.urn, pushCurrent = true, persistOnSuccess = true)
    }

    fun navigateBack() {
        val previousUrn = navigationBackstack.removeLastOrNull() ?: return
        loadTextInternal(urn = previousUrn, pushCurrent = false, persistOnSuccess = false)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(history = emptyList()) }
        }
    }

    private fun loadTextInternal(
        urn: String,
        pushCurrent: Boolean,
        persistOnSuccess: Boolean,
        sourceUrl: String? = null
    ) {
        viewModelScope.launch {
            val currentUrn = _uiState.value.currentUrn
            if (pushCurrent && currentUrn.isNotBlank() && currentUrn != urn) {
                navigationBackstack.addLast(currentUrn)
            }

            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    statusText = "Loading text...",
                    canGoBack = navigationBackstack.isNotEmpty()
                )
            }

            val language = _uiState.value.interfaceLanguage
            val simplified = _uiState.value.useSimplifiedCharacters
            val linkDeferred = async {
                repository.getLink(
                    urn = urn,
                    language = language,
                    simplified = simplified
                )
            }
            val textResult = repository.getText(
                urn = urn,
                language = language,
                simplified = simplified
            )

            when (textResult) {
                is ApiResult.Success -> {
                    val directLink = when (val linkResult = linkDeferred.await()) {
                        is ApiResult.Success -> linkResult.data.link
                        else -> null
                    }
                    handleTextSuccess(
                        data = textResult.data,
                        urn = urn,
                        directLink = directLink,
                        sourceUrl = sourceUrl,
                        persistOnSuccess = persistOnSuccess
                    )
                }
                is ApiResult.ApiError -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "${textResult.code}: ${textResult.descriptionHtml}",
                            title = "",
                            paragraphs = emptyList(),
                            subsections = emptyList(),
                            statusText = "API error",
                            canGoBack = navigationBackstack.isNotEmpty()
                        )
                    }
                }
                is ApiResult.TransportError -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = textResult.message,
                            title = "",
                            paragraphs = emptyList(),
                            subsections = emptyList(),
                            statusText = "Network error",
                            canGoBack = navigationBackstack.isNotEmpty()
                        )
                    }
                }
            }
        }
    }

    private suspend fun handleTextSuccess(
        data: GetTextResponseDto,
        urn: String,
        directLink: String?,
        sourceUrl: String?,
        persistOnSuccess: Boolean
    ) {
        val fulltext = data.fulltext.orEmpty()
        val subsections = data.subsections.orEmpty()
        val title = data.title.orEmpty()
        val history = if (persistOnSuccess) {
            repository.saveHistoryEntry(
                ReaderHistoryEntry(
                    urn = urn,
                    title = title.ifBlank { urn },
                    link = directLink,
                    sourceUrl = sourceUrl,
                    savedAtEpochMillis = System.currentTimeMillis()
                )
            )
        } else {
            repository.getHistory()
        }

        _uiState.update {
            val englishPageLink = directLink?.toEnglishPageUrl()
            val notice = if (it.interfaceLanguage == InterfaceLanguage.ENGLISH) {
                "CTP API returns source text; use Open English Page for available translations."
            } else {
                null
            }
            it.copy(
                loading = false,
                currentUrn = urn,
                directLink = directLink,
                englishPageLink = englishPageLink,
                englishReaderNotice = notice,
                title = title,
                paragraphs = fulltext,
                subsections = subsections,
                history = history,
                error = when {
                    fulltext.isEmpty() && subsections.isEmpty() -> "No readable text or subsections returned for this URN."
                    else -> null
                },
                statusText = when {
                    subsections.isNotEmpty() && fulltext.isEmpty() -> "Loaded navigation data"
                    else -> "Loaded"
                },
                canGoBack = navigationBackstack.isNotEmpty()
            )
        }
    }

    private fun buildSearchCandidates(rawQuery: String): List<String> {
        val candidates = linkedSetOf<String>()
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()
        candidates += trimmed

        val normalized = trimmed
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val stopWords = setOf("find", "search", "for", "about", "the", "a", "an", "text", "book", "chapter", "please")
        val cleaned = normalized
            .split(" ")
            .filter { it.isNotBlank() && it !in stopWords }
            .joinToString(" ")
            .trim()

        if (cleaned.isNotEmpty()) {
            candidates += cleaned
            candidates += cleaned.split(" ")
                .joinToString(" ") { token -> token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        }

        val aliasMap = mapOf(
            "analects" to "Analects",
            "confucius analects" to "Analects",
            "mencius" to "Mencius",
            "mengzi" to "Mencius",
            "dao de jing" to "Dao De Jing",
            "tao te ching" to "Dao De Jing",
            "zhuangzi" to "Zhuangzi",
            "xunzi" to "Xunzi",
            "han feizi" to "Han Feizi",
            "i ching" to "Book of Changes",
            "book of changes" to "Book of Changes",
            "book of songs" to "Book of Songs",
            "shijing" to "Book of Songs"
        )

        val sourceForAliases = if (cleaned.isNotEmpty()) cleaned else normalized
        aliasMap.forEach { (needle, replacement) ->
            if (sourceForAliases.contains(needle)) {
                candidates += replacement
            }
        }

        return candidates.filter { it.isNotBlank() }
    }

    private fun String.toEnglishPageUrl(): String {
        val path = substringBefore('?').trimEnd('/')
        val englishPath = if (path.endsWith("/ens")) path else "$path/ens"
        return if (contains('?')) "$englishPath?${substringAfter('?')}" else englishPath
    }

    private fun persistPreferences(state: CtextUiState) {
        _uiState.update {
            it.copy(
                interfaceLanguage = state.interfaceLanguage,
                useSimplifiedCharacters = state.useSimplifiedCharacters,
                error = state.error,
                statusText = state.statusText
            )
        }

        viewModelScope.launch {
            repository.savePreferences(
                ReaderPreferences(
                    interfaceLanguage = state.interfaceLanguage,
                    useSimplifiedCharacters = state.useSimplifiedCharacters
                )
            )
        }
    }
}
