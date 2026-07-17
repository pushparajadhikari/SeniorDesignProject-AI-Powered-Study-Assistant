package com.example.aistudyassistant.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the GET /quizzes/{user_id} contract to the shape documented for it
 * (the endpoint 404s on the live server as of 2026-07-17 — not deployed yet — so
 * this pins the documented shape, not a captured one; update this JSON the first
 * time a real response is observed).
 */
class QuizHistoryParsingTest {

    @Test
    fun `parses the documented history-index shape`() {
        val raw = """
            [
                {"id":1,"source_pdf":"lecture1.pdf","created_at":"2026-07-06T10:00:00+00:00","total_questions":12,"correct":9}
            ]
        """.trimIndent()

        val result = ApiService.parseQuizHistoryJson(raw)

        assertEquals(1, result.size)
        val entry = result[0]
        assertEquals(1, entry.id)
        assertEquals("lecture1.pdf", entry.sourcePdf)
        assertEquals(12, entry.totalQuestions)
        assertEquals(9, entry.correct)
    }

    @Test
    fun `a null source_pdf means All Documents, not a crash`() {
        val raw = """[{"id":2,"source_pdf":null,"created_at":"2026-07-06T10:00:00+00:00","total_questions":5,"correct":4}]"""

        val result = ApiService.parseQuizHistoryJson(raw)

        assertEquals(1, result.size)
        assertEquals(null, result[0].sourcePdf)
    }

    @Test
    fun `empty array renders as an empty list, not a crash`() {
        val result = ApiService.parseQuizHistoryJson("[]")

        assertTrue(result.isEmpty())
    }
}
