package aki.tr

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import aki.tr.api.data.OpenAICompatibleClient
import aki.tr.config.data.ConfigRepository
import aki.tr.key.data.KeyManager
import aki.tr.provider.data.ProviderManager
import aki.tr.settings.viewmodel.SettingsViewModel
import aki.tr.settings.ui.SettingsScreen
import aki.tr.translator.viewmodel.TranslatorViewModel
import aki.tr.translator.ui.TranslatorScreen
import aki.tr.ui.theme.AkiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AkiTheme {
                SmarTransApp()
            }
        }
    }
}

/**
 * Top-level navigation composable.
 * Creates feature-scoped ViewModels with shared manager instances.
 */
@Composable
fun SmarTransApp() {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Shared singletons — created once per composition
    val providerManager = remember { ProviderManager(application) }
    val keyManager = remember { KeyManager(application) }
    val apiClient = remember { OpenAICompatibleClient() }

    // Initialise ConfigRepository on first composition
    LaunchedEffect(Unit) { ConfigRepository.init(application) }

    val translatorViewModel: TranslatorViewModel = viewModel {
        TranslatorViewModel(providerManager, keyManager, apiClient)
    }
    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(application)
    }

    // Refresh translator providers when returning from settings
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { slideInHorizontally(tween(300)) { it } },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } }
    ) {
        composable("home") {
            LaunchedEffect(Unit) { translatorViewModel.initialise() }
            TranslatorScreen(
                viewModel = translatorViewModel,
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            LaunchedEffect(Unit) { settingsViewModel.initialise() }
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = {
                    translatorViewModel.refreshProviders()
                    navController.popBackStack()
                }
            )
        }
    }
}
