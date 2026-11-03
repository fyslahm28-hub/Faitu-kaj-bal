package com.example.ui.quran

import androidx.compose.ui.graphics.Color

/**
 * Models for the Indo-Pak / Emdadia 15-Line Hafezi Quran Hub.
 * Strictly isolated from Messaging, Feed, and Firestore modules.
 */
data class HafeziSurahInfo(
    val number: Int,
    val nameArabic: String,
    val nameBangla: String,
    val nameEnglish: String,
    val meaningBangla: String,
    val isMakki: Boolean,
    val ayahCount: Int,
    val startPage: Int,
    val endPage: Int,
    val startPara: Int,
    val rukuCount: Int = 1
) {
    val revelationTypeBangla: String
        get() = if (isMakki) "মাক্কী" else "মাদানী"
}

data class HafeziParaInfo(
    val number: Int,
    val nameArabic: String,
    val nameBangla: String,
    val nameEnglish: String,
    val startPage: Int,
    val endPage: Int,
    val startSurahNumber: Int,
    val startSurahNameBangla: String,
    val startAyahLabel: String
) {
    val totalPages: Int
        get() = (endPage - startPage + 1).coerceAtLeast(1)
}

data class MushafLineItem(
    val lineNumber: Int, // 1..15
    val lineType: MushafLineType,
    val arabicText: String,
    val surahHeaderInfo: HafeziSurahInfo? = null,
    val ayahRangeLabel: String = "",
    val hasSajdah: Boolean = false,
    val marginalMarker: String? = null // e.g., "ربع", "نصف", "ثلث", "ركوع"
)

enum class MushafLineType {
    SURAH_HEADER_BANNER,
    BISMILLAH_BANNER,
    QURAN_TEXT_LINE
}

data class HafeziPageMetadata(
    val pageNumber: Int, // 1..610 in standard 15-Line Emdadia Hafezi Mushaf
    val paraNumber: Int,
    val paraNameArabic: String,
    val paraNameBangla: String,
    val primarySurah: HafeziSurahInfo,
    val surahsOnPage: List<HafeziSurahInfo>,
    val manzilNumber: Int, // 1..7
    val hizbQuarterLabel: String,
    val hasSajdah: Boolean,
    val lines: List<MushafLineItem> // Strictly 15 lines per page
)

data class HafeziAyahSearchEntry(
    val surahNumber: Int,
    val surahNameBangla: String,
    val surahNameArabic: String,
    val ayahNumber: Int,
    val paraNumber: Int,
    val pageNumber: Int,
    val lineNumber: Int,
    val arabicText: String,
    val banglaTopicOrTranslation: String
) {
    val referenceKey: String
        get() = "$surahNumber:$ayahNumber"
}

data class QuranBookmark(
    val pageNumber: Int,
    val surahNumber: Int,
    val surahNameBangla: String,
    val paraNumber: Int,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class QuranReadingHistoryEntry(
    val pageNumber: Int,
    val surahNameBangla: String,
    val paraNumber: Int,
    val visitedAtMillis: Long,
    val durationSeconds: Int = 0
)

data class QuranReadingProgress(
    val lastReadPage: Int = 2,
    val lastReadSurahNumber: Int = 1,
    val lastReadSurahNameBangla: String = "আল-ফাতিহা",
    val lastReadParaNumber: Int = 1,
    val lastReadTimestampMillis: Long = 0L,
    val uniquePagesReadCount: Int = 0,
    val totalMushafPages: Int = 610,
    val completedKhatamCount: Int = 0
) {
    val progressFraction: Float
        get() = (uniquePagesReadCount.toFloat() / totalMushafPages.toFloat()).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = ((uniquePagesReadCount * 100f) / totalMushafPages).toInt().coerceIn(0, 100)
}

enum class QuranDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    COMPLETED_VERIFIED,
    ERROR
}

data class QuranDownloadState(
    val status: QuranDownloadStatus = QuranDownloadStatus.NOT_DOWNLOADED,
    val downloadedPages: Int = 0,
    val totalPages: Int = 610,
    val verifiedPages: Int = 0,
    val integrityPassed: Boolean = false,
    val statusMessageBangla: String = "অফলাইনে পড়ার জন্য ১৫ লাইনের হাফেজি মুসহাফ ডাউনলোড করুন",
    val lastChecksumSummary: String = ""
) {
    val progressFraction: Float
        get() = if (totalPages > 0) (downloadedPages.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)
}

data class QuranIntegrityReport(
    val isValid: Boolean,
    val verifiedPageCount: Int,
    val expectedPageCount: Int,
    val corruptedOrMissingPages: List<Int>,
    val sha256DigestPrefix: String,
    val messageBangla: String
)

enum class MushafThemeMode(
    val id: String,
    val titleBangla: String,
    val pageBackground: Color,
    val pageInnerBackground: Color,
    val textColor: Color,
    val borderColor: Color,
    val goldAccent: Color,
    val headerBackground: Color
) {
    EMDADIA_CREAM(
        id = "emdadia_cream",
        titleBangla = "এমদাদিয়া ক্রিম",
        pageBackground = Color(0xFFFDF8EC),
        pageInnerBackground = Color(0xFFFFFCF5),
        textColor = Color(0xFF141A17),
        borderColor = Color(0xFF0F6B4B),
        goldAccent = Color(0xFFD9A82E),
        headerBackground = Color(0xFFEAF4EF)
    ),
    CLASSIC_WHITE(
        id = "classic_white",
        titleBangla = "শুভ্র পৃষ্ঠা",
        pageBackground = Color(0xFFF6FAF8),
        pageInnerBackground = Color(0xFFFFFFFF),
        textColor = Color(0xFF101815),
        borderColor = Color(0xFF0F6B4B),
        goldAccent = Color(0xFFD9A82E),
        headerBackground = Color(0xFFE6F3EC)
    ),
    SEPIA_HERITAGE(
        id = "sepia_heritage",
        titleBangla = "ক্লাসিক সেপিয়া",
        pageBackground = Color(0xFFF4EAD5),
        pageInnerBackground = Color(0xFFF9F1E1),
        textColor = Color(0xFF2B1E12),
        borderColor = Color(0xFF6B4C2B),
        goldAccent = Color(0xFFC89223),
        headerBackground = Color(0xFFEDE0C8)
    ),
    NIGHT_MODE(
        id = "night_mode",
        titleBangla = "নাইট মোড",
        pageBackground = Color(0xFF0B1914),
        pageInnerBackground = Color(0xFF10241D),
        textColor = Color(0xFFF3EFE2),
        borderColor = Color(0xFFD9A82E),
        goldAccent = Color(0xFFD9A82E),
        headerBackground = Color(0xFF153127)
    )
}
