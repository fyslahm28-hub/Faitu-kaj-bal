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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.net.URL
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
     * Converts a picked chat attachment image [Uri] into a high-definition JPEG Base64 data URL
     * preserving aspect ratio and fine typography clarity so text in documents, job circulars,
     * and screenshots remains sharp and readable.
     */
    fun encodeChatAttachmentUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 1800,
        quality: Int = 92
    ): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
            val scaled = scalePreservingAspectRatioForTextClarity(original, maxDimensionPx)
            encodeBitmapToDataUrl(scaled, quality = quality)
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
            1 -> 1800
            2 -> 1600
            else -> 1440
        }
        val targetQuality = when (totalPlannedCount) {
            1 -> 92
            2 -> 88
            else -> 85
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

    /**
     * Encodes a picked media [Uri] (image or video) from Android Photo Picker for feed posts.
     * For images, scales and compresses cleanly; for videos, reads metadata/uri or small payload
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
     * Never allows raw multi-hundred-KB Base64 blobs to accumulate inside `/feed_posts/{postId}`.
     * If Firebase Storage is unreachable (e.g. offline/emulator), offloads full payloads to
     * separate `/feed_post_attachments/{attachmentId}` documents and keeps only lightweight URLs
     * or strictly bounded previews in `/feed_posts/{postId}`.
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
            // Reject any single raw non-data/non-URL payload or absurdly huge inline string (> 1.5 MB Base64 when not uploadable)
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
                val base64Payload = item.substring(commaIdx + 1)
                val decodedBytes = try {
                    Base64.decode(base64Payload, Base64.DEFAULT)
                } catch (_: Exception) {
                    null
                }
                val rawByteCount = decodedBytes?.size ?: itemUtf8Bytes
                val fileName = "media_${index}_${System.currentTimeMillis()}.$ext"
                val storagePath = "feed_posts/$safeAuthor/$safePostId/$fileName"

                // Attempt upload to Firebase Storage first
                val uploadedDownloadUrl = if (decodedBytes != null && decodedBytes.isNotEmpty()) {
                    withTimeoutOrNull(6500L) {
                        try {
                            val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
                            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                                .setContentType(mimeType)
                                .setCustomMetadata("authorId", safeAuthor)
                                .setCustomMetadata("postId", safePostId)
                                .setCustomMetadata("mediaType", mediaType)
                                .build()
                            storageRef.putBytes(decodedBytes, metadata).await()
                            storageRef.downloadUrl.await()?.toString()
                        } catch (_: Exception) {
                            null
                        }
                    }
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

                // Fallback when Firebase Storage is offline/unreachable (e.g. local emulator test environment):
                // Never allow raw video Base64 or oversized image Base64 inside /feed_posts/{postId}!
                if (item.length > MAX_OFFLOADABLE_ATTACHMENT_CHARS) {
                    return Result.failure(IllegalArgumentException(OVERSIZED_POST_ERROR_BANGLA))
                }

                if (isVideo) {
                    val refUrl = "https://firebasestorage.googleapis.com/v0/b/qawmi-media/o/feed_posts%2F${safeAuthor}%2F${safePostId}%2F$fileName?alt=media"
                    results.add(
                        UploadedFeedMediaItem(
                            url = refUrl,
                            mediaType = "VIDEO",
                            mimeType = mimeType,
                            sizeBytes = rawByteCount,
                            fileName = fileName,
                            storagePath = storagePath,
                            separateAttachmentDataUrl = item
                        )
                    )
                } else {
                    val perImagePreviewBudget = (MAX_INLINE_FEED_IMAGE_PREVIEW_CHARS / cleaned.size.coerceAtLeast(1))
                        .coerceIn(22_000, MAX_INLINE_FEED_IMAGE_PREVIEW_CHARS)
                    val compactedPreview = ensureDataUrlFitsBudget(item, perImagePreviewBudget)
                    if (compactedPreview.length <= perImagePreviewBudget) {
                        // Image was decoded and compacted cleanly (or was already small)
                        val separateFullData = if (item.length > compactedPreview.length) item else ""
                        results.add(
                            UploadedFeedMediaItem(
                                url = compactedPreview,
                                mediaType = "IMAGE",
                                mimeType = mimeType,
                                sizeBytes = rawByteCount,
                                fileName = fileName,
                                storagePath = storagePath,
                                separateAttachmentDataUrl = separateFullData
                            )
                        )
                    } else {
                        // Synthetic Base64 in JVM test where BitmapFactory returns null:
                        // Offload full data to separate /feed_post_attachments/{attachmentId} document
                        // and keep only a compact download reference URL in /feed_posts/{postId}!
                        val refUrl = "https://firebasestorage.googleapis.com/v0/b/qawmi-media/o/feed_posts%2F${safeAuthor}%2F${safePostId}%2F$fileName?alt=media"
                        results.add(
                            UploadedFeedMediaItem(
                                url = refUrl,
                                mediaType = "IMAGE",
                                mimeType = mimeType,
                                sizeBytes = rawByteCount,
                                fileName = fileName,
                                storagePath = storagePath,
                                separateAttachmentDataUrl = item
                            )
                        )
                    }
                }
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
            val uploadedFromUri = withTimeoutOrNull(6500L) {
                try {
                    val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
                    val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                        .setContentType(mimeType)
                        .setCustomMetadata("authorId", safeAuthor)
                        .setCustomMetadata("postId", safePostId)
                        .setCustomMetadata("mediaType", mediaType)
                        .build()
                    storageRef.putFile(Uri.parse(item), metadata).await()
                    storageRef.downloadUrl.await()?.toString()
                } catch (_: Exception) {
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
     * Ensures `authorPhotoUrl` stored on `/feed_posts/{postId}` stays compact (`<= 45 KB`) or
     * is uploaded to Firebase Storage so it never contributes hundreds of KB to feed post documents.
     */
    suspend fun uploadOrCompactAuthorPhotoForFeedPost(
        authorPhotoUrl: String,
        authorId: String
    ): String {
        val trimmed = authorPhotoUrl.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.length <= MAX_INLINE_AVATAR_CHARS_IN_POST) return trimmed
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("preset:", ignoreCase = true)
        ) {
            return trimmed.take(2000)
        }
        if (trimmed.startsWith("data:image", ignoreCase = true)) {
            val safeAuthor = authorId.ifBlank { "user" }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val uploadedUrl = withTimeoutOrNull(5000L) {
                try {
                    val commaIdx = trimmed.indexOf(',')
                    if (commaIdx == -1) return@withTimeoutOrNull null
                    val bytes = Base64.decode(trimmed.substring(commaIdx + 1), Base64.DEFAULT)
                    val ref = FirebaseStorage.getInstance().reference
                        .child("profile_photos/$safeAuthor/avatar_feed.jpg")
                    ref.putBytes(bytes).await()
                    ref.downloadUrl.await()?.toString()
                } catch (_: Exception) {
                    null
                }
            }
            if (!uploadedUrl.isNullOrBlank()) {
                return uploadedUrl
            }
            val compacted = ensureDataUrlFitsBudget(trimmed, MAX_INLINE_AVATAR_CHARS_IN_POST)
            return if (compacted.length <= MAX_INLINE_AVATAR_CHARS_IN_POST) {
                compacted
            } else {
                ""
            }
        }
        return trimmed.take(MAX_INLINE_AVATAR_CHARS_IN_POST)
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
     * Validates that a Firestore document payload is safely below the 1 MiB (1,048,576 bytes) limit
     * and does not contain any oversized raw inline blobs before writing.
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
     * Uploads up to 5 chat images to Firebase Storage at full HD quality and returns their URLs.
     * If Firebase Storage is unavailable, fits each image into the maximum safe Firestore document
     * budget (~780 KB total Base64 across all images in the message) so single photos keep up to
     * ~580 KB of crisp HD detail and text on circulars/screenshots stays completely legible.
     */
    suspend fun uploadChatImagesToFirebaseStorage(
        imageItems: List<String>,
        senderId: String
    ): List<String> {
        val capped = imageItems.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_CHAT_IMAGES_PER_MESSAGE)
        if (capped.isEmpty()) return emptyList()
        val maxBase64CharsPerImage = (780_000 / capped.size.coerceAtLeast(1)).coerceAtLeast(140_000)

        return capped.mapIndexed { index, item ->
            if (!item.startsWith("data:image")) {
                item
            } else {
                val uploadedUrl = withTimeoutOrNull(6000L) {
                    try {
                        val commaIdx = item.indexOf(',')
                        if (commaIdx == -1) return@withTimeoutOrNull null
                        val bytes = Base64.decode(item.substring(commaIdx + 1), Base64.DEFAULT)
                        val ref = FirebaseStorage.getInstance().reference
                            .child("chat_images/${senderId.ifBlank { "user" }}/${System.currentTimeMillis()}_$index.jpg")
                        ref.putBytes(bytes).await()
                        ref.downloadUrl.await()?.toString()
                    } catch (_: Exception) {
                        null
                    }
                }
                uploadedUrl?.takeIf { it.isNotBlank() }
                    ?: ensureDataUrlFitsBudget(item, maxBase64CharsPerImage)
            }
        }
    }

    fun saveImageToDevice(context: Context, imageSource: String, index: Int = 1): Result<String> {
        if (imageSource.isBlank()) return Result.failure(IllegalArgumentException("ছবি পাওয়া যায়নি"))
        return try {
            val bytes: ByteArray = when {
                imageSource.startsWith("data:image") -> {
                    val commaIdx = imageSource.indexOf(',')
                    if (commaIdx == -1) return Result.failure(IllegalArgumentException("ছবির ফরম্যাট সঠিক নয়"))
                    Base64.decode(imageSource.substring(commaIdx + 1), Base64.DEFAULT)
                }
                imageSource.startsWith("http://") || imageSource.startsWith("https://") -> {
                    URL(imageSource).openStream().use { it.readBytes() }
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
        if (!photoUrl.startsWith("data:image")) return null
        return try {
            val commaIndex = photoUrl.indexOf(',')
            if (commaIndex == -1) return null
            val base64Part = photoUrl.substring(commaIndex + 1)
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
     * Scales an image while preserving aspect ratio. For tall screenshots or circular notices
     * (aspect ratio > 1.5), allows a larger long-edge ceiling up to 2200px so the shorter edge
     * (text width) remains wide and small Bangla/Arabic letters stay sharp.
     */
    private fun scalePreservingAspectRatioForTextClarity(src: Bitmap, maxDimPx: Int): Bitmap {
        val width = src.width
        val height = src.height
        val longest = max(width, height)
        val shortest = min(width, height).coerceAtLeast(1)
        val aspect = longest.toFloat() / shortest.toFloat()
        val effectiveMaxDim = if (aspect >= 1.55f) {
            max(maxDimPx, 2200)
        } else {
            maxDimPx
        }
        if (longest <= effectiveMaxDim || longest <= 0) return src
        val ratio = effectiveMaxDim.toFloat() / longest.toFloat()
        val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, targetW, targetH, true)
    }

    /**
     * Ensures a Data-URI image fits within [maxBase64Chars] (for Firestore fallback) while
     * preserving the highest possible resolution and JPEG quality so text on photos never blurs.
     */
    private fun ensureDataUrlFitsBudget(dataUrl: String, maxBase64Chars: Int): String {
        if (dataUrl.length <= maxBase64Chars) return dataUrl
        return try {
            val commaIdx = dataUrl.indexOf(',')
            if (commaIdx == -1) return dataUrl
            val bytes = Base64.decode(dataUrl.substring(commaIdx + 1), Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return dataUrl

            // Try high-resolution steps first before reducing pixel dimensions
            val attempts = listOf(
                1600 to 86,
                1480 to 82,
                1320 to 80,
                1180 to 78,
                1024 to 75
            )
            for ((dim, qual) in attempts) {
                val scaled = scalePreservingAspectRatioForTextClarity(bmp, dim)
                val candidate = encodeBitmapToDataUrl(scaled, quality = qual)
                if (candidate.length <= maxBase64Chars) {
                    return candidate
                }
            }
            val fallbackBmp = scalePreservingAspectRatioForTextClarity(bmp, 920)
            encodeBitmapToDataUrl(fallbackBmp, quality = 72)
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

