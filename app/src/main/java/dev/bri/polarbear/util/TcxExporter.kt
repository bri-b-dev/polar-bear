package dev.bri.polarbear.util

import dev.bri.polarbear.data.model.WorkoutSessionWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TcxExporter {

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).also {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun generate(data: WorkoutSessionWithDetails): String {
        val session = data.session
        val startTime = isoFmt.format(Date(session.startedAt))
        val totalSeconds = (session.endedAt - session.startedAt) / 1000.0
        val samples = data.hrSamples.sortedBy { it.elapsedMs }
        val avgBpm = if (samples.isNotEmpty()) samples.map { it.bpm }.average().toInt() else 0
        val maxBpm = samples.maxOfOrNull { it.bpm } ?: 0

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine(
                """<TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2
    http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd">"""
            )
            appendLine("  <Activities>")
            appendLine("    <Activity Sport=\"Other\">")
            appendLine("      <Id>$startTime</Id>")
            appendLine("      <Lap StartTime=\"$startTime\">")
            appendLine("        <TotalTimeSeconds>$totalSeconds</TotalTimeSeconds>")
            appendLine("        <Calories>0</Calories>")
            appendLine("        <AverageHeartRateBpm><Value>$avgBpm</Value></AverageHeartRateBpm>")
            appendLine("        <MaximumHeartRateBpm><Value>$maxBpm</Value></MaximumHeartRateBpm>")
            appendLine("        <Intensity>Active</Intensity>")
            appendLine("        <TriggerMethod>Manual</TriggerMethod>")
            appendLine("        <Track>")
            samples.forEach { sample ->
                val time = isoFmt.format(Date(session.startedAt + sample.elapsedMs))
                appendLine("          <Trackpoint>")
                appendLine("            <Time>$time</Time>")
                appendLine("            <HeartRateBpm><Value>${sample.bpm}</Value></HeartRateBpm>")
                appendLine("          </Trackpoint>")
            }
            appendLine("        </Track>")
            appendLine("      </Lap>")
            appendLine("      <Notes>${escapeXml(session.templateName)}</Notes>")
            appendLine("    </Activity>")
            appendLine("  </Activities>")
            append("</TrainingCenterDatabase>")
        }
    }

    private fun escapeXml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
