package dev.bri.polarphases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.bri.polarphases.ui.screen.HrMonitorScreen
import dev.bri.polarphases.ui.screen.TemplateBuilderScreen
import dev.bri.polarphases.ui.screen.TemplateListScreen
import dev.bri.polarphases.ui.screen.ZoneManagementScreen
import dev.bri.polarphases.ui.theme.PolarPhasesTheme
import dev.bri.polarphases.viewmodel.BleViewModel
import dev.bri.polarphases.viewmodel.TemplateBuilderViewModel
import dev.bri.polarphases.viewmodel.TemplateListViewModel
import dev.bri.polarphases.viewmodel.ZoneViewModel
import androidx.compose.runtime.LaunchedEffect

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
                            onNavigateToTemplates = { navController.navigate("templates") },
                        )
                    }
                    composable("zones") {
                        val zoneViewModel: ZoneViewModel = viewModel()
                        ZoneManagementScreen(
                            viewModel = zoneViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("templates") {
                        val templateListViewModel: TemplateListViewModel = viewModel()
                        TemplateListScreen(
                            viewModel = templateListViewModel,
                            onBack = { navController.popBackStack() },
                            onNewTemplate = { navController.navigate("template_builder/0") },
                            onEditTemplate = { id -> navController.navigate("template_builder/$id") },
                        )
                    }
                    composable(
                        route = "template_builder/{templateId}",
                        arguments = listOf(
                            navArgument("templateId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        ),
                    ) { backStackEntry ->
                        val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
                        val templateBuilderViewModel: TemplateBuilderViewModel = viewModel()
                        LaunchedEffect(templateId) {
                            if (templateId > 0L) templateBuilderViewModel.loadTemplate(templateId)
                        }
                        TemplateBuilderScreen(
                            viewModel = templateBuilderViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
