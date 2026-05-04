package aki.tr.translator.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import aki.tr.translator.viewmodel.TranslatorViewModel
import aki.tr.ui.components.isRtl

/**
 * Main translation screen — input area, paste button, and output area.
 * Orchestrates child composables extracted into their own files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    viewModel: TranslatorViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showLangSheet by remember { mutableStateOf(false) }
    var showProviderSheet by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val inputWeight by animateFloatAsState(
        targetValue = if (state.output.isNotEmpty() || state.isLoading) 0.6f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "weight"
    )

    val currentLang = remember(state.config) {
        state.config.languages.find { it.id == state.config.selectedLanguageId }
    }
    val currentProvider = remember(state.config, state.providers) {
        state.providers.find { it.id == state.config.selectedProviderId }
    }
    val isInputRtl = remember(state.input) { isRtl(state.input) }
    val outputLayoutDir = if (currentLang?.isRtl == true) LayoutDirection.Rtl else LayoutDirection.Ltr
    val inputScrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ProviderSelector(
                            provider = currentProvider,
                            onClick = { showProviderSheet = true }
                        )
                        Spacer(Modifier.width(8.dp))
                        LanguageSelector(
                            language = currentLang,
                            onClick = { showLangSheet = true }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .consumeWindowInsets(innerPadding)
        ) {
            // Input + PasteButton overlaid so fade extends under the button
            Box(
                modifier = Modifier
                    .weight(inputWeight)
                    .fillMaxWidth()
            ) {
                InputSection(
                    input = state.input,
                    onInputChange = { viewModel.onInputChange(it) },
                    isInputRtl = isInputRtl,
                    inputScrollState = inputScrollState,
                    modifier = Modifier.fillMaxSize()
                )
                PasteButton(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onClick = {
                        clipboard.getText()?.text?.let { viewModel.onInputChange(it.toString()) }
                    }
                )
            }
            OutputSection(
                state = state,
                outputLayoutDir = outputLayoutDir,
                currentLang = currentLang,
                onCopy = { clipboard.setText(AnnotatedString(state.output)) }
            )
        }
    }

    if (showLangSheet) {
        LanguageBottomSheet(
            state = state,
            onDismiss = { showLangSheet = false },
            onSelect = { lang ->
                viewModel.saveConfig(state.config.copy(selectedLanguageId = lang.id))
                showLangSheet = false
            }
        )
    }

    if (showProviderSheet) {
        ProviderBottomSheet(
            state = state,
            onDismiss = { showProviderSheet = false },
            onSelect = { provider ->
                viewModel.saveConfig(state.config.copy(selectedProviderId = provider.id))
                showProviderSheet = false
            }
        )
    }
}
