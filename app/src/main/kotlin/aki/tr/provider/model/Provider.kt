package aki.tr.provider.model

import java.util.UUID

/**
 * AI provider configuration.
 * Matches the schema used by [aki.tr.provider.data.ProviderManager].
 * API keys are stored separately via [aki.tr.key.data.KeyManager] for security.
 */
data class Provider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endpoint: String,
    val selectedModel: String = ""
)
