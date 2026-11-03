package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminRegistryEntry
import com.example.data.model.AppNotification
import com.example.data.model.AppReleaseInfo
import com.example.data.model.BookmarkItem
import kotlinx.coroutines.Dispatchers
import com.example.data.model.ChatGroup
import com.example.data.model.DirectMessage
import com.example.data.model.EngagementReceipt
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.FeedPostExtendedAttachment
import com.example.data.model.IntelligenceConfig
import com.example.data.model.KhedmotCategoryHelper
import com.example.data.model.KhedmotSubmission
import com.example.data.model.MadrasaProfile
import com.example.data.model.NotificationReadReceipt
import com.example.data.model.PostIntelligenceMetrics
import com.example.data.model.TelegramNotificationLog
import com.example.data.model.UserAffinityState
import com.example.data.model.UserProfile
import com.example.ui.util.AdminSecurityHelper
import com.example.ui.util.AppUpdateManagerHelper
import com.example.ui.util.KksIntelligenceEngine
import com.example.ui.util.ProfilePhotoHelper
import com.example.ui.util.TelegramNotificationHelper
import java.util.concurrent.ConcurrentHashMap
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class QawmiRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        getConfiguredFirestoreInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun <T> DocumentSnapshot.safeToObject(clazz: Class<T>): T? {
        if (!exists()) return null
        return try {
            toObject(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (_: Throwable) {
            null
        }
    }

    private fun <T> QuerySnapshot.safeToObjects(clazz: Class<T>): List<T> {
        return documents.mapNotNull { doc ->
            doc.safeToObject(clazz)
        }
    }

    private fun resolveEffectiveUserId(userId: String): String {
        val trimmed = userId.trim()
        if (trimmed.isNotBlank()) return trimmed
        return runCatching { auth.currentUser?.uid?.trim().orEmpty() }.getOrDefault("")
    }

    private val lastGrpcCycleEpochMs = java.util.concurrent.atomic.AtomicLong(0L)

    /**
     * Ensures the Firebase Firestore network connection is active without unnecessarily
     * re-initializing or reconnecting Firebase Auth, and without ever calling `db.disableNetwork()`
     * which would interrupt in-flight writes or active snapshot listeners.
     */
    suspend fun ensureLiveConnectionActive(forceTokenRefresh: Boolean = false, reason: String = "ENSURE_LIVE") =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            if (!FirestoreConnectionMonitor.isNetworkAvailable.value && reason != "NETWORK_RESTORED") {
                return@withContext
            }
            FirestoreConnectionMonitor.onNetworkAvailabilityChanged(true)

            val currentUser = runCatching { auth.currentUser }.getOrNull()
            if (currentUser != null && forceTokenRefresh) {
                runCatching {
                    withTimeoutOrNull(1_800L) {
                        currentUser.getIdToken(true).await()
                    }
                }
            }

            runCatching {
                withTimeoutOrNull(1_500L) {
                    db.enableNetwork().await()
                }
            }

            if (reason != "APP_STARTUP" && reason != "AUTH_TOKEN_REFRESHED") {
                FirestoreConnectionMonitor.triggerGlobalReconnect(reason)
            }

            // Flush any pending offline writes non-blockingly in a bounded window
            if (FirestoreConnectionMonitor.hasPendingWrites.value) {
                runCatching {
                    val flushed = withTimeoutOrNull(1_500L) {
                        db.waitForPendingWrites().await()
                        true
                    } ?: false
                    if (flushed) {
                        FirestoreConnectionMonitor.onPendingWritesFlushed()
                    }
                }
            }
        }

    private val activeQueryServerCallbacks =
        ConcurrentHashMap<String, ConcurrentHashMap<String, (QuerySnapshot) -> Unit>>()
    private val activeDocServerCallbacks =
        ConcurrentHashMap<String, ConcurrentHashMap<String, (DocumentSnapshot) -> Unit>>()
    private val confirmedServerMessageIds = ConcurrentHashMap.newKeySet<String>()
    private val confirmedServerPostIds = ConcurrentHashMap.newKeySet<String>()
    private val inFlightServerPostIds = ConcurrentHashMap.newKeySet<String>()

    private fun notifyQueryServerCallbacks(path: String, snap: QuerySnapshot) {
        activeQueryServerCallbacks[path]?.values?.forEach { callback ->
            runCatching { callback(snap) }
        }
    }

    private fun notifyDocServerCallbacks(path: String, snap: DocumentSnapshot) {
        activeDocServerCallbacks[path]?.values?.forEach { callback ->
            runCatching { callback(snap) }
        }
    }

    private suspend fun syncQueryFromServerIfNeeded(
        query: com.google.firebase.firestore.Query,
        path: String
    ) {
        if (!FirestoreConnectionMonitor.isNetworkAvailable.value) return
        if (FirestoreConnectionMonitor.hasRecentServerConfirmation(path, withinMs = 5_000L)) return
        if (FirestoreConnectionMonitor.getActiveListenersForPath(path) > 0 &&
            FirestoreConnectionMonitor.hasRecentServerConfirmation(path, withinMs = 20_000L)
        ) {
            return
        }

        val snap = withTimeoutOrNull(1_800L) {
            runCatching { query.get(Source.SERVER).await() }.getOrNull()
        }

        if (snap != null) {
            FirestoreConnectionMonitor.onSnapshotReceived(
                path = path,
                isFromCache = snap.metadata.isFromCache,
                hasPendingWrites = snap.metadata.hasPendingWrites()
            )
            notifyQueryServerCallbacks(path, snap)
        }
    }

    private suspend fun syncDocFromServerIfNeeded(
        docRef: DocumentReference,
        path: String
    ) {
        if (!FirestoreConnectionMonitor.isNetworkAvailable.value) return
        if (FirestoreConnectionMonitor.hasRecentServerConfirmation(path, withinMs = 5_000L)) return
        if (FirestoreConnectionMonitor.getActiveListenersForPath(path) > 0 &&
            FirestoreConnectionMonitor.hasRecentServerConfirmation(path, withinMs = 20_000L)
        ) {
            return
        }

        val snap = withTimeoutOrNull(1_800L) {
            runCatching { docRef.get(Source.SERVER).await() }.getOrNull()
        }

        if (snap != null) {
            FirestoreConnectionMonitor.onSnapshotReceived(
                path = path,
                isFromCache = snap.metadata.isFromCache,
                hasPendingWrites = snap.metadata.hasPendingWrites()
            )
            notifyDocServerCallbacks(path, snap)
        }
    }

    /**
     * Refreshes all required app datasets (Feed, Profile, Notifications, Sent & Received Applications,
     * Messages, Chat Groups, Bookmarks, Madrasas, Post Comments, Intelligence Config & Metrics, and App
     * Release info) in parallel on [Dispatchers.IO] within a strict < 2-second timeout budget.
     * Skips any collection whose active snapshot listener has already confirmed fresh server data
     * within the last 1.8s so Firebase read costs are never inflated by duplicate queries.
     */
    suspend fun refreshCriticalDataInParallel(userId: String) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (!FirestoreConnectionMonitor.isNetworkAvailable.value) return@withContext
        FirestoreConnectionMonitor.onFullSyncStarted("PARALLEL_FULL_SYNC")
        try {
            withTimeoutOrNull(1_950L) {
                coroutineScope {
                    val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()

                    // 1. Feed Posts
                    jobs += async {
                        syncQueryFromServerIfNeeded(db.collection("feed_posts"), "feed_posts")
                    }
                    // 2. Public Madrasas
                    jobs += async {
                        syncQueryFromServerIfNeeded(db.collection("madrasas"), "madrasas")
                    }
                    // 3. Public Post Comments
                    jobs += async {
                        syncQueryFromServerIfNeeded(db.collection("post_comments"), "post_comments")
                    }
                    // 4. Broadcast Notifications
                    jobs += async {
                        syncQueryFromServerIfNeeded(
                            db.collection("notifications").whereEqualTo("recipientId", "ALL_USERS"),
                            "notifications/broadcast"
                        )
                    }
                    // 5. KKS Intelligence Global Config & Metrics
                    jobs += async {
                        val configRef = db.collection("intelligence_config")
                            .document(IntelligenceConfig.GLOBAL_CONFIG_DOC_ID)
                        syncDocFromServerIfNeeded(configRef, configRef.path)
                    }
                    jobs += async {
                        syncQueryFromServerIfNeeded(
                            db.collection("post_intelligence_metrics"),
                            "post_intelligence_metrics"
                        )
                    }
                    // 6. Latest App Release
                    jobs += async {
                        syncDocFromServerIfNeeded(
                            db.collection("app_releases").document("latest"),
                            "app_releases/latest"
                        )
                    }

                    if (effectiveUid.isNotBlank()) {
                        // 7. Current User Profile
                        jobs += async {
                            syncDocFromServerIfNeeded(
                                db.collection("users").document(effectiveUid),
                                "users/$effectiveUid"
                            )
                        }
                        // 8. Direct Notifications & Read Receipts (also backs Ajker Smoron)
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("notifications").whereEqualTo("recipientId", effectiveUid),
                                "notifications/direct/$effectiveUid"
                            )
                        }
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("users").document(effectiveUid).collection("notification_reads"),
                                "users/$effectiveUid/notification_reads"
                            )
                        }
                        // 9. Sent & Received Khedmot Applications
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("khedmot_submissions").whereEqualTo("applicantId", effectiveUid),
                                "khedmot_submissions/sent/$effectiveUid"
                            )
                        }
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("khedmot_submissions").whereEqualTo("recipientId", effectiveUid),
                                "khedmot_submissions/received/$effectiveUid"
                            )
                        }
                        // 10. Direct & Group Messages + Chat Groups
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("messages").whereEqualTo("receiverId", effectiveUid),
                                "messages/received/$effectiveUid"
                            )
                        }
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("messages").whereEqualTo("senderId", effectiveUid),
                                "messages/sent/$effectiveUid"
                            )
                        }
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("messages").whereArrayContains("groupMemberIds", effectiveUid),
                                "messages/group/$effectiveUid"
                            )
                        }
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("chat_groups").whereArrayContains("memberIds", effectiveUid),
                                "chat_groups/$effectiveUid"
                            )
                        }
                        // 11. Bookmarks & User Affinity
                        jobs += async {
                            syncQueryFromServerIfNeeded(
                                db.collection("users").document(effectiveUid).collection("bookmarks"),
                                "users/$effectiveUid/bookmarks"
                            )
                        }
                        jobs += async {
                            val affinityRef = db.collection("users")
                                .document(effectiveUid)
                                .collection("intelligence_state")
                                .document(UserAffinityState.AFFINITY_DOC_ID)
                            syncDocFromServerIfNeeded(affinityRef, affinityRef.path)
                        }
                    }

                    jobs.forEach { it.await() }
                }
            }
        } finally {
            FirestoreConnectionMonitor.onFullSyncFinished()
        }
    }

    /**
     * Explicitly synchronizes the user's sent, received, and group messages from the Firestore server
     * (falling back to default/cache source if offline or timed out) and delivers snapshots directly
     * to active message query observers so historical messages are guaranteed to load when entering chat.
     */
    suspend fun syncUserMessagesFromServer(userId: String) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (effectiveUid.isBlank()) return@withContext

        runCatching { db.enableNetwork() }

        val queries = listOf(
            "messages/received/$effectiveUid" to db.collection("messages").whereEqualTo("receiverId", effectiveUid),
            "messages/sent/$effectiveUid" to db.collection("messages").whereEqualTo("senderId", effectiveUid),
            "messages/group/$effectiveUid" to db.collection("messages").whereArrayContains("groupMemberIds", effectiveUid)
        )

        coroutineScope {
            queries.map { (path, query) ->
                async {
                    val snap = if (FirestoreConnectionMonitor.isNetworkAvailable.value) {
                        withTimeoutOrNull(2_500L) {
                            runCatching { query.get(Source.SERVER).await() }.getOrNull()
                        } ?: withTimeoutOrNull(1_500L) {
                            runCatching { query.get().await() }.getOrNull()
                        }
                    } else {
                        withTimeoutOrNull(400L) {
                            runCatching { query.get(Source.CACHE).await() }.getOrNull()
                        }
                    }
                    if (snap != null) {
                        FirestoreConnectionMonitor.onSnapshotReceived(
                            path = path,
                            isFromCache = snap.metadata.isFromCache,
                            hasPendingWrites = snap.metadata.hasPendingWrites()
                        )
                        notifyQueryServerCallbacks(path, snap)
                    }
                }
            }.forEach { it.await() }
        }
    }

    /**
     * Self-healing Firestore [com.google.firebase.firestore.Query] observer:
     * 1. Emits local cached data (`Source.CACHE`) immediately on [Dispatchers.IO] (target < 100ms)
     *    so the UI renders stored data right away on cold start or offline mode without clearing existing state.
     * 2. Attaches a single authoritative `addSnapshotListener` per flow (strictly deduplicated via
     *    `activeRegistration`) using [MetadataChanges.INCLUDE] so server confirmation (`!isFromCache`)
     *    is always detected even when cached documents match server documents.
     * 3. Automatically re-subscribes `addSnapshotListener` with bounded exponential backoff whenever
     *    a transient gRPC idle reset, network drop, or token refresh causes `error != null`.
     * 4. Performs an automatic parallel server refresh (< 2s) when a foreground/network reconnect
     *    signal arrives or when the initial stream has not confirmed from server within `initialServerTimeoutMs`.
     */
    private fun <T> observeQueryWithFastInitialAndFallback(
        query: com.google.firebase.firestore.Query,
        path: String,
        includeMetadataChanges: Boolean = false,
        initialServerTimeoutMs: Long = 1_500L,
        mapper: (QuerySnapshot) -> List<T>
    ): Flow<List<T>> = channelFlow {
        val listenerId = java.util.UUID.randomUUID().toString()
        val hasEmittedAny = AtomicBoolean(false)
        val listenerHasEmitted = AtomicBoolean(false)
        val hasReceivedServerSnapshot = AtomicBoolean(false)
        val activeRegistration = AtomicReference<ListenerRegistration?>(null)
        val isCountedAttached = AtomicBoolean(false)
        val retryAttempt = AtomicInteger(0)
        val hasDeterministicError = AtomicBoolean(false)
        var recoveryJob: Job? = null

        fun tryEmitSafe(items: List<T>, fromListener: Boolean = false, isServerData: Boolean = false) {
            hasEmittedAny.set(true)
            if (fromListener) {
                listenerHasEmitted.set(true)
            }
            if (isServerData) {
                hasReceivedServerSnapshot.set(true)
            }
            trySend(items)
        }

        fun detachCurrentRegistration() {
            val old = activeRegistration.getAndSet(null)
            if (old != null) {
                runCatching { old.remove() }
            }
            if (isCountedAttached.compareAndSet(true, false)) {
                FirestoreConnectionMonitor.onListenerDetached(path)
            }
        }

        fun attachSnapshotListener() {
            if (!isActive) return
            // Guarantee at most 1 registration for this flow at any point in time
            detachCurrentRegistration()
            try {
                val reg = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        detachCurrentRegistration()
                        val currentUser = runCatching { auth.currentUser }.getOrNull()
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && currentUser == null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        recoveryJob?.cancel()
                        recoveryJob = launch(Dispatchers.IO) {
                            val attempt = retryAttempt.incrementAndGet()
                            // Serve local cache fallback if nothing emitted yet, or emit emptyList so UI never hangs on Loading
                            if (!hasEmittedAny.get()) {
                                val cachedFallback = withTimeoutOrNull(200L) {
                                    runCatching { query.get(Source.CACHE).await() }.getOrNull()
                                }
                                if (cachedFallback != null) {
                                    FirestoreConnectionMonitor.onSnapshotReceived(
                                        path = path,
                                        isFromCache = true,
                                        hasPendingWrites = cachedFallback.metadata.hasPendingWrites()
                                    )
                                    val mapped = runCatching { mapper(cachedFallback) }.getOrDefault(emptyList())
                                    if (!hasEmittedAny.get()) {
                                        tryEmitSafe(mapped, fromListener = false, isServerData = false)
                                    }
                                } else if (!hasEmittedAny.get()) {
                                    tryEmitSafe(emptyList(), fromListener = false, isServerData = false)
                                }
                            }

                            val isDeterministicQueryError =
                                error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                                    error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ||
                                    error.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT
                            if (isDeterministicQueryError && attempt >= 2) {
                                hasDeterministicError.set(true)
                                return@launch
                            }
                            val backoffMs = if (isDeterministicQueryError) {
                                4_000L
                            } else {
                                minOf(500L * (1L shl (attempt - 1).coerceAtMost(3)), 4_000L)
                            }
                            delay(backoffMs)
                            if (!isActive) return@launch

                            if (attempt == 1 && error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                                runCatching {
                                    withTimeoutOrNull(2_000L) {
                                        auth.currentUser?.getIdToken(true)?.await()
                                    }
                                }
                            }
                            runCatching { db.enableNetwork() }
                            FirestoreConnectionMonitor.onListenerReattached(path, attempt, error.code.name)
                            attachSnapshotListener()
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        retryAttempt.set(0)
                        hasDeterministicError.set(false)
                        val isFromCache = snapshot.metadata.isFromCache
                        val hasPending = snapshot.metadata.hasPendingWrites()
                        FirestoreConnectionMonitor.onSnapshotReceived(
                            path = path,
                            isFromCache = isFromCache,
                            hasPendingWrites = hasPending
                        )
                        if (!isFromCache) {
                            hasReceivedServerSnapshot.set(true)
                        }
                        val shouldEmitPayload = !listenerHasEmitted.get() ||
                            !isFromCache ||
                            includeMetadataChanges ||
                            snapshot.documentChanges.isNotEmpty()
                        if (shouldEmitPayload) {
                            val mapped = runCatching { mapper(snapshot) }.getOrDefault(emptyList())
                            tryEmitSafe(mapped, fromListener = true, isServerData = !isFromCache)
                        }
                    }
                }
                activeRegistration.set(reg)
                if (isCountedAttached.compareAndSet(false, true)) {
                    FirestoreConnectionMonitor.onListenerAttached(path)
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, path)
                if (!hasEmittedAny.get()) {
                    tryEmitSafe(emptyList(), fromListener = false, isServerData = false)
                }
            }
        }

        activeQueryServerCallbacks.getOrPut(path) { ConcurrentHashMap() }[listenerId] = { serverSnap ->
            if (isActive) {
                val mapped = runCatching { mapper(serverSnap) }.getOrNull()
                if (mapped != null) {
                    tryEmitSafe(mapped, fromListener = false, isServerData = !serverSnap.metadata.isFromCache)
                }
            }
        }

        // 1. Attach real-time snapshot listener immediately
        attachSnapshotListener()

        // 2. Instant zero-delay local cache seed on Dispatchers.IO (< 100ms) so UI shows cached data immediately
        launch(Dispatchers.IO) {
            if (listenerHasEmitted.get() || hasEmittedAny.get()) return@launch
            val cachedSnap = withTimeoutOrNull(200L) {
                runCatching { query.get(Source.CACHE).await() }.getOrNull()
            }
            if (cachedSnap != null &&
                (!cachedSnap.isEmpty || !FirestoreConnectionMonitor.isNetworkAvailable.value) &&
                !listenerHasEmitted.get()
            ) {
                FirestoreConnectionMonitor.onSnapshotReceived(
                    path = path,
                    isFromCache = true,
                    hasPendingWrites = cachedSnap.metadata.hasPendingWrites()
                )
                val mapped = runCatching { mapper(cachedSnap) }.getOrNull()
                if (mapped != null && !listenerHasEmitted.get() && !hasEmittedAny.get()) {
                    tryEmitSafe(mapped, fromListener = false, isServerData = false)
                }
            }
        }

        // 3. Re-attach when a foreground/network reconnect signal arrives and the listener was detached
        launch(Dispatchers.IO) {
            FirestoreConnectionMonitor.recoveryGeneration.drop(1).collect {
                if (!isActive) return@collect
                if (activeRegistration.get() == null && !hasDeterministicError.get()) {
                    recoveryJob?.cancel()
                    retryAttempt.set(0)
                    runCatching { db.enableNetwork() }
                    FirestoreConnectionMonitor.onListenerReattached(path, 1, "GLOBAL_RECONNECT")
                    attachSnapshotListener()
                } else if (!hasReceivedServerSnapshot.get() &&
                    FirestoreConnectionMonitor.isNetworkAvailable.value &&
                    !hasDeterministicError.get()
                ) {
                    delay(250L)
                    if (!isActive || hasReceivedServerSnapshot.get()) return@collect
                    val freshSnap = withTimeoutOrNull(1_800L) {
                        runCatching { query.get(Source.SERVER).await() }.getOrNull()
                    }
                    if (freshSnap != null && isActive) {
                        FirestoreConnectionMonitor.onSnapshotReceived(
                            path = path,
                            isFromCache = freshSnap.metadata.isFromCache,
                            hasPendingWrites = freshSnap.metadata.hasPendingWrites()
                        )
                        val mapped = runCatching { mapper(freshSnap) }.getOrNull()
                        if (mapped != null) {
                            tryEmitSafe(mapped, fromListener = false, isServerData = !freshSnap.metadata.isFromCache)
                        }
                    }
                }
            }
        }

        // 4. Bounded fast server sync if this flow's watch stream hasn't confirmed yet
        launch(Dispatchers.IO) {
            delay(initialServerTimeoutMs)
            if (hasReceivedServerSnapshot.get()) {
                return@launch
            }
            var serverErr: Throwable? = null
            val serverSnap = if (FirestoreConnectionMonitor.isNetworkAvailable.value) {
                withTimeoutOrNull(2_000L) {
                    runCatching { query.get(Source.SERVER).await() }
                        .onFailure { serverErr = it }
                        .getOrNull()
                } ?: withTimeoutOrNull(1_200L) {
                    runCatching { query.get().await() }
                        .onFailure { if (serverErr == null) serverErr = it }
                        .getOrNull()
                }
            } else {
                null
            }
            if (serverSnap != null && !hasReceivedServerSnapshot.get()) {
                FirestoreConnectionMonitor.onSnapshotReceived(
                    path = path,
                    isFromCache = serverSnap.metadata.isFromCache,
                    hasPendingWrites = serverSnap.metadata.hasPendingWrites()
                )
                val mapped = runCatching { mapper(serverSnap) }.getOrDefault(emptyList())
                if (!hasReceivedServerSnapshot.get()) {
                    tryEmitSafe(mapped, fromListener = false, isServerData = !serverSnap.metadata.isFromCache)
                }
            } else if (!hasEmittedAny.get()) {
                val fsErr = serverErr as? FirebaseFirestoreException
                if (fsErr != null) {
                    handleFirestoreError(fsErr, OperationType.LIST, path)
                }
                val fallbackCache = withTimeoutOrNull(250L) {
                    runCatching { query.get(Source.CACHE).await() }.getOrNull()
                }
                val fallbackList = if (fallbackCache != null) {
                    FirestoreConnectionMonitor.onSnapshotReceived(
                        path = path,
                        isFromCache = true,
                        hasPendingWrites = fallbackCache.metadata.hasPendingWrites()
                    )
                    runCatching { mapper(fallbackCache) }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
                if (!hasEmittedAny.get()) {
                    tryEmitSafe(fallbackList, fromListener = false, isServerData = false)
                }
            }
        }

        awaitClose {
            activeQueryServerCallbacks[path]?.remove(listenerId)
            recoveryJob?.cancel()
            detachCurrentRegistration()
        }
    }

    /**
     * Self-healing single-document observer (`addSnapshotListener` with automatic re-subscription
     * on stream/token errors, instant zero-delay local cache emission, and < 2s Server sync).
     */
    private fun <T> observeDocumentWithAutoRecovery(
        docRef: DocumentReference,
        path: String,
        includeMetadataChanges: Boolean = false,
        defaultValueOnError: T?,
        mapper: (DocumentSnapshot) -> T?
    ): Flow<T?> = channelFlow {
        val listenerId = java.util.UUID.randomUUID().toString()
        val hasEmittedAny = AtomicBoolean(false)
        val hasReceivedServerSnapshot = AtomicBoolean(false)
        val activeRegistration = AtomicReference<ListenerRegistration?>(null)
        val isCountedAttached = AtomicBoolean(false)
        val retryAttempt = AtomicInteger(0)
        val hasDeterministicError = AtomicBoolean(false)
        var recoveryJob: Job? = null

        fun detachCurrentRegistration() {
            val old = activeRegistration.getAndSet(null)
            if (old != null) {
                runCatching { old.remove() }
            }
            if (isCountedAttached.compareAndSet(true, false)) {
                FirestoreConnectionMonitor.onListenerDetached(path)
            }
        }

        fun attachDocListener() {
            if (!isActive) return
            detachCurrentRegistration()
            try {
                val reg = docRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.GET, path)
                        detachCurrentRegistration()
                        val currentUser = runCatching { auth.currentUser }.getOrNull()
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED && currentUser == null) {
                            if (!hasEmittedAny.get()) {
                                hasEmittedAny.set(true)
                                trySend(defaultValueOnError)
                            }
                            return@addSnapshotListener
                        }
                        recoveryJob?.cancel()
                        recoveryJob = launch(Dispatchers.IO) {
                            val attempt = retryAttempt.incrementAndGet()
                            if (!hasEmittedAny.get()) {
                                val cachedDoc = withTimeoutOrNull(200L) {
                                    runCatching { docRef.get(Source.CACHE).await() }.getOrNull()
                                }
                                if (cachedDoc != null && cachedDoc.exists()) {
                                    FirestoreConnectionMonitor.onSnapshotReceived(
                                        path = path,
                                        isFromCache = true,
                                        hasPendingWrites = cachedDoc.metadata.hasPendingWrites()
                                    )
                                    hasEmittedAny.set(true)
                                    trySend(runCatching { mapper(cachedDoc) }.getOrDefault(defaultValueOnError))
                                } else {
                                    hasEmittedAny.set(true)
                                    trySend(defaultValueOnError)
                                }
                            }
                            val isDeterministicDocError =
                                error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                                    error.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT
                            if (isDeterministicDocError && attempt >= 2) {
                                hasDeterministicError.set(true)
                                return@launch
                            }
                            val backoffMs = if (isDeterministicDocError) {
                                4_000L
                            } else {
                                minOf(500L * (1L shl (attempt - 1).coerceAtMost(3)), 4_000L)
                            }
                            delay(backoffMs)
                            if (!isActive) return@launch
                            if (attempt == 1 && error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                                runCatching {
                                    withTimeoutOrNull(2_000L) {
                                        auth.currentUser?.getIdToken(true)?.await()
                                    }
                                }
                            }
                            runCatching { db.enableNetwork() }
                            FirestoreConnectionMonitor.onListenerReattached(path, attempt, error.code.name)
                            attachDocListener()
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        retryAttempt.set(0)
                        hasDeterministicError.set(false)
                        val isFromCache = snapshot.metadata.isFromCache
                        val hasPending = snapshot.metadata.hasPendingWrites()
                        FirestoreConnectionMonitor.onSnapshotReceived(
                            path = path,
                            isFromCache = isFromCache,
                            hasPendingWrites = hasPending
                        )
                        if (!isFromCache) {
                            hasReceivedServerSnapshot.set(true)
                        }
                        hasEmittedAny.set(true)
                        val mapped = if (snapshot.exists()) {
                            runCatching { mapper(snapshot) }.getOrDefault(defaultValueOnError)
                        } else {
                            null
                        }
                        trySend(mapped)
                    }
                }
                activeRegistration.set(reg)
                if (isCountedAttached.compareAndSet(false, true)) {
                    FirestoreConnectionMonitor.onListenerAttached(path)
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, path)
                if (!hasEmittedAny.get()) {
                    hasEmittedAny.set(true)
                    trySend(defaultValueOnError)
                }
            }
        }

        activeDocServerCallbacks.getOrPut(path) { ConcurrentHashMap() }[listenerId] = { serverDoc ->
            if (isActive) {
                if (!serverDoc.metadata.isFromCache) {
                    hasReceivedServerSnapshot.set(true)
                }
                val mapped = if (serverDoc.exists()) {
                    runCatching { mapper(serverDoc) }.getOrDefault(defaultValueOnError)
                } else {
                    defaultValueOnError
                }
                hasEmittedAny.set(true)
                trySend(mapped)
            }
        }

        attachDocListener()

        // Fast zero-delay initial document fetch from local cache on Dispatchers.IO (< 100ms), then < 2s server fallback
        launch(Dispatchers.IO) {
            if (!hasEmittedAny.get()) {
                val cached = withTimeoutOrNull(180L) {
                    runCatching { docRef.get(Source.CACHE).await() }.getOrNull()
                }
                if (cached != null && cached.exists() && !hasEmittedAny.get()) {
                    FirestoreConnectionMonitor.onSnapshotReceived(
                        path = path,
                        isFromCache = true,
                        hasPendingWrites = cached.metadata.hasPendingWrites()
                    )
                    val mapped = runCatching { mapper(cached) }.getOrDefault(defaultValueOnError)
                    if (!hasEmittedAny.get()) {
                        hasEmittedAny.set(true)
                        trySend(mapped)
                    }
                }
            }
            if (!hasReceivedServerSnapshot.get()) {
                delay(1_200L)
                if (!hasReceivedServerSnapshot.get() &&
                    FirestoreConnectionMonitor.isNetworkAvailable.value &&
                    isActive
                ) {
                    val serverSnap = withTimeoutOrNull(1_500L) {
                        runCatching { docRef.get(Source.SERVER).await() }
                            .recoverCatching { docRef.get().await() }
                            .getOrNull()
                    }
                    if (serverSnap != null && !hasReceivedServerSnapshot.get()) {
                        FirestoreConnectionMonitor.onSnapshotReceived(
                            path = path,
                            isFromCache = serverSnap.metadata.isFromCache,
                            hasPendingWrites = serverSnap.metadata.hasPendingWrites()
                        )
                        if (!serverSnap.metadata.isFromCache) {
                            hasReceivedServerSnapshot.set(true)
                        }
                        val mapped = if (serverSnap.exists()) {
                            runCatching { mapper(serverSnap) }.getOrDefault(defaultValueOnError)
                        } else {
                            defaultValueOnError
                        }
                        hasEmittedAny.set(true)
                        trySend(mapped)
                    }
                }
            }
        }

        // Re-attach when a foreground/network reconnect signal arrives and the listener was detached
        launch(Dispatchers.IO) {
            FirestoreConnectionMonitor.recoveryGeneration.drop(1).collect {
                if (!isActive) return@collect
                if (activeRegistration.get() == null && !hasDeterministicError.get()) {
                    recoveryJob?.cancel()
                    retryAttempt.set(0)
                    runCatching { db.enableNetwork() }
                    FirestoreConnectionMonitor.onListenerReattached(path, 1, "GLOBAL_RECONNECT")
                    attachDocListener()
                } else if (!hasReceivedServerSnapshot.get() &&
                    FirestoreConnectionMonitor.isNetworkAvailable.value &&
                    !hasDeterministicError.get()
                ) {
                    delay(250L)
                    if (!isActive || hasReceivedServerSnapshot.get()) return@collect
                    val freshDoc = withTimeoutOrNull(1_500L) {
                        runCatching { docRef.get(Source.SERVER).await() }.getOrNull()
                    }
                    if (freshDoc != null && isActive) {
                        FirestoreConnectionMonitor.onSnapshotReceived(
                            path = path,
                            isFromCache = freshDoc.metadata.isFromCache,
                            hasPendingWrites = freshDoc.metadata.hasPendingWrites()
                        )
                        if (!freshDoc.metadata.isFromCache) {
                            hasReceivedServerSnapshot.set(true)
                        }
                        val mapped = if (freshDoc.exists()) {
                            runCatching { mapper(freshDoc) }.getOrDefault(defaultValueOnError)
                        } else {
                            null
                        }
                        hasEmittedAny.set(true)
                        trySend(mapped)
                    }
                }
            }
        }

        awaitClose {
            activeDocServerCallbacks[path]?.remove(listenerId)
            recoveryJob?.cancel()
            detachCurrentRegistration()
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("এটি সম্পন্ন করতে গুগল অ্যাকাউন্ট দিয়ে লগইন করা আবশ্যক।")
    }

    // --- 0. ADMIN IDENTITY, SERVER REGISTRY & PASSWORD RECOVERY ---

    fun isCurrentUserAuthorizedAdmin(): Boolean {
        val currentUser = runCatching { auth.currentUser }.getOrNull() ?: return false
        val email = AdminSecurityHelper.resolveEffectiveUserEmail(currentUser)
        val phone = AdminSecurityHelper.resolveEffectiveUserPhone(currentUser)
        return AdminSecurityHelper.isAuthorizedAdminIdentity(email, phone)
    }

    fun observeAdminRegistryEntry(userId: String): Flow<AdminRegistryEntry?> {
        if (userId.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        val path = "admins/$userId"
        return observeDocumentWithAutoRecovery(
            docRef = db.collection("admins").document(userId),
            path = path,
            includeMetadataChanges = false,
            defaultValueOnError = null
        ) { snapshot ->
            snapshot.safeToObject(AdminRegistryEntry::class.java)
        }
    }

    /**
     * Verifies the signed Firebase Auth ID token / Custom Claims and syncs the server-side
     * `/admins/{uid}` registry document when an authorized Admin logs in.
     * Protected on the backend by `isTokenVerifiedAdmin()` in `firestore.rules`.
     */
    suspend fun ensureVerifiedAdminRegistryDocument(): Result<Boolean> {
        val currentUser = runCatching { auth.currentUser }.getOrNull() ?: return Result.success(false)
        val isVerifiedAdmin = AdminSecurityHelper.verifyServerTokenAdminClaims(currentUser)
        if (!isVerifiedAdmin) {
            return Result.success(false)
        }
        val uid = currentUser.uid
        val resolvedEmail = AdminSecurityHelper.resolveEffectiveUserEmail(currentUser)
        val normalizedEmail = resolvedEmail
            .takeIf { AdminSecurityHelper.isAuthorizedAdminEmail(it) }
            ?: "fyslahm28@gmail.com"
        val docRef = db.collection("admins").document(uid)
        try {
            val payload = mapOf(
                "adminId" to uid,
                "email" to normalizedEmail,
                "adminPhone" to AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
                "role" to "SUPER_ADMIN",
                "verifiedByAuthToken" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitMessageWriteConfirmation(docRef, 3_500L)

            kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                runCatching {
                    enqueueAndDispatchTelegramNotification(
                        eventType = "ADMIN_LOGIN",
                        title = "অ্যাডমিন প্যানেল সেশন লগইন",
                        message = "অনুমোদিত অ্যাডমিন ($normalizedEmail) যাচাইকৃত সেশনে লগইন করেছেন।",
                        actorName = normalizedEmail,
                        referenceId = uid
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
        return Result.success(true)
    }

    suspend fun recordAdminLogoutActivity(adminEmailOrName: String): Result<Unit> {
        val currentUser = runCatching { auth.currentUser }.getOrNull() ?: return Result.success(Unit)
        if (!AdminSecurityHelper.verifyServerTokenAdminClaims(currentUser)) {
            return Result.success(Unit)
        }
        val resolvedEmail = AdminSecurityHelper.resolveEffectiveUserEmail(currentUser)
        val actor = adminEmailOrName.ifBlank { resolvedEmail.ifBlank { "নাজিমে আলা (অ্যাডমিন)" } }
        kotlinx.coroutines.withTimeoutOrNull(1_200L) {
            runCatching {
                enqueueAndDispatchTelegramNotification(
                    eventType = "ADMIN_LOGOUT",
                    title = "অ্যাডমিন সেশন লগআউট",
                    message = "অনুমোদিত অ্যাডমিন ($actor) অ্যাপ থেকে লগআউট করেছেন।",
                    actorName = actor,
                    referenceId = currentUser.uid
                )
            }
        }
        return Result.success(Unit)
    }

    /**
     * Sends a secure Firebase Authentication Password Reset Link to a verified, registered
     * Admin email address (`foysalahamad102@gmail.com`, `foysalahamad204@gmail.com`, `fyslahm28@gmail.com`).
     * Enforces strict identity verification, cooldown/rate-limiting, and security logging without
     * ever storing or transmitting passwords, OTPs, or reset links.
     */
    suspend fun sendAdminPasswordResetLink(email: String): Result<String> {
        val normalizedEmail = AdminSecurityHelper.normalizeEmail(email)
        if (!AdminSecurityHelper.isAuthorizedAdminEmail(normalizedEmail)) {
            if (auth.currentUser != null) {
                enqueueAndDispatchTelegramNotification(
                    eventType = "FAILED_AUTH_ATTEMPT",
                    title = "অননুমোদিত পাসওয়ার্ড রিসেট প্রচেষ্টা প্রতিহত",
                    message = "অননুমোদিত ইমেইলে ($normalizedEmail) অ্যাডমিন পাসওয়ার্ড রিসেট করার চেষ্টা প্রতিহত করা হয়েছে।",
                    actorName = auth.currentUser?.email ?: auth.currentUser?.uid.orEmpty(),
                    referenceId = "failed_reset_attempt"
                )
            }
            return Result.failure(
                IllegalArgumentException(
                    "অনুমোদনহীন ইমেইল। পাসওয়ার্ড রিসেট শুধুমাত্র যাচাইকৃত অ্যাডমিন সেশনের জন্য প্রযোজ্য।"
                )
            )
        }

        val rateLimitError = AdminSecurityHelper.checkPasswordResetRateLimit(normalizedEmail)
        if (rateLimitError != null) {
            return Result.failure(IllegalStateException(rateLimitError))
        }

        return try {
            auth.sendPasswordResetEmail(normalizedEmail).awaitWithTimeout(8_000L)
            AdminSecurityHelper.recordPasswordResetRequest(normalizedEmail)
            if (auth.currentUser != null && AdminSecurityHelper.verifyServerTokenAdminClaims(auth.currentUser)) {
                enqueueAndDispatchTelegramNotification(
                    eventType = "ADMIN_SECURITY",
                    title = "অ্যাডমিন পাসওয়ার্ড পুনরুদ্ধার অনুরোধ",
                    message = "অনুমোদিত অ্যাডমিন ইমেইল ($normalizedEmail)-এর জন্য Firebase Authentication পাসওয়ার্ড রিসেট প্রক্রিয়া শুরু হয়েছে (রিসেট লিংক গোপন রাখা হয়েছে)।",
                    actorName = normalizedEmail,
                    referenceId = "password_reset"
                )
            }
            Result.success(normalizedEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- 1. USER PROFILE, PHOTO & REAL-TIME PRESENCE (ONLINE / LAST SEEN) ---

    private fun DocumentSnapshot.toUserProfileOrNull(): UserProfile? {
        return try {
            val obj = safeToObject(UserProfile::class.java) ?: UserProfile(id = id, userId = id)
            val resolvedOnline = runCatching { getBoolean("isOnline") }.getOrNull() ?: obj.isOnline
            val resolvedVerified = runCatching { getBoolean("isVerified") }.getOrNull() ?: obj.isVerified
            val resolvedTypingTo = runCatching { getString("typingToUserId") }.getOrNull() ?: obj.typingToUserId
            val rawBlocked = (runCatching { get("blockedUserIds") }.getOrNull() as? List<*>)
                ?.mapNotNull { it as? String }
                ?: obj.blockedUserIds
            val rawThemes = (runCatching { get("chatThemes") }.getOrNull() as? Map<*, *>)
                ?.entries
                ?.mapNotNull { (k, v) ->
                    val key = k as? String
                    val value = v as? String
                    if (key != null && value != null) key to value else null
                }
                ?.toMap()
                ?: obj.chatThemes
            val resolvedContactEmail = runCatching { getString("contactEmail") }.getOrNull() ?: obj.contactEmail
            val resolvedPhonePrivacy = UserProfile.normalizePrivacyMode(
                runCatching { getString("phonePrivacy") }.getOrNull() ?: obj.phonePrivacy
            )
            val resolvedEmailPrivacy = UserProfile.normalizePrivacyMode(
                runCatching { getString("emailPrivacy") }.getOrNull() ?: obj.emailPrivacy
            )
            val candidate = obj.copy(
                id = id,
                contactEmail = resolvedContactEmail,
                phonePrivacy = resolvedPhonePrivacy,
                emailPrivacy = resolvedEmailPrivacy,
                isOnline = resolvedOnline,
                isVerified = resolvedVerified,
                typingToUserId = resolvedTypingTo,
                blockedUserIds = rawBlocked,
                chatThemes = rawThemes
            )
            val isFromCache = runCatching { metadata.isFromCache }.getOrDefault(false)
            val hasPending = runCatching { metadata.hasPendingWrites() }.getOrDefault(false)
            val effectiveOnline = when {
                !FirestoreConnectionMonitor.isNetworkAvailable.value -> false
                isFromCache && !FirestoreConnectionMonitor.isServerConnected.value -> false
                hasPending && !isFromCache -> candidate.isOnline
                else -> candidate.isEffectivelyOnline()
            }
            candidate.copy(isOnline = effectiveOnline)
        } catch (_: Throwable) {
            null
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        if (userId.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        val path = "users/$userId"
        return observeDocumentWithAutoRecovery(
            docRef = db.collection("users").document(userId),
            path = path,
            includeMetadataChanges = true,
            defaultValueOnError = null
        ) { snapshot ->
            snapshot.toUserProfileOrNull()
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            val snap = db.collection("users").document(userId).get().awaitWithTimeout(8_000L)
            if (snap != null && snap.exists()) {
                snap.toUserProfileOrNull()
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val resolvedName = profile.fullName.trim().ifBlank { "আবদুল্লাহ" }
        val resolvedPhoto = profile.photoUrl.trim()
        val resolvedEmail = profile.contactEmail.trim().take(150)
        val resolvedPhonePrivacy = UserProfile.normalizePrivacyMode(profile.phonePrivacy)
        val resolvedEmailPrivacy = UserProfile.normalizePrivacyMode(profile.emailPrivacy)
        return try {
            val existingSnap = docRef.getFastOrNull(3_500L)
            if (existingSnap == null || !existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to resolvedName,
                    "accountType" to profile.accountType.ifBlank { "khadim" },
                    "district" to profile.district.trim(),
                    "designation" to profile.designation.trim(),
                    "qualification" to profile.qualification.trim(),
                    "experience" to profile.experience.trim(),
                    "preferredDistricts" to profile.preferredDistricts.trim(),
                    "bio" to profile.bio.trim(),
                    "contactInfo" to profile.contactInfo.trim(),
                    "contactEmail" to resolvedEmail,
                    "phonePrivacy" to resolvedPhonePrivacy,
                    "emailPrivacy" to resolvedEmailPrivacy,
                    "madrasaName" to profile.madrasaName.trim(),
                    "photoUrl" to resolvedPhoto,
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "isVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitWithTimeout(8_000L)
            } else {
                val existingPhoto = existingSnap.getString("photoUrl").orEmpty()
                val finalPhoto = resolvedPhoto.ifBlank { existingPhoto }
                val updatePayload = mapOf(
                    "fullName" to resolvedName,
                    "accountType" to profile.accountType.ifBlank { "khadim" },
                    "district" to profile.district.trim(),
                    "designation" to profile.designation.trim(),
                    "qualification" to profile.qualification.trim(),
                    "experience" to profile.experience.trim(),
                    "preferredDistricts" to profile.preferredDistricts.trim(),
                    "bio" to profile.bio.trim(),
                    "contactInfo" to profile.contactInfo.trim(),
                    "contactEmail" to resolvedEmail,
                    "phonePrivacy" to resolvedPhonePrivacy,
                    "emailPrivacy" to resolvedEmailPrivacy,
                    "madrasaName" to profile.madrasaName.trim(),
                    "photoUrl" to finalPhoto,
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).awaitWithTimeout(8_000L)
            }

            // Propagate updated author photo (compacted/uploaded for feed) and name to user's authored feed posts
            try {
                val safeFeedPhoto = if (resolvedPhoto.isNotBlank()) {
                    ProfilePhotoHelper.uploadOrCompactAuthorPhotoForFeedPost(resolvedPhoto, uid)
                } else {
                    ""
                }
                val myPostsSnap = db.collection("feed_posts")
                    .whereEqualTo("authorId", uid)
                    .get()
                    .await()
                if (!myPostsSnap.isEmpty) {
                    val batch = db.batch()
                    for (postDoc in myPostsSnap.documents) {
                        val updates = mutableMapOf<String, Any>(
                            "authorName" to resolvedName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (safeFeedPhoto.isNotBlank()) {
                            updates["authorPhotoUrl"] = safeFeedPhoto
                        }
                        batch.update(postDoc.reference, updates)
                    }
                    batch.commit().awaitWithTimeout(8_000L)
                }
            } catch (_: Exception) {
                // Non-fatal if user has no posts yet
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePhoto(photoUrl: String, fallbackName: String = "আবদুল্লাহ"): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.getFastOrNull(3_500L)
            if (existingSnap == null || !existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to fallbackName.trim().ifBlank { "আবদুল্লাহ" },
                    "accountType" to "khadim",
                    "district" to "",
                    "designation" to "",
                    "qualification" to "",
                    "experience" to "",
                    "preferredDistricts" to "",
                    "bio" to "",
                    "contactInfo" to "",
                    "madrasaName" to "",
                    "photoUrl" to photoUrl,
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "isVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitWithTimeout(8_000L)
            } else {
                docRef.update(
                    mapOf(
                        "photoUrl" to photoUrl,
                        "isOnline" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitWithTimeout(8_000L)
            }

            try {
                val safeFeedPhoto = ProfilePhotoHelper.uploadOrCompactAuthorPhotoForFeedPost(photoUrl, uid)
                val myPostsSnap = db.collection("feed_posts")
                    .whereEqualTo("authorId", uid)
                    .get()
                    .await()
                if (!myPostsSnap.isEmpty) {
                    val batch = db.batch()
                    for (postDoc in myPostsSnap.documents) {
                        batch.update(
                            postDoc.reference,
                            mapOf(
                                "authorPhotoUrl" to safeFeedPhoto,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().awaitWithTimeout(8_000L)
                }
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPresence(isOnline: Boolean, defaultFullName: String = "সম্মানিত দ্বীনি ভাই"): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val docRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>(
            "isOnline" to isOnline,
            "lastSeenAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!isOnline) {
            updates["typingToUserId"] = ""
        }
        return try {
            val cachedSnap = docRef.getFromCacheOnlyOrNull(50L)
            if (cachedSnap != null && cachedSnap.exists()) {
                docRef.update(updates).awaitWithTimeout(5_000L)
                return Result.success(Unit)
            }
            try {
                docRef.update(updates).awaitWithTimeout(5_000L)
            } catch (updateEx: Exception) {
                val msg = updateEx.message.orEmpty()
                if (msg.contains("NOT_FOUND", ignoreCase = true) || msg.contains("No document to update", ignoreCase = true)) {
                    val payload = mapOf(
                        "userId" to uid,
                        "fullName" to defaultFullName.trim().ifBlank { "সম্মানিত দ্বীনি ভাই" }.take(145),
                        "accountType" to "khadim",
                        "district" to "",
                        "designation" to "",
                        "qualification" to "",
                        "experience" to "",
                        "preferredDistricts" to "",
                        "bio" to "",
                        "contactInfo" to "",
                        "madrasaName" to "",
                        "photoUrl" to "",
                        "isOnline" to isOnline,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "typingToUserId" to "",
                        "isVerified" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).awaitWithTimeout(5_000L)
                } else {
                    throw updateEx
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTypingStatus(
        targetUserId: String,
        isTyping: Boolean,
        defaultFullName: String = "সম্মানিত দ্বীনি ভাই"
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val resolvedTarget = if (isTyping && targetUserId.isNotBlank() && targetUserId != uid) targetUserId else ""
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.getCacheFirstOrNull(600L)
            if (snap != null && snap.exists()) {
                val existingTyping = snap.getString("typingToUserId").orEmpty()
                if (!isTyping && existingTyping.isEmpty()) {
                    return Result.success(Unit)
                }
                if (isTyping && existingTyping == resolvedTarget) {
                    return Result.success(Unit)
                }
                docRef.update(
                    mapOf(
                        "typingToUserId" to resolvedTarget,
                        "typingUpdatedAt" to FieldValue.serverTimestamp(),
                        "isOnline" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitWithTimeout(4_000L)
            } else if (isTyping) {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to defaultFullName.trim().ifBlank { "সম্মানিত দ্বীনি ভাই" },
                    "accountType" to "khadim",
                    "district" to "",
                    "designation" to "",
                    "qualification" to "",
                    "experience" to "",
                    "preferredDistricts" to "",
                    "bio" to "",
                    "contactInfo" to "",
                    "madrasaName" to "",
                    "photoUrl" to "",
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "typingToUserId" to resolvedTarget,
                    "typingUpdatedAt" to FieldValue.serverTimestamp(),
                    "isVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitWithTimeout(4_000L)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 2. FEED POSTS (নিয়োগ বিজ্ঞপ্তি ও চাকরিপ্রার্থীর আবেদন, Boosted সহ) ---

    private fun DocumentSnapshot.toFeedPostOrNull(): FeedPost? {
        if (!exists()) return null
        return try {
            val base = toObject(FeedPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: FeedPost()
            val rawReactions = (get("userReactions") as? Map<*, *>)
                ?.entries
                ?.mapNotNull { (k, v) ->
                    val key = k?.toString()?.trim().orEmpty()
                    val value = v?.toString()?.trim().orEmpty()
                    if (key.isNotBlank() && value.isNotBlank()) key to value else null
                }?.toMap() ?: base.userReactions
            val rawReactionNames = (get("userReactionNames") as? Map<*, *>)
                ?.entries
                ?.mapNotNull { (k, v) ->
                    val key = k?.toString()?.trim().orEmpty()
                    val value = v?.toString()?.trim().orEmpty()
                    if (key.isNotBlank() && value.isNotBlank()) key to value else null
                }?.toMap() ?: base.userReactionNames
            val rawCategories = (get("khedmotCategories") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotBlank() } }
                ?: base.khedmotCategories
            val rawImages = (get("imageUrls") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotBlank() } }
                ?: base.imageUrls
            val rawMediaTypes = (get("mediaTypes") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotBlank() } }
                ?: base.mediaTypes
            base.copy(
                id = id,
                authorId = getString("authorId") ?: base.authorId,
                authorName = getString("authorName") ?: base.authorName,
                authorPhotoUrl = getString("authorPhotoUrl") ?: base.authorPhotoUrl,
                postType = getString("postType") ?: base.postType,
                title = getString("title") ?: base.title,
                district = getString("district") ?: base.district,
                upazilaOrLocation = getString("upazilaOrLocation") ?: base.upazilaOrLocation,
                designation = getString("designation") ?: base.designation,
                khedmotCategories = rawCategories,
                customCategory = getString("customCategory") ?: base.customCategory,
                qualification = getString("qualification") ?: base.qualification,
                salaryOrExperience = getString("salaryOrExperience") ?: base.salaryOrExperience,
                deadlineOrPreferredDistricts = getString("deadlineOrPreferredDistricts") ?: base.deadlineOrPreferredDistricts,
                description = getString("description") ?: base.description,
                contactNumber = getString("contactNumber") ?: base.contactNumber,
                madrasaId = getString("madrasaId") ?: base.madrasaId,
                isVerifiedMadrasa = getBoolean("isVerifiedMadrasa") ?: base.isVerifiedMadrasa,
                isBoosted = getBoolean("isBoosted") ?: base.isBoosted,
                imageUrls = rawImages,
                mediaTypes = rawMediaTypes,
                attachmentDocId = getString("attachmentDocId") ?: base.attachmentDocId,
                userReactions = rawReactions,
                userReactionNames = rawReactionNames,
                commentCount = getLong("commentCount")?.toInt() ?: base.commentCount,
                shareCount = getLong("shareCount")?.toInt() ?: base.shareCount,
                sharedPostId = getString("sharedPostId") ?: base.sharedPostId,
                sharedAuthorId = getString("sharedAuthorId") ?: base.sharedAuthorId,
                sharedAuthorName = getString("sharedAuthorName") ?: base.sharedAuthorName,
                sharedTitle = getString("sharedTitle") ?: base.sharedTitle,
                sharedDescription = getString("sharedDescription") ?: base.sharedDescription,
                sharedPostType = getString("sharedPostType") ?: base.sharedPostType,
                isEdited = getBoolean("isEdited") ?: base.isEdited,
                visibility = getString("visibility") ?: base.visibility.ifBlank { "PUBLIC" },
                approvalStatus = getString("approvalStatus") ?: base.approvalStatus.ifBlank { "APPROVED" },
                createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: base.createdAt,
                updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: base.updatedAt
            )
        } catch (_: Exception) {
            safeToObject(FeedPost::class.java)
        }
    }

    private fun DocumentSnapshot.toFeedCommentOrNull(): FeedComment? {
        if (!exists()) return null
        return try {
            val base = toObject(FeedComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: FeedComment()
            base.copy(
                id = id,
                postId = getString("postId") ?: base.postId,
                authorId = getString("authorId") ?: base.authorId,
                authorName = getString("authorName") ?: base.authorName,
                authorPhotoUrl = getString("authorPhotoUrl") ?: base.authorPhotoUrl,
                text = getString("text") ?: base.text,
                parentCommentId = getString("parentCommentId") ?: base.parentCommentId,
                replyToAuthorName = getString("replyToAuthorName") ?: base.replyToAuthorName,
                isEdited = getBoolean("isEdited") ?: base.isEdited,
                visibility = getString("visibility") ?: base.visibility.ifBlank { "PUBLIC" },
                createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: base.createdAt,
                updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: base.updatedAt
            )
        } catch (_: Exception) {
            safeToObject(FeedComment::class.java)
        }
    }

    private fun DocumentSnapshot.toKhedmotSubmissionOrNull(): KhedmotSubmission? {
        if (!exists()) return null
        return try {
            val base = toObject(KhedmotSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: KhedmotSubmission()
            base.copy(
                id = id,
                postId = getString("postId") ?: base.postId,
                postTitle = getString("postTitle") ?: base.postTitle,
                postDesignation = getString("postDesignation") ?: base.postDesignation,
                applicantId = getString("applicantId") ?: base.applicantId,
                applicantName = getString("applicantName") ?: base.applicantName,
                applicantQualification = getString("applicantQualification") ?: base.applicantQualification,
                applicantExperience = getString("applicantExperience") ?: base.applicantExperience,
                applicantPhone = getString("applicantPhone") ?: base.applicantPhone,
                coverNote = getString("coverNote") ?: base.coverNote,
                recipientId = getString("recipientId") ?: base.recipientId,
                status = getString("status") ?: base.status.ifBlank { "PENDING" },
                createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: base.createdAt,
                updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: base.updatedAt
            )
        } catch (_: Exception) {
            safeToObject(KhedmotSubmission::class.java)
        }
    }

    fun observePublicFeedPosts(): Flow<List<FeedPost>> {
        val path = "feed_posts"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(path),
            path = path,
            includeMetadataChanges = true,
            initialServerTimeoutMs = 1_500L
        ) { snapshot ->
            val currentUid = resolveEffectiveUserId("")
            snapshot.documents
                .mapNotNull { doc ->
                    val docPending = runCatching { doc.metadata.hasPendingWrites() }.getOrDefault(false)
                    val docFromCache = runCatching { doc.metadata.isFromCache }.getOrDefault(false)
                    if (!docPending && doc.id.isNotBlank()) {
                        confirmedServerPostIds.add(doc.id)
                    }
                    // Exclude uncommitted local-only writes that are neither actively in-flight nor confirmed on server
                    if (docFromCache && docPending && doc.id !in confirmedServerPostIds && doc.id !in inFlightServerPostIds) {
                        return@mapNotNull null
                    }
                    doc.toFeedPostOrNull()
                }
                .filter { post ->
                    val notRejected = !post.approvalStatus.equals("REJECTED", ignoreCase = true)
                    val isPublic = post.visibility.isBlank() ||
                        post.visibility.equals("PUBLIC", ignoreCase = true)
                    (notRejected && isPublic) ||
                        (currentUid.isNotBlank() && post.authorId == currentUid)
                }
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<FeedPost> { it.isBoosted }
                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                )
        }
    }

    fun generateFeedPostDocumentId(): String {
        return db.collection("feed_posts").document().id
    }

    suspend fun createFeedPost(
        authorName: String,
        postType: String,
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String,
        madrasaId: String = "",
        isVerifiedMadrasa: Boolean = false,
        authorPhotoUrl: String = "",
        khedmotCategories: List<String> = emptyList(),
        customCategory: String = "",
        imageUrls: List<String> = emptyList(),
        sharedPostId: String = "",
        sharedAuthorId: String = "",
        sharedAuthorName: String = "",
        sharedTitle: String = "",
        sharedDescription: String = "",
        sharedPostType: String = "",
        preallocatedPostId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("feed_posts")
        val docRef = if (preallocatedPostId.isNotBlank()) {
            collectionRef.document(preallocatedPostId.trim())
        } else {
            collectionRef.document()
        }
        inFlightServerPostIds.add(docRef.id)
        return try {
            val trimmedDesc = description.trim()
            if (trimmedDesc.toByteArray(Charsets.UTF_8).size > 140_000) {
                return Result.failure(IllegalArgumentException(ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA))
            }
            if (imageUrls.size > ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE) {
                return Result.failure(
                    IllegalArgumentException("একটি পোস্টে সর্বোচ্চ ৫টি ছবি বা ভিডিও যুক্ত করা যাবে।")
                )
            }

            val cleanedCategories = khedmotCategories.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(20)
            val cleanedCustomCategory = customCategory.trim().take(150)
            val rawMediaItems = imageUrls.map { it.trim() }.filter { it.isNotBlank() }.take(5)

            // 1. Prepare media items & compact metadata
            val mediaUploadRes = ProfilePhotoHelper.uploadFeedMediaToFirebaseStorage(
                mediaItems = rawMediaItems,
                authorId = uid,
                postId = docRef.id
            )
            if (mediaUploadRes.isFailure) {
                return Result.failure(
                    mediaUploadRes.exceptionOrNull()
                        ?: IllegalArgumentException(ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA)
                )
            }
            val uploadedMedia = mediaUploadRes.getOrThrow()

            // 2. Prepare attachment document IDs and validate attachment sizes BEFORE writing
            var primaryAttachmentDocId = if (trimmedDesc.length > 2000) "${docRef.id}_ext_text" else ""
            val metadataMaps = mutableListOf<Map<String, String>>()
            val deferredAttachmentWrites = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()

            if (trimmedDesc.length > 2000) {
                val extDocId = "${docRef.id}_ext_text"
                val extRef = db.collection("feed_post_attachments").document(extDocId)
                val extPayload = mapOf(
                    "postId" to docRef.id,
                    "authorId" to uid,
                    "attachmentType" to "EXTENDED_CONTENT",
                    "mimeType" to "text/plain",
                    "fileName" to "extended_description.txt",
                    "downloadUrl" to "",
                    "extendedText" to trimmedDesc.take(140_000),
                    "mediaDataUrl" to "",
                    "sizeBytes" to trimmedDesc.toByteArray(Charsets.UTF_8).size,
                    "visibility" to "PUBLIC",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                deferredAttachmentWrites.add(extRef to extPayload)
            }

            uploadedMedia.forEachIndexed { idx, mediaItem ->
                var itemAttachmentDocId = ""
                if (mediaItem.separateAttachmentDataUrl.isNotBlank()) {
                    val mediaDocId = "${docRef.id}_media_$idx"
                    val mediaRef = db.collection("feed_post_attachments").document(mediaDocId)
                    val mediaPayload = mapOf(
                        "postId" to docRef.id,
                        "authorId" to uid,
                        "attachmentType" to mediaItem.mediaType,
                        "mimeType" to mediaItem.mimeType,
                        "fileName" to mediaItem.fileName,
                        "downloadUrl" to mediaItem.url,
                        "extendedText" to "",
                        "mediaDataUrl" to mediaItem.separateAttachmentDataUrl,
                        "sizeBytes" to mediaItem.sizeBytes,
                        "visibility" to "PUBLIC",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    val attachmentValidation = ProfilePhotoHelper.validateAttachmentDocumentSizeBeforeWrite(
                        documentPath = mediaRef.path,
                        data = mediaPayload
                    )
                    if (attachmentValidation.isFailure) {
                        return Result.failure(
                            attachmentValidation.exceptionOrNull()
                                ?: IllegalArgumentException(ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA)
                        )
                    }
                    ProfilePhotoHelper.cacheAttachmentDataUrl(mediaDocId, mediaItem.separateAttachmentDataUrl)
                    deferredAttachmentWrites.add(mediaRef to mediaPayload)
                    itemAttachmentDocId = mediaDocId
                    if (primaryAttachmentDocId.isBlank()) {
                        primaryAttachmentDocId = mediaDocId
                    }
                }
                metadataMaps.add(mediaItem.toMetadataMap(itemAttachmentDocId))
            }

            // 3. Ensure authorPhotoUrl on feed_posts is compact (<= 12 KB) without network stalls
            val safeAuthorPhotoUrl = ProfilePhotoHelper.uploadOrCompactAuthorPhotoForFeedPost(
                authorPhotoUrl = authorPhotoUrl,
                authorId = uid
            )

            // Never auto-claim user general or seeker posts as verified madrasa content
            val safeVerified = if (postType == "JOB_CIRCULAR") isVerifiedMadrasa else false
            val resolvedDesignation = KhedmotCategoryHelper.buildCombinedDesignation(
                manualDesignation = designation,
                selectedCategories = cleanedCategories,
                customCategory = cleanedCustomCategory
            ).ifBlank {
                if (postType == "GENERAL_POST") "দ্বীনি আলোচনা ও খেদমত" else designation.trim().ifBlank { "দ্বীনি খেদমত" }
            }
            val resolvedDistrict = district.trim().ifBlank {
                if (postType == "GENERAL_POST") "" else "সকল জেলা"
            }
            val resolvedQualification = qualification.trim().ifBlank {
                if (postType == "GENERAL_POST") "" else "আলোচনা সাপেক্ষ"
            }

            val payload = mutableMapOf<String, Any>(
                "authorId" to uid,
                "authorName" to authorName.trim().ifBlank { title.trim() }.ifBlank { "সম্মানিত দ্বীনি ভাই" }.take(200),
                "authorPhotoUrl" to safeAuthorPhotoUrl,
                "postType" to postType,
                "title" to title.trim().ifBlank { "দ্বীনি খেদমত ও আবেদন" }.take(200),
                "district" to resolvedDistrict.take(100),
                "upazilaOrLocation" to upazilaOrLocation.trim().take(200),
                "designation" to resolvedDesignation.take(150),
                "qualification" to resolvedQualification.take(200),
                "salaryOrExperience" to salaryOrExperience.trim().take(150),
                "deadlineOrPreferredDistricts" to deadlineOrPreferredDistricts.trim().take(250),
                "description" to trimmedDesc.take(2000),
                "contactNumber" to contactNumber.trim().take(100),
                "madrasaId" to madrasaId.take(128),
                "isVerifiedMadrasa" to safeVerified,
                "isBoosted" to false,
                "khedmotCategories" to cleanedCategories,
                "customCategory" to cleanedCustomCategory,
                "imageUrls" to uploadedMedia.map { it.url },
                "mediaTypes" to uploadedMedia.map { it.mediaType },
                "mediaMetadata" to metadataMaps,
                "attachmentDocId" to primaryAttachmentDocId,
                "userReactions" to emptyMap<String, String>(),
                "userReactionNames" to emptyMap<String, String>(),
                "commentCount" to 0,
                "shareCount" to 0,
                "sharedPostId" to sharedPostId.trim().take(128),
                "sharedAuthorId" to sharedAuthorId.trim().take(128),
                "sharedAuthorName" to sharedAuthorName.trim().take(200),
                "sharedTitle" to sharedTitle.trim().take(200),
                "sharedDescription" to sharedDescription.trim().take(2000),
                "sharedPostType" to sharedPostType.trim().take(50),
                "isEdited" to false,
                "visibility" to "PUBLIC",
                "approvalStatus" to "APPROVED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // 4. Pre-write document size validation (strictly before docRef.set)
            val sizeValidation = ProfilePhotoHelper.validateFirestoreDocumentSizeBeforeWrite(
                documentPath = docRef.path,
                data = payload
            )
            if (sizeValidation.isFailure) {
                return Result.failure(
                    sizeValidation.exceptionOrNull()
                        ?: IllegalArgumentException(ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA)
                )
            }

            // 5. Idempotency check on retry: if a preallocatedPostId was already confirmed on the server
            // by a previous attempt that timed out on the client, do not overwrite createdAt.
            if (preallocatedPostId.isNotBlank()) {
                if (docRef.id in confirmedServerPostIds) {
                    return Result.success(docRef.id)
                }
                val cachedExisting = docRef.getFromCacheOnlyOrNull(60L)
                if (cachedExisting != null &&
                    cachedExisting.exists() &&
                    !cachedExisting.metadata.hasPendingWrites() &&
                    cachedExisting.getString("authorId") == uid
                ) {
                    confirmedServerPostIds.add(docRef.id)
                    FirestoreConnectionMonitor.onSnapshotReceived(
                        path = "feed_posts",
                        isFromCache = false,
                        hasPendingWrites = false
                    )
                    return Result.success(docRef.id)
                }
            }

            // 6. Ensure network is active and commit the main /feed_posts/{postId} document, any offloaded attachments,
            // and seeker submission to the Firestore server so all other users' phones receive it in real time.
            runCatching { db.enableNetwork() }
            val allWriteTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()
            val mainPostTask = docRef.set(payload)
            mainPostTask.addOnCompleteListener { completedTask ->
                if (completedTask.isSuccessful) {
                    confirmedServerPostIds.add(docRef.id)
                    FirestoreConnectionMonitor.onSnapshotReceived(
                        path = "feed_posts",
                        isFromCache = false,
                        hasPendingWrites = false
                    )
                } else {
                    completedTask.exception?.let { err ->
                        handleFirestoreError(err, OperationType.CREATE, docRef.path)
                    }
                }
            }
            allWriteTasks += mainPostTask

            for ((attachRef, attachPayload) in deferredAttachmentWrites) {
                allWriteTasks += attachRef.set(attachPayload)
            }

            // 7. If this post is a SEEKER_APPLICATION, also register it in /khedmot_submissions
            // so Admin Panel and authorized users can track, approve, and manage it from Firebase.
            var submissionWriteTask: com.google.android.gms.tasks.Task<*>? = null
            if (postType == "SEEKER_APPLICATION") {
                runCatching {
                    val submissionRef = db.collection("khedmot_submissions").document("app_${docRef.id}")
                    val submissionPayload = mapOf(
                        "postId" to docRef.id,
                        "postTitle" to title.trim().ifBlank { "দ্বীনি খেদমত আবেদন" }.take(200),
                        "postDesignation" to resolvedDesignation.take(150),
                        "applicantId" to uid,
                        "applicantName" to authorName.trim().ifBlank { title.trim() }.ifBlank { "দ্বীনি ভাই" }.take(150),
                        "applicantQualification" to resolvedQualification.take(200),
                        "applicantExperience" to salaryOrExperience.trim().ifBlank { "আলোচনা সাপেক্ষ" }.take(150),
                        "applicantPhone" to contactNumber.trim().ifBlank { "প্রোফাইলে উল্লেখিত" }.take(100),
                        "coverNote" to trimmedDesc.ifBlank { "খেদমতের জন্য সরাসরি প্রকাশিত আবেদন।" }.take(1000),
                        "recipientId" to madrasaId.trim().ifBlank { uid }.take(128),
                        "status" to "PENDING",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    submissionWriteTask = submissionRef.set(submissionPayload)
                }
            }

            val writeTimeoutMs = if (FirestoreConnectionMonitor.isNetworkAvailable.value) 12_000L else 3_500L
            try {
                kotlinx.coroutines.withTimeout(writeTimeoutMs) {
                    com.google.android.gms.tasks.Tasks.whenAll(allWriteTasks).await()
                    runCatching { submissionWriteTask?.await() }
                }
                confirmedServerPostIds.add(docRef.id)
            } catch (timeoutEx: kotlinx.coroutines.TimeoutCancellationException) {
                val primaryError = allWriteTasks.firstNotNullOfOrNull { it.exception }
                if (primaryError != null) {
                    handleFirestoreError(primaryError, OperationType.CREATE, docRef.path)
                    return Result.failure(translateFirestoreExceptionToBangla(primaryError))
                }
                val serverConfirmed = runCatching {
                    val snap = withTimeoutOrNull(1_500L) { docRef.get(Source.SERVER).await() }
                    snap != null && snap.exists() && !snap.metadata.hasPendingWrites() && snap.getString("authorId") == uid
                }.getOrDefault(false)
                if (!serverConfirmed) {
                    runCatching { db.enableNetwork() }
                    handleFirestoreError(timeoutEx, OperationType.CREATE, docRef.path)
                    return Result.failure(
                        IllegalStateException("সার্ভারে পোস্ট সংরক্ষণে নির্ধারিত সময়ের বেশি লাগছে। অনুগ্রহ করে ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।")
                    )
                }
                confirmedServerPostIds.add(docRef.id)
            }

            // 8. Deliver community notification and Telegram Admin log with fast bounded timeout
            val isCircular = postType == "JOB_CIRCULAR"
            val isGeneral = postType == "GENERAL_POST"
            val notifTitle = when {
                isCircular -> "নতুন খেদমতের নিয়োগ বিজ্ঞপ্তি"
                isGeneral -> "নতুন দ্বীনি পোস্ট ও আলোচনা"
                else -> "নতুন খেদমতপ্রার্থীর আবেদন"
            }
            val notifMsg = when {
                isCircular -> "${title.trim()} (${resolvedDistrict})-এ '${resolvedDesignation}' পদে নিয়োগ বিজ্ঞপ্তি প্রকাশিত হয়েছে।"
                isGeneral -> "${authorName.trim().ifBlank { title.trim() }} ফিডে একটি নতুন পোস্ট শেয়ার করেছেন: ${title.trim()}"
                else -> "${title.trim()} (${resolvedQualification}) '${resolvedDesignation}' খেদমতের জন্য আবেদন প্রকাশ করেছেন।"
            }
            val resolvedSender = authorName.trim().ifBlank { title.trim() }.ifBlank { "সম্মানিত দ্বীনি ভাই" }
            kotlinx.coroutines.withTimeoutOrNull(900L) {
                coroutineScope {
                    launch {
                        runCatching {
                            createNotification(
                                recipientId = "ALL_USERS",
                                senderName = resolvedSender,
                                title = notifTitle,
                                message = notifMsg.take(500),
                                type = if (isCircular) "NEW_CIRCULAR" else "NEW_APPLICATION",
                                referenceId = docRef.id
                            )
                        }
                    }
                    launch {
                        runCatching {
                            enqueueAndDispatchTelegramNotification(
                                eventType = if (isCircular) "NEW_CIRCULAR" else "NEW_APPLICATION",
                                title = notifTitle,
                                message = "$notifMsg (যোগাযোগ: ${contactNumber.trim().ifBlank { "অপ্রদত্ত" }})",
                                actorName = resolvedSender,
                                referenceId = docRef.id
                            )
                        }
                    }
                }
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (preallocatedPostId.isNotBlank()) {
                val alreadyOnServer = runCatching {
                    val snap = withTimeoutOrNull(2_000L) { docRef.get(Source.SERVER).await() }
                    snap != null && snap.exists() && !snap.metadata.hasPendingWrites() && snap.getString("authorId") == uid
                }.getOrDefault(false)
                if (alreadyOnServer) {
                    confirmedServerPostIds.add(docRef.id)
                    return Result.success(docRef.id)
                }
            }
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        } finally {
            inFlightServerPostIds.remove(docRef.id)
        }
    }

    suspend fun updateOwnFeedPost(
        post: FeedPost,
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String,
        khedmotCategories: List<String> = post.khedmotCategories,
        customCategory: String = post.customCategory,
        imageUrls: List<String> = post.imageUrls
    ): Result<Unit> {
        val uid = requireUserId()
        if (post.authorId != uid) {
            return Result.failure(IllegalAccessException("শুধুমাত্র নিজের প্রকাশিত পোস্ট সম্পাদনা করা যাবে।"))
        }
        val docRef = db.collection("feed_posts").document(post.id)
        return try {
            val trimmedDesc = description.trim()
            if (trimmedDesc.toByteArray(Charsets.UTF_8).size > 140_000) {
                return Result.failure(IllegalArgumentException(ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA))
            }
            if (imageUrls.size > ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE) {
                return Result.failure(
                    IllegalArgumentException("একটি পোস্টে সর্বোচ্চ ৫টি ছবি বা ভিডিও যুক্ত করা যাবে।")
                )
            }

            val cleanedCategories = khedmotCategories.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(20)
            val cleanedCustom = customCategory.trim().take(150)
            val resolvedDesignation = KhedmotCategoryHelper.buildCombinedDesignation(
                manualDesignation = designation,
                selectedCategories = cleanedCategories,
                customCategory = cleanedCustom
            ).ifBlank {
                if (post.postType == "GENERAL_POST") "দ্বীনি আলোচনা ও খেদমত" else designation.trim()
            }

            val rawMediaItems = imageUrls.map { it.trim() }.filter { it.isNotBlank() }.take(5)
            val mediaUploadRes = ProfilePhotoHelper.uploadFeedMediaToFirebaseStorage(
                mediaItems = rawMediaItems,
                authorId = uid,
                postId = post.id
            )
            if (mediaUploadRes.isFailure) {
                return Result.failure(
                    mediaUploadRes.exceptionOrNull()
                        ?: IllegalArgumentException(ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA)
                )
            }
            val uploadedMedia = mediaUploadRes.getOrThrow()

            val updateTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()
            var primaryAttachmentDocId = post.attachmentDocId
            val metadataMaps = mutableListOf<Map<String, String>>()

            if (trimmedDesc.length > 2000) {
                val extDocId = "${post.id}_ext_text"
                val extRef = db.collection("feed_post_attachments").document(extDocId)
                val extPayload = mapOf(
                    "postId" to post.id,
                    "authorId" to uid,
                    "attachmentType" to "EXTENDED_CONTENT",
                    "mimeType" to "text/plain",
                    "fileName" to "extended_description.txt",
                    "downloadUrl" to "",
                    "extendedText" to trimmedDesc.take(140_000),
                    "mediaDataUrl" to "",
                    "sizeBytes" to trimmedDesc.toByteArray(Charsets.UTF_8).size,
                    "visibility" to "PUBLIC",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                updateTasks += extRef.set(extPayload)
                primaryAttachmentDocId = extDocId
            }

            uploadedMedia.forEachIndexed { idx, mediaItem ->
                var itemAttachmentDocId = ""
                if (mediaItem.separateAttachmentDataUrl.isNotBlank()) {
                    val mediaDocId = "${post.id}_media_$idx"
                    val mediaRef = db.collection("feed_post_attachments").document(mediaDocId)
                    val mediaPayload = mapOf(
                        "postId" to post.id,
                        "authorId" to uid,
                        "attachmentType" to mediaItem.mediaType,
                        "mimeType" to mediaItem.mimeType,
                        "fileName" to mediaItem.fileName,
                        "downloadUrl" to mediaItem.url,
                        "extendedText" to "",
                        "mediaDataUrl" to mediaItem.separateAttachmentDataUrl,
                        "sizeBytes" to mediaItem.sizeBytes,
                        "visibility" to "PUBLIC",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    val attachmentValidation = ProfilePhotoHelper.validateAttachmentDocumentSizeBeforeWrite(
                        documentPath = mediaRef.path,
                        data = mediaPayload
                    )
                    if (attachmentValidation.isFailure) {
                        return Result.failure(
                            attachmentValidation.exceptionOrNull()
                                ?: IllegalArgumentException(ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA)
                        )
                    }
                    updateTasks += mediaRef.set(mediaPayload)
                    ProfilePhotoHelper.cacheAttachmentDataUrl(mediaDocId, mediaItem.separateAttachmentDataUrl)
                    itemAttachmentDocId = mediaDocId
                    if (primaryAttachmentDocId.isBlank()) {
                        primaryAttachmentDocId = mediaDocId
                    }
                }
                metadataMaps.add(mediaItem.toMetadataMap(itemAttachmentDocId))
            }

            val updates = mapOf(
                "title" to title.trim().take(200),
                "district" to district.trim().take(100),
                "upazilaOrLocation" to upazilaOrLocation.trim().take(200),
                "designation" to resolvedDesignation.take(150),
                "qualification" to qualification.trim().take(200),
                "salaryOrExperience" to salaryOrExperience.trim().take(150),
                "deadlineOrPreferredDistricts" to deadlineOrPreferredDistricts.trim().take(250),
                "description" to trimmedDesc.take(2000),
                "contactNumber" to contactNumber.trim().take(100),
                "khedmotCategories" to cleanedCategories,
                "customCategory" to cleanedCustom,
                "imageUrls" to uploadedMedia.map { it.url },
                "mediaTypes" to uploadedMedia.map { it.mediaType },
                "mediaMetadata" to metadataMaps,
                "attachmentDocId" to primaryAttachmentDocId,
                "isEdited" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Pre-write document size validation (projected post-update document state)
            val projectedPayload = mapOf(
                "authorId" to post.authorId,
                "authorName" to post.authorName,
                "authorPhotoUrl" to post.authorPhotoUrl,
                "postType" to post.postType,
                "title" to updates["title"],
                "district" to updates["district"],
                "upazilaOrLocation" to updates["upazilaOrLocation"],
                "designation" to updates["designation"],
                "qualification" to updates["qualification"],
                "salaryOrExperience" to updates["salaryOrExperience"],
                "deadlineOrPreferredDistricts" to updates["deadlineOrPreferredDistricts"],
                "description" to updates["description"],
                "contactNumber" to updates["contactNumber"],
                "khedmotCategories" to updates["khedmotCategories"],
                "customCategory" to updates["customCategory"],
                "imageUrls" to updates["imageUrls"],
                "mediaTypes" to updates["mediaTypes"],
                "mediaMetadata" to updates["mediaMetadata"],
                "attachmentDocId" to updates["attachmentDocId"],
                "userReactions" to post.userReactions,
                "userReactionNames" to post.userReactionNames
            )
            val sizeValidation = ProfilePhotoHelper.validateFirestoreDocumentSizeBeforeWrite(
                documentPath = docRef.path,
                data = projectedPayload
            )
            if (sizeValidation.isFailure) {
                return Result.failure(
                    sizeValidation.exceptionOrNull()
                        ?: IllegalArgumentException(ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA)
                )
            }

            runCatching { db.enableNetwork() }
            updateTasks += docRef.update(updates)
            try {
                kotlinx.coroutines.withTimeout(15_000L) {
                    com.google.android.gms.tasks.Tasks.whenAll(updateTasks).await()
                }
            } catch (timeoutEx: kotlinx.coroutines.TimeoutCancellationException) {
                val taskError = updateTasks.firstNotNullOfOrNull { it.exception }
                if (taskError != null) {
                    handleFirestoreError(taskError, OperationType.UPDATE, docRef.path)
                    return Result.failure(translateFirestoreExceptionToBangla(taskError))
                }
                runCatching { db.enableNetwork() }
                handleFirestoreError(timeoutEx, OperationType.UPDATE, docRef.path)
                return Result.failure(
                    IllegalStateException("সার্ভারে পোস্ট হালনাগাদ করতে নির্ধারিত সময়ের বেশি লাগছে। অনুগ্রহ করে পুনরায় চেষ্টা করুন।")
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    /**
     * Fetches a separate extended attachment document (`/feed_post_attachments/{attachmentId}`)
     * for posts that offloaded extended text or large attachment payloads outside `/feed_posts/{postId}`.
     */
    suspend fun getFeedPostExtendedAttachment(attachmentId: String): FeedPostExtendedAttachment? {
        if (attachmentId.isBlank()) return null
        val path = "feed_post_attachments/$attachmentId"
        return try {
            val snap = db.collection("feed_post_attachments").document(attachmentId).get().awaitWithTimeout(8_000L)
            if (snap != null && snap.exists()) {
                val obj = snap.toObject(FeedPostExtendedAttachment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (obj != null && obj.mediaDataUrl.isNotBlank()) {
                    ProfilePhotoHelper.cacheAttachmentDataUrl(attachmentId, obj.mediaDataUrl)
                }
                obj
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    /**
     * Non-destructively inspects existing `feed_posts` authored by the signed-in user and
     * migrates any oversized inline Base64 images/videos or oversized `authorPhotoUrl` to
     * Firebase Storage / separate `/feed_post_attachments/{attachmentId}` documents.
     * Never deletes existing posts or resets data.
     */
    suspend fun inspectAndSanitizeExistingFeedPosts(): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.success(0)
        return try {
            val snap = db.collection("feed_posts")
                .whereEqualTo("authorId", uid)
                .get()
                .await()
            var sanitizedCount = 0
            for (doc in snap.documents) {
                val post = doc.safeToObject(FeedPost::class.java)
                    ?: continue
                val hasLargeInlineImage = post.imageUrls.any {
                    it.length > ProfilePhotoHelper.MAX_INLINE_FEED_IMAGE_PREVIEW_CHARS ||
                        it.startsWith("data:video/", ignoreCase = true)
                }
                val hasLargeAvatar = post.authorPhotoUrl.length > ProfilePhotoHelper.MAX_INLINE_AVATAR_CHARS_IN_POST
                if (!hasLargeInlineImage && !hasLargeAvatar) continue

                val uploadedMedia = ProfilePhotoHelper.uploadFeedMediaToFirebaseStorage(
                    mediaItems = post.imageUrls,
                    authorId = uid,
                    postId = doc.id
                ).getOrNull() ?: continue

                var primaryAttachId = post.attachmentDocId
                val metadataMaps = mutableListOf<Map<String, String>>()
                uploadedMedia.forEachIndexed { idx, item ->
                    var itemDocId = ""
                    if (item.separateAttachmentDataUrl.isNotBlank()) {
                        val attachDocId = "${doc.id}_media_$idx"
                        val attachRef = db.collection("feed_post_attachments").document(attachDocId)
                        val existingAttach = attachRef.getCacheFirstOrNull(1_000L)
                        if (existingAttach == null || !existingAttach.exists()) {
                            attachRef.set(
                                mapOf(
                                    "postId" to doc.id,
                                    "authorId" to uid,
                                    "attachmentType" to item.mediaType,
                                    "mimeType" to item.mimeType,
                                    "fileName" to item.fileName,
                                    "downloadUrl" to item.url,
                                    "extendedText" to "",
                                    "mediaDataUrl" to item.separateAttachmentDataUrl,
                                    "sizeBytes" to item.sizeBytes,
                                    "visibility" to "PUBLIC",
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).awaitWithTimeout(8_000L)
                        }
                        itemDocId = attachDocId
                        if (primaryAttachId.isBlank()) primaryAttachId = attachDocId
                    }
                    metadataMaps.add(item.toMetadataMap(itemDocId))
                }

                val safeAvatar = ProfilePhotoHelper.uploadOrCompactAuthorPhotoForFeedPost(
                    authorPhotoUrl = post.authorPhotoUrl,
                    authorId = uid
                )
                doc.reference.update(
                    mapOf(
                        "authorPhotoUrl" to safeAvatar,
                        "imageUrls" to uploadedMedia.map { it.url },
                        "mediaTypes" to uploadedMedia.map { it.mediaType },
                        "mediaMetadata" to metadataMaps,
                        "attachmentDocId" to primaryAttachId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitWithTimeout(8_000L)
                sanitizedCount++
            }
            Result.success(sanitizedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePostReaction(
        post: FeedPost,
        reactionCode: String,
        reactorName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("feed_posts").document(post.id)
        return try {
            val cachedSnap = docRef.getCacheFirstOrNull(400L)
            val existingPost = cachedSnap?.toFeedPostOrNull() ?: post
            val currentReaction = existingPost.userReactions[uid].orEmpty()
            val isRemoving = reactionCode.isBlank() || currentReaction == reactionCode
            val cleanReactorName = reactorName.trim().ifBlank { "সম্মানিত সদস্য" }.take(120)

            try {
                if (isRemoving) {
                    docRef.update(
                        com.google.firebase.firestore.FieldPath.of("userReactions", uid),
                        FieldValue.delete(),
                        com.google.firebase.firestore.FieldPath.of("userReactionNames", uid),
                        FieldValue.delete(),
                        com.google.firebase.firestore.FieldPath.of("updatedAt"),
                        FieldValue.serverTimestamp()
                    ).awaitMessageWriteConfirmation(docRef, 12_000L)
                } else {
                    docRef.update(
                        com.google.firebase.firestore.FieldPath.of("userReactions", uid),
                        reactionCode,
                        com.google.firebase.firestore.FieldPath.of("userReactionNames", uid),
                        cleanReactorName,
                        com.google.firebase.firestore.FieldPath.of("updatedAt"),
                        FieldValue.serverTimestamp()
                    ).awaitMessageWriteConfirmation(docRef, 12_000L)
                }
            } catch (_: Exception) {
                val freshServerPost = runCatching {
                    kotlinx.coroutines.withTimeoutOrNull(4_000L) {
                        docRef.get().await().toFeedPostOrNull()
                    }
                }.getOrNull() ?: existingPost
                val updatedReactions = freshServerPost.userReactions.toMutableMap()
                val updatedNames = freshServerPost.userReactionNames.toMutableMap()
                if (isRemoving) {
                    updatedReactions.remove(uid)
                    updatedNames.remove(uid)
                } else {
                    updatedReactions[uid] = reactionCode
                    updatedNames[uid] = cleanReactorName
                }
                docRef.update(
                    mapOf(
                        "userReactions" to updatedReactions,
                        "userReactionNames" to updatedNames,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitMessageWriteConfirmation(docRef, 12_000L)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observePublicPostComments(): Flow<List<FeedComment>> {
        val path = "post_comments"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(path),
            path = path,
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            val currentUid = resolveEffectiveUserId("")
            snapshot.documents
                .mapNotNull { it.toFeedCommentOrNull() }
                .filter { comment ->
                    comment.visibility.isBlank() ||
                        comment.visibility.equals("PUBLIC", ignoreCase = true) ||
                        (currentUid.isNotBlank() && comment.authorId == currentUid)
                }
                .distinctBy { it.id }
                .sortedWith(
                    compareBy<FeedComment> { it.createdAt ?: Timestamp(0, 0) }
                        .thenBy { it.id }
                )
        }
    }

    suspend fun addPostComment(
        post: FeedPost,
        authorName: String,
        authorPhotoUrl: String,
        text: String,
        parentCommentId: String = "",
        replyToAuthorName: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(1500)
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("কমেন্ট খালি রাখা যাবে না।"))
        }
        val commentRef = db.collection("post_comments").document()
        val postRef = db.collection("feed_posts").document(post.id)
        return try {
            val resolvedAuthor = authorName.trim().ifBlank { "সম্মানিত দ্বীনি ভাই" }.take(200)
            val compactAvatar = ProfilePhotoHelper.compactSenderPhotoForMessage(authorPhotoUrl)
            val payload = mapOf(
                "postId" to post.id,
                "authorId" to uid,
                "authorName" to resolvedAuthor,
                "authorPhotoUrl" to compactAvatar,
                "text" to cleanText,
                "parentCommentId" to parentCommentId.trim(),
                "replyToAuthorName" to replyToAuthorName.trim().take(200),
                "isEdited" to false,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            commentRef.set(payload).awaitMessageWriteConfirmation(
                commentRef,
                12_000L
            )

            runCatching {
                try {
                    postRef.update(
                        mapOf(
                            "commentCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).awaitMessageWriteConfirmation(postRef, 12_000L)
                } catch (_: Exception) {
                    val postSnap = postRef.getFastOrNull(2_000L)
                    val currentCount = (postSnap?.getLong("commentCount")?.toInt() ?: post.commentCount).coerceAtLeast(0)
                    postRef.update(
                        mapOf(
                            "commentCount" to (currentCount + 1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).awaitMessageWriteConfirmation(postRef, 12_000L)
                }
            }

            if (post.authorId.isNotBlank() && post.authorId != uid) {
                kotlinx.coroutines.withTimeoutOrNull(3_000L) {
                    runCatching {
                        createNotification(
                            recipientId = post.authorId,
                            senderName = resolvedAuthor,
                            title = if (parentCommentId.isNotBlank()) "আপনার পোস্টের কমেন্টে নতুন উত্তর" else "আপনার পোস্টে নতুন কমেন্ট",
                            message = "$resolvedAuthor মন্তব্য করেছেন: \"${cleanText.take(120)}\"",
                            type = "APPLICATION",
                            referenceId = post.id
                        )
                    }
                }
            }

            Result.success(commentRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun editPostComment(
        commentId: String,
        newText: String
    ): Result<Unit> {
        requireUserId()
        val cleanText = newText.trim().take(1500)
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("কমেন্ট খালি রাখা যাবে না।"))
        }
        val commentRef = db.collection("post_comments").document(commentId)
        return try {
            commentRef.update(
                mapOf(
                    "text" to cleanText,
                    "isEdited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitMessageWriteConfirmation(commentRef, 12_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePostComment(
        commentId: String,
        postId: String,
        currentCommentCount: Int
    ): Result<Unit> {
        requireUserId()
        val commentRef = db.collection("post_comments").document(commentId)
        return try {
            commentRef.delete().awaitWithTimeout(12_000L)
            if (postId.isNotBlank()) {
                runCatching {
                    val postRef = db.collection("feed_posts").document(postId)
                    val snap = postRef.getFastOrNull(2_000L)
                    val existingCount = (snap?.getLong("commentCount")?.toInt() ?: currentCommentCount).coerceAtLeast(1)
                    postRef.update(
                        mapOf(
                            "commentCount" to (existingCount - 1).coerceAtLeast(0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).awaitMessageWriteConfirmation(postRef, 12_000L)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            Result.failure(e)
        }
    }

        suspend fun shareFeedPostInApp(
        originalPost: FeedPost,
        sharerName: String,
        sharerPhotoUrl: String,
        sharerDistrict: String,
        shareCaption: String,
        preallocatedPostId: String = ""
    ): Result<String> {
        requireUserId()
        val targetOriginalId = originalPost.sharedPostId.ifBlank { originalPost.id }
        val targetOriginalAuthorId = originalPost.sharedAuthorId.ifBlank { originalPost.authorId }
        val targetOriginalAuthorName = originalPost.sharedAuthorName.ifBlank { originalPost.authorName.ifBlank { originalPost.title } }
        val targetOriginalTitle = originalPost.sharedTitle.ifBlank { originalPost.title }
        val targetOriginalDesc = originalPost.sharedDescription.ifBlank {
            buildString {
                if (originalPost.designation.isNotBlank()) append("পদ/বিভাগ: ${originalPost.designation} | ")
                if (originalPost.district.isNotBlank()) append("জেলা: ${originalPost.district} | ")
                append(originalPost.description)
            }
        }
        val targetOriginalType = originalPost.sharedPostType.ifBlank { originalPost.postType }

        val createRes = createFeedPost(
            authorName = sharerName.ifBlank { "সম্মানিত দ্বীনি ভাই" },
            postType = "GENERAL_POST",
            title = shareCaption.trim().ifBlank { "$targetOriginalAuthorName-এর পোস্ট শেয়ার করেছেন" }.take(200),
            district = sharerDistrict.ifBlank { originalPost.district },
            upazilaOrLocation = originalPost.upazilaOrLocation,
            designation = originalPost.designation.ifBlank { "দ্বীনি খেদমত শেয়ার" },
            qualification = originalPost.qualification,
            salaryOrExperience = originalPost.salaryOrExperience,
            deadlineOrPreferredDistricts = originalPost.deadlineOrPreferredDistricts,
            description = shareCaption.trim(),
            contactNumber = originalPost.contactNumber,
            authorPhotoUrl = sharerPhotoUrl,
            khedmotCategories = originalPost.khedmotCategories,
            customCategory = originalPost.customCategory,
            sharedPostId = targetOriginalId,
            sharedAuthorId = targetOriginalAuthorId,
            sharedAuthorName = targetOriginalAuthorName,
            sharedTitle = targetOriginalTitle,
            sharedDescription = targetOriginalDesc,
            sharedPostType = targetOriginalType,
            preallocatedPostId = preallocatedPostId
        )

        createRes.onSuccess {
            runCatching {
                val origRef = db.collection("feed_posts").document(originalPost.id)
                val incTask = origRef.update(
                    mapOf(
                        "shareCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                    runCatching { incTask.await() }
                }
            }
        }
        return createRes
    }

    suspend fun reportFeedPost(
        post: FeedPost,
        reporterName: String,
        reason: String
    ): Result<Unit> {
        requireUserId()
        return try {
            enqueueAndDispatchTelegramNotification(
                eventType = "CONTENT_MODERATION",
                title = "ফিড পোস্ট রিপোর্ট (মডারেশন প্রয়োজন)",
                message = "পোস্ট: '${post.title}' (প্রকাশক: ${post.authorName}) | রিপোর্টের কারণ: ${reason.trim().ifBlank { "নীতিমালা পরিপন্থী পোস্ট" }}",
                actorName = reporterName.ifBlank { "ব্যবহারকারী" },
                referenceId = post.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFeedPost(postId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("feed_posts").document(postId)
        return try {
            docRef.delete().awaitWithTimeout(8_000L)
            val currentUser = auth.currentUser
            if (currentUser != null && AdminSecurityHelper.verifyServerTokenAdminClaims(currentUser)) {
                enqueueAndDispatchTelegramNotification(
                    eventType = "CONTENT_MODERATION",
                    title = "ফিড পোস্ট অপসারণ (মডারেশন)",
                    message = "পোস্ট আইডি ($postId) ফিড থেকে অপসারণ করা হয়েছে।",
                    actorName = currentUser.email ?: "নাজিমে আলা (অ্যাডমিন)",
                    referenceId = postId
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdatePostVerification(
        post: FeedPost,
        isVerified: Boolean,
        approvalStatus: String,
        adminName: String = "অ্যাডমিন প্যানেল"
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("feed_posts").document(post.id)
        return try {
            docRef.update(
                mapOf(
                    "isVerifiedMadrasa" to isVerified,
                    "approvalStatus" to approvalStatus,
                    "visibility" to if (approvalStatus.equals("REJECTED", ignoreCase = true)) "ARCHIVED" else "PUBLIC",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitMessageWriteConfirmation(docRef, 12_000L)

            kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                coroutineScope {
                    if (isVerified && post.authorId.isNotBlank() && post.authorId != uid) {
                        launch {
                            runCatching {
                                createNotification(
                                    recipientId = post.authorId,
                                    senderName = adminName,
                                    title = "বিজ্ঞপ্তি যাচাইকৃত ব্যাজ প্রাপ্তি",
                                    message = "আপনার প্রকাশিত '${post.title}' পোস্টটিতে যাচাইকৃত ব্যাজ যুক্ত করা হয়েছে।",
                                    type = "VERIFICATION",
                                    referenceId = post.id
                                )
                            }
                        }
                    }
                    launch {
                        runCatching {
                            enqueueAndDispatchTelegramNotification(
                                eventType = "ADMIN_VERIFICATION",
                                title = "পোস্ট যাচাইকরণ হালনাগাদ",
                                message = "'${post.title}' (${post.designation}) পোস্টের যাচাইকরণ অবস্থা: ${if (isVerified) "যাচাইকৃত ✓" else "আনভেরিফাই"}",
                                actorName = adminName,
                                referenceId = post.id
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    suspend fun adminTogglePostBoost(
        post: FeedPost,
        isBoosted: Boolean,
        adminName: String = "অ্যাডমিন প্যানেল"
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("feed_posts").document(post.id)
        return try {
            docRef.update(
                mapOf(
                    "isBoosted" to isBoosted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitMessageWriteConfirmation(docRef, 12_000L)

            kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                coroutineScope {
                    if (isBoosted && post.authorId.isNotBlank() && post.authorId != uid) {
                        launch {
                            runCatching {
                                createNotification(
                                    recipientId = post.authorId,
                                    senderName = adminName,
                                    title = "বিজ্ঞপ্তিটি ফিডের শীর্ষে Boosted হয়েছে",
                                    message = "অ্যাডমিন প্যানেল থেকে আপনার '${post.title}' (${post.designation}) বিজ্ঞপ্তিটি ফিডের উপরে বিশেষভাবে প্রদর্শিত হচ্ছে।",
                                    type = "BOOST",
                                    referenceId = post.id
                                )
                            }
                        }
                    }
                    launch {
                        runCatching {
                            enqueueAndDispatchTelegramNotification(
                                eventType = "ADMIN_VERIFICATION",
                                title = if (isBoosted) "বিজ্ঞপ্তি Boosted করা হয়েছে" else "বিজ্ঞপ্তির Boost সরানো হয়েছে",
                                message = "'${post.title}' (${post.designation} - ${post.district}) বিজ্ঞপ্তির Boost অবস্থা হালনাগাদ করা হয়েছে।",
                                actorName = adminName,
                                referenceId = post.id
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    // --- 3. BOOKMARKS (পছন্দের খেদমত) ---

    fun observeBookmarks(userId: String): Flow<List<BookmarkItem>> {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (effectiveUid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val path = "users/$effectiveUid/bookmarks"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection("users").document(effectiveUid).collection("bookmarks"),
            path = path,
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.safeToObjects(BookmarkItem::class.java)
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun toggleBookmark(post: FeedPost, currentlyBookmarked: Boolean): Result<Boolean> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("bookmarks").document(post.id)
        return try {
            if (currentlyBookmarked) {
                docRef.delete().awaitWithTimeout(8_000L)
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "postId" to post.id,
                    "postType" to post.postType,
                    "title" to post.title,
                    "district" to post.district,
                    "designation" to post.designation,
                    "qualification" to post.qualification,
                    "salaryOrExperience" to post.salaryOrExperience,
                    "deadlineOrPreferredDistricts" to post.deadlineOrPreferredDistricts,
                    "authorId" to post.authorId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitWithTimeout(8_000L)
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 4. MADRASA DIRECTORY (মাদরাসা ও যাচাইকৃত মাদরাসা) ---

    fun observePublicMadrasas(): Flow<List<MadrasaProfile>> {
        val path = "madrasas"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(path),
            path = path,
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            val currentUid = resolveEffectiveUserId("")
            snapshot.safeToObjects(MadrasaProfile::class.java)
                .filter { madrasa ->
                    madrasa.visibility.isBlank() ||
                        madrasa.visibility.equals("PUBLIC", ignoreCase = true) ||
                        (currentUid.isNotBlank() && madrasa.ownerId == currentUid)
                }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun createMadrasa(
        name: String,
        district: String,
        address: String,
        establishedYear: String,
        highestMarhala: String,
        facilities: String,
        studentCount: String,
        contactPhone: String
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("madrasas").document()
        return try {
            val payload = mapOf(
                "ownerId" to uid,
                "name" to name.trim(),
                "district" to district.trim(),
                "address" to address.trim(),
                "establishedYear" to establishedYear.trim(),
                "highestMarhala" to highestMarhala.trim(),
                "facilities" to facilities.trim(),
                "studentCount" to studentCount.trim(),
                "contactPhone" to contactPhone.trim(),
                "isVerified" to false,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitWithTimeout(10_000L)

            kotlinx.coroutines.withTimeoutOrNull(3500L) {
                runCatching {
                    enqueueAndDispatchTelegramNotification(
                        eventType = "NEW_MADRASA",
                        title = "নতুন মাদরাসা নিবন্ধন: ${name.trim()}",
                        message = "জেলা: ${district.trim()} | সর্বোচ্চ মারহালা: ${highestMarhala.trim()} | যোগাযোগ: ${contactPhone.trim()}",
                        actorName = name.trim(),
                        referenceId = docRef.id
                    )
                }
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    suspend fun adminToggleMadrasaVerification(
        madrasa: MadrasaProfile,
        verified: Boolean,
        adminName: String = "অ্যাডমিন প্যানেল"
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("madrasas").document(madrasa.id)
        return try {
            docRef.update(
                mapOf(
                    "isVerified" to verified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitWithTimeout(10_000L)

            if (verified && madrasa.ownerId.isNotBlank() && madrasa.ownerId != uid) {
                kotlinx.coroutines.withTimeoutOrNull(3000L) {
                    runCatching {
                        createNotification(
                            recipientId = madrasa.ownerId,
                            senderName = adminName,
                            title = "মাদরাসা যাচাইকরণ অনুমোদিত",
                            message = "আলহামদুলিল্লাহ, আপনার '${madrasa.name}' (${madrasa.district}) যাচাইকৃত মাদরাসা হিসেবে তালিকাভুক্ত হয়েছে।",
                            type = "VERIFICATION",
                            referenceId = madrasa.id
                        )
                    }
                }
            }

            kotlinx.coroutines.withTimeoutOrNull(3500L) {
                runCatching {
                    enqueueAndDispatchTelegramNotification(
                        eventType = "ADMIN_VERIFICATION",
                        title = if (verified) "মাদরাসা যাচাইকৃত অনুমোদিত" else "মাদরাসা যাচাইকরণ পরিবর্তন",
                        message = "'${madrasa.name}' (${madrasa.district}) মাদরাসার যাচাইকরণ স্ট্যাটাস: ${if (verified) "যাচাইকৃত ✓" else "অযাচাইকৃত"}",
                        actorName = adminName,
                        referenceId = madrasa.id
                    )
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    // --- 5. KHEDMOT SUBMISSIONS (খেদমতে সরাসরি আবেদন) ---

    fun observeMySentSubmissions(userId: String): Flow<List<KhedmotSubmission>> {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (effectiveUid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val collectionName = "khedmot_submissions"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(collectionName).whereEqualTo("applicantId", effectiveUid),
            path = "khedmot_submissions/sent/$effectiveUid",
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.documents
                .mapNotNull { it.toKhedmotSubmissionOrNull() }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observeReceivedSubmissions(userId: String): Flow<List<KhedmotSubmission>> {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (effectiveUid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val collectionName = "khedmot_submissions"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(collectionName).whereEqualTo("recipientId", effectiveUid),
            path = "khedmot_submissions/received/$effectiveUid",
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.documents
                .mapNotNull { it.toKhedmotSubmissionOrNull() }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun generateSubmissionDocumentId(): String {
        return db.collection("khedmot_submissions").document().id
    }

    suspend fun submitApplicationToCircular(
        post: FeedPost,
        applicantName: String,
        applicantQualification: String,
        applicantExperience: String,
        applicantPhone: String,
        coverNote: String,
        preallocatedSubmissionId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val docRef = if (preallocatedSubmissionId.isNotBlank()) {
            db.collection("khedmot_submissions").document(preallocatedSubmissionId.trim())
        } else {
            db.collection("khedmot_submissions").document()
        }
        return try {
            val safePostId = post.id.trim().ifBlank { "post_${System.currentTimeMillis()}" }.take(128)
            val safePostTitle = post.title.trim().ifBlank { "দ্বীনি খেদমত বিজ্ঞপ্তি" }.take(200)
            val safeDesignation = post.designation.trim().ifBlank { "দ্বীনি খেদমত" }.take(150)
            val safeApplicantName = applicantName.trim().ifBlank { "খেদমতপ্রার্থী" }.take(150)
            val safeQualification = applicantQualification.trim().take(200)
            val safeExperience = applicantExperience.trim().take(150)
            val safePhone = applicantPhone.trim().take(100)
            val safeCoverNote = coverNote.trim().take(1000)
            val safeRecipientId = post.authorId.trim().ifBlank { uid }.take(128)

            val payload = mapOf(
                "postId" to safePostId,
                "postTitle" to safePostTitle,
                "postDesignation" to safeDesignation,
                "applicantId" to uid,
                "applicantName" to safeApplicantName,
                "applicantQualification" to safeQualification,
                "applicantExperience" to safeExperience,
                "applicantPhone" to safePhone,
                "coverNote" to safeCoverNote,
                "recipientId" to safeRecipientId,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (preallocatedSubmissionId.isNotBlank()) {
                val cachedExisting = docRef.getFromCacheOnlyOrNull(60L)
                if (cachedExisting != null &&
                    cachedExisting.exists() &&
                    !cachedExisting.metadata.hasPendingWrites() &&
                    cachedExisting.getString("applicantId") == uid
                ) {
                    FirestoreConnectionMonitor.onSnapshotReceived(
                        path = "khedmot_submissions/sent/$uid",
                        isFromCache = false,
                        hasPendingWrites = false
                    )
                    return Result.success(docRef.id)
                }
            }

            runCatching { db.enableNetwork() }
            val writeTask = docRef.set(payload)
            writeTask.addOnCompleteListener { completedTask ->
                if (completedTask.isSuccessful) {
                    FirestoreConnectionMonitor.onSnapshotReceived(
                        path = "khedmot_submissions/sent/$uid",
                        isFromCache = false,
                        hasPendingWrites = false
                    )
                } else {
                    completedTask.exception?.let { err ->
                        handleFirestoreError(err, OperationType.CREATE, docRef.path)
                    }
                }
            }
            try {
                kotlinx.coroutines.withTimeout(15_000L) {
                    writeTask.await()
                }
            } catch (timeoutEx: kotlinx.coroutines.TimeoutCancellationException) {
                val taskError = writeTask.exception
                if (taskError != null) {
                    handleFirestoreError(taskError, OperationType.CREATE, docRef.path)
                    return Result.failure(translateFirestoreExceptionToBangla(taskError))
                }
                val serverConfirmed = runCatching {
                    val snap = withTimeoutOrNull(1_500L) { docRef.get(Source.SERVER).await() }
                    snap != null && snap.exists() && !snap.metadata.hasPendingWrites() && snap.getString("applicantId") == uid
                }.getOrDefault(false)
                if (!serverConfirmed) {
                    runCatching { db.enableNetwork() }
                    handleFirestoreError(timeoutEx, OperationType.CREATE, docRef.path)
                    return Result.failure(
                        IllegalStateException("সার্ভারে আবেদন সংরক্ষণে নির্ধারিত সময়ের বেশি লাগছে। অনুগ্রহ করে ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।")
                    )
                }
            }

            // Deliver real-time notification to the Madrasa / circular author and Telegram Admin log concurrently with a fast timeout
            kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                coroutineScope {
                    if (post.authorId.isNotBlank() && post.authorId != uid) {
                        launch {
                            runCatching {
                                createNotification(
                                    recipientId = post.authorId,
                                    senderName = applicantName.trim(),
                                    title = "নতুন খেদমতের আবেদন প্রাপ্তি",
                                    message = "${applicantName.trim()} আপনার '${post.title}'-এর '${post.designation}' খেদমতের জন্য আবেদন করেছেন।",
                                    type = "APPLICATION",
                                    referenceId = docRef.id
                                )
                            }
                        }
                    }
                    launch {
                        runCatching {
                            enqueueAndDispatchTelegramNotification(
                                eventType = "KHEDMOT_SUBMISSION",
                                title = "বিজ্ঞপ্তিতে নতুন খেদমত আবেদন: ${post.title}",
                                message = "প্রার্থী: ${applicantName.trim()} (${applicantQualification.trim()}) | পদ: ${post.designation} | ফোন: ${applicantPhone.trim()}",
                                actorName = applicantName.trim(),
                                referenceId = docRef.id
                            )
                        }
                    }
                }
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (preallocatedSubmissionId.isNotBlank()) {
                val alreadyOnServer = runCatching {
                    val snap = withTimeoutOrNull(2_000L) { docRef.get(Source.SERVER).await() }
                    snap != null && snap.exists() && !snap.metadata.hasPendingWrites() && snap.getString("applicantId") == uid
                }.getOrDefault(false)
                if (alreadyOnServer) {
                    return Result.success(docRef.id)
                }
            }
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    suspend fun updateSubmissionStatus(
        submission: KhedmotSubmission,
        newStatus: String,
        actorName: String,
        publishToFeedOnAccept: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("khedmot_submissions").document(submission.id)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitMessageWriteConfirmation(docRef, 12_000L)

            if (publishToFeedOnAccept && newStatus == "ACCEPTED") {
                runCatching {
                    publishApprovedSubmissionToFeed(submission, uid)
                }
            } else if (publishToFeedOnAccept && newStatus == "REJECTED") {
                runCatching {
                    unpublishRejectedSubmissionFromFeed(submission)
                }
            }

            if (submission.applicantId.isNotBlank() && submission.applicantId != uid) {
                val statusBangla = when (newStatus) {
                    "SHORTLISTED" -> "প্রাথমিক তালিকায় নির্বাচিত (মুযাকারার জন্য)"
                    "ACCEPTED" -> "কবুল / চূড়ান্তভাবে নির্বাচিত ও ফিডে প্রকাশিত"
                    "REJECTED" -> "অপেক্ষমাণ / এবারের জন্য নির্বাচিত হননি"
                    else -> "হালনাগাদ"
                }
                kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                    runCatching {
                        createNotification(
                            recipientId = submission.applicantId,
                            senderName = actorName,
                            title = "আবেদনের অবস্থা হালনাগাদ",
                            message = "'${submission.postTitle}'-এ আপনার খেদমতের আবেদনের বর্তমান অবস্থা: $statusBangla।",
                            type = "APPLICATION",
                            referenceId = submission.id
                        )
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    private suspend fun publishApprovedSubmissionToFeed(
        submission: KhedmotSubmission,
        fallbackAuthorUid: String
    ) {
        if (submission.id.startsWith("app_") && submission.postId.isNotBlank()) {
            val existingPostRef = db.collection("feed_posts").document(submission.postId)
            existingPostRef.update(
                mapOf(
                    "isVerifiedMadrasa" to true,
                    "approvalStatus" to "APPROVED",
                    "visibility" to "PUBLIC",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitMessageWriteConfirmation(existingPostRef, 10_000L)
            return
        }

        val effectiveAuthorId = submission.applicantId.ifBlank { fallbackAuthorUid }
        val applicantProfileSnap = if (effectiveAuthorId.isNotBlank()) {
            runCatching {
                db.collection("users").document(effectiveAuthorId).get().awaitWithTimeout(4_000L)
            }.getOrNull()
        } else null
        val applicantProfile = applicantProfileSnap?.safeToObject(UserProfile::class.java)
        val rawPhotoUrl = applicantProfile?.photoUrl.orEmpty()
        val safeAuthorPhotoUrl = ProfilePhotoHelper.uploadOrCompactAuthorPhotoForFeedPost(
            authorPhotoUrl = rawPhotoUrl,
            authorId = effectiveAuthorId
        )
        val resolvedDistrict = applicantProfile?.district?.takeIf { it.isNotBlank() } ?: "সকল জেলা"
        val resolvedAuthorName = submission.applicantName
            .ifBlank { applicantProfile?.fullName.orEmpty() }
            .ifBlank { "সম্মানিত খেদমতপ্রার্থী" }
            .take(200)
        val resolvedDesignation = submission.postDesignation
            .ifBlank { applicantProfile?.designation.orEmpty() }
            .ifBlank { "দ্বীনি খেদমত" }
            .take(150)
        val resolvedTitle = "${resolvedAuthorName} — ${resolvedDesignation}".take(200)
        val resolvedQualification = submission.applicantQualification
            .ifBlank { applicantProfile?.qualification.orEmpty() }
            .ifBlank { "আলোচনা সাপেক্ষ" }
            .take(200)
        val resolvedExperience = submission.applicantExperience
            .ifBlank { applicantProfile?.experience.orEmpty() }
            .ifBlank { "আলোচনা সাপেক্ষ" }
            .take(150)
        val resolvedDescription = submission.coverNote
            .ifBlank { "${submission.postTitle.ifBlank { "দ্বীনি প্রতিষ্ঠান" }}-এ '${resolvedDesignation}' খেদমতের জন্য অ্যাডমিন কর্তৃক অনুমোদিত আবেদন।" }
            .take(2000)
        val resolvedContact = submission.applicantPhone
            .ifBlank { applicantProfile?.contactInfo.orEmpty() }
            .ifBlank { "প্রোফাইলে উল্লেখিত" }
            .take(100)

        val feedPostId = "sub_post_${submission.id}".take(128)
        val postRef = db.collection("feed_posts").document(feedPostId)
        val existingSnap = runCatching { postRef.get().awaitWithTimeout(4_000L) }.getOrNull()
        if (existingSnap != null && existingSnap.exists()) {
            postRef.update(
                mapOf(
                    "isVerifiedMadrasa" to true,
                    "approvalStatus" to "APPROVED",
                    "visibility" to "PUBLIC",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitMessageWriteConfirmation(postRef, 10_000L)
        } else {
            val payload = mapOf(
                "authorId" to effectiveAuthorId,
                "authorName" to resolvedAuthorName,
                "authorPhotoUrl" to safeAuthorPhotoUrl,
                "postType" to "SEEKER_APPLICATION",
                "title" to resolvedTitle,
                "district" to resolvedDistrict.take(100),
                "upazilaOrLocation" to resolvedDistrict.take(200),
                "designation" to resolvedDesignation,
                "qualification" to resolvedQualification,
                "salaryOrExperience" to resolvedExperience,
                "deadlineOrPreferredDistricts" to submission.postTitle.ifBlank { "সকল জেলা" }.take(250),
                "description" to resolvedDescription,
                "contactNumber" to resolvedContact,
                "madrasaId" to submission.postId.take(128),
                "isVerifiedMadrasa" to true,
                "isBoosted" to false,
                "khedmotCategories" to emptyList<String>(),
                "customCategory" to "",
                "imageUrls" to emptyList<String>(),
                "mediaTypes" to emptyList<String>(),
                "mediaMetadata" to emptyList<Map<String, String>>(),
                "attachmentDocId" to "",
                "userReactions" to emptyMap<String, String>(),
                "userReactionNames" to emptyMap<String, String>(),
                "commentCount" to 0,
                "shareCount" to 0,
                "sharedPostId" to "",
                "sharedAuthorId" to "",
                "sharedAuthorName" to "",
                "sharedTitle" to "",
                "sharedDescription" to "",
                "sharedPostType" to "",
                "isEdited" to false,
                "visibility" to "PUBLIC",
                "approvalStatus" to "APPROVED",
                "createdAt" to (submission.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            postRef.set(payload).awaitMessageWriteConfirmation(postRef, 10_000L)
        }
    }

    private suspend fun unpublishRejectedSubmissionFromFeed(submission: KhedmotSubmission) {
        val targetPostId = if (submission.id.startsWith("app_") && submission.postId.isNotBlank()) {
            submission.postId
        } else {
            "sub_post_${submission.id}".take(128)
        }
        val postRef = db.collection("feed_posts").document(targetPostId)
        val snap = runCatching { postRef.get().awaitWithTimeout(4_000L) }.getOrNull()
        if (snap != null && snap.exists()) {
            postRef.update(
                mapOf(
                    "approvalStatus" to "REJECTED",
                    "visibility" to "ARCHIVED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitMessageWriteConfirmation(postRef, 8_000L)
        }
    }

    // --- 6. DIRECT & GROUP MESSAGING (মেসেজিং, গ্রুপ কথোপকথন ও ছবি প্রেরণ) ---

    private fun DocumentSnapshot.toChatGroupOrNull(): ChatGroup? {
        return try {
            val obj = safeToObject(ChatGroup::class.java) ?: return null
            val members = (runCatching { get("memberIds") }.getOrNull() as? List<*>)?.mapNotNull { it as? String } ?: obj.memberIds
            val namesMap = (runCatching { get("memberNames") }.getOrNull() as? Map<*, *>)
                ?.entries
                ?.mapNotNull { (k, v) ->
                    val key = k as? String
                    val value = v as? String
                    if (key != null && value != null) key to value else null
                }
                ?.toMap() ?: obj.memberNames
            obj.copy(
                id = id,
                memberIds = members,
                memberNames = namesMap
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun observeUserGroups(userId: String): Flow<List<ChatGroup>> {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (effectiveUid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val collectionName = "chat_groups"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(collectionName).whereArrayContains("memberIds", effectiveUid),
            path = "chat_groups/$effectiveUid",
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.documents.mapNotNull { it.toChatGroupOrNull() }
                .distinctBy { it.id }
                .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun createChatGroup(
        name: String,
        photoUrl: String,
        creatorName: String,
        memberIds: List<String>,
        memberNames: Map<String, String>
    ): Result<String> {
        val uid = requireUserId()
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("অনুগ্রহ করে গ্রুপের একটি সুন্দর নাম লিখুন।"))
        }
        val finalMembers = (memberIds + uid).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val safeCreatorName = creatorName.trim().ifBlank { "সম্মানিত দ্বীনি ভাই" }
        val finalNames = memberNames.toMutableMap().apply {
            put(uid, get(uid)?.takeIf { it.isNotBlank() } ?: safeCreatorName)
        }
        val docRef = db.collection("chat_groups").document()
        return try {
            val payload = mapOf(
                "groupId" to docRef.id,
                "name" to trimmedName,
                "photoUrl" to photoUrl.trim(),
                "creatorId" to uid,
                "creatorName" to safeCreatorName,
                "memberIds" to finalMembers,
                "memberNames" to finalNames,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitWithTimeout(10_000L)
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    suspend fun updateChatGroupDetails(
        groupId: String,
        name: String,
        photoUrl: String
    ): Result<Unit> {
        requireUserId()
        if (groupId.isBlank()) return Result.failure(IllegalArgumentException("গ্রুপ খুঁজে পাওয়া যায়নি।"))
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("গ্রুপের নাম খালি রাখা যাবে না।"))
        val docRef = db.collection("chat_groups").document(groupId)
        return try {
            docRef.update(
                mapOf(
                    "name" to trimmedName,
                    "photoUrl" to photoUrl.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addMemberToChatGroup(
        group: ChatGroup,
        newMemberId: String,
        newMemberName: String
    ): Result<Unit> {
        requireUserId()
        val targetUid = newMemberId.trim()
        if (group.id.isBlank() || targetUid.isBlank()) {
            return Result.failure(IllegalArgumentException("সঠিক সদস্য নির্বাচন করুন।"))
        }
        val docRef = db.collection("chat_groups").document(group.id)
        return try {
            val updatedMembers = (group.memberIds + targetUid).distinct()
            val updatedNames = group.memberNames.toMutableMap().apply {
                put(targetUid, newMemberName.trim().ifBlank { "সম্মানিত সদস্য" })
            }
            docRef.update(
                mapOf(
                    "memberIds" to updatedMembers,
                    "memberNames" to updatedNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromChatGroup(
        group: ChatGroup,
        targetMemberId: String
    ): Result<Unit> {
        requireUserId()
        val targetUid = targetMemberId.trim()
        if (group.id.isBlank() || targetUid.isBlank()) {
            return Result.failure(IllegalArgumentException("সঠিক সদস্য নির্বাচন করুন।"))
        }
        if (targetUid == group.creatorId && group.memberIds.size <= 1) {
            return Result.failure(IllegalStateException("গ্রুপের একমাত্র সদস্যকে বাদ দেওয়া যাবে না।"))
        }
        val docRef = db.collection("chat_groups").document(group.id)
        return try {
            val updatedMembers = group.memberIds.filter { it != targetUid }.ifEmpty { listOf(group.creatorId) }
            val updatedNames = group.memberNames.toMutableMap().apply { remove(targetUid) }
            docRef.update(
                mapOf(
                    "memberIds" to updatedMembers,
                    "memberNames" to updatedNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toDirectMessageOrNull(): DirectMessage? {
        if (!exists()) return null
        return try {
            val obj = safeToObject(DirectMessage::class.java) ?: DirectMessage(id = id)
            val resolvedSenderId = runCatching { getString("senderId") }.getOrNull() ?: obj.senderId
            val resolvedSenderName = runCatching { getString("senderName") }.getOrNull() ?: obj.senderName
            val resolvedSenderPhotoUrl = runCatching { getString("senderPhotoUrl") }.getOrNull() ?: obj.senderPhotoUrl
            val resolvedReceiverId = runCatching { getString("receiverId") }.getOrNull() ?: obj.receiverId
            val resolvedReceiverName = runCatching { getString("receiverName") }.getOrNull() ?: obj.receiverName
            val resolvedSubject = runCatching { getString("subject") }.getOrNull() ?: obj.subject
            val resolvedBody = runCatching { getString("body") ?: getString("text") ?: getString("message") }.getOrNull() ?: obj.body
            val resolvedIsRead = runCatching { getBoolean("isRead") ?: getBoolean("read") }.getOrNull() ?: obj.isRead
            val resolvedSenderReaction = runCatching { getString("senderReaction") }.getOrNull() ?: obj.senderReaction
            val resolvedReceiverReaction = runCatching { getString("receiverReaction") }.getOrNull() ?: obj.receiverReaction
            val resolvedIsEdited = runCatching { getBoolean("isEdited") ?: getBoolean("edited") }.getOrNull() ?: obj.isEdited
            val resolvedEditedAt = runCatching {
                getTimestamp("editedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull() ?: runCatching {
                when (val raw = get("editedAt")) {
                    is Timestamp -> raw
                    is java.util.Date -> Timestamp(raw)
                    is Number -> {
                        val ms = raw.toLong()
                        if (ms > 0L) Timestamp(java.util.Date(ms)) else null
                    }
                    else -> null
                }
            }.getOrNull() ?: obj.editedAt
            val resolvedDeletedForSender = runCatching { getBoolean("deletedForSender") }.getOrNull() ?: obj.deletedForSender
            val resolvedDeletedForReceiver = runCatching { getBoolean("deletedForReceiver") }.getOrNull() ?: obj.deletedForReceiver
            val resolvedDeletedForEveryone = runCatching { getBoolean("isDeletedForEveryone") ?: getBoolean("deletedForEveryone") }.getOrNull() ?: obj.isDeletedForEveryone
            val resolvedGroupId = runCatching { getString("groupId") }.getOrNull() ?: obj.groupId
            val resolvedGroupName = runCatching { getString("groupName") }.getOrNull() ?: obj.groupName
            val resolvedGroupMembers = (runCatching { get("groupMemberIds") }.getOrNull() as? List<*>)?.mapNotNull { it as? String } ?: obj.groupMemberIds
            val resolvedImageUrls = (runCatching { get("imageUrls") }.getOrNull() as? List<*>)?.mapNotNull { it as? String } ?: obj.imageUrls
            val resolvedReadBy = (runCatching { get("readByUserIds") }.getOrNull() as? List<*>)?.mapNotNull { it as? String } ?: obj.readByUserIds
            val resolvedDeletedForUsers = (runCatching { get("deletedForUserIds") }.getOrNull() as? List<*>)?.mapNotNull { it as? String } ?: obj.deletedForUserIds
            val resolvedUserReactions = (runCatching { get("userReactions") }.getOrNull() as? Map<*, *>)
                ?.entries
                ?.mapNotNull { (k, v) ->
                    val key = k as? String
                    val value = v as? String
                    if (key != null && value != null) key to value else null
                }
                ?.toMap() ?: obj.userReactions
            val resolvedCreatedAt = runCatching {
                getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull() ?: runCatching {
                when (val raw = get("createdAt")) {
                    is Timestamp -> raw
                    is java.util.Date -> Timestamp(raw)
                    is Number -> {
                        val ms = raw.toLong()
                        if (ms > 0L) Timestamp(java.util.Date(ms)) else null
                    }
                    else -> null
                }
            }.getOrNull() ?: obj.createdAt ?: Timestamp.now()

            if (resolvedSenderId.isBlank() && resolvedReceiverId.isBlank() && resolvedGroupId.isBlank()) {
                return null
            }

            val hasPending = runCatching { metadata.hasPendingWrites() }.getOrDefault(false)
            if (!hasPending && id.isNotBlank()) {
                confirmedServerMessageIds.add(id)
            }
            val isServerConfirmed = !hasPending || (id.isNotBlank() && id in confirmedServerMessageIds)
            val isGroupMsg = resolvedGroupId.isNotBlank() || resolvedReceiverId.startsWith("GROUP:", ignoreCase = true)
            val isReadByRecipient = resolvedIsRead || if (isGroupMsg) {
                resolvedReadBy.any { token ->
                    token.isNotBlank() &&
                        token != resolvedSenderId &&
                        !token.startsWith("DELIVERED:", ignoreCase = true)
                }
            } else {
                resolvedReceiverId.isNotBlank() && resolvedReadBy.contains(resolvedReceiverId)
            }
            val isDeliveredToRecipient = if (isGroupMsg) {
                resolvedReadBy.any { token ->
                    token.startsWith("DELIVERED:", ignoreCase = true) &&
                        !token.equals("DELIVERED:$resolvedSenderId", ignoreCase = true)
                }
            } else {
                resolvedReceiverId.isNotBlank() && resolvedReadBy.contains("DELIVERED:$resolvedReceiverId")
            }
            val computedDeliveryState = when {
                isReadByRecipient -> "READ"
                isDeliveredToRecipient -> "DELIVERED"
                isServerConfirmed -> "SENT"
                else -> "PENDING"
            }
            obj.copy(
                id = id,
                senderId = resolvedSenderId,
                senderName = resolvedSenderName,
                senderPhotoUrl = resolvedSenderPhotoUrl,
                receiverId = resolvedReceiverId,
                receiverName = resolvedReceiverName,
                groupId = resolvedGroupId,
                groupName = resolvedGroupName,
                groupMemberIds = resolvedGroupMembers,
                subject = resolvedSubject,
                body = resolvedBody,
                imageUrls = resolvedImageUrls,
                isRead = resolvedIsRead,
                readByUserIds = resolvedReadBy,
                senderReaction = resolvedSenderReaction,
                receiverReaction = resolvedReceiverReaction,
                userReactions = resolvedUserReactions,
                isEdited = resolvedIsEdited,
                editedAt = resolvedEditedAt,
                deletedForSender = resolvedDeletedForSender,
                deletedForReceiver = resolvedDeletedForReceiver,
                deletedForUserIds = resolvedDeletedForUsers,
                isDeletedForEveryone = resolvedDeletedForEveryone,
                createdAt = resolvedCreatedAt
            ).apply {
                localDeliveryState = computedDeliveryState
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun observeUserMessages(userId: String): Flow<List<DirectMessage>> {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (effectiveUid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val collectionName = "messages"

        val sentFlow = observeQueryWithFastInitialAndFallback(
            query = db.collection(collectionName).whereEqualTo("senderId", effectiveUid),
            path = "messages/sent/$effectiveUid",
            includeMetadataChanges = true,
            initialServerTimeoutMs = 500L
        ) { snapshot ->
            snapshot.documents.mapNotNull { it.toDirectMessageOrNull() }
        }.catch { emit(emptyList()) }

        val receivedFlow = observeQueryWithFastInitialAndFallback(
            query = db.collection(collectionName).whereEqualTo("receiverId", effectiveUid),
            path = "messages/received/$effectiveUid",
            includeMetadataChanges = true,
            initialServerTimeoutMs = 500L
        ) { snapshot ->
            snapshot.documents.mapNotNull { it.toDirectMessageOrNull() }
        }.catch { emit(emptyList()) }

        val groupMessagesFlow = observeQueryWithFastInitialAndFallback(
            query = db.collection(collectionName).whereArrayContains("groupMemberIds", effectiveUid),
            path = "messages/group/$effectiveUid",
            includeMetadataChanges = true,
            initialServerTimeoutMs = 500L
        ) { snapshot ->
            snapshot.documents.mapNotNull { it.toDirectMessageOrNull() }
        }.catch { emit(emptyList()) }

        return channelFlow {
            val stateLock = Any()
            var sentList: List<DirectMessage> = emptyList()
            var receivedList: List<DirectMessage> = emptyList()
            var groupList: List<DirectMessage> = emptyList()
            var sentEmitted = false
            var receivedEmitted = false
            var groupEmitted = false
            val hasEmittedInitial = AtomicBoolean(false)

            fun emitMergedIfReady(forceEmit: Boolean = false) {
                val merged: List<DirectMessage>? = synchronized(stateLock) {
                    val hasAnyNonEmpty = sentList.isNotEmpty() || receivedList.isNotEmpty() || groupList.isNotEmpty()
                    val directQueriesReady = sentEmitted && receivedEmitted
                    val allQueriesReady = directQueriesReady && groupEmitted
                    if (forceEmit || hasAnyNonEmpty || directQueriesReady || allQueriesReady || hasEmittedInitial.get()) {
                        hasEmittedInitial.set(true)
                        // Merge received and group messages first so read/reaction state is authoritative, and deduplicate strictly by document id
                        (receivedList + groupList + sentList)
                            .filter { it.id.isNotBlank() }
                            .distinctBy { it.id }
                            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    } else {
                        null
                    }
                }
                if (merged != null) {
                    trySend(merged)
                }
            }

            launch {
                sentFlow.collect { list ->
                    synchronized(stateLock) {
                        sentList = list
                        sentEmitted = true
                    }
                    emitMergedIfReady()
                }
            }

            launch {
                receivedFlow.collect { list ->
                    synchronized(stateLock) {
                        receivedList = list
                        receivedEmitted = true
                    }
                    emitMergedIfReady()
                }
            }

            launch {
                groupMessagesFlow.collect { list ->
                    synchronized(stateLock) {
                        groupList = list
                        groupEmitted = true
                    }
                    emitMergedIfReady()
                }
            }

            // Actively fetch historical messages from server in parallel
            launch(Dispatchers.IO) {
                runCatching { syncUserMessagesFromServer(effectiveUid) }
            }

            // Bounded safety timeout so Permission Denied, Query Error, Timeout, or Empty Result never leaves UI stuck on Loading
            launch(Dispatchers.IO) {
                delay(900L)
                if (!hasEmittedInitial.get()) {
                    emitMergedIfReady(forceEmit = true)
                }
            }

            awaitClose { }
        }
    }

    fun generateMessageDocumentId(): String {
        return db.collection("messages").document().id
    }

    suspend fun sendDirectMessage(
        senderName: String,
        receiverId: String,
        receiverName: String,
        subject: String,
        body: String,
        senderPhotoUrl: String = "",
        imageUrls: List<String> = emptyList(),
        groupId: String = "",
        groupName: String = "",
        groupMemberIds: List<String> = emptyList(),
        preallocatedMessageId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val trimmedReceiverId = receiverId.trim()
        val isGroupMessage = groupId.isNotBlank() || trimmedReceiverId.startsWith("GROUP:", ignoreCase = true)
        val resolvedGroupId = if (groupId.isNotBlank()) groupId.trim() else trimmedReceiverId.substringAfter(":", trimmedReceiverId).trim()
        if (!isGroupMessage && (trimmedReceiverId.isBlank() || trimmedReceiverId == uid)) {
            return Result.failure(IllegalArgumentException("নিজের অ্যাকাউন্টে মেসেজ পাঠানো যাবে না।"))
        }
        if (imageUrls.size > ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE) {
            return Result.failure(IllegalArgumentException("একবারে সর্বোচ্চ ৫টি ছবি পাঠানো যাবে।"))
        }
        val safeSenderName = senderName.trim().ifBlank { "সম্মানিত দ্বীনি ভাই" }.take(145)
        val safeReceiverName = receiverName.trim().ifBlank {
            if (isGroupMessage) groupName.trim().ifBlank { "গ্রুপ কথোপকথন" } else "সম্মানিত প্রাপক"
        }.take(145)
        val docRef = if (preallocatedMessageId.isNotBlank()) {
            db.collection("messages").document(preallocatedMessageId.trim())
        } else {
            db.collection("messages").document()
        }
        return try {
            // Only skip set() if the message was ALREADY acknowledged by the remote server (!hasPendingWrites):
            if (preallocatedMessageId.isNotBlank()) {
                if (docRef.id in confirmedServerMessageIds) {
                    return Result.success(docRef.id)
                }
                val existingDoc = docRef.getFromCacheOnlyOrNull(25L)
                if (existingDoc != null &&
                    existingDoc.exists() &&
                    existingDoc.getString("senderId") == uid &&
                    !existingDoc.metadata.hasPendingWrites()
                ) {
                    confirmedServerMessageIds.add(docRef.id)
                    return Result.success(docRef.id)
                }
            }

            if (!isGroupMessage) {
                // Zero-latency local cache block check (no pre-flight network calls before docRef.set).
                // Server-side Firestore Security Rules also enforce !isBlockedBetween atomically.
                val senderSnap = db.collection("users").document(uid).getFromCacheOnlyOrNull(25L)
                val senderBlockedList = (senderSnap?.get("blockedUserIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                if (trimmedReceiverId in senderBlockedList) {
                    return Result.failure(
                        IllegalStateException("আপনি এই ব্যবহারকারীর সাথে পয়গাম আদান-প্রদান বিরত (ব্লক) রেখেছেন। পুনরায় পয়গাম পাঠাতে আনব্লক করুন।")
                    )
                }

                val receiverSnap = db.collection("users").document(trimmedReceiverId).getFromCacheOnlyOrNull(25L)
                val receiverBlockedList = (receiverSnap?.get("blockedUserIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                if (uid in receiverBlockedList) {
                    return Result.failure(
                        IllegalStateException("এই কথোপকথনে পয়গাম প্রেরণ বর্তমানে প্রাপক কর্তৃক সীমাবদ্ধ রয়েছে।")
                    )
                }
            }

            val requestedImageItems = imageUrls.map { it.trim() }.filter { it.isNotBlank() }
                .take(ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE)
            val uploadedImages = if (requestedImageItems.isEmpty()) {
                emptyList()
            } else {
                ProfilePhotoHelper.uploadChatImagesToFirebaseStorage(
                    imageItems = requestedImageItems,
                    senderId = uid
                )
            }
            if (requestedImageItems.isNotEmpty() && uploadedImages.size < requestedImageItems.size) {
                return Result.failure(
                    IllegalArgumentException("স্থানীয় ফাইল বা অবৈধ ছবি সরাসরি সিঙ্ক করা সম্ভব নয়। অনুগ্রহ করে গ্যালারি থেকে বৈধ ছবি নির্বাচন করে পুনরায় চেষ্টা করুন।")
                )
            }
            // Keep body strictly as the user's actual text (empty string when sending images without caption)
            // so no artificial "📷 ১ টি ছবি প্রেরণ করা হয়েছে" or "ছবি মেসেজ পাঠানো হচ্ছে" text is injected.
            val resolvedBody = body.trim()
            if (resolvedBody.isBlank() && uploadedImages.isEmpty()) {
                return Result.failure(IllegalArgumentException("অনুগ্রহ করে পয়গাম লিখুন অথবা ছবি নির্বাচন করুন।"))
            }

            // Avoid uploading inline Base64 avatar data on every chat message (profiles are already synced via users/{uid});
            // only keep lightweight preset/http URLs so text messages stay ~250 bytes and transmit instantaneously.
            val trimmedSenderPhoto = senderPhotoUrl.trim()
            val compactSenderPhoto = if (trimmedSenderPhoto.startsWith("data:image", ignoreCase = true)) {
                ""
            } else {
                ProfilePhotoHelper.compactSenderPhotoForMessage(trimmedSenderPhoto)
            }

            val payload = mutableMapOf<String, Any>(
                "senderId" to uid,
                "senderName" to safeSenderName,
                "senderPhotoUrl" to compactSenderPhoto,
                "receiverId" to if (isGroupMessage) "GROUP:$resolvedGroupId" else trimmedReceiverId,
                "receiverName" to safeReceiverName,
                "groupMemberIds" to emptyList<String>(),
                "subject" to subject.trim().ifBlank { "খেদমত সংক্রান্ত যোগাযোগ" }.take(195),
                "body" to resolvedBody.take(1450),
                "isRead" to false,
                "readByUserIds" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (uploadedImages.isNotEmpty()) {
                payload["imageUrls"] = uploadedImages
            }
            if (isGroupMessage) {
                val finalGroupMembers = (groupMemberIds + uid).map { it.trim() }.filter { it.isNotBlank() }.distinct().take(50)
                payload["groupId"] = resolvedGroupId.take(120)
                payload["groupName"] = groupName.trim().ifBlank { safeReceiverName }.take(145)
                payload["groupMemberIds"] = finalGroupMembers
            }

            val msgSizeValidation = ProfilePhotoHelper.validateFirestoreDocumentSizeBeforeWrite(
                documentPath = docRef.path,
                data = payload
            )
            if (msgSizeValidation.isFailure) {
                return Result.failure(
                    msgSizeValidation.exceptionOrNull()
                        ?: IllegalArgumentException("মেসেজের সংযুক্ত ছবির আকার নির্ধারিত সীমা অতিক্রম করেছে। অনুগ্রহ করে কম সংখ্যক বা ছোট আকারের ছবি নির্বাচন করুন।")
                )
            }

            if (preallocatedMessageId.isNotBlank()) {
                if (docRef.id in confirmedServerMessageIds) {
                    return Result.success(docRef.id)
                }
                val existingSnap = docRef.getCacheFirstOrNull(1_500L)
                if (existingSnap != null &&
                    existingSnap.exists() &&
                    !existingSnap.metadata.hasPendingWrites() &&
                    existingSnap.getString("senderId") == uid
                ) {
                    confirmedServerMessageIds.add(docRef.id)
                    FirestoreConnectionMonitor.onSnapshotReceived(
                        path = "messages",
                        isFromCache = false,
                        hasPendingWrites = false
                    )
                    return Result.success(docRef.id)
                }
            }

            runCatching { db.enableNetwork() }
            val writeTask = docRef.set(payload)
            writeTask.addOnCompleteListener { completedTask ->
                if (completedTask.isSuccessful) {
                    confirmedServerMessageIds.add(docRef.id)
                    FirestoreConnectionMonitor.onSnapshotReceived(
                        path = "messages",
                        isFromCache = false,
                        hasPendingWrites = false
                    )
                } else {
                    completedTask.exception?.let { err ->
                        handleFirestoreError(err, OperationType.CREATE, docRef.path)
                    }
                }
            }

            val writeTimeoutMs = if (FirestoreConnectionMonitor.isNetworkAvailable.value) 12_000L else 3_000L
            try {
                kotlinx.coroutines.withTimeout(writeTimeoutMs) {
                    writeTask.await()
                }
                confirmedServerMessageIds.add(docRef.id)
            } catch (timeoutEx: kotlinx.coroutines.TimeoutCancellationException) {
                val taskError = writeTask.exception
                if (taskError != null) {
                    handleFirestoreError(taskError, OperationType.CREATE, docRef.path)
                    return Result.failure(translateFirestoreExceptionToBangla(taskError))
                }
                val serverConfirmed = runCatching {
                    val snap = withTimeoutOrNull(1_500L) { docRef.get(Source.SERVER).await() }
                    snap != null && snap.exists() && !snap.metadata.hasPendingWrites() && snap.getString("senderId") == uid
                }.getOrDefault(false)
                if (!serverConfirmed) {
                    runCatching { db.enableNetwork() }
                    handleFirestoreError(timeoutEx, OperationType.CREATE, docRef.path)
                    return Result.failure(
                        IllegalStateException("সার্ভারে মেসেজ সংরক্ষণে নির্ধারিত সময়ের বেশি লাগছে। 'পুনরায় পাঠান' বাটনে চাপ দিন।")
                    )
                }
                confirmedServerMessageIds.add(docRef.id)
            }

            // Note: Paygam/Direct messages do NOT create documents in the `notifications` collection.
            // Bell icon notifications are strictly reserved for general notifications (NEW_CIRCULAR, APPLICATION, ANNOUNCEMENT, etc.).
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (preallocatedMessageId.isNotBlank()) {
                val alreadyOnServer = runCatching {
                    val snap = withTimeoutOrNull(2_000L) { docRef.get(Source.SERVER).await() }
                    snap != null && snap.exists() && !snap.metadata.hasPendingWrites() && snap.getString("senderId") == uid
                }.getOrDefault(false)
                if (alreadyOnServer) {
                    confirmedServerMessageIds.add(docRef.id)
                    return Result.success(docRef.id)
                }
            }
            if (!isGroupMessage) {
                val msg = e.message.orEmpty()
                if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
                    msg.contains("Missing or insufficient permissions", ignoreCase = true)
                ) {
                    val senderSnap = runCatching { db.collection("users").document(uid).getCacheFirstOrNull(1_000L) }.getOrNull()
                    val senderBlocked = (senderSnap?.get("blockedUserIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                    if (receiverId in senderBlocked) {
                        return Result.failure(
                            IllegalStateException("আপনি এই ব্যবহারকারীর সাথে পয়গাম আদান-প্রদান বিরত (ব্লক) রেখেছেন। পুনরায় পয়গাম পাঠাতে আনব্লক করুন।")
                        )
                    }
                    val receiverSnap = runCatching { db.collection("users").document(receiverId).getCacheFirstOrNull(1_000L) }.getOrNull()
                    val receiverBlocked = (receiverSnap?.get("blockedUserIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                    if (uid in receiverBlocked) {
                        return Result.failure(
                            IllegalStateException("এই কথোপকথনে পয়গাম প্রেরণ বর্তমানে প্রাপক কর্তৃক সীমাবদ্ধ রয়েছে।")
                        )
                    }
                }
            }
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    /**
     * Marks incoming unread messages (1-on-1 and Group) as delivered (`DELIVERED:<uid>` in `readByUserIds`
     * while keeping `isRead = false`) so the sender's real-time snapshot listener immediately transitions
     * from Sent (`✓`) to Delivered (`✓✓`) when the recipient's device receives the message.
     */
    suspend fun acknowledgeMessagesDelivered(messages: List<DirectMessage>): Result<Unit> {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return Result.success(Unit)
        val deliveredToken = "DELIVERED:$uid"
        val pendingDelivery = messages.filter { msg ->
            val isGroupMsg = msg.groupId.isNotBlank() || msg.receiverId.startsWith("GROUP:", ignoreCase = true)
            val isEligibleRecipient = if (isGroupMsg) {
                uid in msg.groupMemberIds
            } else {
                msg.receiverId == uid
            }
            msg.id.isNotBlank() &&
                msg.senderId != uid &&
                isEligibleRecipient &&
                !msg.isRead &&
                !msg.readByUserIds.contains(uid) &&
                !msg.readByUserIds.contains(deliveredToken) &&
                msg.readByUserIds.size < 48
        }.take(20)
        if (pendingDelivery.isEmpty()) return Result.success(Unit)
        return try {
            val batch = db.batch()
            for (msg in pendingDelivery) {
                batch.update(
                    db.collection("messages").document(msg.id),
                    mapOf("readByUserIds" to FieldValue.arrayUnion(deliveredToken))
                )
            }
            batch.commit().awaitWithTimeout(4_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        val uid = requireUserId()
        if (messageId.isBlank()) return Result.success(Unit)
        val docRef = db.collection("messages").document(messageId)
        return try {
            val snap = docRef.getCacheFirstOrNull(2_000L) ?: docRef.get().awaitWithTimeout(4_000L)
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("মেসেজটি সার্ভারে পাওয়া যায়নি।"))
            }
            val senderId = snap.getString("senderId").orEmpty()
            val receiverId = snap.getString("receiverId").orEmpty()
            val groupMembers = (snap.get("groupMemberIds") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
            val isGroupMsg = !snap.getString("groupId").isNullOrBlank() || receiverId.startsWith("GROUP:", ignoreCase = true)
            val isEligibleRecipient = senderId != uid && (if (isGroupMsg) uid in groupMembers else receiverId == uid)
            if (!isEligibleRecipient) {
                return Result.failure(
                    IllegalAccessException("শুধু প্রাপক নিজেই নিজের মেসেজের Delivery বা Read স্ট্যাটাস আপডেট করতে পারবেন।")
                )
            }
            if (isGroupMsg) {
                docRef.update(
                    mapOf("readByUserIds" to FieldValue.arrayUnion(uid))
                ).awaitWithTimeout(6_000L)
            } else {
                docRef.update(
                    mapOf(
                        "isRead" to true,
                        "readByUserIds" to FieldValue.arrayUnion(uid)
                    )
                ).awaitWithTimeout(6_000L)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    suspend fun markConversationMessagesAsRead(otherUserId: String, messages: List<DirectMessage>): Result<Unit> {
        val uid = requireUserId()
        if (otherUserId.isBlank() || otherUserId == uid) return Result.success(Unit)

        if (otherUserId.startsWith("GROUP:", ignoreCase = true)) {
            val groupId = otherUserId.substringAfter(":")
            val unreadGroupMsgs = messages.filter { msg ->
                val matchesGroup = msg.groupId == groupId || msg.receiverId.equals(otherUserId, ignoreCase = true)
                matchesGroup && msg.senderId != uid && !msg.readByUserIds.contains(uid) && msg.id.isNotBlank()
            }
            if (unreadGroupMsgs.isEmpty()) return Result.success(Unit)
            return try {
                val batch = db.batch()
                for (msg in unreadGroupMsgs) {
                    val updatedReadBy = (msg.readByUserIds + uid).distinct().take(50)
                    batch.update(
                        db.collection("messages").document(msg.id),
                        mapOf("readByUserIds" to updatedReadBy)
                    )
                }
                batch.commit().awaitWithTimeout(8_000L)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "messages")
                Result.failure(e)
            }
        }

        val unreadMap = mutableMapOf<String, List<String>>()
        messages
            .filter { it.senderId == otherUserId && it.receiverId == uid && (!it.isRead || !it.readByUserIds.contains(uid)) && it.id.isNotBlank() }
            .forEach { unreadMap[it.id] = it.readByUserIds }

        return try {
            // Only query Firestore if the caller passed an empty in-memory messages list (before messagesState emitted),
            // checking local cache first to avoid unnecessary full-collection network reads.
            if (unreadMap.isEmpty() && messages.isEmpty()) {
                val cachedSnap = kotlinx.coroutines.withTimeoutOrNull(150L) {
                    runCatching {
                        db.collection("messages")
                            .whereEqualTo("receiverId", uid)
                            .get(Source.CACHE)
                            .await()
                    }.getOrNull()
                }
                val receivedSnap = if (cachedSnap != null && !cachedSnap.isEmpty) {
                    cachedSnap
                } else {
                    kotlinx.coroutines.withTimeoutOrNull(2_500L) {
                        db.collection("messages")
                            .whereEqualTo("receiverId", uid)
                            .get()
                            .await()
                    }
                }
                for (doc in receivedSnap?.documents.orEmpty()) {
                    val docSender = doc.getString("senderId").orEmpty()
                    val docRead = doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: false
                    val docReadBy = (doc.get("readByUserIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                    if (docSender == otherUserId && (!docRead || !docReadBy.contains(uid)) && doc.id.isNotBlank()) {
                        unreadMap[doc.id] = docReadBy
                    }
                }
            }

            if (unreadMap.isNotEmpty()) {
                val batch = db.batch()
                for ((msgId, existingReadBy) in unreadMap) {
                    val updatedReadBy = (existingReadBy + uid).distinct().take(50)
                    batch.update(
                        db.collection("messages").document(msgId),
                        mapOf(
                            "isRead" to true,
                            "readByUserIds" to updatedReadBy
                        )
                    )
                }
                batch.commit().awaitWithTimeout(8_000L)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "messages")
            Result.failure(e)
        }
    }

    /**
     * Adds, updates, or toggles off a reaction (👍, ❤️, 😂, 😮, 😢, 🙏, 🤲, 🌙, ✨, 🕌)
     * on a Paygam message. Real-time visible across 1-on-1 and group chats via `userReactions`.
     */
    suspend fun reactToMessage(
        message: DirectMessage,
        reactionCode: String
    ): Result<Unit> {
        val uid = requireUserId()
        if (message.id.isBlank()) return Result.failure(IllegalArgumentException("পয়গাম খুঁজে পাওয়া যায়নি।"))
        if (message.isDeletedForEveryone) {
            return Result.failure(IllegalStateException("প্রত্যাহারকৃত পয়গামে অভিবাদন যুক্ত করা যাবে না।"))
        }
        val docRef = db.collection("messages").document(message.id)
        return try {
            val mergedMap = message.mergedUserReactions().toMutableMap()
            val currentReaction = mergedMap[uid].orEmpty()
            val isSameReaction = currentReaction.isNotBlank() && (
                currentReaction.equals(reactionCode, ignoreCase = true) ||
                    com.example.data.model.IslamicReaction.resolveEmoji(currentReaction) ==
                    com.example.data.model.IslamicReaction.resolveEmoji(reactionCode)
                )
            val nextReaction = if (isSameReaction) "" else reactionCode.trim()
            if (nextReaction.isBlank()) {
                mergedMap.remove(uid)
            } else {
                mergedMap[uid] = nextReaction
            }
            val updates = mutableMapOf<String, Any>("userReactions" to mergedMap.entries.take(50).associate { it.key to it.value })
            if (uid == message.senderId && message.senderReaction.isNotBlank()) {
                updates["senderReaction"] = ""
            } else if (uid != message.senderId && uid == message.receiverId && message.receiverReaction.isNotBlank()) {
                updates["receiverReaction"] = ""
            }
            docRef.update(updates).awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Edits the body of a Paygam message. Restricted strictly to the original sender.
     * Both users see the updated message body and the 'সম্পাদিত' indicator in real time.
     */
    suspend fun editDirectMessage(
        message: DirectMessage,
        newBody: String
    ): Result<Unit> {
        val uid = requireUserId()
        if (message.id.isBlank()) return Result.failure(IllegalArgumentException("পয়গাম খুঁজে পাওয়া যায়নি।"))
        if (uid != message.senderId) {
            return Result.failure(IllegalStateException("শুধুমাত্র পয়গাম প্রেরক নিজের পয়গাম সম্পাদনা করতে পারবেন।"))
        }
        if (message.isDeletedForEveryone) {
            return Result.failure(IllegalStateException("প্রত্যাহারকৃত পয়গাম সম্পাদনা করা যাবে না।"))
        }
        val trimmed = newBody.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("পয়গামের মূল বক্তব্য খালি রাখা যাবে না।"))
        }
        val docRef = db.collection("messages").document(message.id)
        return try {
            docRef.update(
                mapOf(
                    "body" to trimmed,
                    "isEdited" to true,
                    "editedAt" to FieldValue.serverTimestamp()
                )
            ).awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Deletes/hides a Paygam message only for the current user (নিজের জন্য মুছুন).
     * Both sender, receiver, and group members can delete a message from their own view.
     */
    suspend fun deleteMessageForMe(message: DirectMessage): Result<Unit> {
        val uid = requireUserId()
        if (message.id.isBlank()) return Result.failure(IllegalArgumentException("পয়গাম খুঁজে পাওয়া যায়নি।"))
        val docRef = db.collection("messages").document(message.id)
        val updatedDeletedList = (message.deletedForUserIds + uid).distinct().take(50)
        return try {
            when {
                uid == message.senderId -> docRef.update(
                    mapOf(
                        "deletedForSender" to true,
                        "deletedForUserIds" to updatedDeletedList
                    )
                ).awaitWithTimeout(8_000L)
                uid == message.receiverId -> docRef.update(
                    mapOf(
                        "deletedForReceiver" to true,
                        "deletedForUserIds" to updatedDeletedList
                    )
                ).awaitWithTimeout(8_000L)
                message.groupMemberIds.contains(uid) || message.groupId.isNotBlank() -> {
                    docRef.update("deletedForUserIds", updatedDeletedList).awaitWithTimeout(8_000L)
                }
                else -> return Result.failure(IllegalStateException("অনুমোদনহীন অনুরোধ।"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Recalls/deletes a Paygam message for everyone in the conversation (সবার জন্য মুছুন).
     * Strictly permitted only for the original sender (`uid == message.senderId`).
     */
    suspend fun deleteMessageForEveryone(message: DirectMessage): Result<Unit> {
        val uid = requireUserId()
        if (message.id.isBlank()) return Result.failure(IllegalArgumentException("পয়গাম খুঁজে পাওয়া যায়নি।"))
        if (uid != message.senderId) {
            return Result.failure(IllegalStateException("শুধুমাত্র পয়গাম প্রেরক সবার জন্য পয়গাম মুছতে পারবেন।"))
        }
        val docRef = db.collection("messages").document(message.id)
        return try {
            docRef.update(
                mapOf(
                    "body" to "এই পয়গামটি প্রত্যাহার করা হয়েছে",
                    "imageUrls" to emptyList<String>(),
                    "isDeletedForEveryone" to true,
                    "senderReaction" to "",
                    "userReactions" to emptyMap<String, String>()
                )
            ).awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Blocks or unblocks a conversation partner in Firestore.
     * When blocked, message exchange between the two users is halted until unblocked.
     */
    suspend fun setBlockedUserStatus(
        targetUserId: String,
        block: Boolean,
        defaultFullName: String = "সম্মানিত দ্বীনি ভাই"
    ): Result<Boolean> {
        val uid = requireUserId()
        if (targetUserId.isBlank() || targetUserId == uid) {
            return Result.failure(IllegalArgumentException("সঠিক ব্যবহারকারী নির্বাচন করুন।"))
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.getFastOrNull(3_500L)
            if (snap == null || !snap.exists()) {
                val initialBlocked = if (block) listOf(targetUserId) else emptyList()
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to defaultFullName.trim().ifBlank { "সম্মানিত দ্বীনি ভাই" },
                    "accountType" to "khadim",
                    "district" to "",
                    "designation" to "",
                    "qualification" to "",
                    "experience" to "",
                    "preferredDistricts" to "",
                    "bio" to "",
                    "contactInfo" to "",
                    "madrasaName" to "",
                    "photoUrl" to "",
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "typingToUserId" to "",
                    "blockedUserIds" to initialBlocked,
                    "isVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitWithTimeout(8_000L)
            } else {
                val currentBlocked = (snap.get("blockedUserIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                val updatedBlocked = if (block) {
                    (currentBlocked + targetUserId).distinct()
                } else {
                    currentBlocked.filter { it != targetUserId }
                }
                docRef.update(
                    mapOf(
                        "blockedUserIds" to updatedBlocked,
                        "typingToUserId" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitWithTimeout(8_000L)
            }
            Result.success(block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Saves the selected preset or custom theme for a conversation partner in Firestore.
     */
    suspend fun updateConversationTheme(
        partnerUserId: String,
        themeSpec: String,
        defaultFullName: String = "সম্মানিত দ্বীনি ভাই"
    ): Result<String> {
        val uid = requireUserId()
        if (partnerUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("সঠিক কথোপকথন নির্বাচন করুন।"))
        }
        val safeTheme = themeSpec.trim().ifBlank { "EMERALD_CLASSIC" }
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.getFastOrNull(3_500L)
            if (snap == null || !snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to defaultFullName.trim().ifBlank { "সম্মানিত দ্বীনি ভাই" },
                    "accountType" to "khadim",
                    "district" to "",
                    "designation" to "",
                    "qualification" to "",
                    "experience" to "",
                    "preferredDistricts" to "",
                    "bio" to "",
                    "contactInfo" to "",
                    "madrasaName" to "",
                    "photoUrl" to "",
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "typingToUserId" to "",
                    "chatThemes" to mapOf(partnerUserId to safeTheme),
                    "isVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitWithTimeout(8_000L)
            } else {
                val existingThemes = (snap.get("chatThemes") as? Map<*, *>)
                    ?.entries
                    ?.mapNotNull { (k, v) ->
                        val key = k as? String
                        val value = v as? String
                        if (key != null && value != null) key to value else null
                    }
                    ?.toMap()
                    .orEmpty()
                    .toMutableMap()
                existingThemes[partnerUserId] = safeTheme
                docRef.update(
                    mapOf(
                        "chatThemes" to existingThemes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitWithTimeout(8_000L)
            }
            Result.success(safeTheme)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 7. NOTIFICATIONS (ব্যক্তিগত ও কমিউনিটি নোটিফিকেশন, অপঠিত ব্যাজ ও অ্যাডমিন ঘোষণা) ---

    fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (effectiveUid.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val notifCollection = "notifications"
        val readsPath = "users/$effectiveUid/notification_reads"

        val directFlow = observeQueryWithFastInitialAndFallback(
            query = db.collection(notifCollection).whereEqualTo("recipientId", effectiveUid),
            path = "notifications/direct/$effectiveUid",
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.safeToObjects(AppNotification::class.java)
        }

        val broadcastFlow = observeQueryWithFastInitialAndFallback(
            query = db.collection(notifCollection).whereEqualTo("recipientId", "ALL_USERS"),
            path = "notifications/broadcast",
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.safeToObjects(AppNotification::class.java)
        }

        val readReceiptsFlow = observeQueryWithFastInitialAndFallback(
            query = db.collection("users").document(effectiveUid).collection("notification_reads"),
            path = readsPath,
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.safeToObjects(NotificationReadReceipt::class.java)
                .map { it.notificationId.ifBlank { it.id } }
        }
            .map { it.toSet() }

        return combine(directFlow, broadcastFlow, readReceiptsFlow) { directList, broadcastList, readIds ->
            (directList + broadcastList)
                .distinctBy { it.id }
                // Never show self-generated notifications or Paygam chat messages in the Bell notification list
                .filter { it.senderId != effectiveUid && !it.type.equals("MESSAGE", ignoreCase = true) }
                .map { notif ->
                    val resolvedRead = notif.isRead || readIds.contains(notif.id)
                    notif.copy(isRead = resolvedRead)
                }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun createNotification(
        recipientId: String,
        senderName: String,
        title: String,
        message: String,
        type: String,
        referenceId: String
    ): Result<String> {
        val uid = requireUserId()
        // Never create a direct notification addressed to oneself
        if (recipientId == uid) {
            return Result.success("")
        }
        val docRef = db.collection("notifications").document()
        return try {
            val payload = mapOf(
                "recipientId" to recipientId,
                "senderId" to uid,
                "senderName" to senderName.trim().ifBlank { "কওমি কর্মসংস্থান" },
                "title" to title.trim(),
                "message" to message.trim(),
                "type" to type,
                "referenceId" to referenceId,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitWithTimeout(8_000L)
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendAdminAnnouncement(
        adminName: String,
        title: String,
        message: String
    ): Result<String> {
        requireUserId()
        val resolvedAdmin = adminName.ifBlank { "নাজিমে আলা (অ্যাডমিন)" }
        val notifResult = createNotification(
            recipientId = "ALL_USERS",
            senderName = resolvedAdmin,
            title = title.trim(),
            message = message.trim(),
            type = "ANNOUNCEMENT",
            referenceId = "admin_announcement"
        )
        notifResult.onSuccess { notifId ->
            enqueueAndDispatchTelegramNotification(
                eventType = "ADMIN_ANNOUNCEMENT",
                title = title.trim(),
                message = message.trim(),
                actorName = resolvedAdmin,
                referenceId = notifId.ifBlank { "admin_announcement" }
            )
        }
        return notifResult
    }

    suspend fun markNotificationAsRead(notification: AppNotification): Result<Unit> {
        val uid = requireUserId()
        if (notification.id.isBlank()) return Result.success(Unit)
        val readDocRef = db.collection("users")
            .document(uid)
            .collection("notification_reads")
            .document(notification.id)
        return try {
            val readPayload = mapOf(
                "userId" to uid,
                "notificationId" to notification.id,
                "readAt" to FieldValue.serverTimestamp()
            )
            readDocRef.set(readPayload).awaitWithTimeout(8_000L)

            if (notification.recipientId == uid) {
                db.collection("notifications")
                    .document(notification.id)
                    .update("isRead", true)
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, readDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(notifications: List<AppNotification>): Result<Unit> {
        val uid = requireUserId()
        val unreadList = notifications.filter { !it.isRead && it.id.isNotBlank() }
        if (unreadList.isEmpty()) return Result.success(Unit)

        return try {
            val batch = db.batch()
            for (notif in unreadList) {
                val readRef = db.collection("users")
                    .document(uid)
                    .collection("notification_reads")
                    .document(notif.id)
                batch.set(
                    readRef,
                    mapOf(
                        "userId" to uid,
                        "notificationId" to notif.id,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                )
                if (notif.recipientId == uid) {
                    val notifRef = db.collection("notifications").document(notif.id)
                    batch.update(notifRef, "isRead", true)
                }
            }
            batch.commit().awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/notification_reads")
            Result.failure(e)
        }
    }

    // --- 8. TELEGRAM NOTIFICATION SYSTEM (টেলিগ্রাম নোটিফিকেশন সিস্টেম ও অ্যাডমিন অডিট লগ) ---

    fun observeTelegramNotifications(): Flow<List<TelegramNotificationLog>> {
        val path = "telegram_notifications"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(path),
            path = path,
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.safeToObjects(TelegramNotificationLog::class.java)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun enqueueAndDispatchTelegramNotification(
        eventType: String,
        title: String,
        message: String,
        actorName: String,
        referenceId: String,
        customTargetPhone: String = AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
        requireTelegramDeliverySuccess: Boolean = false,
        bypassDedup: Boolean = false
    ): Result<String> {
        val currentUser = runCatching { auth.currentUser }.getOrNull()
        val uid = currentUser?.uid
        val effectiveEmail = AdminSecurityHelper.resolveEffectiveUserEmail(currentUser)
        val adminEmailSuffix = effectiveEmail.takeIf { it.isNotBlank() }?.let { email ->
            if (actorName.contains(email, ignoreCase = true)) "" else " ($email)"
        }.orEmpty()

        val resolvedActor = TelegramNotificationHelper.sanitizeSensitiveContent(
            (actorName.ifBlank { "কওমি কর্মসংস্থান ব্যবহারকারী/অ্যাডমিন" } + adminEmailSuffix).trim()
        ).take(200)
        val resolvedTitle = TelegramNotificationHelper.sanitizeSensitiveContent(title)
            .ifBlank { "কওমি কর্মসংস্থান ইত্তেলা" }
            .take(250)
        val resolvedMessage = TelegramNotificationHelper.sanitizeSensitiveContent(message)
            .ifBlank { resolvedTitle }
            .take(1700)
        val resolvedTargetPhone = TelegramNotificationHelper.sanitizeSensitiveContent(customTargetPhone)
            .ifBlank { AdminSecurityHelper.ADMIN_MOBILE_NUMBER }
            .take(30)

        val eventKey = "${eventType}_${referenceId}_${resolvedTitle.hashCode()}_${resolvedMessage.hashCode()}"

        // Verify bot via getMe and dispatch to Telegram Bot API (Chat ID: 6255562134), checking real "ok": true + message_id
        val dispatchDiag = if (requireTelegramDeliverySuccess) {
            TelegramNotificationHelper.dispatchToTelegramBotVerified(
                eventType = eventType,
                title = resolvedTitle,
                message = resolvedMessage,
                actorName = resolvedActor,
                targetPhone = resolvedTargetPhone,
                eventKey = eventKey,
                bypassDedup = bypassDedup
            )
        } else {
            kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                TelegramNotificationHelper.dispatchToTelegramBotVerified(
                    eventType = eventType,
                    title = resolvedTitle,
                    message = resolvedMessage,
                    actorName = resolvedActor,
                    targetPhone = resolvedTargetPhone,
                    eventKey = eventKey,
                    bypassDedup = bypassDedup
                )
            } ?: TelegramNotificationHelper.TelegramBotDiagnosticResult(
                isTokenConfigured = TelegramNotificationHelper.isTelegramBotConfigured(),
                isChatIdValid = true,
                getMeVerified = false,
                sendMessageOk = false,
                deliveredMessageId = null,
                sanitizedErrorDescription = "Timeout",
                deliveryStatus = "FAILED",
                diagnosticMessageBangla = "টেলিগ্রাম সংযোগ সময়সীমা অতিক্রম করেছে; অ্যাডমিন লগে সংরক্ষিত হয়েছে।"
            )
        }

        val delivered = dispatchDiag.sendMessageOk && (dispatchDiag.deliveredMessageId ?: 0L) > 0L
        val trueStatus = if (delivered) "DELIVERED" else "FAILED"
        val resolvedRefId = if (delivered && dispatchDiag.deliveredMessageId != null) {
            "${referenceId.take(100)}#msg_${dispatchDiag.deliveredMessageId}"
        } else {
            referenceId.take(128)
        }
        val persistedMessage = if (delivered) {
            resolvedMessage
        } else {
            val safeReason = TelegramNotificationHelper.sanitizeSensitiveContent(
                dispatchDiag.sanitizedErrorDescription.ifBlank { dispatchDiag.diagnosticMessageBangla }
            ).take(220)
            "$resolvedMessage [টেলিগ্রাম ডেলিভারি ব্যর্থ: $safeReason]".take(2000)
        }

        if (uid == null) {
            return if (delivered) {
                Result.success(dispatchDiag.diagnosticMessageBangla)
            } else {
                Result.failure(IllegalStateException(dispatchDiag.diagnosticMessageBangla.ifBlank { "Unauthenticated" }))
            }
        }

        val docRef = db.collection("telegram_notifications").document()
        return try {
            val payload = mapOf(
                "eventType" to eventType,
                "title" to resolvedTitle,
                "message" to persistedMessage,
                "actorId" to uid,
                "actorName" to resolvedActor,
                "targetPhone" to resolvedTargetPhone,
                "referenceId" to resolvedRefId,
                "deliveryStatus" to trueStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitMessageWriteConfirmation(docRef, 3_500L)
            if (requireTelegramDeliverySuccess && !delivered) {
                Result.failure(IllegalStateException(dispatchDiag.diagnosticMessageBangla))
            } else if (delivered) {
                Result.success(dispatchDiag.diagnosticMessageBangla)
            } else {
                // Firestore log saved with FAILED status, but Telegram API did not return ok:true
                Result.success("FIRESTORE_SAVED_TELEGRAM_FAILED::${dispatchDiag.diagnosticMessageBangla}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            if (delivered) {
                Result.success(dispatchDiag.diagnosticMessageBangla)
            } else {
                Result.failure(
                    IllegalStateException(
                        dispatchDiag.diagnosticMessageBangla.ifBlank {
                            e.localizedMessage ?: "টেলিগ্রাম ও সার্ভার সংযোগে ত্রুটি হয়েছে।"
                        }
                    )
                )
            }
        }
    }

    /**
     * Saves a user's support request, complaint, technical issue, or suggestion to Firebase Firestore
     * so it is visible in the Admin Panel AND attempts verified dispatch to the existing Telegram Bot.
     * Never claims Telegram delivery succeeded unless Telegram API returned `"ok": true`.
     */
    suspend fun submitSupportOrComplaintMessage(
        category: String,
        subject: String,
        message: String,
        contactPhone: String,
        senderName: String
    ): Result<String> {
        val cleanCategory = category.trim().ifBlank { "সহায়তা ও যোগাযোগ" }
        val cleanSubject = subject.trim().ifBlank { "ব্যবহারকারীর বার্তা" }
        val cleanBody = message.trim()
        if (cleanBody.isBlank()) {
            return Result.failure(IllegalArgumentException("অনুগ্রহ করে আপনার বার্তা বা অভিযোগের বিস্তারিত লিখুন।"))
        }
        val cleanPhone = contactPhone.trim().ifBlank { AdminSecurityHelper.ADMIN_MOBILE_NUMBER }
        val cleanSender = senderName.trim().ifBlank {
            auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "ব্যবহারকারী"
        }

        val formattedTitle = "[$cleanCategory] $cleanSubject"
        val formattedMessage = "প্রেরক: $cleanSender | যোগাযোগ: ${contactPhone.trim().ifBlank { "উল্লেখ নেই" }} | বিস্তারিত: $cleanBody"

        return enqueueAndDispatchTelegramNotification(
            eventType = "KHEDMOT_SUBMISSION",
            title = formattedTitle,
            message = formattedMessage,
            actorName = cleanSender,
            referenceId = "SUPPORT_TICKET:$cleanCategory",
            customTargetPhone = cleanPhone,
            requireTelegramDeliverySuccess = false,
            bypassDedup = false
        )
    }

    /**
     * Sends a live test alert to the configured Telegram Bot and Chat ID (`6255562134`).
     * Strictly requires Telegram Bot API (`getMe` + `sendMessage`) to return `"ok": true`
     * and a valid `message_id` — otherwise returns `Result.failure` with the exact diagnostic reason.
     */
    suspend fun sendAdminTestTelegramNotification(
        adminName: String,
        customAlertText: String = ""
    ): Result<String> {
        val msg = customAlertText.trim().ifBlank {
            "আলহামদুলিল্লাহ, অ্যাপের ভেতরে স্থায়ীভাবে সংযুক্ত টেলিগ্রাম বট ও চ্যাট আইডি (${TelegramNotificationHelper.resolveTargetChatId()})-তে অটো নোটিফিকেশন সিস্টেম সক্রিয় রয়েছে।"
        }
        return enqueueAndDispatchTelegramNotification(
            eventType = "ADMIN_SECURITY",
            title = "অ্যাডমিন টেলিগ্রাম অটো-বট সিস্টেম যাচাই",
            message = msg,
            actorName = adminName.ifBlank { "নাজিমে আলা (অ্যাডমিন)" },
            referenceId = "telegram_test_alert",
            requireTelegramDeliverySuccess = true,
            bypassDedup = true
        )
    }

    suspend fun verifyAdminTelegramBotConnection(): TelegramNotificationHelper.TelegramBotDiagnosticResult {
        return TelegramNotificationHelper.verifyBotConnectionWithGetMe()
    }

    // --- 9. ADMIN PANEL MANAGEMENT (ব্যবহারকারী, আবেদন, সেটিংস ও লাইভ টেলিগ্রাম ইত্তেলা) ---

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> {
        val path = "users"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(path),
            path = "users/admin_all",
            includeMetadataChanges = true,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.documents.mapNotNull { it.toUserProfileOrNull() }
                .distinctBy { it.id.ifBlank { it.userId } }
                .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observeAllSubmissionsForAdmin(): Flow<List<KhedmotSubmission>> {
        val path = "khedmot_submissions"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(path),
            path = "khedmot_submissions/admin_all",
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.documents.mapNotNull { it.toKhedmotSubmissionOrNull() }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observeAllAdminsForAdmin(): Flow<List<AdminRegistryEntry>> {
        val path = "admins"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(path),
            path = path,
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.safeToObjects(AdminRegistryEntry::class.java)
                .distinctBy { it.id.ifBlank { it.adminId } }
                .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun adminUpdateUserVerification(
        user: UserProfile,
        verified: Boolean,
        adminName: String
    ): Result<Unit> {
        requireUserId()
        val targetId = user.id.ifBlank { user.userId }
        if (targetId.isBlank()) {
            return Result.failure(IllegalArgumentException("ব্যবহারকারীর আইডি পাওয়া যায়নি।"))
        }
        val docRef = db.collection("users").document(targetId)
        return try {
            docRef.update(
                mapOf(
                    "isVerified" to verified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitMessageWriteConfirmation(docRef, 12_000L)

            val resolvedAdmin = adminName.ifBlank { "নাজিমে আলা (অ্যাডমিন)" }
            val eventType = if (verified) "ADMIN_VERIFICATION" else "CONTENT_MODERATION"
            val title = if (verified) {
                "ব্যবহারকারী সক্রিয় ও অনুমোদন করা হয়েছে"
            } else {
                "ব্যবহারকারী ব্লক / নিষ্ক্রিয় করা হয়েছে"
            }
            val message = if (verified) {
                "ব্যবহারকারী '${user.fullName.ifBlank { targetId }}'-কে সক্রিয় (যাচাইকৃত) করা হয়েছে।"
            } else {
                "ব্যবহারকারী '${user.fullName.ifBlank { targetId }}'-এর অ্যাকাউন্ট নিষ্ক্রিয় / ব্লক করা হয়েছে।"
            }
            kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                runCatching {
                    enqueueAndDispatchTelegramNotification(
                        eventType = eventType,
                        title = title,
                        message = message,
                        actorName = resolvedAdmin,
                        referenceId = targetId
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    suspend fun adminUpdateSubmissionStatus(
        submission: KhedmotSubmission,
        newStatus: String,
        adminName: String
    ): Result<Unit> {
        val resolvedAdmin = adminName.ifBlank { "নাজিমে আলা (অ্যাডমিন)" }
        val res = updateSubmissionStatus(
            submission = submission,
            newStatus = newStatus,
            actorName = resolvedAdmin,
            publishToFeedOnAccept = true
        )
        res.onSuccess {
            val statusBangla = when (newStatus) {
                "ACCEPTED" -> "নির্বাচিত (অনুমোদিত)"
                "SHORTLISTED" -> "পর্যালোচনাধীন (প্রাথমিক নির্বাচিত)"
                "REJECTED" -> "বাতিল"
                else -> "নতুন"
            }
            kotlinx.coroutines.withTimeoutOrNull(1_500L) {
                runCatching {
                    enqueueAndDispatchTelegramNotification(
                        eventType = "KHEDMOT_SUBMISSION",
                        title = if (newStatus == "ACCEPTED") "আবেদন অনুমোদন করা হয়েছে" else "আবেদনের অবস্থা পরিবর্তন: $statusBangla",
                        message = "প্রার্থী: ${submission.applicantName} | পদ: ${submission.postDesignation} | বিজ্ঞপ্তি: ${submission.postTitle} | বর্তমান অবস্থা: $statusBangla",
                        actorName = resolvedAdmin,
                        referenceId = submission.id.ifBlank { "submission_update" }
                    )
                }
            }
        }
        return res
    }

    suspend fun adminSaveAdminMemberEntry(
        memberId: String,
        name: String,
        role: String,
        isActive: Boolean,
        actorAdminName: String
    ): Result<String> {
        requireUserId()
        val currentUser = runCatching { auth.currentUser }.getOrNull()
        val resolvedEmail = AdminSecurityHelper.resolveEffectiveUserEmail(currentUser)
            .takeIf { AdminSecurityHelper.isAuthorizedAdminEmail(it) }
            ?: "fyslahm28@gmail.com"
        val cleanId = memberId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank {
            "adm_${System.currentTimeMillis()}"
        }
        val cleanName = name.trim().ifBlank { "অ্যাডমিন সদস্য" }.take(140)
        val cleanRole = role.trim().ifBlank { "অ্যাডমিন" }.take(90)
        val docRef = db.collection("admins").document(cleanId)
        return try {
            val payload = mapOf(
                "adminId" to cleanId,
                "name" to cleanName,
                "email" to resolvedEmail,
                "adminPhone" to AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
                "role" to cleanRole,
                "isActive" to isActive,
                "verifiedByAuthToken" to isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitMessageWriteConfirmation(docRef, 12_000L)
            kotlinx.coroutines.withTimeoutOrNull(2_000L) {
                runCatching {
                    enqueueAndDispatchTelegramNotification(
                        eventType = "ADMIN_SECURITY",
                        title = "অ্যাডমিন সদস্য তথ্য হালনাগাদ",
                        message = "অ্যাডমিন: $cleanName ($cleanRole) - অবস্থা: ${if (isActive) "সক্রিয়" else "নিষ্ক্রিয়"}",
                        actorName = actorAdminName.ifBlank { "নাজিমে আলা (অ্যাডমিন)" },
                        referenceId = cleanId
                    )
                }
            }
            Result.success(cleanId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    suspend fun adminRecordLiveChangeNotification(
        eventType: String,
        title: String,
        message: String,
        adminName: String,
        referenceId: String = "admin_action"
    ): Result<String> {
        return enqueueAndDispatchTelegramNotification(
            eventType = eventType,
            title = title,
            message = message,
            actorName = adminName.ifBlank { "নাজিমে আলা (অ্যাডমিন)" },
            referenceId = referenceId
        )
    }

    // --- 10. AJKER SMORON (আজকের স্মরণ — সার্ভার-ভিত্তিক ইতিহাস, বরাদ্দ ও নোটিফিকেশন সিঙ্ক) ---

    fun observeSmoronServerReceipts(userId: String): Flow<Map<String, Long>> {
        val trimmed = userId.trim()
        val currentUid = auth.currentUser?.uid.orEmpty()
        if (trimmed.isBlank() || (currentUid.isNotBlank() && trimmed != currentUid)) {
            return kotlinx.coroutines.flow.flowOf(emptyMap())
        }
        val readsPath = "users/$trimmed/smoron_receipts"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection("users")
                .document(trimmed)
                .collection("notification_reads")
                .whereEqualTo("userId", trimmed),
            path = readsPath,
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snapshot ->
            snapshot.safeToObjects(NotificationReadReceipt::class.java)
                .mapNotNull { receipt ->
                    val key = receipt.notificationId.ifBlank { receipt.id }
                    if (key.startsWith("smoron_")) {
                        val tsMillis = receipt.readAt?.toDate()?.time ?: System.currentTimeMillis()
                        key to tsMillis
                    } else {
                        null
                    }
                }
        }.map { pairs -> pairs.toMap() }
    }

    suspend fun recordSmoronAssignmentInServer(
        dateKey: String,
        reminderId: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (dateKey.isBlank() || reminderId.isBlank()) return Result.success(Unit)
        val readsCol = db.collection("users").document(uid).collection("notification_reads")
        val dayDocId = "smoron_day_${dateKey}__$reminderId"
        val histDocId = "smoron_hist_$reminderId"
        return try {
            val batch = db.batch()
            listOf(dayDocId, histDocId).forEach { docId ->
                batch.set(
                    readsCol.document(docId),
                    mapOf(
                        "userId" to uid,
                        "notificationId" to docId,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/notification_reads")
            Result.failure(e)
        }
    }

    suspend fun recordSmoronPushSentInServer(
        dateKey: String,
        reminderId: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (dateKey.isBlank() || reminderId.isBlank()) return Result.success(Unit)
        val readsCol = db.collection("users").document(uid).collection("notification_reads")
        val pushDocId = "smoron_push_${dateKey}__$reminderId"
        val histDocId = "smoron_hist_$reminderId"
        return try {
            val batch = db.batch()
            listOf(pushDocId, histDocId).forEach { docId ->
                batch.set(
                    readsCol.document(docId),
                    mapOf(
                        "userId" to uid,
                        "notificationId" to docId,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/notification_reads")
            Result.failure(e)
        }
    }

    suspend fun recordSmoronAmolCompletedInServer(dateKey: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (dateKey.isBlank()) return Result.success(Unit)
        val docId = "smoron_amol_$dateKey"
        val docRef = db.collection("users")
            .document(uid)
            .collection("notification_reads")
            .document(docId)
        return try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "notificationId" to docId,
                    "readAt" to FieldValue.serverTimestamp()
                )
            ).awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 11. APP AUTO-UPDATE SYSTEM (সর্বশেষ ভার্সন ও ডাউনলোড লিংক পরিচালনা) ---

    private fun DocumentSnapshot.toAppReleaseInfoOrNull(): AppReleaseInfo? {
        return AppUpdateManagerHelper.parseReleaseSnapshotSafely(this)
    }

    fun observeLatestAppRelease(): Flow<AppReleaseInfo?> {
        val path = "app_releases/latest"
        return observeDocumentWithAutoRecovery(
            docRef = db.collection("app_releases").document("latest"),
            path = path,
            includeMetadataChanges = false,
            defaultValueOnError = null
        ) { snapshot ->
            snapshot.toAppReleaseInfoOrNull()
        }
    }

    suspend fun publishAppReleaseInfo(
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotes: String,
        apkSha256: String = "",
        apkFileSizeBytes: Long = 0L,
        commitSha: String = "",
        githubReleaseTag: String = "",
        isMandatory: Boolean = false,
        releaseStatus: String = "PUBLISHED",
        publishedBy: String = "নাজিমে আলা (অ্যাডমিন)"
    ): Result<Unit> {
        requireUserId()
        val trimmedUrl = apkDownloadUrl.trim()
        val trimmedVersionName = versionName.trim()
        val normalizedSha256 = AppUpdateManagerHelper.normalizeSha256(apkSha256)
        val normalizedStatus = releaseStatus.trim().uppercase().let {
            if (it in setOf("PUBLISHED", "PAUSED", "DISABLED")) it else "PUBLISHED"
        }
        if (versionCode < 1) {
            return Result.failure(IllegalArgumentException("সঠিক ভার্সন কোড প্রদান করুন (ন্যূনতম ১)।"))
        }
        if (trimmedVersionName.isBlank()) {
            return Result.failure(IllegalArgumentException("সঠিক ভার্সন নাম লিখুন (যেমন: 1.0.2)।"))
        }
        if (!AppUpdateManagerHelper.isTrustedHttpsApkUrl(trimmedUrl)) {
            return Result.failure(IllegalArgumentException("নিরাপদ ও বৈধ HTTPS APK ডাউনলোড লিংক (https://...) প্রদান করুন।"))
        }
        if (normalizedSha256.isNotBlank() && !AppUpdateManagerHelper.isValidSha256Hex(normalizedSha256)) {
            return Result.failure(IllegalArgumentException("সঠিক ৬৪ অক্ষরের SHA-256 চেকসাম (Hex) প্রদান করুন।"))
        }

        val latestRef = db.collection("app_releases").document("latest")
        val versionRef = db.collection("app_releases").document("v_$versionCode")
        val safeNotes = releaseNotes.trim().ifBlank {
            "কওমি কর্মসংস্থান বাংলাদেশ নতুন সংস্করণ ($trimmedVersionName) প্রকাশিত হয়েছে।"
        }
        val safePublisher = publishedBy.trim().ifBlank { "GitHub Actions CI" }
        val safeTag = githubReleaseTag.trim().ifBlank { "v$trimmedVersionName" }
        val isPublishedBool = normalizedStatus == "PUBLISHED"

        return try {
            val latestPayload = mapOf(
                "releaseId" to "latest",
                "versionCode" to versionCode,
                "versionName" to trimmedVersionName,
                "apkDownloadUrl" to trimmedUrl,
                "apkSha256" to normalizedSha256,
                "apkFileSizeBytes" to apkFileSizeBytes.coerceAtLeast(0L),
                "releaseNotes" to safeNotes,
                "commitSha" to commitSha.trim(),
                "githubReleaseTag" to safeTag,
                "isMandatory" to isMandatory,
                "isPublished" to isPublishedBool,
                "releaseStatus" to normalizedStatus,
                "publishedBy" to safePublisher,
                "publishedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val versionPayload = latestPayload.toMutableMap().apply {
                put("releaseId", "v_$versionCode")
            }
            val batch = db.batch()
            batch.set(latestRef, latestPayload)
            batch.set(versionRef, versionPayload)
            batch.commit().awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, latestRef.path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 10. KKS INTELLIGENCE ENGINE (স্মার্ট র‍্যাঙ্কিং, অ্যান্টি-ফ্রড ও অ্যাডাপ্টিভ লার্নিং)
    // =========================================================================

    // Session-level deduplication cache for deterministic engagement receipts: "{uid}_{postId}_{eventType}"
    private val sessionRecordedReceipts = ConcurrentHashMap.newKeySet<String>()
    private val lastEngagementTimestampByEvent = ConcurrentHashMap<String, Long>()

    /**
     * Observes the global KKS Intelligence Engine configuration (`/intelligence_config/global_weights`).
     * Always emits a valid [IntelligenceConfig] immediately from cache or default values.
     */
    fun observeIntelligenceConfig(): Flow<IntelligenceConfig> {
        val docRef = db.collection("intelligence_config")
            .document(IntelligenceConfig.GLOBAL_CONFIG_DOC_ID)
        return observeDocumentWithAutoRecovery(
            docRef = docRef,
            path = docRef.path,
            includeMetadataChanges = false,
            defaultValueOnError = IntelligenceConfig()
        ) { snap ->
            if (snap.exists()) {
                snap.safeToObject(IntelligenceConfig::class.java) ?: IntelligenceConfig()
            } else {
                IntelligenceConfig()
            }
        }.map { it ?: IntelligenceConfig() }
            .catch { emit(IntelligenceConfig()) }
    }

    /**
     * Saves updated KKS Intelligence Engine weights and feature flags to `/intelligence_config/global_weights`.
     * Restricted to verified Admins.
     */
    suspend fun saveIntelligenceConfig(
        config: IntelligenceConfig,
        adminName: String = "নাজিমে আলা (অ্যাডমিন)"
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("intelligence_config")
            .document(IntelligenceConfig.GLOBAL_CONFIG_DOC_ID)
        return try {
            val safeAdmin = adminName.trim().ifBlank { "নাজিমে আলা (অ্যাডমিন)" }.take(120)
            val payload = mapOf(
                "configId" to IntelligenceConfig.GLOBAL_CONFIG_DOC_ID,
                "skillMatchWeight" to config.skillMatchWeight.coerceIn(0.0, 100.0),
                "locationMatchWeight" to config.locationMatchWeight.coerceIn(0.0, 100.0),
                "experienceWeight" to config.experienceWeight.coerceIn(0.0, 100.0),
                "freshnessHalfLifeHours" to config.freshnessHalfLifeHours.coerceIn(4.0, 720.0),
                "trendingWeight" to config.trendingWeight.coerceIn(0.0, 100.0),
                "verifiedMadrasaBonus" to config.verifiedMadrasaBonus.coerceIn(0.0, 50.0),
                "newPostBoostHours" to config.newPostBoostHours.coerceIn(1.0, 168.0),
                "minDwellTimeMs" to config.minDwellTimeMs.coerceIn(1_000L, 30_000L),
                "fraudPenaltyWeight" to config.fraudPenaltyWeight.coerceIn(0.0, 100.0),
                "smartNotificationThreshold" to config.smartNotificationThreshold.coerceIn(20.0, 95.0),
                "isSmartRankingEnabled" to config.isSmartRankingEnabled,
                "isTrendingEnabled" to config.isTrendingEnabled,
                "isFraudDetectionEnabled" to config.isFraudDetectionEnabled,
                "isSmartNotificationEnabled" to config.isSmartNotificationEnabled,
                "updatedBy" to safeAdmin,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitWithTimeout(8_000L)

            withTimeoutOrNull(2_000L) {
                runCatching {
                    enqueueAndDispatchTelegramNotification(
                        eventType = "ADMIN_SECURITY",
                        title = "KKS Intelligence Engine কনফিগারেশন হালনাগাদ",
                        message = "র‍্যাঙ্কিং: ${if (config.isSmartRankingEnabled) "চালু" else "বন্ধ"} | ট্রেন্ডিং: ${if (config.isTrendingEnabled) "চালু" else "বন্ধ"} | অ্যান্টি-ফ্রড: ${if (config.isFraudDetectionEnabled) "চালু" else "বন্ধ"} | যোগ্যতা গুরুত্ব: ${config.skillMatchWeight.toInt()}%",
                        actorName = safeAdmin,
                        referenceId = IntelligenceConfig.GLOBAL_CONFIG_DOC_ID
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    /**
     * Observes post intelligence metrics (`/post_intelligence_metrics/{postId}`) across active posts.
     */
    fun observePostIntelligenceMetrics(): Flow<Map<String, PostIntelligenceMetrics>> {
        val path = "post_intelligence_metrics"
        return observeQueryWithFastInitialAndFallback(
            query = db.collection(path),
            path = path,
            includeMetadataChanges = false,
            initialServerTimeoutMs = 700L
        ) { snap ->
            snap.safeToObjects(PostIntelligenceMetrics::class.java)
                .filter { it.id.isNotBlank() || it.postId.isNotBlank() }
        }.map { metricsList ->
            metricsList.associateBy { it.postId.ifBlank { it.id } }
        }.catch { emit(emptyMap()) }
    }

    /**
     * Observes the signed-in user's adaptive learning affinity profile
     * (`/users/{userId}/intelligence_state/affinity`).
     */
    fun observeUserAffinityState(userId: String): Flow<UserAffinityState?> {
        val effectiveUid = resolveEffectiveUserId(userId)
        if (effectiveUid.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        val docRef = db.collection("users")
            .document(effectiveUid)
            .collection("intelligence_state")
            .document(UserAffinityState.AFFINITY_DOC_ID)
        return observeDocumentWithAutoRecovery(
            docRef = docRef,
            path = docRef.path,
            includeMetadataChanges = false,
            defaultValueOnError = null
        ) { snap ->
            if (snap.exists()) {
                snap.safeToObject(UserAffinityState::class.java)
            } else {
                null
            }
        }.catch { emit(null) }
    }

    /**
     * Records a verified unique user engagement event (`VIEW`, `CLICK`, `APPLY`, `BOOKMARK`, `SHARE`)
     * with deterministic deduplication receipt (`/post_intelligence_metrics/{postId}/engagement_receipts/{uid}_{eventType}`),
     * dwell-time gating, rate-limiting cooldown, and adaptive learning user affinity update.
     */
    suspend fun recordVerifiedEngagementEvent(
        post: FeedPost,
        eventType: String,
        dwellTimeMs: Long = 0L,
        currentConfig: IntelligenceConfig = IntelligenceConfig(),
        currentAffinity: UserAffinityState? = null
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return Result.success(false)
        val postId = post.id.trim()
        val normalizedType = eventType.trim().uppercase()
        if (postId.isBlank() || normalizedType !in EngagementReceipt.VALID_EVENT_TYPES) {
            return Result.success(false)
        }
        // Authors viewing/clicking their own post do not inflate unique engagement counters
        if (post.authorId == uid && (normalizedType == EngagementReceipt.EVENT_VIEW || normalizedType == EngagementReceipt.EVENT_CLICK)) {
            return Result.success(false)
        }
        // Enforce minimum dwell time for VIEW events (prevents rapid scroll / bot view inflation)
        val requiredDwellMs = currentConfig.minDwellTimeMs.coerceAtLeast(1_500L)
        if (normalizedType == EngagementReceipt.EVENT_VIEW && dwellTimeMs < requiredDwellMs) {
            return Result.success(false)
        }

        val sessionKey = "${uid}_${postId}_$normalizedType"
        if (!sessionRecordedReceipts.add(sessionKey)) {
            // Already recorded in this session
            return Result.success(false)
        }

        // Anti-burst rate limit per event type (minimum 600ms between identical event types)
        val nowMs = System.currentTimeMillis()
        val prevEventMs = lastEngagementTimestampByEvent[normalizedType] ?: 0L
        if ((nowMs - prevEventMs) in 1L..500L && normalizedType == EngagementReceipt.EVENT_CLICK) {
            sessionRecordedReceipts.remove(sessionKey)
            return Result.success(false)
        }
        lastEngagementTimestampByEvent[normalizedType] = nowMs

        val receiptId = EngagementReceipt.buildDeterministicReceiptId(uid, normalizedType)
        val metricsRef = db.collection("post_intelligence_metrics").document(postId)
        val receiptRef = metricsRef.collection("engagement_receipts").document(receiptId)
        val affinityRef = db.collection("users")
            .document(uid)
            .collection("intelligence_state")
            .document(UserAffinityState.AFFINITY_DOC_ID)

        return try {
            // Check cache/server first so we never double-increment if receipt already exists
            val existingReceiptSnap = receiptRef.getFastOrNull(1_800L)
            if (existingReceiptSnap != null && existingReceiptSnap.exists()) {
                return Result.success(false)
            }

            val existingMetricsSnap = metricsRef.getFastOrNull(1_800L)
            val existingMetrics = existingMetricsSnap?.safeToObject(PostIntelligenceMetrics::class.java)
                ?: PostIntelligenceMetrics(id = postId, postId = postId)

            val newViews = existingMetrics.uniqueViewCount + if (normalizedType == EngagementReceipt.EVENT_VIEW) 1 else 0
            val newClicks = existingMetrics.uniqueClickCount + if (normalizedType == EngagementReceipt.EVENT_CLICK) 1 else 0
            val newApplies = existingMetrics.applyCount + if (normalizedType == EngagementReceipt.EVENT_APPLY) 1 else 0
            val newBookmarks = existingMetrics.bookmarkCount + if (normalizedType == EngagementReceipt.EVENT_BOOKMARK) 1 else 0
            val newShares = existingMetrics.shareCount + if (normalizedType == EngagementReceipt.EVENT_SHARE) 1 else 0
            val safeDwellAddition = if (normalizedType == EngagementReceipt.EVENT_VIEW) {
                dwellTimeMs.coerceIn(requiredDwellMs, 180_000L)
            } else {
                0L
            }
            val newTotalDwell = existingMetrics.totalDwellTimeMs + safeDwellAddition

            val projectedMetrics = existingMetrics.copy(
                postId = postId,
                uniqueViewCount = newViews,
                uniqueClickCount = newClicks,
                applyCount = newApplies,
                bookmarkCount = newBookmarks,
                shareCount = newShares,
                totalDwellTimeMs = newTotalDwell
            )

            val ageHours = post.createdAt?.toDate()?.time?.let {
                ((nowMs - it).coerceAtLeast(0L).toDouble()) / (1000.0 * 3600.0)
            } ?: 4.0

            val anomalyEval = KksIntelligenceEngine.evaluateAnomalyAndFraud(post, projectedMetrics, currentConfig)
            val updatedTrending = KksIntelligenceEngine.computeTrendingVelocity(
                post = post,
                metrics = projectedMetrics,
                ageHours = ageHours,
                anomalyRiskNorm = anomalyEval.anomalyRiskNorm
            )
            val updatedQuality = (
                50.0 +
                    (newApplies * 6.0) +
                    (newBookmarks * 3.0) +
                    (newShares * 2.5) +
                    (newClicks * 1.2) -
                    (anomalyEval.anomalyScore * 0.45)
                ).coerceIn(0.0, 100.0)

            val receiptPayload = mapOf(
                "userId" to uid,
                "postId" to postId,
                "eventType" to normalizedType,
                "dwellTimeMs" to safeDwellAddition,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val metricsPayload = mapOf(
                "postId" to postId,
                "uniqueViewCount" to newViews,
                "uniqueClickCount" to newClicks,
                "applyCount" to newApplies,
                "bookmarkCount" to newBookmarks,
                "shareCount" to newShares,
                "totalDwellTimeMs" to newTotalDwell,
                "trendingScore" to ((updatedTrending * 100.0).toInt() / 100.0),
                "qualityScore" to ((updatedQuality * 100.0).toInt() / 100.0),
                "anomalyScore" to ((anomalyEval.anomalyScore * 100.0).toInt() / 100.0),
                "isFlagged" to anomalyEval.isFlagged,
                "flagReason" to anomalyEval.reasonBangla.take(300),
                "lastInteractionAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val updatedAffinity = KksIntelligenceEngine.computeUpdatedAffinityState(
                currentState = currentAffinity,
                userId = uid,
                post = post,
                eventType = normalizedType,
                dwellTimeMs = safeDwellAddition,
                nowMillis = nowMs
            )

            val affinityPayload = mapOf(
                "userId" to uid,
                "categoryWeights" to updatedAffinity.categoryWeights,
                "districtWeights" to updatedAffinity.districtWeights,
                "totalInteractions" to updatedAffinity.totalInteractions,
                "lastEventAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(receiptRef, receiptPayload)
            batch.set(metricsRef, metricsPayload)
            batch.set(affinityRef, affinityPayload)
            batch.commit().awaitWithTimeout(6_000L)
            Result.success(true)
        } catch (e: CancellationException) {
            sessionRecordedReceipts.remove(sessionKey)
            throw e
        } catch (e: Exception) {
            sessionRecordedReceipts.remove(sessionKey)
            Result.failure(e)
        }
    }

    /**
     * Allows a verified Admin from the Intelligence Control Center to clear an anomaly flag
     * or manually flag a post for artificial engagement / spam penalty.
     */
    suspend fun adminOverridePostAnomalyFlag(
        post: FeedPost,
        existingMetrics: PostIntelligenceMetrics?,
        flagPost: Boolean,
        reasonBangla: String,
        adminName: String = "নাজিমে আলা (অ্যাডমিন)"
    ): Result<Unit> {
        requireUserId()
        val postId = post.id.trim()
        if (postId.isBlank()) return Result.failure(IllegalArgumentException("বৈধ পোস্ট আইডি পাওয়া যায়নি।"))
        val metricsRef = db.collection("post_intelligence_metrics").document(postId)
        return try {
            val base = existingMetrics ?: PostIntelligenceMetrics(id = postId, postId = postId)
            val resolvedReason = if (flagPost) {
                "[অ্যাডমিন প্যানেল] ${reasonBangla.trim().ifBlank { "কৃত্রিম এনগেজমেন্ট বা স্প্যাম সন্দেহে চিহ্নিত" }}".take(300)
            } else {
                ""
            }
            val payload = mapOf(
                "postId" to postId,
                "uniqueViewCount" to base.uniqueViewCount,
                "uniqueClickCount" to base.uniqueClickCount,
                "applyCount" to base.applyCount,
                "bookmarkCount" to base.bookmarkCount,
                "shareCount" to base.shareCount,
                "totalDwellTimeMs" to base.totalDwellTimeMs,
                "trendingScore" to if (flagPost) 0.0 else base.trendingScore,
                "qualityScore" to if (flagPost) 15.0 else base.qualityScore.coerceAtLeast(60.0),
                "anomalyScore" to if (flagPost) 85.0 else 0.0,
                "isFlagged" to flagPost,
                "flagReason" to resolvedReason,
                "lastInteractionAt" to (base.lastInteractionAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            metricsRef.set(payload).awaitWithTimeout(8_000L)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, metricsRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }
}




