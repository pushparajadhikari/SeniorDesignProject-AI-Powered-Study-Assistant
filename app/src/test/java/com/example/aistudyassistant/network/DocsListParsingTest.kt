package com.example.aistudyassistant.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the GET /docs-list contract to literal responses captured from the reset live
 * server on 2026-07-18 — not a hand-typed approximation. This is the second time this
 * exact field ("documents") has changed shape (bare strings, then {filename,timestamp}
 * objects); a hand-typed fixture would have encoded the same wrong assumption as the
 * code being tested and never caught either break.
 */
class DocsListParsingTest {

    @Test
    fun `parses a fresh account's empty response`() {
        // curl "https://studyai.binodtiwari.com/docs-list?user_id=45" (freshly registered account)
        val raw = """{"documents":[]}"""

        val result = ApiService.parseDocsList(raw)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parses a populated response with filename and timestamp objects`() {
        // curl "https://studyai.binodtiwari.com/docs-list?user_id=1"
        val raw = """{"documents":[{"filename":"solar_system_overview.pdf","timestamp":"2026-07-18T08:45:57.506058+00:00"}]}"""

        val result = ApiService.parseDocsList(raw)

        assertEquals(1, result.size)
        assertEquals("solar_system_overview.pdf", result[0].filename)
        assertEquals("2026-07-18T08:45:57.506058+00:00", result[0].timestamp)
    }
}
