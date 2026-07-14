package dev.bri.polarbear.ui.screen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bri.polarbear.ble.BleUiState
import dev.bri.polarbear.data.model.HrZone
import dev.bri.polarbear.viewmodel.BleViewModel
import dev.bri.polarbear.viewmodel.ExecutionPhase
import dev.bri.polarbear.viewmodel.PlanGroup
import dev.bri.polarbear.viewmodel.WorkoutExecutionViewModel
import dev.bri.polarbear.viewmodel.WorkoutState
import dev.bri.polarbear.viewmodel.ZoneCompliance

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

    // Android 13+: the foreground-service notification is only shown with this
    // permission; the service itself runs either way.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
            var isOverviewOpen by rememberSaveable { mutableStateOf(false) }
            val isReconnecting = bleState is BleUiState.Reconnecting

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
                            TextButton(onClick = { isOverviewOpen = true }) {
                                Text("Overview")
                            }
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
                    if (isReconnecting) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = "Reconnecting to sensor…",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
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

            if (isOverviewOpen) {
                ModalBottomSheet(
                    onDismissRequest = { isOverviewOpen = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    OverviewSheet(state = state)
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

private enum class GroupStatus { PENDING, ACTIVE, DONE }

private fun groupStatus(firstIndex: Int, lastIndex: Int, currentIndex: Int): GroupStatus = when {
    currentIndex > lastIndex -> GroupStatus.DONE
    currentIndex >= firstIndex -> GroupStatus.ACTIVE
    else -> GroupStatus.PENDING
}

@Composable
private fun OverviewSheet(state: WorkoutState.Active) {
    val expandedBlocks = remember { mutableStateMapOf<Long, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Workout Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))

        val progress = state.overallProgress
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(state.elapsedSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTime(state.totalDurationSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(
                items = state.planGroups,
                key = { group ->
                    when (group) {
                        is PlanGroup.StandalonePhase -> "phase_${group.firstIndex}"
                        is PlanGroup.BlockGroup -> "block_${group.blockId}"
                    }
                },
            ) { group ->
                when (group) {
                    is PlanGroup.StandalonePhase -> StandalonePhaseRow(group, state.currentIndex)
                    is PlanGroup.BlockGroup -> BlockGroupRow(
                        group = group,
                        currentIndex = state.currentIndex,
                        isExpanded = expandedBlocks[group.blockId] == true,
                        onToggleExpand = {
                            expandedBlocks[group.blockId] = !(expandedBlocks[group.blockId] ?: false)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StandalonePhaseRow(group: PlanGroup.StandalonePhase, currentIndex: Int) {
    val status = groupStatus(group.firstIndex, group.lastIndex, currentIndex)
    val zoneColor = group.targetZones.firstOrNull()?.let { Color(it.colorArgb) }
    val textAlpha = if (status == GroupStatus.PENDING) 0.45f else 1f
    val rowBg = if (status == GroupStatus.ACTIVE)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(4.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = when (status) {
                GroupStatus.DONE -> "✓"
                GroupStatus.ACTIVE -> "▶"
                GroupStatus.PENDING -> "○"
            },
            color = when (status) {
                GroupStatus.DONE -> Color(0xFF4CAF50).copy(alpha = 0.7f)
                GroupStatus.ACTIVE -> zoneColor ?: MaterialTheme.colorScheme.primary
                GroupStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        if (zoneColor != null) {
            Box(Modifier.size(10.dp).background(zoneColor.copy(alpha = textAlpha), CircleShape))
        }
        Text(
            text = group.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (status == GroupStatus.ACTIVE) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatTime(group.durationSeconds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
        )
    }
}

@Composable
private fun BlockGroupRow(
    group: PlanGroup.BlockGroup,
    currentIndex: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    val status = groupStatus(group.firstIndex, group.lastIndex, currentIndex)
    val textAlpha = if (status == GroupStatus.PENDING) 0.45f else 1f
    val rowBg = if (status == GroupStatus.ACTIVE)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else Color.Transparent

    val currentRep: Int? = if (status == GroupStatus.ACTIVE) {
        val indexInBlock = currentIndex - group.firstIndex
        (indexInBlock / group.phaseNames.size) + 1
    } else null

    val blockTotalDuration = group.phaseDurations.sum() * group.totalReps

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBg, RoundedCornerShape(4.dp))
                .clickable(onClick = onToggleExpand)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = when (status) {
                    GroupStatus.DONE -> "✓"
                    GroupStatus.ACTIVE -> "▶"
                    GroupStatus.PENDING -> "○"
                },
                color = when (status) {
                    GroupStatus.DONE -> Color(0xFF4CAF50).copy(alpha = 0.7f)
                    GroupStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                    GroupStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Block × ${group.totalReps}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (status == GroupStatus.ACTIVE) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                )
                when {
                    currentRep != null -> Text(
                        text = "Rep $currentRep of ${group.totalReps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    status == GroupStatus.DONE -> Text(
                        text = "Completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            Text(
                text = formatTime(blockTotalDuration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
            )
            Text(
                text = if (isExpanded) "▲" else "▼",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                group.phaseNames.forEachIndexed { idx, name ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val zoneColor = group.phaseZones[idx].firstOrNull()?.let { Color(it.colorArgb) }
                        if (zoneColor != null) {
                            Box(Modifier.size(8.dp).background(zoneColor, CircleShape))
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatTime(group.phaseDurations[idx]),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
