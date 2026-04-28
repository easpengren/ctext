package com.easpengren.ctextreader.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface CtextApiService {
    @GET("getstatus")
    suspend fun getStatus(
        @Query("if") lang: String = "en",
        @Query("remap") remap: String? = null,
        @Query("apikey") apiKey: String? = null
    ): StatusResponseDto

    @GET("readlink")
    suspend fun readLink(
        @Query("url") url: String,
        @Query("if") lang: String = "en",
        @Query("remap") remap: String? = null,
        @Query("apikey") apiKey: String? = null
    ): ReadLinkResponseDto

    @GET("getlink")
    suspend fun getLink(
        @Query("urn") urn: String,
        @Query("redirect") redirect: Int = 0,
        @Query("if") lang: String = "en",
        @Query("remap") remap: String? = null,
        @Query("apikey") apiKey: String? = null
    ): GetLinkResponseDto

    @GET("gettext")
    suspend fun getText(
        @Query("urn") urn: String,
        @Query("if") lang: String = "en",
        @Query("remap") remap: String? = null,
        @Query("apikey") apiKey: String? = null
    ): GetTextResponseDto

    @GET("searchtexts")
    suspend fun searchTexts(
        @Query("title") title: String,
        @Query("if") lang: String = "en",
        @Query("remap") remap: String? = null,
        @Query("apikey") apiKey: String? = null
    ): SearchTextsResponseDto
}
