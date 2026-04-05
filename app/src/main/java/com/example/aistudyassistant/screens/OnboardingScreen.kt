package com.example.aistudyassistant.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.ui.theme.*

// ── Onboarding data ──────────────────────────────────────────────────────────

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

private val pages = listOf(
    OnboardingPage(
        emoji       = "📚",
        title       = "Study Smarter",
        subtitle    = "Upload your notes and get instant AI-powered answers drawn directly from your own material.",
        accentColor = BrandTeal
    ),
    OnboardingPage(
        emoji       = "📄",
        title       = "Upload Anything",
        subtitle    = "Drop in PDFs, lecture notes, or textbooks. We read the text and understand the diagrams too.",
        accentColor = BrandBlue
    ),
    OnboardingPage(
        emoji       = "🎯",
        title       = "Practice Better",
        subtitle    = "Generate quizzes from your own notes. Know exactly what you understand and what to review.",
        accentColor = BrandViolet
    )
)

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {

    var pageIndex by remember { mutableStateOf(0) }
    val page = pages[pageIndex]

    val gradientBrush = Brush.verticalGradient(
        listOf(GradientStart, GradientMid, GradientEnd)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {

        // ── Skip button top right ─────────────────────────────────────────
        if (pageIndex < pages.lastIndex) {
            TextButton(
                onClick  = onFinish,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("Skip", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Large emoji illustration ──────────────────────────────────
            Box(
                modifier            = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment    = Alignment.Center
            ) {
                Text(page.emoji, fontSize = 72.sp)
            }

            Spacer(Modifier.height(48.dp))

            // ── Card ─────────────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier            = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Accent pill
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(page.accentColor)
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text       = page.title,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text      = page.subtitle,
                        fontSize  = 15.sp,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(28.dp))

                    // ── Page dots ─────────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        pages.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (i == pageIndex) 24.dp else 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (i == pageIndex) page.accentColor
                                        else Color.LightGray
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Button ────────────────────────────────────────────
                    Button(
                        onClick = {
                            if (pageIndex < pages.lastIndex) pageIndex++
                            else onFinish()
                        },
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = page.accentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text       = if (pageIndex < pages.lastIndex) "Continue" else "Get Started",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White
                        )
                    }
                }
            }
        }
    }
}