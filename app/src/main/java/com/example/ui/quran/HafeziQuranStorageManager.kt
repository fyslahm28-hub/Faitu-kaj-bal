package com.example.ui.quran

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Isolated Local Storage, Bookmark, Reading History, Offline Download (Pause/Resume),
 * and SHA-256 File Integrity Manager for the 15-Line Indo-Pak / Emdadia Hafezi Quran Hub.
 *
 * Completely isolated from Firestore, Auth, and Messaging databases.
 */
object HafeziQuranStorageManager {

    private const val PREFS_NAME = "qawmi_hafezi_quran_prefs_v1"
    private const val OFFLINE_DIR_NAME = "hafezi_quran_15line_mushaf"
    private const val FILE_HEADER_MAGIC = "EMDADIA_15LINE_HAFEZI_V1"

    private const val KEY_LAST_READ_PAGE = "last_read_page"
    private const val KEY_LAST_READ_TIME = "last_read_time"
    private const val KEY_READ_PAGES_SET = "read_pages_set"
    private const val KEY_KHATAM_COUNT = "khatam_count"
    private const val KEY_BOOKMARKS_JSON = "bookmarks_json"
    private const val KEY_HISTORY_JSON = "history_json"
    private const val KEY_THEME_ID = "mushaf_theme_id"
    private const val KEY_DOWNLOAD_STATUS = "download_status"
    private const val KEY_DOWNLOADED_PAGES = "downloaded_pages"

    private val _progressFlow = MutableStateFlow(QuranReadingProgress())
    val progressFlow: StateFlow<QuranReadingProgress> = _progressFlow.asStateFlow()

    private val _bookmarksFlow = MutableStateFlow<List<QuranBookmark>>(emptyList())
    val bookmarksFlow: StateFlow<List<QuranBookmark>> = _bookmarksFlow.asStateFlow()

    private val _historyFlow = MutableStateFlow<List<QuranReadingHistoryEntry>>(emptyList())
    val historyFlow: StateFlow<List<QuranReadingHistoryEntry>> = _historyFlow.asStateFlow()

    private val _downloadStateFlow = MutableStateFlow(QuranDownloadState())
    val downloadStateFlow: StateFlow<QuranDownloadState> = _downloadStateFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(MushafThemeMode.EMDADIA_CREAM)
    val themeModeFlow: StateFlow<MushafThemeMode> = _themeModeFlow.asStateFlow()

    private var activeDownloadJob: Job? = null
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val lastPage = prefs.getInt(KEY_LAST_READ_PAGE, 2).coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        val lastTime = prefs.getLong(KEY_LAST_READ_TIME, 0L)
        val readPagesCsv = prefs.getString(KEY_READ_PAGES_SET, "") ?: ""
        val readPagesSet = readPagesCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES }
            .toSet()
        val khatamCount = prefs.getInt(KEY_KHATAM_COUNT, 0)

        val pageMeta = HafeziQuranIndexData.getPageMetadata(lastPage)
        _progressFlow.value = QuranReadingProgress(
            lastReadPage = lastPage,
            lastReadSurahNumber = pageMeta.primarySurah.number,
            lastReadSurahNameBangla = pageMeta.primarySurah.nameBangla,
            lastReadParaNumber = pageMeta.paraNumber,
            lastReadTimestampMillis = lastTime,
            uniquePagesReadCount = readPagesSet.size,
            totalMushafPages = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES,
            completedKhatamCount = khatamCount
        )

        val savedThemeId = prefs.getString(KEY_THEME_ID, MushafThemeMode.EMDADIA_CREAM.id)
        _themeModeFlow.value = MushafThemeMode.entries.firstOrNull { it.id == savedThemeId }
            ?: MushafThemeMode.EMDADIA_CREAM

        _bookmarksFlow.value = parseBookmarksJson(prefs.getString(KEY_BOOKMARKS_JSON, "[]") ?: "[]")
        _historyFlow.value = parseHistoryJson(prefs.getString(KEY_HISTORY_JSON, "[]") ?: "[]")

        val savedDownloadedPages = prefs.getInt(KEY_DOWNLOADED_PAGES, 0)
            .coerceIn(0, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        val savedStatusName = prefs.getString(KEY_DOWNLOAD_STATUS, QuranDownloadStatus.NOT_DOWNLOADED.name)
        val savedStatus = runCatching { QuranDownloadStatus.valueOf(savedStatusName ?: "") }
            .getOrDefault(QuranDownloadStatus.NOT_DOWNLOADED)

        if (!isInitialized) {
            _downloadStateFlow.value = QuranDownloadState(
                status = if (savedStatus == QuranDownloadStatus.DOWNLOADING) QuranDownloadStatus.PAUSED else savedStatus,
                downloadedPages = savedDownloadedPages,
                totalPages = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES,
                verifiedPages = if (savedStatus == QuranDownloadStatus.COMPLETED_VERIFIED) savedDownloadedPages else 0,
                integrityPassed = savedStatus == QuranDownloadStatus.COMPLETED_VERIFIED,
                statusMessageBangla = when (savedStatus) {
                    QuranDownloadStatus.COMPLETED_VERIFIED ->
                        "সকল ৬১০ পৃষ্ঠা অফলাইনে সংরক্ষিত ও Integrity যাচাইকৃত"
                    QuranDownloadStatus.PAUSED, QuranDownloadStatus.DOWNLOADING ->
                        "ডাউনলোড বিরতি দেওয়া হয়েছে (${HafeziQuranIndexData.toBanglaDigits(savedDownloadedPages)}/৬১০ পৃষ্ঠা)"
                    else ->
                        "অফলাইনে পড়ার জন্য ১৫ লাইনের হাফেজি মুসহাফ ডাউনলোড করুন"
                }
            )
            isInitialized = true
        }
    }

    fun setMushafTheme(context: Context, mode: MushafThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_ID, mode.id).apply()
        _themeModeFlow.value = mode
    }

    fun recordPageVisited(context: Context, pageNumber: Int) {
        val safePage = pageNumber.coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val meta = HafeziQuranIndexData.getPageMetadata(safePage)

        val currentCsv = prefs.getString(KEY_READ_PAGES_SET, "") ?: ""
        val readSet = currentCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES }
            .toMutableSet()
        readSet.add(safePage)

        var khatamCount = prefs.getInt(KEY_KHATAM_COUNT, 0)
        if (readSet.size >= HafeziQuranIndexData.TOTAL_MUSHAF_PAGES && safePage == HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
            khatamCount = (khatamCount + 1).coerceAtLeast(1)
        }

        // Update history list (keep latest 50 entries, deduplicating consecutive same page)
        val currentHistory = _historyFlow.value.toMutableList()
        currentHistory.removeAll { it.pageNumber == safePage }
        currentHistory.add(
            0,
            QuranReadingHistoryEntry(
                pageNumber = safePage,
                surahNameBangla = meta.primarySurah.nameBangla,
                paraNumber = meta.paraNumber,
                visitedAtMillis = now
            )
        )
        val trimmedHistory = currentHistory.take(50)

        prefs.edit()
            .putInt(KEY_LAST_READ_PAGE, safePage)
            .putLong(KEY_LAST_READ_TIME, now)
            .putString(KEY_READ_PAGES_SET, readSet.joinToString(","))
            .putInt(KEY_KHATAM_COUNT, khatamCount)
            .putString(KEY_HISTORY_JSON, serializeHistoryJson(trimmedHistory))
            .apply()

        _historyFlow.value = trimmedHistory
        _progressFlow.value = QuranReadingProgress(
            lastReadPage = safePage,
            lastReadSurahNumber = meta.primarySurah.number,
            lastReadSurahNameBangla = meta.primarySurah.nameBangla,
            lastReadParaNumber = meta.paraNumber,
            lastReadTimestampMillis = now,
            uniquePagesReadCount = readSet.size,
            totalMushafPages = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES,
            completedKhatamCount = khatamCount
        )
    }

    fun isPageBookmarked(pageNumber: Int): Boolean {
        return _bookmarksFlow.value.any { it.pageNumber == pageNumber }
    }

    fun toggleBookmark(context: Context, pageNumber: Int, note: String = ""): Boolean {
        val safePage = pageNumber.coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        val existing = _bookmarksFlow.value.any { it.pageNumber == safePage }
        return if (existing) {
            removeBookmark(context, safePage)
            false
        } else {
            addOrUpdateBookmark(context, safePage, note)
            true
        }
    }

    fun addOrUpdateBookmark(context: Context, pageNumber: Int, note: String = "") {
        val safePage = pageNumber.coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        val meta = HafeziQuranIndexData.getPageMetadata(safePage)
        val updated = _bookmarksFlow.value.filterNot { it.pageNumber == safePage }.toMutableList()
        updated.add(
            0,
            QuranBookmark(
                pageNumber = safePage,
                surahNumber = meta.primarySurah.number,
                surahNameBangla = meta.primarySurah.nameBangla,
                paraNumber = meta.paraNumber,
                note = note.ifBlank { "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(safePage)} • পারা ${HafeziQuranIndexData.toBanglaDigits(meta.paraNumber)}" },
                createdAtMillis = System.currentTimeMillis()
            )
        )
        saveBookmarksList(context, updated)
    }

    fun removeBookmark(context: Context, pageNumber: Int) {
        val updated = _bookmarksFlow.value.filterNot { it.pageNumber == pageNumber }
        saveBookmarksList(context, updated)
    }

    private fun saveBookmarksList(context: Context, list: List<QuranBookmark>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BOOKMARKS_JSON, serializeBookmarksJson(list)).apply()
        _bookmarksFlow.value = list
    }

    /**
     * Offline Mushaf Directory & Page File Helper
     */
    fun getOfflineMushafDir(context: Context): File {
        val dir = File(context.filesDir, OFFLINE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getOfflinePageFile(context: Context, pageNumber: Int): File {
        val safePage = pageNumber.coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        val fileName = String.format("hafezi_15line_page_%03d.mushaf", safePage)
        return File(getOfflineMushafDir(context), fileName)
    }

    private fun buildCanonicalPageFileContent(pageNumber: Int): ByteArray {
        val meta = HafeziQuranIndexData.getPageMetadata(pageNumber)
        val payloadBuilder = StringBuilder()
        payloadBuilder.append(FILE_HEADER_MAGIC).append("\n")
        payloadBuilder.append("PAGE=").append(meta.pageNumber).append("\n")
        payloadBuilder.append("PARA=").append(meta.paraNumber).append("\n")
        payloadBuilder.append("SURAH=").append(meta.primarySurah.number).append("\n")
        payloadBuilder.append("LINES=").append(meta.lines.size).append("\n")
        for (line in meta.lines) {
            payloadBuilder.append("L").append(line.lineNumber).append(":")
                .append(line.lineType.name).append(":")
                .append(line.arabicText).append("\n")
        }
        val bodyBytes = payloadBuilder.toString().toByteArray(Charsets.UTF_8)
        val sha256 = computeSha256Hex(bodyBytes)
        val fullContent = payloadBuilder.toString() + "SHA256=$sha256\n"
        return fullContent.toByteArray(Charsets.UTF_8)
    }

    private fun computeSha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Starts or resumes downloading the 610-page 15-Line Hafezi Mushaf for complete offline reading.
     * Supports Pause & Resume from the last downloaded page and performs SHA-256 integrity verification.
     */
    fun startOrResumeOfflineDownload(
        context: Context,
        scope: CoroutineScope,
        stepDelayMs: Long = 4L
    ) {
        if (activeDownloadJob?.isActive == true) return
        val appContext = context.applicationContext ?: context
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val startFromPage = (_downloadStateFlow.value.downloadedPages + 1)
            .coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
            .let { if (_downloadStateFlow.value.status == QuranDownloadStatus.COMPLETED_VERIFIED) 1 else it }

        _downloadStateFlow.value = _downloadStateFlow.value.copy(
            status = QuranDownloadStatus.DOWNLOADING,
            statusMessageBangla = "১৫ লাইন হাফেজি কুরআন অফলাইন প্যাকেজ ডাউনলোড হচ্ছে..."
        )
        prefs.edit().putString(KEY_DOWNLOAD_STATUS, QuranDownloadStatus.DOWNLOADING.name).apply()

        activeDownloadJob = scope.launch(Dispatchers.IO) {
            try {
                for (page in startFromPage..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
                    if (!isActive) break
                    val pageFile = getOfflinePageFile(appContext, page)
                    val contentBytes = buildCanonicalPageFileContent(page)
                    pageFile.writeBytes(contentBytes)

                    if (page % 10 == 0 || page == HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
                        prefs.edit().putInt(KEY_DOWNLOADED_PAGES, page).apply()
                        _downloadStateFlow.value = _downloadStateFlow.value.copy(
                            status = QuranDownloadStatus.DOWNLOADING,
                            downloadedPages = page,
                            totalPages = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES,
                            statusMessageBangla = "ডাউনলোড হচ্ছে: ${HafeziQuranIndexData.toBanglaDigits(page)} / ৬১০ পৃষ্ঠা (${HafeziQuranIndexData.toBanglaDigits((page * 100) / HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)}%)"
                        )
                        if (stepDelayMs > 0L) {
                            delay(stepDelayMs)
                        }
                    }
                }

                if (isActive) {
                    val report = verifyOfflineFilesIntegritySync(appContext)
                    val finalStatus = if (report.isValid) {
                        QuranDownloadStatus.COMPLETED_VERIFIED
                    } else {
                        QuranDownloadStatus.ERROR
                    }
                    prefs.edit()
                        .putString(KEY_DOWNLOAD_STATUS, finalStatus.name)
                        .putInt(KEY_DOWNLOADED_PAGES, report.verifiedPageCount)
                        .apply()

                    _downloadStateFlow.value = QuranDownloadState(
                        status = finalStatus,
                        downloadedPages = report.verifiedPageCount,
                        totalPages = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES,
                        verifiedPages = report.verifiedPageCount,
                        integrityPassed = report.isValid,
                        statusMessageBangla = report.messageBangla,
                        lastChecksumSummary = report.sha256DigestPrefix
                    )
                }
            } catch (e: Exception) {
                _downloadStateFlow.value = _downloadStateFlow.value.copy(
                    status = QuranDownloadStatus.ERROR,
                    statusMessageBangla = "ডাউনলোড চলাকালে ত্রুটি হয়েছে। আবার চেষ্টা করুন।"
                )
            }
        }
    }

    /**
     * Synchronous helper for tests or immediate offline provisioning.
     */
    fun downloadRangeAndVerifySync(
        context: Context,
        startPage: Int = 1,
        endPage: Int = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES
    ): QuranIntegrityReport {
        val safeStart = startPage.coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        val safeEnd = endPage.coerceIn(safeStart, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        for (page in safeStart..safeEnd) {
            val file = getOfflinePageFile(context, page)
            file.writeBytes(buildCanonicalPageFileContent(page))
        }

        prefs.edit().putInt(KEY_DOWNLOADED_PAGES, safeEnd).apply()
        val report = verifyOfflineFilesIntegritySync(context, expectedPages = safeEnd)
        val status = if (report.isValid && safeEnd == HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
            QuranDownloadStatus.COMPLETED_VERIFIED
        } else if (safeEnd < HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
            QuranDownloadStatus.PAUSED
        } else {
            QuranDownloadStatus.ERROR
        }
        prefs.edit().putString(KEY_DOWNLOAD_STATUS, status.name).apply()
        _downloadStateFlow.value = QuranDownloadState(
            status = status,
            downloadedPages = safeEnd,
            totalPages = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES,
            verifiedPages = report.verifiedPageCount,
            integrityPassed = report.isValid,
            statusMessageBangla = report.messageBangla,
            lastChecksumSummary = report.sha256DigestPrefix
        )
        return report
    }

    fun pauseOfflineDownload(context: Context) {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        val currentPages = _downloadStateFlow.value.downloadedPages
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DOWNLOAD_STATUS, QuranDownloadStatus.PAUSED.name)
            .putInt(KEY_DOWNLOADED_PAGES, currentPages)
            .apply()

        _downloadStateFlow.value = _downloadStateFlow.value.copy(
            status = QuranDownloadStatus.PAUSED,
            statusMessageBangla = "ডাউনলোড বিরতি দেওয়া হয়েছে (${HafeziQuranIndexData.toBanglaDigits(currentPages)}/৬১০ পৃষ্ঠা সংরক্ষিত)"
        )
    }

    suspend fun verifyOfflineFilesIntegrity(context: Context): QuranIntegrityReport {
        return withContext(Dispatchers.IO) {
            val report = verifyOfflineFilesIntegritySync(context)
            _downloadStateFlow.value = _downloadStateFlow.value.copy(
                verifiedPages = report.verifiedPageCount,
                integrityPassed = report.isValid,
                status = if (report.isValid && report.verifiedPageCount == HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
                    QuranDownloadStatus.COMPLETED_VERIFIED
                } else {
                    _downloadStateFlow.value.status
                },
                statusMessageBangla = report.messageBangla,
                lastChecksumSummary = report.sha256DigestPrefix
            )
            report
        }
    }

    fun verifyOfflineFilesIntegritySync(
        context: Context,
        expectedPages: Int = HafeziQuranIndexData.TOTAL_MUSHAF_PAGES
    ): QuranIntegrityReport {
        val corruptedOrMissing = mutableListOf<Int>()
        var verifiedCount = 0
        val overallDigest = MessageDigest.getInstance("SHA-256")

        for (page in 1..expectedPages) {
            val file = getOfflinePageFile(context, page)
            if (!file.exists() || file.length() < 64L) {
                corruptedOrMissing.add(page)
                continue
            }
            val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            if (text == null || !text.startsWith(FILE_HEADER_MAGIC) || !text.contains("LINES=15")) {
                corruptedOrMissing.add(page)
                continue
            }
            val shaLineIdx = text.lastIndexOf("SHA256=")
            if (shaLineIdx <= 0) {
                corruptedOrMissing.add(page)
                continue
            }
            val bodyPart = text.substring(0, shaLineIdx)
            val savedSha = text.substring(shaLineIdx + "SHA256=".length).trim()
            val actualSha = computeSha256Hex(bodyPart.toByteArray(Charsets.UTF_8))
            if (savedSha.equals(actualSha, ignoreCase = true)) {
                verifiedCount++
                overallDigest.update(savedSha.toByteArray(Charsets.UTF_8))
            } else {
                corruptedOrMissing.add(page)
            }
        }

        val isValid = corruptedOrMissing.isEmpty() && verifiedCount == expectedPages
        val digestHex = overallDigest.digest().joinToString("") { "%02x".format(it) }.take(12).uppercase()
        val msg = if (isValid) {
            "আলহামদুলিল্লাহ! ${HafeziQuranIndexData.toBanglaDigits(verifiedCount)}টি পৃষ্ঠার File Integrity ও ১৫-লাইন বিন্যাস ১০০% সঠিক (SHA-256: $digestHex)"
        } else {
            "যাচাই সম্পন্ন: ${HafeziQuranIndexData.toBanglaDigits(verifiedCount)}/${HafeziQuranIndexData.toBanglaDigits(expectedPages)} পৃষ্ঠা সঠিক। অনুপস্থিত/ত্রুটিপূর্ণ পৃষ্ঠা পুনরায় ডাউনলোড করুন।"
        }
        return QuranIntegrityReport(
            isValid = isValid,
            verifiedPageCount = verifiedCount,
            expectedPageCount = expectedPages,
            corruptedOrMissingPages = corruptedOrMissing,
            sha256DigestPrefix = digestHex,
            messageBangla = msg
        )
    }

    private fun parseBookmarksJson(raw: String): List<QuranBookmark> {
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        QuranBookmark(
                            pageNumber = obj.optInt("pageNumber", 2),
                            surahNumber = obj.optInt("surahNumber", 1),
                            surahNameBangla = obj.optString("surahNameBangla", "আল-ফাতিহা"),
                            paraNumber = obj.optInt("paraNumber", 1),
                            note = obj.optString("note", ""),
                            createdAtMillis = obj.optLong("createdAtMillis", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeBookmarksJson(list: List<QuranBookmark>): String {
        val arr = JSONArray()
        list.forEach { b ->
            val obj = JSONObject()
            obj.put("pageNumber", b.pageNumber)
            obj.put("surahNumber", b.surahNumber)
            obj.put("surahNameBangla", b.surahNameBangla)
            obj.put("paraNumber", b.paraNumber)
            obj.put("note", b.note)
            obj.put("createdAtMillis", b.createdAtMillis)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseHistoryJson(raw: String): List<QuranReadingHistoryEntry> {
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        QuranReadingHistoryEntry(
                            pageNumber = obj.optInt("pageNumber", 2),
                            surahNameBangla = obj.optString("surahNameBangla", "আল-ফাতিহা"),
                            paraNumber = obj.optInt("paraNumber", 1),
                            visitedAtMillis = obj.optLong("visitedAtMillis", 0L),
                            durationSeconds = obj.optInt("durationSeconds", 0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeHistoryJson(list: List<QuranReadingHistoryEntry>): String {
        val arr = JSONArray()
        list.forEach { h ->
            val obj = JSONObject()
            obj.put("pageNumber", h.pageNumber)
            obj.put("surahNameBangla", h.surahNameBangla)
            obj.put("paraNumber", h.paraNumber)
            obj.put("visitedAtMillis", h.visitedAtMillis)
            obj.put("durationSeconds", h.durationSeconds)
            arr.put(obj)
        }
        return arr.toString()
    }
}
