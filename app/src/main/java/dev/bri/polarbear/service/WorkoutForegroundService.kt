package dev.bri.polarbear.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.bri.polarbear.WorkoutNotifier

/**
 * Keeps the app process alive while a workout is running, so the phase timer,
 * vibration feedback and BLE connection continue when the app is in the
 * background (e.g. while watching something else). Without this, Android
 * freezes the cached process and the viewModelScope timer stops ticking.
 */
class WorkoutForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = WorkoutNotifier.buildOngoing(this)
        ServiceCompat.startForeground(
            this,
            WorkoutNotifier.ONGOING_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )

        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PolarBear:Workout")
                .apply { acquire(WAKE_LOCK_TIMEOUT_MS) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // Safety net so the wake lock can never leak indefinitely
        private const val WAKE_LOCK_TIMEOUT_MS = 4 * 60 * 60 * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WorkoutForegroundService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WorkoutForegroundService::class.java))
        }
    }
}
