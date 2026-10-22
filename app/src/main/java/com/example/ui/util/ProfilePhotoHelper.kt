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
     * Converts a picked chat attachment image [Uri] into a compressed JPEG Base64 data URL
     * preserving aspect ratio so 1-5 images can be previewed and stored cleanly.
     */
    fun encodeChatAttachmentUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 480,
        quality: Int = 66
    ): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val scaled = scalePreservingAspectRatio(original, maxDimensionPx)
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
        val newlyEncoded = uris.take(remainingSlots).mapNotNull { uri ->
            encodeChatAttachmentUriToDataUrl(context, uri)
        }
        return (existingImages + newlyEncoded).take(MAX_CHAT_IMAGES_PER_MESSAGE)
    }

    /**
     * Uploads up to 5 chat images to Firebase Storage and returns their URLs.
     * Falls back seamlessly to the compact JPEG data URL if Firebase Storage bucket
     * is offline or unprovisioned so images always render across personal & group chats.
     */
    suspend fun uploadChatImagesToFirebaseStorage(
        imageItems: List<String>,
        senderId: String
    ): List<String> {
        val capped = imageItems.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_CHAT_IMAGES_PER_MESSAGE)
        if (capped.isEmpty()) return emptyList()
        return capped.mapIndexed { index, item ->
            if (!item.startsWith("data:image")) {
                item
            } else {
                val uploadedUrl = withTimeoutOrNull(2200L) {
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
                uploadedUrl?.takeIf { it.isNotBlank() } ?: item
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
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    private fun scalePreservingAspectRatio(src: Bitmap, maxDimPx: Int): Bitmap {
        val width = src.width
        val height = src.height
        val longest = max(width, height)
        if (longest <= maxDimPx || longest <= 0) return src
        val ratio = maxDimPx.toFloat() / longest.toFloat()
        val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, targetW, targetH, true)
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
