package com.example.aistudyassistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.ui.theme.*

// ── Data model ────────────────────────────────────────────────────────────────

data class QuizQuestion(
    val question:      String,
    val options:       List<String>,
    val correctIndex:  Int,
    val explanation:   String
)

// ── Placeholder questions (replace with FastAPI /quiz call) ───────────────────

private val sampleQuestions = listOf(
    QuizQuestion(
        question     = "What technique does the app use to find answers from your notes?",
        options      = listOf(
            "Full document search",
            "Retrieval-Augmented Generation (RAG)",
            "Keyword matching",
            "Random sampling"
        ),
        correctIndex = 1,
        explanation  = "RAG retrieves the most semantically relevant chunks and passes them to the LLM."
    ),
    QuizQuestion(
        question     = "Which model converts your text into vectors for search?",
        options      = listOf("llama3.2:1b", "moondream", "nomic-embed-text", "GPT-4"),
        correctIndex = 2,
        explanation  = "nomic-embed-text produces 768-dimensional embeddings for similarity search."
    ),
    QuizQuestion(
        question     = "What does the vision model moondream do?",
        options      = listOf(
            "Generates answers",
            "Describes images and diagrams in PDFs",
            "Converts text to speech",
            "Encrypts your files"
        ),
        correctIndex = 1,
        explanation  = "moondream reads images in your PDFs and produces text descriptions that are also indexed."
    )
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(onBack: () -> Unit) {

    var isLoading       by remember { mutableStateOf(false) }
    var questions       by remember { mutableStateOf(sampleQuestions) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var showResults     by remember { mutableStateOf(false) }

    val score = selectedAnswers.entries.count { (qi, ai) -> questions[qi].correctIndex == ai }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quiz", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text("From your notes", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // TODO: call FastAPI /quiz
                        selectedAnswers = mutableMapOf()
                        showResults     = false
                    }) {
                        Icon(Icons.Default.Refresh, "Regenerate", tint = BrandTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = SurfaceLight
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BrandViolet)
                    Spacer(Modifier.height(12.dp))
                    Text("Generating quiz from your notes…", color = TextSecondary)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Score banner (shown after submit) ─────────────────────────
            if (showResults) {
                item {
                    Card(
                        shape  = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (score == questions.size) AccentGreen else BrandBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier          = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text   = if (score == questions.size) "🎉" else "📊",
                                fontSize = 36.sp
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "$score / ${questions.size} correct",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 20.sp,
                                    color      = Color.White
                                )
                                Text(
                                    if (score == questions.size) "Perfect score!"
                                    else "Keep studying — you've got this!",
                                    fontSize = 13.sp,
                                    color    = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Questions ─────────────────────────────────────────────────
            itemsIndexed(questions) { qi, question ->
                QuizCard(
                    questionIndex  = qi,
                    question       = question,
                    selectedAnswer = selectedAnswers[qi],
                    showResult     = showResults,
                    onAnswerSelect = { ai ->
                        if (!showResults) {
                            selectedAnswers = selectedAnswers.toMutableMap().apply { put(qi, ai) }
                        }
                    }
                )
            }

            // ── Submit / Try again button ─────────────────────────────────
            item {
                if (!showResults) {
                    Button(
                        onClick  = { showResults = true },
                        enabled  = selectedAnswers.size == questions.size,
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "Submit Answers",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick  = {
                            selectedAnswers = mutableMapOf()
                            showResults     = false
                        },
                        shape    = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = BrandViolet)
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again", color = BrandViolet, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Quiz card component ───────────────────────────────────────────────────────

@Composable
fun QuizCard(
    questionIndex:  Int,
    question:       QuizQuestion,
    selectedAnswer: Int?,
    showResult:     Boolean,
    onAnswerSelect: (Int) -> Unit
) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Question number + text
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier         = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BrandViolet),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${questionIndex + 1}",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    question.question,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Options
            question.options.forEachIndexed { ai, option ->
                val isSelected = selectedAnswer == ai
                val isCorrect  = question.correctIndex == ai
                val isWrong    = showResult && isSelected && !isCorrect

                val bgColor = when {
                    showResult && isCorrect  -> AccentGreen.copy(alpha = 0.12f)
                    isWrong                  -> AccentRed.copy(alpha = 0.12f)
                    isSelected               -> BrandViolet.copy(alpha = 0.1f)
                    else                     -> Color(0xFFF8F9FA)
                }
                val borderColor = when {
                    showResult && isCorrect -> AccentGreen
                    isWrong                -> AccentRed
                    isSelected             -> BrandViolet
                    else                   -> Color.LightGray
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onAnswerSelect(ai) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option letter
                    Text(
                        text       = listOf("A", "B", "C", "D")[ai],
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = borderColor,
                        modifier   = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.1f))
                            .wrapContentSize()
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(option, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    if (showResult && isCorrect) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Explanation (shown after submit)
            if (showResult) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandBlue.copy(alpha = 0.08f))
                        .padding(12.dp)
                ) {
                    Text(
                        "💡 ${question.explanation}",
                        fontSize = 13.sp,
                        color    = BrandBlue
                    )
                }
            }
        }
    }
}