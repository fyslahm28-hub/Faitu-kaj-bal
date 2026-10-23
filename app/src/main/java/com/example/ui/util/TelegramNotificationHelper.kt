package com.example.ui.util

import android.content.Context
import android.content.Intent
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TelegramNotificationHelper {

    const val DEFAULT_ADMIN_CHAT_ID = "6255562134"
    const val DEFAULT_ADMIN_BOT_ID = "8917160878"

    private const val PLACEHOLDER_TOKEN = "TELEGRAM_BOT_TOKEN_DEFAULT_VALUE"
    private const val PLACEHOLDER_CHAT_ID = "TELEGRAM_CHAT_ID_DEFAULT_VALUE"

    // Regex patterns to strictly redact any accidental secrets, tokens, OTPs, or reset links
    private val BOT_TOKEN_REGEX = Regex("""\b\d{8,12}:[A-Za-z0-9_-]{25,}\b""")
    private val FIREBASE_KEY_REGEX = Regex("""\bAIza[0-9A-Za-z_-]{20,}\b""")
    private val RESET_LINK_REGEX = Regex("""https?://[^\s]*(oobCode|resetPassword|apiKey=)[^\s]*""", RegexOption.IGNORE_CASE)
    private val PASSWORD_KV_REGEX = Regex("""(?i)(password|পাসওয়ার্ড|পাসওয়ার্ড|otp|secret|token)\s*[:=]\s*\S+""")

    /**
     * Permanently fitted target Chat ID inside the app (6255562134).
     */
    fun resolveTargetChatId(): String {
        val configured = BuildConfig.TELEGRAM_CHAT_ID.trim()
        return if (configured.isNotBlank() && configured != PLACEHOLDER_CHAT_ID) {
            configured
        } else {
            DEFAULT_ADMIN_CHAT_ID
        }
    }

    /**
     * Permanently fitted Telegram Bot ID inside the app (8917160878).
     */
    fun resolveConfiguredBotId(): String {
        return DEFAULT_ADMIN_BOT_ID
    }

    private fun resolveActiveBotToken(): String {
        return BuildConfig.TELEGRAM_BOT_TOKEN.trim()
    }

    fun isTelegramBotConfigured(): Boolean {
        val token = resolveActiveBotToken()
        val chatId = resolveTargetChatId()
        return token.isNotBlank() &&
            token != PLACEHOLDER_TOKEN &&
            chatId.isNotBlank()
    }

    /**
     * Strictly strips/redacts passwords, OTPs, Reset Links, Firebase API Keys, or Bot Tokens
     * so sensitive credentials are NEVER sent to Telegram or stored in logs.
     */
    fun sanitizeSensitiveContent(raw: String): String {
        return raw
            .replace(BOT_TOKEN_REGEX, "[REDACTED_BOT_TOKEN]")
            .replace(FIREBASE_KEY_REGEX, "[REDACTED_FIREBASE_SECRET]")
            .replace(RESET_LINK_REGEX, "[REDACTED_RESET_LINK]")
            .replace(PASSWORD_KV_REGEX, "$1: [REDACTED]")
            .trim()
    }

    fun formatCurrentTimestampBangla(date: Date = Date()): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }
        return "${sdf.format(date)} (BDT)"
    }

    fun formatTelegramEventMessage(
        eventType: String,
        title: String,
        message: String,
        actorName: String,
        targetPhone: String = AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
        timestampText: String = formatCurrentTimestampBangla()
    ): String {
        val eventBadge = when (eventType) {
            "ADMIN_LOGIN" -> "🔑 অ্যাডমিন লগইন"
            "ADMIN_LOGOUT" -> "🚪 অ্যাডমিন লগআউট"
            "ADMIN_SETTINGS_CHANGE" -> "⚙️ অ্যাডমিন নিয়ম-নীতি ও সেটিংস পরিবর্তন"
            "FAILED_AUTH_ATTEMPT" -> "🚨 ব্যর্থ লগইন / অননুমোদিত বা সন্দেহজনক প্রচেষ্টা"
            "ADMIN_SECURITY" -> "🔐 অ্যাডমিন নিরাপত্তা ও দায়িত্ব পরিবর্তন ইত্তেলা"
            "ADMIN_VERIFICATION" -> "✅ অ্যাডমিন যাচাইকরণ, ব্লক ও Boost নিয়ন্ত্রণ"
            "CONTENT_MODERATION" -> "🛡️ কনটেন্ট অপসারণ ও মডারেশন অ্যাকশন"
            "ADMIN_ANNOUNCEMENT" -> "📣 অ্যাডমিন জরুরি ঘোষণা ও বিজ্ঞপ্তি প্রকাশ"
            "NEW_CIRCULAR" -> "📢 নতুন নিয়োগ বিজ্ঞপ্তি"
            "NEW_APPLICATION" -> "📝 নতুন খেদমত আবেদন"
            "KHEDMOT_SUBMISSION" -> "📨 বিজ্ঞপ্তিতে আবেদন পর্যালোচনা ও সিদ্ধান্ত"
            "NEW_MADRASA" -> "🕌 নতুন মাদরাসা নিবন্ধন"
            else -> "🔔 কওমি কর্মসংস্থান অটো অ্যাক্টিভিটি অ্যালার্ট"
        }

        val safeTitle = sanitizeSensitiveContent(title)
        val safeMessage = sanitizeSensitiveContent(message)
        val safeActor = sanitizeSensitiveContent(actorName)

        return buildString {
            appendLine("【কওমি কর্মসংস্থান • স্বয়ংক্রিয় অ্যাডমিন বট অ্যালার্ট】")
            appendLine("টেলিগ্রাম চ্যাট আইডি: ${resolveTargetChatId()}")
            appendLine("টেলিগ্রাম বট আইডি: ${resolveConfiguredBotId()}")
            appendLine("সময়: $timestampText")
            appendLine("কার্যক্রমের ধরন: $eventBadge")
            appendLine("শিরোনাম: $safeTitle")
            appendLine("বিস্তারিত পরিবর্তন/কার্যকলাপ: $safeMessage")
            appendLine("সম্পাদনকারী অ্যাডমিন/পরিচয়: $safeActor")
            appendLine("অ্যাডমিন হটলাইন: $targetPhone")
        }.trim()
    }

    /**
     * Automatically dispatches a real-time Telegram Bot API notification using the permanently
     * fitted Bot Token and Admin Chat ID (`6255562134`) from BuildConfig.
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
        val token = resolveActiveBotToken()
        val configuredTarget = resolveTargetChatId()
        val formattedText = formatTelegramEventMessage(
            eventType = eventType,
            title = title,
            message = message,
            actorName = actorName,
            targetPhone = targetPhone
        )

        sendTelegramMessageRequest(token, configuredTarget, formattedText)
    }

    private fun sendTelegramMessageRequest(
        token: String,
        chatId: String,
        formattedText: String
    ): Boolean {
        return try {
            val apiUrl = URL("https://api.telegram.org/bot$token/sendMessage")
            val payload = JSONObject().apply {
                put("chat_id", chatId)
                put("text", formattedText)
                put("disable_web_page_preview", true)
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
        val safeText = sanitizeSensitiveContent(formattedText)
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, safeText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(sendIntent, "প্রধান অ্যাডমিনের টেলিগ্রামে ইত্তেলা পাঠান").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            // Ignore if chooser unavailable
        }
    }
}
