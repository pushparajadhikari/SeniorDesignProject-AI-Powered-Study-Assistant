package com.example.aistudyassistant.models

/**
 * Shared data model for quiz questions.
 * Used by both ApiService (deserialization) and QuizScreen (display).
 */
data class QuizQuestion(
    val question:     String,
    val options:      List<String>,
    val correctIndex: Int,
    val explanation:  String
)
