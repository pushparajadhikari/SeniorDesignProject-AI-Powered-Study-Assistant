package com.example.aistudyassistant.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.auth.UserManager
import com.example.aistudyassistant.network.ApiService
import com.example.aistudyassistant.ui.components.BrandLogoMark
import com.example.aistudyassistant.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Upload state machine
private sealed class UploadState {
    object Idle      : UploadState()
    object Uploading : UploadState()
    data class Success(val message: String) : UploadState()
    data class Error(val message: String)   : UploadState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPdfScreen(onBack: () -> Unit) {

    val context          = LocalContext.current
    val scope            = rememberCoroutineScope()
    // Server-assigned user id (null if registered offline) — tags the upload as this user's.
    val serverId         = remember { UserManager.getCurrentSession(context)?.serverId }
    var selectedUri      by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var uploadState      by remember { mutableStateOf<UploadState>(UploadState.Idle) }

    // Indexing-progress polling state (0–100). isPolling drives the progress bar's visibility.
    var uploadProgress   by remember { mutableStateOf(0) }
    var isPolling        by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri      = uri
            selectedFileName = resolveDisplayName(context, uri)
            uploadState      = UploadState.Idle
        }
    }

    // Poll GET /upload-progress/{filename} every 2 s, stopping when the backend
    // reports complete=true or after 10 attempts.
    fun pollUploadProgress() {
        isPolling      = true
        uploadProgress = 0
        scope.launch {
            repeat(10) {
                val (percent, isComplete) = ApiService.getUploadProgress(selectedFileName)
                uploadProgress = percent
                if (isComplete) {
                    uploadProgress = 100
                    isPolling      = false
                    return@launch
                }
                delay(2000)
            }
            isPolling = false
        }
    }

    fun doUpload() {
        val uri = selectedUri ?: return
        uploadState = UploadState.Uploading
        scope.launch {
            ApiService.uploadPdf(context, uri, selectedFileName, serverId)
                .onSuccess { msg ->
                    uploadState = UploadState.Success(msg)
                    pollUploadProgress()
                }
                .onFailure { err -> uploadState = UploadState.Error(err.message ?: "Upload failed") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogoMark(size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Upload Material", fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(16.dp))

            // ── Drop zone ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selectedUri != null) BrandTeal.copy(alpha = 0.08f) else Color.White
                    )
                    .border(
                        width = 2.dp,
                        brush = if (selectedUri != null)
                            Brush.linearGradient(listOf(BrandTeal, BrandBlue))
                        else
                            Brush.linearGradient(listOf(Color.LightGray, Color.LightGray)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (selectedUri == null) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint               = BrandTeal,
                            modifier           = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Tap to select a PDF",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp,
                            color      = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Supports PDF files up to 50 MB", fontSize = 13.sp, color = TextSecondary)
                    } else {
                        Icon(
                            Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint               = BrandTeal,
                            modifier           = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            selectedFileName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Tap 'Change' to pick another file", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Select / Change button ────────────────────────────────────
            OutlinedButton(
                onClick  = { pdfPickerLauncher.launch("application/pdf") },
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (selectedUri == null) "Select PDF" else "Change File", color = BrandBlue)
            }

            Spacer(Modifier.height(12.dp))

            // ── Upload button ─────────────────────────────────────────────
            val isUploading = uploadState is UploadState.Uploading
            Button(
                onClick  = { doUpload() },
                enabled  = selectedUri != null && !isUploading,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Indexing your notes…", color = Color.White, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload & Index", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            // ── Indexing progress bar ─────────────────────────────────────
            AnimatedVisibility(visible = isPolling || uploadProgress > 0) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Indexing progress", fontSize = 13.sp, color = TextSecondary)
                        Text("$uploadProgress%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandTeal)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color    = BrandTeal,
                        trackColor = BrandTeal.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Status cards ──────────────────────────────────────────────

            AnimatedVisibility(visible = uploadState is UploadState.Success) {
                val msg = (uploadState as? UploadState.Success)?.message ?: ""
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Successfully indexed!", fontWeight = FontWeight.SemiBold, color = AccentGreen)
                            Text(msg, fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uploadState is UploadState.Error) {
                val msg = (uploadState as? UploadState.Error)?.message ?: ""
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Upload failed", fontWeight = FontWeight.SemiBold, color = AccentRed)
                            Text(msg, fontSize = 12.sp, color = TextSecondary)
                        }
                        TextButton(onClick = { doUpload() }) {
                            Text("Retry", color = BrandTeal)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Tips section ──────────────────────────────────────────────
            Card(
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tips for best results", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    TipRow("📝", "Use PDFs with selectable text, not scanned images")
                    TipRow("📊", "Diagrams and charts are described by our vision AI")
                    TipRow("📚", "Upload multiple PDFs to query across all of them")
                    TipRow("⏳", "Indexing can take a minute for large files — please wait")
                }
            }
        }
    }
}

@Composable
private fun TipRow(emoji: String, text: String) {
    Row(
        modifier          = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = TextSecondary)
    }
}

/**
 * Resolves a picked content:// Uri to its real display name (e.g. "lecture1.pdf") via
 * the content resolver's DISPLAY_NAME column — [Uri.lastPathSegment] returns an opaque
 * path fragment like "document:1000001067" for SAF Uris, not a filename, and was
 * previously (wrongly) used directly as the uploaded document's stored name.
 */
private fun resolveDisplayName(context: Context, uri: Uri): String {
    val resolved = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: "document.pdf"
    return if (resolved.endsWith(".pdf", ignoreCase = true)) resolved else "$resolved.pdf"
}
