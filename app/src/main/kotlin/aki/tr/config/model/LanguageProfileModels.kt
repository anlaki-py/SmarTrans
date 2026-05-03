package aki.tr.config.model

import kotlinx.serialization.Serializable

/**
 * Language translation profile.
 * Contains instructions for translating to a specific language.
 */
@Serializable
data class LanguageProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val instruction: String,
    val isRtl: Boolean = false
)
