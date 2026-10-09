package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * v69: background-music keep-alive.
 *
 * This service does NOT play anything itself. The audio comes from the page
 * inside the WebView. Its only job is to keep this app's process alive, and
 * show the notification Android requires, while the user has "Background
 * music" switched on in Settings and something is actually playing.
 *
 * Deliberate design points:
 *  - It is only ever started by MainActivity, and only when the user has the
 *    setting on. It is never started on its own.
 *  - START_NOT_STICKY: if the system kills it, it must NOT come back.
 *  - MainActivity stops it the moment playback ends, and in onDestroy, so it
 *    dies with the app exactly like everything else here.
 */
class PlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "pulse_horizon_playback"
        const val NOTIFICATION_ID = 4711
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) {
            stopSelf()
        }
        // Never resurrect after a kill - the app promises to leave nothing behind.
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Background music",
                    NotificationManager.IMPORTANCE_LOW
                )
                ch.description = "Shown only while audio keeps playing in the background"
                ch.setShowBadge(false)
                mgr.createNotificationChannel(ch)
            }
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(): Notification {
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(this, 0, open, flags)

        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID)
        else
            @Suppress("DEPRECATION") Notification.Builder(this)

        return b.setContentTitle("Pulse Horizon")
            .setContentText("Audio is playing in the background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
