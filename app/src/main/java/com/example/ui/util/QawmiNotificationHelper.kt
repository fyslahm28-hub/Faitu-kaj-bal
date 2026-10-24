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
import com.example.data.model.ChatGroup
import com.example.data.model.DirectMessage
import kotlin.math.abs

object QawmiNotificationHelper {
    const val CHANNEL_ID = "qawmi_khedmot_channel"
    private const val CHANNEL_NAME = "কওমি কর্মসংস্থান ইত্তেলা"
    private const val CHANNEL_DESC = "নতুন নিয়োগ বিজ্ঞপ্তি, খেদমতের আবেদন ও অ্যাডমিন ঘোষণার নোটিফিকেশন"

    const val MESSAGE_CHANNEL_ID = "qawmi_paygam_messages_channel"
    private const val MESSAGE_CHANNEL_NAME = "পয়গাম ও ইনবক্স বার্তা"
    private const val MESSAGE_CHANNEL_DESC = "ব্যক্তিগত ও গ্রুপ চ্যাটের নতুন মেসেজের তাৎক্ষণিক মোবাইল নোটিফিকেশন"

    const val SMORON_CHANNEL_ID = "qawmi_ajker_smoron_channel"
    private const val SMORON_CHANNEL_NAME = "আজকের স্মরণ (দৈনিক ইসলামিক নসিহত)"
    private const val SMORON_CHANNEL_DESC = "প্রতিদিনের দরুদ, জিকির, ইস্তিগফার, কুরআন-হাদিস ও নেক আমলের স্মরণবার্তা"
    const val SMORON_NOTIFICATION_ID = 786_500

    const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
    const val EXTRA_OPEN_CHAT_PARTNER_ID = "open_chat_partner_id"
    const val EXTRA_OPEN_CHAT_PARTNER_NAME = "open_chat_partner_name"
    const val EXTRA_OPEN_AJKER_SMORON = "open_ajker_smoron"
    const val EXTRA_AJKER_SMORON_ID = "ajker_smoron_id"

    private const val PREFS_PREFIX = "qawmi_push_delivered_"
    private const val MSG_PREFS_PREFIX = "qawmi_msg_push_delivered_"
    private const val SETTINGS_PREFS = "qawmi_notification_settings"
    private const val KEY_HIDE_MESSAGE_PREVIEW = "hide_message_preview"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val generalChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                MESSAGE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = MESSAGE_CHANNEL_DESC
                enableVibration(true)
            }
            val smoronChannel = NotificationChannel(
                SMORON_CHANNEL_ID,
                SMORON_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = SMORON_CHANNEL_DESC
                enableVibration(true)
            }
            manager.createNotificationChannel(generalChannel)
            manager.createNotificationChannel(messageChannel)
            manager.createNotificationChannel(smoronChannel)
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

    fun isMessagePreviewHidden(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HIDE_MESSAGE_PREVIEW, false)
    }

    fun setMessagePreviewHidden(context: Context, hidden: Boolean) {
        val prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HIDE_MESSAGE_PREVIEW, hidden).apply()
    }

    /**
     * Delivers real Android system tray notifications for newly arrived general notifications
     * (strictly excluding `MESSAGE` type, which is handled by [deliverNewUnreadMessageNotifications]).
     */
    fun deliverNewUnreadPushNotifications(
        context: Context,
        currentUserId: String,
        notifications: List<AppNotification>
    ) {
        if (currentUserId.isBlank()) return
        ensureChannelCreated(context)

        val generalNotifications = notifications.filter { !it.type.equals("MESSAGE", ignoreCase = true) }
        val prefs = context.getSharedPreferences("$PREFS_PREFIX$currentUserId", Context.MODE_PRIVATE)
        val deliveredSet = prefs.getStringSet("delivered_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        val isInitialized = prefs.getBoolean("initialized", false)

        if (!isInitialized) {
            val unreadFromOthers = generalNotifications.filter { !it.isRead && it.senderId != currentUserId }
            generalNotifications.forEach { deliveredSet.add(it.id) }
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
        for (notif in generalNotifications) {
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

    /**
     * Delivers real Android system status bar notifications for newly arrived unread Paygam/Inbox
     * messages (both 1-on-1 and Group chats), and automatically cancels notifications for
     * conversations whose unread messages have been read.
     */
    fun deliverNewUnreadMessageNotifications(
        context: Context,
        currentUserId: String,
        messages: List<DirectMessage>,
        chatGroups: List<ChatGroup> = emptyList(),
        activeConversationKey: String? = null
    ) {
        if (currentUserId.isBlank()) return
        ensureChannelCreated(context)

        val prefs = context.getSharedPreferences("$MSG_PREFS_PREFIX$currentUserId", Context.MODE_PRIVATE)
        val deliveredSet = prefs.getStringSet("delivered_msg_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        val activeNotifiedConvKeys = prefs.getStringSet("active_conv_keys", emptySet())?.toMutableSet() ?: mutableSetOf()
        val isInitialized = prefs.getBoolean("msg_initialized", false)

        val unreadByConversation = messages
            .filter { it.isUnreadForUser(currentUserId) && !it.isDeletedForEveryone }
            .groupBy { it.resolvedConversationKey(currentUserId) }

        // Automatically dismiss system status bar notifications for any conversation that now has 0 unread messages
        val readConvKeys = activeNotifiedConvKeys.filter { key ->
            unreadByConversation[key].isNullOrEmpty() || key == activeConversationKey
        }
        if (readConvKeys.isNotEmpty()) {
            readConvKeys.forEach { key ->
                cancelConversationNotificationInternal(context, key)
                activeNotifiedConvKeys.remove(key)
            }
            prefs.edit().putStringSet("active_conv_keys", activeNotifiedConvKeys).apply()
        }

        if (!isInitialized) {
            messages.forEach { msg ->
                if (msg.id.isNotBlank()) {
                    deliveredSet.add(msg.id)
                }
            }
            // Notify at most the latest unread conversation on initial launch if not currently viewing it
            val latestUnreadEntry = unreadByConversation.entries
                .firstOrNull { (convKey, _) -> convKey.isNotBlank() && convKey != activeConversationKey }
            if (latestUnreadEntry != null && hasNotificationPermission(context)) {
                val (convKey, convUnreadMsgs) = latestUnreadEntry
                showConversationMessageNotification(
                    context = context,
                    conversationKey = convKey,
                    unreadMessages = convUnreadMsgs,
                    chatGroups = chatGroups
                )
                activeNotifiedConvKeys.add(convKey)
            }
            prefs.edit()
                .putStringSet("delivered_msg_ids", deliveredSet)
                .putStringSet("active_conv_keys", activeNotifiedConvKeys)
                .putBoolean("msg_initialized", true)
                .apply()
            return
        }

        var updated = false
        val conversationsToNotify = mutableSetOf<String>()

        for (msg in messages) {
            if (msg.id.isBlank()) continue
            if (msg.senderId == currentUserId || !msg.isUnreadForUser(currentUserId) || msg.isDeletedForEveryone) {
                if (deliveredSet.add(msg.id)) updated = true
                continue
            }
            val convKey = msg.resolvedConversationKey(currentUserId)
            if (convKey == activeConversationKey) {
                if (deliveredSet.add(msg.id)) updated = true
                continue
            }
            if (!deliveredSet.contains(msg.id)) {
                deliveredSet.add(msg.id)
                updated = true
                if (convKey.isNotBlank()) {
                    conversationsToNotify.add(convKey)
                }
            }
        }

        if (hasNotificationPermission(context)) {
            for (convKey in conversationsToNotify) {
                val convUnread = unreadByConversation[convKey].orEmpty()
                if (convUnread.isNotEmpty()) {
                    showConversationMessageNotification(
                        context = context,
                        conversationKey = convKey,
                        unreadMessages = convUnread,
                        chatGroups = chatGroups
                    )
                    if (activeNotifiedConvKeys.add(convKey)) {
                        updated = true
                    }
                }
            }
        }

        if (updated) {
            prefs.edit()
                .putStringSet("delivered_msg_ids", deliveredSet)
                .putStringSet("active_conv_keys", activeNotifiedConvKeys)
                .apply()
        }
    }

    fun cancelConversationNotification(context: Context, conversationKey: String, currentUserId: String = "") {
        if (conversationKey.isBlank()) return
        cancelConversationNotificationInternal(context, conversationKey)
        if (currentUserId.isNotBlank()) {
            val prefs = context.getSharedPreferences("$MSG_PREFS_PREFIX$currentUserId", Context.MODE_PRIVATE)
            val activeNotifiedConvKeys = prefs.getStringSet("active_conv_keys", emptySet())?.toMutableSet() ?: mutableSetOf()
            if (activeNotifiedConvKeys.remove(conversationKey)) {
                prefs.edit().putStringSet("active_conv_keys", activeNotifiedConvKeys).apply()
            }
        }
    }

    private fun cancelConversationNotificationInternal(context: Context, conversationKey: String) {
        try {
            NotificationManagerCompat.from(context).cancel(conversationNotificationId(conversationKey))
        } catch (_: SecurityException) {
        }
    }

    private fun conversationNotificationId(conversationKey: String): Int {
        return 200_000 + abs(conversationKey.hashCode() % 700_000)
    }

    private fun showConversationMessageNotification(
        context: Context,
        conversationKey: String,
        unreadMessages: List<DirectMessage>,
        chatGroups: List<ChatGroup>
    ) {
        if (!hasNotificationPermission(context) || unreadMessages.isEmpty()) return
        val latestMsg = unreadMessages.maxByOrNull { it.createdAt?.seconds ?: 0L } ?: unreadMessages.first()
        val isGroup = conversationKey.startsWith("GROUP:", ignoreCase = true) || latestMsg.groupId.isNotBlank()
        val groupId = if (latestMsg.groupId.isNotBlank()) latestMsg.groupId else conversationKey.substringAfter(":")
        val groupObj = if (isGroup) chatGroups.firstOrNull { it.id == groupId } else null

        val partnerTitle = if (isGroup) {
            groupObj?.name?.takeIf { it.isNotBlank() }
                ?: latestMsg.groupName.takeIf { it.isNotBlank() }
                ?: latestMsg.receiverName.takeIf { it.isNotBlank() }
                ?: "গ্রুপ কথোপকথন"
        } else {
            latestMsg.senderName.ifBlank { "সম্মানিত দ্বীনি ভাই" }
        }

        val hidePreview = isMessagePreviewHidden(context)
        val singlePreview = when {
            hidePreview -> "নতুন পয়গাম এসেছে • দেখতে ট্যাপ করুন"
            latestMsg.imageUrls.isNotEmpty() && latestMsg.body.startsWith("📷") -> {
                if (isGroup) "${latestMsg.senderName}: 📷 ছবি পাঠিয়েছেন" else "📷 ছবি পাঠিয়েছেন"
            }
            else -> {
                val snippet = latestMsg.body.trim().ifBlank { "নতুন পয়গাম" }
                if (isGroup) "${latestMsg.senderName}: $snippet" else snippet
            }
        }

        val unreadCount = unreadMessages.size
        val contentTitle = if (unreadCount > 1) {
            "$partnerTitle (${toBanglaDigits(unreadCount)}টি অপঠিত পয়গাম)"
        } else {
            partnerTitle
        }

        val expandedLines = if (hidePreview) {
            "আপনার ইনবক্সে ${toBanglaDigits(unreadCount)}টি নতুন অপঠিত পয়গাম রয়েছে।"
        } else {
            unreadMessages
                .sortedBy { it.createdAt?.seconds ?: 0L }
                .takeLast(4)
                .joinToString("\n") { msg ->
                    val text = if (msg.imageUrls.isNotEmpty() && msg.body.startsWith("📷")) {
                        "📷 ছবি পাঠিয়েছেন"
                    } else {
                        msg.body.trim()
                    }
                    if (isGroup) "${msg.senderName}: $text" else text
                }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CHAT_PARTNER_ID, conversationKey)
            putExtra(EXTRA_OPEN_CHAT_PARTNER_NAME, partnerTitle)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val notificationId = conversationNotificationId(conversationKey)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(contentTitle)
            .setContentText(singlePreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedLines))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(unreadCount)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun showSystemNotification(context: Context, notification: AppNotification) {
        if (!hasNotificationPermission(context)) return

        val isSmoronNotif = notification.referenceId.startsWith("ajker_smoron", ignoreCase = true) ||
            notification.title.contains("আজকের স্মরণ")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (isSmoronNotif) {
                putExtra(EXTRA_OPEN_AJKER_SMORON, true)
                putExtra(
                    EXTRA_AJKER_SMORON_ID,
                    notification.referenceId.removePrefix("ajker_smoron:")
                )
            } else {
                putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            }
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

        val builder = NotificationCompat.Builder(context, if (isSmoronNotif) SMORON_CHANNEL_ID else CHANNEL_ID)
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

    /**
     * Posts the daily automated "আজকের স্মরণ" OS Push Notification (max 1 per day per user).
     * Tapping the notification opens [MainActivity] directly to the "আজকের স্মরণ" screen.
     */
    fun deliverDailyAjkerSmoronPushNotification(
        context: Context,
        userId: String,
        reminder: IslamicReminderItem,
        dateKey: String = AjkerSmoronHelper.currentBangladeshDateKey()
    ): Boolean {
        if (!AjkerSmoronHelper.isNotificationEnabled(context)) return false
        if (!hasNotificationPermission(context)) return false
        val effectiveUser = userId.trim().ifBlank { "default_user" }
        if (AjkerSmoronHelper.hasPushBeenSentForDate(context, effectiveUser, dateKey)) {
            return false
        }

        ensureChannelCreated(context)

        val intent = buildAjkerSmoronDeepLinkIntent(context, reminder.id)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            SMORON_NOTIFICATION_ID,
            intent,
            pendingIntentFlags
        )

        val title = "আজকের স্মরণ • ${reminder.categoryBangla}"
        val expandedText = buildString {
            append(reminder.messageBangla)
            if (reminder.quoteBangla.isNotBlank()) {
                append("\n\n")
                append(reminder.quoteBangla)
                if (reminder.sourceReferenceBangla.isNotBlank()) {
                    append(" — (")
                    append(reminder.sourceReferenceBangla)
                    append(")")
                }
            }
        }

        val builder = NotificationCompat.Builder(context, SMORON_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(reminder.messageBangla)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            NotificationManagerCompat.from(context).notify(SMORON_NOTIFICATION_ID, builder.build())
            AjkerSmoronHelper.recordPushSentForDate(
                context = context,
                userId = effectiveUser,
                dateKey = dateKey,
                reminderId = reminder.id
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun buildAjkerSmoronDeepLinkIntent(
        context: Context,
        reminderId: String = ""
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_AJKER_SMORON, true)
            if (reminderId.isNotBlank()) {
                putExtra(EXTRA_AJKER_SMORON_ID, reminderId.trim())
            }
        }
    }

    private fun toBanglaDigits(number: Int): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { ch ->
            if (ch in '0'..'9') banglaDigits[ch - '0'] else ch
        }.joinToString("")
    }
}
