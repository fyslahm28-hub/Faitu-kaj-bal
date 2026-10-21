package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.BookmarkItem
import com.example.data.model.DirectMessage
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotSubmission
import com.example.data.model.MadrasaProfile
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("এটি সম্পন্ন করতে গুগল অ্যাকাউন্ট দিয়ে লগইন করা আবশ্যক।")
    }

    // --- 1. USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
            val snap = db.collection("users").document(userId).get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to profile.fullName.ifBlank { "আবদুল্লাহ" },
                    "accountType" to profile.accountType.ifBlank { "khadim" },
                    "district" to profile.district,
                    "designation" to profile.designation,
                    "qualification" to profile.qualification,
                    "experience" to profile.experience,
                    "preferredDistricts" to profile.preferredDistricts,
                    "bio" to profile.bio,
                    "contactInfo" to profile.contactInfo,
                    "madrasaName" to profile.madrasaName,
                    "isVerified" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "fullName" to profile.fullName.ifBlank { "আবদুল্লাহ" },
                    "accountType" to profile.accountType.ifBlank { "khadim" },
                    "district" to profile.district,
                    "designation" to profile.designation,
                    "qualification" to profile.qualification,
                    "experience" to profile.experience,
                    "preferredDistricts" to profile.preferredDistricts,
                    "bio" to profile.bio,
                    "contactInfo" to profile.contactInfo,
                    "madrasaName" to profile.madrasaName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 2. FEED POSTS (নিয়োগ বিজ্ঞপ্তি ও চাকরিপ্রার্থীর আবেদন) ---

    fun observePublicFeedPosts(): Flow<List<FeedPost>> {
        val path = "feed_posts"
        return db.collection(path)
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FeedPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
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
        isVerifiedMadrasa: Boolean = false
    ): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("feed_posts")
        val docRef = collectionRef.document()
        return try {
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to authorName.trim(),
                "postType" to postType,
                "title" to title.trim(),
                "district" to district.trim(),
                "upazilaOrLocation" to upazilaOrLocation.trim(),
                "designation" to designation.trim(),
                "qualification" to qualification.trim(),
                "salaryOrExperience" to salaryOrExperience.trim(),
                "deadlineOrPreferredDistricts" to deadlineOrPreferredDistricts.trim(),
                "description" to description.trim(),
                "contactNumber" to contactNumber.trim(),
                "madrasaId" to madrasaId,
                "isVerifiedMadrasa" to isVerifiedMadrasa,
                "visibility" to "PUBLIC",
                "approvalStatus" to "APPROVED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteFeedPost(postId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("feed_posts").document(postId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdatePostVerification(postId: String, isVerified: Boolean, approvalStatus: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("feed_posts").document(postId)
        return try {
            docRef.update(
                mapOf(
                    "isVerifiedMadrasa" to isVerified,
                    "approvalStatus" to approvalStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
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
                snapshot.toObjects(BookmarkItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
                docRef.delete().await()
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
                docRef.set(payload).await()
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
                snapshot.toObjects(MadrasaProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminToggleMadrasaVerification(madrasaId: String, verified: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection("madrasas").document(madrasaId)
        return try {
            docRef.update(
                mapOf(
                    "isVerified" to verified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 5. KHEDMOT SUBMISSIONS (খেদমতে সরাসরি আবেদন) ---

    fun observeMySentSubmissions(userId: String): Flow<List<KhedmotSubmission>> {
        val path = "khedmot_submissions"
        return db.collection(path)
            .whereEqualTo("applicantId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(KhedmotSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
                snapshot.toObjects(KhedmotSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
            docRef.set(payload).await()

            // Deliver real-time notification to the Madrasa / circular author
            if (post.authorId.isNotBlank() && post.authorId != uid) {
                createNotification(
                    recipientId = post.authorId,
                    senderName = applicantName.trim(),
                    title = "নতুন খেদমতের আবেদন প্রাপ্তি",
                    message = "${applicantName.trim()} আপনার '${post.title}'-এর '${post.designation}' খেদমতের জন্য আবেদন করেছেন।",
                    type = "APPLICATION",
                    referenceId = docRef.id
                )
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
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
            ).await()

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

    // --- 6. DIRECT MESSAGING (মেসেজিং ও মুযাকারা) ---

    fun observeUserMessages(userId: String): Flow<List<DirectMessage>> {
        val path = "messages"
        val sentFlow = db.collection(path)
            .whereEqualTo("senderId", userId)
            .snapshots()
            .map { it.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }

        val receivedFlow = db.collection(path)
            .whereEqualTo("receiverId", userId)
            .snapshots()
            .map { it.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }

        return combine(sentFlow, receivedFlow) { sent, received ->
            (sent + received)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    suspend fun sendDirectMessage(
        senderName: String,
        receiverId: String,
        receiverName: String,
        subject: String,
        body: String
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("messages").document()
        return try {
            val payload = mapOf(
                "senderId" to uid,
                "senderName" to senderName.trim(),
                "receiverId" to receiverId,
                "receiverName" to receiverName.trim(),
                "subject" to subject.trim(),
                "body" to body.trim(),
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            if (receiverId.isNotBlank() && receiverId != uid) {
                createNotification(
                    recipientId = receiverId,
                    senderName = senderName.trim(),
                    title = "নতুন পয়গাম: ${senderName.trim()}",
                    message = body.trim().take(120),
                    type = "MESSAGE",
                    referenceId = docRef.id
                )
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("messages").document(messageId)
        return try {
            docRef.update("isRead", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 7. NOTIFICATIONS (নোটিফিকেশন) ---

    fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        val path = "notifications"
        return db.collection(path)
            .whereEqualTo("recipientId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
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
        val docRef = db.collection("notifications").document()
        return try {
            val payload = mapOf(
                "recipientId" to recipientId,
                "senderId" to uid,
                "senderName" to senderName.trim(),
                "title" to title.trim(),
                "message" to message.trim(),
                "type" to type,
                "referenceId" to referenceId,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("notifications").document(notificationId)
        return try {
            docRef.update("isRead", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
