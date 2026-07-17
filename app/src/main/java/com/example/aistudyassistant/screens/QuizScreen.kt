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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.models.QuizQuestion
import com.example.aistudyassistant.network.ApiService
import com.example.aistudyassistant.ui.components.BrandLogoMark
import com.example.aistudyassistant.ui.theme.*

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(onBack: () -> Unit) {

    var isLoading       by remember { mutableStateOf(true) }
    var errorMessage    by remember { mutableStateOf<String?>(null) }
    var questions       by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var showResults     by remember { mutableStateOf(false) }
    var refreshKey      by remember { mutableStateOf(0) }   // increment to reload

    val score = selectedAnswers.entries.count { (qi, ai) ->
        questions.getOrNull(qi)?.correctIndex == ai
    }

    // Fetch questions from the backend whenever refreshKey changes
    LaunchedEffect(refreshKey) {
        isLoading       = true
        errorMessage    = null
        selectedAnswers = mutableMapOf()
        showResults     = false

        ApiService.generateQuiz()
            .onSuccess { q ->
                questions = q
                isLoading = false
            }
            .onFailure { err ->
                errorMessage = err.message ?: "Quiz generation failed"
                isLoading    = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogoMark(size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Quiz", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            Text("From your notes", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick  = { refreshKey++ },
                        enabled  = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, "Regenerate quiz", tint = BrandTeal)
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

        // ── Loading ───────────────────────────────────────────────────────
        if (isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BrandViolet)
                    Spacer(Modifier.height(16.dp))
                    Text("Generating quiz from your notes…", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("This may take up to a minute.", fontSize = 12.sp, color = TextSecondary)
                }
            }
            return@Scaffold
        }

        // ── Error ─────────────────────────────────────────────────────────
        if (errorMessage != null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Could not generate quiz",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 18.sp,
                        color      = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMessage ?: "",
                        fontSize  = 13.sp,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { refreshKey++ },
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again", color = Color.White)
                    }
                }
            }
            return@Scaffold
        }

        // ── Quiz content ──────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Score banner (shown after submit)
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
                                text     = if (score == questions.size) "🎉" else "📊",
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

            // Questions
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

            // Submit / Try again
            item {
                if (!showResults) {
                    Button(
                        onClick  = { showResults = true },
                        enabled  = selectedAnswers.size == questions.size,
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            "Submit Answers",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White
                        )
                    }
                } else {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick  = {
                                selectedAnswers = mutableMapOf()
                                showResults     = false
                            },
                            shape    = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text("Try Again", color = BrandViolet, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick  = { refreshKey++ },
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New Quiz", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
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
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth()
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

            // Options A / B / C / D
            question.options.forEachIndexed { ai, option ->
                val isSelected = selectedAnswer == ai
                val isCorrect  = question.correctIndex == ai
                val isWrong    = showResult && isSelected && !isCorrect

                val bgColor = when {
                    showResult && isCorrect -> AccentGreen.copy(alpha = 0.12f)
                    isWrong                -> AccentRed.copy(alpha = 0.12f)
                    isSelected             -> BrandViolet.copy(alpha = 0.1f)
                    else                   -> Color(0xFFF8F9FA)
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
