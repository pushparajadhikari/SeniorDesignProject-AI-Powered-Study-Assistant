package com.example.aistudyassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.aistudyassistant.screens.LoginScreen
import com.example.aistudyassistant.ui.theme.AIStudyAssistantTheme // This might be slightly different depending on your exact project name

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIStudyAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Here is where we display your new screen!
                    LoginScreen(
                        onLoginSuccess = {
                            println("Login button was clicked!")
                        }
                    )
                }
            }
        }
    }
}
