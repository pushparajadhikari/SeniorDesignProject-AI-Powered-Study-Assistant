package com.example.aistudyassistant.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.models.FlashcardHistoryEntry
import com.example.aistudyassistant.models.FlashcardSetDetail
import com.example.aistudyassistant.models.QuizDetail
import com.example.aistudyassistant.models.QuizHistoryEntry
import com.example.aistudyassistant.network.ApiService
import com.example.aistudyassistant.ui.animation.ShimmerBox
import com.example.aistudyassistant.ui.animation.StaggeredEntranceItem
import com.example.aistudyassistant.ui.components.BrandLogoMark
import com.example.aistudyassistant.ui.components.DestructiveConfirmDialog
import com.example.aistudyassistant.ui.components.formatUploadDate
import com.example.aistudyassistant.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class HistoryTab(val label: String) { QUIZZES("Quizzes"), FLASHCARDS("Flashcards") }

/**
 * One History destination with Quizzes / Flashcards tabs, reached from the History
 * icon already on the Chat screen's top bar. Reads the signed-in user itself,
 * matching how every other top-level screen in this app sources its session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(onBack: () -> Unit) {
    val context  = LocalContext.current
    val serverId = remember { UserManager.getCurrentSession(context)?.serverId }

    var tab                by remember { mutableStateOf(HistoryTab.QUIZZES) }
    var openQuizId         by remember { mutableStateOf<Int?>(null) }
    var openFlashcardSetId by remember { mutableStateOf<Int?>(null) }

    if (openQuizId != null) {
        QuizDetailScreen(userId = serverId, quizId = openQuizId!!, onBack = { openQuizId = null })
        return
    }
    if (openFlashcardSetId != null) {
        FlashcardSetDetailScreen(userId = serverId, setId = openFlashcardSetId!!, onBack = { openFlashcardSetId = null })
        return
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandLogoMark(size = 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("History", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = Color.White,
                        titleContentColor = TextPrimary
                    )
                )
                TabRow(
                    selectedTabIndex = tab.ordinal,
                    containerColor   = Color.White,
                    contentColor     = BrandTeal
                ) {
                    HistoryTab.entries.forEach { t ->
                        Tab(
                            selected = tab == t,
                            onClick  = { tab = t },
                            text     = { Text(t.label, fontWeight = FontWeight.Medium) }
                        )
                    }
                }
            }
        },
        containerColor = SurfaceLight
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                HistoryTab.QUIZZES     -> QuizHistoryList(serverId, onOpen = { openQuizId = it })
                HistoryTab.FLASHCARDS  -> FlashcardHistoryList(serverId, onOpen = { openFlashcardSetId = it })
            }
        }
    }
}

// ── Quizzes tab ────────────────────────────────────────────────────────────────

@Composable
private fun QuizHistoryList(userId: Int?, onOpen: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var isLoading    by remember { mutableStateOf(true) }
    var entries      by remember { mutableStateOf<List<QuizHistoryEntry>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey     by remember { mutableStateOf(0) }

    var pendingDelete by remember { mutableStateOf<QuizHistoryEntry?>(null) }
    var deleteError    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId, retryKey) {
        if (userId == null) { isLoading = false; return@LaunchedEffect }
        isLoading    = true
        errorMessage = null
        ApiService.getQuizHistory(userId)
            .onSuccess { entries = it.sortedByDescending { e -> e.createdAt }; isLoading = false }
            .onFailure { errorMessage = it.message ?: "Failed to load quiz history"; isLoading = false }
    }

    LaunchedEffect(deleteError) {
        if (deleteError != null) { delay(4000); deleteError = null }
    }

    fun deleteQuiz(entry: QuizHistoryEntry) {
        val uid = userId ?: return
        val previous = entries
        entries = entries.filterNot { it.id == entry.id }   // optimistic
        scope.launch {
            ApiService.deleteQuiz(uid, entry.id)
                .onFailure { err ->
                    entries     = previous                   // restore the row
                    deleteError = err.message ?: "Could not delete this quiz"
                }
        }
    }

    pendingDelete?.let { entry ->
        DestructiveConfirmDialog(
            title     = "Delete this quiz?",
            message   = "This can't be undone.",
            onConfirm = { deleteQuiz(entry); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }

    HistoryListContent(
        isLoading    = isLoading,
        errorMessage = errorMessage,
        isEmpty      = entries.isEmpty(),
        onRetry      = { retryKey++ },
        deleteError  = deleteError,
        emptyEmoji   = "🎯",
        emptyTitle   = "No quizzes taken yet",
        emptySubtitle = "Finish a quiz and it will show up here"
    ) {
        itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
            StaggeredEntranceItem(index = index, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                HistoryRow(
                    title       = entry.sourcePdf ?: "All documents",
                    subtitle    = "${entry.correct} / ${entry.totalQuestions}",
                    dateLabel   = formatUploadDate(entry.createdAt),
                    icon        = Icons.Default.Description,
                    accentColor = BrandViolet,
                    onClick     = { onOpen(entry.id) },
                    onLongClick = { pendingDelete = entry }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizDetailScreen(userId: Int?, quizId: Int, onBack: () -> Unit) {
    var isLoading    by remember { mutableStateOf(true) }
    var detail       by remember { mutableStateOf<QuizDetail?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId, quizId) {
        if (userId == null) { isLoading = false; errorMessage = "Please log in again"; return@LaunchedEffect }
        ApiService.getQuizDetail(userId, quizId)
            .onSuccess { detail = it; isLoading = false }
            .onFailure { errorMessage = it.message ?: "Failed to load this quiz"; isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quiz", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        detail?.let {
                            Text(it.sourcePdf ?: "All documents", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = TextPrimary)
            )
        },
        containerColor = SurfaceLight
    ) { padding ->
        when {
            isLoading -> CenteredLoading(Modifier.fillMaxSize().padding(padding), "Loading quiz…", BrandViolet)
            errorMessage != null -> CenteredError(Modifier.fillMaxSize().padding(padding), errorMessage!!, BrandViolet) {}
            else -> {
                val d = detail ?: return@Scaffold
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape  = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${d.correct} / ${d.totalQuestions} correct",
                                color      = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                modifier   = Modifier.padding(20.dp)
                            )
                        }
                    }
                    itemsIndexed(d.questions) { qi, question ->
                        // Read-only: showResult highlights the correct answer, no
                        // selection is possible (onAnswerSelect is a no-op).
                        QuizCard(
                            questionIndex  = qi,
                            question       = question,
                            selectedAnswer = null,
                            showResult     = true,
                            onAnswerSelect = {}
                        )
                    }
                }
            }
        }
    }
}

// ── Flashcards tab ───────────────────────────────────────────────────────────

@Composable
private fun FlashcardHistoryList(userId: Int?, onOpen: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var isLoading    by remember { mutableStateOf(true) }
    var entries      by remember { mutableStateOf<List<FlashcardHistoryEntry>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey     by remember { mutableStateOf(0) }

    var pendingDelete by remember { mutableStateOf<FlashcardHistoryEntry?>(null) }
    var deleteError    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId, retryKey) {
        if (userId == null) { isLoading = false; return@LaunchedEffect }
        isLoading    = true
        errorMessage = null
        ApiService.getFlashcardHistory(userId)
            .onSuccess { entries = it.sortedByDescending { e -> e.createdAt }; isLoading = false }
            .onFailure { errorMessage = it.message ?: "Failed to load flashcard history"; isLoading = false }
    }

    LaunchedEffect(deleteError) {
        if (deleteError != null) { delay(4000); deleteError = null }
    }

    fun deleteSet(entry: FlashcardHistoryEntry) {
        val uid = userId ?: return
        val previous = entries
        entries = entries.filterNot { it.id == entry.id }   // optimistic
        scope.launch {
            ApiService.deleteFlashcardSet(uid, entry.id)
                .onFailure { err ->
                    entries     = previous                   // restore the row
                    deleteError = err.message ?: "Could not delete this set"
                }
        }
    }

    pendingDelete?.let { entry ->
        DestructiveConfirmDialog(
            title     = "Delete this flashcard set?",
            message   = "This can't be undone.",
            onConfirm = { deleteSet(entry); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }

    HistoryListContent(
        isLoading    = isLoading,
        errorMessage = errorMessage,
        isEmpty      = entries.isEmpty(),
        onRetry      = { retryKey++ },
        deleteError  = deleteError,
        emptyEmoji   = "🗂️",
        emptyTitle   = "No flashcard sets yet",
        emptySubtitle = "Generate a set from a PDF and it will show up here"
    ) {
        itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
            StaggeredEntranceItem(index = index, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                HistoryRow(
                    title       = entry.sourcePdf ?: "All documents",
                    subtitle    = "${entry.cardsRevealed} of ${entry.cardCount} revealed",
                    dateLabel   = formatUploadDate(entry.createdAt),
                    icon        = Icons.Default.Style,
                    accentColor = BrandTeal,
                    onClick     = { onOpen(entry.id) },
                    onLongClick = { pendingDelete = entry }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardSetDetailScreen(userId: Int?, setId: Int, onBack: () -> Unit) {
    var isLoading    by remember { mutableStateOf(true) }
    var detail       by remember { mutableStateOf<FlashcardSetDetail?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId, setId) {
        if (userId == null) { isLoading = false; errorMessage = "Please log in again"; return@LaunchedEffect }
        ApiService.getFlashcardSet(userId, setId)
            .onSuccess { detail = it; isLoading = false }
            .onFailure { errorMessage = it.message ?: "Failed to load this set"; isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Flashcard Set", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        detail?.let {
                            Text(it.sourcePdf ?: "All documents", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = TextPrimary)
            )
        },
        containerColor = SurfaceLight
    ) { padding ->
        when {
            isLoading -> CenteredLoading(Modifier.fillMaxSize().padding(padding), "Loading set…", BrandTeal)
            errorMessage != null -> CenteredError(Modifier.fillMaxSize().padding(padding), errorMessage!!, BrandTeal) {}
            else -> {
                val d = detail ?: return@Scaffold
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Read-only review: every card is shown already revealed.
                    itemsIndexed(d.cards) { _, card ->
                        FlashcardCardView(question = card, isRevealed = true, onReveal = {})
                    }
                }
            }
        }
    }
}

// ── Shared row / state composables ────────────────────────────────────────────

@Composable
private fun HistoryListContent(
    isLoading:     Boolean,
    errorMessage:  String?,
    isEmpty:       Boolean,
    onRetry:       () -> Unit,
    emptyEmoji:    String,
    emptyTitle:    String,
    emptySubtitle: String,
    deleteError:   String? = null,
    content:       androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    when {
        errorMessage != null -> CenteredError(Modifier.fillMaxSize(), errorMessage, BrandTeal, onRetry)
        isLoading -> Column(
            modifier            = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(64.dp)) }
        }
        isEmpty -> CenteredEmpty(Modifier.fillMaxSize().padding(24.dp), emptyEmoji, emptyTitle, emptySubtitle)
        else -> Column(modifier = Modifier.fillMaxSize()) {
            if (deleteError != null) {
                Card(
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(deleteError, fontSize = 12.sp, color = AccentRed, modifier = Modifier.padding(12.dp))
                }
            }
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                content        = content
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    title:       String,
    subtitle:    String,
    dateLabel:   String,
    icon:        androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick:     () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Card(
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text("$subtitle  ·  $dateLabel", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun CenteredLoading(modifier: Modifier, message: String, color: Color) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = color)
            Spacer(Modifier.height(16.dp))
            Text(message, color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun CenteredError(modifier: Modifier, message: String, color: Color, onRetry: () -> Unit) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry", color = Color.White)
            }
        }
    }
}

@Composable
private fun CenteredEmpty(modifier: Modifier, emoji: String, title: String, subtitle: String) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}
