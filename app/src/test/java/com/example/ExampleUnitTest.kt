package com.example

import com.example.ui.util.AdminSecurityHelper
import com.example.ui.util.TelegramNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    @Before
    fun setUp() {
        AdminSecurityHelper.clearPasswordResetRateLimitsForTest()
    }

    @Test
    fun adminSecurityHelper_recognizesAllThreeAuthorizedAdminEmails_andRejectsOthers() {
        assertTrue(AdminSecurityHelper.isAuthorizedAdminEmail("foysalahamad102@gmail.com"))
        assertTrue(AdminSecurityHelper.isAuthorizedAdminEmail("FOYSALAHAMAD204@GMAIL.COM"))
        assertTrue(AdminSecurityHelper.isAuthorizedAdminEmail("  fyslahm28@gmail.com  "))

        assertFalse(AdminSecurityHelper.isAuthorizedAdminEmail("regular.user@gmail.com"))
        assertFalse(AdminSecurityHelper.isAuthorizedAdminEmail("foysalahamad102@yahoo.com"))
        assertFalse(AdminSecurityHelper.isAuthorizedAdminEmail(""))
        assertFalse(AdminSecurityHelper.isAuthorizedAdminEmail(null))
    }

    @Test
    fun adminSecurityHelper_recognizesAuthorizedAdminPhoneVariants_andRejectsOthers() {
        assertTrue(AdminSecurityHelper.isAuthorizedAdminPhone("01300448147"))
        assertTrue(AdminSecurityHelper.isAuthorizedAdminPhone("+8801300448147"))
        assertTrue(AdminSecurityHelper.isAuthorizedAdminPhone("8801300448147"))
        assertTrue(AdminSecurityHelper.isAuthorizedAdminPhone("+880 1300-448147"))

        assertFalse(AdminSecurityHelper.isAuthorizedAdminPhone("01711000000"))
        assertFalse(AdminSecurityHelper.isAuthorizedAdminPhone(""))
        assertFalse(AdminSecurityHelper.isAuthorizedAdminPhone(null))
    }

    @Test
    fun adminSecurityHelper_enforcesPasswordResetCooldownAndHourlyLimits() {
        val email = "foysalahamad102@gmail.com"
        val baseTime = 1_700_000_000_000L

        assertNull(AdminSecurityHelper.checkPasswordResetRateLimit(email, baseTime))
        AdminSecurityHelper.recordPasswordResetRequest(email, baseTime)

        // Cooldown within 60 seconds must block immediate repeat request
        val cooldownError = AdminSecurityHelper.checkPasswordResetRateLimit(email, baseTime + 15_000L)
        assertNotNull("Expected cooldown error within 60s", cooldownError)

        // Allowed after 61 seconds
        assertNull(AdminSecurityHelper.checkPasswordResetRateLimit(email, baseTime + 61_000L))
        AdminSecurityHelper.recordPasswordResetRequest(email, baseTime + 61_000L)

        // 3rd request after another 61 seconds
        assertNull(AdminSecurityHelper.checkPasswordResetRateLimit(email, baseTime + 125_000L))
        AdminSecurityHelper.recordPasswordResetRequest(email, baseTime + 125_000L)

        // 4th request within 1 hour must be blocked by hourly cap
        val hourlyCapError = AdminSecurityHelper.checkPasswordResetRateLimit(email, baseTime + 300_000L)
        assertNotNull("Expected hourly limit error after 3 requests", hourlyCapError)
    }

    @Test
    fun telegramNotificationHelper_formatsChatAndBotIdAndRedactsSensitiveSecrets() {
        assertEquals("6255562134", TelegramNotificationHelper.resolveTargetChatId())
        assertEquals("8982853197", TelegramNotificationHelper.resolveConfiguredBotId())

        val message = TelegramNotificationHelper.formatTelegramEventMessage(
            eventType = "ADMIN_LOGIN",
            title = "অ্যাডমিন প্যানেল সেশন লগইন",
            message = "অনুমোদিত অ্যাডমিন যাচাইকৃত সেশনে লগইন করেছেন।",
            actorName = "fyslahm28@gmail.com",
            targetPhone = AdminSecurityHelper.ADMIN_MOBILE_NUMBER
        )

        assertTrue(message.contains("6255562134"))
        assertTrue(message.contains("8982853197"))
        assertFalse(message.contains("Q_kormosngsthan_Bangladesh"))
        assertFalse(message.contains("Q_kormosongsthan_Bangladesh"))
        assertTrue(message.contains("সময়:"))
        assertTrue(message.contains("কার্যক্রমের ধরন:"))
        assertTrue(message.contains("সম্পাদনকারী অ্যাডমিন/পরিচয়: fyslahm28@gmail.com"))
        assertTrue(message.contains("01300448147"))

        // Verify sensitive secrets, passwords, OTPs, reset links, and bot tokens are strictly redacted
        val unsafeText = "User password: MySecretPass123 otp=998877 link https://example.firebaseapp.com/__/auth/action?mode=resetPassword&oobCode=SECRET123 bot 123456789:AAHfiqksKZ8WmR2zS7YcX9vB1nM0pL3qW4e key AIzaSyD1234567890abcdefghijklmnopqrstuvwx"
        val sanitized = TelegramNotificationHelper.sanitizeSensitiveContent(unsafeText)

        assertFalse(sanitized.contains("MySecretPass123"))
        assertFalse(sanitized.contains("998877"))
        assertFalse(sanitized.contains("oobCode=SECRET123"))
        assertFalse(sanitized.contains("AAHfiqksKZ8WmR2zS7YcX9vB1nM0pL3qW4e"))
        assertFalse(sanitized.contains("AIzaSyD1234567890abcdefghijklmnopqrstuvwx"))
        assertTrue(sanitized.contains("[REDACTED]"))
        assertTrue(sanitized.contains("[REDACTED_RESET_LINK]"))
        assertTrue(sanitized.contains("[REDACTED_BOT_TOKEN]"))
        assertTrue(sanitized.contains("[REDACTED_FIREBASE_SECRET]"))
    }

    @Test
    fun telegramNotificationHelper_resolvesConfiguredChatIdAndDispatchesSafely() = kotlinx.coroutines.runBlocking {
        val targetChatId = TelegramNotificationHelper.resolveTargetChatId()
        val configuredBotId = TelegramNotificationHelper.resolveConfiguredBotId()
        assertEquals("6255562134", targetChatId)
        assertEquals("8982853197", configuredBotId)
        // Attempt dispatch using BuildConfig credentials (gracefully returns Boolean without throwing)
        val dispatched = TelegramNotificationHelper.dispatchToTelegramBot(
            eventType = "ADMIN_SECURITY",
            title = "অ্যাডমিন প্যানেল সংযোগ পরীক্ষা",
            message = "কওমি কর্মসংস্থান অ্যাপ থেকে টেলিগ্রাম নোটিফিকেশন সিস্টেম সফলভাবে সচল রয়েছে।",
            actorName = "fyslahm28@gmail.com"
        )
        println("Telegram dispatch result in test environment: $dispatched (chatId=$targetChatId, botId=$configuredBotId)")
    }

    @Test
    fun telegramNotificationHelper_verifiesRealApiJsonResponsesAndBlocksFalsePositives() = kotlinx.coroutines.runBlocking {
        // 1. Real ok:true response with valid message_id -> DELIVERED
        val okResponse = """{"ok":true,"result":{"message_id":445566,"from":{"id":8982853197,"is_bot":true,"first_name":"Qawmi Kormo Bot","username":"qawmi_kormo_bot"},"chat":{"id":6255562134,"type":"private"}}}"""
        val okDiag = TelegramNotificationHelper.parseTelegramSendMessageJson(
            httpCode = 200,
            rawResponseBody = okResponse,
            isTokenConfigured = true
        )
        assertTrue(okDiag.sendMessageOk)
        assertEquals(445566L, okDiag.deliveredMessageId)
        assertEquals("DELIVERED", okDiag.deliveryStatus)
        assertTrue(okDiag.diagnosticMessageBangla.contains("সফলভাবে পাঠানো হয়েছে"))
        assertTrue(okDiag.diagnosticMessageBangla.contains("#445566"))

        // 2. HTTP 200 WITHOUT ok:true or without message_id -> must NOT report DELIVERED
        val fake200 = """{"ok":true,"result":{}}"""
        val fakeDiag = TelegramNotificationHelper.parseTelegramSendMessageJson(
            httpCode = 200,
            rawResponseBody = fake200,
            isTokenConfigured = true
        )
        assertFalse(fakeDiag.sendMessageOk)
        assertEquals("FAILED", fakeDiag.deliveryStatus)
        assertFalse(fakeDiag.diagnosticMessageBangla.contains("সফলভাবে পাঠানো হয়েছে"))

        // 3. HTTP 401 Unauthorized (Revoked/Invalid Bot Token) -> FAILED with 401 diagnostic
        val err401 = """{"ok":false,"error_code":401,"description":"Unauthorized"}"""
        val diag401 = TelegramNotificationHelper.parseTelegramSendMessageJson(
            httpCode = 401,
            rawResponseBody = err401,
            isTokenConfigured = true
        )
        assertFalse(diag401.sendMessageOk)
        assertEquals("FAILED", diag401.deliveryStatus)
        assertTrue(diag401.diagnosticMessageBangla.contains("401 (Unauthorized)"))
        assertFalse(diag401.diagnosticMessageBangla.contains("সফলভাবে পাঠানো হয়েছে"))

        // 4. HTTP 400 Bad Request: chat not found (/start not sent) -> FAILED with 400 diagnostic
        val err400 = """{"ok":false,"error_code":400,"description":"Bad Request: chat not found"}"""
        val diag400 = TelegramNotificationHelper.parseTelegramSendMessageJson(
            httpCode = 400,
            rawResponseBody = err400,
            isTokenConfigured = true
        )
        assertFalse(diag400.sendMessageOk)
        assertEquals("FAILED", diag400.deliveryStatus)
        assertTrue(diag400.diagnosticMessageBangla.contains("chat not found"))
        assertTrue(diag400.diagnosticMessageBangla.contains("/start"))

        // 5. HTTP 403 Forbidden: bot was blocked by the user -> FAILED with 403 diagnostic
        val err403 = """{"ok":false,"error_code":403,"description":"Forbidden: bot was blocked by the user"}"""
        val diag403 = TelegramNotificationHelper.parseTelegramSendMessageJson(
            httpCode = 403,
            rawResponseBody = err403,
            isTokenConfigured = true
        )
        assertFalse(diag403.sendMessageOk)
        assertEquals("FAILED", diag403.deliveryStatus)
        assertTrue(diag403.diagnosticMessageBangla.contains("403 (Forbidden)"))

        // 6. Missing or placeholder TELEGRAM_BOT_TOKEN -> must report FAILED and NEVER claim success
        try {
            TelegramNotificationHelper.clearDeliveredEventKeysForTest()
            TelegramNotificationHelper.setTokenOverrideForTest("TELEGRAM_BOT_TOKEN_DEFAULT_VALUE")
            assertFalse(TelegramNotificationHelper.isTelegramBotConfigured())

            val missingGetMe = TelegramNotificationHelper.verifyBotConnectionWithGetMe()
            assertFalse(missingGetMe.isTokenConfigured)
            assertFalse(missingGetMe.getMeVerified)
            assertFalse(missingGetMe.sendMessageOk)
            assertEquals("FAILED", missingGetMe.deliveryStatus)
            assertTrue(missingGetMe.diagnosticMessageBangla.contains("TELEGRAM_BOT_TOKEN কনফিগার করা নেই"))
            assertFalse(missingGetMe.diagnosticMessageBangla.contains("সফলভাবে পাঠানো হয়েছে"))

            val missingDispatch = TelegramNotificationHelper.dispatchToTelegramBotVerified(
                eventType = "ADMIN_SECURITY",
                title = "টোকেন বিহীন পরীক্ষা",
                message = "টোকেন ছাড়া মেসেজ সফল দেখানো যাবে না",
                actorName = "অ্যাডমিন",
                bypassDedup = true
            )
            assertFalse(missingDispatch.isTokenConfigured)
            assertFalse(missingDispatch.sendMessageOk)
            assertEquals("FAILED", missingDispatch.deliveryStatus)
            assertFalse(missingDispatch.diagnosticMessageBangla.contains("সফলভাবে পাঠানো হয়েছে"))
        } finally {
            TelegramNotificationHelper.setTokenOverrideForTest(null)
        }

        // 7. Token normalization strips quotes and 'bot' prefix safely
        assertEquals("", TelegramNotificationHelper.normalizeBotToken("TELEGRAM_BOT_TOKEN_DEFAULT_VALUE"))
        assertEquals("", TelegramNotificationHelper.normalizeBotToken("   "))
        assertFalse(TelegramNotificationHelper.isBotTokenStructurallyValid("invalid_token_without_colon"))
    }

    @Test
    fun hanafiPrayerSchedule_calculatesAccurateStartAndEndTimesAndDistrictDifferences() {
        val helper = com.example.ui.util.IslamicDateAndPrayerHelper
        // Verify all 64 districts of Bangladesh are configured
        assertEquals(64, helper.allBangladeshDistrictCoords.size)

        // Calculate Dhaka (23.8103, 90.4125) on October 23, 2026 during Asr (16:15 = 975 min)
        val dhakaOct23 = helper.calculateHanafiScheduleForCoordinates(
            districtNameBangla = "ঢাকা",
            latitude = 23.8103,
            longitude = 90.4125,
            year = 2026,
            month = 10,
            day = 23,
            nowMinutesOfDay = 16 * 60 + 15
        )

        assertEquals(5, dhakaOct23.waqts.size)
        val fajr = dhakaOct23.waqts[0]
        val dhuhr = dhakaOct23.waqts[1]
        val asr = dhakaOct23.waqts[2]
        val maghrib = dhakaOct23.waqts[3]
        val isha = dhakaOct23.waqts[4]

        // Every prayer has both start and end times
        dhakaOct23.waqts.forEach { w ->
            assertTrue("Start time must not be blank for ${w.nameBangla}", w.timeBangla.isNotBlank())
            assertTrue("End time must not be blank for ${w.nameBangla}", w.endTimeBangla.isNotBlank())
        }

        // Verify Hanafi Asr on Oct 23 in Dhaka starts around 03:52 PM (952 min),
        // NOT Shafi'i (~03:02 PM = 902 min)
        assertTrue(
            "Expected Hanafi Asr start between 15:48 and 16:00, got ${asr.startMinutes}",
            asr.startMinutes in (15 * 60 + 48)..(16 * 60)
        )
        // Verify Dhuhr ends 1 minute before Hanafi Asr starts
        assertEquals(asr.startMinutes - 1, dhuhr.endMinutes)
        // Verify Fajr ends 1 minute before Sunrise, and Maghrib starts 3 minutes after Asr ends (Sunset)
        assertTrue(fajr.endMinutes > fajr.startMinutes)
        assertEquals(asr.endMinutes + 3, maghrib.startMinutes)
        assertEquals(isha.startMinutes - 1, maghrib.endMinutes)

        // At 16:15 (4:15 PM), Asr must be the active waqt and Maghrib must be the next prayer
        assertTrue(asr.isCurrentWaqt)
        assertTrue(dhakaOct23.currentWaqtNameBangla.contains("আসর"))
        assertTrue(dhakaOct23.nextWaqtNameBangla.contains("মাগরিব"))

        // Verify geographical longitudinal difference: Sylhet (East, 91.87E) vs Rajshahi (West, 88.60E)
        val sylhetOct23 = helper.calculateHanafiScheduleForCoordinates(
            districtNameBangla = "সিলেট",
            latitude = 24.8949,
            longitude = 91.8687,
            year = 2026,
            month = 10,
            day = 23,
            nowMinutesOfDay = 12 * 60
        )
        val rajshahiOct23 = helper.calculateHanafiScheduleForCoordinates(
            districtNameBangla = "রাজশাহী",
            latitude = 24.3745,
            longitude = 88.6042,
            year = 2026,
            month = 10,
            day = 23,
            nowMinutesOfDay = 12 * 60
        )

        // Sylhet prayer times are earlier than Dhaka, and Rajshahi prayer times are later than Dhaka
        assertTrue(sylhetOct23.waqts[1].startMinutes < dhakaOct23.waqts[1].startMinutes)
        assertTrue(rajshahiOct23.waqts[1].startMinutes > dhakaOct23.waqts[1].startMinutes)
    }

    @Test
    fun appUpdateManagerHelper_parsesGitHubAndFirestoreReleasesAndPicksNewestVersion() {
        val helper = com.example.ui.util.AppUpdateManagerHelper

        // 1. Parse GitHub Releases API JSON for v1.0.40 (Build #140)
        val githubJson = """
            {
              "tag_name": "v1.0.40",
              "name": "কওমি কর্মসংস্থান বাংলাদেশ v1.0.40 (Build #40)",
              "draft": false,
              "prerelease": false,
              "body": "## কওমি কর্মসংস্থান বাংলাদেশ — নতুন আপডেট (v1.0.40)\n- **ভার্সন:** 1.0.40\n- **ভার্সন কোড:** 140\nনতুন অটো আপডেট ও পারফরম্যান্স উন্নয়ন।",
              "assets": [
                {
                  "name": "Qawmi-Kormosangsthan-Bangladesh.apk",
                  "size": 26107235,
                  "browser_download_url": "https://github.com/fyslahm28-hub/Faitu-kaj-bal/releases/download/v1.0.40/Qawmi-Kormosangsthan-Bangladesh.apk"
                }
              ]
            }
        """.trimIndent()
        val parsedGitHub = helper.parseGitHubReleaseJson(githubJson)
        assertNotNull("Expected GitHub Release JSON to parse", parsedGitHub)
        assertEquals(140, parsedGitHub!!.versionCode)
        assertEquals("1.0.40", parsedGitHub.versionName)
        assertEquals("v1.0.40", parsedGitHub.githubReleaseTag)
        assertEquals(
            "https://github.com/fyslahm28-hub/Faitu-kaj-bal/releases/download/v1.0.40/Qawmi-Kormosangsthan-Bangladesh.apk",
            parsedGitHub.apkDownloadUrl
        )
        assertEquals(26107235L, parsedGitHub.apkFileSizeBytes)

        // 2. Parse Firestore REST API JSON for /app_releases/latest
        val firestoreRestJson = """
            {
              "name": "projects/gen-lang-client-0048059944/databases/ai-studio-android-811ea734-e9e8-4ff4-8e56-ca97ec8a429c/documents/app_releases/latest",
              "fields": {
                "versionCode": { "integerValue": "140" },
                "versionName": { "stringValue": "1.0.40" },
                "apkDownloadUrl": { "stringValue": "https://github.com/fyslahm28-hub/Faitu-kaj-bal/releases/download/v1.0.40/Qawmi-Kormosangsthan-Bangladesh.apk" },
                "apkSha256": { "stringValue": "b4591d1e09a8a5aed8bd8992f35b74bf5681bbd9854fb68c4e3708aa47b7a108" },
                "apkFileSizeBytes": { "integerValue": "26107235" },
                "releaseNotes": { "stringValue": "নতুন আপডেট ও বাগ ফিক্স।" },
                "githubReleaseTag": { "stringValue": "v1.0.40" },
                "isMandatory": { "booleanValue": true },
                "isPublished": { "booleanValue": true },
                "releaseStatus": { "stringValue": "PUBLISHED" }
              }
            }
        """.trimIndent()
        val parsedFirestore = helper.parseFirestoreRestReleaseJson(firestoreRestJson)
        assertNotNull("Expected Firestore REST JSON to parse", parsedFirestore)
        assertEquals(140, parsedFirestore!!.versionCode)
        assertEquals("1.0.40", parsedFirestore.versionName)
        assertEquals("b4591d1e09a8a5aed8bd8992f35b74bf5681bbd9854fb68c4e3708aa47b7a108", parsedFirestore.apkSha256)
        assertTrue(parsedFirestore.isMandatory)

        // 3. Stale local cache (versionCode 136) must NEVER override newer remote release (versionCode 140)
        val staleCachedRelease = parsedFirestore.copy(versionCode = 136, versionName = "1.0.36")
        val picked = helper.pickMoreRecentRelease(staleCachedRelease, parsedGitHub)
        assertNotNull(picked)
        assertEquals(140, picked!!.versionCode)
        assertEquals("1.0.40", picked.versionName)

        // 4. Same version merges richer metadata (e.g. SHA-256 from Firestore + GitHub release)
        val mergedSameVersion = helper.pickMoreRecentRelease(parsedGitHub, parsedFirestore)
        assertEquals(140, mergedSameVersion!!.versionCode)
        assertEquals("b4591d1e09a8a5aed8bd8992f35b74bf5681bbd9854fb68c4e3708aa47b7a108", mergedSameVersion.apkSha256)
        assertTrue(mergedSameVersion.isMandatory)

        // 5. Version comparison only triggers update prompt when remote > installed
        assertTrue(helper.isUpdateAvailable(136, "1.0.36", mergedSameVersion))
        assertFalse(helper.isUpdateAvailable(140, "1.0.40", mergedSameVersion))
        assertFalse(helper.isUpdateAvailable(141, "1.0.41", mergedSameVersion))
    }

    @Test
    fun messageDeliveryStatusAndCleanDisplayBody_resolvesSentDeliveredSeenFailedAndStripsWrongLabels() {
        val nowMs = 1_750_000_000_000L
        val msgCreatedTs = com.google.firebase.Timestamp(nowMs / 1000L, 0)

        // 1. Plain text message -> shows only text, status = SENT (✓)
        val plainTextMsg = com.example.data.model.DirectMessage(
            id = "msg_text_1",
            senderId = "user_a",
            receiverId = "user_b",
            body = "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ",
            imageUrls = emptyList(),
            isRead = false,
            readByUserIds = listOf("user_a"),
            createdAt = msgCreatedTs
        )
        assertEquals("আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ", plainTextMsg.cleanDisplayBody())
        assertEquals(
            "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ",
            com.example.ui.screens.formatMessagePreviewForInbox(plainTextMsg)
        )
        assertEquals(
            com.example.data.model.MessageDeliveryStatus.SENT,
            plainTextMsg.resolveDeliveryStatus(partnerProfile = null, nowMillis = nowMs)
        )

        // 2. Image message without caption (even if legacy auto-label was stored) -> cleanDisplayBody is empty so only thumbnail renders
        val imageOnlyMsg = com.example.data.model.DirectMessage(
            id = "msg_img_1",
            senderId = "user_a",
            receiverId = "user_b",
            body = "📷 ১ টি ছবি প্রেরণ করা হয়েছে",
            imageUrls = listOf("https://example.com/chat_photo.jpg"),
            isRead = false,
            readByUserIds = listOf("user_a"),
            createdAt = msgCreatedTs
        )
        assertEquals("", imageOnlyMsg.cleanDisplayBody())
        assertEquals("📷 ছবি", com.example.ui.screens.formatMessagePreviewForInbox(imageOnlyMsg))

        val imageWithSendingLabel = imageOnlyMsg.copy(body = "ছবি মেসেজ পাঠানো হচ্ছে")
        assertEquals("", imageWithSendingLabel.cleanDisplayBody())

        // 3. Delivered status (✓✓) when recipient device acknowledges delivery ("DELIVERED:user_b") or recipient is online
        val deliveredMsg = plainTextMsg.copy(
            readByUserIds = listOf("user_a", "DELIVERED:user_b")
        )
        assertFalse("DELIVERED marker must keep message unread for recipient until opened", !deliveredMsg.isUnreadForUser("user_b"))
        assertEquals(
            com.example.data.model.MessageDeliveryStatus.DELIVERED,
            deliveredMsg.resolveDeliveryStatus(partnerProfile = null, nowMillis = nowMs)
        )

        // 4. Seen/Read status (colored ✓✓) when recipient opens conversation
        val seenMsg = plainTextMsg.copy(
            isRead = true,
            readByUserIds = listOf("user_a", "user_b")
        )
        assertEquals(
            com.example.data.model.MessageDeliveryStatus.SEEN,
            seenMsg.resolveDeliveryStatus(partnerProfile = null, nowMillis = nowMs)
        )

        // 5. Failed status (!) and Sending status
        val failedMsg = plainTextMsg.copy().apply { localDeliveryState = "FAILED" }
        assertEquals(
            com.example.data.model.MessageDeliveryStatus.FAILED,
            failedMsg.resolveDeliveryStatus(partnerProfile = null, nowMillis = nowMs)
        )
        val sendingMsg = plainTextMsg.copy().apply { localDeliveryState = "SENDING" }
        assertEquals(
            com.example.data.model.MessageDeliveryStatus.SENDING,
            sendingMsg.resolveDeliveryStatus(partnerProfile = null, nowMillis = nowMs)
        )

        // 6. Media attachment category detection for Image, Video, Audio, Document
        assertEquals(
            com.example.ui.screens.AttachmentMediaCategory.IMAGE,
            com.example.ui.screens.detectAttachmentMediaCategory("https://example.com/photo.jpg")
        )
        assertEquals(
            com.example.ui.screens.AttachmentMediaCategory.VIDEO,
            com.example.ui.screens.detectAttachmentMediaCategory("https://example.com/lecture.mp4")
        )
        assertEquals(
            com.example.ui.screens.AttachmentMediaCategory.AUDIO,
            com.example.ui.screens.detectAttachmentMediaCategory("https://example.com/tilawat.mp3")
        )
        assertEquals(
            com.example.ui.screens.AttachmentMediaCategory.DOCUMENT,
            com.example.ui.screens.detectAttachmentMediaCategory("https://example.com/cv_biodata.pdf")
        )
    }

    @Test
    fun firestoreErrorHandler_translatesDistinctErrorCodesWithoutFalseNetworkBlame() {
        val permDenied = com.example.data.repository.translateFirestoreExceptionToBangla(
            RuntimeException("PERMISSION_DENIED: Missing or insufficient permissions.")
        )
        assertTrue(permDenied.message.orEmpty().contains("Permission Denied"))
        assertFalse(permDenied.message.orEmpty().contains("ইন্টারনেট সংযোগ ধীর"))

        val unauth = com.example.data.repository.translateFirestoreExceptionToBangla(
            RuntimeException("UNAUTHENTICATED: Request had invalid authentication credentials.")
        )
        assertTrue(unauth.message.orEmpty().contains("লগইন সেশন"))

        val invalidArg = com.example.data.repository.translateFirestoreExceptionToBangla(
            RuntimeException("INVALID_ARGUMENT: Document field value is invalid")
        )
        assertTrue(invalidArg.message.orEmpty().contains("ফরম্যাট বা আকার সঠিক নয়"))

        val notFound = com.example.data.repository.translateFirestoreExceptionToBangla(
            RuntimeException("NOT_FOUND: Some requested document was not found.")
        )
        assertTrue(notFound.message.orEmpty().contains("সার্ভারে পাওয়া যায়নি"))

        val unavailable = com.example.data.repository.translateFirestoreExceptionToBangla(
            RuntimeException("UNAVAILABLE: io exception")
        )
        assertTrue(unavailable.message.orEmpty().contains("সার্ভারের সাথে সংযোগ"))
    }
}



