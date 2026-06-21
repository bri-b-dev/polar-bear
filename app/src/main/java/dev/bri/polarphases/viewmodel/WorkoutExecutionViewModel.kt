package dev.bri.polarphases.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.bri.polarphases.PolarPhasesApp
import dev.bri.polarphases.ble.BleUiState
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.data.model.WorkoutTemplateWithItems
import dev.bri.polarphases.repository.toZoneIdList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ExecutionPhase(
    val name: String,
    val durationSeconds: Int,
    val targetZones: List<HrZone>,
    val blockRepIndex: Int?,
    val blockTotalReps: Int?,
    val blockId: Long? = null,
)

enum class ZoneCompliance { IN_ZONE, TOO_HIGH, TOO_LOW, UNKNOWN }

sealed class WorkoutState {
    data object NotStarted : WorkoutState()
    data class Active(
        val templateName: String,
        val plan: List<ExecutionPhase>,
        val currentIndex: Int,
        val remainingSeconds: Int,
        val currentBpm: Int?,
        val allZones: List<HrZone>,
        val zoneCompliance: ZoneCompliance,
        // true once the user has been inside the target zone during this phase
        val enteredTargetZone: Boolean,
        // prevents re-signaling every second after leaving the zone
        val outOfZoneSignalFired: Boolean,
        val isPaused: Boolean,
    ) : WorkoutState() {
        val current: ExecutionPhase get() = plan[currentIndex]
        val totalPhases: Int get() = plan.size
    }
    data object Finished : WorkoutState()
}

class WorkoutExecutionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PolarPhasesApp
    private val templateRepo = app.templateRepository
    private val zoneRepo = app.zoneRepository

    private val _state = MutableStateFlow<WorkoutState>(WorkoutState.NotStarted)
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun loadAndStart(templateId: Long) {
        viewModelScope.launch {
            val withItems = templateRepo.observeWithItems(templateId).first() ?: return@launch
            val allZones = zoneRepo.observeZones().first()
            val zoneMap = allZones.associateBy { it.id }
            val plan = buildExecutionPlan(withItems, zoneMap)
            if (plan.isEmpty()) return@launch
            _state.value = WorkoutState.Active(
                templateName = withItems.template.name,
                plan = plan,
                currentIndex = 0,
                remainingSeconds = plan[0].durationSeconds,
                currentBpm = null,
                allZones = allZones,
                zoneCompliance = ZoneCompliance.UNKNOWN,
                enteredTargetZone = false,
                outOfZoneSignalFired = false,
                isPaused = false,
            )
            startTimer()
        }
    }

    fun onBleStateChange(bleState: BleUiState) {
        val active = _state.value as? WorkoutState.Active ?: return
        val bpm = (bleState as? BleUiState.Connected)?.bpm
        if (bpm == null) {
            // No live reading — show "--", suspend zone compliance
            _state.value = active.copy(
                currentBpm = null,
                zoneCompliance = ZoneCompliance.UNKNOWN,
            )
            return
        }
        val compliance = checkZoneCompliance(bpm, active.current.targetZones)
        var enteredTargetZone = active.enteredTargetZone
        var outOfZoneSignalFired = active.outOfZoneSignalFired
        when {
            compliance == ZoneCompliance.IN_ZONE -> {
                enteredTargetZone = true
                outOfZoneSignalFired = false  // reset so leaving again will signal again
            }
            enteredTargetZone && !outOfZoneSignalFired -> {
                triggerOutOfZoneFeedback()
                outOfZoneSignalFired = true
            }
        }
        _state.value = active.copy(
            currentBpm = bpm,
            zoneCompliance = compliance,
            enteredTargetZone = enteredTargetZone,
            outOfZoneSignalFired = outOfZoneSignalFired,
        )
    }

    fun pause() {
        val active = _state.value as? WorkoutState.Active ?: return
        _state.value = active.copy(isPaused = true)
    }

    fun resume() {
        val active = _state.value as? WorkoutState.Active ?: return
        _state.value = active.copy(isPaused = false)
    }

    fun end() {
        timerJob?.cancel()
        timerJob = null
        _state.value = WorkoutState.Finished
    }

    fun skipPhase() {
        val active = _state.value as? WorkoutState.Active ?: return
        advancePhase(active)
    }

    fun exitBlock() {
        val active = _state.value as? WorkoutState.Active ?: return
        val currentBlockId = active.current.blockId ?: return
        val exitIndex = (active.currentIndex + 1 until active.plan.size)
            .firstOrNull { active.plan[it].blockId != currentBlockId }
        triggerTransitionFeedback()
        if (exitIndex == null) {
            timerJob?.cancel()
            _state.value = WorkoutState.Finished
        } else {
            val nextPhase = active.plan[exitIndex]
            _state.value = active.copy(
                currentIndex = exitIndex,
                remainingSeconds = nextPhase.durationSeconds,
                zoneCompliance = checkZoneCompliance(active.currentBpm, nextPhase.targetZones),
                enteredTargetZone = false,
                outOfZoneSignalFired = false,
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val active = _state.value as? WorkoutState.Active ?: break
                if (active.isPaused) continue
                if (active.remainingSeconds <= 1) {
                    advancePhase(active)
                } else {
                    _state.value = active.copy(remainingSeconds = active.remainingSeconds - 1)
                }
            }
        }
    }

    private fun advancePhase(current: WorkoutState.Active) {
        triggerTransitionFeedback()
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.plan.size) {
            _state.value = WorkoutState.Finished
            timerJob?.cancel()
        } else {
            val nextPhase = current.plan[nextIndex]
            _state.value = current.copy(
                currentIndex = nextIndex,
                remainingSeconds = nextPhase.durationSeconds,
                zoneCompliance = checkZoneCompliance(current.currentBpm, nextPhase.targetZones),
                enteredTargetZone = false,
                outOfZoneSignalFired = false,
            )
        }
    }

    private fun checkZoneCompliance(bpm: Int?, zones: List<HrZone>): ZoneCompliance {
        val b = bpm ?: return ZoneCompliance.UNKNOWN
        if (zones.isEmpty()) return ZoneCompliance.UNKNOWN
        if (zones.any { b in it.bpmMin..it.bpmMax }) return ZoneCompliance.IN_ZONE
        return if (b < zones.minOf { it.bpmMin }) ZoneCompliance.TOO_LOW else ZoneCompliance.TOO_HIGH
    }

    private fun triggerTransitionFeedback() {
        vibrate(longArrayOf(0, 350))
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 80)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
            Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, 600)
        } catch (_: Exception) { }
    }

    private fun triggerOutOfZoneFeedback() {
        // Short double-pulse, distinct from the phase-transition single pulse
        vibrate(longArrayOf(0, 150, 100, 150))
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 70)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, 400)
        } catch (_: Exception) { }
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getApplication<Application>()
                .getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getApplication<Application>().getSystemService(Vibrator::class.java)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

private fun buildExecutionPlan(
    withItems: WorkoutTemplateWithItems,
    zoneMap: Map<Long, HrZone>,
): List<ExecutionPhase> {
    val result = mutableListOf<ExecutionPhase>()
    for (itemWithPhases in withItems.items.sortedBy { it.item.sortOrder }) {
        val item = itemWithPhases.item
        if (item.itemType == "PHASE") {
            result += ExecutionPhase(
                name = item.phaseName ?: "Phase",
                durationSeconds = item.durationSeconds ?: 0,
                targetZones = item.zoneIds?.toZoneIdList()?.mapNotNull { zoneMap[it] } ?: emptyList(),
                blockRepIndex = null,
                blockTotalReps = null,
            )
        } else {
            val repCount = item.repeatCount ?: 1
            val phases = itemWithPhases.blockPhases.sortedBy { it.sortOrder }
            repeat(repCount) { repIdx ->
                phases.forEach { phase ->
                    result += ExecutionPhase(
                        name = phase.phaseName,
                        durationSeconds = phase.durationSeconds,
                        targetZones = phase.zoneIds.toZoneIdList().mapNotNull { zoneMap[it] },
                        blockRepIndex = repIdx + 1,
                        blockTotalReps = repCount,
                        blockId = item.id,
                    )
                }
            }
        }
    }
    return result
}
