package aki.tr.translator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import aki.tr.provider.model.Provider
import aki.tr.translator.viewmodel.TranslatorUiState

/**
 * Bottom sheet for selecting an AI provider from the configured list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderBottomSheet(
    state: TranslatorUiState,
    onDismiss: () -> Unit,
    onSelect: (Provider) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(bottom = 48.dp)) {
            Text(
                "Select Provider",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
            )
            HorizontalDivider()
            LazyColumn {
                items(
                    items = state.providers,
                    key = { it.id }
                ) { provider ->
                    ListItem(
                        headlineContent = { Text(provider.name) },
                        leadingContent = {
                            if (provider.id == state.config.selectedProviderId) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Spacer(Modifier.size(24.dp))
                            }
                        },
                        modifier = Modifier.clickable { onSelect(provider) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}
