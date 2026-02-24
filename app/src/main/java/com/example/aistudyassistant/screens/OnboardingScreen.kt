package com.example.aistudyassistant.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
        Triple(
            R.drawable.onboarding_study,
            "Study smarter",
            "AI powered study assistant for students"
        ),
        Triple(
            R.drawable.onboarding_upload,
            "Upload materials",
            "Upload PDFs, notes & documents easily"
        ),
        Triple(
            R.drawable.onboarding_practice,
            "Practice better",
            "Get quizzes & insights instantly"
        )
    )

    val current = pages[page]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF7F7CFB),
                        Color(0xFFB39DFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(60.dp))

            Image(
                painter = painterResource(current.first),
                contentDescription = null,
                modifier = Modifier
                    .height(260.dp)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = current.second,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = current.third,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Row {
                        pages.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == page) 10.dp else 8.dp)
                                    .padding(4.dp)
                                    .background(
                                        if (index == page)
                                            Color(0xFF7F7CFB)
                                        else
                                            Color.LightGray,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (page < pages.lastIndex) page++
                            else onFinish()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (page < pages.lastIndex) "Next" else "Get Started",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}