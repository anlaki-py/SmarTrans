package aki.tr.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import aki.tr.api.data.OpenAICompatibleClient
import aki.tr.config.data.ConfigRepository
import aki.tr.config.model.AppConfig
import aki.tr.key.data.KeyManager
import aki.tr.provider.data.ProviderManager
import aki.tr.provider.model.Provider
import aki.tr.provider.validation.KeyValidation
import aki.tr.provider.validation.KeyValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings screen state holding the current config, provider list, and keys per provider.
 *
 * @param config Current app configuration.
 * @param providers List of configured providers.
 * @param providerKeys Map of provider ID to list of API key strings (masked).
 */
data class SettingsUiState(
    val config: AppConfig = AppConfig(),
    val providers: List<Provider> = emptyList(),
    val providerKeys: Map<String, List<String>> = emptyMap()
)

/**
 * ViewModel for the settings screen only.
 * Handles provider CRUD and config persistence.
 * Does not hold Context — uses Application via AndroidViewModel.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val providerManager = ProviderManager(application)
    private val keyManager = KeyManager(application)
    private val apiClient = OpenAICompatibleClient()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Loads the current config, provider list, and keys for each provider.
     */
    fun initialise() {
        val config = ConfigRepository.loadConfig()
        val providers = providerManager.getProviders()
        val keysMap = providers.associate { it.id to keyManager.getKeys(it.id) }
        _uiState.update { it.copy(config = config, providers = providers, providerKeys = keysMap) }
    }

    /**
     * Refreshes the provider keys map from [KeyManager].
     */
    private fun refreshKeys() {
        val keysMap = _uiState.value.providers.associate { it.id to keyManager.getKeys(it.id) }
        _uiState.update { it.copy(providerKeys = keysMap) }
    }

    /**
     * Persists a config update and refreshes the state.
     *
     * @param newConfig The updated config.
     */
    fun saveConfig(newConfig: AppConfig) {
        ConfigRepository.saveConfig(newConfig)
        _uiState.update { it.copy(config = newConfig) }
    }

    /**
     * Creates or updates a provider and refreshes the provider list.
     *
     * @param provider The provider to save.
     * @param isNew True if this is a new provider creation.
     */
    fun saveProvider(provider: Provider, isNew: Boolean) {
        if (isNew) {
            providerManager.addProvider(provider.name, provider.endpoint)
            if (provider.selectedModel.isNotBlank()) {
                providerManager.updateProvider(id = provider.id, selectedModel = provider.selectedModel)
            }
        } else {
            providerManager.updateProvider(
                id = provider.id,
                name = provider.name,
                endpoint = provider.endpoint,
                selectedModel = provider.selectedModel
            )
        }
        val providers = providerManager.getProviders()
        val keysMap = providers.associate { it.id to keyManager.getKeys(it.id) }
        _uiState.update { it.copy(providers = providers, providerKeys = keysMap) }
    }

    /**
     * Adds an API key for a provider without network validation.
     *
     * Duplicate keys are ignored.
     *
     * @param providerId The provider UUID.
     * @param key The API key to add.
     */
    fun addKey(providerId: String, key: String) {
        keyManager.addKey(providerId, key)
        refreshKeys()
    }

    /**
     * Removes an API key from a provider.
     *
     * @param providerId The provider UUID.
     * @param key The API key to remove.
     */
    fun removeKey(providerId: String, key: String) {
        keyManager.removeKey(providerId, key)
        refreshKeys()
    }

    /**
     * Removes a provider, its keys, and updates the active selection.
     *
     * @param id The provider UUID to remove.
     */
    fun deleteProvider(id: String) {
        providerManager.removeProvider(id)
        keyManager.removeKeysForProvider(id)
        val providers = providerManager.getProviders()
        val currentSelectedId = _uiState.value.config.selectedProviderId
        val newSelectedId = if (currentSelectedId == id) {
            providers.firstOrNull()?.id ?: ""
        } else {
            currentSelectedId
        }
        _uiState.update {
            it.copy(
                providers = providers,
                config = it.config.copy(selectedProviderId = newSelectedId),
                providerKeys = it.providerKeys - id
            )
        }
        ConfigRepository.saveConfig(_uiState.value.config)
    }

    /**
     * Removes a language profile and updates the active selection.
     *
     * @param id The language profile UUID to remove.
     */
    fun deleteLanguage(id: String) {
        val current = _uiState.value.config
        if (current.languages.size <= 1) return
        val newList = current.languages.filter { it.id != id }
        val newSelected = if (current.selectedLanguageId == id) {
            newList.first().id
        } else {
            current.selectedLanguageId
        }
        saveConfig(current.copy(languages = newList, selectedLanguageId = newSelected))
    }
}
