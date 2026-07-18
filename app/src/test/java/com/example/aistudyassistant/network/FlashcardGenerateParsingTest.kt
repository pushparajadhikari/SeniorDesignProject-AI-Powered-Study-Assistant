package com.example.aistudyassistant.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the POST /flashcards success response to literal responses captured from the
 * reset live server on 2026-07-18. Confirms the "All documents" request (source_pdf
 * omitted) parses the same as a specific-PDF request — the id is a server-generated
 * string, not "set_id"/Int as an earlier round assumed.
 */
class FlashcardGenerateParsingTest {

    @Test
    fun `parses a set generated from a specific pdf`() {
        // curl -X POST .../flashcards -d '{"user_id":1,"source_pdf":"solar_system_overview.pdf","count":5}'
        val raw = """{"id":"f_20260718_090128","source_pdf":"solar_system_overview.pdf","created_at":"2026-07-18T09:01:28.691537+00:00","cards":[{"question":"What is the approximate temperature of the Sun's core?","options":["15 million degrees Celsius","5,500 degrees Celsius","1.989 x 10^30 kg","4.6 billion years"],"correct_index":0,"explanation":"The Sun's core temperature reaches approximately 15 million degrees Celsius, where nuclear fusion occurs."}]}"""

        val result = ApiService.parseFlashcardGenerateResponse(raw)

        assertEquals("f_20260718_090128", result.id)
        assertEquals("solar_system_overview.pdf", result.sourcePdf)
        assertEquals(1, result.cards.size)
        assertEquals(0, result.cards[0].correctIndex)
    }

    @Test
    fun `parses a set generated from All Documents (source_pdf omitted in the request)`() {
        // curl -X POST .../flashcards -d '{"user_id":1,"count":5}'  (no source_pdf key sent)
        val raw = """{"id":"f_20260718_085937","source_pdf":null,"created_at":"2026-07-18T08:59:37.028088+00:00","cards":[{"question":"What is the orbital period of Saturn?","options":["11.9 Earth years","29.5 Earth years","84 Earth years","165 Earth years"],"correct_index":1,"explanation":"The study material states Saturn's orbital period is 29.5 Earth years."}]}"""

        val result = ApiService.parseFlashcardGenerateResponse(raw)

        assertEquals("f_20260718_085937", result.id)
        assertNull(result.sourcePdf)
        assertEquals(1, result.cards.size)
    }
}
