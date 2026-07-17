package com.example.aistudyassistant.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.aistudyassistant.ui.theme.AccentRed

/**
 * Shared destructive-action confirmation dialog — same shape as Profile's sign-out
 * dialog, reused so every delete (PDF, quiz, flashcard set) confirms the same way.
 */
@Composable
fun DestructiveConfirmDialog(
    title:        String,
    message:      String,
    confirmLabel: String = "Delete",
    onConfirm:    () -> Unit,
    onDismiss:    () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(title) },
        text             = { Text(message) },
        confirmButton    = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = AccentRed) } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
