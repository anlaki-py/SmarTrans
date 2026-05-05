package aki.tr.translator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.Material3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import aki.tr.config.model.LanguageProfile
import aki.tr.translator.viewmodel.TranslatorUiState
import aki.tr.ui.components.ExpressiveButton
import aki.tr.ui.components.ExpressiveIconButton
import aki.tr.ui.components.fadingEdge

/**
 * Primary action button for triggering manual translation.
 */
@Material3ExpressiveApi
@Composable
private fun TranslateButton(onClick: () -> Unit) {
    ExpressiveButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Translate, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("TRANSLATE", fontWeight = FontWeight.Bold)
    }
}

/**
 * Output section showing the translated result, loading indicator, or error.
 * Adapts layout direction to match the target language's RTL setting.
 */
@Material3ExpressiveApi
@Composable
fun ColumnScope.OutputSection(
    state: TranslatorUiState,
    outputLayoutDir: LayoutDirection,
    currentLang: LanguageProfile?,
    onCopy: () -> Unit,
    onTranslate: () -> Unit
) {
    val outputScrollState = rememberScrollState()
    val weightFraction: Float = remember(state.output, state.isLoading) {
        val inpWeight = if (state.output.isNotEmpty() || state.isLoading) 0.6f else 1f
        2f - inpWeight
    }

    CompositionLocalProvider(LocalLayoutDirection provides outputLayoutDir) {
        Surface(
            modifier = Modifier
                .weight(weightFraction)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (currentLang?.name ?: "RESULT").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.output.isNotEmpty()) {
                        ExpressiveIconButton(onClick = onCopy) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fadingEdge(outputScrollState)
                ) {
                    when {
                        state.isLoading -> {
                            ContainedLoadingIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                containerShape = LoadingIndicatorDefaults.containerShape,
                                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
                            )
                        }
                        state.error != null -> {
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            SelectionContainer {
                                Text(
                                    text = state.output.ifEmpty { "Translation will appear here" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (state.output.isEmpty())
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(outputScrollState)
                                )
                            }
                        }
                    }
                }

                // Translate button shown when there's manual input that hasn't been translated yet.
                if (state.input.isNotBlank() && !state.isPasteTriggered && !state.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TranslateButton(onClick = onTranslate)
                }
            }
        }
    }
}
