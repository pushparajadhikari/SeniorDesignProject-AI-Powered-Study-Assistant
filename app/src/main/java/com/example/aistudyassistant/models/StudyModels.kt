package com.example.aistudyassistant.models

import com.google.gson.annotations.SerializedName

/**
 * Shared data model for quiz questions.
 * Used by both ApiService (deserialization) and QuizScreen (display).
 * Flashcards reuse this same shape — a flashcard is just a QuizQuestion the
 * user reveals instead of answers, so no separate Flashcard type exists.
 */
data class QuizQuestion(
    val question:     String,
    val options:      List<String>,
    val correctIndex: Int,
    val explanation:  String
)

/** A document available to draw quiz/flashcard questions from. GET /docs-list. */
data class DocumentEntry(
    val filename:  String = "",
    val timestamp: String = ""
)

/** GET /documents/{user_id}/{filename}/usage — what deleting this document would also delete. */
data class DocumentUsage(
    @SerializedName("quiz_count")      val quizCount:      Int = 0,
    @SerializedName("flashcard_count") val flashcardCount: Int = 0
)

/**
 * POST /flashcards response — a freshly generated set.
 * Set ids are server-generated strings like "f_20260718_090128", not integers —
 * confirmed 2026-07-18 against the reset backend; an earlier assumption of Int cost
 * a debugging round, so don't reintroduce it.
 */
data class FlashcardSet(
    val id: String = "",
    @SerializedName("source_pdf") val sourcePdf: String? = null,
    @SerializedName("created_at") val createdAt: String  = "",
    val cards: List<QuizQuestion> = emptyList()
)

/** One row of GET /flashcards/{user_id} — wrapped in {"flashcard_sets": [...]}, not a bare array. */
data class FlashcardHistoryEntry(
    val id: String = "",
    @SerializedName("source_pdf")     val sourcePdf:      String? = null,
    @SerializedName("created_at")     val createdAt:      String  = "",
    @SerializedName("card_count")     val cardCount:      Int     = 0,
    @SerializedName("cards_revealed") val cardsRevealed:  Int     = 0
)

/**
 * GET /flashcards/{user_id}/{set_id} — a saved set opened read-only. This response has
 * no cards_revealed field (that only lives on the history-index row), so it isn't modeled here.
 */
data class FlashcardSetDetail(
    val id: String = "",
    @SerializedName("source_pdf") val sourcePdf: String? = null,
    @SerializedName("created_at") val createdAt: String  = "",
    val cards: List<QuizQuestion> = emptyList()
)

/**
 * One row of GET /quizzes/{user_id} — wrapped in {"quizzes": [...]}, not a bare array.
 * [correct] is null for a quiz that's been generated but not yet submitted via
 * POST /quiz-result — that's a real, common state, not missing data.
 */
data class QuizHistoryEntry(
    // Legacy rows omit "id" entirely — they were recorded before the backend issued a
    // quiz_id, so there's no server-side quiz to open or delete. Same Gson caveat as the
    // date fields below: a non-null type + default does not save you here.
    val id: String? = null,
    @SerializedName("source_pdf")      val sourcePdf:      String? = null,
    // Legacy quiz-result rows (written by POST /quiz-result with no quiz_id) send
    // created_at:null and carry their date in a separate `timestamp` field instead.
    // Both are nullable; callers fall back created_at -> timestamp. A bare Gson()
    // ignores the non-null type + default and leaves a real null here, so declaring
    // these non-null crashed the Progress screen. Confirmed 2026-07-28 against a live
    // /progress payload.
    @SerializedName("created_at")      val createdAt:      String? = null,
    @SerializedName("timestamp")       val timestamp:      String? = null,
    @SerializedName("total_questions") val totalQuestions: Int     = 0,
    val correct: Int? = null
)

/**
 * GET /quizzes/{user_id}/{quiz_id} — a saved quiz opened read-only. This response carries
 * no score fields (those only live on the history-index row) — callers should pass the
 * [QuizHistoryEntry] they already have alongside this for the score display.
 */
data class QuizDetail(
    val id: String = "",
    @SerializedName("source_pdf") val sourcePdf: String? = null,
    @SerializedName("created_at") val createdAt: String  = "",
    val questions: List<QuizQuestion> = emptyList()
)
