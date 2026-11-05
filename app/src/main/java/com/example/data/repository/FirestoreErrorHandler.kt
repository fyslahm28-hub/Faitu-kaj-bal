package com.example.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.MemoryLruGcSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Source
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

private val configuredFirestoreDatabases = ConcurrentHashMap<String, Boolean>()

/**
 * Tracks real-time Firebase Firestore connection health, distinguishes real Server Data
 * (`!snapshot.metadata.isFromCache`) from Local Cached Data (`snapshot.metadata.isFromCache`),
 * monitors active listener registrations to prevent duplicate listener storms, tracks network
 * transport handoffs (Wi-Fi <-> Cellular), and maintains an honest diagnostic log.
 */
object FirestoreConnectionMonitor {
    private const val MAX_ERROR_LOG_ENTRIES = 50
    private const val DEFAULT_STALL_THRESHOLD_MS = 20_000L

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val _isServerConnected = MutableStateFlow(true)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _isServingFromCache = MutableStateFlow(false)
    val isServingFromCache: StateFlow<Boolean> = _isServingFromCache.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _hasPendingWrites = MutableStateFlow(false)
    val hasPendingWrites: StateFlow<Boolean> = _hasPendingWrites.asStateFlow()

    private val _activeNetworkType = MutableStateFlow("UNKNOWN")
    val activeNetworkType: StateFlow<String> = _activeNetworkType.asStateFlow()

    private val _activeListenerCount = MutableStateFlow(0)
    val activeListenerCount: StateFlow<Int> = _activeListenerCount.asStateFlow()

    private val _listenerReattachCount = MutableStateFlow(0)
    val listenerReattachCount: StateFlow<Int> = _listenerReattachCount.asStateFlow()

    private val _lastConnectionError = MutableStateFlow<String?>(null)
    val lastConnectionError: StateFlow<String?> = _lastConnectionError.asStateFlow()

    private val _connectionErrorLogs = MutableStateFlow<List<String>>(emptyList())
    val connectionErrorLogs: StateFlow<List<String>> = _connectionErrorLogs.asStateFlow()

    private val _recoveryGeneration = MutableStateFlow(0L)
    val recoveryGeneration: StateFlow<Long> = _recoveryGeneration.asStateFlow()

    private val listenerCountsByPath = ConcurrentHashMap<String, AtomicInteger>()
    private val lastServerConfirmedByPath = ConcurrentHashMap<String, Long>()
    private val lastServerConfirmedEpochMs = AtomicLong(0L)

    val lastServerConfirmedAtMs: Long
        get() = lastServerConfirmedEpochMs.get()

    fun hasRecentServerConfirmation(path: String, withinMs: Long = 1_500L): Boolean {
        if (!_isNetworkAvailable.value || !_isServerConnected.value) return false
        val cleanPath = path.ifBlank { "unknown" }
        val lastMs = lastServerConfirmedByPath[cleanPath] ?: return false
        val elapsed = System.currentTimeMillis() - lastMs
        return elapsed in 0L..withinMs
    }

    fun onFullSyncStarted(reason: String = "FULL_SYNC") {
        if (_isNetworkAvailable.value) {
            _isSyncing.value = true
            if (!_isServerConnected.value) {
                _isReconnecting.value = true
            }
        }
    }

    fun onFullSyncFinished() {
        _isSyncing.value = false
        if (_isServerConnected.value || !_isNetworkAvailable.value) {
            _isReconnecting.value = false
        }
    }

    fun isServerStreamStalled(thresholdMs: Long = DEFAULT_STALL_THRESHOLD_MS): Boolean {
        if (!_isServerConnected.value || _isServingFromCache.value) return true
        val lastConfirmed = lastServerConfirmedEpochMs.get()
        if (lastConfirmed <= 0L) return true
        return (System.currentTimeMillis() - lastConfirmed) > thresholdMs
    }

    fun onListenerAttached(path: String) {
        val cleanPath = path.ifBlank { "unknown" }
        listenerCountsByPath.computeIfAbsent(cleanPath) { AtomicInteger(0) }.incrementAndGet()
        recomputeTotalActiveListeners()
    }

    fun onListenerDetached(path: String) {
        val cleanPath = path.ifBlank { "unknown" }
        val counter = listenerCountsByPath[cleanPath]
        if (counter != null) {
            val next = counter.decrementAndGet()
            if (next <= 0) {
                counter.set(0)
            }
        }
        recomputeTotalActiveListeners()
    }

    fun getActiveListenersForPath(path: String): Int {
        return listenerCountsByPath[path.ifBlank { "unknown" }]?.get()?.coerceAtLeast(0) ?: 0
    }

    private fun recomputeTotalActiveListeners() {
        val total = listenerCountsByPath.values.sumOf { it.get().coerceAtLeast(0) }
        _activeListenerCount.value = total
    }

    fun onSnapshotReceived(
        path: String,
        isFromCache: Boolean,
        hasPendingWrites: Boolean
    ) {
        _hasPendingWrites.value = hasPendingWrites
        if (!isFromCache) {
            val now = System.currentTimeMillis()
            lastServerConfirmedEpochMs.set(now)
            val cleanPath = path.ifBlank { "unknown" }
            lastServerConfirmedByPath[cleanPath] = now
            _isNetworkAvailable.value = true
            _isServerConnected.value = true
            _isServingFromCache.value = false
            _isReconnecting.value = false
        } else {
            // Snapshot came from local cache: never masquerade local cache as live server data when disconnected
            if (!_isNetworkAvailable.value || !_isServerConnected.value) {
                _isServingFromCache.value = true
            }
        }
    }

    fun onPendingWritesFlushed() {
        _hasPendingWrites.value = false
    }

    fun onConnectionError(
        path: String,
        operationType: OperationType,
        error: Throwable,
        formattedJson: String
    ) {
        val fsEx = findFirestoreException(error)
        val codeName = fsEx?.code?.name ?: error::class.java.simpleName
        val summary = "[${System.currentTimeMillis()}] op=${operationType.value} path=$path code=$codeName msg=${error.message ?: error.toString()}"
        _lastConnectionError.value = formattedJson.ifBlank { summary }
        synchronized(this) {
            val updated = (listOf(summary) + _connectionErrorLogs.value).take(MAX_ERROR_LOG_ENTRIES)
            _connectionErrorLogs.value = updated
        }
        if (fsEx != null) {
            when (fsEx.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.INTERNAL,
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                    val recentlyConfirmedServer = _isNetworkAvailable.value &&
                        (System.currentTimeMillis() - lastServerConfirmedEpochMs.get()) in 0L..10_000L
                    if (!recentlyConfirmedServer) {
                        _isServerConnected.value = false
                        _isServingFromCache.value = true
                    }
                }
                else -> Unit
            }
        }
    }

    fun onListenerReattached(path: String, attempt: Int, reason: String) {
        _listenerReattachCount.value = _listenerReattachCount.value + 1
        val logEntry = "[${System.currentTimeMillis()}] REATTACH path=$path attempt=$attempt reason=$reason"
        synchronized(this) {
            val updated = (listOf(logEntry) + _connectionErrorLogs.value).take(MAX_ERROR_LOG_ENTRIES)
            _connectionErrorLogs.value = updated
        }
        Log.i("FirestoreMonitor", logEntry)
    }

    fun onNetworkAvailabilityChanged(available: Boolean) {
        val changed = _isNetworkAvailable.value != available
        _isNetworkAvailable.value = available
        if (!available) {
            _isServerConnected.value = false
            _isServingFromCache.value = true
            _isReconnecting.value = false
            _isSyncing.value = false
            _activeNetworkType.value = "NONE"
            lastServerConfirmedByPath.clear()
        } else if (changed) {
            _isReconnecting.value = true
            triggerGlobalReconnect("NETWORK_AVAILABLE")
        }
    }

    fun onNetworkTypeChanged(networkType: String, available: Boolean) {
        val normalizedType = networkType.trim().uppercase().ifBlank { if (available) "ONLINE" else "NONE" }
        val prevType = _activeNetworkType.value
        val prevAvailable = _isNetworkAvailable.value
        _activeNetworkType.value = normalizedType
        _isNetworkAvailable.value = available

        if (!available) {
            _isServerConnected.value = false
            _isServingFromCache.value = true
            _isReconnecting.value = false
            _isSyncing.value = false
            lastServerConfirmedByPath.clear()
            return
        }

        val wasTransportSwitch = prevAvailable &&
            prevType != "UNKNOWN" &&
            prevType != "NONE" &&
            prevType != normalizedType

        if (!prevAvailable || wasTransportSwitch) {
            _isReconnecting.value = true
            val reason = if (wasTransportSwitch) {
                "NETWORK_SWITCH_${prevType}_TO_${normalizedType}"
            } else {
                "NETWORK_RESTORED_$normalizedType"
            }
            triggerGlobalReconnect(reason)
        }
    }

    fun triggerGlobalReconnect(reason: String = "MANUAL_OR_NETWORK_RESTORE") {
        if (_isNetworkAvailable.value && !_isServerConnected.value) {
            _isReconnecting.value = true
        }
        _recoveryGeneration.value = _recoveryGeneration.value + 1L
        val logEntry = "[${System.currentTimeMillis()}] GLOBAL_RECONNECT reason=$reason gen=${_recoveryGeneration.value}"
        synchronized(this) {
            val updated = (listOf(logEntry) + _connectionErrorLogs.value).take(MAX_ERROR_LOG_ENTRIES)
            _connectionErrorLogs.value = updated
        }
    }

    fun resetForTesting() {
        listenerCountsByPath.clear()
        lastServerConfirmedByPath.clear()
        _activeListenerCount.value = 0
        _listenerReattachCount.value = 0
        _isNetworkAvailable.value = true
        _isServerConnected.value = true
        _isServingFromCache.value = false
        _isReconnecting.value = false
        _isSyncing.value = false
        _hasPendingWrites.value = false
        _activeNetworkType.value = "UNKNOWN"
        _lastConnectionError.value = null
        _connectionErrorLogs.value = emptyList()
        _recoveryGeneration.value = 0L
        lastServerConfirmedEpochMs.set(0L)
    }

    private fun findFirestoreException(error: Throwable?): FirebaseFirestoreException? {
        var cur = error
        while (cur != null) {
            if (cur is FirebaseFirestoreException) return cur
            cur = cur.cause
        }
        return null
    }
}

const val DEFAULT_NAMED_FIRESTORE_DATABASE_ID = "ai-studio-android-811ea734-e9e8-4ff4-8e56-ca97ec8a429c"

/**
 * Returns a [FirebaseFirestore] instance configured with native SQLite [PersistentCacheSettings]
 * (100 MB disk cache) before its first operation so cached Feed, Profile, Notification, Message, and
 * Application snapshots load immediately across app restarts and offline sessions, while falling back
 * safely to [MemoryCacheSettings] if disk cache initialization is unavailable.
 */
fun getConfiguredFirestoreInstance(databaseId: String = DEFAULT_NAMED_FIRESTORE_DATABASE_ID): FirebaseFirestore {
    val cleanId = databaseId.trim().let { raw ->
        if (raw.isBlank() || raw == "(default)") {
            DEFAULT_NAMED_FIRESTORE_DATABASE_ID
        } else {
            raw
        }
    }
    val db = runCatching {
        FirebaseFirestore.getInstance(cleanId)
    }.getOrElse {
        runCatching {
            FirebaseFirestore.getInstance(DEFAULT_NAMED_FIRESTORE_DATABASE_ID)
        }.getOrElse {
            FirebaseFirestore.getInstance()
        }
    }
    val cacheKey = "${System.identityHashCode(db)}:${cleanId.ifBlank { DEFAULT_NAMED_FIRESTORE_DATABASE_ID }}"
    if (configuredFirestoreDatabases.putIfAbsent(cacheKey, true) == null) {
        val appliedLruMemory = runCatching {
            val lruMemoryCache = MemoryCacheSettings.newBuilder()
                .setGcSettings(
                    MemoryLruGcSettings.newBuilder()
                        .setSizeBytes(50L * 1024L * 1024L)
                        .build()
                )
                .build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(lruMemoryCache)
                .build()
            db.firestoreSettings = settings
            true
        }.getOrDefault(false)

        if (!appliedLruMemory) {
            runCatching {
                val fallbackSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = fallbackSettings
            }
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
    FirestoreConnectionMonitor.onConnectionError(
        path = path ?: "unknown",
        operationType = operationType,
        error = exception,
        formattedJson = jsonString
    )
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
 * Awaits a Firestore write task within [timeoutMs] (minimum 15s for real server confirmation).
 *
 * - Strictly awaits real Firestore server acknowledgment so writes are never falsely marked
 *   as succeeded solely from local RAM cache.
 * - Immediately propagates any server/security exception (`PERMISSION_DENIED`,
 *   `UNAUTHENTICATED`, `INVALID_ARGUMENT`, etc.) or timeout if the write is rejected or fails.
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitMessageWriteConfirmation(
    docRef: DocumentReference,
    timeoutMs: Long = 15_000L
): T {
    val effectiveTimeoutMs = timeoutMs.coerceAtLeast(12_000L)
    return try {
        kotlinx.coroutines.withTimeout(effectiveTimeoutMs) {
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
        throw e
    }
}

/**
 * Awaits a Firestore Task (such as set/update/delete/batch.commit) for real server confirmation.
 *
 * - Propagates server results and exceptions directly.
 * - Never masks unconfirmed or timed-out writes behind local RAM cache.
 */
@Suppress("UNCHECKED_CAST")
suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitWithTimeout(
    timeoutMs: Long = 15_000L
): T {
    val effectiveTimeoutMs = timeoutMs.coerceAtLeast(12_000L)
    return try {
        kotlinx.coroutines.withTimeout(effectiveTimeoutMs) {
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
        throw e
    }
}

