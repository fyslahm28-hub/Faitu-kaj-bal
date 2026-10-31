package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.IslamicStarEmblem
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.AsrMadhhab
import com.example.ui.util.AzanSoundType
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.util.PrayerAzanManager
import com.example.ui.util.PrayerCalculationMethod
import com.example.ui.util.PrayerUiLanguage
import com.example.ui.util.PrayerWaqtKey
import com.example.ui.viewmodel.MainDestination
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

data class IslamicServiceGridItem(
    val id: String,
    val titleBangla: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val testTag: String
)

/**
 * Home Screen section: "ইসলামিক ও প্রয়োজনীয় সেবাসমূহ" matching the existing Islamic Green, White & Gold design.
 * Clicking "নামাজের সময়" opens the full Prayer Times & Auto Azan Screen.
 */
@Composable
fun IslamicAndEssentialServicesSection(
    onOpenPrayerTimes: () -> Unit,
    onNavigate: (MainDestination) -> Unit
) {
    var selectedInfoItem by remember { mutableStateOf<IslamicServiceGridItem?>(null) }

    val ilmAndAmolItems = remember {
        listOf(
            IslamicServiceGridItem("quran", "আল-কুরআন", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_quran"),
            IslamicServiceGridItem("hadith", "হাদিস", Icons.Default.LibraryBooks, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_hadith"),
            IslamicServiceGridItem("prayer_times", "নামাজের সময়", Icons.Default.Mosque, Color(0xFF0E5E46), Color(0xFFFFF8E1), "home_islamic_service_prayer_times"),
            IslamicServiceGridItem("tilawat", "তিলাওয়াত", Icons.Default.Headphones, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_tilawat"),
            IslamicServiceGridItem("islamic_books", "ইসলামিক বই", Icons.Default.School, Color(0xFF1565C0), Color(0xFFE3F2FD), "home_islamic_service_books"),
            IslamicServiceGridItem("articles", "প্রবন্ধ", Icons.Default.EditNote, Color(0xFFB45309), Color(0xFFFFF8E1), "home_islamic_service_articles"),
            IslamicServiceGridItem("quran_learning", "কুরআন শিক্ষা", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_quran_edu"),
            IslamicServiceGridItem("dua", "দোয়া", Icons.Default.VolunteerActivism, Color(0xFFD97706), Color(0xFFFFF8E1), "home_islamic_service_dua"),
            IslamicServiceGridItem("namaz_learning", "নামাজ শিক্ষা", Icons.Default.Mosque, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_namaz_edu"),
            IslamicServiceGridItem("asmaul_husna", "আসমাউল হুসনা", Icons.Default.AutoAwesome, Color(0xFF0E5E46), Color(0xFFFFF8E1), "home_islamic_service_asmaul_husna"),
            IslamicServiceGridItem("ruqyah", "রুকইয়া", Icons.Default.Security, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_ruqyah"),
            IslamicServiceGridItem("sunnah", "সুন্নাহ", Icons.Default.VerifiedUser, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_sunnah"),
            IslamicServiceGridItem("islamic_names", "ইসলামিক নাম", Icons.Default.AccountBalance, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_names")
        )
    }

    val dailyServiceItems = remember {
        listOf(
            IslamicServiceGridItem("tasbih", "তাসবিহ", Icons.Default.AutoAwesome, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_tasbih"),
            IslamicServiceGridItem("boyan", "বয়ান", Icons.Default.Campaign, Color(0xFFB45309), Color(0xFFFFF8E1), "home_islamic_service_boyan"),
            IslamicServiceGridItem("find_mosque", "মসজিদ খুঁজুন", Icons.Default.Mosque, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_mosque"),
            IslamicServiceGridItem("qibla", "কিবলা", Icons.Default.Explore, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_qibla"),
            IslamicServiceGridItem("live", "লাইভ", Icons.Default.LiveTv, Color(0xFFC62828), Color(0xFFFFEBEE), "home_islamic_service_live"),
            IslamicServiceGridItem("islamic_calendar", "ইসলামিক ক্যালেন্ডার", Icons.Default.CalendarMonth, Color(0xFF0E5E46), Color(0xFFFFF8E1), "home_islamic_service_calendar"),
            IslamicServiceGridItem("important_days", "গুরুত্বপূর্ণ দিন", Icons.Default.Event, Color(0xFFC62828), Color(0xFFFFEBEE), "home_islamic_service_days"),
            IslamicServiceGridItem("bookmarks", "বুকমার্ক", Icons.Default.Bookmark, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_bookmarks"),
            IslamicServiceGridItem("others", "অন্যান্য", Icons.Default.GridView, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_others")
        )
    }

    val welfareServiceItems = remember {
        listOf(
            IslamicServiceGridItem("blood_donation", "রক্তদান", Icons.Default.Bloodtype, Color(0xFFC62828), Color(0xFFFFEBEE), "home_islamic_service_blood"),
            IslamicServiceGridItem("my_mosque", "আমার মসজিদ", Icons.Default.Mosque, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_my_mosque"),
            IslamicServiceGridItem("support", "সাপোর্ট", Icons.Default.SupportAgent, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_support"),
            IslamicServiceGridItem("quiz", "ইসলামিক কুইজ", Icons.Default.Quiz, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_quiz"),
            IslamicServiceGridItem("amol_tracker", "আমল ট্র্যাকার", Icons.Default.CheckCircle, Color(0xFFB45309), Color(0xFFFFF8E1), "home_islamic_service_amol_tracker"),
            IslamicServiceGridItem("deeni_question", "দ্বীনি জিজ্ঞাসা", Icons.AutoMirrored.Filled.HelpOutline, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_deeni_qa"),
            IslamicServiceGridItem("marriage", "বিবাহ", Icons.Default.Groups, Color(0xFFB45309), Color(0xFFFFF8E1), "home_islamic_service_marriage"),
            IslamicServiceGridItem("zakat", "যাকাত ও ফিতরা", Icons.Default.VolunteerActivism, Color(0xFF0E5E46), Color(0xFFE8F5E9), "home_islamic_service_zakat"),
            IslamicServiceGridItem("sadaqah", "সদাকাহ ও দান", Icons.Default.Favorite, Color(0xFFC62828), Color(0xFFFFEBEE), "home_islamic_service_sadaqah")
        )
    }

    fun handleServiceClick(item: IslamicServiceGridItem) {
        when (item.id) {
            "prayer_times", "islamic_calendar" -> onOpenPrayerTimes()
            "bookmarks" -> onNavigate(MainDestination.BOOKMARKS)
            "support" -> onNavigate(MainDestination.HELP_AND_SUPPORT)
            "amol_tracker", "dua", "hadith", "sunnah" -> onNavigate(MainDestination.AJKER_SMORON)
            "my_mosque", "find_mosque" -> onNavigate(MainDestination.MADRASA)
            else -> selectedInfoItem = item
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(18.dp))
            .testTag("home_islamic_essential_services_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = QawmiDeepGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = null,
                            tint = QawmiGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ইসলামিক ও প্রয়োজনীয় সেবাসমূহ",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "নতুন নতুন দ্বীনি ও জনকল্যাণমূলক সুবিধা যুক্ত হচ্ছে ধাপে ধাপে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }

            // Category 1: ইলম ও আমল
            ServiceSubCategoryBlock(
                badgeTitle = "ইলম ও আমল",
                items = ilmAndAmolItems,
                highlightedItemId = "prayer_times",
                onItemClick = ::handleServiceClick
            )

            // Category 2: ইসলামিক ও দৈনন্দিন সেবা
            ServiceSubCategoryBlock(
                badgeTitle = "ইসলামিক ও দৈনন্দিন সেবা",
                items = dailyServiceItems,
                highlightedItemId = "",
                onItemClick = ::handleServiceClick
            )

            // Category 3: জনকল্যাণমূলক সেবা
            ServiceSubCategoryBlock(
                badgeTitle = "জনকল্যাণমূলক সেবা",
                items = welfareServiceItems,
                highlightedItemId = "",
                onItemClick = ::handleServiceClick
            )
        }
    }

    if (selectedInfoItem != null) {
        val item = selectedInfoItem!!
        AlertDialog(
            onDismissRequest = { selectedInfoItem = null },
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = QawmiDeepGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.titleBangla,
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "আলহামদুলিল্লাহ, '${item.titleBangla}' বিভাগের বিস্তারিত কন্টেন্ট ধাপে ধাপে সংযোজন করা হচ্ছে। এখন নামাজের সময়সূচি ও স্বয়ংক্রিয় আজান সেবা দেখতে নিচের বাটনে ট্যাপ করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QawmiTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedInfoItem = null
                        onOpenPrayerTimes()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen)
                ) {
                    Text("নামাজের সময়সূচি দেখুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInfoItem = null }) {
                    Text("বন্ধ করুন", color = QawmiTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ServiceSubCategoryBlock(
    badgeTitle: String,
    items: List<IslamicServiceGridItem>,
    highlightedItemId: String,
    onItemClick: (IslamicServiceGridItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = QawmiDeepGreen
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IslamicStarEmblem(size = 13.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = badgeTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val rows = remember(items) { items.chunked(4) }
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val isHighlighted = item.id == highlightedItemId
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isHighlighted) 1.5.dp else 1.dp,
                                color = if (isHighlighted) QawmiGold else QawmiMintBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onItemClick(item) }
                            .testTag(item.testTag),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color = item.iconBg
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.titleBangla,
                                        tint = item.iconTint,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.titleBangla,
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiTextDark,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Complete Prayer Times & Automatic Azan Sound System Screen.
 * Supports:
 * - 5 Daily Waqts (Fajr, Dhuhr, Asr, Maghrib, Isha) + Sunrise, Sunset, Sehri & Iftar
 * - Current Time, Active Waqt Highlight & Live Second-by-Second Countdown to Next Prayer
 * - Hijri, Bangla Ponjika (বঙ্গাব্দ) & Gregorian Dates
 * - Current GPS/Network Location Detection + Manual 64 Bangladesh Districts & Pinning
 * - Calculation Method & Asr Madhhab (Hanafi / Shafi'i) Selection
 * - Automatic Azan Sound System (4 Sound Types: Full Azan, Short Azan, Reminder Tone, Notification Only)
 * - Per-Waqt Individual Controls (Enable/Disable, Sound Type, Volume Slider, Pre-Reminder 5/10/15/Custom min)
 * - Live Sound Preview & Instant Stop Controls
 * - Android Notification, Exact Alarm, Battery Optimization & DND/Silent Mode Guidance
 * - Bilingual UI (Bangla & English)
 */
@Composable
fun PrayerTimesAndAzanScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        IslamicDateAndPrayerHelper.initWithContext(context)
        PrayerAzanManager.initWithContext(context)
    }

    val selectedDistrict by IslamicDateAndPrayerHelper.selectedDistrictFlow.collectAsState()
    val pinnedDistrict by IslamicDateAndPrayerHelper.pinnedDistrictFlow.collectAsState()
    val favoriteDistricts by IslamicDateAndPrayerHelper.favoriteDistrictsFlow.collectAsState()
    val preferences by PrayerAzanManager.preferencesFlow.collectAsState()
    val playbackState by PrayerAzanManager.playbackStateFlow.collectAsState()

    val isBangla = preferences.language == PrayerUiLanguage.BANGLA

    var showDistrictPicker by rememberSaveable { mutableStateOf(false) }
    var showCalcMethodDialog by rememberSaveable { mutableStateOf(false) }
    var expandedWaqtId by rememberSaveable { mutableStateOf(PrayerWaqtKey.FAJR.id) }
    var locationFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var permissionRefreshTick by remember { mutableIntStateOf(0) }
    var clockTick by remember { mutableIntStateOf(0) }

    // Live 1-second ticker for real-time countdown and current time
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            clockTick++
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRefreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val res = PrayerAzanManager.detectCurrentDeviceLocation(context)
            res.onSuccess { matched ->
                locationFeedbackMessage = if (isBangla) {
                    "বর্তমান লোকেশন অনুযায়ী '${matched.nameBangla}' জেলার সময়সূচি সেট করা হয়েছে।"
                } else {
                    "Prayer times updated to current location (${PrayerAzanManager.getDistrictEnglishName(matched.nameBangla)})."
                }
            }.onFailure { err ->
                locationFeedbackMessage = err.message
            }
        } else {
            locationFeedbackMessage = if (isBangla) {
                "লোকেশন অনুমতি না দেওয়ায় ম্যানুয়ালি নির্বাচিত জেলা (${selectedDistrict}) ব্যবহৃত হচ্ছে।"
            } else {
                "Location permission denied; using manually selected district (${PrayerAzanManager.getDistrictEnglishName(selectedDistrict)})."
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionRefreshTick++
    }

    val dailySchedule = remember(selectedDistrict, preferences, clockTick) {
        PrayerAzanManager.computeDailyPrayerSchedule(
            districtBangla = selectedDistrict,
            preferences = preferences,
            calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
        )
    }

    val devicePermStatus = remember(context, permissionRefreshTick, preferences) {
        PrayerAzanManager.checkDeviceAlarmPermissions(context)
    }

    val divisionsAndFavorites = remember(selectedDistrict, pinnedDistrict, favoriteDistricts) {
        val orderedNames = (listOf(selectedDistrict, pinnedDistrict) +
            favoriteDistricts +
            listOf("ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী", "খুলনা", "বরিশাল", "রংপুর", "ময়মনসিংহ"))
            .distinct()
        orderedNames.map { IslamicDateAndPrayerHelper.getScheduleForDistrict(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("full_prayer_schedule_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Top Title, Language Switch (BN/EN), GPS Auto-Location & 64-District Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) {
                                "বাংলাদেশের জেলা ও বিভাগওয়ারী নামাজের সময়সূচী"
                            } else {
                                "Prayer Times & Auto Azan System"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) {
                                "${preferences.asrMadhhab.titleBangla} • পিন করা জেলা: $pinnedDistrict"
                            } else {
                                "${preferences.asrMadhhab.titleEnglish} • Pinned: ${PrayerAzanManager.getDistrictEnglishName(pinnedDistrict)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Language Toggle (বাংলা / EN)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                val nextLang = if (isBangla) PrayerUiLanguage.ENGLISH else PrayerUiLanguage.BANGLA
                                PrayerAzanManager.setUiLanguage(context, nextLang)
                            }
                            .testTag("prayer_language_toggle_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "EN" else "বাংলা",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Manual 64-District Selector Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showDistrictPicker = true }
                            .testTag("full_schedule_change_district_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = QawmiDeepGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = QawmiGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "জেলা নির্বাচন (৬৪)" else "Select District (64)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // GPS Current Location + Calculation Method Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 1.dp,
                                color = if (preferences.useGpsLocation) QawmiGold else QawmiMintBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                val res = PrayerAzanManager.detectCurrentDeviceLocation(context)
                                if (res.isSuccess) {
                                    val matched = res.getOrNull()
                                    if (matched != null) {
                                        locationFeedbackMessage = if (isBangla) {
                                            "বর্তমান লোকেশন অনুযায়ী '${matched.nameBangla}' জেলার সময়সূচি সক্রিয় করা হয়েছে।"
                                        } else {
                                            "Updated to current GPS location (${PrayerAzanManager.getDistrictEnglishName(matched.nameBangla)})."
                                        }
                                    }
                                } else {
                                    runCatching {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                            }
                            .testTag("prayer_gps_location_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (preferences.useGpsLocation) Color(0xFFE8F5E9) else Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) {
                                    if (preferences.useGpsLocation) "GPS লোকেশন সক্রিয় ($selectedDistrict)" else "বর্তমান লোকেশন (GPS)"
                                } else {
                                    if (preferences.useGpsLocation) "GPS Active (${dailySchedule.locationNameEnglish})" else "Use Current Location (GPS)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                            .clickable { showCalcMethodDialog = true }
                            .testTag("prayer_calc_method_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBangla) "গণনা ও মাজহাব" else "Method & Madhhab",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!locationFeedbackMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = QawmiMintSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = locationFeedbackMessage.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { locationFeedbackMessage = null }) {
                                Text(if (isBangla) "ঠিক আছে" else "OK", color = QawmiDeepGreen)
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick-switch chips for Pinned & Favorite Districts
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                favoriteDistricts.forEach { dist ->
                    val isSelected = dist == selectedDistrict
                    val isPinned = dist == pinnedDistrict
                    val label = if (isBangla) dist else PrayerAzanManager.getDistrictEnglishName(dist)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { PrayerAzanManager.setManualDistrictSelection(context, dist) }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) QawmiDeepGreen else QawmiMintBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .testTag("schedule_fav_chip_$dist"),
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) QawmiDeepGreen else QawmiMintSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPinned) "📌 $label" else label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Active Azan / Preview Playback Control Banner (shown whenever Azan or preview is playing)
        if (playbackState.isPlaying) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, QawmiGold, RoundedCornerShape(14.dp))
                        .testTag("active_azan_playing_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = QawmiDarkGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = QawmiGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isBangla) playbackState.statusBangla else playbackState.statusEnglish,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) "সাউন্ড শেষ হলে স্বয়ংক্রিয়ভাবে বন্ধ হবে" else "Playback stops automatically when finished",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDDF3EC)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { PrayerAzanManager.stopAzanPlayback(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("banner_stop_azan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "আজান বন্ধ করুন" else "Stop Azan",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Hero Live Prayer Status, Countdown, Hijri & Bangla Dates, Sunrise & Sunset Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, QawmiGold, RoundedCornerShape(18.dp))
                    .testTag("prayer_hero_countdown_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiDarkGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0A4B39), QawmiDarkGreen)
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row: Selected District & Pin Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = null,
                                tint = QawmiGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "জেলা/বিভাগ: $selectedDistrict",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    PrayerAzanManager.setPinnedDistrictSelection(context, selectedDistrict)
                                }
                                .testTag("schedule_pin_btn_$selectedDistrict"),
                            shape = RoundedCornerShape(50),
                            color = if (selectedDistrict == pinnedDistrict) QawmiGold else Color(0xFF165B48)
                        ) {
                            Text(
                                text = if (selectedDistrict == pinnedDistrict) {
                                    if (isBangla) "📌 পিন করা জেলা" else "📌 Pinned District"
                                } else {
                                    if (isBangla) "📌 পিন করুন" else "📌 Pin District"
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedDistrict == pinnedDistrict) QawmiTextDark else Color(0xFFDDF3EC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Dates Box (Hijri + Bangla Ponjika + Gregorian)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF073A2B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1D6E57), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌙 ${if (isBangla) dailySchedule.hijriDateBangla else dailySchedule.hijriDateEnglish}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QawmiGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "🗓️ ${if (isBangla) dailySchedule.banglaCalendarDateBangla else dailySchedule.banglaCalendarDateEnglish}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDDF3EC),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBangla) dailySchedule.gregorianDateBangla else dailySchedule.gregorianDateEnglish,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB5DCD0)
                                )
                                Text(
                                    text = if (isBangla) {
                                        "বর্তমান সময়: ${dailySchedule.currentTimeBangla}"
                                    } else {
                                        "Now: ${dailySchedule.currentTimeEnglish}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Highlighted Current Waqt & Live Countdown to Next Prayer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Current Active Waqt Box
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, QawmiGold.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF125A45)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isBangla) dailySchedule.currentWaqtLabelBangla else dailySchedule.currentWaqtLabelEnglish,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QawmiGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) dailySchedule.currentWaqtRangeBangla else dailySchedule.currentWaqtRangeEnglish,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isBangla) "ওয়াক্তের শুরু ও শেষ সময়" else "Active Waqt Window",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB5DCD0),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Next Prayer & Live Countdown Box
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF1D6E57), RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF073A2B)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isBangla) {
                                        "পরবর্তী নামাজ: ${dailySchedule.nextWaqtNameBangla} (${dailySchedule.nextWaqtStartBangla})"
                                    } else {
                                        "Next: ${dailySchedule.nextWaqtNameEnglish} (${dailySchedule.nextWaqtStartEnglish})"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) dailySchedule.countdownBangla else dailySchedule.countdownEnglish,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QawmiGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.testTag("prayer_live_countdown_text")
                                )
                                Text(
                                    text = if (isBangla) "পরবর্তী ওয়াক্ত শুরু হতে বাকি" else "Time until next Azan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB5DCD0),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Sunrise, Sunset, Sehri End & Iftar Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SunInfoPill(
                            icon = Icons.Default.WbSunny,
                            label = if (isBangla) "সূর্যোদয়" else "Sunrise",
                            value = if (isBangla) dailySchedule.sunriseBangla else dailySchedule.sunriseEnglish,
                            modifier = Modifier.weight(1f)
                        )
                        SunInfoPill(
                            icon = Icons.Default.WbTwilight,
                            label = if (isBangla) "সূর্যাস্ত" else "Sunset",
                            value = if (isBangla) dailySchedule.sunsetBangla else dailySchedule.sunsetEnglish,
                            modifier = Modifier.weight(1f)
                        )
                        SunInfoPill(
                            icon = Icons.Default.AccessTime,
                            label = if (isBangla) "সাহরি শেষ" else "Sehri Ends",
                            value = if (isBangla) dailySchedule.sehriEndBangla else dailySchedule.sehriEndEnglish,
                            modifier = Modifier.weight(1f)
                        )
                        SunInfoPill(
                            icon = Icons.Default.Alarm,
                            label = if (isBangla) "ইফতার" else "Iftar",
                            value = if (isBangla) dailySchedule.iftarStartBangla else dailySchedule.iftarStartEnglish,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Daily 5 Waqt Prayer Cards with Per-Waqt Azan Toggle, Sound, Volume & Pre-Reminder Controls
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) {
                                    "পাঁচ ওয়াক্তের সময়সূচি ও পৃথক আজান নিয়ন্ত্রণ"
                                } else {
                                    "5 Daily Prayers & Per-Waqt Azan Controls"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBangla) {
                                    "প্রতিটি ওয়াক্তের আজান চালু/বন্ধ, সাউন্ড, ভলিউম ও রিমাইন্ডার আলাদাভাবে সেট করুন"
                                } else {
                                    "Configure Azan switch, sound type, volume, and pre-reminder for each prayer"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiTextSecondary
                            )
                        }
                    }

                    dailySchedule.waqts.forEach { waqt ->
                        val cfg = preferences.configFor(waqt.key)
                        val isExpanded = expandedWaqtId == waqt.key.id

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (waqt.isCurrentWaqt) 2.dp else 1.dp,
                                    color = if (waqt.isCurrentWaqt) QawmiGold else QawmiMintBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("prayer_waqt_card_${waqt.key.id}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (waqt.isCurrentWaqt) Color(0xFFE8F5E9) else QawmiMintSurface.copy(alpha = 0.55f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedWaqtId = if (isExpanded) "" else waqt.key.id
                                        },
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
                                            color = if (waqt.isCurrentWaqt) QawmiDeepGreen else Color.White
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (cfg.azanEnabled && preferences.masterAzanEnabled) {
                                                        Icons.AutoMirrored.Filled.VolumeUp
                                                    } else {
                                                        Icons.AutoMirrored.Filled.VolumeOff
                                                    },
                                                    contentDescription = null,
                                                    tint = if (waqt.isCurrentWaqt) QawmiGold else QawmiDeepGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isBangla) waqt.nameBangla else waqt.nameEnglish,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = QawmiTextDark,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (waqt.isCurrentWaqt) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = QawmiDeepGreen
                                                    ) {
                                                        Text(
                                                            text = if (isBangla) "চলমান ওয়াক্ত" else "Active Now",
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = QawmiGold,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                } else if (waqt.isNextWaqt) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = Color(0xFFFFF8E1)
                                                    ) {
                                                        Text(
                                                            text = if (isBangla) "পরবর্তী" else "Next",
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFFB45309),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (isBangla) {
                                                    "শুরু: ${waqt.startFormattedBangla} • শেষ: ${waqt.endFormattedBangla}"
                                                } else {
                                                    "Start: ${waqt.startFormattedEnglish} • End: ${waqt.endFormattedEnglish}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = buildString {
                                                    if (isBangla) {
                                                        append(if (cfg.azanEnabled) cfg.soundType.titleBangla else "আজান বন্ধ")
                                                        if (cfg.preReminderMinutes > 0) {
                                                            append(" • রিমাইন্ডার: ${IslamicDateAndPrayerHelper.toBanglaDigits(cfg.preReminderMinutes.toString())} মি. আগে")
                                                        }
                                                    } else {
                                                        append(if (cfg.azanEnabled) cfg.soundType.titleEnglish else "Azan Off")
                                                        if (cfg.preReminderMinutes > 0) {
                                                            append(" • Reminder: ${cfg.preReminderMinutes}m before")
                                                        }
                                                    }
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QawmiTextSecondary
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = cfg.azanEnabled,
                                            onCheckedChange = { enabled ->
                                                PrayerAzanManager.updateWaqtConfig(
                                                    context = context,
                                                    waqtKey = waqt.key,
                                                    azanEnabled = enabled
                                                )
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = QawmiDeepGreen
                                            ),
                                            modifier = Modifier.testTag("waqt_azan_switch_${waqt.key.id}")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isBangla) "বিস্তারিত সেটিংস" else "Expand settings",
                                            tint = QawmiDeepGreen,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable {
                                                    expandedWaqtId = if (isExpanded) "" else waqt.key.id
                                                }
                                                .testTag("waqt_expand_settings_${waqt.key.id}")
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        HorizontalDivider(color = QawmiMintBorder)

                                        // A. Per-Waqt Sound Type Selection
                                        Text(
                                            text = if (isBangla) {
                                                "${waqt.nameBangla}-এর আজান সাউন্ড নির্বাচন:"
                                            } else {
                                                "${waqt.nameEnglish} Sound Type:"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = QawmiTextDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            AzanSoundType.entries.chunked(2).forEach { pair ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    pair.forEach { soundOpt ->
                                                        val selected = cfg.soundType == soundOpt
                                                        Surface(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = if (selected) QawmiDeepGreen else QawmiMintBorder,
                                                                    shape = RoundedCornerShape(10.dp)
                                                                )
                                                                .clickable {
                                                                    PrayerAzanManager.updateWaqtConfig(
                                                                        context = context,
                                                                        waqtKey = waqt.key,
                                                                        soundType = soundOpt
                                                                    )
                                                                }
                                                                .testTag("waqt_${waqt.key.id}_sound_${soundOpt.id}"),
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = if (selected) QawmiDeepGreen else Color.White
                                                        ) {
                                                            Text(
                                                                text = if (isBangla) soundOpt.titleBangla else soundOpt.titleEnglish,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (selected) Color.White else QawmiTextDark,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // B. Per-Waqt Pre-Azan Reminder (বন্ধ / ৫ / ১০ / ১৫ মিনিট আগে + কাস্টম পরিবর্তন)
                                        Text(
                                            text = if (isBangla) {
                                                "আজানের আগে রিমাইন্ডার নোটিফিকেশন:"
                                            } else {
                                                "Pre-Azan Reminder Notification:"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = QawmiTextDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(0, 5, 10, 15, 20, 30).forEach { mins ->
                                                val isSel = cfg.preReminderMinutes == mins
                                                val chipText = when {
                                                    mins == 0 -> if (isBangla) "বন্ধ" else "Off"
                                                    isBangla -> "${IslamicDateAndPrayerHelper.toBanglaDigits(mins.toString())} মিনিট আগে"
                                                    else -> "${mins}m before"
                                                }
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSel) QawmiDeepGreen else QawmiMintBorder,
                                                            shape = RoundedCornerShape(50)
                                                        )
                                                        .clickable {
                                                            PrayerAzanManager.updateWaqtConfig(
                                                                context = context,
                                                                waqtKey = waqt.key,
                                                                preReminderMinutes = mins
                                                            )
                                                        }
                                                        .testTag("waqt_${waqt.key.id}_prereminder_$mins"),
                                                    shape = RoundedCornerShape(50),
                                                    color = if (isSel) QawmiDeepGreen else Color.White
                                                ) {
                                                    Text(
                                                        text = chipText,
                                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSel) Color.White else QawmiTextDark,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        // C. Per-Waqt Volume Slider & Preview Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isBangla) {
                                                    "সাউন্ড ভলিউম: ${IslamicDateAndPrayerHelper.toBanglaDigits(cfg.volumePercent.toString())}%"
                                                } else {
                                                    "Sound Volume: ${cfg.volumePercent}%"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = QawmiTextDark,
                                                fontWeight = FontWeight.Bold
                                            )

                                            OutlinedButton(
                                                onClick = {
                                                    if (playbackState.isPlaying && playbackState.activeWaqtKey == waqt.key) {
                                                        PrayerAzanManager.stopAzanPlayback(context)
                                                    } else {
                                                        PrayerAzanManager.startSoundPreview(
                                                            context = context,
                                                            soundType = cfg.soundType,
                                                            waqtKey = waqt.key,
                                                            volumePercent = cfg.volumePercent
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("waqt_preview_sound_${waqt.key.id}")
                                            ) {
                                                val isThisPlaying = playbackState.isPlaying && playbackState.activeWaqtKey == waqt.key
                                                Icon(
                                                    imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = QawmiDeepGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isThisPlaying) {
                                                        if (isBangla) "থামান" else "Stop"
                                                    } else {
                                                        if (isBangla) "সাউন্ড পরীক্ষা (Preview)" else "Preview Sound"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = QawmiDeepGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Slider(
                                            value = cfg.volumePercent.toFloat(),
                                            onValueChange = { newVal ->
                                                PrayerAzanManager.updateWaqtConfig(
                                                    context = context,
                                                    waqtKey = waqt.key,
                                                    volumePercent = newVal.toInt()
                                                )
                                            },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = QawmiDeepGreen,
                                                activeTrackColor = QawmiDeepGreen
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Global Automatic Azan Sound Settings & Preview Studio Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(18.dp))
                    .testTag("global_azan_sound_settings_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                color = QawmiMintSurface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AlarmOn,
                                        contentDescription = null,
                                        tint = QawmiDeepGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBangla) {
                                        "স্বয়ংক্রিয় আজান সাউন্ড সিস্টেম"
                                    } else {
                                        "Automatic Azan Sound System"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) {
                                        "অ্যাপ বন্ধ বা স্ক্রিন অফ থাকলেও ওয়াক্ত শুরু হলে নির্বাচিত সাউন্ড বাজবে"
                                    } else {
                                        "Plays selected Azan sound automatically even when screen is off"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = preferences.masterAzanEnabled,
                            onCheckedChange = { enabled ->
                                PrayerAzanManager.setMasterAzanEnabled(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = QawmiDeepGreen
                            ),
                            modifier = Modifier.testTag("master_azan_enable_switch")
                        )
                    }

                    HorizontalDivider(color = QawmiMintBorder)

                    Text(
                        text = if (isBangla) {
                            "সকল ওয়াক্তের জন্য পছন্দের সাউন্ড নির্বাচন করুন ও শুনুন:"
                        } else {
                            "Select Default Sound Type & Preview:"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )

                    AzanSoundType.entries.forEach { soundType ->
                        val isSelected = preferences.defaultSoundType == soundType
                        val isPlayingThis = playbackState.isPlaying && playbackState.activeSoundType == soundType
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) QawmiDeepGreen else QawmiMintBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    PrayerAzanManager.setDefaultSoundTypeForAll(context, soundType)
                                }
                                .testTag("global_sound_option_${soundType.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) QawmiMintSurface else Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBangla) soundType.titleBangla else soundType.titleEnglish,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBangla) soundType.subtitleBangla else soundType.subtitleEnglish,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            if (isPlayingThis) {
                                                PrayerAzanManager.stopAzanPlayback(context)
                                            } else {
                                                PrayerAzanManager.startSoundPreview(
                                                    context = context,
                                                    soundType = soundType,
                                                    waqtKey = PrayerWaqtKey.FAJR,
                                                    volumePercent = 90
                                                )
                                            }
                                        }
                                        .testTag("preview_btn_${soundType.id}"),
                                    shape = RoundedCornerShape(50),
                                    color = if (isPlayingThis) Color(0xFFC62828) else QawmiDeepGreen
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPlayingThis) {
                                                if (isBangla) "থামান" else "Stop"
                                            } else {
                                                if (isBangla) "শুনুন" else "Preview"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Primary Preview & Stop Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                PrayerAzanManager.startSoundPreview(
                                    context = context,
                                    soundType = preferences.defaultSoundType,
                                    waqtKey = PrayerWaqtKey.FAJR,
                                    volumePercent = 90
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("azan_sound_preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = QawmiGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "সাউন্ড পরীক্ষা করুন (Preview)" else "Preview Selected Sound",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (playbackState.isPlaying) {
                            Button(
                                onClick = { PrayerAzanManager.stopAzanPlayback(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("azan_sound_stop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "বন্ধ করুন" else "Stop",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Background Reliability, Permissions, Battery Optimization & DND/Silent Mode Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(18.dp))
                    .testTag("prayer_permissions_reliability_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isBangla) {
                                    "ব্যাকগ্রাউন্ড অ্যালার্ম ও সিস্টেম পারমিশন নির্দেশনা"
                                } else {
                                    "Background Alarm & System Permissions"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBangla) {
                                    "অ্যাপ মিনিমাইজ বা স্ক্রিন বন্ধ থাকলেও নির্ধারিত সময়ে আজান বাজার জন্য প্রয়োজনীয় সেটিংস"
                                } else {
                                    "Recommended permissions so Azan plays reliably when the app is closed"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiTextSecondary
                            )
                        }
                    }

                    // 1. Notification Permission
                    PermissionStatusRow(
                        icon = Icons.Default.Notifications,
                        title = if (isBangla) "নোটিফিকেশন অনুমতি (Notification Permission)" else "Notification Permission",
                        statusText = if (devicePermStatus.hasNotificationPermission) {
                            if (isBangla) "সক্রিয় আছে" else "Granted"
                        } else {
                            if (isBangla) "অনুমতি প্রয়োজন" else "Permission Needed"
                        },
                        isGranted = devicePermStatus.hasNotificationPermission,
                        actionLabel = if (isBangla) "অনুমতি দিন" else "Grant",
                        onActionClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                runCatching {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    )

                    // 2. Exact Alarm Permission
                    PermissionStatusRow(
                        icon = Icons.Default.Alarm,
                        title = if (isBangla) "নির্দিষ্ট সময়ে অ্যালার্ম (Exact Alarm)" else "Exact Alarm Permission",
                        statusText = if (devicePermStatus.canScheduleExactAlarms) {
                            if (isBangla) "সক্রিয় আছে" else "Active"
                        } else {
                            if (isBangla) "সেটিংস থেকে চালু করুন" else "Enable in Settings"
                        },
                        isGranted = devicePermStatus.canScheduleExactAlarms,
                        actionLabel = if (isBangla) "সেটিংস খুলুন" else "Open Settings",
                        onActionClick = {
                            PrayerAzanManager.openExactAlarmPermissionSettings(context)
                        }
                    )

                    // 3. Battery Optimization Guidance
                    PermissionStatusRow(
                        icon = Icons.Default.BatteryChargingFull,
                        title = if (isBangla) "ব্যাটারি অপটিমাইজেশন (Battery Optimization)" else "Battery Optimization",
                        statusText = if (devicePermStatus.isIgnoringBatteryOptimizations) {
                            if (isBangla) "ব্যাকগ্রাউন্ডে বাধাহীন" else "Unrestricted"
                        } else {
                            if (isBangla) "'Unrestricted' রাখলে আজান কখনো মিস হবে না" else "Set Unrestricted to prevent OEM delays"
                        },
                        isGranted = devicePermStatus.isIgnoringBatteryOptimizations,
                        actionLabel = if (isBangla) "পরীক্ষা করুন" else "Configure",
                        onActionClick = {
                            PrayerAzanManager.openBatteryOptimizationSettings(context)
                        }
                    )

                    // 4. Do Not Disturb & Silent Mode Control
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QawmiMintSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoNotDisturbOn,
                                    contentDescription = null,
                                    tint = QawmiDeepGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isBangla) {
                                            "Silent ও Do Not Disturb মোডে আজান সাউন্ড বন্ধ রাখুন"
                                        } else {
                                            "Mute Azan Speaker During Silent / DND Mode"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBangla) {
                                            if (devicePermStatus.isDoNotDisturbActive || devicePermStatus.isPhoneSilentOrVibrate) {
                                                "আপনার ফোন বর্তমানে সাইলেন্ট/DND মোডে রয়েছে"
                                            } else {
                                                "মসজিদ বা মিটিংয়ে ফোন সাইলেন্ট থাকলে শুধু নোটিফিকেশন দেখাবে"
                                            }
                                        } else {
                                            "When enabled, respects phone Silent/DND mode and shows notification only"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiTextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = preferences.respectSilentAndDndMode,
                                onCheckedChange = { respect ->
                                    PrayerAzanManager.setRespectSilentAndDndMode(context, respect)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = QawmiDeepGreen
                                ),
                                modifier = Modifier.testTag("respect_silent_dnd_switch")
                            )
                        }
                    }
                }
            }
        }

        // 8. Divisional & Pinned District Comparison Section
        item {
            Text(
                text = if (isBangla) {
                    "বিভাগ ও প্রিয় জেলাসমূহের তুলনামূলক সময়সূচী"
                } else {
                    "Divisional & Favorite Districts Schedule"
                },
                style = MaterialTheme.typography.titleSmall,
                color = QawmiTextDark,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(divisionsAndFavorites, key = { it.divisionBangla }) { divSchedule ->
            val isPinned = divSchedule.divisionBangla == pinnedDistrict
            val isSelected = divSchedule.divisionBangla == selectedDistrict
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) QawmiGold else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        PrayerAzanManager.setManualDistrictSelection(context, divSchedule.divisionBangla)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiDarkGreen)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = QawmiGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "জেলা/বিভাগ: ${divSchedule.divisionBangla}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    PrayerAzanManager.setPinnedDistrictSelection(context, divSchedule.divisionBangla)
                                },
                            shape = RoundedCornerShape(50),
                            color = if (isPinned) QawmiGold else Color(0xFF165B48)
                        ) {
                            Text(
                                text = if (isPinned) "📌 পিন করা জেলা" else "📌 পিন করুন",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPinned) QawmiTextDark else Color(0xFFDDF3EC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1D6E57), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0A4B39)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${divSchedule.currentWaqtNameBangla} (${divSchedule.currentWaqtRangeBangla})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "সূর্যোদয়: ${divSchedule.sunriseBangla}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDDF3EC),
                                    fontSize = 10.5.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${divSchedule.nextWaqtNameBangla} • শুরু: ${divSchedule.nextWaqtStartBangla}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "সূর্যাস্ত: ${divSchedule.sunsetBangla}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB5DCD0),
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        divSchedule.waqts.forEach { waqt ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (waqt.isCurrentWaqt) 1.5.dp else 1.dp,
                                        color = if (waqt.isCurrentWaqt) QawmiGold else Color(0xFF1D6E57),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                color = if (waqt.isCurrentWaqt) Color(0xFF125A45) else Color(0xFF0A4B39)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = waqt.nameBangla,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (waqt.isCurrentWaqt) QawmiGold else Color(0xFFDDF3EC),
                                        fontWeight = if (waqt.isCurrentWaqt) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "শুরু ${waqt.timeBangla}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = "শেষ ${waqt.endTimeBangla}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFDDF3EC),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = waqt.period,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB5DCD0),
                                        fontSize = 9.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDistrictPicker) {
        PrayerDistrictSelectionDialog(
            selectedDistrict = selectedDistrict,
            pinnedDistrict = pinnedDistrict,
            favoriteDistricts = favoriteDistricts,
            onSelectDistrict = { dist ->
                PrayerAzanManager.setManualDistrictSelection(context, dist)
            },
            onPinDistrict = { dist ->
                PrayerAzanManager.setPinnedDistrictSelection(context, dist)
            },
            onToggleFavoriteDistrict = { dist ->
                IslamicDateAndPrayerHelper.toggleFavoriteDistrict(context, dist)
            },
            onDismiss = { showDistrictPicker = false }
        )
    }

    if (showCalcMethodDialog) {
        AlertDialog(
            onDismissRequest = { showCalcMethodDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (isBangla) "সময় গণনা পদ্ধতি ও আসরের মাজহাব" else "Calculation Method & Asr Madhhab",
                    style = MaterialTheme.typography.titleMedium,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prayer_calc_method_dialog"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isBangla) "আসরের ওয়াক্তের মাজহাব নির্বাচন:" else "Asr Juristic Method (Madhhab):",
                        style = MaterialTheme.typography.labelMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    AsrMadhhab.entries.forEach { madhhab ->
                        val isSel = preferences.asrMadhhab == madhhab
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) QawmiDeepGreen else QawmiMintBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    PrayerAzanManager.setAsrMadhhab(context, madhhab)
                                }
                                .testTag("madhhab_chip_${madhhab.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) QawmiMintSurface else Color.White
                        ) {
                            Text(
                                text = if (isBangla) madhhab.titleBangla else madhhab.titleEnglish,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = QawmiTextDark,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBangla) "ফজর ও এশার কোণ গণনা পদ্ধতি:" else "Fajr & Isha Calculation Convention:",
                        style = MaterialTheme.typography.labelMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    PrayerCalculationMethod.entries.forEach { method ->
                        val isSel = preferences.calculationMethod == method
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) QawmiDeepGreen else QawmiMintBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    PrayerAzanManager.setCalculationMethod(context, method)
                                }
                                .testTag("calc_method_chip_${method.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) QawmiMintSurface else Color.White
                        ) {
                            Text(
                                text = if (isBangla) method.titleBangla else method.titleEnglish,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = QawmiTextDark,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalcMethodDialog = false }) {
                    Text(
                        text = if (isBangla) "সম্পন্ন" else "Done",
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun SunInfoPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, Color(0xFF1D6E57), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0A4B39)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = QawmiGold,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB5DCD0),
                fontSize = 9.5.sp,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    title: String,
    statusText: String,
    isGranted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isGranted) Color(0xFFF1F8F5) else Color(0xFFFFF8E1),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isGranted) QawmiMintBorder else QawmiGold,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) QawmiDeepGreen else Color(0xFFB45309),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGranted) QawmiDeepGreen else Color(0xFFB45309)
                    )
                }
            }
            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onActionClick),
                    shape = RoundedCornerShape(50),
                    color = QawmiDeepGreen
                ) {
                    Text(
                        text = actionLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = QawmiDeepGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
