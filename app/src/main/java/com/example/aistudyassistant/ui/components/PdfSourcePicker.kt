package com.example.aistudyassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistudyassistant.models.DocumentEntry
import com.example.aistudyassistant.network.ApiService
import com.example.aistudyassistant.ui.animation.ShimmerBox
import com.example.aistudyassistant.ui.animation.pressScale
import com.example.aistudyassistant.ui.theme.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** A chosen quiz/flashcard generation source: every uploaded document, or one specific file. */
sealed class PdfSource {
    object AllDocuments : PdfSource()
    data class Specific(val filename: String) : PdfSource()
}

/**
 * Shared PDF-source picker used by both the quiz and flashcard generation flows —
 * built once so the two never drift out of sync. Fetches this user's documents via
 * GET /docs-list, newest first, and lets them pick one or "All documents" to keep
 * today's whole-library behaviour (sends no source_pdf).
 */
@Composable
fun PdfSourcePicker(
    userId:        Int?,
    selected:      PdfSource?,
    onSelect:      (PdfSource) -> Unit,
    onUploadClick: () -> Unit,
    modifier:      Modifier = Modifier,
    accentColor:   Color    = BrandTeal,
    refreshKey:    Int      = 0
) {
    var isLoading    by remember { mutableStateOf(true) }
    var documents    by remember { mutableStateOf<List<DocumentEntry>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryKey     by remember { mutableStateOf(0) }

    LaunchedEffect(userId, refreshKey, retryKey) {
        isLoading    = true
        errorMessage = null
        ApiService.getDocumentsList(userId)
            .onSuccess { docs ->
                documents = docs.sortedByDescending { it.timestamp }
                isLoading = false
            }
            .onFailure { err ->
                errorMessage = err.message ?: "Could not load your documents"
                isLoading    = false
            }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        Text("Choose a source", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        // "All documents" doesn't depend on the fetch below, so it's always available.
        PdfSourceRow(
            title       = "All documents",
            subtitle    = "Draw from everything you've uploaded",
            icon        = Icons.Default.Description,
            isSelected  = selected == PdfSource.AllDocuments,
            accentColor = accentColor,
            onClick     = { onSelect(PdfSource.AllDocuments) }
        )

        Spacer(Modifier.height(8.dp))

        when {
            errorMessage != null -> PdfPickerError(errorMessage!!, accentColor) { retryKey++ }
            isLoading            -> PdfPickerLoading()
            documents.isEmpty()  -> PdfPickerEmpty(accentColor, onUploadClick)
            else -> Column {
                documents.forEach { doc ->
                    PdfSourceRow(
                        title       = doc.filename,
                        subtitle    = formatUploadDate(doc.timestamp),
                        icon        = Icons.Default.Description,
                        isSelected  = selected is PdfSource.Specific && selected.filename == doc.filename,
                        accentColor = accentColor,
                        onClick     = { onSelect(PdfSource.Specific(doc.filename)) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PdfSourceRow(
    title:       String,
    subtitle:    String,
    icon:        ImageVector,
    isSelected:  Boolean,
    accentColor: Color,
    onClick:     () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.08f) else Color.White
        ),
        border   = if (isSelected) BorderStroke(1.5.dp, accentColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .pressScale(interactionSource)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = TextSecondary)
                }
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PdfPickerLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(2) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(58.dp))
        }
    }
}

@Composable
private fun PdfPickerEmpty(accentColor: Color, onUploadClick: () -> Unit) {
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📂", fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text("Upload a PDF first", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "You need at least one document before generating from it",
                fontSize  = 13.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUploadClick,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(Icons.Default.UploadFile, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Upload a PDF", color = Color.White)
            }
        }
    }
}

@Composable
private fun PdfPickerError(message: String, accentColor: Color, onRetry: () -> Unit) {
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠️", fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text("Could not load your documents", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry", color = accentColor)
            }
        }
    }
}

/** Parses a backend ISO-8601 timestamp into a locale-formatted date; falls back to the raw string. */
fun formatUploadDate(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return ""
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )
    for (pattern in patterns) {
        try {
            val parsed = SimpleDateFormat(pattern, Locale.US).parse(isoTimestamp)
            if (parsed != null) {
                return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(parsed)
            }
        } catch (_: Exception) {
            // try the next pattern
        }
    }
    return isoTimestamp
}
