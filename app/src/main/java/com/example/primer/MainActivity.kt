package com.example.primer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.primer.notification.AlarmScheduler
import com.example.primer.ui.main.MainScreen
import com.example.primer.ui.main.MainViewModel
import com.example.primer.ui.settings.SettingsScreen
import com.example.primer.ui.settings.SettingsViewModel
import com.example.primer.ui.theme.PrimerTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        val prefs = getSharedPreferences("primer_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("alarm_initialized", false)) {
            AlarmScheduler.scheduleNext(this)
            prefs.edit().putBoolean("alarm_initialized", true).apply()
        }

        setContent {
            PrimerTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        val backStackEntry = it
                        DisposableEffect(backStackEntry.lifecycle) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    mainViewModel.refresh()
                                }
                            }
                            backStackEntry.lifecycle.addObserver(observer)
                            onDispose { backStackEntry.lifecycle.removeObserver(observer) }
                        }
                        MainScreen(
                            viewModel = mainViewModel,
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        SettingsScreen(viewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}
