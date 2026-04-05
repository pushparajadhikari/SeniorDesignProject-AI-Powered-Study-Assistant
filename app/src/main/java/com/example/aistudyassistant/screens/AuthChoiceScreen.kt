package com.example.aistudyassistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun AuthChoiceScreen(
    onLogin:  () -> Unit,
    onSignup: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd))
            )
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Logo area ─────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📖", fontSize = 44.sp)
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text       = "StudyAI",
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )

            Text(
                text      = "Smart Notes. Smarter You.",
                fontSize  = 15.sp,
                color     = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(56.dp))

            // ── Auth card ─────────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    Text(
                        text       = "Get Started",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text     = "Join thousands of students studying smarter",
                        fontSize = 14.sp,
                        color    = TextSecondary
                    )

                    Spacer(Modifier.height(24.dp))

                    // Primary — Sign up
                    Button(
                        onClick = onSignup,
                        shape   = RoundedCornerShape(14.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "Create Account",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Secondary — Login
                    OutlinedButton(
                        onClick = onLogin,
                        shape   = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "Sign In",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color      = TextPrimary
                        )
                    }
                }
            }
        }
    }
}