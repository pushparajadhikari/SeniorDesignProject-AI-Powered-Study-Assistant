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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.network.ApiService
import com.example.aistudyassistant.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

// ── Data models ───────────────────────────────────────────────────────────────

data class ChatMessage(
    val text:    String,
    val isUser:  Boolean,
    val source:  String? = null,   // e.g. "solar_system.pdf — Page 2"
    val isError: Boolean = false
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit, onHistory: () -> Unit = {}, showBackButton: Boolean = true) {

    val context = LocalContext.current
    // Server-assigned user id (null if offline) — lets the backend scope chat to this user's docs.
    val serverId = remember { UserManager.getCurrentSession(context)?.serverId }

    // Unique session ID for this chat session — enables multi-turn conversation memory on the backend.
    // A var so "clear session" can generate a fresh UUID and start a brand-new conversation.
    var sessionId by remember { mutableStateOf(UUID.randomUUID().toString()) }

    // The greeting the chat always opens with — also used to reset the list on clear.
    val welcomeMessage = ChatMessage(
        text   = "Hi! I'm your AI study assistant. Upload a PDF and ask me anything about it — I'll find the answer from your own notes. 📚",
        isUser = false
    )

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val messages  = remember { mutableStateListOf(welcomeMessage) }

    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    fun sendMessage() {
        val question = inputText.trim()
        if (question.isEmpty() || isLoading) return

        messages.add(ChatMessage(text = question, isUser = true))
        inputText = ""
        isLoading = true

        scope.launch {
            listState.animateScrollToItem(messages.lastIndex)

            ApiService.chat(question, sessionId, serverId)
                .onSuccess { (answer, source) ->
                    messages.add(ChatMessage(text = answer, isUser = false, source = source))
                }
                .onFailure { err ->
                    messages.add(
                        ChatMessage(
                            text    = "Could not reach the server. Is it running?\n${err.message ?: ""}",
                            isUser  = false,
                            isError = true
                        )
                    )
                }

            isLoading = false
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun clearSession() {
        val oldSession = sessionId
        // Reset the conversation locally first so the UI feels instant.
        messages.clear()
        messages.add(welcomeMessage)
        // Start a fresh session so the backend treats the next question as a new conversation.
        sessionId = UUID.randomUUID().toString()
        scope.launch {
            ApiService.clearSession(oldSession)
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
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onHistory) {
                        Icon(Icons.Default.History, "History")
                    }
                    IconButton(onClick = { clearSession() }) {
                        Icon(Icons.Default.Delete, "Clear session")
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
            Surface(shadowElevation = 8.dp, color = Color.White) {
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
                        onClick        = { sendMessage() },
                        containerColor = if (inputText.isNotBlank() && !isLoading) BrandTeal else Color.LightGray,
                        contentColor   = Color.White,
                        modifier       = Modifier.size(48.dp),
                        shape          = CircleShape
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->

        LazyColumn(
            state               = listState,
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding      = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(messages) { msg ->
                ChatBubble(msg)
            }

            // Typing indicator while waiting for response
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
                                    color       = BrandTeal,
                                    modifier    = Modifier.size(16.dp),
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
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
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
    } else {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (message.isError) AccentRed.copy(alpha = 0.15f)
                        else BrandTeal.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (message.isError) "⚠️" else "🤖", fontSize = 14.sp)
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.widthIn(max = 280.dp)) {
                Card(
                    shape  = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.isError) AccentRed.copy(alpha = 0.08f) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Text(
                        text     = parseSimpleMarkdown(message.text),
                        fontSize = 14.sp,
                        color    = if (message.isError) AccentRed else TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

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

// ── Lightweight markdown rendering ──────────────────────────────────────────────

/**
 * Renders a small subset of markdown as an [AnnotatedString] — no external library.
 * Supported:
 *   • **bold text**                       → bold span
 *   • lines starting with "- " or "* "    → indented bullet point ("  • …")
 *   • "[From your notes]:" / "[From general knowledge]:" line prefixes
 *                                         → prefix in BrandTeal bold, rest normal
 *   • line breaks are preserved
 */
private val boldRegex = Regex("""\*\*(.+?)\*\*""")
private val sourcePrefixes = listOf("[From your notes]:", "[From general knowledge]:")

fun parseSimpleMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.split("\n")
    lines.forEachIndexed { index, rawLine ->
        var line = rawLine

        // Source-attribution prefixes: teal bold prefix, normal remainder.
        val matchedPrefix = sourcePrefixes.firstOrNull { line.trimStart().startsWith(it) }
        if (matchedPrefix != null) {
            val leading = line.takeWhile { it == ' ' || it == '\t' }
            if (leading.isNotEmpty()) append(leading)
            withStyle(SpanStyle(color = BrandTeal, fontWeight = FontWeight.Bold)) {
                append(matchedPrefix)
            }
            val rest = line.trimStart().removePrefix(matchedPrefix)
            appendInlineBold(rest)
        } else {
            // Bullet points: "- " or "* " → indented "  • ".
            val trimmed = line.trimStart()
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                append("  • ")
                appendInlineBold(trimmed.substring(2))
            } else {
                appendInlineBold(line)
            }
        }

        if (index != lines.lastIndex) append("\n")
    }
}

/** Appends [text], converting `**bold**` runs into bold spans. */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineBold(text: String) {
    var lastIndex = 0
    for (match in boldRegex.findAll(text)) {
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}
