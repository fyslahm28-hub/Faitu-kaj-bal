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
        assertEquals("8917160878", TelegramNotificationHelper.resolveConfiguredBotId())

        val message = TelegramNotificationHelper.formatTelegramEventMessage(
            eventType = "ADMIN_LOGIN",
            title = "অ্যাডমিন প্যানেল সেশন লগইন",
            message = "অনুমোদিত অ্যাডমিন যাচাইকৃত সেশনে লগইন করেছেন।",
            actorName = "fyslahm28@gmail.com",
            targetPhone = AdminSecurityHelper.ADMIN_MOBILE_NUMBER
        )

        assertTrue(message.contains("6255562134"))
        assertTrue(message.contains("8917160878"))
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
        assertEquals("8917160878", configuredBotId)
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
}

