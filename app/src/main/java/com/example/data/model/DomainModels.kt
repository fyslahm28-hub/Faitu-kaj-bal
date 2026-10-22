package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AdminRegistryEntry(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val email: String = "",
    val adminPhone: String = "01300448147",
    val role: String = "SUPER_ADMIN",
    @get:PropertyName("verifiedByAuthToken")
    @set:PropertyName("verifiedByAuthToken")
    var verifiedByAuthToken: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TelegramNotificationLog(
    @DocumentId val id: String = "",
    val eventType: String = "NEW_CIRCULAR",
    val title: String = "",
    val message: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val targetPhone: String = "01300448147",
    val referenceId: String = "",
    val deliveryStatus: String = "QUEUED", // "DELIVERED", "QUEUED", "FAILED"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val accountType: String = "khadim", // "khadim" (খেদমতপ্রার্থী/মুদাররিস) or "muhtamim" (মাদরাসা কর্তৃপক্ষ/মুহতামিম)
    val district: String = "",
    val designation: String = "",
    val qualification: String = "",
    val experience: String = "",
    val preferredDistricts: String = "",
    val bio: String = "",
    val contactInfo: String = "",
    val madrasaName: String = "",
    val photoUrl: String = "", // Base64 data:image/jpeg;base64,... or avatar preset / URL stored in Firestore
    @field:PropertyName("isOnline")
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    val typingToUserId: String = "",
    @ServerTimestamp val typingUpdatedAt: Timestamp? = null,
    @field:PropertyName("isVerified")
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeedPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val postType: String = "JOB_CIRCULAR", // "JOB_CIRCULAR" (নিয়োগ বিজ্ঞপ্তি) or "SEEKER_APPLICATION" (চাকরিপ্রার্থীর আবেদন)
    val title: String = "",
    val district: String = "",
    val upazilaOrLocation: String = "",
    val designation: String = "",
    val qualification: String = "",
    val salaryOrExperience: String = "",
    val deadlineOrPreferredDistricts: String = "",
    val description: String = "",
    val contactNumber: String = "",
    val madrasaId: String = "",
    @get:PropertyName("isVerifiedMadrasa")
    @set:PropertyName("isVerifiedMadrasa")
    var isVerifiedMadrasa: Boolean = false,
    @get:PropertyName("isBoosted")
    @set:PropertyName("isBoosted")
    var isBoosted: Boolean = false,
    val visibility: String = "PUBLIC", // "PUBLIC" or "ARCHIVED"
    val approvalStatus: String = "APPROVED", // "APPROVED", "PENDING", "REJECTED"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MadrasaProfile(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val district: String = "",
    val address: String = "",
    val establishedYear: String = "",
    val highestMarhala: String = "",
    val facilities: String = "",
    val studentCount: String = "",
    val contactPhone: String = "",
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class KhedmotSubmission(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postTitle: String = "",
    val postDesignation: String = "",
    val applicantId: String = "",
    val applicantName: String = "",
    val applicantQualification: String = "",
    val applicantExperience: String = "",
    val applicantPhone: String = "",
    val coverNote: String = "",
    val recipientId: String = "",
    val status: String = "PENDING", // "PENDING", "SHORTLISTED", "ACCEPTED", "REJECTED"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookmarkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val postType: String = "JOB_CIRCULAR",
    val title: String = "",
    val district: String = "",
    val designation: String = "",
    val qualification: String = "",
    val salaryOrExperience: String = "",
    val deadlineOrPreferredDistricts: String = "",
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val subject: String = "",
    val body: String = "",
    @field:PropertyName("isRead")
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "", // Specific userId or "ALL_USERS"
    val senderId: String = "",
    val senderName: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "APPLICATION", // "APPLICATION", "MESSAGE", "VERIFICATION", "NEW_CIRCULAR", "NEW_APPLICATION", "ANNOUNCEMENT", "BOOST"
    val referenceId: String = "",
    @field:PropertyName("isRead")
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationReadReceipt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val notificationId: String = "",
    @ServerTimestamp val readAt: Timestamp? = null
)
