package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.quran.HafeziQuranHubScreen
import com.example.ui.quran.HafeziQuranIndexData
import com.example.ui.quran.HafeziQuranStorageManager
import com.example.ui.quran.QuranDownloadStatus
import com.example.ui.screens.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w420dp-h2800dp")
class HafeziQuranAndHomeOrderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        HafeziQuranStorageManager.init(context)
    }

    @Test
    fun verify15LineHafeziMushafIndex_114Surahs_30Paras_610Pages_andExact15Lines() {
        assertEquals(114, HafeziQuranIndexData.all114Surahs.size)
        assertEquals(30, HafeziQuranIndexData.all30Paras.size)
        assertEquals(610, HafeziQuranIndexData.TOTAL_MUSHAF_PAGES)

        // Verify Surah Al-Fatiha (Surah 1) -> Page 2, Para 1, 7 Ayahs
        val fatiha = HafeziQuranIndexData.getSurahByNumber(1)
        assertEquals("আল-ফাতিহা", fatiha.nameBangla)
        assertEquals(7, fatiha.ayahCount)
        assertEquals(2, fatiha.startPage)
        assertEquals(1, fatiha.startPara)

        // Verify Surah Al-Baqarah (Surah 2) -> Page 3, Para 1, 286 Ayahs
        val baqarah = HafeziQuranIndexData.getSurahByNumber(2)
        assertEquals("আল-বাকারা", baqarah.nameBangla)
        assertEquals(286, baqarah.ayahCount)
        assertEquals(3, baqarah.startPage)

        // Verify Surah Ya-Sin (Surah 36) -> Page 441, Para 22, 83 Ayahs
        val yasin = HafeziQuranIndexData.getSurahByNumber(36)
        assertEquals("ইয়াসিন", yasin.nameBangla)
        assertEquals(83, yasin.ayahCount)
        assertEquals(441, yasin.startPage)
        assertEquals(22, yasin.startPara)

        // Verify Surah An-Nas (Surah 114) -> Page 610, Para 30, 6 Ayahs
        val nas = HafeziQuranIndexData.getSurahByNumber(114)
        assertEquals("আন-নাস", nas.nameBangla)
        assertEquals(6, nas.ayahCount)
        assertEquals(610, nas.endPage)
        assertEquals(30, nas.startPara)

        // Verify every sampled page has strictly 15 lines
        listOf(1, 2, 3, 22, 43, 294, 441, 533, 566, 582, 610).forEach { pageNo ->
            val pageMeta = HafeziQuranIndexData.getPageMetadata(pageNo)
            assertEquals("Page $pageNo must have strictly 15 lines", 15, pageMeta.lines.size)
        }
    }

    @Test
    fun verifyUnifiedSearch_Surah_Para_Page_andAyah() {
        // 1. Direct page search (Bangla & English digits)
        val pageSearch = HafeziQuranIndexData.searchAll("৪৪১")
        assertEquals(441, pageSearch.directPageMatch)

        // 2. Surah search by Bangla name
        val surahSearch = HafeziQuranIndexData.searchAll("ইয়াসিন")
        assertTrue(surahSearch.matchingSurahs.any { it.number == 36 && it.startPage == 441 })

        // 3. Para search
        val paraSearch = HafeziQuranIndexData.searchAll("তাবারাকাল্লাযী")
        assertTrue(paraSearch.matchingParas.any { it.number == 29 && it.startPage == 562 })

        // 4. Ayah reference search (2:255 - Ayatul Kursi)
        val ayahSearch = HafeziQuranIndexData.searchAll("2:255")
        assertTrue(ayahSearch.matchingAyahs.any { it.surahNumber == 2 && it.ayahNumber == 255 && it.pageNumber == 43 })
    }

    @Test
    fun verifyBookmarksAndLastReadPagePersistence() {
        // Record reading page 441 (Surah Ya-Sin, Para 22)
        HafeziQuranStorageManager.recordPageVisited(context, 441)
        val progress = HafeziQuranStorageManager.progressFlow.value
        assertEquals(441, progress.lastReadPage)
        assertEquals(36, progress.lastReadSurahNumber)
        assertEquals(22, progress.lastReadParaNumber)
        assertTrue(progress.uniquePagesReadCount >= 1)

        // Bookmark multiple pages (Page 43 and Page 566)
        HafeziQuranStorageManager.addOrUpdateBookmark(context, 43, "আয়াতুল কুরসি")
        HafeziQuranStorageManager.addOrUpdateBookmark(context, 566, "সূরা মুলক তিলাওয়াত")

        assertTrue(HafeziQuranStorageManager.isPageBookmarked(43))
        assertTrue(HafeziQuranStorageManager.isPageBookmarked(566))

        val savedBookmarks = HafeziQuranStorageManager.bookmarksFlow.value
        assertTrue(savedBookmarks.any { it.pageNumber == 43 && it.note == "আয়াতুল কুরসি" })
        assertTrue(savedBookmarks.any { it.pageNumber == 566 && it.note == "সূরা মুলক তিলাওয়াত" })

        // Remove one bookmark
        HafeziQuranStorageManager.removeBookmark(context, 43)
        assertTrue(!HafeziQuranStorageManager.isPageBookmarked(43))
        assertTrue(HafeziQuranStorageManager.isPageBookmarked(566))
    }

    @Test
    fun verifyOfflineDownloadPauseResumeAndSha256FileIntegrity() {
        // Step 1: Partial download (Pages 1..50) simulating Pause at page 50
        val partialReport = HafeziQuranStorageManager.downloadRangeAndVerifySync(context, startPage = 1, endPage = 50)
        assertTrue(partialReport.isValid)
        assertEquals(50, partialReport.verifiedPageCount)
        assertEquals(QuranDownloadStatus.PAUSED, HafeziQuranStorageManager.downloadStateFlow.value.status)

        // Step 2: Resume and complete all 610 pages
        val fullReport = HafeziQuranStorageManager.downloadRangeAndVerifySync(context, startPage = 51, endPage = 610)
        assertTrue(fullReport.isValid)
        assertEquals(610, fullReport.verifiedPageCount)
        assertTrue(fullReport.corruptedOrMissingPages.isEmpty())
        assertEquals(QuranDownloadStatus.COMPLETED_VERIFIED, HafeziQuranStorageManager.downloadStateFlow.value.status)
        assertNotNull(fullReport.sha256DigestPrefix)
        assertTrue(fullReport.sha256DigestPrefix.isNotBlank())
    }

    @Test
    fun verifyHomeScreenSectionOrder_Date_PrayerTimes_AjkerSmoron_IslamicServices() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(420.dp).height(2600.dp)) {
                HomeScreen(
                    currentUserId = "test_user",
                    onNavigate = {},
                    onOpenFeedFilter = {}
                )
            }
        }

        // Verify top elements above the 4 target sections remain intact
        composeTestRule.onNodeWithTag("home_card_publish_application").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_card_madrasa_directory").assertIsDisplayed()

        val madrasaTop = composeTestRule.onNodeWithTag("home_card_madrasa_directory")
            .fetchSemanticsNode().boundsInRoot.top
        val dateTop = composeTestRule.onNodeWithTag("home_todays_date_card")
            .fetchSemanticsNode().boundsInRoot.top
        val prayerTop = composeTestRule.onNodeWithTag("home_prayer_times_card")
            .fetchSemanticsNode().boundsInRoot.top
        val smoronTop = composeTestRule.onNodeWithTag("home_ajker_smoron_card")
            .fetchSemanticsNode().boundsInRoot.top
        val servicesTop = composeTestRule.onNodeWithTag("home_islamic_essential_services_section")
            .fetchSemanticsNode().boundsInRoot.top

        // Verify exact vertical sequence:
        // মাদরাসা (above) -> ১. আজকের তারিখ -> ২. নামাজের সময়সূচি -> ৩. আজকের স্মরণ -> ৪. ইসলামিক ও প্রয়োজনীয় সেবাসমূহ
        assertTrue("মাদরাসা card must be above ১. আজকের তারিখ", madrasaTop < dateTop)
        assertTrue("১. আজকের তারিখ must appear above ২. নামাজের সময়সূচি", dateTop < prayerTop)
        assertTrue("২. নামাজের সময়সূচি must appear above ৩. আজকের স্মরণ", prayerTop < smoronTop)
        assertTrue("৩. আজকের স্মরণ must appear above ৪. ইসলামিক ও প্রয়োজনীয় সেবাসমূহ", smoronTop < servicesTop)
    }

    @Test
    fun verifyHafeziQuranHubScreenAndReaderNavigation() {
        composeTestRule.setContent {
            HafeziQuranHubScreen(onCloseHub = {})
        }

        composeTestRule.onNodeWithTag("hafezi_quran_hub_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_hub_hero_resume_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quran_hub_offline_download_card").assertIsDisplayed()

        // Scroll to and click on Surah 1 (Al-Fatiha) to open the 15-Line Hafezi Mushaf Reader at Page 2
        composeTestRule.onNodeWithTag("quran_hub_lazy_list")
            .performScrollToNode(hasTestTag("quran_surah_item_1"))
        composeTestRule.onNodeWithTag("quran_surah_item_1").performClick()

        composeTestRule.onNodeWithTag("hafezi_quran_reader_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hafezi_mushaf_page_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hafezi_mushaf_15_lines_container").assertIsDisplayed()
    }
}
