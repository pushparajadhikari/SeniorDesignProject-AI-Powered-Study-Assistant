package com.example.aistudyassistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.ui.components.BrandLogoMark
import com.example.aistudyassistant.ui.theme.*

// ── Sub-screen enum ───────────────────────────────────────────────────────────

private enum class ProfileSubScreen { NONE, CHANGE_PASSWORD, PRIVACY, ABOUT, HELP }

// ── Main Profile Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack:         () -> Unit,
    onLogout:       () -> Unit,
    showBackButton: Boolean = true
) {
    val context = LocalContext.current
    val session = UserManager.getCurrentSession(context)

    var subScreen       by remember { mutableStateOf(ProfileSubScreen.NONE) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Sub-screens
    when (subScreen) {
        ProfileSubScreen.CHANGE_PASSWORD -> {
            ChangePasswordScreen(onBack = { subScreen = ProfileSubScreen.NONE })
            return
        }
        ProfileSubScreen.PRIVACY -> {
            InfoScreen(
                title   = "Privacy Policy",
                onBack  = { subScreen = ProfileSubScreen.NONE },
                content = privacyPolicyText
            )
            return
        }
        ProfileSubScreen.ABOUT -> {
            InfoScreen(
                title   = "About StudyAI",
                onBack  = { subScreen = ProfileSubScreen.NONE },
                content = aboutText
            )
            return
        }
        ProfileSubScreen.HELP -> {
            InfoScreen(
                title   = "Help & FAQ",
                onBack  = { subScreen = ProfileSubScreen.NONE },
                content = helpText
            )
            return
        }
        ProfileSubScreen.NONE -> { /* show main profile */ }
    }

    // ── Logout confirmation dialog ────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title            = { Text("Sign Out") },
            text             = { Text("Are you sure you want to sign out?") },
            confirmButton    = {
                TextButton(onClick = {
                    UserManager.logout(context)
                    onLogout()
                }) {
                    Text("Sign Out", color = AccentRed)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogoMark(size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Profile", fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = SurfaceLight
    ) { padding ->

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Avatar + name card ────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(GradientStart, GradientMid))
                        )
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // Avatar circle with initials
                        Box(
                            modifier         = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = session?.userName?.take(1)?.uppercase() ?: "?",
                                fontSize   = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            session?.userName ?: "Student",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            session?.userEmail ?: "",
                            fontSize = 14.sp,
                            color    = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ── Account section ───────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Account")
            }

            item {
                ProfileCard {
                    ProfileRow(
                        icon    = Icons.Default.Lock,
                        label   = "Change Password",
                        color   = BrandTeal,
                        onClick = { subScreen = ProfileSubScreen.CHANGE_PASSWORD }
                    )
                }
            }

            // ── App section ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("App")
            }

            item {
                ProfileCard {
                    ProfileRow(
                        icon    = Icons.Default.Shield,
                        label   = "Privacy Policy",
                        color   = BrandBlue,
                        onClick = { subScreen = ProfileSubScreen.PRIVACY }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFF0F0F0))
                    ProfileRow(
                        icon    = Icons.Default.Info,
                        label   = "About StudyAI",
                        color   = BrandViolet,
                        onClick = { subScreen = ProfileSubScreen.ABOUT }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = Color(0xFFF0F0F0))
                    ProfileRow(
                        icon    = Icons.AutoMirrored.Filled.Help,
                        label   = "Help & FAQ",
                        color   = AccentAmber,
                        onClick = { subScreen = ProfileSubScreen.HELP }
                    )
                }
            }

            // ── Danger zone ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedButton(
                        onClick  = { showLogoutDialog = true },
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = AccentRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out", color = AccentRed, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "StudyAI v1.0  ·  CSE 4316 Senior Design",
                    fontSize  = 11.sp,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Change Password Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = UserManager.getCurrentSession(context)

    var currentPassword by remember { mutableStateOf("") }
    var newPassword     by remember { mutableStateOf("") }
    var confirmNew      by remember { mutableStateOf("") }
    var errorMessage    by remember { mutableStateOf("") }
    var successMessage  by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SurfaceLight
    ) { padding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text("Current Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value                = currentPassword,
                        onValueChange        = { currentPassword = it; errorMessage = ""; successMessage = "" },
                        visualTransformation = PasswordVisualTransformation(),
                        shape                = RoundedCornerShape(12.dp),
                        singleLine           = true,
                        modifier             = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("New Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value                = newPassword,
                        onValueChange        = { newPassword = it; errorMessage = ""; successMessage = "" },
                        placeholder          = { Text("Min. 8 characters") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape                = RoundedCornerShape(12.dp),
                        singleLine           = true,
                        modifier             = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Text("Confirm New Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value                = confirmNew,
                        onValueChange        = { confirmNew = it; errorMessage = ""; successMessage = "" },
                        visualTransformation = PasswordVisualTransformation(),
                        shape                = RoundedCornerShape(12.dp),
                        singleLine           = true,
                        modifier             = Modifier.fillMaxWidth()
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(errorMessage, color = AccentRed, fontSize = 13.sp)
                    }

                    if (successMessage.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(successMessage, color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (newPassword != confirmNew) {
                                errorMessage = "New passwords do not match"
                                return@Button
                            }
                            val error = UserManager.changePassword(
                                context, session?.userId ?: "", currentPassword, newPassword
                            )
                            if (error == null) {
                                successMessage  = "✓ Password changed successfully"
                                currentPassword = ""
                                newPassword     = ""
                                confirmNew      = ""
                            } else {
                                errorMessage = error
                            }
                        },
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Update Password", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Generic Info Screen (Privacy / About / Help) ──────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(title: String, content: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SurfaceLight
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = content,
                        fontSize = 14.sp,
                        color    = TextPrimary,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}

// ── Small helper composables ──────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text     = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color    = TextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun ProfileRow(
    icon:    ImageVector,
    label:   String,
    color:   Color,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
    }
}

// ── Static content strings ────────────────────────────────────────────────────

private val privacyPolicyText = """
Privacy Policy — StudyAI
Last updated: April 2026

1. Data We Collect
StudyAI stores your account information (name, email, and a hashed version of your password) locally on your device. We do not transmit your personal information to any external server.

2. Documents You Upload
PDF documents you upload are processed by the AI backend running on your own configured server. Document content is converted to vector embeddings and stored locally on that server. We do not send your documents to any third-party cloud service.

3. How We Store Your Data
Account data is stored in your device's local SharedPreferences storage. This data is private to your device and is not accessible to other apps or users.

4. Data Security
Passwords are never stored in plain text. We use SHA-256 hashing before storing any password. Your documents are processed only on your own server via an encrypted Tailscale VPN connection.

5. Data Deletion
You can delete your account and all associated data by uninstalling the application. Document embeddings on the server can be cleared by deleting the chroma_db folder.

6. Third-Party Services
When using the OpenAI API option for demonstrations, only the assembled prompt text is sent to OpenAI — your original documents are never transmitted externally. Please refer to OpenAI's privacy policy for their data handling practices.

7. Contact
This is an academic project developed at the University of Texas at Arlington for CSE 4316. For questions, contact the development team through the university.
""".trimIndent()

private val aboutText = """
About StudyAI

StudyAI is an AI-powered mobile study assistant developed as a Senior Design Project at the University of Texas at Arlington.

Project: CSE 4316 / CSE 4317 — Senior Design I & II
Semester: Spring / Summer 2026
Institution: University of Texas at Arlington
Department: Computer Science & Engineering

Development Team:
• Binod Tiwari — Backend Lead & AI Integration
• Saujan Parajuli — Backend Support & Sprint Coordination  
• Pushpa Raj Adhikari — UI/UX Design & Android Frontend

How It Works:
StudyAI uses a technique called Retrieval-Augmented Generation (RAG). When you upload a PDF, the system breaks it into small chunks, converts them into mathematical representations called embeddings, and stores them in a local vector database (ChromaDB). When you ask a question, the system finds the most relevant chunks and passes them to an AI language model to generate a precise, context-aware answer from your own material.

AI Models Used:
• nomic-embed-text — converts text to vectors
• moondream — understands diagrams and images
• llama3.2:1b — generates answers from your notes

Version: 1.0.0
""".trimIndent()

private val helpText = """
Help & Frequently Asked Questions

Q: How do I upload my notes?
A: Tap "Upload PDF" on the dashboard, then select a PDF file from your device. The system will process and index it automatically.

Q: Why is it taking long to process my PDF?
A: First-time indexing embeds every chunk of your document using an AI model, which takes time on CPU-only hardware. Subsequent runs are much faster because results are cached.

Q: Can I upload multiple PDFs?
A: Yes. Upload as many PDFs as you need. Questions will draw answers from across all your uploaded documents.

Q: Why does the answer sometimes say "not found in your notes"?
A: The AI first searches your uploaded material. If the specific information is not in your documents, it will clearly say so and provide an answer from its general knowledge instead.

Q: Can other users see my documents?
A: No. Each account has its own document storage. Documents uploaded under your account are only visible when you are logged in.

Q: I forgot my password. What do I do?
A: Currently, password recovery requires creating a new account. A password recovery feature via email is planned for a future version.

Q: What file types are supported?
A: Currently only PDF files are supported. Support for Word documents and plain text files is planned.

Q: The app can't connect to the AI server. What do I do?
A: Make sure the backend server is running and that Tailscale is active on both your phone and the server. Check that the server IP in the app configuration matches your Tailscale IP.

Q: How do I generate a quiz?
A: Tap "Generate Quiz" on the dashboard. The AI will create multiple choice questions based on your uploaded material.

Still need help? Contact your team member or raise an issue on the project GitHub repository.
""".trimIndent()