package dev.bri.polarbear.ui.screen

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.bri.polarbear.data.model.HrSample
import dev.bri.polarbear.data.model.SessionPhaseRecord
import dev.bri.polarbear.data.model.WorkoutSessionWithDetails
import dev.bri.polarbear.data.model.ZoneSnapshot
import dev.bri.polarbear.viewmodel.WorkoutSessionDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionDetailScreen(
    viewModel: WorkoutSessionDetailViewModel,
    sessionId: Long,
    onBack: () -> Unit,
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val details by viewModel.details.collectAsState()
    val exportFilePath by viewModel.exportFilePath.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exportFilePath) {
        val path = exportFilePath ?: return@LaunchedEffect
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.garmin.tcx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export TCX"))
        viewModel.clearExportRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.session?.templateName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (details != null) {
                        TextButton(onClick = { viewModel.exportTcx(context) }) {
                            Text("Export TCX")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (details == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            SessionDetailContent(details = details!!, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun SessionDetailContent(
    details: WorkoutSessionWithDetails,
    modifier: Modifier = Modifier,
) {
    val durationMs = details.session.endedAt - details.session.startedAt
    val blockSummaries = remember(details.phaseRecords) { computeBlockSummaries(details.phaseRecords) }
    val earlyExitBlocks = remember(blockSummaries) {
        blockSummaries.values.filter { it.repsCompleted < it.totalRepsPlanned }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatDetailDate(details.session.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = buildString {
                    append(formatSessionDuration(durationMs))
                    append(" · ")
                    append("${details.session.totalPhasesCompleted} of ${details.session.totalPhasesPlanned} phases completed")
                    if (details.session.endReason == "EARLY_EXIT") append(" (early exit)")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            HrChart(
                hrSamples = details.hrSamples,
                zoneSnapshots = details.zoneSnapshots,
                sessionDurationMs = durationMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }

        if (details.zoneSnapshots.isNotEmpty()) {
            item {
                ZoneLegend(details.zoneSnapshots)
            }
        }

        if (earlyExitBlocks.isNotEmpty()) {
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Block Notes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    earlyExitBlocks.forEach { summary ->
                        Text(
                            text = "${summary.repsCompleted} of ${summary.totalRepsPlanned} repetitions completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HrChart(
    hrSamples: List<HrSample>,
    zoneSnapshots: List<ZoneSnapshot>,
    sessionDurationMs: Long,
    modifier: Modifier = Modifier,
) {
    if (hrSamples.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "No heart rate data recorded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val sortedSamples = remember(hrSamples) { hrSamples.sortedBy { it.elapsedMs } }
    val sortedZones = remember(zoneSnapshots) { zoneSnapshots.sortedBy { it.bpmMin } }

    val yMin = remember(sortedZones, sortedSamples) {
        val zoneLow = sortedZones.firstOrNull()?.bpmMin ?: 40
        val sampleLow = sortedSamples.minOfOrNull { it.bpm } ?: 40
        (minOf(zoneLow, sampleLow) - 5).coerceAtLeast(0)
    }
    val yMax = remember(sortedZones, sortedSamples) {
        val zoneHigh = sortedZones.lastOrNull()?.bpmMax ?: 200
        val sampleHigh = sortedSamples.maxOfOrNull { it.bpm } ?: 200
        maxOf(zoneHigh, sampleHigh) + 5
    }
    val xMax = if (sessionDurationMs > 0L) sessionDurationMs else 1L

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun bpmToY(bpm: Int): Float =
            h - ((bpm - yMin).toFloat() / (yMax - yMin)) * h

        fun msToX(ms: Long): Float =
            (ms.toFloat() / xMax) * w

        sortedZones.forEach { zone ->
            val yTop = bpmToY(zone.bpmMax).coerceIn(0f, h)
            val yBottom = bpmToY(zone.bpmMin).coerceIn(0f, h)
            if (yBottom > yTop) {
                drawRect(
                    color = Color(zone.colorArgb).copy(alpha = 0.22f),
                    topLeft = Offset(0f, yTop),
                    size = Size(w, yBottom - yTop),
                )
            }
        }

        if (sortedSamples.size >= 2) {
            val path = Path()
            sortedSamples.forEachIndexed { idx, sample ->
                val x = msToX(sample.elapsedMs)
                val y = bpmToY(sample.bpm)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun ZoneLegend(zones: List<ZoneSnapshot>) {
    val sorted = remember(zones) { zones.sortedBy { it.sortOrder } }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        sorted.forEach { zone ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(zone.colorArgb), CircleShape)
                )
                Text(
                    text = "${zone.name}  ${zone.bpmMin}–${zone.bpmMax} bpm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class BlockSummary(val totalRepsPlanned: Int, val repsCompleted: Int)

private fun computeBlockSummaries(records: List<SessionPhaseRecord>): Map<Long, BlockSummary> =
    records
        .filter { it.blockId != null }
        .groupBy { it.blockId!! }
        .mapValues { (_, phases) ->
            val totalReps = phases.firstOrNull()?.blockTotalReps ?: 0
            val completedReps = phases
                .groupBy { it.blockRepIndex }
                .count { (_, repPhases) -> repPhases.all { it.completionStatus == "COMPLETED" } }
            BlockSummary(totalReps, completedReps)
        }

private fun formatDetailDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("EEEE d MMMM yyyy · HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

private fun formatSessionDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
