package com.example.aistudyassistant.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the GET /quizzes/{user_id} contract to a literal response captured from the reset
 * live server on 2026-07-18: `curl https://studyai.binodtiwari.com/quizzes/1`. The array
 * is wrapped in {"quizzes": [...]}, not bare; ids are strings; and `correct` is `null`
 * for a quiz that was generated but never submitted via POST /quiz-result — a real,
 * common state that must not crash the parse.
 */
class QuizHistoryParsingTest {

    private val raw = """{"quizzes":[{"id":"q_20260718_084730","source_pdf":null,"created_at":"2026-07-18T08:47:30.671904+00:00","total_questions":3,"correct":null},{"id":"q_20260718_084715","source_pdf":"solar_system_overview.pdf","created_at":"2026-07-18T08:47:15.827079+00:00","total_questions":3,"correct":null}]}"""

    @Test
    fun `unwraps the quizzes envelope`() {
        val result = ApiService.parseQuizHistoryJson(raw)

        assertEquals(2, result.size)
    }

    @Test
    fun `a null correct means not yet submitted, not a crash`() {
        val result = ApiService.parseQuizHistoryJson(raw)

        assertNull(result[0].correct)
        assertEquals(3, result[0].totalQuestions)
    }

    @Test
    fun `a null source_pdf means All Documents, not a crash`() {
        val result = ApiService.parseQuizHistoryJson(raw)

        assertNull(result[0].sourcePdf)
        assertEquals("solar_system_overview.pdf", result[1].sourcePdf)
    }

    @Test
    fun `empty quizzes renders as an empty list, not a crash`() {
        val result = ApiService.parseQuizHistoryJson("""{"quizzes":[]}""")

        assertTrue(result.isEmpty())
    }
}
