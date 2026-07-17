package com.example.aistudyassistant.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the GET /docs-list contract to what the live server actually returns
 * (captured 2026-07-17: `curl https://studyai.binodtiwari.com/docs-list?user_id=1`).
 *
 * The client used to model this as a bare JSON array of objects
 * ([{"filename":...,"timestamp":...}]); the server sends an OBJECT wrapping an
 * array of plain filename strings. That mismatch crashed the PDF picker with
 * "Expected BEGIN_ARRAY but was BEGIN_OBJECT". This test fails at build time if
 * that contract ever drifts again instead of failing at the demo.
 */
class DocsListParsingTest {

    @Test
    fun `parses the actual live response shape`() {
        val raw = """{"documents":["pt_solar_v2.pdf"]}"""

        val result = ApiService.parseDocsList(raw)

        assertEquals(1, result.size)
        assertEquals("pt_solar_v2.pdf", result[0].filename)
    }

    @Test
    fun `multiple documents all come through`() {
        val raw = """{"documents":["lecture1.pdf","lecture2.pdf","notes.pdf"]}"""

        val result = ApiService.parseDocsList(raw)

        assertEquals(listOf("lecture1.pdf", "lecture2.pdf", "notes.pdf"), result.map { it.filename })
    }

    @Test
    fun `empty documents array renders as an empty list, not a crash`() {
        val raw = """{"documents":[]}"""

        val result = ApiService.parseDocsList(raw)

        assertTrue(result.isEmpty())
    }
}
