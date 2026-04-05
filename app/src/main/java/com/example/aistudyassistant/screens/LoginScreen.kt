package com.example.aistudyassistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack:         () -> Unit
) {
    val context = LocalContext.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage    by remember { mutableStateOf("") }
    var isLoading       by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SurfaceLight)) {

        Box(
            modifier = Modifier
                .fillMaxWidth().height(240.dp)
                .background(Brush.verticalGradient(listOf(GradientStart, GradientMid)))
        )

        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                Text("Welcome back 👋", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Sign in to continue studying", fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))
            }

            Spacer(Modifier.height(32.dp))

            Card(
                shape    = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    Spacer(Modifier.height(8.dp))

                    Text("Email", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value           = email,
                        onValueChange   = { email = it; errorMessage = "" },
                        placeholder     = { Text("you@university.edu") },
                        leadingIcon     = { Icon(Icons.Default.Email, null, tint = BrandTeal) },
                        shape           = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value                = password,
                        onValueChange        = { password = it; errorMessage = "" },
                        placeholder          = { Text("••••••••") },
                        leadingIcon          = { Icon(Icons.Default.Lock, null, tint = BrandTeal) },
                        trailingIcon         = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape                = RoundedCornerShape(12.dp),
                        singleLine           = true,
                        modifier             = Modifier.fillMaxWidth()
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape  = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.08f))
                        ) {
                            Text(
                                errorMessage,
                                color    = AccentRed,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = {
                            isLoading    = true
                            errorMessage = ""
                            val error = UserManager.login(context, email.trim(), password)
                            if (error == null) {
                                onLoginSuccess()
                            } else {
                                errorMessage = error
                                isLoading    = false
                            }
                        },
                        enabled  = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    TextButton(
                        onClick  = {},
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Forgot password?", color = BrandBlue, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}