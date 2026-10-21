package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.min

object ProfilePhotoHelper {

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
