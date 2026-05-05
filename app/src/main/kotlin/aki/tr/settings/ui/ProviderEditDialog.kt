package aki.tr.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Material3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import aki.tr.provider.model.Provider
import aki.tr.provider.validation.EndpointValidation
import aki.tr.provider.validation.EndpointValidationResult
import aki.tr.ui.components.ExpressiveIconButton
import aki.tr.ui.components.ExpressiveTextButton

/**
 * Dialog for creating or editing a provider.
 * Validates the endpoint URL and allows managing multiple API keys.
 * For existing providers, can fetch available models from the /models endpoint
 * and select one via a searchable list.
 *
 * @param provider The provider to edit, or null for a new blank provider.
 * @param storedKeys List of existing API keys for this provider (shown masked).
 * @param fetchedModels List of model IDs fetched from the provider's /models endpoint.
 * @param isFetchingModels Whether a model fetch is currently in progress.
 * @param modelFetchError Error message from the last model fetch attempt, or null.
 * @param modelSearchQuery Current search query for filtering models.
 * @param onDismiss Callback when the dialog is dismissed.
 * @param onSave Callback with the updated provider and whether it is new.
 * @param onDelete Callback to delete the provider (hidden for new providers).
 * @param onAddKey Callback to add a new key.
 * @param onRemoveKey Callback to remove a stored key.
 * @param onFetchModels Callback to trigger fetching models from the provider.
 * @param onModelSearchQueryChange Callback when the model search query changes.
 * @param isCreating True if this is a new provider creation.
 */
@Material3ExpressiveApi
@Composable
fun ProviderEditDialog(
    provider: Provider?,
    storedKeys: List<String>,
    fetchedModels: List<String>,
    isFetchingModels: Boolean,
    modelFetchError: String?,
    modelSearchQuery: String,
    onDismiss: () -> Unit,
    onSave: (Provider, Boolean) -> Unit,
    onDelete: () -> Unit,
    onAddKey: (String) -> Unit,
    onRemoveKey: (String) -> Unit,
    onFetchModels: () -> Unit,
    onModelSearchQueryChange: (String) -> Unit,
    isCreating: Boolean
) {
    val target = provider ?: Provider(name = "", endpoint = "")
    var name by remember { mutableStateOf(target.name) }
    var endpoint by remember { mutableStateOf(target.endpoint) }
    var selectedModel by remember { mutableStateOf(target.selectedModel) }
    var endpointError by remember { mutableStateOf(false) }

    var newKey by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }

    var showModelPicker by remember { mutableStateOf(false) }

    val filteredModels = remember(fetchedModels, modelSearchQuery) {
        if (modelSearchQuery.isBlank()) fetchedModels
        else fetchedModels.filter { it.contains(modelSearchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreating) "New Provider" else "Edit Provider") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = if (showModelPicker && !isCreating) {
                    Modifier
                } else {
                    Modifier.verticalScroll(rememberScrollState())
                }
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

                // Model selection section
                if (showModelPicker && !isCreating) {
                    ModelPickerSection(
                        filteredModels = filteredModels,
                        isFetchingModels = isFetchingModels,
                        modelFetchError = modelFetchError,
                        modelSearchQuery = modelSearchQuery,
                        selectedModel = selectedModel,
                        onModelSelected = {
                            selectedModel = it
                            showModelPicker = false
                        },
                        onSearchQueryChange = onModelSearchQueryChange,
                        onClose = { showModelPicker = false }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { selectedModel = it },
                            label = { Text("Model ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (!isCreating) {
                            ExpressiveTextButton(
                                onClick = {
                                    if (!isFetchingModels) onFetchModels()
                                    showModelPicker = true
                                },
                                enabled = !isFetchingModels
                            ) {
                                if (isFetchingModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(8.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Fetch")
                                }
                            }
                        }
                    }
                    if (!isCreating && modelFetchError != null) {
                        Text(
                            text = modelFetchError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

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
                            ExpressiveIconButton(onClick = { onRemoveKey(key) }) {
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
                            ExpressiveTextButton(
                                onClick = {
                                    if (newKey.isBlank()) return@ExpressiveTextButton
                                    val trimmed = newKey.trim()
                                    if (storedKeys.contains(trimmed)) {
                                        keyError = "Key already added"
                                        return@ExpressiveTextButton
                                    }
                                    onAddKey(trimmed)
                                    newKey = ""
                                    keyError = null
                                },
                                enabled = newKey.isNotBlank(),
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text("Add") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        "Save the provider first, then edit it to add API keys and fetch models.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            ExpressiveTextButton(
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
                    ExpressiveTextButton(
                        onClick = onDelete
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                ExpressiveTextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/**
 * Section for picking a model from a fetched and filtered list.
 *
 * @param filteredModels The list of models filtered by search query.
 * @param isFetchingModels Whether models are currently being fetched.
 * @param modelFetchError Error message if fetching failed.
 * @param modelSearchQuery Current search query.
 * @param selectedModel The currently selected model ID.
 * @param onModelSelected Callback when a model is selected.
 * @param onSearchQueryChange Callback when search query changes.
 * @param onClose Callback to close the picker.
 */
@Composable
private fun ModelPickerSection(
    filteredModels: List<String>,
    isFetchingModels: Boolean,
    modelFetchError: String?,
    modelSearchQuery: String,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Select Model",
                style = MaterialTheme.typography.titleSmall
            )
            ExpressiveTextButton(onClick = onClose) { Text("Close") }
        }

        OutlinedTextField(
            value = modelSearchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search models") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        when {
            isFetchingModels -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }
            modelFetchError != null -> {
                Text(
                    text = modelFetchError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            filteredModels.isEmpty() && modelSearchQuery.isNotBlank() -> {
                Text(
                    text = "No models match your search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            filteredModels.isEmpty() -> {
                Text(
                    text = "No models fetched yet. Click Fetch to load models.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(top = 4.dp)
                ) {
                    items(filteredModels, key = { it }) { modelId ->
                        ListItem(
                            headlineContent = { Text(modelId) },
                            leadingContent = {
                                RadioButton(
                                    selected = modelId == selectedModel,
                                    onClick = { onModelSelected(modelId) }
                                )
                            },
                            modifier = Modifier.clickable { onModelSelected(modelId) }
                        )
                    }
                }
            }
        }
    }
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
