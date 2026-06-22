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
import dev.bri.polarphases.data.model.HrSample
import dev.bri.polarphases.data.model.HrZone
import dev.bri.polarphases.data.model.SessionPhaseRecord
import dev.bri.polarphases.data.model.WorkoutSession
import dev.bri.polarphases.data.model.WorkoutTemplateWithItems
import dev.bri.polarphases.data.model.ZoneSnapshot
import dev.bri.polarphases.repository.toZoneIdList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExecutionPhase(
    val name: String,
    val durationSeconds: Int,
    val targetZones: List<HrZone>,
    val blockRepIndex: Int?,
    val blockTotalReps: Int?,
    val blockId: Long? = null,
)

sealed class PlanGroup {
    abstract val firstIndex: Int
    abstract val lastIndex: Int

    data class StandalonePhase(
        override val firstIndex: Int,
        val name: String,
        val durationSeconds: Int,
        val targetZones: List<HrZone>,
    ) : PlanGroup() {
        override val lastIndex: Int = firstIndex
    }

    data class BlockGroup(
        override val firstIndex: Int,
        override val lastIndex: Int,
        val blockId: Long,
        val totalReps: Int,
        val phaseNames: List<String>,
        val phaseDurations: List<Int>,
        val phaseZones: List<List<HrZone>>,
    ) : PlanGroup()
}

enum class ZoneCompliance { IN_ZONE, TOO_HIGH, TOO_LOW, UNKNOWN }

sealed class WorkoutState {
    data object NotStarted : WorkoutState()
    data class Active(
        val templateName: String,
        val plan: List<ExecutionPhase>,
        val planGroups: List<PlanGroup>,
        val totalDurationSeconds: Int,
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
        val elapsedSeconds: Int get() {
            val done = plan.take(currentIndex).sumOf { it.durationSeconds }
            return done + maxOf(0, plan[currentIndex].durationSeconds - remainingSeconds)
        }
        val overallProgress: Float get() =
            if (totalDurationSeconds > 0)
                (elapsedSeconds.toFloat() / totalDurationSeconds).coerceIn(0f, 1f)
            else 0f
    }
    data object Finished : WorkoutState()
}

class WorkoutExecutionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PolarPhasesApp
    private val templateRepo = app.templateRepository
    private val zoneRepo = app.zoneRepository
    private val sessionRepo = app.sessionRepository

    private val _state = MutableStateFlow<WorkoutState>(WorkoutState.NotStarted)
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private var timerJob: Job? = null

    // Session recording state — reset on each loadAndStart
    private var sessionStartMs = 0L
    private var phaseStartMs = 0L
    private var lastHrSampleMs = 0L
    private var isSaving = false
    private val recordedHrSamples = mutableListOf<HrSample>()
    private val recordedPhaseRecords = mutableListOf<SessionPhaseRecord>()
    private var phaseRecordSortOrder = 0
    private var capturedZones: List<HrZone> = emptyList()

    fun loadAndStart(templateId: Long) {
        viewModelScope.launch {
            val withItems = templateRepo.observeWithItems(templateId).first() ?: return@launch
            val allZones = zoneRepo.observeZones().first()
            val zoneMap = allZones.associateBy { it.id }
            val plan = buildExecutionPlan(withItems, zoneMap)
            if (plan.isEmpty()) return@launch

            // Initialize recording for this session
            val now = System.currentTimeMillis()
            sessionStartMs = now
            phaseStartMs = now
            lastHrSampleMs = 0L
            isSaving = false
            recordedHrSamples.clear()
            recordedPhaseRecords.clear()
            phaseRecordSortOrder = 0
            capturedZones = allZones

            _state.value = WorkoutState.Active(
                templateName = withItems.template.name,
                plan = plan,
                planGroups = buildPlanGroups(plan),
                totalDurationSeconds = plan.sumOf { it.durationSeconds },
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

        // Record HR sample at most once per second
        if (bpm != null) {
            val now = System.currentTimeMillis()
            if (now - lastHrSampleMs >= 1_000L) {
                recordedHrSamples += HrSample(
                    sessionId = 0,
                    elapsedMs = now - sessionStartMs,
                    bpm = bpm,
                )
                lastHrSampleMs = now
            }
        }

        if (bpm == null) {
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
                outOfZoneSignalFired = false
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
        if (isSaving) return
        timerJob?.cancel()
        timerJob = null
        val active = _state.value as? WorkoutState.Active ?: run {
            _state.value = WorkoutState.Finished
            return
        }
        isSaving = true
        val elapsed = ((System.currentTimeMillis() - phaseStartMs) / 1000).toInt()
        recordPhaseExecution(active.current, "SKIPPED", elapsed)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { saveSession(active, earlyExit = true) }
            _state.value = WorkoutState.Finished
        }
    }

    fun skipPhase() {
        val active = _state.value as? WorkoutState.Active ?: return
        advancePhase(active, status = "SKIPPED")
    }

    fun exitBlock() {
        val active = _state.value as? WorkoutState.Active ?: return
        if (isSaving) return
        val currentBlockId = active.current.blockId ?: return

        val elapsed = ((System.currentTimeMillis() - phaseStartMs) / 1000).toInt()
        recordPhaseExecution(active.current, "SKIPPED", elapsed)

        val exitIndex = (active.currentIndex + 1 until active.plan.size)
            .firstOrNull { active.plan[it].blockId != currentBlockId }

        triggerTransitionFeedback()
        if (exitIndex == null) {
            isSaving = true
            timerJob?.cancel()
            viewModelScope.launch {
                withContext(Dispatchers.IO) { saveSession(active, earlyExit = true) }
                _state.value = WorkoutState.Finished
            }
        } else {
            phaseStartMs = System.currentTimeMillis()
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

    private fun advancePhase(current: WorkoutState.Active, status: String = "COMPLETED") {
        if (isSaving) return
        val actualDuration = if (status == "COMPLETED") {
            current.current.durationSeconds
        } else {
            ((System.currentTimeMillis() - phaseStartMs) / 1000).toInt()
        }
        recordPhaseExecution(current.current, status, actualDuration)

        triggerTransitionFeedback()
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.plan.size) {
            isSaving = true
            timerJob?.cancel()
            viewModelScope.launch {
                withContext(Dispatchers.IO) { saveSession(current, earlyExit = false) }
                _state.value = WorkoutState.Finished
            }
        } else {
            phaseStartMs = System.currentTimeMillis()
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

    private fun recordPhaseExecution(phase: ExecutionPhase, status: String, actualDurationSeconds: Int) {
        recordedPhaseRecords += SessionPhaseRecord(
            sessionId = 0,
            sortOrder = phaseRecordSortOrder++,
            phaseName = phase.name,
            plannedDurationSeconds = phase.durationSeconds,
            actualDurationSeconds = actualDurationSeconds,
            completionStatus = status,
            blockId = phase.blockId,
            blockRepIndex = phase.blockRepIndex,
            blockTotalReps = phase.blockTotalReps,
        )
    }

    private suspend fun saveSession(active: WorkoutState.Active, earlyExit: Boolean) {
        val endMs = System.currentTimeMillis()
        val completedCount = recordedPhaseRecords.count { it.completionStatus == "COMPLETED" }
        val session = WorkoutSession(
            templateName = active.templateName,
            startedAt = sessionStartMs,
            endedAt = endMs,
            endReason = if (earlyExit) "EARLY_EXIT" else "COMPLETED",
            totalPhasesPlanned = active.plan.size,
            totalPhasesCompleted = completedCount,
        )
        val snapshots = capturedZones.map { zone ->
            ZoneSnapshot(
                sessionId = 0,
                name = zone.name,
                colorArgb = zone.colorArgb,
                bpmMin = zone.bpmMin,
                bpmMax = zone.bpmMax,
                sortOrder = zone.sortOrder,
            )
        }
        sessionRepo.save(
            session = session,
            zoneSnapshots = snapshots,
            hrSamples = recordedHrSamples.toList(),
            phaseRecords = recordedPhaseRecords.toList(),
        )
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

private fun buildPlanGroups(plan: List<ExecutionPhase>): List<PlanGroup> {
    val groups = mutableListOf<PlanGroup>()
    var i = 0
    while (i < plan.size) {
        val phase = plan[i]
        if (phase.blockId == null) {
            groups += PlanGroup.StandalonePhase(
                firstIndex = i,
                name = phase.name,
                durationSeconds = phase.durationSeconds,
                targetZones = phase.targetZones,
            )
            i++
        } else {
            val blockId = phase.blockId
            val blockStart = i
            while (i < plan.size && plan[i].blockId == blockId) i++
            val blockEnd = i - 1
            val totalReps = phase.blockTotalReps ?: 1
            val phaseCountPerRep = (blockEnd - blockStart + 1) / totalReps
            val firstRepPhases = plan.subList(blockStart, blockStart + phaseCountPerRep)
            groups += PlanGroup.BlockGroup(
                firstIndex = blockStart,
                lastIndex = blockEnd,
                blockId = blockId,
                totalReps = totalReps,
                phaseNames = firstRepPhases.map { it.name },
                phaseDurations = firstRepPhases.map { it.durationSeconds },
                phaseZones = firstRepPhases.map { it.targetZones },
            )
        }
    }
    return groups
}
