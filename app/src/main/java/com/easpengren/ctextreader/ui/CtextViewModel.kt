package com.easpengren.ctextreader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easpengren.ctextreader.data.api.GetTextResponseDto
import com.easpengren.ctextreader.domain.model.ApiResult
import com.easpengren.ctextreader.domain.model.ReaderHistoryEntry
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
    val statusText: String = "Idle",
    val currentUrn: String = "",
    val directLink: String? = null,
    val title: String = "",
    val paragraphs: List<String> = emptyList(),
    val subsections: List<String> = emptyList(),
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
            _uiState.update { it.copy(history = repository.getHistory()) }
        }
    }

    fun updateUrn(value: String) {
        _uiState.update { it.copy(urnInput = value) }
    }

    fun updateUrl(value: String) {
        _uiState.update { it.copy(urlInput = value) }
    }

    fun checkStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, statusText = "Checking status...") }
            when (val result = repository.getStatus()) {
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
            when (val result = repository.readLink(url = url)) {
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

            val textDeferred = async { repository.getText(urn = urn) }
            val linkDeferred = async { repository.getLink(urn = urn) }

            when (val textResult = textDeferred.await()) {
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
            it.copy(
                loading = false,
                currentUrn = urn,
                directLink = directLink,
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
}
