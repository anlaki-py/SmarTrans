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
     * Executes the translation API call with key rotation.
     *
     * Iterates through available API keys for the provider, skipping rate-limited
     * and invalid keys. Stops on first success or when all keys are exhausted.
     */
    private fun performTranslation(text: String) {
        val config = _uiState.value.config
        val provider = _uiState.value.currentProvider ?: return
        val language = config.languages.find { it.id == config.selectedLanguageId } ?: return

        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val prompt = "${config.systemPrompt}\n${language.instruction}"
            val result = tryGenerateWithRotation(provider, prompt, text)

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(output = result.getOrNull()?.text ?: "", isLoading = false)
                }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiState.update { it.copy(output = "", error = errorMessage, isLoading = false) }
            }
        }
    }

    /**
     * Attempts translation with key rotation.
     *
     * Tries each available key in round-robin order. On rate-limit or invalid-key
     * errors, marks the key and tries the next one. Returns the first success or
     * the final error after all keys are exhausted.
     *
     * @param provider The selected provider.
     * @param prompt The full prompt including system and language instructions.
     * @param text The user input text.
     * @return [Result] wrapping [GenerateResult] on success, or failure with the last error.
     */
    private suspend fun tryGenerateWithRotation(
        provider: Provider,
        prompt: String,
        text: String
    ): Result<aki.tr.api.model.GenerateResult> {
        val providerId = provider.id
        val attemptedKeys = mutableSetOf<String>()

        while (true) {
            val apiKey = keyManager.getNextKey(providerId) ?: break
            if (!attemptedKeys.add(apiKey)) break // No new keys left

            val result = apiClient.generate(
                prompt = prompt,
                text = text,
                apiKey = apiKey,
                model = provider.selectedModel,
                temperature = 0.3,
                endpoint = provider.endpoint
            )

            if (result.isSuccess) return result

            val exception = result.exceptionOrNull()
            if (exception is ApiException) {
                when (exception.apiError) {
                    is ApiError.RateLimit -> {
                        val retryAfter = (exception.apiError as ApiError.RateLimit).retryAfterSeconds
                        keyManager.reportRateLimit(providerId, apiKey, retryAfter?.toLong() ?: 60)
                        continue // Try next key
                    }
                    is ApiError.InvalidKey -> {
                        keyManager.markInvalid(providerId, apiKey)
                        continue // Try next key
                    }
                    else -> return result // Non-rotatable error
                }
            } else {
                return result // Non-ApiException error
            }
        }

        // All keys exhausted or none available
        val waitTime = keyManager.getShortestWaitTimeMs(providerId)
        return if (waitTime != null) {
            Result.failure(Exception("All keys rate-limited. Retry in ${waitTime / 1000}s."))
        } else {
            Result.failure(Exception("No valid API keys available. Please check your settings."))
        }
    }

    companion object {
        private const val DEBOUNCE_DELAY = 600L
    }
}
