package com.easpengren.ctextreader.data.api

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.Json
import org.junit.Test

class GetTextResponseDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses error payload`() {
        val payload = """
            {
              "error": {
                "code": "ERR_INVALID_URN",
                "description": "Invalid URN."
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<GetTextResponseDto>(payload)

        assertNotNull(parsed.error)
        assertEquals("ERR_INVALID_URN", parsed.error.code)
    }
}
