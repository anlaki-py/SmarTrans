package aki.tr.settings.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Material3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import aki.tr.ui.components.ExpressiveTextButton

/**
 * Confirmation dialog for deleting a language profile.
 *
 * @param languageName The display name shown in the confirmation message.
 * @param onConfirm Callback when the user confirms deletion.
 * @param onDismiss Callback when the user cancels.
 */
@Material3ExpressiveApi
@Composable
fun DeleteLanguageDialog(
    languageName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
        title = { Text("Delete Language") },
        text = { Text("Are you sure you want to remove \"$languageName\"?") },
        confirmButton = {
            ExpressiveTextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { ExpressiveTextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
