package com.example.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

private val configuredFirestoreDatabases = ConcurrentHashMap<String, Boolean>()

/**
 * Returns a [FirebaseFirestore] instance configured with [MemoryCacheSettings] before its first
 * operation. This guarantees that stale, oversized, or wedged local SQLite mutations from earlier
 * app sessions on a user's device can never clog Firestore's single FIFO gRPC write stream.
 */
fun getConfiguredFirestoreInstance(databaseId: String = ""): FirebaseFirestore {
    val cleanId = databaseId.trim()
    val db = runCatching {
        if (cleanId.isNotBlank() && cleanId != "(default)") {
            FirebaseFirestore.getInstance(cleanId)
        } else {
            FirebaseFirestore.getInstance()
        }
    }.getOrElse {
        FirebaseFirestore.getInstance()
    }
    val cacheKey = cleanId.ifBlank { "(default)" }
    if (configuredFirestoreDatabases.putIfAbsent(cacheKey, true) == null) {
        runCatching {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
        }
    }
    return db
}

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
    var rootFirestoreEx: com.google.firebase.firestore.FirebaseFirestoreException? = null
    var current: Throwable? = exception
    while (current != null) {
        if (current is com.google.firebase.firestore.FirebaseFirestoreException) {
            rootFirestoreEx = current
            break
        }
        current = current.cause
    }

    val codeName = runCatching { rootFirestoreEx?.code?.name }.getOrNull().orEmpty()
    val msg = (rootFirestoreEx?.message ?: exception.message).orEmpty()

    if (msg.contains("exceeds", ignoreCase = true) ||
        msg.contains("1 MiB", ignoreCase = true) ||
        msg.contains("1048576", ignoreCase = true) ||
        msg.contains("maximum size", ignoreCase = true) ||
        ((codeName == "INVALID_ARGUMENT" || msg.contains("INVALID_ARGUMENT", ignoreCase = true)) &&
            msg.contains("feed_posts", ignoreCase = true))
    ) {
        return IllegalArgumentException(
            com.example.ui.util.ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA,
            exception
        )
    }
    if (codeName == "PERMISSION_DENIED" ||
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
        msg.contains("Missing or insufficient permissions", ignoreCase = true)
    ) {
        return IllegalStateException(
            "নিরাপত্তা বিধিমালার (Permission Denied) কারণে অনুরোধটি সম্পন্ন করা যায়নি। অনুগ্রহ করে লগইন ও প্রয়োজনীয় তথ্য যাচাই করে পুনরায় চেষ্টা করুন।",
            exception
        )
    }
    if (codeName == "UNAUTHENTICATED" ||
        msg.contains("UNAUTHENTICATED", ignoreCase = true)
    ) {
        return IllegalStateException(
            "আপনার লগইন সেশন মেয়াদোত্তীর্ণ হয়েছে। অনুগ্রহ করে পুনরায় সাইন-ইন করে চেষ্টা করুন।",
            exception
        )
    }
    if (codeName == "INVALID_ARGUMENT" ||
        msg.contains("INVALID_ARGUMENT", ignoreCase = true)
    ) {
        return IllegalArgumentException(
            "প্রদত্ত তথ্যের ফরম্যাট বা আকার সঠিক নয়। অনুগ্রহ করে তথ্য যাচাই করে পুনরায় চেষ্টা করুন।",
            exception
        )
    }
    if (codeName == "NOT_FOUND" ||
        msg.contains("NOT_FOUND", ignoreCase = true)
    ) {
        return IllegalStateException(
            "কাঙ্ক্ষিত তথ্যটি সার্ভারে পাওয়া যায়নি। অনুগ্রহ করে পৃষ্ঠাটি রিফ্রেশ করে পুনরায় চেষ্টা করুন।",
            exception
        )
    }
    if (codeName == "UNAVAILABLE" ||
        codeName == "DEADLINE_EXCEEDED" ||
        msg.contains("UNAVAILABLE", ignoreCase = true) ||
        msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
        msg.contains("Unable to resolve host", ignoreCase = true)
    ) {
        return IllegalStateException(
            "সার্ভারের সাথে সংযোগ স্থাপনে সাময়িক বিঘ্ন ঘটেছে। অনুগ্রহ করে কিছুক্ষণ পর পুনরায় চেষ্টা করুন।",
            exception
        )
    }
    if (msg.contains("Error getting Query snapshot", ignoreCase = true) ||
        msg.contains("Failed to get document", ignoreCase = true)
    ) {
        return IllegalStateException(
            "সার্ভার থেকে তথ্য লোড করতে সাময়িক বিঘ্ন ঘটেছে। অনুগ্রহ করে পুনরায় চেষ্টা করুন।",
            exception
        )
    }
    return exception
}

fun formatFirestoreErrorBangla(throwable: Throwable?, defaultBangla: String): String {
    if (throwable == null) return defaultBangla
    val translated = if (throwable is Exception) {
        translateFirestoreExceptionToBangla(throwable)
    } else {
        throwable
    }
    val msg = translated.localizedMessage?.trim().orEmpty()
    if (msg.isBlank() ||
        msg.contains("Error getting Query snapshot", ignoreCase = true) ||
        msg.contains("Failed to get documents", ignoreCase = true) ||
        msg.contains("Failed to get document", ignoreCase = true)
    ) {
        return defaultBangla
    }
    return msg
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
 * Reads a [DocumentSnapshot] strictly from the local Firestore SQLite/memory cache (`Source.CACHE`)
 * without ever making a blocking network round-trip. Returns `null` immediately if not cached.
 */
suspend fun DocumentReference.getFromCacheOnlyOrNull(timeoutMs: Long = 50L): DocumentSnapshot? {
    return kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
        runCatching { this@getFromCacheOnlyOrNull.get(Source.CACHE).await() }.getOrNull()
    }?.takeIf { it.exists() }
}

/**
 * Fetches a [DocumentSnapshot] from local Firestore cache (`Source.CACHE`) first for zero-latency
 * checks, falling back to a bounded server fetch only if the document is not yet cached locally.
 */
suspend fun DocumentReference.getCacheFirstOrNull(serverTimeoutMs: Long = 1_200L): DocumentSnapshot? {
    val fromCache = getFromCacheOnlyOrNull(60L)
    if (fromCache != null) return fromCache
    return kotlinx.coroutines.withTimeoutOrNull(serverTimeoutMs) {
        runCatching { this@getCacheFirstOrNull.get().await() }.getOrNull()
    }
}

/**
 * Determines whether a message write failure is a transient connectivity/server condition that
 * should be automatically retried with exponential backoff (or kept in the local offline queue),
 * versus a permanent validation/security/blocking error that should fail immediately.
 */
fun isRetryableFirestoreMessageError(throwable: Throwable?): Boolean {
    if (throwable == null) return false
    if (throwable is IllegalArgumentException) return false

    var rootFirestoreEx: com.google.firebase.firestore.FirebaseFirestoreException? = null
    var current: Throwable? = throwable
    while (current != null) {
        if (current is com.google.firebase.firestore.FirebaseFirestoreException) {
            rootFirestoreEx = current
            break
        }
        if (current is kotlinx.coroutines.TimeoutCancellationException) {
            return true
        }
        current = current.cause
    }

    val codeName = runCatching { rootFirestoreEx?.code?.name }.getOrNull().orEmpty()
    val msg = (rootFirestoreEx?.message ?: throwable.message).orEmpty()

    if (codeName == "PERMISSION_DENIED" ||
        codeName == "UNAUTHENTICATED" ||
        codeName == "INVALID_ARGUMENT" ||
        codeName == "NOT_FOUND" ||
        codeName == "ALREADY_EXISTS" ||
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
        msg.contains("Missing or insufficient permissions", ignoreCase = true) ||
        msg.contains("UNAUTHENTICATED", ignoreCase = true) ||
        msg.contains("INVALID_ARGUMENT", ignoreCase = true) ||
        msg.contains("ব্লক", ignoreCase = true) ||
        msg.contains("সীমাবদ্ধ", ignoreCase = true)
    ) {
        return false
    }

    return codeName == "UNAVAILABLE" ||
        codeName == "DEADLINE_EXCEEDED" ||
        codeName == "ABORTED" ||
        codeName == "RESOURCE_EXHAUSTED" ||
        msg.contains("UNAVAILABLE", ignoreCase = true) ||
        msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
        msg.contains("Unable to resolve host", ignoreCase = true) ||
        msg.contains("সার্ভারের সাথে সংযোগ", ignoreCase = true) ||
        msg.contains("নির্ধারিত সময়ের বেশি", ignoreCase = true)
}

/**
 * Awaits a Firestore message write task within [timeoutMs].
 *
 * - Immediately propagates any real server/security exception (`PERMISSION_DENIED`,
 *   `UNAUTHENTICATED`, `INVALID_ARGUMENT`, etc.) if the write is rejected.
 * - If the remote gRPC ACK takes longer than [timeoutMs] on a slow mobile connection
 *   (e.g., 25–30 KB/s), verifies that the document has been persisted in Firestore's local
 *   SQLite cache without error so Firestore's native background write stream can finish
 *   uploading it while the UI transitions out of the `SENDING` spinner to `SENT` (`✓`).
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitMessageWriteConfirmation(
    docRef: DocumentReference,
    timeoutMs: Long = 3_500L
): T {
    return try {
        kotlinx.coroutines.withTimeout(timeoutMs) {
            this@awaitMessageWriteConfirmation.await()
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        val taskException = this@awaitMessageWriteConfirmation.exception
        if (taskException != null) {
            throw taskException
        }
        if (this@awaitMessageWriteConfirmation.isSuccessful) {
            return this@awaitMessageWriteConfirmation.result as T
        }
        val confirmedInCache = docRef.getFromCacheOnlyOrNull(100L)
        val postCheckException = this@awaitMessageWriteConfirmation.exception
        if (postCheckException != null) {
            throw postCheckException
        }
        if (confirmedInCache != null && confirmedInCache.exists()) {
            this@awaitMessageWriteConfirmation.addOnFailureListener { asyncErr ->
                Log.w("FirestoreError", "Background-synced message write eventually failed: ${asyncErr.message}", asyncErr)
            }
            return null as T
        }
        throw e
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
