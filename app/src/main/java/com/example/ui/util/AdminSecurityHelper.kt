package com.example.ui.util

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import java.util.Locale

object AdminSecurityHelper {

    val AUTHORIZED_ADMIN_EMAILS: Set<String> = setOf(
        "foysalahamad102@gmail.com",
        "foysalahamad204@gmail.com",
        "fyslahm28@gmail.com"
    )

    const val ADMIN_MOBILE_NUMBER = "01300448147"
    const val ADMIN_MOBILE_E164 = "+8801300448147"

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
        return normalized == ADMIN_MOBILE_NUMBER
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
        return try {
            val tokenResult = user.getIdToken(false).await()
            val claims = tokenResult.claims
            val hasAdminClaim = (claims["admin"] as? Boolean == true) ||
                (claims["role"] as? String == "admin") ||
                (claims["role"] as? String == "SUPER_ADMIN")
            val tokenEmail = (claims["email"] as? String) ?: user.email
            val tokenPhone = (claims["phone_number"] as? String) ?: user.phoneNumber
            isFirebaseAuthUserAdmin(
                authEmail = tokenEmail,
                authPhone = tokenPhone,
                hasCustomAdminClaim = hasAdminClaim
            )
        } catch (_: Exception) {
            isFirebaseAuthUserAdmin(
                authEmail = user.email,
                authPhone = user.phoneNumber
            )
        }
    }
}
