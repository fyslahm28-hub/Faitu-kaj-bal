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
    val blockedUserIds: List<String> = emptyList(),
    val chatThemes: Map<String, String> = emptyMap(),
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
    val postType: String = "JOB_CIRCULAR", // "JOB_CIRCULAR", "SEEKER_APPLICATION", or "GENERAL_POST"
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
    val khedmotCategories: List<String> = emptyList(),
    val customCategory: String = "",
    val imageUrls: List<String> = emptyList(),
    val mediaTypes: List<String> = emptyList(), // e.g., ["IMAGE", "VIDEO"]
    val mediaMetadata: List<Map<String, String>> = emptyList(), // url, mediaType, mimeType, sizeBytes, fileName, storagePath
    val attachmentDocId: String = "", // Optional reference to /feed_post_attachments/{attachmentId}
    val userReactions: Map<String, String> = emptyMap(),
    val userReactionNames: Map<String, String> = emptyMap(),
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val sharedPostId: String = "",
    val sharedAuthorId: String = "",
    val sharedAuthorName: String = "",
    val sharedTitle: String = "",
    val sharedDescription: String = "",
    val sharedPostType: String = "",
    @get:PropertyName("isEdited")
    @set:PropertyName("isEdited")
    var isEdited: Boolean = false,
    val visibility: String = "PUBLIC", // "PUBLIC" or "ARCHIVED"
    val approvalStatus: String = "APPROVED", // "APPROVED", "PENDING", "REJECTED"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeedPostExtendedAttachment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val attachmentType: String = "EXTENDED_CONTENT", // "EXTENDED_CONTENT", "IMAGE", "VIDEO"
    val mimeType: String = "text/plain",
    val fileName: String = "",
    val downloadUrl: String = "",
    val extendedText: String = "",
    val mediaDataUrl: String = "",
    val sizeBytes: Int = 0,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FeedComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val parentCommentId: String = "",
    val replyToAuthorName: String = "",
    @get:PropertyName("isEdited")
    @set:PropertyName("isEdited")
    var isEdited: Boolean = false,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object KhedmotCategoryHelper {
    const val CAT_NOORANI = "নূরানী"
    const val CAT_NAZERA = "নাজেরা"
    const val CAT_DARSIYAT = "দরসিয়াত"
    const val CAT_IMAM = "ইমাম"
    const val CAT_MUAZZIN = "মুয়াজ্জিন"
    const val CAT_KHATIB = "খতিব"
    const val CAT_OTHER = "অন্যান্য"

    val allCategories: List<String> = listOf(
        CAT_NOORANI,
        CAT_NAZERA,
        CAT_DARSIYAT,
        CAT_IMAM,
        CAT_MUAZZIN,
        CAT_KHATIB,
        CAT_OTHER
    )

    fun resolveDisplayBadges(post: FeedPost): List<String> {
        val badges = mutableListOf<String>()
        post.khedmotCategories.forEach { cat ->
            if (cat == CAT_OTHER) {
                val custom = post.customCategory.trim()
                if (custom.isNotBlank()) {
                    badges.add("$CAT_OTHER: $custom")
                } else {
                    badges.add(CAT_OTHER)
                }
            } else if (cat.isNotBlank()) {
                badges.add(cat.trim())
            }
        }
        if (post.customCategory.isNotBlank() && CAT_OTHER !in post.khedmotCategories) {
            badges.add("$CAT_OTHER: ${post.customCategory.trim()}")
        }
        return badges.distinct()
    }

    fun buildCombinedDesignation(
        manualDesignation: String,
        selectedCategories: List<String>,
        customCategory: String
    ): String {
        val parts = mutableListOf<String>()
        selectedCategories.forEach { cat ->
            if (cat == CAT_OTHER) {
                if (customCategory.isNotBlank()) parts.add(customCategory.trim())
            } else if (cat.isNotBlank()) {
                parts.add(cat.trim())
            }
        }
        val manual = manualDesignation.trim()
        if (manual.isNotBlank() && parts.none { it.equals(manual, ignoreCase = true) }) {
            parts.add(0, manual)
        }
        return parts.distinct().joinToString(", ")
    }

    fun matchesCategoryFilter(post: FeedPost, selectedCategory: String): Boolean {
        if (selectedCategory.isBlank() || selectedCategory == "সকল বিভাগ") return true
        if (selectedCategory in post.khedmotCategories) return true
        if (selectedCategory == CAT_OTHER && post.customCategory.isNotBlank()) return true
        val combinedText = "${post.designation} ${post.title} ${post.description} ${post.customCategory}"
        return combinedText.contains(selectedCategory, ignoreCase = true)
    }
}

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
data class ChatGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val memberIds: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val groupMemberIds: List<String> = emptyList(),
    val subject: String = "",
    val body: String = "",
    val imageUrls: List<String> = emptyList(),
    @field:PropertyName("isRead")
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val readByUserIds: List<String> = emptyList(),
    val senderReaction: String = "",
    val receiverReaction: String = "",
    val userReactions: Map<String, String> = emptyMap(),
    @field:PropertyName("isEdited")
    @get:PropertyName("isEdited")
    @set:PropertyName("isEdited")
    var isEdited: Boolean = false,
    @ServerTimestamp val editedAt: Timestamp? = null,
    @field:PropertyName("deletedForSender")
    @get:PropertyName("deletedForSender")
    @set:PropertyName("deletedForSender")
    var deletedForSender: Boolean = false,
    @field:PropertyName("deletedForReceiver")
    @get:PropertyName("deletedForReceiver")
    @set:PropertyName("deletedForReceiver")
    var deletedForReceiver: Boolean = false,
    val deletedForUserIds: List<String> = emptyList(),
    @field:PropertyName("isDeletedForEveryone")
    @get:PropertyName("isDeletedForEveryone")
    @set:PropertyName("isDeletedForEveryone")
    var isDeletedForEveryone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun isVisibleForUser(userId: String): Boolean {
        if (userId.isBlank()) return true
        if (userId == senderId && deletedForSender) return false
        if (userId == receiverId && deletedForReceiver) return false
        if (deletedForUserIds.contains(userId)) return false
        return true
    }

    fun isUnreadForUser(userId: String): Boolean {
        if (userId.isBlank() || senderId == userId || !isVisibleForUser(userId)) return false
        val isGroupMsg = groupId.isNotBlank() || receiverId.startsWith("GROUP:", ignoreCase = true)
        return if (isGroupMsg) {
            !readByUserIds.contains(userId)
        } else {
            receiverId == userId && !isRead && !readByUserIds.contains(userId)
        }
    }

    fun resolvedConversationKey(currentUserId: String): String {
        return if (groupId.isNotBlank()) {
            "GROUP:$groupId"
        } else if (receiverId.startsWith("GROUP:", ignoreCase = true)) {
            receiverId
        } else if (senderId == currentUserId) {
            receiverId
        } else {
            senderId
        }
    }

    fun mergedUserReactions(): Map<String, String> {
        val merged = mutableMapOf<String, String>()
        if (senderId.isNotBlank() && senderReaction.isNotBlank()) {
            merged[senderId] = senderReaction
        }
        if (receiverId.isNotBlank() && !receiverId.startsWith("GROUP:") && receiverReaction.isNotBlank()) {
            merged[receiverId] = receiverReaction
        }
        userReactions.forEach { (uid, code) ->
            if (uid.isNotBlank()) {
                if (code.isBlank()) {
                    merged.remove(uid)
                } else {
                    merged[uid] = code
                }
            }
        }
        return merged
    }

    fun parsedReplyQuote(): ReplyQuoteInfo? {
        return ReplyQuoteInfo.fromSubject(subject)
    }

    fun displaySubject(): String {
        return ReplyQuoteInfo.stripReplyPrefix(subject)
    }
}

data class ReplyQuoteInfo(
    val replyToMessageId: String,
    val replyToSenderName: String,
    val replyToSnippet: String
) {
    companion object {
        private const val PREFIX = "REPLY|"

        fun encodeSubject(
            replyToMessageId: String,
            replyToSenderName: String,
            replyToSnippet: String
        ): String {
            val safeId = replyToMessageId.replace("|", "").trim().take(36)
            val safeName = replyToSenderName.replace("|", " ").replace("\n", " ").trim().ifBlank { "ব্যবহারকারী" }.take(40)
            val safeSnippet = replyToSnippet.replace("|", " ").replace("\n", " ").trim().ifBlank { "পয়গাম" }.take(95)
            return "$PREFIX$safeId|$safeName|$safeSnippet".take(195)
        }

        fun fromSubject(subject: String): ReplyQuoteInfo? {
            if (!subject.startsWith(PREFIX)) return null
            val parts = subject.removePrefix(PREFIX).split("|", limit = 3)
            if (parts.size < 3) return null
            return ReplyQuoteInfo(
                replyToMessageId = parts[0].trim(),
                replyToSenderName = parts[1].trim().ifBlank { "ব্যবহারকারী" },
                replyToSnippet = parts[2].trim().ifBlank { "পয়গাম" }
            )
        }

        fun stripReplyPrefix(subject: String): String {
            return if (subject.startsWith(PREFIX)) "খেদমত সংক্রান্ত যোগাযোগ" else subject
        }
    }
}

data class PaygamReactionOption(
    val code: String,
    val emoji: String,
    val banglaTitle: String
)

enum class IslamicReaction(
    val code: String,
    val emoji: String,
    val banglaTitle: String
) {
    SAMMATI("SAMMATI", "👍", "সম্মতি / লাইক"),
    MUHABBAT("MUHABBAT", "❤️", "ভালোবাসা"),
    HAHA("HAHA", "😂", "আনন্দ"),
    WOW("WOW", "😮", "বিস্ময়"),
    SAD("SAD", "😢", "সমবেদনা"),
    PRAY("PRAY", "🙏", "কৃতজ্ঞতা / দোয়া"),
    DUA("DUA", "🤲", "দোয়া"),
    HILAL("HILAL", "🌙", "চাঁদ"),
    SUNDOR("SUNDOR", "✨", "মাশাআল্লাহ"),
    DEENI_PRERONA("DEENI_PRERONA", "🕌", "দ্বীনি অনুপ্রেরণা");

    val titleBangla: String
        get() = banglaTitle

    companion object {
        val primaryBarOptions: List<IslamicReaction> = listOf(
            SAMMATI,
            MUHABBAT,
            HAHA,
            WOW,
            SAD,
            PRAY
        )

        val extendedOptions: List<IslamicReaction> = listOf(
            SAMMATI,
            MUHABBAT,
            HAHA,
            WOW,
            SAD,
            PRAY,
            DUA,
            SUNDOR,
            HILAL,
            DEENI_PRERONA
        )

        fun fromCode(code: String): IslamicReaction? {
            val trimmed = code.trim()
            if (trimmed.isBlank()) return null
            entries.firstOrNull { it.code.equals(trimmed, ignoreCase = true) || it.emoji == trimmed }?.let { return it }
            return when (trimmed.uppercase()) {
                "LIKE", "TAYYIB" -> SAMMATI
                "LOVE" -> MUHABBAT
                "MUBARAK" -> HILAL
                "MASHALLAH" -> SUNDOR
                "SHUKRAN" -> PRAY
                else -> null
            }
        }

        fun resolveEmoji(codeOrEmoji: String): String {
            val trimmed = codeOrEmoji.trim()
            if (trimmed.isBlank()) return ""
            return fromCode(trimmed)?.emoji ?: trimmed
        }

        fun resolveBanglaTitle(codeOrEmoji: String): String {
            val trimmed = codeOrEmoji.trim()
            if (trimmed.isBlank()) return ""
            return fromCode(trimmed)?.banglaTitle ?: "অভিবাদন"
        }
    }
}

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

@IgnoreExtraProperties
data class AppReleaseInfo(
    @DocumentId val id: String = "",
    val releaseId: String = "latest",
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val apkDownloadUrl: String = "",
    val releaseNotes: String = "",
    val commitSha: String = "",
    val githubReleaseTag: String = "",
    @field:PropertyName("isMandatory")
    @get:PropertyName("isMandatory")
    @set:PropertyName("isMandatory")
    var isMandatory: Boolean = false,
    @field:PropertyName("isPublished")
    @get:PropertyName("isPublished")
    @set:PropertyName("isPublished")
    var isPublished: Boolean = true,
    val publishedBy: String = "",
    @ServerTimestamp val publishedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

