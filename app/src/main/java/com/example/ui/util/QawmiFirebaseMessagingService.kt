package com.example.ui.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging (FCM) service that receives real-time push notifications
 * for new Paygam (1-on-1 and Group) messages and system alerts even when the app is in
 * the background or completely closed.
 */
class QawmiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            val prefs = applicationContext.getSharedPreferences(FCM_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LATEST_FCM_TOKEN, token).apply()
            val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }.getOrDefault("")
            if (uid.isNotBlank()) {
                QawmiNotificationHelper.syncFcmTopicSubscriptions(applicationContext, uid)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to persist new FCM token: ${t.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            val data = remoteMessage.data ?: emptyMap()
            val notifTitle = remoteMessage.notification?.title
            val notifBody = remoteMessage.notification?.body
            val currentUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }.getOrDefault("")

            QawmiNotificationHelper.handleIncomingFcmPushMessage(
                context = applicationContext,
                data = data,
                notificationTitle = notifTitle,
                notificationBody = notifBody,
                currentUserId = currentUid
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Error handling incoming FCM message: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "QawmiFcmService"
        const val FCM_PREFS_NAME = "qawmi_fcm_token_prefs"
        const val KEY_LATEST_FCM_TOKEN = "latest_fcm_token"
    }
}
