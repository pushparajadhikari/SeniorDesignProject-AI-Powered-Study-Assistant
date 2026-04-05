package com.example.aistudyassistant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.ui.theme.*
import kotlinx.coroutines.launch

// ── Data models ───────────────────────────────────────────────────────────────

data class ChatMessage(
    val text:    String,
    val isUser:  Boolean,
    val source:  String? = null   // e.g. "solar_system_overview.pdf — Page 2"
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit) {

    var inputText  by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(false) }
    val messages   = remember { mutableStateListOf<ChatMessage>(
        ChatMessage(
            text   = "Hi! I'm your AI study assistant. Upload a PDF and ask me anything about it — I'll find the answer from your own notes. 📚",
            isUser = false
        )
    ) }

    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()

    fun sendMessage() {
        val question = inputText.trim()
        if (question.isEmpty() || isLoading) return

        // Add user message
        messages.add(ChatMessage(text = question, isUser = true))
        inputText = ""
        isLoading = true

        scope.launch {
            listState.animateScrollToItem(messages.lastIndex)
        }

        // TODO: Replace this with real FastAPI call
        // val response = ApiService.ask(question)
        // Simulated response for now:
        scope.launch {
            kotlinx.coroutines.delay(1500)
            messages.add(
                ChatMessage(
                    text   = "This is where the AI answer will appear. Connect to the FastAPI backend at POST /ask to get real answers from your uploaded notes.",
                    isUser = false,
                    source = "Connect backend to see source"
                )
            )
            isLoading = false
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier         = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI Assistant", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Powered by your notes", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = SurfaceLight,
        bottomBar = {
            // ── Input bar ─────────────────────────────────────────────────
            Surface(
                shadowElevation = 8.dp,
                color           = Color.White
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        placeholder   = { Text("Ask anything about your notes…", fontSize = 14.sp) },
                        shape         = RoundedCornerShape(24.dp),
                        modifier      = Modifier.weight(1f),
                        maxLines      = 3,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BrandTeal,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick            = { sendMessage() },
                        containerColor     = if (inputText.isNotBlank()) BrandTeal else Color.LightGray,
                        contentColor       = Color.White,
                        modifier           = Modifier.size(48.dp),
                        shape              = CircleShape
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->

        LazyColumn(
            state          = listState,
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(messages) { msg ->
                ChatBubble(msg)
            }

            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier         = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BrandTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape  = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color    = BrandTeal,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Searching your notes…", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@Composable
fun ChatBubble(message: ChatMessage) {
    if (message.isUser) {
        // User bubble — right aligned
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                        .background(BrandTeal)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(message.text, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    } else {
        // AI bubble — left aligned
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BrandTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 14.sp)
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.widthIn(max = 280.dp)) {
                Card(
                    shape  = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Text(
                        message.text,
                        fontSize = 14.sp,
                        color    = TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                // Source citation
                if (!message.source.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "📄 ${message.source}",
                        fontSize = 11.sp,
                        color    = BrandBlue,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}