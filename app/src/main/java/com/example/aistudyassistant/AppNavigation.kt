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
        if (UserManager.isLoggedIn(context)) "main" else "onboarding"
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
                    navController.navigate("main") {
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
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Main (bottom-nav shell: Home / Chat / Quiz / Progress / Profile) ─
        composable("main") {
            // Auth guard — if session expired, send back to auth
            val session = UserManager.getCurrentSession(context)
            if (session == null) {
                LaunchedEffect(Unit) {
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                }
                return@composable
            }

            MainScaffold(
                onUploadClick  = { navController.navigate("upload") },
                onHistoryClick = { navController.navigate("history") },
                onLogout       = {
                    UserManager.logout(context)
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        // ── Upload PDF (one-off action, reached via the Home FAB) ──────────
        composable("upload") {
            UploadPdfScreen(onBack = { navController.popBackStack() })
        }

        // ── Flashcards (Home -> pick PDF -> pick count -> generate -> reveal) ─
        composable("flashcards") {
            FlashcardScreen(
                onBack        = { navController.popBackStack() },
                onUploadClick = { navController.navigate("upload") }
            )
        }

        // ── History (Quizzes / Flashcards tabs, reached from Chat's History icon) ─
        composable("history") {
            ActivityHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}