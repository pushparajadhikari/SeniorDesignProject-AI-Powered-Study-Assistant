package com.example.aistudyassistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.network.ApiService
import com.example.aistudyassistant.ui.components.BrandLogoBadge
import com.example.aistudyassistant.ui.theme.*
import java.util.Calendar

@Composable
fun DashboardScreen(
    onUploadClick:  () -> Unit,
    onChatClick:    () -> Unit,
    onQuizClick:    () -> Unit,
    onProfileClick: () -> Unit,
    onLogout:       () -> Unit
) {
    val context = LocalContext.current
    val session = UserManager.getCurrentSession(context)

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good morning"
            in 12..17 -> "Good afternoon"
            else      -> "Good evening"
        }
    }

    // ── Backend state ─────────────────────────────────────────────────────────
    var docs         by remember { mutableStateOf<List<String>>(emptyList()) }
    var docsLoading  by remember { mutableStateOf(true) }
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }   // null = checking
    var refreshKey   by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        docsLoading = true
        // Run health-check and docs-list concurrently
        serverOnline = ApiService.checkHealth()
        if (serverOnline == true) {
            ApiService.getDocsList()
                .onSuccess { docs = it }
                .onFailure { /* keep previous list */ }
        }
        docsLoading = false
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(SurfaceLight),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {

        // ── Gradient header ───────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(GradientStart, GradientMid, GradientEnd)))
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column {
                    BrandLogoBadge(
                        size     = 40.dp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("$greeting 👋", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                session?.userName ?: "Student",
                                fontSize   = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Server status dot
                            ServerStatusDot(serverOnline)
                            Spacer(Modifier.width(12.dp))
                            // Profile avatar
                            Box(
                                modifier         = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable(onClick = onProfileClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    session?.userName?.take(1)?.uppercase() ?: "?",
                                    fontSize   = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Stats row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip("${docs.size} PDF${if (docs.size != 1) "s" else ""}", "📄")
                        StatChip("Ask AI", "💬")
                        StatChip("Quiz", "🎯")
                    }
                }
            }
        }

        // ── Quick actions ─────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {

                Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title    = "Upload PDF",
                        subtitle = "Add study material",
                        emoji    = "📄",
                        color    = BrandTeal,
                        onClick  = onUploadClick,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title    = "Ask AI",
                        subtitle = "Chat with your notes",
                        emoji    = "💬",
                        color    = BrandBlue,
                        onClick  = onChatClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                ActionCardWide(
                    title    = "Generate Quiz",
                    subtitle = "Test yourself on your uploaded material",
                    emoji    = "🎯",
                    color    = BrandViolet,
                    onClick  = onQuizClick
                )
            }
        }

        // ── Documents section ─────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Your Documents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(
                        onClick  = { refreshKey++ },
                        enabled  = !docsLoading
                    ) {
                        if (docsLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                color       = BrandTeal,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh docs", tint = BrandTeal)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (serverOnline == false) {
                    // Server offline banner
                    Card(
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WifiOff, null, tint = AccentRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Backend unreachable — upload and chat unavailable",
                                fontSize = 13.sp,
                                color    = AccentRed
                            )
                        }
                    }
                } else if (docsLoading) {
                    // Skeleton rows
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        )
                    }
                } else if (docs.isEmpty()) {
                    Card(
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📂", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No documents yet",
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary
                            )
                            Text("Upload a PDF to get started", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                } else {
                    docs.forEach { docName ->
                        Card(
                            shape    = RoundedCornerShape(12.dp),
                            colors   = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier          = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📄", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    docName,
                                    fontSize = 13.sp,
                                    color    = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── How it works ──────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("How It Works", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                HowItWorksCard("1", "Upload your notes", "Add any PDF — lecture slides, textbooks, or notes", BrandTeal)
                Spacer(Modifier.height(8.dp))
                HowItWorksCard("2", "Ask any question", "Type in plain English — AI finds the answer from your material", BrandBlue)
                Spacer(Modifier.height(8.dp))
                HowItWorksCard("3", "Practice with quizzes", "Auto-generated questions to test your understanding", BrandViolet)
            }
        }
    }
}

// ── Server status indicator ───────────────────────────────────────────────────

@Composable
fun ServerStatusDot(serverOnline: Boolean?) {
    val (dotColor, label) = when (serverOnline) {
        true  -> Pair(AccentGreen, "Server online")
        false -> Pair(AccentRed, "Server offline")
        null  -> Pair(AccentAmber, "Checking…")
    }
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
fun StatChip(label: String, icon: String) {
    Row(
        modifier              = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 13.sp)
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ActionCard(
    title:    String,
    subtitle: String,
    emoji:    String,
    color:    Color,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick   = onClick,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        modifier  = modifier.height(130.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 20.sp) }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ActionCardWide(title: String, subtitle: String, emoji: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = color),
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 36.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.White)
        }
    }
}

@Composable
fun HowItWorksCard(number: String, title: String, desc: String, color: Color) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(36.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center
            ) { Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Text(desc, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
