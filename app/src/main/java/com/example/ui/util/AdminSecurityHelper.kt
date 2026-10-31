package com.example.ui.util

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object AdminSecurityHelper {

    val AUTHORIZED_ADMIN_EMAILS: Set<String> = setOf(
        "foysalahamad102@gmail.com",
        "foysalahamad204@gmail.com",
        "fyslahm28@gmail.com"
    )

    const val ADMIN_MOBILE_NUMBER = "01300448147"
    const val ADMIN_MOBILE_E164 = "+8801300448147"

    // Password reset rate-limiting: max 3 requests per hour, minimum 60s cooldown per email
    private const val RESET_COOLDOWN_MS = 60_000L
    private const val RESET_WINDOW_MS = 3_600_000L
    private const val MAX_RESETS_PER_WINDOW = 3
    private val passwordResetHistory = ConcurrentHashMap<String, MutableList<Long>>()

    fun normalizeEmail(email: String?): String {
        return email?.trim()?.lowercase(Locale.ROOT).orEmpty()
    }

    fun normalizeBdPhone(phone: String?): String {
        val digits = phone?.filter { it.isDigit() }.orEmpty()
        return when {
            digits.startsWith("880") && digits.length == 13 -> digits.removePrefix("88")
            digits.length == 11 && digits.startsWith("01") -> digits
            else -> digits
        }
    }

    fun isAuthorizedAdminEmail(email: String?): Boolean {
        val normalized = normalizeEmail(email)
        return normalized.isNotBlank() && normalized in AUTHORIZED_ADMIN_EMAILS
    }

    fun isAuthorizedAdminPhone(phone: String?): Boolean {
        val normalized = normalizeBdPhone(phone)
        return normalized.isNotBlank() && normalized == ADMIN_MOBILE_NUMBER
    }

    fun isAuthorizedAdminIdentity(email: String?, phone: String? = null): Boolean {
        return isAuthorizedAdminEmail(email) || isAuthorizedAdminPhone(phone)
    }

    fun resolveEffectiveUserEmail(user: FirebaseUser?): String {
        if (user == null) return ""
        val primary = user.email?.trim().orEmpty()
        if (primary.isNotBlank()) return primary
        val fromProviders = runCatching {
            user.providerData
                .asSequence()
                .mapNotNull { it.email?.trim() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        }.getOrDefault("")
        return fromProviders
    }

    fun resolveEffectiveUserPhone(user: FirebaseUser?): String {
        if (user == null) return ""
        val primary = user.phoneNumber?.trim().orEmpty()
        if (primary.isNotBlank()) return primary
        val fromProviders = runCatching {
            user.providerData
                .asSequence()
                .mapNotNull { it.phoneNumber?.trim() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        }.getOrDefault("")
        return fromProviders
    }

    /**
     * Checks rate-limiting and cooldown for Admin Password Reset requests.
     * Returns null if allowed, or a Bangla error message if rate-limited.
     */
    @Synchronized
    fun checkPasswordResetRateLimit(email: String, nowMs: Long = System.currentTimeMillis()): String? {
        val normalized = normalizeEmail(email)
        val timestamps = passwordResetHistory.getOrPut(normalized) { mutableListOf() }
        timestamps.removeAll { nowMs - it > RESET_WINDOW_MS }

        val lastRequest = timestamps.maxOrNull()
        if (lastRequest != null && (nowMs - lastRequest) < RESET_COOLDOWN_MS) {
            val remainingSec = ((RESET_COOLDOWN_MS - (nowMs - lastRequest)) / 1000L).coerceAtLeast(1L)
            return "নিরাপত্তার স্বার্থে অনুগ্রহ করে $remainingSec সেকেন্ড পর পুনরায় চেষ্টা করুন।"
        }
        if (timestamps.size >= MAX_RESETS_PER_WINDOW) {
            return "অতিরিক্ত অনুরোধ করা হয়েছে। নিরাপত্তার স্বার্থে ১ ঘণ্টায় সর্বোচ্চ ৩ বার পাসওয়ার্ড রিসেট লিংক পাঠানো যায়।"
        }
        return null
    }

    @Synchronized
    fun recordPasswordResetRequest(email: String, nowMs: Long = System.currentTimeMillis()) {
        val normalized = normalizeEmail(email)
        val timestamps = passwordResetHistory.getOrPut(normalized) { mutableListOf() }
        timestamps.add(nowMs)
    }

    @Synchronized
    fun clearPasswordResetRateLimitsForTest() {
        passwordResetHistory.clear()
    }

    /**
     * Checks whether the authenticated FirebaseUser has a verified Admin identity
     * via signed Firebase Auth token email, verified Auth phone number, or server-side Custom Claims.
     * Note: Plain profile fields (UserProfile.contactInfo) are NEVER trusted for admin access.
     */
    fun isFirebaseAuthUserAdmin(
        authEmail: String?,
        authPhone: String? = null,
        hasCustomAdminClaim: Boolean = false,
        hasServerRegistryEntry: Boolean = false
    ): Boolean {
        if (hasCustomAdminClaim || hasServerRegistryEntry) return true
        if (isAuthorizedAdminEmail(authEmail)) return true
        if (isAuthorizedAdminPhone(authPhone)) return true
        return false
    }

    /**
     * Inspects the signed Firebase ID Token (GetTokenResult) from Firebase Authentication
     * to verify Custom Claims (`admin == true` or `role == "admin"`) alongside verified token email/phone.
     */
    suspend fun verifyServerTokenAdminClaims(user: FirebaseUser?): Boolean {
        if (user == null) return false
        val resolvedEmail = resolveEffectiveUserEmail(user)
        val resolvedPhone = resolveEffectiveUserPhone(user)
        if (isAuthorizedAdminIdentity(resolvedEmail, resolvedPhone)) {
            return true
        }
        return try {
            val tokenResult = kotlinx.coroutines.withTimeoutOrNull(2_500L) {
                user.getIdToken(false).await()
            } ?: return isFirebaseAuthUserAdmin(authEmail = resolvedEmail, authPhone = resolvedPhone)
            val claims = tokenResult.claims
            val hasAdminClaim = (claims["admin"] as? Boolean == true) ||
                (claims["role"] as? String == "admin") ||
                (claims["role"] as? String == "SUPER_ADMIN")
            val tokenEmail = (claims["email"] as? String)?.takeIf { it.isNotBlank() } ?: resolvedEmail
            val tokenPhone = (claims["phone_number"] as? String)?.takeIf { it.isNotBlank() } ?: resolvedPhone
            isFirebaseAuthUserAdmin(
                authEmail = tokenEmail,
                authPhone = tokenPhone,
                hasCustomAdminClaim = hasAdminClaim
            )
        } catch (_: Exception) {
            isFirebaseAuthUserAdmin(
                authEmail = resolvedEmail,
                authPhone = resolvedPhone
            )
        }
    }
}
