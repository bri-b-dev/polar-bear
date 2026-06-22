package dev.bri.polarphases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.bri.polarphases.ui.screen.HrMonitorScreen
import dev.bri.polarphases.ui.screen.TemplateBuilderScreen
import dev.bri.polarphases.ui.screen.TemplateListScreen
import dev.bri.polarphases.ui.screen.WorkoutHistoryScreen
import dev.bri.polarphases.ui.screen.WorkoutScreen
import dev.bri.polarphases.ui.screen.WorkoutSessionDetailScreen
import dev.bri.polarphases.ui.screen.ZoneManagementScreen
import dev.bri.polarphases.ui.theme.PolarPhasesTheme
import dev.bri.polarphases.viewmodel.BleViewModel
import dev.bri.polarphases.viewmodel.TemplateBuilderViewModel
import dev.bri.polarphases.viewmodel.TemplateListViewModel
import dev.bri.polarphases.viewmodel.WorkoutExecutionViewModel
import dev.bri.polarphases.viewmodel.WorkoutHistoryViewModel
import dev.bri.polarphases.viewmodel.WorkoutSessionDetailViewModel
import dev.bri.polarphases.viewmodel.ZoneViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolarPhasesTheme {
                val navController = rememberNavController()
                // Activity-scoped so BLE connection persists across navigation (N-6)
                val bleVm: BleViewModel = viewModel()
                NavHost(navController = navController, startDestination = "hr_monitor") {
                    composable("hr_monitor") {
                        HrMonitorScreen(
                            viewModel = bleVm,
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
                            onStartWorkout = { id -> navController.navigate("workout/$id") },
                            onNavigateToHistory = { navController.navigate("history") },
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
                    composable(
                        route = "workout/{templateId}",
                        arguments = listOf(
                            navArgument("templateId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        ),
                    ) { backStackEntry ->
                        val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
                        val workoutVm: WorkoutExecutionViewModel = viewModel()
                        LaunchedEffect(templateId) {
                            workoutVm.loadAndStart(templateId)
                        }
                        WorkoutScreen(
                            viewModel = workoutVm,
                            bleVm = bleVm,
                            onEnd = { navController.popBackStack() },
                        )
                    }
                    composable("history") {
                        val historyVm: WorkoutHistoryViewModel = viewModel()
                        WorkoutHistoryScreen(
                            viewModel = historyVm,
                            onBack = { navController.popBackStack() },
                            onNavigateToSession = { id -> navController.navigate("history/session/$id") },
                        )
                    }
                    composable(
                        route = "history/session/{sessionId}",
                        arguments = listOf(
                            navArgument("sessionId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        ),
                    ) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                        val detailVm: WorkoutSessionDetailViewModel = viewModel()
                        WorkoutSessionDetailScreen(
                            viewModel = detailVm,
                            sessionId = sessionId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
