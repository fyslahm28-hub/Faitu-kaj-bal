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
import java.util.concurrent.ConcurrentHashMap

object TelegramNotificationHelper {

    const val DEFAULT_ADMIN_CHAT_ID = "6255562134"
    const val DEFAULT_ADMIN_BOT_ID = "8982853197"

    private const val PLACEHOLDER_TOKEN = "TELEGRAM_BOT_TOKEN_DEFAULT_VALUE"
    private const val PLACEHOLDER_CHAT_ID = "TELEGRAM_CHAT_ID_DEFAULT_VALUE"
    private const val DEDUP_WINDOW_MS = 30_000L

    // Regex patterns to strictly redact any accidental secrets, tokens, OTPs, or reset links
    private val BOT_TOKEN_REGEX = Regex("""\b\d{8,12}:[A-Za-z0-9_-]{25,}\b""")
    private val FIREBASE_KEY_REGEX = Regex("""\bAIza[0-9A-Za-z_-]{20,}\b""")
    private val RESET_LINK_REGEX = Regex("""https?://[^\s]*(oobCode|resetPassword|apiKey=)[^\s]*""", RegexOption.IGNORE_CASE)
    private val PASSWORD_KV_REGEX = Regex("""(?i)(password|পাসওয়ার্ড|পাসওয়ার্ড|otp|secret|token)\s*[:=]\s*\S+""")

    // Idempotency tracking map: eventKey -> timestampMillis when successfully delivered to Telegram
    private val deliveredEventKeys = ConcurrentHashMap<String, Long>()

    data class TelegramBotDiagnosticResult(
        val isTokenConfigured: Boolean,
        val isChatIdValid: Boolean,
        val getMeVerified: Boolean,
        val getMeHttpCode: Int = 0,
        val botId: String = DEFAULT_ADMIN_BOT_ID,
        val botUsername: String = "",
        val botFirstName: String = "",
        val sendMessageOk: Boolean = false,
        val sendMessageHttpCode: Int = 0,
        val deliveredMessageId: Long? = null,
        val errorCode: Int? = null,
        val sanitizedErrorDescription: String = "",
        val deliveryStatus: String = "FAILED", // "DELIVERED", "QUEUED", "FAILED"
        val diagnosticMessageBangla: String = "",
        val checkedAtBangla: String = formatCurrentTimestampBangla()
    )

    fun clearDeliveredEventKeysForTest() {
        deliveredEventKeys.clear()
    }

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
     * Permanently fitted Telegram Bot ID inside the app (8982853197).
     */
    fun resolveConfiguredBotId(): String {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        if (token.isNotBlank() && token != PLACEHOLDER_TOKEN && token.contains(":")) {
            val prefix = token.substringBefore(":").trim()
            if (prefix.matches(Regex("""^\d{6,15}$"""))) {
                return prefix
            }
        }
        return DEFAULT_ADMIN_BOT_ID
    }

    private fun resolveActiveBotToken(): String {
        val fromBuildConfig = BuildConfig.TELEGRAM_BOT_TOKEN.trim()
        if (fromBuildConfig.isNotBlank() && fromBuildConfig != PLACEHOLDER_TOKEN) {
            return fromBuildConfig
        }
        val fromEnv = try {
            System.getenv("TELEGRAM_BOT_TOKEN")?.trim().orEmpty()
        } catch (_: Exception) {
            ""
        }
        return if (fromEnv.isNotBlank()) fromEnv else fromBuildConfig
    }

    fun isChatIdStructurallyValid(chatId: String = resolveTargetChatId()): Boolean {
        val clean = chatId.trim()
        return clean.matches(Regex("""^-?\d{5,20}$"""))
    }

    fun isTelegramBotConfigured(): Boolean {
        val token = resolveActiveBotToken()
        val chatId = resolveTargetChatId()
        return token.isNotBlank() &&
            token != PLACEHOLDER_TOKEN &&
            token.contains(":") &&
            isChatIdStructurallyValid(chatId)
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
        val isSupportTicket = eventType == "USER_SUPPORT_TICKET" ||
            title.startsWith("[অভিযোগ") ||
            title.startsWith("[সমস্যা") ||
            title.startsWith("[পরামর্শ") ||
            title.startsWith("[সহায়তা") ||
            title.startsWith("[সহায়তা") ||
            title.contains("সহায়তা ও যোগাযোগ")

        val eventBadge = when {
            isSupportTicket -> "🆘 ব্যবহারকারীর সহায়তা, অভিযোগ ও পরামর্শ বার্তা"
            eventType == "ADMIN_LOGIN" -> "🔑 অ্যাডমিন লগইন"
            eventType == "ADMIN_LOGOUT" -> "🚪 অ্যাডমিন লগআউট"
            eventType == "ADMIN_SETTINGS_CHANGE" -> "⚙️ অ্যাডমিন নিয়ম-নীতি ও সেটিংস পরিবর্তন"
            eventType == "FAILED_AUTH_ATTEMPT" -> "🚨 ব্যর্থ লগইন / অননুমোদিত বা সন্দেহজনক প্রচেষ্টা"
            eventType == "ADMIN_SECURITY" -> "🔐 অ্যাডমিন নিরাপত্তা ও দায়িত্ব পরিবর্তন ইত্তেলা"
            eventType == "ADMIN_VERIFICATION" -> "✅ অ্যাডমিন যাচাইকরণ, ব্লক ও Boost নিয়ন্ত্রণ"
            eventType == "CONTENT_MODERATION" -> "🛡️ কনটেন্ট অপসারণ ও মডারেশন অ্যাকশন"
            eventType == "ADMIN_ANNOUNCEMENT" -> "📣 অ্যাডমিন জরুরি ঘোষণা ও বিজ্ঞপ্তি প্রকাশ"
            eventType == "NEW_CIRCULAR" -> "📢 নতুন নিয়োগ বিজ্ঞপ্তি"
            eventType == "NEW_APPLICATION" -> "📝 নতুন খেদমত আবেদন"
            eventType == "KHEDMOT_SUBMISSION" -> "📨 বিজ্ঞপ্তিতে আবেদন পর্যালোচনা ও সিদ্ধান্ত"
            eventType == "NEW_MADRASA" -> "🕌 নতুন মাদরাসা নিবন্ধন"
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
     * Maps raw HTTP status codes and Telegram API error descriptions into clear, actionable
     * Bengali diagnostic messages for the Admin without exposing any raw token.
     */
    fun buildBanglaDiagnosticMessage(
        isTokenConfigured: Boolean,
        isChatIdValid: Boolean,
        httpCode: Int,
        rawDescription: String,
        chatId: String = resolveTargetChatId(),
        botId: String = resolveConfiguredBotId(),
        messageId: Long? = null
    ): String {
        val safeDesc = sanitizeSensitiveContent(rawDescription)
        if (!isChatIdValid) {
            return "ত্রুটি: কনফিগার করা টেলিগ্রাম চ্যাট আইডি ($chatId) সঠিক সংখ্যার ফরম্যাটে নেই।"
        }
        if (!isTokenConfigured) {
            return "ত্রুটি: অ্যাপের পরিবেশে TELEGRAM_BOT_TOKEN কনফিগার করা নেই (ডিফল্ট প্লেসহোল্ডার সক্রিয়)। অনুগ্রহ করে AI Studio Secrets প্যানেলে অথবা Firebase Cloud Functions Secret-এ বট ($botId)-এর সক্রিয় টোকেন যুক্ত করুন।"
        }
        if (httpCode in 200..299 && messageId != null && messageId > 0L) {
            return "আলহামদুলিল্লাহ, টেলিগ্রাম বট (Bot ID: $botId) থেকে চ্যাট আইডি ($chatId)-তে মেসেজ সফলভাবে পাঠানো হয়েছে (Telegram Message ID: #$messageId)।"
        }
        val lower = safeDesc.lowercase(Locale.ROOT)
        return when {
            httpCode == 401 || lower.contains("unauthorized") -> {
                "Telegram API 401 (Unauthorized): বট টোকেনটি বৈধ নয় অথবা @BotFather থেকে বাতিল/পরিবর্তিত (Revoked) হয়েছে। অনুগ্রহ করে @BotFather থেকে বট ($botId)-এর সক্রিয় টোকেন নিয়ে AI Studio Secrets প্যানেলে TELEGRAM_BOT_TOKEN হালনাগাদ করুন।"
            }
            httpCode == 400 && lower.contains("chat not found") -> {
                "Telegram API 400 (chat not found): বট টোকেন শতভাগ সচল, কিন্তু চ্যাট আইডি ($chatId) থেকে নতুন বটটিতে (@Quwmi_kormosongsthan_bot) এখনো মেসেজ (/start) পাঠানো হয়নি। টেলিগ্রামে @Quwmi_kormosongsthan_bot ওপেন করে Start বাটনে চাপুন।"
            }
            httpCode == 403 && (lower.contains("blocked") || lower.contains("forbidden")) -> {
                "Telegram API 403 (Forbidden): চ্যাট আইডি ($chatId) থেকে বটটিকে ব্লক করা হয়েছে অথবা বটের মেসেজ পাঠানোর অনুমতি নেই। বট আনব্লক করে /start পাঠান।"
            }
            httpCode == 404 -> {
                "Telegram API 404 (Not Found): বট এন্ডপয়েন্ট বা টোকেন ফরম্যাট সঠিক নয়।"
            }
            httpCode == 429 -> {
                "Telegram API 429 (Too Many Requests): অতিরিক্ত রিকোয়েস্টের কারণে সাময়িক বিরতি চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
            }
            httpCode <= 0 -> {
                "নেটওয়ার্ক ত্রুটি: Telegram Bot API সার্ভারে সংযোগ করা যায়নি (${safeDesc.ifBlank { "Connection failed" }})।"
            }
            else -> {
                "Telegram API ত্রুটি (HTTP $httpCode): ${safeDesc.ifBlank { "অজ্ঞাত সার্ভার রেসপন্স" }}"
            }
        }
    }

    private val OK_TRUE_REGEX = Regex(""""ok"\s*:\s*true\b""")
    private val MSG_ID_REGEX = Regex(""""message_id"\s*:\s*(\d+)""")
    private val ERR_CODE_REGEX = Regex(""""error_code"\s*:\s*(\d+)""")
    private val DESC_REGEX = Regex(""""description"\s*:\s*"([^"]*)"""")
    private val USERNAME_REGEX = Regex(""""username"\s*:\s*"([^"]*)"""")
    private val FIRST_NAME_REGEX = Regex(""""first_name"\s*:\s*"([^"]*)"""")
    private val BOT_ID_FIELD_REGEX = Regex(""""id"\s*:\s*(\d+)""")

    /**
     * Parses a raw JSON response from Telegram Bot API (`getMe` or `sendMessage`) and verifies
     * whether `"ok": true` is explicitly present.
     */
    fun parseTelegramSendMessageJson(
        httpCode: Int,
        rawResponseBody: String,
        isTokenConfigured: Boolean = true,
        chatId: String = resolveTargetChatId(),
        botId: String = resolveConfiguredBotId()
    ): TelegramBotDiagnosticResult {
        val validChat = isChatIdStructurallyValid(chatId)
        val body = rawResponseBody.trim()
        return try {
            val isOk = OK_TRUE_REGEX.containsMatchIn(body)
            if (httpCode in 200..299 && isOk) {
                val msgId = MSG_ID_REGEX.find(body)?.groupValues?.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0L }
                val username = USERNAME_REGEX.find(body)?.groupValues?.getOrNull(1).orEmpty()
                val firstName = FIRST_NAME_REGEX.find(body)?.groupValues?.getOrNull(1).orEmpty()
                if (msgId != null) {
                    TelegramBotDiagnosticResult(
                        isTokenConfigured = isTokenConfigured,
                        isChatIdValid = validChat,
                        getMeVerified = true,
                        getMeHttpCode = 200,
                        botId = botId,
                        botUsername = username,
                        botFirstName = firstName,
                        sendMessageOk = true,
                        sendMessageHttpCode = httpCode,
                        deliveredMessageId = msgId,
                        errorCode = null,
                        sanitizedErrorDescription = "",
                        deliveryStatus = "DELIVERED",
                        diagnosticMessageBangla = buildBanglaDiagnosticMessage(
                            isTokenConfigured = isTokenConfigured,
                            isChatIdValid = validChat,
                            httpCode = httpCode,
                            rawDescription = "",
                            chatId = chatId,
                            botId = botId,
                            messageId = msgId
                        )
                    )
                } else {
                    val errDesc = "Telegram API ok:true রেসপন্সে বৈধ message_id পাওয়া যায়নি।"
                    TelegramBotDiagnosticResult(
                        isTokenConfigured = isTokenConfigured,
                        isChatIdValid = validChat,
                        getMeVerified = false,
                        sendMessageOk = false,
                        sendMessageHttpCode = httpCode,
                        errorCode = httpCode,
                        sanitizedErrorDescription = errDesc,
                        deliveryStatus = "FAILED",
                        diagnosticMessageBangla = errDesc
                    )
                }
            } else {
                val errCode = ERR_CODE_REGEX.find(body)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: httpCode
                val extractedDesc = DESC_REGEX.find(body)?.groupValues?.getOrNull(1) ?: body.take(200)
                val desc = sanitizeSensitiveContent(extractedDesc)
                TelegramBotDiagnosticResult(
                    isTokenConfigured = isTokenConfigured,
                    isChatIdValid = validChat,
                    getMeVerified = false,
                    sendMessageOk = false,
                    sendMessageHttpCode = httpCode,
                    errorCode = errCode,
                    sanitizedErrorDescription = desc,
                    deliveryStatus = "FAILED",
                    diagnosticMessageBangla = buildBanglaDiagnosticMessage(
                        isTokenConfigured = isTokenConfigured,
                        isChatIdValid = validChat,
                        httpCode = errCode.takeIf { it > 0 } ?: httpCode,
                        rawDescription = desc,
                        chatId = chatId,
                        botId = botId
                    )
                )
            }
        } catch (e: Exception) {
            val safeErr = sanitizeSensitiveContent(e.localizedMessage ?: "Invalid JSON response")
            TelegramBotDiagnosticResult(
                isTokenConfigured = isTokenConfigured,
                isChatIdValid = validChat,
                getMeVerified = false,
                sendMessageOk = false,
                sendMessageHttpCode = httpCode,
                errorCode = httpCode,
                sanitizedErrorDescription = safeErr,
                deliveryStatus = "FAILED",
                diagnosticMessageBangla = buildBanglaDiagnosticMessage(
                    isTokenConfigured = isTokenConfigured,
                    isChatIdValid = validChat,
                    httpCode = httpCode,
                    rawDescription = safeErr,
                    chatId = chatId,
                    botId = botId
                )
            )
        }
    }

    /**
     * Verifies the configured Telegram Bot Token by calling Telegram's official `getMe` endpoint
     * and inspecting the real JSON response (`"ok": true` and `result.id` / `result.username`).
     */
    suspend fun verifyBotConnectionWithGetMe(): TelegramBotDiagnosticResult = withContext(Dispatchers.IO) {
        val chatId = resolveTargetChatId()
        val botId = resolveConfiguredBotId()
        val validChat = isChatIdStructurallyValid(chatId)
        val tokenConfigured = isTelegramBotConfigured()

        if (!tokenConfigured) {
            val msg = buildBanglaDiagnosticMessage(
                isTokenConfigured = false,
                isChatIdValid = validChat,
                httpCode = 0,
                rawDescription = "TELEGRAM_BOT_TOKEN_NOT_CONFIGURED",
                chatId = chatId,
                botId = botId
            )
            return@withContext TelegramBotDiagnosticResult(
                isTokenConfigured = false,
                isChatIdValid = validChat,
                getMeVerified = false,
                getMeHttpCode = 0,
                botId = botId,
                sendMessageOk = false,
                sanitizedErrorDescription = "TELEGRAM_BOT_TOKEN is not configured in BuildConfig/.env",
                deliveryStatus = "FAILED",
                diagnosticMessageBangla = msg
            )
        }

        val token = resolveActiveBotToken()
        var conn: HttpURLConnection? = null
        try {
            val apiUrl = URL("https://api.telegram.org/bot$token/getMe")
            conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val rawBody = readHttpBodySafely(conn, code).trim()
            val isOk = OK_TRUE_REGEX.containsMatchIn(rawBody)
            if (code in 200..299 && isOk) {
                val returnedId = BOT_ID_FIELD_REGEX.find(rawBody)?.groupValues?.getOrNull(1).orEmpty()
                val username = USERNAME_REGEX.find(rawBody)?.groupValues?.getOrNull(1).orEmpty()
                val firstName = FIRST_NAME_REGEX.find(rawBody)?.groupValues?.getOrNull(1).orEmpty()
                TelegramBotDiagnosticResult(
                    isTokenConfigured = true,
                    isChatIdValid = validChat,
                    getMeVerified = true,
                    getMeHttpCode = code,
                    botId = returnedId.ifBlank { botId },
                    botUsername = username,
                    botFirstName = firstName,
                    sendMessageOk = false,
                    deliveryStatus = "QUEUED",
                    diagnosticMessageBangla = "আলহামদুলিল্লাহ, Telegram getMe যাচাই সফল! বট সচল রয়েছে (${if (username.isNotBlank()) "@$username" else firstName}, Bot ID: ${returnedId.ifBlank { botId }}, Chat ID: $chatId)।"
                )
            } else {
                val errCode = ERR_CODE_REGEX.find(rawBody)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: code
                val extractedDesc = DESC_REGEX.find(rawBody)?.groupValues?.getOrNull(1) ?: rawBody.take(200)
                val desc = sanitizeSensitiveContent(extractedDesc)
                TelegramBotDiagnosticResult(
                    isTokenConfigured = true,
                    isChatIdValid = validChat,
                    getMeVerified = false,
                    getMeHttpCode = code,
                    botId = botId,
                    sendMessageOk = false,
                    errorCode = errCode,
                    sanitizedErrorDescription = desc,
                    deliveryStatus = "FAILED",
                    diagnosticMessageBangla = buildBanglaDiagnosticMessage(
                        isTokenConfigured = true,
                        isChatIdValid = validChat,
                        httpCode = errCode.takeIf { it > 0 } ?: code,
                        rawDescription = desc,
                        chatId = chatId,
                        botId = botId
                    )
                )
            }
        } catch (e: Exception) {
            val safeErr = sanitizeSensitiveContent(e.localizedMessage ?: "Network exception")
            TelegramBotDiagnosticResult(
                isTokenConfigured = true,
                isChatIdValid = validChat,
                getMeVerified = false,
                getMeHttpCode = 0,
                botId = botId,
                sendMessageOk = false,
                errorCode = -1,
                sanitizedErrorDescription = safeErr,
                deliveryStatus = "FAILED",
                diagnosticMessageBangla = buildBanglaDiagnosticMessage(
                    isTokenConfigured = true,
                    isChatIdValid = validChat,
                    httpCode = 0,
                    rawDescription = safeErr,
                    chatId = chatId,
                    botId = botId
                )
            )
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Performs end-to-end verified Telegram Bot dispatch:
     * 1. Enforces idempotency via `eventKey` so duplicate messages are not dispatched for the same event.
     * 2. Verifies bot configuration and `getMe` status.
     * 3. Dispatches `sendMessage` to Chat ID `6255562134` and parses the JSON response (`"ok": true` + `result.message_id`).
     */
    suspend fun dispatchToTelegramBotVerified(
        eventType: String,
        title: String,
        message: String,
        actorName: String,
        targetPhone: String = AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
        eventKey: String = "",
        bypassDedup: Boolean = false
    ): TelegramBotDiagnosticResult = withContext(Dispatchers.IO) {
        val chatId = resolveTargetChatId()
        val botId = resolveConfiguredBotId()
        val validChat = isChatIdStructurallyValid(chatId)
        val tokenConfigured = isTelegramBotConfigured()

        val resolvedEventKey = eventKey.trim().ifBlank {
            "${eventType}_${title.hashCode()}_${message.hashCode()}"
        }
        val now = System.currentTimeMillis()
        if (!bypassDedup) {
            val previousSentAt = deliveredEventKeys[resolvedEventKey]
            if (previousSentAt != null && (now - previousSentAt) < DEDUP_WINDOW_MS) {
                return@withContext TelegramBotDiagnosticResult(
                    isTokenConfigured = tokenConfigured,
                    isChatIdValid = validChat,
                    getMeVerified = true,
                    getMeHttpCode = 200,
                    botId = botId,
                    sendMessageOk = true,
                    sendMessageHttpCode = 200,
                    deliveredMessageId = 1L,
                    deliveryStatus = "DELIVERED",
                    diagnosticMessageBangla = "এই ইভেন্টের টেলিগ্রাম বার্তা ইতোমধ্যে পাঠানো হয়েছে (ডুপ্লিকেট প্রেরণ রোধ করা হয়েছে)।"
                )
            }
        }

        if (!tokenConfigured) {
            val msg = buildBanglaDiagnosticMessage(
                isTokenConfigured = false,
                isChatIdValid = validChat,
                httpCode = 0,
                rawDescription = "TELEGRAM_BOT_TOKEN_NOT_CONFIGURED",
                chatId = chatId,
                botId = botId
            )
            return@withContext TelegramBotDiagnosticResult(
                isTokenConfigured = false,
                isChatIdValid = validChat,
                getMeVerified = false,
                botId = botId,
                sendMessageOk = false,
                sanitizedErrorDescription = "TELEGRAM_BOT_TOKEN not configured",
                deliveryStatus = "FAILED",
                diagnosticMessageBangla = msg
            )
        }

        // Step 1: Verify bot token via getMe before sending
        val getMeResult = verifyBotConnectionWithGetMe()
        if (!getMeResult.getMeVerified) {
            return@withContext getMeResult.copy(
                sendMessageOk = false,
                deliveryStatus = "FAILED"
            )
        }

        // Step 2: Send message via sendMessage and verify "ok": true + result.message_id
        val token = resolveActiveBotToken()
        val formattedText = formatTelegramEventMessage(
            eventType = eventType,
            title = title,
            message = message,
            actorName = actorName,
            targetPhone = targetPhone
        )

        val sendResult = executeSendMessageHttpCall(
            token = token,
            chatId = chatId,
            botId = getMeResult.botId.ifBlank { botId },
            formattedText = formattedText
        )

        if (sendResult.sendMessageOk) {
            deliveredEventKeys[resolvedEventKey] = System.currentTimeMillis()
        }

        sendResult.copy(
            getMeVerified = true,
            getMeHttpCode = getMeResult.getMeHttpCode,
            botUsername = getMeResult.botUsername.ifBlank { sendResult.botUsername },
            botFirstName = getMeResult.botFirstName.ifBlank { sendResult.botFirstName }
        )
    }

    /**
     * Backward-compatible Boolean wrapper that returns `true` ONLY if Telegram Bot API
     * explicitly returns `"ok": true` and a valid `message_id`.
     */
    suspend fun dispatchToTelegramBot(
        eventType: String,
        title: String,
        message: String,
        actorName: String,
        targetPhone: String = AdminSecurityHelper.ADMIN_MOBILE_NUMBER
    ): Boolean {
        val result = dispatchToTelegramBotVerified(
            eventType = eventType,
            title = title,
            message = message,
            actorName = actorName,
            targetPhone = targetPhone,
            bypassDedup = true
        )
        return result.sendMessageOk
    }

    private fun escapeJsonString(raw: String): String {
        val sb = StringBuilder(raw.length + 16)
        for (ch in raw) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> {
                    if (ch < ' ') {
                        sb.append(String.format(Locale.US, "\\u%04x", ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun executeSendMessageHttpCall(
        token: String,
        chatId: String,
        botId: String,
        formattedText: String
    ): TelegramBotDiagnosticResult {
        var conn: HttpURLConnection? = null
        return try {
            val apiUrl = URL("https://api.telegram.org/bot$token/sendMessage")
            val payload = """{"chat_id":"${escapeJsonString(chatId)}","text":"${escapeJsonString(formattedText)}","disable_web_page_preview":true}"""

            conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }
            conn.outputStream.use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val rawBody = readHttpBodySafely(conn, code)
            parseTelegramSendMessageJson(
                httpCode = code,
                rawResponseBody = rawBody,
                isTokenConfigured = true,
                chatId = chatId,
                botId = botId
            )
        } catch (e: Exception) {
            val safeErr = sanitizeSensitiveContent(e.localizedMessage ?: "Network exception")
            TelegramBotDiagnosticResult(
                isTokenConfigured = true,
                isChatIdValid = isChatIdStructurallyValid(chatId),
                getMeVerified = false,
                botId = botId,
                sendMessageOk = false,
                sendMessageHttpCode = 0,
                errorCode = -1,
                sanitizedErrorDescription = safeErr,
                deliveryStatus = "FAILED",
                diagnosticMessageBangla = buildBanglaDiagnosticMessage(
                    isTokenConfigured = true,
                    isChatIdValid = isChatIdStructurallyValid(chatId),
                    httpCode = 0,
                    rawDescription = safeErr,
                    chatId = chatId,
                    botId = botId
                )
            )
        } finally {
            conn?.disconnect()
        }
    }

    private fun readHttpBodySafely(conn: HttpURLConnection, responseCode: Int): String {
        return try {
            val stream = if (responseCode in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream ?: conn.inputStream
            }
            stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        } catch (_: Exception) {
            ""
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
