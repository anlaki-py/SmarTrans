package aki.tr.config.data

import android.content.Context
import aki.tr.config.model.AppConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for managing app configuration persistence.
 * Uses SharedPreferences for safe, scoped storage (no external storage permission needed).
 * Owns data access only — no business logic.
 */
object ConfigRepository {

    private const val PREFS_NAME = "smartrans_config"
    private const val KEY_CONFIG = "app_config_json"

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private lateinit var appContext: Context

    /**
     * Initialises the repository with the application context.
     * Must be called once during Application or Activity onCreate.
     *
     * @param context Any context; internally converts to applicationContext.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Loads the application configuration from SharedPreferences.
     * Creates default config if none exists.
     *
     * @return The loaded or default [AppConfig].
     */
    fun loadConfig(): AppConfig {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val text = prefs.getString(KEY_CONFIG, null)
            ?: return AppConfig().also { saveConfig(it) }
        return try {
            json.decodeFromString<AppConfig>(text)
        } catch (_: Exception) {
            AppConfig()
        }
    }

    /**
     * Saves the configuration to SharedPreferences.
     *
     * @param config The configuration to save.
     */
    fun saveConfig(config: AppConfig) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CONFIG, json.encodeToString(config)).apply()
    }
}
