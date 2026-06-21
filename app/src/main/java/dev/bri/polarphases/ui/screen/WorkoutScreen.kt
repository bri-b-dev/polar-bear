package dev.bri.polarphases.ui.screen

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.viewmodel.BleViewModel
import dev.bri.polarphases.viewmodel.ExecutionPhase
import dev.bri.polarphases.viewmodel.WorkoutExecutionViewModel
import dev.bri.polarphases.viewmodel.WorkoutState
import dev.bri.polarphases.viewmodel.ZoneCompliance

private val COLOR_TOO_HIGH = Color(0xFFF44336)
private val COLOR_TOO_LOW = Color(0xFFFF9800)
private val COLOR_NO_READING = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutExecutionViewModel,
    bleVm: BleViewModel,
    onEnd: () -> Unit,
) {
    val workoutState by viewModel.state.collectAsState()
    val bleState by bleVm.state.collectAsState()

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(bleState) {
        viewModel.onBleStateChange(bleState)
    }

    LaunchedEffect(workoutState) {
        if (workoutState is WorkoutState.Finished) onEnd()
    }

    when (val state = workoutState) {
        is WorkoutState.NotStarted -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is WorkoutState.Active -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = state.templateName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        actions = {
                            TextButton(onClick = viewModel::end) {
                                Text("End", color = MaterialTheme.colorScheme.error)
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BpmSection(modifier = Modifier.weight(0.30f), state = state)
                    HorizontalDivider()
                    PhaseSection(modifier = Modifier.weight(0.42f), state = state)
                    HorizontalDivider()
                    ControlsSection(
                        modifier = Modifier.weight(0.28f),
                        state = state,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onSkip = viewModel::skipPhase,
                        onExitBlock = viewModel::exitBlock,
                    )
                }
            }
        }
        is WorkoutState.Finished -> {
            Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BpmSection(modifier: Modifier, state: WorkoutState.Active) {
    val bpmColor = currentZoneColor(state.currentBpm, state.allZones)
    val showWarning = state.enteredTargetZone &&
        (state.zoneCompliance == ZoneCompliance.TOO_HIGH || state.zoneCompliance == ZoneCompliance.TOO_LOW)

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = state.currentBpm?.toString() ?: "--",
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = bpmColor,
        )
        Text(
            text = "bpm",
            style = MaterialTheme.typography.bodyMedium,
            color = bpmColor,
        )
        if (showWarning) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.zoneCompliance == ZoneCompliance.TOO_HIGH) "▲ Too high" else "▼ Too low",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.zoneCompliance == ZoneCompliance.TOO_HIGH) COLOR_TOO_HIGH else COLOR_TOO_LOW,
            )
        }
    }
}

@Composable
private fun PhaseSection(modifier: Modifier, state: WorkoutState.Active) {
    val phase = state.current
    val phaseColor = targetZoneColor(phase) ?: MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = phase.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = phaseColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatTime(state.remainingSeconds),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = phaseColor,
        )
        if (phase.blockRepIndex != null && phase.blockTotalReps != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Repetition ${phase.blockRepIndex} of ${phase.blockTotalReps}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (phase.targetZones.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ZoneChipsRow(phase.targetZones)
        }
    }
}

@Composable
private fun ControlsSection(
    modifier: Modifier,
    state: WorkoutState.Active,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onExitBlock: () -> Unit,
) {
    val isInBlock = state.current.blockId != null
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Phase ${state.currentIndex + 1} of ${state.totalPhases}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (state.currentIndex + 1).toFloat() / state.totalPhases },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = if (state.isPaused) onResume else onPause,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (state.isPaused) "▶ Resume" else "⏸ Pause",
                    fontSize = 16.sp,
                )
            }
            Button(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
            ) {
                Text("⏭ Skip", fontSize = 16.sp)
            }
        }
        if (isInBlock) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onExitBlock,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Exit Block", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ZoneChipsRow(zones: List<HrZone>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        zones.forEach { zone ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color(zone.colorArgb), CircleShape)
                )
                Text(zone.name, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun currentZoneColor(bpm: Int?, allZones: List<HrZone>): Color {
    if (bpm == null) return COLOR_NO_READING
    return allZones.firstOrNull { bpm in it.bpmMin..it.bpmMax }
        ?.let { Color(it.colorArgb) }
        ?: COLOR_NO_READING
}

private fun targetZoneColor(phase: ExecutionPhase): Color? =
    phase.targetZones.firstOrNull()?.let { Color(it.colorArgb) }

private fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
