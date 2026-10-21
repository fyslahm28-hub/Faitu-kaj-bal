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
    fun telegramNotificationHelper_formatsChannelAndRedactsSensitiveSecrets() {
        assertEquals("@Q_kormosongsthan_Bangladesh", TelegramNotificationHelper.DEFAULT_TELEGRAM_CHANNEL)

        val message = TelegramNotificationHelper.formatTelegramEventMessage(
            eventType = "ADMIN_LOGIN",
            title = "অ্যাডমিন প্যানেল সেশন লগইন",
            message = "অনুমোদিত অ্যাডমিন যাচাইকৃত সেশনে লগইন করেছেন।",
            actorName = "fyslahm28@gmail.com",
            targetPhone = AdminSecurityHelper.ADMIN_MOBILE_NUMBER
        )

        assertTrue(message.contains("@Q_kormosongsthan_Bangladesh"))
        assertTrue(message.contains("সময়:"))
        assertTrue(message.contains("কার্যক্রমের ধরন:"))
        assertTrue(message.contains("সংশ্লিষ্ট পরিচয়: fyslahm28@gmail.com"))
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
}
