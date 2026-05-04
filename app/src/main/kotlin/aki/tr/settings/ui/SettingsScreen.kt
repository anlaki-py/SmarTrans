package aki.tr.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aki.tr.config.model.LanguageProfile
import aki.tr.provider.model.Provider
import aki.tr.settings.viewmodel.SettingsViewModel
import aki.tr.ui.components.SectionHeader

/**
 * Settings screen with provider CRUD and language profile CRUD.
 * Each section is kept small; dialogs and dismiss backgrounds are in their own files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var editingProvider by remember { mutableStateOf<Provider?>(null) }
    var creatingNewProvider by remember { mutableStateOf(false) }
    var editingLanguage by remember { mutableStateOf<LanguageProfile?>(null) }
    var creatingNewLanguage by remember { mutableStateOf(false) }
    var languageToDelete by remember { mutableStateOf<LanguageProfile?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            // Providers section
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("PROVIDERS", noPadding = true)
                    TextButton(onClick = { creatingNewProvider = true }) { Text("Add New") }
                }
            }
            items(items = state.providers, key = { it.id }) { provider ->
                ListItem(
                    headlineContent = { Text(provider.name, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(provider.endpoint, maxLines = 1) },
                    leadingContent = {
                        RadioButton(
                            selected = provider.id == state.config.selectedProviderId,
                            onClick = {
                                viewModel.saveConfig(state.config.copy(selectedProviderId = provider.id))
                            }
                        )
                    },
                    modifier = Modifier.clickable { editingProvider = provider }
                )
            }

            // Languages section
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("LANGUAGES", noPadding = true)
                    TextButton(onClick = { creatingNewLanguage = true }) { Text("Add New") }
                }
            }
            items(items = state.config.languages, key = { it.id }) { lang ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            languageToDelete = lang
                            false
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = { DismissBackground(dismissState.targetValue) },
                    content = {
                        ListItem(
                            headlineContent = { Text(lang.name) },
                            trailingContent = {
                                if (lang.isRtl) {
                                    Text("RTL", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            modifier = Modifier.clickable { editingLanguage = lang },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                )
            }
        }
    }

    // Dialogs
    languageToDelete?.let { lang ->
        DeleteLanguageDialog(
            languageName = lang.name,
            onConfirm = { viewModel.deleteLanguage(lang.id); languageToDelete = null },
            onDismiss = { languageToDelete = null }
        )
    }

    if (editingProvider != null || creatingNewProvider) {
        val providerId = editingProvider?.id ?: ""
        ProviderEditDialog(
            provider = editingProvider,
            storedKeys = state.providerKeys[providerId] ?: emptyList(),
            onDismiss = { editingProvider = null; creatingNewProvider = false },
            onSave = { newProvider, isNew ->
                viewModel.saveProvider(newProvider, isNew)
                editingProvider = null; creatingNewProvider = false
            },
            onDelete = { editingProvider?.let { viewModel.deleteProvider(it.id) }; editingProvider = null },
            onAddKey = { key ->
                viewModel.addKey(providerId, key)
            },
            onRemoveKey = { key -> viewModel.removeKey(providerId, key) },
            isCreating = creatingNewProvider
        )
    }

    if (editingLanguage != null || creatingNewLanguage) {
        LanguageEditDialog(
            language = editingLanguage,
            onDismiss = { editingLanguage = null; creatingNewLanguage = false },
            onSave = { newLang, isNew ->
                val list = if (isNew) state.config.languages + newLang
                else state.config.languages.map { if (it.id == newLang.id) newLang else it }
                viewModel.saveConfig(state.config.copy(languages = list))
                editingLanguage = null; creatingNewLanguage = false
            },
            isCreating = creatingNewLanguage
        )
    }
}
