package aki.tr.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import aki.tr.provider.model.Provider
import aki.tr.provider.validation.EndpointValidation
import aki.tr.provider.validation.EndpointValidationResult
import aki.tr.provider.validation.KeyValidationResult
import kotlinx.coroutines.launch

/**
 * Dialog for creating or editing a provider.
 * Validates the endpoint URL and allows managing multiple API keys.
 *
 * @param provider The provider to edit, or null for a new blank provider.
 * @param storedKeys List of existing API keys for this provider (shown masked).
 * @param onDismiss Callback when the dialog is dismissed.
 * @param onSave Callback with the updated provider and whether it is new.
 * @param onDelete Callback to delete the provider (hidden for new providers).
 * @param onAddKey Callback to validate and add a new key.
 * @param onRemoveKey Callback to remove a stored key.
 * @param isCreating True if this is a new provider creation.
 */
@Composable
fun ProviderEditDialog(
    provider: Provider?,
    storedKeys: List<String>,
    onDismiss: () -> Unit,
    onSave: (Provider, Boolean) -> Unit,
    onDelete: () -> Unit,
    onAddKey: suspend (String) -> KeyValidationResult,
    onRemoveKey: (String) -> Unit,
    isCreating: Boolean
) {
    val target = provider ?: Provider(name = "", endpoint = "")
    var name by remember { mutableStateOf(target.name) }
    var endpoint by remember { mutableStateOf(target.endpoint) }
    var selectedModel by remember { mutableStateOf(target.selectedModel) }
    var endpointError by remember { mutableStateOf(false) }

    var newKey by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }
    var isValidating by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreating) "New Provider" else "Edit Provider") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                        endpointError = EndpointValidation.validate(it) is EndpointValidationResult.Error
                    },
                    label = { Text("Base URL") },
                    singleLine = true,
                    isError = endpointError,
                    supportingText = if (endpointError) {
                        { Text("Must be HTTPS (or HTTP for localhost)") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = { selectedModel = it },
                    label = { Text("Model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // API Keys section — only for existing providers
                if (!isCreating) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        "API Keys (${storedKeys.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    storedKeys.forEach { key ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = maskKey(key),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveKey(key) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove key",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = {
                            newKey = it
                            keyError = null
                        },
                        label = { Text("New API Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = keyError != null,
                        supportingText = keyError?.let { { Text(it) } },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    if (newKey.isBlank()) return@TextButton
                                    if (EndpointValidation.isSafeToSave(endpoint).not()) {
                                        keyError = "Enter a valid endpoint first"
                                        return@TextButton
                                    }
                                    isValidating = true
                                    scope.launch {
                                        val result = onAddKey(newKey.trim())
                                        isValidating = false
                                        when (result) {
                                            is KeyValidationResult.Valid -> {
                                                newKey = ""
                                                keyError = null
                                            }
                                            is KeyValidationResult.Duplicate -> {
                                                keyError = "Key already added"
                                            }
                                            is KeyValidationResult.Invalid -> {
                                                keyError = result.message
                                            }
                                        }
                                    }
                                },
                                enabled = newKey.isNotBlank() && !isValidating,
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text("Add") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        "Save the provider first, then edit it to add API keys.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

/**
 * Masks an API key for display, showing only the last 4 characters.
 *
 * @param key The raw API key.
 * @return Masked string like "****abcd".
 */
private fun maskKey(key: String): String {
    if (key.length <= 4) return "****"
    return "****" + key.takeLast(4)
}
