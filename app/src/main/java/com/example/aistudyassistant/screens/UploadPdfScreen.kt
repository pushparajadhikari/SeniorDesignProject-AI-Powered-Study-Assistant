package com.example.aistudyassistant.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPdfScreen(onBack: () -> Unit) {

    var selectedUri      by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var uploadStatus     by remember { mutableStateOf<String?>(null) } // null | "uploading" | "done" | "error"

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri      = uri
            selectedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "document.pdf"
            uploadStatus     = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Upload Material", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
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
                        if (selectedUri != null)
                            BrandTeal.copy(alpha = 0.08f)
                        else
                            Color.White
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
                        Text(
                            "Supports PDF files up to 50MB",
                            fontSize = 13.sp,
                            color    = TextSecondary
                        )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    if (selectedUri == null) "Select PDF" else "Change File",
                    color = BrandBlue
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Upload button ─────────────────────────────────────────────
            Button(
                onClick  = {
                    if (selectedUri != null) {
                        uploadStatus = "uploading"
                        // TODO: call FastAPI /upload endpoint here
                        // For now simulate success
                        uploadStatus = "done"
                    }
                },
                enabled  = selectedUri != null && uploadStatus != "uploading",
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uploadStatus == "uploading") {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Indexing your notes...", color = Color.White)
                } else {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload & Index", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            // ── Success state ─────────────────────────────────────────────
            if (uploadStatus == "done") {
                Spacer(Modifier.height(20.dp))
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
                            Text("Your notes are ready to query.", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Tips section ──────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tips for best results", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    TipRow("📝", "Use PDFs with selectable text, not scanned images")
                    TipRow("📊", "Diagrams and charts are described by our vision AI")
                    TipRow("📚", "Upload multiple PDFs to query across all of them")
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