package com.example.aistudyassistant.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.ui.theme.*

// ── Bottom-nav tabs ─────────────────────────────────────────────────────────────

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME    ("Home",     Icons.Default.Home),
    CHAT    ("Chat",     Icons.AutoMirrored.Filled.Chat),
    QUIZ    ("Quiz",     Icons.Default.Quiz),
    PROGRESS("Progress", Icons.Default.Insights),
    PROFILE ("Profile",  Icons.Default.Person)
}

/**
 * Persistent bottom-navigation shell hosting the five primary tabs.
 * [onUploadClick] is surfaced as a FAB on the Home tab; [onLogout] is forwarded
 * to the Profile tab. The server-assigned user id is read once and passed to the
 * Progress tab so it can fetch per-user data from /progress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    onUploadClick: () -> Unit,
    onLogout:      () -> Unit
) {
    val context     = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    // Backend user id for per-user API calls (Progress tab). The session is stable
    // for the lifetime of this shell, so reading it once is enough.
    val serverId = remember { UserManager.getCurrentSession(context)?.serverId }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick  = { selectedTab = tab },
                        icon     = { Icon(tab.icon, contentDescription = tab.label) },
                        label    = { Text(tab.label, fontSize = 11.sp) },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor   = BrandTeal,
                            selectedTextColor   = BrandTeal,
                            indicatorColor      = BrandTeal.copy(alpha = 0.12f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            // Upload is a Home-tab action, matching the previous "Home FAB" entry point.
            if (selectedTab == MainTab.HOME) {
                FloatingActionButton(
                    onClick        = onUploadClick,
                    containerColor = BrandTeal,
                    contentColor   = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Upload PDF")
                }
            }
        },
        containerColor = SurfaceLight
    ) { padding ->
        // Each tab is a self-contained screen; inset by the scaffold padding so its
        // content sits above the bottom navigation bar.
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                MainTab.HOME     -> DashboardScreen(onProfileClick = { selectedTab = MainTab.PROFILE })
                MainTab.CHAT     -> ChatScreen(onBack = {}, showBackButton = false)
                MainTab.QUIZ     -> QuizScreen(onBack = {}, showBackButton = false)
                MainTab.PROGRESS -> HistoryScreen(userId = serverId)
                MainTab.PROFILE  -> ProfileScreen(onBack = {}, onLogout = onLogout, showBackButton = false)
            }
        }
    }
}
