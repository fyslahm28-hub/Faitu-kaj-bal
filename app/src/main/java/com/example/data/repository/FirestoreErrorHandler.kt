package com.example.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun translateFirestoreExceptionToBangla(exception: Exception): Exception {
    val msg = exception.message.orEmpty()
    if (msg.contains("exceeds", ignoreCase = true) ||
        msg.contains("1 MiB", ignoreCase = true) ||
        msg.contains("1048576", ignoreCase = true) ||
        msg.contains("maximum size", ignoreCase = true) ||
        (msg.contains("INVALID_ARGUMENT", ignoreCase = true) && msg.contains("feed_posts", ignoreCase = true))
    ) {
        return IllegalArgumentException(
            com.example.ui.util.ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA,
            exception
        )
    }
    if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
        msg.contains("Missing or insufficient permissions", ignoreCase = true)
    ) {
        return IllegalStateException(
            "নিরাপত্তা বিধিমালার কারণে অনুরোধটি সম্পন্ন করা যায়নি। অনুগ্রহ করে লগইন ও প্রয়োজনীয় তথ্য যাচাই করে পুনরায় চেষ্টা করুন।",
            exception
        )
    }
    if (msg.contains("UNAUTHENTICATED", ignoreCase = true)) {
        return IllegalStateException(
            "আপনার লগইন সেশন মেয়াদোত্তীর্ণ হয়েছে। অনুগ্রহ করে পুনরায় সাইন-ইন করে চেষ্টা করুন।",
            exception
        )
    }
    if (msg.contains("NOT_FOUND", ignoreCase = true)) {
        return IllegalStateException(
            "কাঙ্ক্ষিত তথ্যটি সার্ভারে পাওয়া যায়নি। অনুগ্রহ করে পৃষ্ঠাটি রিফ্রেশ করে পুনরায় চেষ্টা করুন।",
            exception
        )
    }
    if (msg.contains("UNAVAILABLE", ignoreCase = true) ||
        msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
        msg.contains("Unable to resolve host", ignoreCase = true)
    ) {
        return IllegalStateException(
            "সার্ভারের সাথে সংযোগ স্থাপনে সাময়িক বিঘ্ন ঘটেছে। অনুগ্রহ করে কিছুক্ষণ পর পুনরায় চেষ্টা করুন।",
            exception
        )
    }
    return exception
}

/**
 * Fetches a [DocumentSnapshot] with a bounded server timeout, falling back to local Firestore
 * cache (`Source.CACHE`) if the server round-trip exceeds [timeoutMs].
 */
suspend fun DocumentReference.getFastOrNull(timeoutMs: Long = 3_500L): DocumentSnapshot? {
    val fromServer = kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
        runCatching { this@getFastOrNull.get().await() }.getOrNull()
    }
    if (fromServer != null) return fromServer
    return kotlinx.coroutines.withTimeoutOrNull(1_500L) {
        runCatching { this@getFastOrNull.get(Source.CACHE).await() }.getOrNull()
    }
}

/**
 * Awaits a Firestore Task (typically a write operation such as set/update/delete/batch.commit).
 *
 * In the Firebase Firestore Android SDK, write operations are immediately staged into the local
 * persistent SQLite cache and emitted to active SnapshotListeners, while the returned Task<Void>
 * waits for the remote gRPC Write stream ACK after all prior queued mutations complete.
 *
 * - If the task completes or fails with a server error (e.g., PERMISSION_DENIED, INVALID_ARGUMENT)
 *   within [timeoutMs], the result or exception is propagated directly.
 * - If the gRPC stream takes longer than [timeoutMs] to ACK (e.g., due to gRPC stream reconnection
 *   or queued background mutations) and the task has NOT failed, we do not throw a false network
 *   disconnection error because Firestore has already persisted the write locally and will finish
 *   syncing it in the background.
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitWithTimeout(
    timeoutMs: Long = 8_000L
): T {
    return try {
        kotlinx.coroutines.withTimeout(timeoutMs) {
            this@awaitWithTimeout.await()
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        val taskException = this@awaitWithTimeout.exception
        if (taskException != null) {
            throw taskException
        }
        if (this@awaitWithTimeout.isSuccessful) {
            return this@awaitWithTimeout.result as T
        }
        this@awaitWithTimeout.addOnFailureListener { asyncErr ->
            Log.w("FirestoreError", "Background-synced Firestore task eventually failed: ${asyncErr.message}", asyncErr)
        }
        null as T
    }
}
