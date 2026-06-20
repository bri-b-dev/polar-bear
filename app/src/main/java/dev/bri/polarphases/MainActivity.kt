package dev.bri.polarphases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.bri.polarphases.ui.screen.HrMonitorScreen
import dev.bri.polarphases.ui.screen.ZoneManagementScreen
import dev.bri.polarphases.ui.theme.PolarPhasesTheme
import dev.bri.polarphases.viewmodel.BleViewModel
import dev.bri.polarphases.viewmodel.ZoneViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolarPhasesTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "hr_monitor") {
                    composable("hr_monitor") {
                        val bleViewModel: BleViewModel = viewModel()
                        HrMonitorScreen(
                            viewModel = bleViewModel,
                            onNavigateToZones = { navController.navigate("zones") },
                        )
                    }
                    composable("zones") {
                        val zoneViewModel: ZoneViewModel = viewModel()
                        ZoneManagementScreen(
                            viewModel = zoneViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
