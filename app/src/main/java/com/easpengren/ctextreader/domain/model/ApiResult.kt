package com.easpengren.ctextreader.domain.model

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    data class ApiError(
        val code: String,
        val descriptionHtml: String
    ) : ApiResult<Nothing>

    data class TransportError(val message: String) : ApiResult<Nothing>
}
