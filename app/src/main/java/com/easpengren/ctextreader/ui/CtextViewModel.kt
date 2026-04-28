package com.easpengren.ctextreader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easpengren.ctextreader.data.api.GetTextResponseDto
import com.easpengren.ctextreader.domain.model.ApiResult
import com.easpengren.ctextreader.domain.repository.CtextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CtextUiState(
    val urnInput: String = "ctp:analects/xue-er",
    val statusText: String = "Idle",
    val title: String = "",
    val paragraphs: List<String> = emptyList(),
    val error: String? = null,
    val loading: Boolean = false
)

@HiltViewModel
class CtextViewModel @Inject constructor(
    private val repository: CtextRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CtextUiState())
    val uiState: StateFlow<CtextUiState> = _uiState.asStateFlow()

    fun updateUrn(value: String) {
        _uiState.update { it.copy(urnInput = value) }
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

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = repository.getText(urn = urn)) {
                is ApiResult.Success -> handleTextSuccess(result.data)
                is ApiResult.ApiError -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "${result.code}: ${result.descriptionHtml}",
                            title = "",
                            paragraphs = emptyList()
                        )
                    }
                }
                is ApiResult.TransportError -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = result.message,
                            title = "",
                            paragraphs = emptyList()
                        )
                    }
                }
            }
        }
    }

    private fun handleTextSuccess(data: GetTextResponseDto) {
        val fulltext = data.fulltext.orEmpty()
        _uiState.update {
            it.copy(
                loading = false,
                title = data.title.orEmpty(),
                paragraphs = fulltext,
                error = if (fulltext.isEmpty()) "No fulltext returned for this URN." else null
            )
        }
    }
}
