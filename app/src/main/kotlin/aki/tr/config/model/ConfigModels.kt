package aki.tr.config.model

import kotlinx.serialization.Serializable

/**
 * Main application configuration.
 * Contains user preferences including the active provider/language selection
 * and language profiles. Provider data is managed separately by ProviderManager.
 */
@Serializable
data class AppConfig(
    val selectedProviderId: String = "",
    val selectedLanguageId: String = "lang-en",
    val languages: List<LanguageProfile> = DEFAULT_LANGUAGES
) {
    companion object {
        private val DEFAULT_LANGUAGES = listOf(
            LanguageProfile("lang-en", "English", "Translate to English.", false),
            LanguageProfile("lang-ar", "Arabic", "Translate to Arabic.", true),
            LanguageProfile("lang-fr", "French", "Translate to French.", false),
            LanguageProfile("lang-ja", "Japanese", "Translate to Japanese.", false),
            LanguageProfile("lang-zh", "Chinese", "Translate to Chinese.", false)
        )
    }
}
