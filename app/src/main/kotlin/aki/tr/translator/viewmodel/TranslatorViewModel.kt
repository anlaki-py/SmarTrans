package aki.tr.translator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import aki.tr.api.data.OpenAICompatibleClient
import aki.tr.api.model.ApiError
import aki.tr.api.model.ApiException
import aki.tr.config.data.ConfigRepository
import aki.tr.config.model.AppConfig
import aki.tr.config.model.LanguageProfile
import aki.tr.key.data.KeyManager
import aki.tr.provider.data.ProviderManager
import aki.tr.provider.model.Provider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Translation screen state. Keeps providers list alongside config
 * so the UI can observe both from a single source of truth.
 */
data class TranslatorUiState(
    val input: String = "",
    val output: String = "",
    val isLoading: Boolean = false,
    val config: AppConfig = AppConfig(),
    val providers: List<Provider> = emptyList(),
    val error: String? = null
) {
    val hasOutput: Boolean get() = output.isNotEmpty()
    val hasInput: Boolean get() = input.isNotEmpty()

    val currentProvider: Provider?
        get() = providers.find { it.id == config.selectedProviderId }

    val currentLanguage: LanguageProfile?
        get() = config.languages.find { it.id == config.selectedLanguageId }
}

/**
 * ViewModel for the translation screen only.
 * Handles input debouncing, API calls, and error mapping.
 */
class TranslatorViewModel(
    private val providerManager: ProviderManager,
    private val keyManager: KeyManager,
    private val apiClient: OpenAICompatibleClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private var translationJob: Job? = null
    private val inputFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            inputFlow
                .debounce(DEBOUNCE_DELAY)
                .distinctUntilChanged()
                .collect { text -> if (text.isNotBlank()) performTranslation(text) }
        }
    }

    /**
     * Called once after the Activity is created to load persisted data.
     */
    fun initialise() {
        val config = ConfigRepository.loadConfig()
        val providers = providerManager.getProviders()
        _uiState.update { it.copy(config = config, providers = providers) }
    }

    /**
     * Persists a new config and re-triggers translation if input is present.
     *
     * @param newConfig The updated config to save.
     */
    fun saveConfig(newConfig: AppConfig) {
        ConfigRepository.saveConfig(newConfig)
        _uiState.update { it.copy(config = newConfig) }
        if (_uiState.value.input.isNotBlank()) performTranslation(_uiState.value.input)
    }

    /**
     * Refreshes the providers list from [ProviderManager].
     * Called after provider mutations from the settings screen.
     */
    fun refreshProviders() {
        _uiState.update { it.copy(providers = providerManager.getProviders()) }
    }

    /**
     * Handles input text changes with debounced translation.
     *
     * @param text The new input text.
     */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
        if (text.isEmpty()) {
            _uiState.update { it.copy(output = "", isLoading = false) }
            translationJob?.cancel()
        } else {
            inputFlow.tryEmit(text)
        }
    }

    /**
     * Immediately triggers a translation for the current input.
     */
    fun triggerTranslation() {
        val text = _uiState.value.input
        if (text.isNotBlank()) performTranslation(text)
    }

    /**
     * Executes the translation API call and maps errors to user-facing messages.
     */
    private fun performTranslation(text: String) {
        val config = _uiState.value.config
        val provider = _uiState.value.currentProvider ?: return
        val language = config.languages.find { it.id == config.selectedLanguageId } ?: return
        val apiKey = keyManager.getNextKey(provider.id) ?: return

        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val prompt = "${config.systemPrompt}\n${language.instruction}"
            val result = apiClient.generate(
                prompt = prompt,
                text = text,
                apiKey = apiKey,
                model = provider.selectedModel,
                temperature = 0.3,
                endpoint = provider.endpoint
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(output = result.getOrNull()?.text ?: "", isLoading = false)
                }
            } else {
                val errorMessage = mapApiError(result.exceptionOrNull(), apiKey)
                _uiState.update { it.copy(output = "", error = errorMessage, isLoading = false) }
            }
        }
    }

    /**
     * Maps an API exception to a user-facing string and reports issues to [KeyManager].
     */
    private fun mapApiError(exception: Throwable?, apiKey: String): String {
        if (exception is ApiException) {
            return when (exception.apiError) {
                is ApiError.RateLimit -> {
                    keyManager.reportRateLimit(apiKey)
                    "Rate limited. Please try again later."
                }
                is ApiError.InvalidKey -> {
                    keyManager.markInvalid(apiKey)
                    "Invalid API key. Please check your settings."
                }
                is ApiError.Network -> "Network error. Please check your connection."
                is ApiError.ServerError -> "Server error. Please try again later."
                is ApiError.Other -> exception.message ?: "Unknown error"
            }
        }
        return exception?.message ?: "Unknown error"
    }

    companion object {
        private const val DEBOUNCE_DELAY = 600L
    }
}
