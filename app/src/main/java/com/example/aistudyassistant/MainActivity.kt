package com.example.aistudyassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.aistudyassistant.ui.theme.AIStudyAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIStudyAssistantTheme {
                AppNavigation()
            }
        }
    }
}