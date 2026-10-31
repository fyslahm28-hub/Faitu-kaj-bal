package com.example.ui.util

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * BroadcastReceiver for automated 5-Waqt Azan alarms, pre-waqt reminders (5/10/15 min before),
 * daily midnight rescheduling, stop-playback actions, and boot/time-change recovery.
 */
class PrayerAzanAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        PrayerAzanManager.initWithContext(context)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            ACTION_PRAYER_DAILY_RESCHEDULE,
            ACTION_PRAYER_WIDGET_TRANSITION_REFRESH -> {
                PrayerAzanManager.scheduleAllUpcomingPrayerAlarms(context)
                PrayerTimeWidgetManager.updateAllWidgetsAndLockScreen(context)
            }

            ACTION_STOP_AZAN_PLAYBACK -> {
                PrayerAzanManager.stopAzanPlayback(context)
            }

            ACTION_PRAYER_AZAN_ALARM -> {
                val waqtId = intent?.getStringExtra(EXTRA_WAQT_ID)
                val waqtKey = PrayerWaqtKey.fromId(waqtId)
                PrayerAzanManager.triggerScheduledWaqtAzan(context, waqtKey)
                PrayerAzanManager.scheduleAllUpcomingPrayerAlarms(context)
                PrayerTimeWidgetManager.updateAllWidgetsAndLockScreen(context)
            }

            ACTION_PRAYER_PRE_REMINDER_ALARM -> {
                val waqtId = intent?.getStringExtra(EXTRA_WAQT_ID)
                val waqtKey = PrayerWaqtKey.fromId(waqtId)
                PrayerAzanManager.triggerScheduledPreWaqtReminder(context, waqtKey)
                PrayerAzanManager.scheduleAllUpcomingPrayerAlarms(context)
                PrayerTimeWidgetManager.updateAllWidgetsAndLockScreen(context)
            }
        }
    }

    companion object {
        const val ACTION_PRAYER_AZAN_ALARM = "com.example.action.PRAYER_AZAN_ALARM"
        const val ACTION_PRAYER_PRE_REMINDER_ALARM = "com.example.action.PRAYER_PRE_REMINDER_ALARM"
        const val ACTION_STOP_AZAN_PLAYBACK = "com.example.action.STOP_AZAN_PLAYBACK"
        const val ACTION_PRAYER_DAILY_RESCHEDULE = "com.example.action.PRAYER_DAILY_RESCHEDULE"
        const val ACTION_PRAYER_WIDGET_TRANSITION_REFRESH = "com.example.action.PRAYER_WIDGET_TRANSITION_REFRESH"
        const val EXTRA_WAQT_ID = "extra_prayer_waqt_id"
        const val EXTRA_SOUND_TYPE = "extra_azan_sound_type"
    }
}

/**
 * Foreground-capable playback service that keeps the process alive while Azan audio is playing
 * when the app is minimized or the screen is off, and provides a 1-tap "আজান বন্ধ করুন" action.
 */
class AzanPlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            PrayerAzanManager.ensureChannelsCreated(this)
            val waqtKey = PrayerWaqtKey.fromId(intent?.getStringExtra(PrayerAzanAlarmReceiver.EXTRA_WAQT_ID))
            val soundType = AzanSoundType.fromId(intent?.getStringExtra(PrayerAzanAlarmReceiver.EXTRA_SOUND_TYPE))

            val openIntent = Intent(this, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(PrayerAzanManager.EXTRA_OPEN_PRAYER_SCREEN, true)
            }
            val stopIntent = Intent(this, PrayerAzanAlarmReceiver::class.java).apply {
                action = PrayerAzanAlarmReceiver.ACTION_STOP_AZAN_PLAYBACK
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val contentPending = PendingIntent.getActivity(
                this,
                PrayerAzanManager.AZAN_NOTIFICATION_ID,
                openIntent,
                pendingFlags
            )
            val stopPending = PendingIntent.getBroadcast(
                this,
                PrayerAzanManager.AZAN_NOTIFICATION_ID + 1,
                stopIntent,
                pendingFlags
            )

            val notif = NotificationCompat.Builder(this, PrayerAzanManager.AZAN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_qawmi_notification)
                .setContentTitle("🕌 ${waqtKey.nameBangla} নামাজের আজান চলছে")
                .setContentText("${soundType.titleBangla} • যেকোনো সময় বন্ধ করতে নিচে ট্যাপ করুন")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(contentPending)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "আজান বন্ধ করুন",
                    stopPending
                )
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    PrayerAzanManager.AZAN_NOTIFICATION_ID,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(PrayerAzanManager.AZAN_NOTIFICATION_ID, notif)
            }
        } catch (_: Throwable) {
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        super.onDestroy()
    }
}
