package aki.tr.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import aki.tr.provider.validation.EndpointValidation
import aki.tr.provider.model.Provider

/**
 * Dialog for creating or editing a provider.
 * Validates the endpoint URL against HTTPS/localhost rules.
 *
 * @param provider The provider to edit, or null for a new blank provider.
 * @param onDismiss Callback when the dialog is dismissed.
 * @param onSave Callback with the updated provider and whether it is new.
 * @param onDelete Callback to delete the provider (hidden for new providers).
 * @param isCreating True if this is a new provider creation.
 */
@Composable
fun ProviderEditDialog(
    provider: Provider?,
    onDismiss: () -> Unit,
    onSave: (Provider, Boolean) -> Unit,
    onDelete: () -> Unit,
    isCreating: Boolean
) {
    val target = provider ?: Provider(name = "", endpoint = "")
    var name by remember { mutableStateOf(target.name) }
    var endpoint by remember { mutableStateOf(target.endpoint) }
    var selectedModel by remember { mutableStateOf(target.selectedModel) }
    var endpointError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreating) "New Provider" else "Edit Provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                        endpointError = EndpointValidation.validate(it) is EndpointValidation.Error
                    },
                    label = { Text("Base URL") },
                    singleLine = true,
                    isError = endpointError,
                    supportingText = if (endpointError) {
                        { Text("Must be HTTPS (or HTTP for localhost)") }
                    } else null
                )
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = { selectedModel = it },
                    label = { Text("Model ID") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (EndpointValidation.isSafeToSave(endpoint)) {
                        onSave(
                            target.copy(name = name, endpoint = endpoint, selectedModel = selectedModel),
                            isCreating
                        )
                    }
                },
                enabled = name.isNotBlank() && endpoint.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isCreating) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
