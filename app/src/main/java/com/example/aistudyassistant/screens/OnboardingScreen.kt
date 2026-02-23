package com.example.aistudyassistant.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {

    var page by remember { mutableStateOf(0) }

    val titles = listOf(
        "Welcome to AI Study Assistant",
        "Upload Your Notes",
        "Ask Questions & Practice"
    )

    val descriptions = listOf(
        "Your personal AI-powered study partner",
        "Upload PDFs and class materials easily",
        "Get answers, quizzes, and insights"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(titles[page], fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(descriptions[page])

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (page < 2) page++ else onFinish()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (page < 2) "Next" else "Get Started")
        }
    }
}