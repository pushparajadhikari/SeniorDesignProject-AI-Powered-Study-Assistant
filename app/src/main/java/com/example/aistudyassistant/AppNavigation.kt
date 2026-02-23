package com.example.aistudyassistant

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aistudyassistant.screens.*

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "onboarding"
    ) {

        // ---------------- Onboarding ----------------
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    navController.navigate("auth") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // ---------------- Auth Choice ----------------
        composable("auth") {
            AuthChoiceScreen(
                onLogin = { navController.navigate("login") },
                onSignup = { navController.navigate("signup") }
            )
        }

        // ---------------- Login ----------------
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ---------------- Signup ----------------
        composable("signup") {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ---------------- Dashboard ----------------
        composable("dashboard") {
            DashboardScreen(
                onUploadClick = {
                    navController.navigate("upload")
                },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

        // ---------------- Upload PDF ----------------
        composable("upload") {
            UploadPdfScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}