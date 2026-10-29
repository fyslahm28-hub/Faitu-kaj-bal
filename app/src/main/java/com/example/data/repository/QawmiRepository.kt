package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminRegistryEntry
import com.example.data.model.AppNotification
import com.example.data.model.AppReleaseInfo
import com.example.data.model.BookmarkItem
import com.example.data.model.ChatGroup
import com.example.data.model.DirectMessage
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.FeedPostExtendedAttachment
import com.example.data.model.KhedmotCategoryHelper
import com.example.data.model.KhedmotSubmission
import com.example.data.model.MadrasaProfile
import com.example.data.model.NotificationReadReceipt
import com.example.data.model.TelegramNotificationLog
import com.example.data.model.UserProfile
import com.example.ui.util.AdminSecurityHelper
import com.example.ui.util.AppUpdateManagerHelper
import com.example.ui.util.ProfilePhotoHelper
import com.example.ui.util.TelegramNotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class QawmiRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
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

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("এটি সম্পন্ন করতে গুগল অ্যাকাউন্ট দিয়ে লগইন করা আবশ্যক।")
    }

    // --- 0. ADMIN IDENTITY, SERVER REGISTRY & PASSWORD RECOVERY ---

    fun observeAdminRegistryEntry(userId: String): Flow<AdminRegistryEntry?> {
        val path = "admins/$userId"
        return db.collection("admins").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObject(AdminRegistryEntry::class.java)
            }
            .catch {
                emit(null)
            }
    }

    /**
     * Verifies the signed Firebase Auth ID token / Custom Claims and syncs the server-side
     * `/admins/{uid}` registry document when an authorized Admin logs in.
     * Protected on the backend by `isTokenVerifiedAdmin()` in `firestore.rules`.
     */
    suspend fun ensureVerifiedAdminRegistryDocument(): Result<Boolean> {
        val currentUser = auth.currentUser ?: return Result.success(false)
        val isVerifiedAdmin = AdminSecurityHelper.verifyServerTokenAdminClaims(currentUser)
        if (!isVerifiedAdmin) {
            return Result.success(false)
        }
        val uid = currentUser.uid
        val normalizedEmail = AdminSecurityHelper.normalizeEmail(currentUser.email)
            .takeIf { AdminSecurityHelper.isAuthorizedAdminEmail(it) }
            ?: "fyslahm28@gmail.com"
        val docRef = db.collection("admins").document(uid)
        return try {
            val payload = mapOf(
                "adminId" to uid,
                "email" to normalizedEmail,
                "adminPhone" to AdminSecurityHelper.ADMIN_MOBILE_NUMBER,
                "role" to "SUPER_ADMIN",
                "verifiedByAuthToken" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitWithTimeout(8_000L)

            enqueueAndDispatchTelegramNotification(
                eventType = "ADMIN_LOGIN",
                title = "অ্যাডমিন প্যানেল সেশন লগইন",
                message = "অনুমোদিত অ্যাডমিন ($normalizedEmail) যাচাইকৃত সেশনে লগইন করেছেন।",
                actorName = normalizedEmail,
                referenceId = uid
            )
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordAdminLogoutActivity(adminEmailOrName: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.success(Unit)
        if (!AdminSecurityHelper.verifyServerTokenAdminClaims(currentUser)) {
            return Result.success(Unit)
        }
        val actor = adminEmailOrName.ifBlank { currentUser.email ?: "নাজিমে আলা (অ্যাডমিন)" }
        enqueueAndDispatchTelegramNotification(
            eventType = "ADMIN_LOGOUT",
            title = "অ্যাডমিন সেশন লগআউট",
            message = "অনুমোদিত অ্যাডমিন ($actor) অ্যাপ থেকে লগআউট করেছেন।",
            actorName = actor,
            referenceId = currentUser.uid
        )
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
            val candidate = obj.copy(
                id = id,
                isOnline = resolvedOnline,
                isVerified = resolvedVerified,
                typingToUserId = resolvedTypingTo,
                blockedUserIds = rawBlocked,
                chatThemes = rawThemes
            )
            val effectiveOnline = if (metadata.hasPendingWrites()) {
                candidate.isOnline
            } else {
                candidate.isEffectivelyOnline()
            }
            candidate.copy(isOnline = effectiveOnline)
        } catch (_: Throwable) {
            null
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toUserProfileOrNull()
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
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
        return try {
            val snap = docRef.getFastOrNull(3_500L)
            if (snap != null && snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "isOnline" to isOnline,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!isOnline) {
                    updates["typingToUserId"] = ""
                }
                docRef.update(updates).awaitWithTimeout(8_000L)
            } else {
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
                    "isOnline" to isOnline,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "typingToUserId" to "",
                    "isVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitWithTimeout(8_000L)
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

    fun observePublicFeedPosts(): Flow<List<FeedPost>> {
        val path = "feed_posts"
        return db.collection(path)
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(FeedPost::class.java)
                    .distinctBy { it.id }
                    .sortedWith(
                        compareByDescending<FeedPost> { it.isBoosted }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
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
        sharedPostType: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("feed_posts")
        val docRef = collectionRef.document()
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

            // 1. Upload images/videos to Firebase Storage and extract compact metadata
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

            // 2. Store any large attachment or extended text (> 2000 chars) in separate /feed_post_attachments documents
            var primaryAttachmentDocId = ""
            val metadataMaps = mutableListOf<Map<String, String>>()

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
                extRef.set(extPayload).awaitWithTimeout(10_000L)
                primaryAttachmentDocId = extDocId
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
                    mediaRef.set(mediaPayload).awaitWithTimeout(10_000L)
                    ProfilePhotoHelper.cacheAttachmentDataUrl(mediaDocId, mediaItem.separateAttachmentDataUrl)
                    itemAttachmentDocId = mediaDocId
                    if (primaryAttachmentDocId.isBlank()) {
                        primaryAttachmentDocId = mediaDocId
                    }
                }
                metadataMaps.add(mediaItem.toMetadataMap(itemAttachmentDocId))
            }

            // 3. Ensure authorPhotoUrl on feed_posts is compact or a Storage URL
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
                if (postType == "GENERAL_POST") "দ্বীনি আলোচনা ও খেদমত" else designation.trim()
            }

            val payload = mutableMapOf<String, Any>(
                "authorId" to uid,
                "authorName" to authorName.trim().ifBlank { title.trim() }.take(200),
                "authorPhotoUrl" to safeAuthorPhotoUrl,
                "postType" to postType,
                "title" to title.trim().take(200),
                "district" to district.trim().take(100),
                "upazilaOrLocation" to upazilaOrLocation.trim().take(200),
                "designation" to resolvedDesignation.take(150),
                "qualification" to qualification.trim().take(200),
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

            docRef.set(payload).awaitWithTimeout(10_000L)

            // Deliver community notification and Telegram Admin log concurrently with bounded timeout so post publishing returns fast
            val isCircular = postType == "JOB_CIRCULAR"
            val isGeneral = postType == "GENERAL_POST"
            val notifTitle = when {
                isCircular -> "নতুন খেদমতের নিয়োগ বিজ্ঞপ্তি"
                isGeneral -> "নতুন দ্বীনি পোস্ট ও আলোচনা"
                else -> "নতুন খেদমতপ্রার্থীর আবেদন"
            }
            val notifMsg = when {
                isCircular -> "${title.trim()} (${district.trim()})-এ '${resolvedDesignation}' পদে নিয়োগ বিজ্ঞপ্তি প্রকাশিত হয়েছে।"
                isGeneral -> "${authorName.trim().ifBlank { title.trim() }} ফিডে একটি নতুন পোস্ট শেয়ার করেছেন: ${title.trim()}"
                else -> "${title.trim()} (${qualification.trim()}) '${resolvedDesignation}' খেদমতের জন্য আবেদন প্রকাশ করেছেন।"
            }
            val resolvedSender = authorName.trim().ifBlank { title.trim() }
            kotlinx.coroutines.withTimeoutOrNull(2_500L) {
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
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
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
                extRef.set(extPayload).awaitWithTimeout(10_000L)
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
                    mediaRef.set(mediaPayload).awaitWithTimeout(10_000L)
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

            docRef.update(updates).awaitWithTimeout(10_000L)
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
                        db.collection("feed_post_attachments").document(attachDocId).set(
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
            val snap = docRef.getFastOrNull(3_500L)
            val existingPost = if (snap != null && snap.exists()) {
                snap.safeToObject(FeedPost::class.java) ?: post
            } else {
                post
            }
            val updatedReactions = existingPost.userReactions.toMutableMap()
            val updatedNames = existingPost.userReactionNames.toMutableMap()
            val currentReaction = updatedReactions[uid].orEmpty()

            if (reactionCode.isBlank() || currentReaction == reactionCode) {
                updatedReactions.remove(uid)
                updatedNames.remove(uid)
            } else {
                updatedReactions[uid] = reactionCode
                updatedNames[uid] = reactorName.trim().ifBlank { "সম্মানিত সদস্য" }.take(120)
            }

            docRef.update(
                mapOf(
                    "userReactions" to updatedReactions,
                    "userReactionNames" to updatedNames,
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

    fun observePublicPostComments(): Flow<List<FeedComment>> {
        val path = "post_comments"
        return db.collection(path)
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(FeedComment::class.java)
                    .distinctBy { it.id }
                    .sortedWith(
                        compareBy<FeedComment> { it.createdAt ?: Timestamp(0, 0) }
                            .thenBy { it.id }
                    )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
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
            val payload = mapOf(
                "postId" to post.id,
                "authorId" to uid,
                "authorName" to resolvedAuthor,
                "authorPhotoUrl" to authorPhotoUrl.trim(),
                "text" to cleanText,
                "parentCommentId" to parentCommentId.trim(),
                "replyToAuthorName" to replyToAuthorName.trim().take(200),
                "isEdited" to false,
                "visibility" to "PUBLIC",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            commentRef.set(payload).awaitWithTimeout(8_000L)

            try {
                val postSnap = postRef.getFastOrNull(3_000L)
                val currentCount = (postSnap?.getLong("commentCount")?.toInt() ?: post.commentCount).coerceAtLeast(0)
                postRef.update(
                    mapOf(
                        "commentCount" to (currentCount + 1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitWithTimeout(8_000L)
            } catch (_: Exception) {
                // Non-fatal if post counter update is skipped
            }

            if (post.authorId.isNotBlank() && post.authorId != uid) {
                createNotification(
                    recipientId = post.authorId,
                    senderName = resolvedAuthor,
                    title = if (parentCommentId.isNotBlank()) "আপনার পোস্টের কমেন্টে নতুন উত্তর" else "আপনার পোস্টে নতুন কমেন্ট",
                    message = "$resolvedAuthor মন্তব্য করেছেন: \"${cleanText.take(120)}\"",
                    type = "APPLICATION",
                    referenceId = post.id
                )
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
            ).awaitWithTimeout(8_000L)
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
            commentRef.delete().awaitWithTimeout(8_000L)
            if (postId.isNotBlank()) {
                try {
                    val postRef = db.collection("feed_posts").document(postId)
                    val snap = postRef.getFastOrNull(3_000L)
                    val existingCount = (snap?.getLong("commentCount")?.toInt() ?: currentCommentCount).coerceAtLeast(1)
                    postRef.update(
                        mapOf(
                            "commentCount" to (existingCount - 1).coerceAtLeast(0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).awaitWithTimeout(8_000L)
                } catch (_: Exception) {
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
        shareCaption: String
    ): Result<String> {
        val uid = requireUserId()
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
            sharedPostType = targetOriginalType
        )

        createRes.onSuccess {
            try {
                val origRef = db.collection("feed_posts").document(originalPost.id)
                val snap = origRef.getFastOrNull(3_000L)
                val currentShares = (snap?.getLong("shareCount")?.toInt() ?: originalPost.shareCount).coerceAtLeast(0)
                origRef.update(
                    mapOf(
                        "shareCount" to (currentShares + 1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitWithTimeout(8_000L)
            } catch (_: Exception) {
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
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitWithTimeout(8_000L)

            if (isVerified && post.authorId.isNotBlank() && post.authorId != uid) {
                createNotification(
                    recipientId = post.authorId,
                    senderName = adminName,
                    title = "বিজ্ঞপ্তি যাচাইকৃত ব্যাজ প্রাপ্তি",
                    message = "আপনার প্রকাশিত '${post.title}' পোস্টটিতে যাচাইকৃত ব্যাজ যুক্ত করা হয়েছে।",
                    type = "VERIFICATION",
                    referenceId = post.id
                )
            }
            enqueueAndDispatchTelegramNotification(
                eventType = "ADMIN_VERIFICATION",
                title = "পোস্ট যাচাইকরণ হালনাগাদ",
                message = "'${post.title}' (${post.designation}) পোস্টের যাচাইকরণ অবস্থা: ${if (isVerified) "যাচাইকৃত ✓" else "আনভেরিফাই"}",
                actorName = adminName,
                referenceId = post.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
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
            ).awaitWithTimeout(8_000L)

            if (isBoosted && post.authorId.isNotBlank() && post.authorId != uid) {
                createNotification(
                    recipientId = post.authorId,
                    senderName = adminName,
                    title = "বিজ্ঞপ্তিটি ফিডের শীর্ষে Boosted হয়েছে",
                    message = "অ্যাডমিন প্যানেল থেকে আপনার '${post.title}' (${post.designation}) বিজ্ঞপ্তিটি ফিডের উপরে বিশেষভাবে প্রদর্শিত হচ্ছে।",
                    type = "BOOST",
                    referenceId = post.id
                )
            }
            enqueueAndDispatchTelegramNotification(
                eventType = "ADMIN_VERIFICATION",
                title = if (isBoosted) "বিজ্ঞপ্তি Boosted করা হয়েছে" else "বিজ্ঞপ্তির Boost সরানো হয়েছে",
                message = "'${post.title}' (${post.designation} - ${post.district}) বিজ্ঞপ্তির Boost অবস্থা হালনাগাদ করা হয়েছে।",
                actorName = adminName,
                referenceId = post.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 3. BOOKMARKS (পছন্দের খেদমত) ---

    fun observeBookmarks(userId: String): Flow<List<BookmarkItem>> {
        val path = "users/$userId/bookmarks"
        return db.collection("users").document(userId).collection("bookmarks")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(BookmarkItem::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
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
        return db.collection(path)
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(MadrasaProfile::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
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
        val path = "khedmot_submissions"
        return db.collection(path)
            .whereEqualTo("applicantId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(KhedmotSubmission::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeReceivedSubmissions(userId: String): Flow<List<KhedmotSubmission>> {
        val path = "khedmot_submissions"
        return db.collection(path)
            .whereEqualTo("recipientId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(KhedmotSubmission::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun submitApplicationToCircular(
        post: FeedPost,
        applicantName: String,
        applicantQualification: String,
        applicantExperience: String,
        applicantPhone: String,
        coverNote: String
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("khedmot_submissions").document()
        return try {
            val payload = mapOf(
                "postId" to post.id,
                "postTitle" to post.title,
                "postDesignation" to post.designation,
                "applicantId" to uid,
                "applicantName" to applicantName.trim(),
                "applicantQualification" to applicantQualification.trim(),
                "applicantExperience" to applicantExperience.trim(),
                "applicantPhone" to applicantPhone.trim(),
                "coverNote" to coverNote.trim(),
                "recipientId" to post.authorId,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).awaitWithTimeout(10_000L)

            // Deliver real-time notification ONLY to the Madrasa / circular author (not to self)
            if (post.authorId.isNotBlank() && post.authorId != uid) {
                kotlinx.coroutines.withTimeoutOrNull(3000L) {
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

            kotlinx.coroutines.withTimeoutOrNull(3500L) {
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

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    suspend fun updateSubmissionStatus(
        submission: KhedmotSubmission,
        newStatus: String,
        actorName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("khedmot_submissions").document(submission.id)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitWithTimeout(8_000L)

            if (submission.applicantId.isNotBlank() && submission.applicantId != uid) {
                val statusBangla = when (newStatus) {
                    "SHORTLISTED" -> "প্রাথমিক তালিকায় নির্বাচিত (মুযাকারার জন্য)"
                    "ACCEPTED" -> "কবুল / চূড়ান্তভাবে নির্বাচিত"
                    "REJECTED" -> "অপেক্ষমাণ / এবারের জন্য নির্বাচিত হননি"
                    else -> "হালনাগাদ"
                }
                createNotification(
                    recipientId = submission.applicantId,
                    senderName = actorName,
                    title = "আবেদনের অবস্থা হালনাগাদ",
                    message = "'${submission.postTitle}'-এ আপনার খেদমতের আবেদনের বর্তমান অবস্থা: $statusBangla।",
                    type = "APPLICATION",
                    referenceId = submission.id
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
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
        val path = "chat_groups"
        return db.collection(path)
            .whereArrayContains("memberIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toChatGroupOrNull() }
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
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
        return try {
            val obj = safeToObject(DirectMessage::class.java) ?: return null
            val resolvedIsRead = runCatching { getBoolean("isRead") ?: getBoolean("read") }.getOrNull() ?: obj.isRead
            val resolvedSenderReaction = runCatching { getString("senderReaction") }.getOrNull() ?: obj.senderReaction
            val resolvedReceiverReaction = runCatching { getString("receiverReaction") }.getOrNull() ?: obj.receiverReaction
            val resolvedIsEdited = runCatching { getBoolean("isEdited") }.getOrNull() ?: obj.isEdited
            val resolvedEditedAt = runCatching { getTimestamp("editedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }.getOrNull() ?: obj.editedAt
            val resolvedDeletedForSender = runCatching { getBoolean("deletedForSender") }.getOrNull() ?: obj.deletedForSender
            val resolvedDeletedForReceiver = runCatching { getBoolean("deletedForReceiver") }.getOrNull() ?: obj.deletedForReceiver
            val resolvedDeletedForEveryone = runCatching { getBoolean("isDeletedForEveryone") }.getOrNull() ?: obj.isDeletedForEveryone
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
            obj.copy(
                id = id,
                groupId = resolvedGroupId,
                groupName = resolvedGroupName,
                groupMemberIds = resolvedGroupMembers,
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
                isDeletedForEveryone = resolvedDeletedForEveryone
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun observeUserMessages(userId: String): Flow<List<DirectMessage>> {
        val path = "messages"
        val sentFlow = db.collection(path)
            .whereEqualTo("senderId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toDirectMessageOrNull() }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }

        val receivedFlow = db.collection(path)
            .whereEqualTo("receiverId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toDirectMessageOrNull() }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }

        val groupMessagesFlow = db.collection(path)
            .whereArrayContains("groupMemberIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toDirectMessageOrNull() }
            }
            .catch {
                emit(emptyList())
            }

        return combine(sentFlow, receivedFlow, groupMessagesFlow) { sent, received, groupMsgs ->
            // Merge received and group messages first so read/reaction state is authoritative, and deduplicate strictly by document id
            (received + groupMsgs + sent)
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
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
        val isGroupMessage = groupId.isNotBlank() || receiverId.startsWith("GROUP:", ignoreCase = true)
        val resolvedGroupId = if (groupId.isNotBlank()) groupId else receiverId.substringAfter(":", receiverId)
        if (!isGroupMessage && (receiverId.isBlank() || receiverId == uid)) {
            return Result.failure(IllegalArgumentException("নিজের অ্যাকাউন্টে মেসেজ পাঠানো যাবে না।"))
        }
        if (imageUrls.size > ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE) {
            return Result.failure(IllegalArgumentException("একবারে সর্বোচ্চ ৫টি ছবি পাঠানো যাবে।"))
        }
        val safeSenderName = senderName.trim().ifBlank { "সম্মানিত দ্বীনি ভাই" }
        val safeReceiverName = receiverName.trim().ifBlank {
            if (isGroupMessage) groupName.trim().ifBlank { "গ্রুপ কথোপকথন" } else "সম্মানিত প্রাপক"
        }
        val docRef = if (preallocatedMessageId.isNotBlank()) {
            db.collection("messages").document(preallocatedMessageId.trim())
        } else {
            db.collection("messages").document()
        }
        return try {
            // Idempotency check on retry: if this preallocated message ID was already saved by the sender,
            // return success immediately without re-writing createdAt or duplicating work.
            if (preallocatedMessageId.isNotBlank()) {
                val existingDoc = docRef.getCacheFirstOrNull(500L)
                if (existingDoc != null && existingDoc.exists() && existingDoc.getString("senderId") == uid) {
                    return Result.success(docRef.id)
                }
            }

            if (!isGroupMessage) {
                // Check block status between sender and receiver concurrently using cache-first reads
                val (senderSnap, receiverSnap) = coroutineScope {
                    val senderDeferred = async {
                        runCatching { db.collection("users").document(uid).getCacheFirstOrNull(800L) }.getOrNull()
                    }
                    val receiverDeferred = async {
                        runCatching { db.collection("users").document(receiverId).getCacheFirstOrNull(800L) }.getOrNull()
                    }
                    senderDeferred.await() to receiverDeferred.await()
                }
                val senderBlockedList = (senderSnap?.get("blockedUserIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                if (receiverId in senderBlockedList) {
                    return Result.failure(
                        IllegalStateException("আপনি এই ব্যবহারকারীর সাথে পয়গাম আদান-প্রদান বিরত (ব্লক) রেখেছেন। পুনরায় পয়গাম পাঠাতে আনব্লক করুন।")
                    )
                }

                val receiverBlockedList = (receiverSnap?.get("blockedUserIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                if (uid in receiverBlockedList) {
                    return Result.failure(
                        IllegalStateException("এই কথোপকথনে পয়গাম প্রেরণ বর্তমানে প্রাপক কর্তৃক সীমাবদ্ধ রয়েছে।")
                    )
                }
            }

            val uploadedImages = ProfilePhotoHelper.uploadChatImagesToFirebaseStorage(
                imageItems = imageUrls.take(ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE),
                senderId = uid
            )
            // Keep body strictly as the user's actual text (empty string when sending images without caption)
            // so no artificial "📷 ১ টি ছবি প্রেরণ করা হয়েছে" or "ছবি মেসেজ পাঠানো হচ্ছে" text is injected.
            val resolvedBody = body.trim()
            if (resolvedBody.isBlank() && uploadedImages.isEmpty()) {
                return Result.failure(IllegalArgumentException("অনুগ্রহ করে পয়গাম লিখুন অথবা ছবি নির্বাচন করুন।"))
            }

            // Compact senderPhotoUrl synchronously so we never upload 100KB-240KB of raw Base64 avatar data on every chat message
            val compactSenderPhoto = ProfilePhotoHelper.compactSenderPhotoForMessage(senderPhotoUrl.trim())

            val payload = mutableMapOf<String, Any>(
                "senderId" to uid,
                "senderName" to safeSenderName,
                "senderPhotoUrl" to compactSenderPhoto,
                "receiverId" to if (isGroupMessage) "GROUP:$resolvedGroupId" else receiverId,
                "receiverName" to safeReceiverName,
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
                payload["groupId"] = resolvedGroupId
                payload["groupName"] = groupName.trim().ifBlank { safeReceiverName }
                payload["groupMemberIds"] = finalGroupMembers
            }

            docRef.set(payload).awaitMessageWriteConfirmation(docRef, timeoutMs = 8_000L)

            // Note: Paygam/Direct messages do NOT create documents in the `notifications` collection.
            // Bell icon notifications are strictly reserved for general notifications (NEW_CIRCULAR, APPLICATION, ANNOUNCEMENT, etc.).
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(translateFirestoreExceptionToBangla(e))
        }
    }

    /**
     * Marks incoming unread messages as delivered (`DELIVERED:<uid>` in `readByUserIds` while keeping
     * `isRead = false`) so the sender's real-time snapshot listener immediately transitions from
     * Sent (`✓`) to Delivered (`✓✓`) when the recipient's device receives the message.
     */
    suspend fun acknowledgeMessagesDelivered(messages: List<DirectMessage>): Result<Unit> {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return Result.success(Unit)
        val deliveredToken = "DELIVERED:$uid"
        val pendingDelivery = messages.filter { msg ->
            msg.id.isNotBlank() &&
                msg.senderId != uid &&
                msg.receiverId == uid &&
                !msg.isRead &&
                !msg.readByUserIds.contains(uid) &&
                !msg.readByUserIds.contains(deliveredToken) &&
                msg.readByUserIds.size < 48
        }.take(20)
        if (pendingDelivery.isEmpty()) return Result.success(Unit)
        return try {
            val batch = db.batch()
            for (msg in pendingDelivery) {
                val updatedReadBy = (msg.readByUserIds + deliveredToken).distinct().take(50)
                batch.update(
                    db.collection("messages").document(msg.id),
                    mapOf("readByUserIds" to updatedReadBy)
                )
            }
            batch.commit().awaitWithTimeout(5_000L)
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
            docRef.update(
                mapOf(
                    "isRead" to true,
                    "readByUserIds" to FieldValue.arrayUnion(uid)
                )
            ).awaitWithTimeout(6_000L)
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
            // Only query Firestore directly if the in-memory messages list had no matching unread items
            // (e.g., if called before messagesState emitted), avoiding redundant full-collection downloads.
            if (unreadMap.isEmpty()) {
                val receivedSnap = kotlinx.coroutines.withTimeoutOrNull(4_000L) {
                    db.collection("messages")
                        .whereEqualTo("receiverId", uid)
                        .get()
                        .await()
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
        val notifPath = "notifications"
        val readsPath = "users/$userId/notification_reads"

        val directFlow = db.collection(notifPath)
            .whereEqualTo("recipientId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(AppNotification::class.java)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, notifPath)
                throw error
            }

        val broadcastFlow = db.collection(notifPath)
            .whereEqualTo("recipientId", "ALL_USERS")
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(AppNotification::class.java)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, notifPath)
                throw error
            }

        val readReceiptsFlow = db.collection("users")
            .document(userId)
            .collection("notification_reads")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(NotificationReadReceipt::class.java)
                    .map { it.notificationId.ifBlank { it.id } }
                    .toSet()
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, readsPath)
                throw error
            }

        return combine(directFlow, broadcastFlow, readReceiptsFlow) { directList, broadcastList, readIds ->
            (directList + broadcastList)
                .distinctBy { it.id }
                // Never show self-generated notifications or Paygam chat messages in the Bell notification list
                .filter { it.senderId != userId && !it.type.equals("MESSAGE", ignoreCase = true) }
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
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(TelegramNotificationLog::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
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
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        val adminEmailSuffix = currentUser?.email?.takeIf { it.isNotBlank() }?.let { email ->
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
        val dispatchDiag = TelegramNotificationHelper.dispatchToTelegramBotVerified(
            eventType = eventType,
            title = resolvedTitle,
            message = resolvedMessage,
            actorName = resolvedActor,
            targetPhone = resolvedTargetPhone,
            eventKey = eventKey,
            bypassDedup = bypassDedup
        )

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
            docRef.set(payload).awaitWithTimeout(8_000L)
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
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toUserProfileOrNull() }
                    .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllSubmissionsForAdmin(): Flow<List<KhedmotSubmission>> {
        val path = "khedmot_submissions"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(KhedmotSubmission::class.java)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeAllAdminsForAdmin(): Flow<List<AdminRegistryEntry>> {
        val path = "admins"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.safeToObjects(AdminRegistryEntry::class.java)
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
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
            ).awaitWithTimeout(8_000L)

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
            enqueueAndDispatchTelegramNotification(
                eventType = eventType,
                title = title,
                message = message,
                actorName = resolvedAdmin,
                referenceId = targetId
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateSubmissionStatus(
        submission: KhedmotSubmission,
        newStatus: String,
        adminName: String
    ): Result<Unit> {
        val resolvedAdmin = adminName.ifBlank { "নাজিমে আলা (অ্যাডমিন)" }
        val res = updateSubmissionStatus(submission, newStatus, resolvedAdmin)
        res.onSuccess {
            val statusBangla = when (newStatus) {
                "ACCEPTED" -> "নির্বাচিত (অনুমোদিত)"
                "SHORTLISTED" -> "পর্যালোচনাধীন (প্রাথমিক নির্বাচিত)"
                "REJECTED" -> "বাতিল"
                else -> "নতুন"
            }
            enqueueAndDispatchTelegramNotification(
                eventType = "KHEDMOT_SUBMISSION",
                title = if (newStatus == "ACCEPTED") "আবেদন অনুমোদন করা হয়েছে" else "আবেদনের অবস্থা পরিবর্তন: $statusBangla",
                message = "প্রার্থী: ${submission.applicantName} | পদ: ${submission.postDesignation} | বিজ্ঞপ্তি: ${submission.postTitle} | বর্তমান অবস্থা: $statusBangla",
                actorName = resolvedAdmin,
                referenceId = submission.id.ifBlank { "submission_update" }
            )
        }
        return res
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
        if (userId.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyMap())
        val readsPath = "users/$userId/notification_reads"
        return db.collection("users")
            .document(userId)
            .collection("notification_reads")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                val result = mutableMapOf<String, Long>()
                snapshot.safeToObjects(NotificationReadReceipt::class.java).forEach { receipt ->
                    val key = receipt.notificationId.ifBlank { receipt.id }
                    if (key.startsWith("smoron_")) {
                        val tsMillis = receipt.readAt?.toDate()?.time ?: System.currentTimeMillis()
                        result[key] = tsMillis
                    }
                }
                result.toMap()
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, readsPath)
                emit(emptyMap())
            }
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
        return db.collection("app_releases")
            .document("latest")
            .snapshots()
            .map { snapshot ->
                snapshot.toAppReleaseInfoOrNull()
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
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
}



