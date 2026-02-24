package com.example.aistudyassistant.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.R

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {

    var page by remember { mutableStateOf(0) }

    val pages = listOf(
        OnboardingPage(
            image = R.drawable.onboarding_study,
            title = "Study Smarter,\nNot Harder",
            description = "Your AI-powered study assistant to help you learn efficiently."
        ),
        OnboardingPage(
            image = R.drawable.onboarding_upload,
            title = "Upload Your\nStudy Material",
            description = "Upload PDFs, notes, and documents in seconds."
        ),
        OnboardingPage(
            image = R.drawable.onboarding_practice,
            title = "Practice &\nMaster Topics",
            description = "Get quizzes, summaries, and identify weak areas instantly."
        )
    )

    val current = pages[page]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        // -------- IMAGE (FIXED SIZE, ALWAYS VISIBLE) --------
        Image(
            painter = painterResource(id = current.image),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // -------- TITLE --------
        Text(
            text = current.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // -------- DESCRIPTION --------
        Text(
            text = current.description,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.weight(1f))

        // -------- DOT INDICATORS --------
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == page) 12.dp else 8.dp)
                        .padding(4.dp)
                        .background(
                            color = if (index == page)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.LightGray,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // -------- BUTTON --------
        Button(
            onClick = {
                if (page < pages.lastIndex) {
                    page++
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (page < pages.lastIndex) "Next" else "Get Started",
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -------- DATA MODEL --------
data class OnboardingPage(
    val image: Int,
    val title: String,
    val description: String
)