package aki.tr.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import aki.tr.config.model.LanguageProfile

/**
 * Dialog for creating or editing a language profile.
 *
 * @param language The language to edit, or null for a new blank profile.
 * @param onDismiss Callback when the dialog is dismissed.
 * @param onSave Callback with the updated language and whether it is new.
 * @param isCreating True if this is a new language creation.
 */
@Composable
fun LanguageEditDialog(
    language: LanguageProfile?,
    onDismiss: () -> Unit,
    onSave: (LanguageProfile, Boolean) -> Unit,
    isCreating: Boolean
) {
    val target = language ?: LanguageProfile(name = "", instruction = "")
    var name by remember { mutableStateOf(target.name) }
    var instruction by remember { mutableStateOf(target.instruction) }
    var isRtl by remember { mutableStateOf(target.isRtl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreating) "New Language" else "Edit Language") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("Instruction Prompt") }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isRtl = !isRtl }
                ) {
                    Checkbox(checked = isRtl, onCheckedChange = { isRtl = it })
                    Text("Right-to-Left (RTL) Script")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(target.copy(name = name, instruction = instruction, isRtl = isRtl), isCreating)
                },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
