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

/** POST /flashcards response — a freshly generated set. */
data class FlashcardSet(
    @SerializedName("set_id") val setId: Int,
    val cards: List<QuizQuestion> = emptyList()
)

/** One row of GET /flashcards/{user_id} — the Flashcards history index. */
data class FlashcardHistoryEntry(
    val id: Int = 0,
    @SerializedName("source_pdf")     val sourcePdf:      String? = null,
    @SerializedName("created_at")     val createdAt:      String  = "",
    @SerializedName("card_count")     val cardCount:      Int     = 0,
    @SerializedName("cards_revealed") val cardsRevealed:  Int     = 0
)

/** GET /flashcards/{user_id}/{set_id} — a saved set opened read-only. */
data class FlashcardSetDetail(
    val id: Int = 0,
    @SerializedName("source_pdf")     val sourcePdf:     String? = null,
    @SerializedName("created_at")     val createdAt:     String  = "",
    @SerializedName("cards_revealed") val cardsRevealed: Int     = 0,
    val cards: List<QuizQuestion> = emptyList()
)

/** One row of GET /quizzes/{user_id} — the Quizzes history index. */
data class QuizHistoryEntry(
    val id: Int = 0,
    @SerializedName("source_pdf")      val sourcePdf:      String? = null,
    @SerializedName("created_at")      val createdAt:      String  = "",
    @SerializedName("total_questions") val totalQuestions: Int     = 0,
    val correct: Int = 0
)

/** GET /quizzes/{user_id}/{quiz_id} — a saved quiz opened read-only. */
data class QuizDetail(
    val id: Int = 0,
    @SerializedName("source_pdf")      val sourcePdf:      String? = null,
    @SerializedName("created_at")      val createdAt:      String  = "",
    val correct: Int = 0,
    @SerializedName("total_questions") val totalQuestions: Int = 0,
    val questions: List<QuizQuestion> = emptyList()
)
