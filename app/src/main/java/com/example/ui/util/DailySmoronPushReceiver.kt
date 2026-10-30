package com.example.ui.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Background receiver for automated daily "আজকের স্মরণ" Push Notifications and FCM data payloads.
 * Works even when the app is closed, enforces at most 1 notification per day per user,
 * syncs delivery history with Firebase Firestore, and schedules the next daily cycle.
 */
class DailySmoronPushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                if (AjkerSmoronHelper.isNotificationEnabled(context)) {
                    val (hour, min) = AjkerSmoronHelper.getPreferredNotificationTime(context)
                    AjkerSmoronHelper.scheduleDailyPushAlarm(context, hour, min)
                }
            }

            ACTION_DAILY_SMORON_ALARM,
            ACTION_FCM_SMORON_PUSH,
            "com.google.android.c2dm.intent.RECEIVE" -> {
                handleDailySmoronPushTrigger(context, intent)
                if (AjkerSmoronHelper.isNotificationEnabled(context)) {
                    val (hour, min) = AjkerSmoronHelper.getPreferredNotificationTime(context)
                    AjkerSmoronHelper.scheduleDailyPushAlarm(context, hour, min)
                }
            }
        }
    }

    private fun handleDailySmoronPushTrigger(context: Context, intent: Intent?) {
        if (!AjkerSmoronHelper.isNotificationEnabled(context)) return
        if (!QawmiNotificationHelper.hasNotificationPermission(context)) return

        val authUid = try {
            FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        } catch (_: Exception) {
            ""
        }
        val effectiveUserId = authUid.ifBlank {
            AjkerSmoronHelper.getActiveUserId(context).ifBlank { "default_user" }
        }
        val todayDateKey = AjkerSmoronHelper.currentBangladeshDateKey()

        // Enforce strict max 1 notification per day per user (no duplicate daily messages)
        if (AjkerSmoronHelper.hasPushBeenSentForDate(context, effectiveUserId, todayDateKey)) {
            return
        }

        val payloadReminderId = intent?.getStringExtra(EXTRA_REMINDER_ID).orEmpty().trim()
        val reminder = AjkerSmoronHelper.findReminderById(payloadReminderId)
            ?: AjkerSmoronHelper.getOrAssignTodayReminder(
                context = context,
                userId = effectiveUserId,
                dateKey = todayDateKey
            )

        val delivered = QawmiNotificationHelper.deliverDailyAjkerSmoronPushNotification(
            context = context,
            userId = effectiveUserId,
            reminder = reminder,
            dateKey = todayDateKey
        )

        if (delivered && authUid.isNotBlank()) {
            syncPushDeliveryToFirestore(context, authUid, todayDateKey, reminder.id)
        }
    }

    private fun syncPushDeliveryToFirestore(
        context: Context,
        userId: String,
        dateKey: String,
        reminderId: String
    ) {
        try {
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            val db = com.example.data.repository.getConfiguredFirestoreInstance(databaseId)
            val readsCol = db.collection("users").document(userId).collection("notification_reads")

            val pushReceiptId = "${AjkerSmoronHelper.RECEIPT_PREFIX_PUSH_SENT}${dateKey}__$reminderId"
            val histReceiptId = "${AjkerSmoronHelper.RECEIPT_PREFIX_HISTORY}$reminderId"
            val dayReceiptId = "${AjkerSmoronHelper.RECEIPT_PREFIX_DAY_ASSIGN}${dateKey}__$reminderId"

            val batch = db.batch()
            listOf(pushReceiptId, histReceiptId, dayReceiptId).forEach { docId ->
                val ref = readsCol.document(docId)
                batch.set(
                    ref,
                    mapOf(
                        "userId" to userId,
                        "notificationId" to docId,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit()
        } catch (_: Exception) {
            // Firestore offline cache or next foreground sync will reconcile
        }
    }

    companion object {
        const val ACTION_DAILY_SMORON_ALARM = "com.example.action.DAILY_SMORON_ALARM"
        const val ACTION_FCM_SMORON_PUSH = "com.example.action.FCM_SMORON_PUSH"
        const val EXTRA_REMINDER_ID = "smoron_reminder_id"
        const val ALARM_REQUEST_CODE = 786_101
    }
}
