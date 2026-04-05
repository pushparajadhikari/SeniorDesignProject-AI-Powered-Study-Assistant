package com.example.aistudyassistant

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.screens.*

@Composable
fun AppNavigation() {

    val context       = LocalContext.current
    val navController = rememberNavController()

    // Auto-navigate: if already logged in skip onboarding and auth
    val startDestination = remember {
        if (UserManager.isLoggedIn(context)) "dashboard" else "onboarding"
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // ── Onboarding ────────────────────────────────────────────────────
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    navController.navigate("auth") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // ── Auth Choice ───────────────────────────────────────────────────
        composable("auth") {
            AuthChoiceScreen(
                onLogin  = { navController.navigate("login") },
                onSignup = { navController.navigate("signup") }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Signup ────────────────────────────────────────────────────────
        composable("signup") {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────────
        composable("dashboard") {
            // Auth guard — if session expired, send back to auth
            val session = UserManager.getCurrentSession(context)
            if (session == null) {
                LaunchedEffect(Unit) {
                    navController.navigate("auth") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
                return@composable
            }

            DashboardScreen(
                onUploadClick = { navController.navigate("upload") },
                onChatClick   = { navController.navigate("chat") },
                onQuizClick   = { navController.navigate("quiz") },
                onProfileClick = { navController.navigate("profile") },
                onLogout      = {
                    UserManager.logout(context)
                    navController.navigate("auth") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

        // ── Upload PDF ────────────────────────────────────────────────────
        composable("upload") {
            UploadPdfScreen(onBack = { navController.popBackStack() })
        }

        // ── Chat ──────────────────────────────────────────────────────────
        composable("chat") {
            ChatScreen(onBack = { navController.popBackStack() })
        }

        // ── Quiz ──────────────────────────────────────────────────────────
        composable("quiz") {
            QuizScreen(onBack = { navController.popBackStack() })
        }

        // ── Profile ───────────────────────────────────────────────────────
        composable("profile") {
            ProfileScreen(
                onBack   = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}