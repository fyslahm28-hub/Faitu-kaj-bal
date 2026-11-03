package com.example.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Full-Screen 15-Line Indo-Pak / Emdadia Hafezi Mushaf Reader
 * with HorizontalPager (Swipe), Pinch & Button Zoom, Bookmarking,
 * Direct Page Jump, Night/Paper Theme Switching, and Automatic Last-Read Saving.
 */
@Composable
fun HafeziQuranReaderView(
    initialPage: Int,
    onBackToHub: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val safeInitialPage = initialPage.coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage - 1,
        pageCount = { HafeziQuranIndexData.TOTAL_MUSHAF_PAGES }
    )

    val currentPageNumber = (pagerState.currentPage + 1).coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
    val currentPageMeta = remember(currentPageNumber) {
        HafeziQuranIndexData.getPageMetadata(currentPageNumber)
    }

    val bookmarks by HafeziQuranStorageManager.bookmarksFlow.collectAsState()
    val themeMode by HafeziQuranStorageManager.themeModeFlow.collectAsState()
    val downloadState by HafeziQuranQuranDownloadStateFlow()

    val isCurrentPageBookmarked = remember(bookmarks, currentPageNumber) {
        bookmarks.any { it.pageNumber == currentPageNumber }
    }

    var currentZoomScale by remember(currentPageNumber) { mutableFloatStateOf(1f) }
    var showGoToPageDialog by rememberSaveable { mutableStateOf(false) }
    var showBookmarkNoteDialog by rememberSaveable { mutableStateOf(false) }
    var feedbackToast by remember { mutableStateOf<String?>(null) }

    // Automatically record visited page for Last Read & Reading History/Progress
    LaunchedEffect(currentPageNumber) {
        HafeziQuranStorageManager.recordPageVisited(context, currentPageNumber)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeMode.pageBackground)
            .testTag("hafezi_quran_reader_screen")
    ) {
        // 1. Reader Top Islamic Bar (#0F6B4B with Gold #D9A82E accents)
        Surface(
            color = Color(0xFF0F6B4B),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back to Hub Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackToHub,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("quran_reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "সূরা ${currentPageMeta.primarySurah.nameBangla} (${currentPageMeta.primarySurah.nameArabic})",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "পারা ${HafeziQuranIndexData.toBanglaDigits(currentPageMeta.paraNumber)} • পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)}/৬১০ • ১৫ লাইন হাফেজি",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFE082),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Reader Action Controls: Zoom Out/In, Go-To-Page, Night Mode, Bookmark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Zoom Out / Reset
                        IconButton(
                            onClick = {
                                currentZoomScale = (currentZoomScale - 0.25f).coerceAtLeast(1f)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("quran_reader_zoom_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "জুম আউট",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Zoom In
                        IconButton(
                            onClick = {
                                currentZoomScale = (currentZoomScale + 0.35f).coerceAtMost(3.0f)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("quran_reader_zoom_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "জুম ইন",
                                tint = Color(0xFFD9A82E),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Direct Page Jump Dialog Button
                        IconButton(
                            onClick = { showGoToPageDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("quran_reader_goto_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FindInPage,
                                contentDescription = "পৃষ্ঠা নম্বর দিয়ে খুঁজুন",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Theme / Night Mode Toggle
                        IconButton(
                            onClick = {
                                val nextMode = when (themeMode) {
                                    MushafThemeMode.EMDADIA_CREAM -> MushafThemeMode.NIGHT_MODE
                                    MushafThemeMode.NIGHT_MODE -> MushafThemeMode.SEPIA_HERITAGE
                                    MushafThemeMode.SEPIA_HERITAGE -> MushafThemeMode.CLASSIC_WHITE
                                    MushafThemeMode.CLASSIC_WHITE -> MushafThemeMode.EMDADIA_CREAM
                                }
                                HafeziQuranStorageManager.setMushafTheme(context, nextMode)
                                feedbackToast = "থিম: ${nextMode.titleBangla}"
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("quran_reader_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (themeMode == MushafThemeMode.NIGHT_MODE) {
                                    Icons.Default.LightMode
                                } else {
                                    Icons.Default.DarkMode
                                },
                                contentDescription = "নাইট ও পেপার মোড",
                                tint = Color(0xFFD9A82E),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Bookmark Toggle Button
                        IconButton(
                            onClick = {
                                if (isCurrentPageBookmarked) {
                                    HafeziQuranStorageManager.removeBookmark(context, currentPageNumber)
                                    feedbackToast = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} বুকমার্ক থেকে সরানো হয়েছে"
                                } else {
                                    HafeziQuranStorageManager.addOrUpdateBookmark(context, currentPageNumber)
                                    feedbackToast = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} বুকমার্কে সংরক্ষিত হয়েছে"
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("quran_reader_bookmark_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) {
                                    Icons.Default.Bookmark
                                } else {
                                    Icons.Default.BookmarkBorder
                                },
                                contentDescription = "বুকমার্ক করুন",
                                tint = if (isCurrentPageBookmarked) Color(0xFFD9A82E) else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Feedback / Zoom / Note Row
                if (feedbackToast != null || currentZoomScale > 1.02f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feedbackToast ?: "জুম সক্রিয়: ${(currentZoomScale * 100).toInt()}% (ডাবল ট্যাপ করে রিসেট করুন)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDDF3EC),
                            fontSize = 10.5.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (currentZoomScale > 1.02f) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFD9A82E),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { currentZoomScale = 1f }
                                        .testTag("quran_reader_reset_zoom_chip")
                                ) {
                                    Text(
                                        text = "জুম ১০০% করুন",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = Color(0xFF0B1914),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (feedbackToast != null) {
                                Text(
                                    text = "নোট যুক্ত করুন",
                                    color = Color(0xFFFFE082),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        feedbackToast = null
                                        showBookmarkNoteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Main Full-Screen Swipeable 15-Line Hafezi Mushaf Pager
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentZoomScale <= 1.05f,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("hafezi_quran_horizontal_pager")
            ) { pageIdx ->
                val pageNum = (pageIdx + 1).coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
                val meta = remember(pageNum) { HafeziQuranIndexData.getPageMetadata(pageNum) }
                HafeziMushafPageView(
                    pageMetadata = meta,
                    themeMode = themeMode,
                    externalZoomScale = if (pageNum == currentPageNumber) currentZoomScale else 1f,
                    onZoomChanged = { updatedScale ->
                        if (pageNum == currentPageNumber) {
                            currentZoomScale = updatedScale
                        }
                    }
                )
            }
        }

        // 3. Bottom Navigation & Quick Page Slider Bar
        Surface(
            color = if (themeMode == MushafThemeMode.NIGHT_MODE) Color(0xFF10241D) else Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.8.dp, Color(0xFFD9A82E).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Prev Page Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF0F6B4B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = currentPageNumber > 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                                }
                            }
                            .testTag("quran_reader_prev_page_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "পূর্ববর্তী পৃষ্ঠা",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "পূর্ববর্তী",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Center Status & Offline Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showGoToPageDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        if (downloadState.status == QuranDownloadStatus.COMPLETED_VERIFIED) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = Color(0xFF0F6B4B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} / ৬১০ • পারা ${HafeziQuranIndexData.toBanglaDigits(currentPageMeta.paraNumber)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (themeMode == MushafThemeMode.NIGHT_MODE) Color.White else Color(0xFF0F6B4B),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Next Page Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF0F6B4B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = currentPageNumber < HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage + 1).coerceAtMost(HafeziQuranIndexData.TOTAL_MUSHAF_PAGES - 1)
                                    )
                                }
                            }
                            .testTag("quran_reader_next_page_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "পরবর্তী",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "পরবর্তী পৃষ্ঠা",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Slider(
                    value = currentPageNumber.toFloat(),
                    onValueChange = { newVal ->
                        val target = newVal.toInt().coerceIn(1, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)
                        scope.launch {
                            pagerState.scrollToPage(target - 1)
                        }
                    },
                    valueRange = 1f..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD9A82E),
                        activeTrackColor = Color(0xFF0F6B4B),
                        inactiveTrackColor = Color(0xFF0F6B4B).copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .testTag("quran_reader_page_slider")
                )
            }
        }
    }

    // Direct Page Lookup Dialog inside Reader
    if (showGoToPageDialog) {
        DirectPageJumpDialog(
            currentPage = currentPageNumber,
            onDismiss = { showGoToPageDialog = false },
            onJumpToPage = { targetPage ->
                showGoToPageDialog = false
                scope.launch {
                    pagerState.scrollToPage((targetPage - 1).coerceIn(0, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES - 1))
                }
            }
        )
    }

    // Bookmark Note Dialog
    if (showBookmarkNoteDialog) {
        var noteText by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBookmarkNoteDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(currentPageNumber)} বুকমার্ক ও নোট",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF0F6B4B),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "সূরা ${currentPageMeta.primarySurah.nameBangla} • পারা ${HafeziQuranIndexData.toBanglaDigits(currentPageMeta.paraNumber)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4A5D54)
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("বুকমার্ক নোট (ঐচ্ছিক, যেমন: হিফজ সবক / তিলাওয়াত)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quran_bookmark_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        HafeziQuranStorageManager.addOrUpdateBookmark(context, currentPageNumber, noteText)
                        showBookmarkNoteDialog = false
                        feedbackToast = "বুকমার্ক ও নোট সংরক্ষিত হয়েছে"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F6B4B)),
                    modifier = Modifier.testTag("quran_bookmark_save_note_button")
                ) {
                    Text("সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkNoteDialog = false }) {
                    Text("বাতিল", color = Color(0xFF4A5D54))
                }
            }
        )
    }
}

@Composable
private fun HafeziQuranQuranDownloadStateFlow() =
    HafeziQuranStorageManager.downloadStateFlow.collectAsState()

@Composable
fun DirectPageJumpDialog(
    currentPage: Int,
    onDismiss: () -> Unit,
    onJumpToPage: (Int) -> Unit
) {
    var pageInput by rememberSaveable { mutableStateOf(currentPage.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val parsedPreviewPage = HafeziQuranIndexData.parseBanglaOrEnglishNumber(pageInput)
    val previewMeta = remember(parsedPreviewPage) {
        if (parsedPreviewPage != null && parsedPreviewPage in 1..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
            HafeziQuranIndexData.getPageMetadata(parsedPreviewPage)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "পৃষ্ঠা নম্বর দিয়ে সরাসরি খুঁজুন",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF0F6B4B),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ইন্দো-পাক / এমদাদিয়া ১৫ লাইনের হাফেজি মুসহাফের যেকোনো পৃষ্ঠা নম্বর লিখুন (১ থেকে ৬১০):",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A5D54)
                )
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = {
                        pageInput = it
                        errorMsg = null
                    },
                    label = { Text("পৃষ্ঠা নম্বর (১ - ৬১০)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_direct_page_input_field")
                )
                if (previewMeta != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEAF4EF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(previewMeta.pageNumber)} • পারা ${HafeziQuranIndexData.toBanglaDigits(previewMeta.paraNumber)} (${previewMeta.paraNameBangla})",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF0F6B4B),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "সূরা ${previewMeta.primarySurah.nameBangla} (${previewMeta.primarySurah.nameArabic}) • মঞ্জিল ${HafeziQuranIndexData.toBanglaDigits(previewMeta.manzilNumber)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF141A17)
                            )
                        }
                    }
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = HafeziQuranIndexData.parseBanglaOrEnglishNumber(pageInput)
                    if (target != null && target in 1..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
                        onJumpToPage(target)
                    } else {
                        errorMsg = "অনুগ্রহ করে ১ থেকে ৬১০ এর মধ্যে সঠিক পৃষ্ঠা নম্বর লিখুন।"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F6B4B)),
                modifier = Modifier.testTag("quran_direct_page_confirm_button")
            ) {
                Text("পৃষ্ঠায় যান", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = Color(0xFF4A5D54))
            }
        }
    )
}
