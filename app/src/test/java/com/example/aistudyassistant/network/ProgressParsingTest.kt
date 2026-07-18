package com.example.aistudyassistant.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the GET /progress/{user_id} contract to a literal response captured from the
 * live backend on 2026-07-18: `curl https://studyai.binodtiwari.com/progress/1`.
 * flashcard_sets was previously modeled as an Int; it is actually an array of the same
 * shape flashcard history uses — that mismatch crashed every Progress tab load with
 * "Could not load progress / Couldn't read the server's response."
 */
class ProgressParsingTest {

    @Test
    fun `parses a populated progress response, including the flashcard_sets array`() {
        val raw = """{"user_id":1,"pdfs_uploaded":[{"filename":"solar_system_overview.pdf","timestamp":"2026-07-18T08:45:57.506058+00:00"}],"quiz_history":[{"id":"q_20260718_084652","source_pdf":"solar_system_overview.pdf","created_at":"2026-07-18T08:46:52.087642+00:00","total_questions":3,"correct":null}],"flashcard_sets":[{"id":"f_20260718_084830","source_pdf":"solar_system_overview.pdf","created_at":"2026-07-18T08:48:30.571288+00:00","card_count":5,"cards_revealed":0}],"questions_answered_total":0,"questions_correct_total":0,"flashcards_revealed_total":0}"""

        val result = ApiService.parseProgress(raw)

        assertEquals(1, result.pdfsUploaded.size)
        assertEquals("solar_system_overview.pdf", result.pdfsUploaded[0].filename)

        assertEquals(1, result.quizHistory.size)
        assertEquals("q_20260718_084652", result.quizHistory[0].id)
        assertNull(result.quizHistory[0].correct)

        assertEquals(1, result.flashcardSets.size)
        assertEquals("f_20260718_084830", result.flashcardSets[0].id)
        assertEquals(5, result.flashcardSets[0].cardCount)
    }

    @Test
    fun `parses a fresh account's empty progress response`() {
        // curl "https://studyai.binodtiwari.com/progress/45" (freshly registered account)
        val raw = """{"user_id":45,"pdfs_uploaded":[],"quiz_history":[],"flashcard_sets":[],"questions_answered_total":0,"questions_correct_total":0,"flashcards_revealed_total":0}"""

        val result = ApiService.parseProgress(raw)

        assertEquals(0, result.pdfsUploaded.size)
        assertEquals(0, result.quizHistory.size)
        assertEquals(0, result.flashcardSets.size)
    }
}
