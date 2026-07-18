package com.example.aistudyassistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
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
import com.example.aistudyassistant.models.DocumentEntry
import com.example.aistudyassistant.models.FlashcardHistoryEntry
import com.example.aistudyassistant.models.QuizHistoryEntry
import com.example.aistudyassistant.network.ApiService
import com.example.aistudyassistant.ui.animation.CountUpText
import com.example.aistudyassistant.ui.components.BrandLogoMark
import com.example.aistudyassistant.ui.components.formatUploadDate
import com.example.aistudyassistant.ui.theme.*
import com.google.gson.annotations.SerializedName

// ── Data model ────────────────────────────────────────────────────────────────
// Shared with ApiService.getProgress(). Confirmed 2026-07-18 against the live,
// locked-response_model backend. pdfs_uploaded and quiz_history/flashcard_sets now
// have the exact same shape as the picker/history-screen models, so this reuses
// DocumentEntry/QuizHistoryEntry/FlashcardHistoryEntry instead of near-duplicating
// them as PdfUpload/QuizResult — those two are gone.
//
// flashcard_sets was previously (wrongly) modeled as an Int; it's actually the same
// array of objects flashcard history uses. That mismatch was the parse crash behind
// "Could not load progress / Couldn't read the server's response."

data class UserProgress(
    @SerializedName("pdfs_uploaded")             val pdfsUploaded:            List<DocumentEntry>         = emptyList(),
    @SerializedName("quiz_history")              val quizHistory:             List<QuizHistoryEntry>      = emptyList(),
    @SerializedName("questions_answered_total")  val questionsAnsweredTotal:  Int = 0,
    @SerializedName("questions_correct_total")   val questionsCorrectTotal:   Int = 0,
    @SerializedName("flashcards_revealed_total") val flashcardsRevealedTotal: Int = 0,
    @SerializedName("flashcard_sets")            val flashcardSets:           List<FlashcardHistoryEntry> = emptyList()
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(userId: Int?) {

    var isLoading    by remember { mutableStateOf(true) }
    var progress     by remember { mutableStateOf<UserProgress?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey     by remember { mutableStateOf(0) }   // increment to re-fetch

    // Re-fetch whenever the user changes or the user taps Retry.
    LaunchedEffect(userId, retryKey) {
        if (userId == null) {
            // No server id — nothing we can fetch. The UI shows a sign-in prompt.
            isLoading = false
            return@LaunchedEffect
        }
        isLoading    = true
        errorMessage = null
        ApiService.getProgress(userId)
            .onSuccess { progress = it; isLoading = false }
            .onFailure { errorMessage = it.message ?: "Failed to load progress"; isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogoMark(size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Your Progress", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            Text("Uploads & quiz history", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                actions = {
                    if (userId != null) {
                        IconButton(onClick = { retryKey++ }, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = BrandTeal)
                        }
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

        // ── Not signed in (defensive — shouldn't happen post-login) ───────────
        if (userId == null) {
            CenteredMessage(
                modifier = Modifier.fillMaxSize().padding(padding),
                emoji    = "🔒",
                title    = "Please log in again to see your progress",
                subtitle = "We couldn't find your account on the server."
            )
            return@Scaffold
        }

        // ── Loading ───────────────────────────────────────────────────────────
        if (isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BrandTeal)
                    Spacer(Modifier.height(16.dp))
                    Text("Loading your progress…", color = TextSecondary, fontSize = 14.sp)
                }
            }
            return@Scaffold
        }

        // ── Error + Retry ─────────────────────────────────────────────────────
        if (errorMessage != null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Could not load progress",
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
                        onClick = { retryKey++ },
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry", color = Color.White)
                    }
                }
            }
            return@Scaffold
        }

        // ── Content ───────────────────────────────────────────────────────────
        val data = progress ?: UserProgress()
        val accuracy =
            if (data.questionsAnsweredTotal > 0)
                (data.questionsCorrectTotal * 100) / data.questionsAnsweredTotal
            else 0

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Summary card
            item {
                Card(
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryStat("${data.pdfsUploaded.size}", "PDFs", BrandTeal)
                        SummaryStat("${data.quizHistory.size}", "Quizzes", BrandBlue)
                        SummaryStat("$accuracy%", "Accuracy", BrandViolet)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CountUpText(targetValue = data.flashcardsRevealedTotal, fontSize = 22.sp, color = AccentGreen)
                            Spacer(Modifier.height(2.dp))
                            Text("Answers Revealed", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // ── Uploaded PDFs ─────────────────────────────────────────────────
            item {
                Text("Uploaded PDFs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            if (data.pdfsUploaded.isEmpty()) {
                item { EmptyCard("📂", "No PDFs uploaded yet", "Upload a PDF to start building your library") }
            } else {
                items(data.pdfsUploaded) { pdf ->
                    Card(
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier          = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, null, tint = BrandTeal, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pdf.filename.ifBlank { "Untitled document" },
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = TextPrimary
                                )
                                if (pdf.timestamp.isNotBlank()) {
                                    Text(formatUploadDate(pdf.timestamp), fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // ── Quiz history ──────────────────────────────────────────────────
            item {
                Text("Quiz History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            if (data.quizHistory.isEmpty()) {
                item { EmptyCard("🎯", "No quizzes taken yet", "Finish a quiz and your score will show up here") }
            } else {
                items(data.quizHistory) { quiz ->
                    Card(
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier          = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val perfect = quiz.correct != null && quiz.totalQuestions > 0 && quiz.correct == quiz.totalQuestions
                            Text(if (perfect) "🏆" else "📊", fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (quiz.correct != null) "${quiz.correct} / ${quiz.totalQuestions} correct" else "${quiz.totalQuestions} questions — not yet taken",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = TextPrimary
                                )
                                if (quiz.createdAt.isNotBlank()) {
                                    Text(formatUploadDate(quiz.createdAt), fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SummaryStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun EmptyCard(emoji: String, title: String, subtitle: String) {
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CenteredMessage(modifier: Modifier, emoji: String, title: String, subtitle: String) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier         = Modifier.size(64.dp).clip(CircleShape).background(BrandTeal.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 30.sp) }
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}
