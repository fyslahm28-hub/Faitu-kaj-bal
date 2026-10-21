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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TelegramNotificationHelper {

    const val DEFAULT_TELEGRAM_CHANNEL = "@Q_kormosongsthan_Bangladesh"
    const val TELEGRAM_CHANNEL_URL = "https://t.me/Q_kormosongsthan_Bangladesh"

    private const val PLACEHOLDER_TOKEN = "TELEGRAM_BOT_TOKEN_DEFAULT_VALUE"
    private const val PLACEHOLDER_CHAT_ID = "TELEGRAM_CHAT_ID_DEFAULT_VALUE"

    // Regex patterns to strictly redact any accidental secrets, tokens, OTPs, or reset links
    private val BOT_TOKEN_REGEX = Regex("""\b\d{8,12}:[A-Za-z0-9_-]{25,}\b""")
    private val FIREBASE_KEY_REGEX = Regex("""\bAIza[0-9A-Za-z_-]{20,}\b""")
    private val RESET_LINK_REGEX = Regex("""https?://[^\s]*(oobCode|resetPassword|apiKey=)[^\s]*""", RegexOption.IGNORE_CASE)
    private val PASSWORD_KV_REGEX = Regex("""(?i)(password|পাসওয়ার্ড|পাসওয়ার্ড|otp|secret|token)\s*[:=]\s*\S+""")

    fun resolveTargetChatId(): String {
        val configured = BuildConfig.TELEGRAM_CHAT_ID.trim()
        return if (configured.isNotBlank() && configured != PLACEHOLDER_CHAT_ID) {
            configured
        } else {
            DEFAULT_TELEGRAM_CHANNEL
        }
    }

    fun isTelegramBotConfigured(): Boolean {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
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
            "ADMIN_SETTINGS_CHANGE" -> "⚙️ অ্যাডমিন সেটিংস ও অনুমতি পরিবর্তন"
            "FAILED_AUTH_ATTEMPT" -> "🚨 ব্যর্থ লগইন / অননুমোদিত প্রচেষ্টা"
            "ADMIN_SECURITY" -> "🔐 অ্যাডমিন নিরাপত্তা ও পাসওয়ার্ড ইত্তেলা"
            "ADMIN_VERIFICATION" -> "✅ অ্যাডমিন যাচাইকরণ ও Boost"
            "CONTENT_MODERATION" -> "🛡️ কনটেন্ট ও পোস্ট পরিচালনা"
            "ADMIN_ANNOUNCEMENT" -> "📣 অ্যাডমিন জরুরি ঘোষণা"
            "NEW_CIRCULAR" -> "📢 নতুন নিয়োগ বিজ্ঞপ্তি"
            "NEW_APPLICATION" -> "📝 নতুন খেদমত আবেদন"
            "KHEDMOT_SUBMISSION" -> "📨 বিজ্ঞপ্তিতে নতুন আবেদন জমা"
            "NEW_MADRASA" -> "🕌 নতুন মাদরাসা নিবন্ধন"
            else -> "🔔 কওমি কর্মসংস্থান অ্যাক্টিভিটি"
        }

        val safeTitle = sanitizeSensitiveContent(title)
        val safeMessage = sanitizeSensitiveContent(message)
        val safeActor = sanitizeSensitiveContent(actorName)

        return buildString {
            appendLine("【কওমি কর্মসংস্থান • প্রধান অ্যাডমিন টেলিগ্রাম ইত্তেলা】")
            appendLine("প্রধান অ্যাডমিন আইডি: $DEFAULT_TELEGRAM_CHANNEL")
            appendLine("সময়: $timestampText")
            appendLine("কার্যক্রমের ধরন: $eventBadge")
            appendLine("শিরোনাম: $safeTitle")
            appendLine("বিবরণ: $safeMessage")
            appendLine("সংশ্লিষ্ট পরিচয়: $safeActor")
            appendLine("অ্যাডমিন হটলাইন: $targetPhone")
        }.trim()
    }

    @Volatile
    private var cachedPersonalChatId: String? = null

    /**
     * Dispatches a real Telegram Bot API notification to `@Q_kormosongsthan_Bangladesh`
     * (whether it is a personal Chief Admin Telegram account or a channel/group) when
     * TELEGRAM_BOT_TOKEN is configured in BuildConfig via AI Studio Secrets Panel.
     *
     * Note on personal Telegram accounts: Telegram Bot API requires numeric chat_id for personal
     * user DMs. If `chat_id = "@Q_kormosongsthan_Bangladesh"` returns 400 for a personal account,
     * this helper automatically queries `getUpdates` to resolve the Chief Admin's numeric `chat.id`
     * (once the Chief Admin has sent `/start` to the bot) and delivers the message directly.
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
        val configuredTarget = cachedPersonalChatId ?: resolveTargetChatId()
        val formattedText = formatTelegramEventMessage(
            eventType = eventType,
            title = title,
            message = message,
            actorName = actorName,
            targetPhone = targetPhone
        )

        if (sendTelegramMessageRequest(token, configuredTarget, formattedText)) {
            return@withContext true
        }

        // If target is a personal @username (like @Q_kormosongsthan_Bangladesh), resolve numeric chat_id via getUpdates
        if (configuredTarget.startsWith("@")) {
            val resolvedNumericId = resolveNumericChatIdFromBotUpdates(
                token = token,
                targetUsername = configuredTarget.removePrefix("@")
            )
            if (!resolvedNumericId.isNullOrBlank()) {
                cachedPersonalChatId = resolvedNumericId
                return@withContext sendTelegramMessageRequest(token, resolvedNumericId, formattedText)
            }
        }
        false
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

    private fun resolveNumericChatIdFromBotUpdates(
        token: String,
        targetUsername: String
    ): String? {
        return try {
            val updatesUrl = URL("https://api.telegram.org/bot$token/getUpdates?limit=50")
            val conn = (updatesUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val root = JSONObject(body)
            val results = root.optJSONArray("result") ?: return null
            var fallbackFirstPrivateChatId: String? = null

            for (i in results.length() - 1 downTo 0) {
                val item = results.optJSONObject(i) ?: continue
                val msg = item.optJSONObject("message")
                    ?: item.optJSONObject("edited_message")
                    ?: item.optJSONObject("my_chat_member")
                    ?: continue
                val chat = msg.optJSONObject("chat") ?: continue
                val chatId = chat.optLong("id", 0L)
                if (chatId == 0L) continue
                val username = chat.optString("username", "")
                if (username.equals(targetUsername, ignoreCase = true)) {
                    return chatId.toString()
                }
                if (fallbackFirstPrivateChatId == null && chat.optString("type") == "private") {
                    fallbackFirstPrivateChatId = chatId.toString()
                }
            }
            fallbackFirstPrivateChatId
        } catch (_: Exception) {
            null
        }
    }

    fun openTelegramChannel(context: Context) {
        val cleanUsername = DEFAULT_TELEGRAM_CHANNEL.removePrefix("@")
        try {
            val tgDeepLink = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$cleanUsername")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(tgDeepLink)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL_URL)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                // Ignore if browser/Telegram unavailable
            }
        }
    }

    fun shareAlertToTelegram(context: Context, formattedText: String) {
        val safeText = sanitizeSensitiveContent(formattedText)
        val cleanUsername = DEFAULT_TELEGRAM_CHANNEL.removePrefix("@")
        try {
            val encodedText = Uri.encode(safeText)
            val directAdminUri = Uri.parse("tg://resolve?domain=$cleanUsername&text=$encodedText")
            val intent = Intent(Intent.ACTION_VIEW, directAdminUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val encodedText = Uri.encode(safeText)
                val tgUri = Uri.parse("https://t.me/share/url?url=${Uri.encode(TELEGRAM_CHANNEL_URL)}&text=$encodedText")
                val intent = Intent(Intent.ACTION_VIEW, tgUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, safeText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(sendIntent, "প্রধান অ্যাডমিনের টেলিগ্রামে ইত্তেলা পাঠান").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}
