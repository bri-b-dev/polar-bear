package dev.bri.polarbear

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.bri.polarbear.data.model.HrZone
import dev.bri.polarbear.viewmodel.ExecutionPhase
import dev.bri.polarbear.viewmodel.WorkoutState
import dev.bri.polarbear.viewmodel.ZoneCompliance

/**
 * Status-bar presentation of a running workout:
 *  - one silent, ongoing notification with current zone + remaining time
 *  - short heads-up alerts on phase change (what comes next) and when leaving the target zone
 */
object WorkoutNotifier {
    const val ONGOING_ID = 1
    private const val ALERT_ID = 1002
    private const val CHANNEL_ONGOING = "workout_ongoing"
    private const val CHANNEL_ALERT = "workout_alert"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "Workout status", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Current zone and remaining time" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "Workout alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Phase changes and zone warnings" }
        )
    }

    fun buildOngoing(context: Context, state: WorkoutState.Active? = null): Notification {
        ensureChannels(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent(context))
        if (state == null) {
            return builder
                .setContentTitle(context.getString(R.string.workout_notification_title))
                .setContentText(context.getString(R.string.workout_notification_text))
                .build()
        }
        val phase = state.current
        val next = state.plan.getOrNull(state.currentIndex + 1)
        val time = formatMinSec(state.remainingSeconds) + if (state.isPaused) " (paused)" else ""
        val bpm = state.currentBpm?.let { "$it bpm" } ?: "-- bpm"
        val nextText = next?.let { "Next: ${it.name}" } ?: "Last phase"
        return builder
            .setContentTitle("${zoneLabel(phase.targetZones)} · $time left")
            .setContentText("${phase.name} · $bpm · $nextText")
            .setSubText(state.templateName)
            .build()
    }

    fun update(context: Context, state: WorkoutState.Active) {
        notify(context, ONGOING_ID, buildOngoing(context, state))
    }

    fun phaseChanged(context: Context, phase: ExecutionPhase, next: ExecutionPhase?) {
        val text = buildString {
            append(zoneLabel(phase.targetZones)).append(" · ").append(formatMinSec(phase.durationSeconds))
            if (next != null) append(" — then ${next.name}")
        }
        alert(context, "Now: ${phase.name}", text)
    }

    fun outOfZone(context: Context, compliance: ZoneCompliance, zones: List<HrZone>) {
        val title = if (compliance == ZoneCompliance.TOO_HIGH) "▲ Heart rate too high" else "▼ Heart rate too low"
        alert(context, title, "Target: ${zoneLabel(zones)}")
    }

    fun cancelAll(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(ONGOING_ID)
        nm.cancel(ALERT_ID)
    }

    private fun alert(context: Context, title: String, text: String) {
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(8_000)
            .setContentIntent(openAppIntent(context))
            .build()
        notify(context, ALERT_ID, n)
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun zoneLabel(zones: List<HrZone>): String =
        if (zones.isEmpty()) "No target zone" else zones.joinToString(" / ") { it.name }

    private fun formatMinSec(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
