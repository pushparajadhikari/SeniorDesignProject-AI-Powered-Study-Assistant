package com.example.aistudyassistant.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the GET /flashcards/{user_id} contract to a literal response captured from the
 * reset live server on 2026-07-18: `curl https://studyai.binodtiwari.com/flashcards/1`.
 * The array is wrapped in {"flashcard_sets": [...]}, not bare — and each set's id is a
 * server-generated string ("f_20260718_090128"), not an int.
 */
class FlashcardHistoryParsingTest {

    private val raw = """{"flashcard_sets":[{"id":"f_20260718_090128","source_pdf":"solar_system_overview.pdf","created_at":"2026-07-18T09:01:28.691537+00:00","card_count":5,"cards_revealed":0},{"id":"f_20260718_085944","source_pdf":null,"created_at":"2026-07-18T08:59:44.904114+00:00","card_count":5,"cards_revealed":0}]}"""

    @Test
    fun `unwraps the flashcard_sets envelope`() {
        val result = ApiService.parseFlashcardHistoryJson(raw)

        assertEquals(2, result.size)
    }

    @Test
    fun `parses a set generated from a specific pdf`() {
        val result = ApiService.parseFlashcardHistoryJson(raw)

        val specific = result[0]
        assertEquals("f_20260718_090128", specific.id)
        assertEquals("solar_system_overview.pdf", specific.sourcePdf)
        assertEquals(5, specific.cardCount)
        assertEquals(0, specific.cardsRevealed)
    }

    @Test
    fun `a null source_pdf means All Documents, not a crash`() {
        val result = ApiService.parseFlashcardHistoryJson(raw)

        assertNull(result[1].sourcePdf)
    }

    @Test
    fun `empty flashcard_sets renders as an empty list, not a crash`() {
        val result = ApiService.parseFlashcardHistoryJson("""{"flashcard_sets":[]}""")

        assertTrue(result.isEmpty())
    }
}
