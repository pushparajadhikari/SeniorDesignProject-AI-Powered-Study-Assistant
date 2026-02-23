package com.example.aistudyassistant

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aistudyassistant.screens.LoginScreen
import com.example.aistudyassistant.screens.DashboardScreen



@Composable
fun AppNavigation() {
    // This controller actually handles moving between screens
    val navController = rememberNavController()

    // NavHost acts as the container holding your screens
    NavHost(navController = navController, startDestination = "login") {

        // Screen 1: Login
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // When login is successful, go to dashboard
                    navController.navigate("dashboard") {
                        // This prevents the user from hitting the "back" button to return to login
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Screen 2: Dashboard
        composable("dashboard") {
            DashboardScreen()
        }
    }
}
