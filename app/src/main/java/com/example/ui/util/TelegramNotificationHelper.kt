package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TelegramNotificationHelper {

    private const val PLACEHOLDER_TOKEN = "TELEGRAM_BOT_TOKEN_DEFAULT_VALUE"
    private const val PLACEHOLDER_CHAT_ID = "TELEGRAM_CHAT_ID_DEFAULT_VALUE"

    fun isTelegramBotConfigured(): Boolean {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        val chatId = BuildConfig.TELEGRAM_CHAT_ID.trim()
        return token.isNotBlank() &&
            token != PLACEHOLDER_TOKEN &&
            chatId.isNotBlank() &&
            chatId != PLACEHOLDER_CHAT_ID
    }

    fun formatTelegramEventMessage(
        eventType: String,
        title: String,
        message: String,
        actorName: String,
        targetPhone: String = AdminSecurityHelper.ADMIN_MOBILE_NUMBER
    ): String {
        val eventBadge = when (eventType) {
            "NEW_CIRCULAR" -> "📢 নতুন নিয়োগ বিজ্ঞপ্তি"
            "NEW_APPLICATION" -> "📝 নতুন খেদমত আবেদন"
            "KHEDMOT_SUBMISSION" -> "📨 বিজ্ঞপ্তিতে নতুন আবেদন জমা"
            "NEW_MADRASA" -> "🕌 নতুন মাদরাসা নিবন্ধন"
            "ADMIN_ANNOUNCEMENT" -> "📣 অ্যাডমিন জরুরি ঘোষণা"
            "ADMIN_VERIFICATION" -> "✅ অ্যাডমিন যাচাইকরণ ও Boost"
            "ADMIN_SECURITY" -> "🔐 অ্যাডমিন নিরাপত্তা ইত্তেলা"
            else -> "🔔 কওমি কর্মসংস্থান ইত্তেলা"
        }
        return buildString {
            appendLine("【কওমি কর্মসংস্থান • টেলিগ্রাম ইত্তেলা】")
            appendLine("ধরন: $eventBadge")
            appendLine("শিরোনাম: $title")
            appendLine("বিবরণ: $message")
            appendLine("সংশ্লিষ্ট: $actorName")
            appendLine("অ্যাডমিন হটলাইন: $targetPhone")
        }.trim()
    }

    /**
     * Dispatches a real Telegram Bot API notification when TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID
     * are configured in BuildConfig (via AI Studio Secrets Panel).
     * Returns true if delivered via Bot API, or false if queued in Firestore for Admin Panel / Bot sync.
     */
    suspend fun dispatchToTelegramBot(
        eventType: String,
        title: String,
        message: String,
        actorName: String,
        targetPhone: String = AdminSecurityHelper.ADMIN_MOBILE_NUMBER
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isTelegramBotConfigured()) {
            return@withContext false
        }
        val token = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        val chatId = BuildConfig.TELEGRAM_CHAT_ID.trim()
        val formattedText = formatTelegramEventMessage(
            eventType = eventType,
            title = title,
            message = message,
            actorName = actorName,
            targetPhone = targetPhone
        )

        try {
            val apiUrl = URL("https://api.telegram.org/bot$token/sendMessage")
            val payload = JSONObject().apply {
                put("chat_id", chatId)
                put("text", formattedText)
            }.toString()

            val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            conn.outputStream.use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    fun shareAlertToTelegram(context: Context, formattedText: String) {
        try {
            val encodedText = Uri.encode(formattedText)
            val tgUri = Uri.parse("https://t.me/share/url?url=${Uri.encode("https://qawmikormo.app")}&text=$encodedText")
            val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, formattedText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(sendIntent, "টেলিগ্রামে ইত্তেলা পাঠান").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
