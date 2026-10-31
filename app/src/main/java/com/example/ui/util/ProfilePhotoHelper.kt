package com.example.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ProfilePhotoHelper {

    const val MAX_CHAT_IMAGES_PER_MESSAGE = 5

    const val PRESET_SCHOLAR_EMERALD = "preset:scholar_emerald"
    const val PRESET_SCHOLAR_GOLD = "preset:scholar_gold"
    const val PRESET_MUHTAMIM_ROYAL = "preset:muhtamim_royal"
    const val PRESET_MADRASA_DOME = "preset:madrasa_dome"

    val presetAvatars = listOf(
        PRESET_SCHOLAR_EMERALD to "ইলমি সবুজ",
        PRESET_SCHOLAR_GOLD to "সোনালি মিনার",
        PRESET_MUHTAMIM_ROYAL to "দাপ্তরিক নীল",
        PRESET_MADRASA_DOME to "মাদরাসা গম্বুজ"
    )

    const val FIRESTORE_DATABASE_ID = "ai-studio-android-811ea734-e9e8-4ff4-8e56-ca97ec8a429c"
    private val CANDIDATE_STORAGE_BUCKETS = listOf(
        "", // Default FirebaseStorage.getInstance()
        "gs://gen-lang-client-0048059944.firebasestorage.app",
        "gs://gen-lang-client-0048059944.appspot.com"
    )

    // In-memory cache mapping attachmentDocId -> data:image/jpeg;base64,... for instant cross-screen rendering
    private val attachmentDataUrlCache = ConcurrentHashMap<String, String>()

    fun cacheAttachmentDataUrl(attachmentDocId: String, dataUrl: String) {
        if (attachmentDocId.isNotBlank() && dataUrl.isNotBlank()) {
            attachmentDataUrlCache[attachmentDocId] = dataUrl
        }
    }

    fun getCachedAttachmentDataUrl(attachmentDocId: String): String? {
        if (attachmentDocId.isBlank()) return null
        return attachmentDataUrlCache[attachmentDocId]
    }

    fun extractAttachmentDocId(url: String): String? {
        if (url.isBlank()) return null
        val marker = "attachmentDocId="
        val idx = url.indexOf(marker)
        if (idx == -1) return null
        val after = url.substring(idx + marker.length)
        val endIdx = after.indexOf('&').let { if (it == -1) after.length else it }
        return after.substring(0, endIdx).trim().takeIf { it.isNotBlank() }
    }

    /**
     * Fetches the offloaded image data URL from `/feed_post_attachments/{attachmentDocId}`
     * when a post's media URL contains `attachmentDocId=...`, caches it in memory, and decodes it.
     */
    suspend fun fetchAndDecodeAttachmentBitmap(
        attachmentDocId: String,
        firestoreInstance: FirebaseFirestore? = null
    ): ImageBitmap? {
        if (attachmentDocId.isBlank()) return null
        val cached = attachmentDataUrlCache[attachmentDocId]
        if (!cached.isNullOrBlank()) {
            decodeDataUrlToImageBitmap(cached)?.let { return it }
        }
        val dataUrl = withTimeoutOrNull(6000L) {
            try {
                val db = firestoreInstance ?: Firebase.firestore(FIRESTORE_DATABASE_ID)
                val snap = db.collection("feed_post_attachments")
                    .document(attachmentDocId)
                    .get()
                    .await()
                if (snap.exists()) {
                    snap.getString("mediaDataUrl")?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        } ?: return null

        attachmentDataUrlCache[attachmentDocId] = dataUrl
        return decodeDataUrlToImageBitmap(dataUrl)
    }

    /**
     * Converts a picked image [Uri] from Android Photo Picker into a compact,
     * center-cropped 256x256 JPEG Base64 data URI suitable for Cloud Firestore storage
     * and instant cross-user visibility.
     */
    fun encodeUriToDataUrl(context: Context, uri: Uri, targetSizePx: Int = 256): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val cropped = centerCropAndScale(original, targetSizePx)
            encodeBitmapToDataUrl(cropped, quality = 76)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a picked attachment image [Uri] into a high-definition JPEG Base64 data URL
     * preserving aspect ratio and fine typography clarity so text in documents, job circulars,
     * and screenshots remains sharp and readable while strictly respecting [maxDimensionPx].
     */
    fun encodeChatAttachmentUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 1440,
        quality: Int = 84
    ): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
            val scaled = scalePreservingAspectRatioForTextClarity(original, maxDimensionPx)
            val encoded = encodeBitmapToDataUrl(scaled, quality = quality)
            // Always guarantee every single encoded image fits safely within the single-attachment budget
            ensureDataUrlFitsBudget(encoded, MAX_ATTACHMENT_DOC_BASE64_CHARS)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Processes a list of picked [Uri]s from the Photo Picker, merging with any existing
     * selected images and capping strictly at [MAX_CHAT_IMAGES_PER_MESSAGE] (5).
     */
    fun processSelectedChatImages(
        context: Context,
        uris: List<Uri>,
        existingImages: List<String> = emptyList()
    ): List<String> {
        val remainingSlots = (MAX_CHAT_IMAGES_PER_MESSAGE - existingImages.size).coerceAtLeast(0)
        if (remainingSlots == 0) return existingImages.take(MAX_CHAT_IMAGES_PER_MESSAGE)
        val totalPlannedCount = (existingImages.size + min(uris.size, remainingSlots))
            .coerceIn(1, MAX_CHAT_IMAGES_PER_MESSAGE)
        val targetMaxDim = when (totalPlannedCount) {
            1 -> 1440
            2 -> 1280
            else -> 1120
        }
        val targetQuality = when (totalPlannedCount) {
            1 -> 84
            2 -> 80
            else -> 76
        }
        val newlyEncoded = uris.take(remainingSlots).mapNotNull { uri ->
            encodeChatAttachmentUriToDataUrl(
                context = context,
                uri = uri,
                maxDimensionPx = targetMaxDim,
                quality = targetQuality
            )
        }
        return (existingImages + newlyEncoded).take(MAX_CHAT_IMAGES_PER_MESSAGE)
    }

    const val FIRESTORE_HARD_LIMIT_BYTES = 1_048_576 // 1 MiB Firestore document hard limit
    const val SAFE_FEED_POST_MAX_BYTES = 850_000 // Safe pre-write ceiling (~830 KiB)
    const val MAX_SINGLE_INLINE_FIELD_BYTES = 240_000
    const val MAX_INLINE_AVATAR_CHARS_IN_POST = 45_000
    const val MAX_INLINE_FEED_IMAGE_PREVIEW_CHARS = 70_000
    const val MAX_ATTACHMENT_DOC_BASE64_CHARS = 620_000 // Strictly <= 750,000 rule limit & < 1 MiB doc limit
    const val MAX_OFFLOADABLE_ATTACHMENT_CHARS = 700_000

    const val OVERSIZED_POST_ERROR_BANGLA =
        "পোস্টের ডেটা বা সংযুক্ত ফাইলের আকার নির্ধারিত সীমা (১ মেগাবাইট) অতিক্রম করেছে। বড় ছবি বা ভিডিও সরাসরি পোস্টে সংরক্ষণ না করে ছোট আকারের ফাইল বা সংক্ষিপ্ত বিবরণ ব্যবহার করুন।"

    data class UploadedFeedMediaItem(
        val url: String,
        val mediaType: String, // "IMAGE" or "VIDEO"
        val mimeType: String,
        val sizeBytes: Int,
        val fileName: String,
        val storagePath: String = "",
        val separateAttachmentDataUrl: String = ""
    ) {
        fun toMetadataMap(attachmentDocId: String = ""): Map<String, String> {
            val map = mutableMapOf(
                "url" to url,
                "mediaType" to mediaType,
                "mimeType" to mimeType,
                "sizeBytes" to sizeBytes.toString(),
                "fileName" to fileName
            )
            if (storagePath.isNotBlank()) {
                map["storagePath"] = storagePath
            }
            if (attachmentDocId.isNotBlank()) {
                map["attachmentDocId"] = attachmentDocId
            }
            return map
        }
    }

    // Cooldown timestamp when Firebase Storage is unprovisioned/unreachable so multi-image posts & chats never stall repeatedly
    private val storageUnavailableUntilMs = java.util.concurrent.atomic.AtomicLong(0L)

    /**
     * Attempts to upload raw bytes to Firebase Storage across default and project-specific bucket URLs.
     * Returns the public/authenticated download URL if Firebase Storage is provisioned and reachable,
     * or `null` immediately if Firebase Storage is offline/unprovisioned.
     */
    private suspend fun tryUploadBytesToFirebaseStorage(
        storagePath: String,
        bytes: ByteArray,
        mimeType: String,
        customMetadata: Map<String, String> = emptyMap()
    ): String? {
        if (bytes.isEmpty()) return null
        val now = System.currentTimeMillis()
        if (now < storageUnavailableUntilMs.get()) {
            return null
        }
        val uploaded = try {
            withTimeoutOrNull(2200L) {
                for (bucketUrl in CANDIDATE_STORAGE_BUCKETS) {
                    try {
                        val storage = if (bucketUrl.isBlank()) {
                            FirebaseStorage.getInstance()
                        } else {
                            FirebaseStorage.getInstance(bucketUrl)
                        }
                        storage.maxUploadRetryTimeMillis = 1800L
                        storage.maxOperationRetryTimeMillis = 1800L
                        val storageRef = storage.reference.child(storagePath)
                        val metaBuilder = StorageMetadata.Builder().setContentType(mimeType)
                        customMetadata.forEach { (k, v) -> metaBuilder.setCustomMetadata(k, v) }
                        val task = storageRef.putBytes(bytes, metaBuilder.build())
                        task.await()
                        val downloadUrl = storageRef.downloadUrl.await()?.toString()
                        if (!downloadUrl.isNullOrBlank()) {
                            storageUnavailableUntilMs.set(0L)
                            return@withTimeoutOrNull downloadUrl
                        }
                    } catch (_: Throwable) {
                        // Ignore and try next candidate or fall back locally
                    }
                }
                null
            }
        } catch (_: Throwable) {
            null
        }
        if (uploaded.isNullOrBlank()) {
            storageUnavailableUntilMs.set(System.currentTimeMillis() + 10 * 60 * 1000L)
        }
        return uploaded
    }

    /**
     * Encodes a picked media [Uri] (image or video) from Android Photo Picker for feed posts.
     * For images, scales and compresses cleanly within safe dimensions; for videos, reads payload
     * so it can be uploaded to Firebase Storage via [uploadFeedMediaToFirebaseStorage].
     */
    fun encodeFeedMediaUri(
        context: Context,
        uri: Uri,
        existingCount: Int = 0
    ): Result<String> {
        return try {
            val mimeType = context.contentResolver.getType(uri)?.lowercase().orEmpty()
            if (mimeType.startsWith("video/")) {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return Result.failure(IllegalArgumentException("ভিডিও ফাইল পড়া সম্ভব হয়নি।"))
                if (bytes.size > 15 * 1024 * 1024) {
                    return Result.failure(
                        IllegalArgumentException("ভিডিও ফাইলের আকার ১৫ মেগাবাইটের বেশি হওয়ায় আপলোড করা যাবে না।")
                    )
                }
                val extMime = mimeType.ifBlank { "video/mp4" }
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Result.success("data:$extMime;base64,$base64")
            } else {
                val targetDim = when {
                    existingCount <= 0 -> 1440
                    existingCount == 1 -> 1280
                    else -> 1120
                }
                val targetQuality = when {
                    existingCount <= 0 -> 84
                    existingCount == 1 -> 80
                    else -> 76
                }
                val encoded = encodeChatAttachmentUriToDataUrl(
                    context = context,
                    uri = uri,
                    maxDimensionPx = targetDim,
                    quality = targetQuality
                ) ?: return Result.failure(IllegalArgumentException("ছবি প্রক্রিয়াকরণ করা সম্ভব হয়নি।"))
                Result.success(encoded)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.localizedMessage ?: "ফাইল যুক্ত করা সম্ভব হয়নি।"))
        }
    }

    /**
     * Uploads feed post images and videos to Firebase Storage (`feed_posts/{authorId}/{postId}/...`)
     * and returns compact download URLs + file type (`IMAGE` / `VIDEO`) + metadata.
     *
     * Key guarantee:
     * 1. NEVER stores raw Base64 image/video strings inside `/feed_posts/{postId}.imageUrls`.
     * 2. If Firebase Storage succeeds, `url` is the Firebase Storage download URL and `separateAttachmentDataUrl` is empty.
     * 3. If Firebase Storage is unavailable/unprovisioned, `separateAttachmentDataUrl` is compacted to
     *    strictly fit within [MAX_ATTACHMENT_DOC_BASE64_CHARS] (`<= 620,000` chars, well below the 1 MiB
     *    Firestore limit) and stored in its own `/feed_post_attachments/{postId}_media_{index}` document,
     *    while `/feed_posts/{postId}.imageUrls` receives a compact ~175-byte reference URL containing
     *    `attachmentDocId={postId}_media_{index}`.
     */
    suspend fun uploadFeedMediaToFirebaseStorage(
        mediaItems: List<String>,
        authorId: String,
        postId: String
    ): Result<List<UploadedFeedMediaItem>> {
        val cleaned = mediaItems.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_CHAT_IMAGES_PER_MESSAGE)
        if (cleaned.isEmpty()) return Result.success(emptyList())

        val safeAuthor = authorId.ifBlank { "user" }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val safePostId = postId.ifBlank { "post_${System.currentTimeMillis()}" }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val results = mutableListOf<UploadedFeedMediaItem>()

        for ((index, item) in cleaned.withIndex()) {
            val itemUtf8Bytes = item.toByteArray(Charsets.UTF_8).size
            // Reject any single raw non-data/non-URL payload that exceeds inline field limits
            if (!item.startsWith("http://", ignoreCase = true) &&
                !item.startsWith("https://", ignoreCase = true) &&
                !item.startsWith("data:image/", ignoreCase = true) &&
                !item.startsWith("data:video/", ignoreCase = true) &&
                !item.startsWith("content://", ignoreCase = true) &&
                !item.startsWith("file://", ignoreCase = true) &&
                !item.startsWith("storage://", ignoreCase = true) &&
                itemUtf8Bytes > MAX_INLINE_FEED_IMAGE_PREVIEW_CHARS
            ) {
                return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
            }

            // 1. Already a remote HTTP/HTTPS URL or storage:// reference
            if (item.startsWith("http://", ignoreCase = true) ||
                item.startsWith("https://", ignoreCase = true) ||
                item.startsWith("storage://", ignoreCase = true)
            ) {
                val lower = item.lowercase()
                val isVideo = lower.contains(".mp4") || lower.contains(".mov") ||
                    lower.contains(".webm") || lower.contains("video")
                val mediaType = if (isVideo) "VIDEO" else "IMAGE"
                val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
                val ext = if (isVideo) "mp4" else "jpg"
                results.add(
                    UploadedFeedMediaItem(
                        url = item.take(1800),
                        mediaType = mediaType,
                        mimeType = mimeType,
                        sizeBytes = itemUtf8Bytes,
                        fileName = "media_${index}.$ext"
                    )
                )
                continue
            }

            // 2. Data URI (data:image/... or data:video/...)
            if (item.startsWith("data:", ignoreCase = true)) {
                val commaIdx = item.indexOf(',')
                if (commaIdx == -1) {
                    return Result.failure(IllegalArgumentException("সংযুক্ত ফাইলের ফরম্যাট সঠিক নয়।"))
                }
                val header = item.substring(5, commaIdx).lowercase()
                val mimeType = header.substringBefore(';').ifBlank { "image/jpeg" }
                val isVideo = mimeType.startsWith("video/")
                val mediaType = if (isVideo) "VIDEO" else "IMAGE"
                val ext = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("webp") -> "webp"
                    mimeType.contains("mp4") -> "mp4"
                    mimeType.contains("webm") -> "webm"
                    isVideo -> "mp4"
                    else -> "jpg"
                }

                // For images, always ensure the Data-URI fits safely within MAX_ATTACHMENT_DOC_BASE64_CHARS
                // before decoding/uploading so even if Firebase Storage is unprovisioned, the attachment doc
                // is guaranteed < 650 KB and never triggers INVALID_ARGUMENT (> 1,048,576 bytes).
                val normalizedItem = if (!isVideo) {
                    ensureDataUrlFitsBudget(item, MAX_ATTACHMENT_DOC_BASE64_CHARS)
                } else {
                    item
                }

                val normCommaIdx = normalizedItem.indexOf(',').takeIf { it != -1 } ?: commaIdx
                val base64Payload = normalizedItem.substring(normCommaIdx + 1)
                val decodedBytes = try {
                    Base64.decode(base64Payload, Base64.DEFAULT)
                } catch (_: Exception) {
                    null
                }
                val rawByteCount = decodedBytes?.size ?: normalizedItem.toByteArray(Charsets.UTF_8).size
                val fileName = "media_${index}_${System.currentTimeMillis()}.$ext"
                val storagePath = "feed_posts/$safeAuthor/$safePostId/$fileName"

                // Attempt upload to Firebase Storage only for video payloads;
                // for images, offload immediately to /feed_post_attachments without blocking on unprovisioned Storage.
                val uploadedDownloadUrl = if (isVideo && decodedBytes != null && decodedBytes.isNotEmpty()) {
                    tryUploadBytesToFirebaseStorage(
                        storagePath = storagePath,
                        bytes = decodedBytes,
                        mimeType = mimeType,
                        customMetadata = mapOf(
                            "authorId" to safeAuthor,
                            "postId" to safePostId,
                            "mediaType" to mediaType
                        )
                    )
                } else {
                    null
                }

                if (!uploadedDownloadUrl.isNullOrBlank()) {
                    results.add(
                        UploadedFeedMediaItem(
                            url = uploadedDownloadUrl,
                            mediaType = mediaType,
                            mimeType = mimeType,
                            sizeBytes = rawByteCount,
                            fileName = fileName,
                            storagePath = storagePath
                        )
                    )
                    continue
                }

                // Fallback when Firebase Storage bucket is not yet provisioned or in local emulator:
                // Never store Base64 inside /feed_posts/{postId}! Instead, offload the bounded payload
                // (<= 620,000 chars) to /feed_post_attachments/{postId}_media_{index} and store only
                // a compact download reference URL with attachmentDocId in /feed_posts/{postId}.imageUrls.
                if (normalizedItem.length > MAX_OFFLOADABLE_ATTACHMENT_CHARS) {
                    return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
                }

                val attachmentDocId = "${safePostId}_media_$index"
                val refUrl = "https://firebasestorage.googleapis.com/v0/b/gen-lang-client-0048059944.firebasestorage.app/o/feed_posts%2F${safeAuthor}%2F${safePostId}%2F$fileName?alt=media&attachmentDocId=$attachmentDocId"
                cacheAttachmentDataUrl(attachmentDocId, normalizedItem)

                results.add(
                    UploadedFeedMediaItem(
                        url = refUrl,
                        mediaType = mediaType,
                        mimeType = mimeType,
                        sizeBytes = rawByteCount,
                        fileName = fileName,
                        storagePath = storagePath,
                        separateAttachmentDataUrl = normalizedItem
                    )
                )
                continue
            }

            // 3. Local URI (content:// or file://)
            val lowerUri = item.lowercase()
            val isVideoUri = lowerUri.contains("video") || lowerUri.endsWith(".mp4") || lowerUri.endsWith(".mov")
            val mediaType = if (isVideoUri) "VIDEO" else "IMAGE"
            val mimeType = if (isVideoUri) "video/mp4" else "image/jpeg"
            val ext = if (isVideoUri) "mp4" else "jpg"
            val fileName = "media_${index}_${System.currentTimeMillis()}.$ext"
            val storagePath = "feed_posts/$safeAuthor/$safePostId/$fileName"
            val uploadedFromUri = if (!isVideoUri || System.currentTimeMillis() < storageUnavailableUntilMs.get()) {
                null
            } else {
                try {
                    withTimeoutOrNull(2200L) {
                        try {
                            val storage = FirebaseStorage.getInstance()
                            storage.maxUploadRetryTimeMillis = 1800L
                            storage.maxOperationRetryTimeMillis = 1800L
                            val storageRef = storage.reference.child(storagePath)
                            val metadata = StorageMetadata.Builder()
                                .setContentType(mimeType)
                                .setCustomMetadata("authorId", safeAuthor)
                                .setCustomMetadata("postId", safePostId)
                                .setCustomMetadata("mediaType", mediaType)
                                .build()
                            val task = storageRef.putFile(Uri.parse(item), metadata)
                            task.await()
                            storageRef.downloadUrl.await()?.toString()
                        } catch (_: Throwable) {
                            null
                        }
                    }
                } catch (_: Throwable) {
                    null
                }
            }
            results.add(
                UploadedFeedMediaItem(
                    url = uploadedFromUri ?: item.take(1000),
                    mediaType = mediaType,
                    mimeType = mimeType,
                    sizeBytes = itemUtf8Bytes,
                    fileName = fileName,
                    storagePath = storagePath
                )
            )
        }

        return Result.success(results)
    }

    /**
     * Synchronously ensures `authorPhotoUrl` stored on `/feed_posts/{postId}` stays compact (`<= 12 KB`)
     * without blocking on unprovisioned Firebase Storage uploads, so feed post publishing completes fast
     * even on slow mobile networks.
     */
    fun uploadOrCompactAuthorPhotoForFeedPost(
        authorPhotoUrl: String,
        @Suppress("UNUSED_PARAMETER") authorId: String = ""
    ): String {
        return compactSenderPhotoForMessage(authorPhotoUrl)
    }

    private const val MAX_INLINE_AVATAR_CHARS_IN_MESSAGE = 12_000

    /**
     * Synchronously compacts `senderPhotoUrl` for direct/group messages without making any
     * Firebase Storage network request. Ensures that 100KB–240KB raw Base64 profile photos
     * never bloat message payloads or cause write timeouts on slow mobile networks.
     */
    fun compactSenderPhotoForMessage(senderPhotoUrl: String): String {
        val trimmed = senderPhotoUrl.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.length <= MAX_INLINE_AVATAR_CHARS_IN_MESSAGE) return trimmed
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("preset:", ignoreCase = true)
        ) {
            return trimmed.take(2000)
        }
        if (trimmed.startsWith("data:image", ignoreCase = true)) {
            val compacted = ensureDataUrlFitsBudget(trimmed, MAX_INLINE_AVATAR_CHARS_IN_MESSAGE)
            return if (compacted.length <= MAX_INLINE_AVATAR_CHARS_IN_MESSAGE) {
                compacted
            } else {
                ""
            }
        }
        return trimmed.take(MAX_INLINE_AVATAR_CHARS_IN_MESSAGE)
    }

    /**
     * Estimates the serialized Firestore document size in bytes before writing.
     * Accounts for document path overhead, field names, UTF-8 string bytes, lists, maps, numbers, booleans, and timestamps.
     */
    fun estimateFirestoreDocumentBytes(
        documentPath: String,
        data: Map<String, Any?>
    ): Int {
        fun estimateValueBytes(value: Any?): Int {
            return when (value) {
                null -> 1
                is Boolean -> 1
                is Number -> 8
                is String -> value.toByteArray(Charsets.UTF_8).size + 2
                is List<*> -> 8 + value.sumOf { estimateValueBytes(it) }
                is Map<*, *> -> 16 + value.entries.sumOf { (k, v) ->
                    val keyBytes = (k?.toString() ?: "").toByteArray(Charsets.UTF_8).size + 2
                    keyBytes + estimateValueBytes(v)
                }
                else -> 16 // Timestamp / FieldValue.serverTimestamp()
            }
        }

        val pathBytes = documentPath.toByteArray(Charsets.UTF_8).size + 32
        val fieldsBytes = data.entries.sumOf { (key, value) ->
            val keyBytes = key.toByteArray(Charsets.UTF_8).size + 2
            keyBytes + estimateValueBytes(value) + 8
        }
        return pathBytes + fieldsBytes
    }

    /**
     * Validates that a Firestore `/feed_posts/{postId}` document payload is safely below the 1 MiB
     * (1,048,576 bytes) limit and does not contain any oversized raw inline blobs before writing.
     */
    fun validateFirestoreDocumentSizeBeforeWrite(
        documentPath: String,
        data: Map<String, Any?>,
        maxAllowedBytes: Int = SAFE_FEED_POST_MAX_BYTES
    ): Result<Int> {
        val estimatedBytes = estimateFirestoreDocumentBytes(documentPath, data)
        if (estimatedBytes > maxAllowedBytes || estimatedBytes >= FIRESTORE_HARD_LIMIT_BYTES) {
            return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
        }
        val imageUrls = (data["imageUrls"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        for (url in imageUrls) {
            val urlBytes = url.toByteArray(Charsets.UTF_8).size
            if (urlBytes > MAX_SINGLE_INLINE_FIELD_BYTES) {
                return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
            }
            if (url.startsWith("data:video/", ignoreCase = true)) {
                return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
            }
        }
        val authorPhoto = (data["authorPhotoUrl"] as? String).orEmpty()
        if (authorPhoto.toByteArray(Charsets.UTF_8).size > MAX_SINGLE_INLINE_FIELD_BYTES) {
            return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
        }
        return Result.success(estimatedBytes)
    }

    /**
     * Validates that a `/feed_post_attachments/{attachmentId}` document is safely below both
     * the Firestore 1 MiB hard limit and the `firestore.rules` 750,000-byte mediaDataUrl limit.
     */
    fun validateAttachmentDocumentSizeBeforeWrite(
        documentPath: String,
        data: Map<String, Any?>
    ): Result<Int> {
        val estimatedBytes = estimateFirestoreDocumentBytes(documentPath, data)
        if (estimatedBytes > SAFE_FEED_POST_MAX_BYTES || estimatedBytes >= FIRESTORE_HARD_LIMIT_BYTES) {
            return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
        }
        val mediaDataUrl = (data["mediaDataUrl"] as? String).orEmpty()
        if (mediaDataUrl.length > 740_000) {
            return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
        }
        return Result.success(estimatedBytes)
    }

    /**
     * Compacts up to 5 chat images into a fast, mobile-network-friendly Firestore document budget
     * without blocking on unprovisioned Firebase Storage buckets.
     */
    suspend fun uploadChatImagesToFirebaseStorage(
        imageItems: List<String>,
        @Suppress("UNUSED_PARAMETER") senderId: String
    ): List<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val capped = imageItems.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_CHAT_IMAGES_PER_MESSAGE)
        if (capped.isEmpty()) return@withContext emptyList()
        val maxBase64CharsPerImage = (160_000 / capped.size.coerceAtLeast(1)).coerceIn(60_000, 125_000)

        coroutineScope {
            capped.map { item ->
                async {
                    if (!item.startsWith("data:image", ignoreCase = true)) {
                        item
                    } else {
                        ensureDataUrlFitsBudget(item, maxBase64CharsPerImage)
                    }
                }
            }.map { it.await() }
        }
    }

    fun saveImageToDevice(context: Context, imageSource: String, index: Int = 1): Result<String> {
        if (imageSource.isBlank()) return Result.failure(IllegalArgumentException("ছবি পাওয়া যায়নি"))
        return try {
            val resolvedSource = extractAttachmentDocId(imageSource)?.let { docId ->
                attachmentDataUrlCache[docId] ?: imageSource
            } ?: imageSource

            val bytes: ByteArray = when {
                resolvedSource.startsWith("data:image") -> {
                    val commaIdx = resolvedSource.indexOf(',')
                    if (commaIdx == -1) return Result.failure(IllegalArgumentException("ছবির ফরম্যাট সঠিক নয়"))
                    Base64.decode(resolvedSource.substring(commaIdx + 1), Base64.DEFAULT)
                }
                resolvedSource.startsWith("http://") || resolvedSource.startsWith("https://") -> {
                    URL(resolvedSource).openStream().use { it.readBytes() }
                }
                else -> return Result.failure(IllegalArgumentException("ছবি সংরক্ষণ করা সম্ভব হয়নি"))
            }
            val fileName = "Qawmi_Paygam_${System.currentTimeMillis()}_$index.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QawmiKhedmot")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return Result.failure(IllegalStateException("গ্যালারিতে ফাইল তৈরি করা যায়নি"))
            resolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return Result.failure(IllegalStateException("গ্যালারিতে লেখা সম্ভব হয়নি"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Result.success("আলহামদুলিল্লাহ, ছবিটি গ্যালারিতে সংরক্ষিত হয়েছে")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveMultipleImagesToDevice(context: Context, imageSources: List<String>): Int {
        var savedCount = 0
        imageSources.take(MAX_CHAT_IMAGES_PER_MESSAGE).forEachIndexed { idx, src ->
            if (saveImageToDevice(context, src, idx + 1).isSuccess) {
                savedCount++
            }
        }
        return savedCount
    }

    fun encodeBitmapToDataUrl(bitmap: Bitmap, quality: Int = 76): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    fun decodeDataUrlToImageBitmap(photoUrl: String): ImageBitmap? {
        val effectiveUrl = if (photoUrl.startsWith("data:image")) {
            photoUrl
        } else {
            val docId = extractAttachmentDocId(photoUrl)
            if (docId != null) attachmentDataUrlCache[docId] ?: return null else return null
        }
        return try {
            val commaIndex = effectiveUrl.indexOf(',')
            if (commaIndex == -1) return null
            val base64Part = effectiveUrl.substring(commaIndex + 1)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Scales an image while preserving aspect ratio, strictly capping the longest edge at [maxDimPx].
     * Never increases dimensions above [maxDimPx], preventing oversized bitmaps that could exceed
     * the 1 MiB Firestore document limit.
     */
    private fun scalePreservingAspectRatioForTextClarity(src: Bitmap, maxDimPx: Int): Bitmap {
        val width = src.width
        val height = src.height
        val longest = max(width, height)
        val effectiveMaxDim = maxDimPx.coerceAtLeast(1)
        if (longest <= effectiveMaxDim || longest <= 0) return src
        val ratio = effectiveMaxDim.toFloat() / longest.toFloat()
        val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, targetW, targetH, true)
    }

    /**
     * Ensures a Data-URI image strictly fits within [maxBase64Chars] while preserving the highest
     * possible resolution and JPEG quality that satisfies the budget.
     */
    fun ensureDataUrlFitsBudget(dataUrl: String, maxBase64Chars: Int): String {
        if (dataUrl.length <= maxBase64Chars) return dataUrl
        return try {
            val commaIdx = dataUrl.indexOf(',')
            if (commaIdx == -1) return dataUrl.take(maxBase64Chars)
            val bytes = Base64.decode(dataUrl.substring(commaIdx + 1), Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return dataUrl

            val attempts = listOf(
                1600 to 84,
                1440 to 80,
                1280 to 78,
                1120 to 75,
                960 to 72,
                840 to 68,
                720 to 64,
                600 to 60,
                480 to 56,
                360 to 52,
                280 to 48,
                200 to 44
            )
            var lastCandidate: String = dataUrl
            for ((dim, qual) in attempts) {
                val scaled = scalePreservingAspectRatioForTextClarity(bmp, dim)
                val candidate = encodeBitmapToDataUrl(scaled, quality = qual)
                lastCandidate = candidate
                if (candidate.length <= maxBase64Chars) {
                    return candidate
                }
            }
            lastCandidate
        } catch (_: Exception) {
            dataUrl
        }
    }

    private fun centerCropAndScale(src: Bitmap, sizePx: Int): Bitmap {
        val width = src.width
        val height = src.height
        val minEdge = min(width, height)
        val xOffset = (width - minEdge) / 2
        val yOffset = (height - minEdge) / 2
        val square = Bitmap.createBitmap(src, xOffset, yOffset, minEdge, minEdge)
        return if (minEdge != sizePx) {
            Bitmap.createScaledBitmap(square, sizePx, sizePx, true)
        } else {
            square
        }
    }
}
