package com.example.aistudyassistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onBack:          () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage    by remember { mutableStateOf("") }
    var isLoading       by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SurfaceLight)) {

        Box(
            modifier = Modifier
                .fillMaxWidth().height(220.dp)
                .background(Brush.verticalGradient(listOf(GradientMid, GradientEnd)))
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

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                Text("Create Account ✨", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Start studying smarter today", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }

            Spacer(Modifier.height(24.dp))

            Card(
                shape    = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    Spacer(Modifier.height(4.dp))

                    // Full Name
                    Text("Full Name", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value         = name,
                        onValueChange = { name = it; errorMessage = "" },
                        placeholder   = { Text("Your name") },
                        leadingIcon   = { Icon(Icons.Default.Person, null, tint = BrandViolet) },
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Email
                    Text("Email", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value           = email,
                        onValueChange   = { email = it; errorMessage = "" },
                        placeholder     = { Text("you@university.edu") },
                        leadingIcon     = { Icon(Icons.Default.Email, null, tint = BrandViolet) },
                        shape           = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Password
                    Text("Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value                = password,
                        onValueChange        = { password = it; errorMessage = "" },
                        placeholder          = { Text("Min. 8 characters") },
                        leadingIcon          = { Icon(Icons.Default.Lock, null, tint = BrandViolet) },
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

                    Spacer(Modifier.height(12.dp))

                    // Confirm Password
                    Text("Confirm Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value                = confirmPassword,
                        onValueChange        = { confirmPassword = it; errorMessage = "" },
                        placeholder          = { Text("Repeat password") },
                        leadingIcon          = { Icon(Icons.Default.Lock, null, tint = BrandViolet) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape                = RoundedCornerShape(12.dp),
                        singleLine           = true,
                        modifier             = Modifier.fillMaxWidth()
                    )

                    // Error
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

                    Spacer(Modifier.height(24.dp))

                    // Local validation first, then register
                    Button(
                        onClick = {
                            errorMessage = when {
                                password != confirmPassword -> "Passwords do not match"
                                else -> ""
                            }
                            if (errorMessage.isEmpty()) {
                                isLoading = true
                                scope.launch {
                                    val error = UserManager.registerWithServer(
                                        context, name.trim(), email.trim(), password
                                    )
                                    if (error == null) {
                                        onSignupSuccess()
                                    } else {
                                        errorMessage = error
                                        isLoading    = false
                                    }
                                }
                            }
                        },
                        enabled  = !isLoading,
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}