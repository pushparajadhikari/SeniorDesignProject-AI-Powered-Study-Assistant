package com.example.aistudyassistant.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.models.FlashcardSet
import com.example.aistudyassistant.models.QuizQuestion
import com.example.aistudyassistant.network.ApiService
import com.example.aistudyassistant.ui.components.BrandLogoMark
import com.example.aistudyassistant.ui.components.PdfSource
import com.example.aistudyassistant.ui.components.PdfSourcePicker
import com.example.aistudyassistant.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home -> pick PDF -> pick count -> generate -> tap-to-reveal cards.
 * Unlike Quiz, nothing here is graded: the user is looking answers up, not
 * choosing one, so there's no scoring or submit step — only reveal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(onBack: () -> Unit, onUploadClick: () -> Unit) {

    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val serverId = remember { UserManager.getCurrentSession(context)?.serverId }

    var pdfSource       by remember { mutableStateOf<PdfSource?>(null) }
    var count           by remember { mutableStateOf(10) }
    var isGenerating    by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var flashcardSet    by remember { mutableStateOf<FlashcardSet?>(null) }
    var currentIndex    by remember { mutableStateOf(0) }
    // Distinct card indices revealed — NOT a tap count. Re-revealing the same card
    // twice must not inflate this number past the set size.
    var revealedIndices by remember { mutableStateOf(setOf<Int>()) }

    fun generate() {
        val source = pdfSource ?: return
        isGenerating    = true
        generationError = null
        val sourceFilename = (source as? PdfSource.Specific)?.filename
        scope.launch {
            ApiService.generateFlashcards(serverId, sourceFilename, count)
                .onSuccess { set ->
                    flashcardSet    = set
                    currentIndex    = 0
                    revealedIndices = emptySet()
                    isGenerating    = false
                }
                .onFailure { err ->
                    generationError = err.message ?: "Flashcard generation failed"
                    isGenerating    = false
                }
        }
    }

    fun revealCurrent() {
        val set = flashcardSet ?: return
        if (currentIndex in revealedIndices) return
        revealedIndices = revealedIndices + currentIndex
        val uid = serverId ?: return
        // Reported as each new card is revealed (not only on exit) so the count is
        // never lost if the user backgrounds the app instead of tapping Back.
        scope.launch { ApiService.postFlashcardReveal(uid, set.id, revealedIndices.size) }
    }

    val sourceLabel = when (val s = pdfSource) {
        null                   -> "From your notes"
        PdfSource.AllDocuments -> "All documents"
        is PdfSource.Specific  -> s.filename
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogoMark(size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Flashcards", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                            val subtitle = flashcardSet?.let { "${currentIndex + 1} of ${it.cards.size}" } ?: sourceLabel
                            Text(subtitle, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (pdfSource != null) {
                        TextButton(onClick = {
                            pdfSource       = null
                            flashcardSet    = null
                            generationError = null
                        }) {
                            Text("New Set", color = BrandTeal, fontSize = 13.sp)
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

        // ── Step 1: choose a source ──────────────────────────────────────
        if (pdfSource == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                PdfSourcePicker(
                    userId        = serverId,
                    selected      = pdfSource,
                    onSelect      = { pdfSource = it },
                    onUploadClick = onUploadClick,
                    accentColor   = BrandTeal
                )
            }
            return@Scaffold
        }

        // ── Step 2: choose a count, then generate ────────────────────────
        if (flashcardSet == null && !isGenerating && generationError == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FlashcardCountPicker(current = count, onSelect = { count = it })
                Button(
                    onClick  = { generate() },
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Generate Flashcards", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            return@Scaffold
        }

        // ── Generating ────────────────────────────────────────────────────
        if (isGenerating) {
            GeneratingLoader(count = count, modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        // ── Error ─────────────────────────────────────────────────────────
        if (generationError != null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Could not generate flashcards", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(generationError ?: "", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { generate() },
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                    ) {
                        Text("Try Again", color = Color.White)
                    }
                }
            }
            return@Scaffold
        }

        // ── Card view ─────────────────────────────────────────────────────
        val set = flashcardSet ?: return@Scaffold
        val card = set.cards.getOrNull(currentIndex) ?: return@Scaffold
        val isRevealed = currentIndex in revealedIndices

        Column(
            modifier            = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FlashcardCardView(
                question   = card,
                isRevealed = isRevealed,
                onReveal   = { revealCurrent() }
            )

            Row(
                modifier              = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous card", tint = if (currentIndex > 0) BrandTeal else Color.LightGray)
                }
                Text("${currentIndex + 1} of ${set.cards.size}", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                IconButton(
                    onClick = { if (currentIndex < set.cards.size - 1) currentIndex++ },
                    enabled = currentIndex < set.cards.size - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next card", tint = if (currentIndex < set.cards.size - 1) BrandTeal else Color.LightGray)
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun FlashcardCountPicker(current: Int, onSelect: (Int) -> Unit) {
    val presets = listOf(5, 10, 15, 20)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("How many cards?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            presets.forEach { n ->
                val selected = n == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) BrandTeal else Color(0xFFF1F3F5))
                        .clickable { onSelect(n) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$n", color = if (selected) Color.White else TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// Package-visible (not private) so the read-only History detail screen can reuse it.
@Composable
fun FlashcardCardView(
    question:   QuizQuestion,
    isRevealed: Boolean,
    onReveal:   () -> Unit
) {
    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isRevealed) { onReveal() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(question.question, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            question.options.forEachIndexed { ai, option ->
                val isCorrect  = ai == question.correctIndex
                val bgColor    = if (isRevealed && isCorrect) AccentGreen.copy(alpha = 0.12f) else Color(0xFFF8F9FA)
                val borderColor = if (isRevealed && isCorrect) AccentGreen else Color.LightGray

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text       = listOf("A", "B", "C", "D").getOrElse(ai) { "" },
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = borderColor
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(option, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    if (isRevealed && isCorrect) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (isRevealed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandBlue.copy(alpha = 0.08f))
                        .padding(12.dp)
                ) {
                    Text("💡 ${question.explanation}", fontSize = 13.sp, color = BrandBlue)
                }
            } else {
                Text(
                    "Tap the card to reveal the answer",
                    fontSize  = 12.sp,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Determinate-feeling generation loader — a real progress bar + rotating status text. */
@Composable
private fun GeneratingLoader(count: Int, modifier: Modifier = Modifier) {
    val phases = remember(count) {
        listOf(
            "Reading your document…",
            "Finding key concepts…",
            "Writing $count flashcards…",
            "Almost done…"
        )
    }
    var phaseIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            phaseIndex = (phaseIndex + 1) % phases.size
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(0.92f, animationSpec = tween(22000, easing = LinearEasing))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BrandTeal)
            Spacer(Modifier.height(20.dp))
            Text(phases[phaseIndex], color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress   = { progress.value },
                modifier   = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = BrandTeal,
                trackColor = BrandTeal.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(8.dp))
            Text("This can take up to a minute.", fontSize = 12.sp, color = TextSecondary)
        }
    }
}
