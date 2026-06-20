package dev.bri.polarphases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.bri.polarphases.ui.screen.HrMonitorScreen
import dev.bri.polarphases.ui.theme.PolarPhasesTheme
import dev.bri.polarphases.viewmodel.BleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolarPhasesTheme {
                val bleViewModel: BleViewModel = viewModel()
                HrMonitorScreen(viewModel = bleViewModel)
            }
        }
    }
}
