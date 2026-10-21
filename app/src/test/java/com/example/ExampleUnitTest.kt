package com.example

import com.example.ui.util.AdminSecurityHelper
import com.example.ui.util.TelegramNotificationHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

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
    fun adminSecurityHelper_verifiesFirebaseAuthAdminIdentity_andRejectsRegularUsers() {
        assertTrue(
            AdminSecurityHelper.isFirebaseAuthUserAdmin(
                authEmail = "foysalahamad102@gmail.com",
                authPhone = null,
                hasCustomAdminClaim = false,
                hasServerRegistryEntry = false
            )
        )
        assertTrue(
            AdminSecurityHelper.isFirebaseAuthUserAdmin(
                authEmail = "foysalahamad204@gmail.com",
                authPhone = null,
                hasCustomAdminClaim = false,
                hasServerRegistryEntry = false
            )
        )
        assertTrue(
            AdminSecurityHelper.isFirebaseAuthUserAdmin(
                authEmail = "fyslahm28@gmail.com",
                authPhone = null,
                hasCustomAdminClaim = false,
                hasServerRegistryEntry = true
            )
        )

        // Regular user must never gain admin access
        assertFalse(
            AdminSecurityHelper.isFirebaseAuthUserAdmin(
                authEmail = "someone@example.com",
                authPhone = "+8801800000000",
                hasCustomAdminClaim = false,
                hasServerRegistryEntry = false
            )
        )
    }

    @Test
    fun telegramNotificationHelper_formatsStructuredBanglaMessagesCorrectly() {
        val message = TelegramNotificationHelper.formatTelegramEventMessage(
            eventType = "NEW_CIRCULAR",
            title = "নতুন নিয়োগ বিজ্ঞপ্তি জমা হয়েছে",
            message = "জামিয়া ইসলামিয়া দারুল উলুম নতুন বিজ্ঞপ্তি প্রকাশ করেছে (ঢাকা)।",
            actorName = "মুফতি আব্দুল্লাহ",
            targetPhone = AdminSecurityHelper.ADMIN_MOBILE_NUMBER
        )

        assertTrue(message.contains("কওমি কর্মসংস্থান • টেলিগ্রাম ইত্তেলা"))
        assertTrue(message.contains("নতুন নিয়োগ বিজ্ঞপ্তি জমা হয়েছে"))
        assertTrue(message.contains("মুফতি আব্দুল্লাহ"))
        assertTrue(message.contains("01300448147"))
    }
}
