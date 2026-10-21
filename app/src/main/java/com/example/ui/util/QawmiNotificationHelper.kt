package com.example.ui.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppNotification

object QawmiNotificationHelper {
    const val CHANNEL_ID = "qawmi_khedmot_channel"
    private const val CHANNEL_NAME = "কওমি কর্মসংস্থান ইত্তেলা"
    private const val CHANNEL_DESC = "নতুন নিয়োগ বিজ্ঞপ্তি, খেদমতের আবেদন, পয়গাম ও অ্যাডমিন ঘোষণার নোটিফিকেশন"
    private const val PREFS_PREFIX = "qawmi_push_delivered_"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Delivers real Android system tray notifications for newly arrived unread notifications
     * that belong to other senders (`senderId != currentUserId`).
     */
    fun deliverNewUnreadPushNotifications(
        context: Context,
        currentUserId: String,
        notifications: List<AppNotification>
    ) {
        if (currentUserId.isBlank()) return
        ensureChannelCreated(context)

        val prefs = context.getSharedPreferences("$PREFS_PREFIX$currentUserId", Context.MODE_PRIVATE)
        val deliveredSet = prefs.getStringSet("delivered_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        val isInitialized = prefs.getBoolean("initialized", false)

        if (!isInitialized) {
            // On very first initialization on this device, record already-read notifications
            // and deliver at most the latest 1 recent unread notification if any exists.
            val unreadFromOthers = notifications.filter { !it.isRead && it.senderId != currentUserId }
            notifications.forEach { deliveredSet.add(it.id) }
            prefs.edit()
                .putStringSet("delivered_ids", deliveredSet)
                .putBoolean("initialized", true)
                .apply()

            val newestUnread = unreadFromOthers.firstOrNull()
            if (newestUnread != null && hasNotificationPermission(context)) {
                showSystemNotification(context, newestUnread)
            }
            return
        }

        var updated = false
        for (notif in notifications) {
            if (notif.id.isBlank()) continue
            if (notif.senderId == currentUserId) {
                if (deliveredSet.add(notif.id)) updated = true
                continue
            }
            if (!notif.isRead && !deliveredSet.contains(notif.id)) {
                deliveredSet.add(notif.id)
                updated = true
                if (hasNotificationPermission(context)) {
                    showSystemNotification(context, notif)
                }
            } else if (notif.isRead && !deliveredSet.contains(notif.id)) {
                deliveredSet.add(notif.id)
                updated = true
            }
        }

        if (updated) {
            prefs.edit().putStringSet("delivered_ids", deliveredSet).apply()
        }
    }

    private fun showSystemNotification(context: Context, notification: AppNotification) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_notifications", true)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notification.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission may have been revoked at runtime
        }
    }
}
