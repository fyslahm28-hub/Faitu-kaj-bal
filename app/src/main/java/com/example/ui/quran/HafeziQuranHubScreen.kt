package com.example.ui.quran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.IslamicStarEmblem
import kotlinx.coroutines.launch

private val QuranPrimaryGreen = Color(0xFF0F6B4B)
private val QuranDeepEmerald = Color(0xFF0A4D36)
private val QuranGold = Color(0xFFD9A82E)
private val QuranLightBg = Color(0xFFF6FAF8)
private val QuranMintBorder = Color(0xFFCFE6DC)

enum class QuranHubTab(val titleBangla: String, val testTag: String) {
    SURAHS("১১৪ সূরা", "quran_hub_tab_surahs"),
    PARAS("৩০ পারা", "quran_hub_tab_paras"),
    BOOKMARKS("বুকমার্ক ও পৃষ্ঠা", "quran_hub_tab_bookmarks"),
    HISTORY("ইতিহাস ও অগ্রগতি", "quran_hub_tab_history")
}

/**
 * Full-Screen Dialog wrapper for the Indo-Pak / Emdadia 15-Line Hafezi Quran Hub.
 */
@Composable
fun HafeziQuranHubDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        HafeziQuranHubScreen(onCloseHub = onDismiss)
    }
}

/**
 * Full-Screen Modern Islamic Quran Hub (ইন্দো-পাক / এমদাদিয়া ১৫ লাইনের হাফেজি কুরআন শরীফ).
 */
@Composable
fun HafeziQuranHubScreen(
    onCloseHub: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        HafeziQuranStorageManager.init(context)
    }

    val progress by HafeziQuranStorageManager.progressFlow.collectAsState()
    val bookmarks by HafeziQuranStorageManager.bookmarksFlow.collectAsState()
    val history by HafeziQuranStorageManager.historyFlow.collectAsState()
    val downloadState by HafeziQuranStorageManager.downloadStateFlow.collectAsState()
    val themeMode by HafeziQuranStorageManager.themeModeFlow.collectAsState()

    val isNightMode = themeMode == MushafThemeMode.NIGHT_MODE
    val screenBg = if (isNightMode) Color(0xFF0B1914) else QuranLightBg
    val cardBg = if (isNightMode) Color(0xFF132A22) else Color.White
    val primaryText = if (isNightMode) Color(0xFFF3EFE2) else Color(0xFF12241D)
    val secondaryText = if (isNightMode) Color(0xFFB7CFC4) else Color(0xFF4A6359)

    var activeReaderPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedTabOrdinal by rememberSaveable { mutableIntStateOf(QuranHubTab.SURAHS.ordinal) }
    val selectedTab = QuranHubTab.entries.getOrElse(selectedTabOrdinal) { QuranHubTab.SURAHS }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var directPageQuery by rememberSaveable { mutableStateOf("") }
    var showDirectPageModal by rememberSaveable { mutableStateOf(false) }

    val searchResults = remember(searchQuery) {
        HafeziQuranIndexData.searchAll(searchQuery)
    }

    // If Reader is open, render the full-screen 15-Line Hafezi Mushaf Reader
    if (activeReaderPage != null) {
        BackHandler {
            activeReaderPage = null
        }
        HafeziQuranReaderView(
            initialPage = activeReaderPage ?: 2,
            onBackToHub = { activeReaderPage = null }
        )
        return
    }

    BackHandler {
        onCloseHub()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("hafezi_quran_hub_screen")
    ) {
        // 1. Top Islamic Quran Hub AppBar
        Surface(
            color = QuranPrimaryGreen,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onCloseHub,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("quran_hub_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IslamicStarEmblem(size = 15.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "হাফেজি কুরআন শরীফ",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "ইন্দো-পাক / এমদাদিয়া ১৫ লাইন মুসহাফ • ৬১০ পৃষ্ঠা",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFE082),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Direct Page Lookup Button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = QuranDeepEmerald,
                        modifier = Modifier
                            .border(1.dp, QuranGold, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable { showDirectPageModal = true }
                            .testTag("quran_hub_direct_page_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FindInPage,
                                contentDescription = "পৃষ্ঠা খুঁজুন",
                                tint = QuranGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "পৃষ্ঠা খুঁজুন",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Night Mode Toggle
                    IconButton(
                        onClick = {
                            val next = if (isNightMode) MushafThemeMode.EMDADIA_CREAM else MushafThemeMode.NIGHT_MODE
                            HafeziQuranStorageManager.setMushafTheme(context, next)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("quran_hub_night_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "নাইট মোড",
                            tint = QuranGold
                        )
                    }
                }
            }
        }

        // 2. Hub Scrollable Body
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("quran_hub_lazy_list"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // A. Hero Card: Resume Last Read + Progress + 15-Line Emdadia Authenticity Badge
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(18.dp))
                        .border(1.5.dp, QuranGold, RoundedCornerShape(18.dp))
                        .testTag("quran_hub_hero_resume_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = QuranPrimaryGreen)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(QuranPrimaryGreen, QuranDeepEmerald)
                                )
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = QuranGold.copy(alpha = 0.22f),
                                    modifier = Modifier.border(0.8.dp, QuranGold, RoundedCornerShape(50))
                                ) {
                                    Text(
                                        text = "এমদাদিয়া ১৫ লাইন হাফেজি মুসহাফ",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        color = Color(0xFFFFE082),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "শেষ পড়া পৃষ্ঠা: ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadPage)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "সূরা ${progress.lastReadSurahNameBangla} • পারা ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadParaNumber)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFDDF3EC)
                                )
                            }

                            Button(
                                onClick = { activeReaderPage = progress.lastReadPage },
                                colors = ButtonDefaults.buttonColors(containerColor = QuranGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("quran_hub_resume_last_read_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF0B1914),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "পড়া চালিয়ে যান",
                                    color = Color(0xFF0B1914),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        // Reading Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "তিলাওয়াত অগ্রগতি: ${HafeziQuranIndexData.toBanglaDigits(progress.uniquePagesReadCount)} / ৬১০ পৃষ্ঠা",
                                    color = Color(0xFFDDF3EC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${HafeziQuranIndexData.toBanglaDigits(progress.progressPercent)}% সম্পন্ন",
                                    color = Color(0xFFFFE082),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = QuranGold,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // B. Offline Download, Pause/Resume & File Integrity Verification Card
            item {
                OfflineMushafDownloadCard(
                    downloadState = downloadState,
                    cardBg = cardBg,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onStartOrResume = {
                        HafeziQuranStorageManager.startOrResumeOfflineDownload(context, scope)
                    },
                    onPause = {
                        HafeziQuranStorageManager.pauseOfflineDownload(context)
                    },
                    onVerifyIntegrity = {
                        scope.launch {
                            HafeziQuranStorageManager.verifyOfflineFilesIntegrity(context)
                        }
                    }
                )
            }

            // C. Unified Search (Surah, Para, Page, Ayah) & Quick Direct Page Input Row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QuranMintBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "সূরা, পারা, পৃষ্ঠা বা আয়াত খুঁজুন (যেমন: ইয়াসিন, ২:২৫৫, ৩০৫)...",
                                    fontSize = 12.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = QuranPrimaryGreen
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "মুছুন",
                                            tint = secondaryText
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quran_hub_unified_search_input")
                        )

                        // Quick Direct Page Number Jump Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = directPageQuery,
                                onValueChange = { directPageQuery = it },
                                placeholder = { Text("পৃষ্ঠা নম্বর লিখুন (১ - ৬১০)", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quran_hub_quick_page_input")
                            )
                            Button(
                                onClick = {
                                    val page = HafeziQuranIndexData.parseBanglaOrEnglishNumber(directPageQuery)
                                    if (page != null && page in 1..HafeziQuranIndexData.TOTAL_MUSHAF_PAGES) {
                                        activeReaderPage = page
                                    } else {
                                        showDirectPageModal = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = QuranPrimaryGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("quran_hub_quick_page_go_button")
                            ) {
                                Text(
                                    text = "সরাসরি পৃষ্ঠায় যান",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // If search query matches a direct page or Ayahs, show instant jump chips
                        if (searchQuery.isNotBlank()) {
                            SearchInstantMatchesSection(
                                searchResults = searchResults,
                                onOpenPage = { page -> activeReaderPage = page }
                            )
                        }
                    }
                }
            }

            // D. 4 Category Tabs (১১৪ সূরা | ৩০ পারা | বুকমার্ক ও পৃষ্ঠা | ইতিহাস ও অগ্রগতি)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuranHubTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) QuranGold else QuranMintBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTabOrdinal = tab.ordinal }
                                .testTag(tab.testTag),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) QuranPrimaryGreen else cardBg
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.titleBangla,
                                    color = if (isSelected) Color.White else primaryText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // E. Tab Content List
            when (selectedTab) {
                QuranHubTab.SURAHS -> {
                    items(
                        items = searchResults.matchingSurahs,
                        key = { "surah_${it.number}" }
                    ) { surah ->
                        SurahListItemCard(
                            surah = surah,
                            cardBg = cardBg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { activeReaderPage = surah.startPage }
                        )
                    }
                }

                QuranHubTab.PARAS -> {
                    items(
                        items = searchResults.matchingParas,
                        key = { "para_${it.number}" }
                    ) { para ->
                        ParaListItemCard(
                            para = para,
                            cardBg = cardBg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { activeReaderPage = para.startPage }
                        )
                    }
                }

                QuranHubTab.BOOKMARKS -> {
                    if (bookmarks.isEmpty()) {
                        item {
                            EmptyStateInfoCard(
                                title = "কোনো বুকমার্ক সংরক্ষিত নেই",
                                subtitle = "মুসহাফের যেকোনো পৃষ্ঠা পড়ার সময় উপরের বুকমার্ক আইকনে ট্যাপ করে একাধিক পৃষ্ঠা ও নোট সংরক্ষণ করতে পারবেন।",
                                cardBg = cardBg,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                        }
                    } else {
                        items(
                            items = bookmarks,
                            key = { "bm_${it.pageNumber}" }
                        ) { bm ->
                            BookmarkItemCard(
                                bookmark = bm,
                                cardBg = cardBg,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onOpenPage = { activeReaderPage = bm.pageNumber },
                                onDelete = {
                                    HafeziQuranStorageManager.removeBookmark(context, bm.pageNumber)
                                }
                            )
                        }
                    }

                    // Featured Ayahs & Quick Pages Reference
                    item {
                        Text(
                            text = "গুরুত্বপূর্ণ সূরা ও আয়াতের পৃষ্ঠাসমূহ",
                            style = MaterialTheme.typography.titleSmall,
                            color = primaryText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(
                        items = HafeziQuranIndexData.featuredAyahSearchIndex,
                        key = { "feat_${it.surahNumber}_${it.ayahNumber}" }
                    ) { entry ->
                        FeaturedAyahCard(
                            entry = entry,
                            cardBg = cardBg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { activeReaderPage = entry.pageNumber }
                        )
                    }
                }

                QuranHubTab.HISTORY -> {
                    item {
                        ReadingProgressSummaryCard(
                            progress = progress,
                            historyCount = history.size,
                            cardBg = cardBg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onResumeClick = { activeReaderPage = progress.lastReadPage }
                        )
                    }
                    if (history.isEmpty()) {
                        item {
                            EmptyStateInfoCard(
                                title = "পড়ার ইতিহাস এখনো শুরু হয়নি",
                                subtitle = "আপনি যে পৃষ্ঠাগুলো তিলাওয়াত করবেন তার ক্রমানুসারে তালিকা এখানে সংরক্ষিত থাকবে।",
                                cardBg = cardBg,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                        }
                    } else {
                        items(
                            items = history,
                            key = { "hist_${it.pageNumber}_${it.visitedAtMillis}" }
                        ) { item ->
                            HistoryItemCard(
                                entry = item,
                                cardBg = cardBg,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { activeReaderPage = item.pageNumber }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDirectPageModal) {
        DirectPageJumpDialog(
            currentPage = progress.lastReadPage,
            onDismiss = { showDirectPageModal = false },
            onJumpToPage = { targetPage ->
                showDirectPageModal = false
                activeReaderPage = targetPage
            }
        )
    }
}

@Composable
private fun OfflineMushafDownloadCard(
    downloadState: QuranDownloadState,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onVerifyIntegrity: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(16.dp))
            .testTag("quran_hub_offline_download_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = QuranPrimaryGreen.copy(alpha = 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (downloadState.status == QuranDownloadStatus.COMPLETED_VERIFIED) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.CloudDownload
                                },
                                contentDescription = null,
                                tint = QuranPrimaryGreen,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "অফলাইন হাফেজি মুসহাফ ও Integrity যাচাই",
                            style = MaterialTheme.typography.titleSmall,
                            color = primaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = downloadState.statusMessageBangla,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { downloadState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .testTag("quran_offline_download_progress_bar"),
                color = QuranPrimaryGreen,
                trackColor = QuranMintBorder
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ডাউনলোড: ${HafeziQuranIndexData.toBanglaDigits(downloadState.downloadedPages)}/৬১০ পৃষ্ঠা (${HafeziQuranIndexData.toBanglaDigits(downloadState.progressPercent)}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = QuranPrimaryGreen,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (downloadState.status == QuranDownloadStatus.DOWNLOADING) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFB45309),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onPause)
                                .testTag("quran_offline_pause_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "বিরতি (Pause)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = QuranPrimaryGreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onStartOrResume)
                                .testTag("quran_offline_start_resume_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (downloadState.status) {
                                        QuranDownloadStatus.PAUSED -> "পুনরায় শুরু (Resume)"
                                        QuranDownloadStatus.COMPLETED_VERIFIED -> "পুনরায় সিঙ্ক"
                                        else -> "অফলাইন ডাউনলোড"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = QuranGold.copy(alpha = 0.2f),
                        modifier = Modifier
                            .border(1.dp, QuranGold, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onVerifyIntegrity)
                            .testTag("quran_offline_verify_integrity_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = QuranPrimaryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Integrity যাচাই",
                                color = QuranPrimaryGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInstantMatchesSection(
    searchResults: HafeziQuranIndexData.QuranUnifiedSearchResults,
    onOpenPage: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (searchResults.directPageMatch != null) {
            val page = searchResults.directPageMatch
            val meta = HafeziQuranIndexData.getPageMetadata(page)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEAF4EF),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QuranPrimaryGreen, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenPage(page) }
                    .testTag("quran_search_direct_page_result_$page")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "সরাসরি পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(page)}-এ যান",
                            style = MaterialTheme.typography.labelLarge,
                            color = QuranPrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "সূরা ${meta.primarySurah.nameBangla} • পারা ${HafeziQuranIndexData.toBanglaDigits(meta.paraNumber)} (${meta.paraNameBangla})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF141A17)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = QuranPrimaryGreen
                    )
                }
            }
        }

        searchResults.matchingAyahs.take(3).forEach { ayah ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFF8E1),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QuranGold, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenPage(ayah.pageNumber) }
                    .testTag("quran_search_ayah_result_${ayah.surahNumber}_${ayah.ayahNumber}")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${ayah.banglaTopicOrTranslation} (${HafeziQuranIndexData.toBanglaDigits(ayah.referenceKey)})",
                            style = MaterialTheme.typography.labelMedium,
                            color = QuranPrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(ayah.pageNumber)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = ayah.arabicText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF141A17),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahListItemCard(
    surah: HafeziSurahInfo,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("quran_surah_item_${surah.number}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = QuranPrimaryGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = HafeziQuranIndexData.toBanglaDigits(surah.number),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "সূরা ${surah.nameBangla} (${surah.meaningBangla})",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${surah.revelationTypeBangla} • আয়াত: ${HafeziQuranIndexData.toBanglaDigits(surah.ayahCount)} • পারা: ${HafeziQuranIndexData.toBanglaDigits(surah.startPara)} • পৃষ্ঠা: ${HafeziQuranIndexData.toBanglaDigits(surah.startPage)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = surah.nameArabic,
                    style = MaterialTheme.typography.titleMedium,
                    color = QuranPrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = QuranGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(surah.startPage)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = QuranPrimaryGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ParaListItemCard(
    para: HafeziParaInfo,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("quran_para_item_${para.number}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = QuranPrimaryGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = HafeziQuranIndexData.toBanglaDigits(para.number),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "পারা ${HafeziQuranIndexData.toBanglaDigits(para.number)}: ${para.nameBangla}",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(para.startPage)} - ${HafeziQuranIndexData.toBanglaDigits(para.endPage)} • শুরু: সূরা ${para.startSurahNameBangla} (${para.startAyahLabel})",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = para.nameArabic,
                    style = MaterialTheme.typography.titleMedium,
                    color = QuranPrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = QuranGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(para.startPage)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = QuranPrimaryGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkItemCard(
    bookmark: QuranBookmark,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onOpenPage: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranGold, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenPage)
            .testTag("quran_bookmark_card_${bookmark.pageNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = QuranGold,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(bookmark.pageNumber)} • সূরা ${bookmark.surahNameBangla}",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "পারা ${HafeziQuranIndexData.toBanglaDigits(bookmark.paraNumber)} • ${bookmark.note}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("quran_bookmark_delete_${bookmark.pageNumber}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "মুছে ফেলুন",
                    tint = Color(0xFFC62828)
                )
            }
        }
    }
}

@Composable
private fun FeaturedAyahCard(
    entry: HafeziAyahSearchEntry,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.banglaTopicOrTranslation,
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = QuranPrimaryGreen
                ) {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(entry.pageNumber)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.arabicText,
                style = MaterialTheme.typography.bodyMedium,
                color = QuranPrimaryGreen,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "সূরা ${entry.surahNameBangla} (${HafeziQuranIndexData.toBanglaDigits(entry.referenceKey)}) • পারা ${HafeziQuranIndexData.toBanglaDigits(entry.paraNumber)}",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText
            )
        }
    }
}

@Composable
private fun ReadingProgressSummaryCard(
    progress: QuranReadingProgress,
    historyCount: Int,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onResumeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranGold, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "তিলাওয়াত ও খতম অগ্রগতির সারসংক্ষেপ",
                style = MaterialTheme.typography.titleSmall,
                color = primaryText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• মোট পঠিত পৃষ্ঠা: ${HafeziQuranIndexData.toBanglaDigits(progress.uniquePagesReadCount)} / ৬১০ পৃষ্ঠা (${HafeziQuranIndexData.toBanglaDigits(progress.progressPercent)}%)\n" +
                    "• সর্বশেষ পঠিত পৃষ্ঠা: পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadPage)} (সূরা ${progress.lastReadSurahNameBangla}, পারা ${HafeziQuranIndexData.toBanglaDigits(progress.lastReadParaNumber)})\n" +
                    "• সংরক্ষিত সেশন ইতিহাস: ${HafeziQuranIndexData.toBanglaDigits(historyCount)}টি পৃষ্ঠা",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText
            )
            Button(
                onClick = onResumeClick,
                colors = ButtonDefaults.buttonColors(containerColor = QuranPrimaryGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("সর্বশেষ পঠিত পৃষ্ঠায় ফিরে যান", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    entry: QuranReadingHistoryEntry,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("quran_history_item_${entry.pageNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = QuranPrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "পৃষ্ঠা ${HafeziQuranIndexData.toBanglaDigits(entry.pageNumber)} • সূরা ${entry.surahNameBangla}",
                        style = MaterialTheme.typography.titleSmall,
                        color = primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "পারা ${HafeziQuranIndexData.toBanglaDigits(entry.paraNumber)} • ইন্দো-পাক ১৫ লাইন হাফেজি মুসহাফ",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryText
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = QuranPrimaryGreen
            )
        }
    }
}

@Composable
private fun EmptyStateInfoCard(
    title: String,
    subtitle: String,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QuranMintBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = primaryText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText
            )
        }
    }
}
