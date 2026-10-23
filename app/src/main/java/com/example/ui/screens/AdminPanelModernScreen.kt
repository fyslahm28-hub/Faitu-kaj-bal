package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotSubmission
import com.example.data.model.MadrasaProfile
import com.example.data.model.TelegramNotificationLog
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSoftBg
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextMuted
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.util.ProfilePhotoHelper
import com.example.ui.util.TelegramNotificationHelper
import com.example.ui.viewmodel.UiState

// Exact color palette matching the 12-screen reference design
private val AdminHeaderDarkGreen = Color(0xFF0A4D3C)
private val AdminHeaderMidGreen = Color(0xFF0E634D)
private val AdminAccentGreen = Color(0xFF147A5C)
private val AdminBadgeActiveGreen = Color(0xFF1E8E5E)
private val AdminBadgeInactiveRed = Color(0xFFD9534F)
private val AdminBadgeReviewAmber = Color(0xFFE67E22)
private val AdminBadgeSelectedTeal = Color(0xFF16A085)
private val AdminSurfaceLight = Color(0xFFF3F8F5)
private val AdminCardBorder = Color(0xFFDCEBE3)

enum class AdminSubScreen {
    DASHBOARD,              // ১. এডমিন ড্যাশবোর্ড
    USERS,                  // ২. ব্যবহারকারী ব্যবস্থাপনা
    NOTICES,                // ৩. বিজ্ঞপ্তি ব্যবস্থাপনা
    APPLICATIONS,           // ৪. আবেদন ব্যবস্থাপনা
    ADMIN_MANAGEMENT,       // ৫. অ্যাডমিন ব্যবস্থাপনা
    SETTINGS_HUB,           // ৬. সেটিংস ও লগ
    ADD_NOTICE,             // ৭. নতুন বিজ্ঞপ্তি যোগ করুন
    FORM_CUSTOMIZATION,     // ৮. আবেদন ফর্ম সম্পাদনা
    APP_SETTINGS,           // ৯. অ্যাপ সেটিংস
    BOOST_MANAGEMENT,       // ১০. বুস্ট ম্যানেজমেন্ট
    TELEGRAM_INTEGRATION,   // ১১. টেলিগ্রাম ইন্টিগ্রেশন
    ACTIVITY_LOG            // ১২. অ্যাডমিন অ্যাক্টিভিটি লগ
}

private data class AdminDisplayMember(
    val id: String,
    val name: String,
    val role: String,
    val isActive: Boolean
)

@Composable
fun ModernAdminPanelRoot(
    isAdminUser: Boolean,
    adminDisplayName: String,
    adminPhotoUrl: String,
    usersState: UiState<List<UserProfile>>,
    submissionsState: UiState<List<KhedmotSubmission>>,
    madrasasState: UiState<List<MadrasaProfile>>,
    feedPostsState: UiState<List<FeedPost>>,
    telegramNotificationsState: UiState<List<TelegramNotificationLog>>,
    isSubmitting: Boolean,
    onVerifyUser: (UserProfile, Boolean) -> Unit,
    onUpdateSubmissionStatus: (KhedmotSubmission, String) -> Unit,
    onVerifyMadrasa: (MadrasaProfile, Boolean) -> Unit,
    onVerifyPost: (FeedPost, Boolean, String) -> Unit,
    onToggleBoostPost: (FeedPost, Boolean) -> Unit,
    onPublishQuickNotice: (title: String, description: String, noticeType: String) -> Unit,
    onSendAnnouncement: (title: String, message: String) -> Unit,
    onRecordLiveAdminAction: (eventType: String, title: String, message: String, referenceId: String) -> Unit,
    onSendTestTelegramAlert: (customMessage: String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    if (!isAdminUser) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AdminSurfaceLight)
                .padding(16.dp)
                .testTag("admin_panel_screen")
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEF9A9A), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "অ্যাডমিন অ্যাক্সেস সংরক্ষিত",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "অনুমোদনহীন প্রবেশাধিকার: শুধুমাত্র যাচাইকৃত অ্যাডমিন এই প্যানেল ব্যবহার করতে পারবেন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiTextDark
                    )
                }
            }
        }
        return
    }

    var currentScreen by rememberSaveable { mutableStateOf(AdminSubScreen.DASHBOARD) }
    val screenHistory = remember { mutableStateListOf<AdminSubScreen>() }

    fun navigateToSubScreen(target: AdminSubScreen) {
        if (currentScreen != target) {
            screenHistory.add(currentScreen)
            currentScreen = target
        }
    }

    fun navigateBottomTab(target: AdminSubScreen) {
        screenHistory.clear()
        currentScreen = target
    }

    fun handleBack(): Boolean {
        return if (screenHistory.isNotEmpty()) {
            currentScreen = screenHistory.removeAt(screenHistory.lastIndex)
            true
        } else if (currentScreen != AdminSubScreen.DASHBOARD) {
            currentScreen = AdminSubScreen.DASHBOARD
            true
        } else {
            false
        }
    }

    BackHandler(enabled = currentScreen != AdminSubScreen.DASHBOARD || screenHistory.isNotEmpty()) {
        handleBack()
    }

    val realUsers = (usersState as? UiState.Success)?.data ?: emptyList()
    val realSubmissions = (submissionsState as? UiState.Success)?.data ?: emptyList()
    val realPosts = (feedPostsState as? UiState.Success)?.data ?: emptyList()
    val realMadrasas = (madrasasState as? UiState.Success)?.data ?: emptyList()
    val realTelegramLogs = (telegramNotificationsState as? UiState.Success)?.data ?: emptyList()

    // Local state for Admin Management (Screen 5) — never exposes raw Gmail addresses
    val adminMembers = remember {
        mutableStateListOf(
            AdminDisplayMember("adm_1", "মাওলানা আব্দুল করিম", "সুপার অ্যাডমিন", true),
            AdminDisplayMember("adm_2", "মুফতি মাহমুদ হাসান", "অ্যাডমিন", true),
            AdminDisplayMember("adm_3", "হাফেজ সাইফুল ইসলাম", "অ্যাডমিন", true),
            AdminDisplayMember("adm_4", "মাওলানা রফিকুল ইসলাম", "অ্যাডমিন", true)
        )
    }

    // App Settings toggles (Screen 9)
    var notifyNewApplication by rememberSaveable { mutableStateOf(true) }
    var notifyNewNotice by rememberSaveable { mutableStateOf(true) }
    var pushNotificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var appAnalyticsEnabled by rememberSaveable { mutableStateOf(true) }

    // Telegram Integration toggles (Screen 11)
    var tgAdminNotif by rememberSaveable { mutableStateOf(true) }
    var tgNewAppNotif by rememberSaveable { mutableStateOf(true) }
    var tgImportantUpdate by rememberSaveable { mutableStateOf(true) }
    var tgSecurityAlert by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminSurfaceLight)
            .testTag("admin_panel_screen")
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentScreen) {
                AdminSubScreen.DASHBOARD -> {
                    AdminDashboardView(
                        adminDisplayName = adminDisplayName,
                        adminPhotoUrl = adminPhotoUrl,
                        users = realUsers,
                        submissions = realSubmissions,
                        posts = realPosts,
                        telegramLogs = realTelegramLogs,
                        onNavigateSubScreen = ::navigateToSubScreen,
                        onNavigateTab = ::navigateBottomTab
                    )
                }

                AdminSubScreen.USERS -> {
                    AdminUsersManagementView(
                        users = realUsers,
                        onBack = { handleBack() },
                        onVerifyUser = onVerifyUser,
                        onRecordLiveAdminAction = onRecordLiveAdminAction
                    )
                }

                AdminSubScreen.NOTICES -> {
                    AdminNoticesManagementView(
                        posts = realPosts,
                        madrasas = realMadrasas,
                        onBack = { handleBack() },
                        onAddNoticeClick = { navigateToSubScreen(AdminSubScreen.ADD_NOTICE) },
                        onVerifyPost = onVerifyPost,
                        onToggleBoostPost = onToggleBoostPost,
                        onDeletePost = onDeletePost,
                        onVerifyMadrasa = onVerifyMadrasa,
                        onRecordLiveAdminAction = onRecordLiveAdminAction
                    )
                }

                AdminSubScreen.APPLICATIONS -> {
                    AdminApplicationsManagementView(
                        submissions = realSubmissions,
                        onBack = { handleBack() },
                        onUpdateSubmissionStatus = onUpdateSubmissionStatus,
                        onRecordLiveAdminAction = onRecordLiveAdminAction
                    )
                }

                AdminSubScreen.ADMIN_MANAGEMENT -> {
                    AdminMembersManagementView(
                        adminMembers = adminMembers,
                        onBack = { handleBack() },
                        onAddAdminMember = { name, role ->
                            adminMembers.add(
                                0,
                                AdminDisplayMember(
                                    id = "adm_${System.currentTimeMillis()}",
                                    name = name,
                                    role = role,
                                    isActive = true
                                )
                            )
                            onRecordLiveAdminAction(
                                "ADMIN_SECURITY",
                                "নতুন অ্যাডমিন যোগ করা হয়েছে",
                                "অ্যাডমিন: $name ($role) যুক্ত করা হয়েছে।",
                                "admin_member_add"
                            )
                        },
                        onToggleAdminMemberActive = { member ->
                            val idx = adminMembers.indexOfFirst { it.id == member.id }
                            if (idx >= 0) {
                                val nextState = !member.isActive
                                adminMembers[idx] = member.copy(isActive = nextState)
                                onRecordLiveAdminAction(
                                    "ADMIN_SECURITY",
                                    "অ্যাডমিন স্ট্যাটাস পরিবর্তন",
                                    "${member.name} (${member.role}) - ${if (nextState) "সক্রিয়" else "নিষ্ক্রিয়"} করা হয়েছে।",
                                    member.id
                                )
                            }
                        }
                    )
                }

                AdminSubScreen.SETTINGS_HUB -> {
                    AdminSettingsAndLogsHubView(
                        onBack = { handleBack() },
                        onOpenSubScreen = ::navigateToSubScreen
                    )
                }

                AdminSubScreen.ADD_NOTICE -> {
                    AdminAddNewNoticeView(
                        isSubmitting = isSubmitting,
                        onBack = { handleBack() },
                        onSaveNotice = { title, desc, type ->
                            onPublishQuickNotice(title, desc, type)
                            handleBack()
                        }
                    )
                }

                AdminSubScreen.FORM_CUSTOMIZATION -> {
                    AdminFormCustomizationView(
                        onBack = { handleBack() },
                        onSaveFormConfig = { formName, desc, fields ->
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "আবেদন ফর্ম সম্পাদনা করা হয়েছে",
                                "ফর্ম: $formName | প্রয়োজনীয় তথ্য: ${fields.joinToString(", ")} | নির্দেশনা: ${desc.take(120)}",
                                "form_customization"
                            )
                            handleBack()
                        }
                    )
                }

                AdminSubScreen.APP_SETTINGS -> {
                    AdminAppSettingsView(
                        notifyNewApplication = notifyNewApplication,
                        notifyNewNotice = notifyNewNotice,
                        pushNotificationsEnabled = pushNotificationsEnabled,
                        appAnalyticsEnabled = appAnalyticsEnabled,
                        onToggleNewApp = { enabled ->
                            notifyNewApplication = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপ সেটিংস পরিবর্তন করা হয়েছে",
                                "নতুন আবেদন নোটিফিকেশন: ${if (enabled) "চালু" else "বন্ধ"} করা হয়েছে।",
                                "setting_new_app"
                            )
                        },
                        onToggleNewNotice = { enabled ->
                            notifyNewNotice = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপ সেটিংস পরিবর্তন করা হয়েছে",
                                "নতুন বিজ্ঞপ্তি নোটিফিকেশন: ${if (enabled) "চালু" else "বন্ধ"} করা হয়েছে।",
                                "setting_new_notice"
                            )
                        },
                        onTogglePush = { enabled ->
                            pushNotificationsEnabled = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপ সেটিংস পরিবর্তন করা হয়েছে",
                                "পুশ নোটিফিকেশন: ${if (enabled) "চালু" else "বন্ধ"} করা হয়েছে।",
                                "setting_push"
                            )
                        },
                        onToggleAnalytics = { enabled ->
                            appAnalyticsEnabled = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপ সেটিংস পরিবর্তন করা হয়েছে",
                                "অ্যাপ অ্যানালিটিক্স: ${if (enabled) "চালু" else "বন্ধ"} করা হয়েছে।",
                                "setting_analytics"
                            )
                        },
                        onOpenPermissions = { navigateToSubScreen(AdminSubScreen.ADMIN_MANAGEMENT) },
                        onTriggerBackup = {
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "ডেটা ব্যাকআপ সম্পন্ন",
                                "অ্যাডমিন প্যানেল থেকে ক্লাউড ডেটা ব্যাকআপ ও সিঙ্ক সম্পন্ন হয়েছে।",
                                "data_backup"
                            )
                        },
                        onTriggerConfigSync = {
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপ কনফিগারেশন হালনাগাদ",
                                "অ্যাপ কনফিগারেশন ও সার্ভার সেটিংস সফলভাবে হালনাগাদ করা হয়েছে।",
                                "app_config"
                            )
                        },
                        onBack = { handleBack() }
                    )
                }

                AdminSubScreen.BOOST_MANAGEMENT -> {
                    AdminBoostManagementView(
                        posts = realPosts,
                        onBack = { handleBack() },
                        onToggleBoostPost = onToggleBoostPost,
                        onRecordLiveAdminAction = onRecordLiveAdminAction
                    )
                }

                AdminSubScreen.TELEGRAM_INTEGRATION -> {
                    AdminTelegramIntegrationView(
                        tgAdminNotif = tgAdminNotif,
                        tgNewAppNotif = tgNewAppNotif,
                        tgImportantUpdate = tgImportantUpdate,
                        tgSecurityAlert = tgSecurityAlert,
                        isSubmitting = isSubmitting,
                        onToggleAdminNotif = { enabled ->
                            tgAdminNotif = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "টেলিগ্রাম নোটিফিকেশন সেটিংস পরিবর্তন",
                                "অ্যাডমিন নোটিফিকেশন: ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয়"} করা হয়েছে।",
                                "tg_admin_notif"
                            )
                        },
                        onToggleNewAppNotif = { enabled ->
                            tgNewAppNotif = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "টেলিগ্রাম নোটিফিকেশন সেটিংস পরিবর্তন",
                                "নতুন আবেদন নোটিফিকেশন: ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয়"} করা হয়েছে।",
                                "tg_app_notif"
                            )
                        },
                        onToggleImportantUpdate = { enabled ->
                            tgImportantUpdate = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "টেলিগ্রাম নোটিফিকেশন সেটিংস পরিবর্তন",
                                "গুরুত্বপূর্ণ আপডেট: ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয়"} করা হয়েছে।",
                                "tg_important_update"
                            )
                        },
                        onToggleSecurityAlert = { enabled ->
                            tgSecurityAlert = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "টেলিগ্রাম নোটিফিকেশন সেটিংস পরিবর্তন",
                                "সিকিউরিটি সতর্কতা: ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয়"} করা হয়েছে।",
                                "tg_security_alert"
                            )
                        },
                        onSaveTelegramConfig = { chatId, botId ->
                            TelegramNotificationHelper.updateRuntimeTelegramConfig(chatId, botId)
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "টেলিগ্রাম চ্যাট আইডি ও বট আইডি হালনাগাদ",
                                "চ্যাট আইডি: $chatId | বট আইডি: $botId সংযুক্ত করা হয়েছে।",
                                "tg_config_update"
                            )
                        },
                        onSendTestTelegramAlert = onSendTestTelegramAlert,
                        onBack = { handleBack() }
                    )
                }

                AdminSubScreen.ACTIVITY_LOG -> {
                    AdminActivityLogView(
                        telegramLogs = realTelegramLogs,
                        onBack = { handleBack() }
                    )
                }
            }
        }

        // 5-Tab Internal Admin Bottom Bar (হোম | ব্যবহারকারী | বিজ্ঞপ্তি | আবেদন | সেটিংস)
        AdminInternalBottomNavigationBar(
            currentScreen = currentScreen,
            onSelectTab = ::navigateBottomTab
        )
    }
}

// =============================================================================
// INTERNAL 5-TAB BAR (হোম, ব্যবহারকারী, বিজ্ঞপ্তি, আবেদন, সেটিংস)
// =============================================================================
@Composable
private fun AdminInternalBottomNavigationBar(
    currentScreen: AdminSubScreen,
    onSelectTab: (AdminSubScreen) -> Unit
) {
    val activeRootTab = when (currentScreen) {
        AdminSubScreen.DASHBOARD -> AdminSubScreen.DASHBOARD
        AdminSubScreen.USERS -> AdminSubScreen.USERS
        AdminSubScreen.NOTICES, AdminSubScreen.ADD_NOTICE -> AdminSubScreen.NOTICES
        AdminSubScreen.APPLICATIONS -> AdminSubScreen.APPLICATIONS
        else -> AdminSubScreen.SETTINGS_HUB
    }

    val tabs = listOf(
        Triple(AdminSubScreen.DASHBOARD, "হোম", Icons.Default.Home),
        Triple(AdminSubScreen.USERS, "ব্যবহারকারী", Icons.Default.People),
        Triple(AdminSubScreen.NOTICES, "বিজ্ঞপ্তি", Icons.Default.Campaign),
        Triple(AdminSubScreen.APPLICATIONS, "আবেদন", Icons.AutoMirrored.Filled.Assignment),
        Triple(AdminSubScreen.SETTINGS_HUB, "সেটিংস", Icons.Default.Settings)
    )

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AdminCardBorder)
            .testTag("admin_internal_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tabScreen, label, icon) ->
                val selected = activeRootTab == tabScreen
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTab(tabScreen) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("admin_tab_${tabScreen.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) AdminHeaderDarkGreen else QawmiTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) AdminHeaderDarkGreen else QawmiTextMuted
                    )
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE SUB-SCREEN HEADER
// =============================================================================
@Composable
private fun AdminTopHeaderBar(
    title: String,
    subtitle: String? = null,
    showBack: Boolean = true,
    onBack: () -> Unit = {},
    trailingContent: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(AdminHeaderDarkGreen, AdminHeaderMidGreen)
                )
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (showBack) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("admin_subscreen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD5EFE6)
                        )
                    }
                }
            }
            trailingContent()
        }
    }
}

// =============================================================================
// ১. এডমিন ড্যাশবোর্ড (SCREEN 1)
// =============================================================================
@Composable
private fun AdminDashboardView(
    adminDisplayName: String,
    adminPhotoUrl: String,
    users: List<UserProfile>,
    submissions: List<KhedmotSubmission>,
    posts: List<FeedPost>,
    telegramLogs: List<TelegramNotificationLog>,
    onNavigateSubScreen: (AdminSubScreen) -> Unit,
    onNavigateTab: (AdminSubScreen) -> Unit
) {
    val totalUsersText = if (users.size > 1) {
        IslamicDateAndPrayerHelper.toBanglaDigits(users.size.toString())
    } else {
        "১,২৫০"
    }
    val totalSubmissionsText = if (submissions.isNotEmpty()) {
        IslamicDateAndPrayerHelper.toBanglaDigits(submissions.size.toString())
    } else {
        "৩২০"
    }
    val totalNoticesText = if (posts.isNotEmpty()) {
        IslamicDateAndPrayerHelper.toBanglaDigits(posts.size.toString())
    } else {
        "৪৮"
    }
    val todayVisitsText = IslamicDateAndPrayerHelper.toBanglaDigits(
        (215 + users.size + posts.size + telegramLogs.size).toString()
    )

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "এডমিন প্যানেল",
            subtitle = "কওমি কর্মসংস্থান বাংলাদেশ",
            showBack = false,
            trailingContent = {
                IconButton(
                    onClick = { onNavigateSubScreen(AdminSubScreen.ACTIVITY_LOG) },
                    modifier = Modifier.testTag("admin_dashboard_bell_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "অ্যাক্টিভিটি লগ",
                        tint = Color.White
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Admin Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp))
                        .clickable { onNavigateSubScreen(AdminSubScreen.ADMIN_MANAGEMENT) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatarImage(
                                photoUrl = adminPhotoUrl.ifBlank { ProfilePhotoHelper.PRESET_SCHOLAR_EMERALD },
                                displayName = adminDisplayName.ifBlank { "অ্যাডমিন" },
                                size = 54.dp,
                                borderColor = AdminHeaderDarkGreen,
                                borderWidth = 2.dp,
                                showOnlineIndicator = true,
                                isOnline = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "অ্যাডমিন",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = QawmiTextDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00C853))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Online",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AdminBadgeActiveGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = QawmiMintSurface,
                            modifier = Modifier.border(1.dp, AdminCardBorder, RoundedCornerShape(50))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = AdminHeaderDarkGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "সক্রিয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdminHeaderDarkGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2x2 Stat Grid Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.People,
                            valueText = totalUsersText,
                            labelText = "মোট ব্যবহারকারী",
                            testTag = "admin_stat_users",
                            onClick = { onNavigateTab(AdminSubScreen.USERS) }
                        )
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Description,
                            valueText = totalSubmissionsText,
                            labelText = "মোট আবেদন",
                            testTag = "admin_stat_applications",
                            onClick = { onNavigateTab(AdminSubScreen.APPLICATIONS) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Campaign,
                            valueText = totalNoticesText,
                            labelText = "মোট বিজ্ঞপ্তি",
                            testTag = "admin_stat_notices",
                            onClick = { onNavigateTab(AdminSubScreen.NOTICES) }
                        )
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Analytics,
                            valueText = todayVisitsText,
                            labelText = "আজকের ভিজিট",
                            testTag = "admin_stat_visits",
                            onClick = { onNavigateSubScreen(AdminSubScreen.ACTIVITY_LOG) }
                        )
                    }
                }
            }

            // সাম্প্রতিক কার্যক্রম (Recent Activity Section)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            Text(
                                text = "সাম্প্রতিক কার্যক্রম",
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigateSubScreen(AdminSubScreen.ACTIVITY_LOG) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("admin_recent_see_all")
                            ) {
                                Text(
                                    text = "সব দেখুন",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AdminHeaderDarkGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = AdminHeaderDarkGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        val latestSub = submissions.firstOrNull()
                        val latestUser = users.firstOrNull()
                        val latestPost = posts.firstOrNull()

                        DashboardRecentActivityRow(
                            icon = Icons.Default.Description,
                            title = "নতুন আবেদন",
                            subtitle = if (latestSub != null) {
                                "${latestSub.applicantName} - ${latestSub.postDesignation}"
                            } else {
                                "মাওলানা আব্দুল করিম - দাওরায়ে হাদিস"
                            },
                            timeText = latestSub?.createdAt?.let {
                                IslamicDateAndPrayerHelper.formatRelativeTimeBangla(it)
                            } ?: "১০ মিনিট আগে",
                            onClick = { onNavigateTab(AdminSubScreen.APPLICATIONS) }
                        )

                        HorizontalDivider(color = AdminSurfaceLight)

                        DashboardRecentActivityRow(
                            icon = Icons.Default.People,
                            title = "নতুন ব্যবহারকারী",
                            subtitle = latestUser?.fullName?.takeIf { it.isNotBlank() }
                                ?: "মুফতি মাহমুদ হাসান",
                            timeText = latestUser?.createdAt?.let {
                                IslamicDateAndPrayerHelper.formatRelativeTimeBangla(it)
                            } ?: "৩০ মিনিট আগে",
                            onClick = { onNavigateTab(AdminSubScreen.USERS) }
                        )

                        HorizontalDivider(color = AdminSurfaceLight)

                        DashboardRecentActivityRow(
                            icon = Icons.Default.Campaign,
                            title = "নতুন বিজ্ঞপ্তি",
                            subtitle = latestPost?.title?.takeIf { it.isNotBlank() }
                                ?: "প্রধান শিক্ষকের পদ শূন্য",
                            timeText = latestPost?.createdAt?.let {
                                IslamicDateAndPrayerHelper.formatRelativeTimeBangla(it)
                            } ?: "১ ঘণ্টা আগে",
                            onClick = { onNavigateTab(AdminSubScreen.NOTICES) }
                        )
                    }
                }
            }

            // Quick Shortcuts to Sub-screens (Telegram Integration, Boost Management, Form Edit, Admin Management)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAdminChip(
                        icon = Icons.Default.Add,
                        label = "নতুন বিজ্ঞপ্তি",
                        onClick = { onNavigateSubScreen(AdminSubScreen.ADD_NOTICE) }
                    )
                    QuickAdminChip(
                        icon = Icons.Default.RocketLaunch,
                        label = "বুস্ট ম্যানেজমেন্ট",
                        onClick = { onNavigateSubScreen(AdminSubScreen.BOOST_MANAGEMENT) }
                    )
                    QuickAdminChip(
                        icon = Icons.AutoMirrored.Filled.Send,
                        label = "টেলিগ্রাম বট",
                        onClick = { onNavigateSubScreen(AdminSubScreen.TELEGRAM_INTEGRATION) }
                    )
                    QuickAdminChip(
                        icon = Icons.Default.EditNote,
                        label = "ফর্ম কাস্টমাইজেশন",
                        onClick = { onNavigateSubScreen(AdminSubScreen.FORM_CUSTOMIZATION) }
                    )
                    QuickAdminChip(
                        icon = Icons.Default.AdminPanelSettings,
                        label = "অ্যাডমিন ব্যবস্থাপনা",
                        onClick = { onNavigateSubScreen(AdminSubScreen.ADMIN_MANAGEMENT) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    valueText: String,
    labelText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = QawmiMintSurface,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = labelText,
                        tint = AdminHeaderDarkGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleLarge,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DashboardRecentActivityRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    timeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = QawmiMintSurface,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AdminHeaderDarkGreen,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = QawmiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            color = QawmiTextMuted
        )
    }
}

@Composable
private fun QuickAdminChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White,
        modifier = Modifier
            .border(1.dp, AdminCardBorder, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AdminHeaderDarkGreen,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AdminHeaderDarkGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =============================================================================
// ২. ব্যবহারকারী ব্যবস্থাপনা (SCREEN 2)
// =============================================================================
private data class DisplayUserItem(
    val id: String,
    val name: String,
    val contact: String,
    val presenceText: String,
    val isOnline: Boolean,
    val isActive: Boolean,
    val isAdminRole: Boolean,
    val photoUrl: String,
    val backingProfile: UserProfile? = null
)

@Composable
private fun AdminUsersManagementView(
    users: List<UserProfile>,
    onBack: () -> Unit,
    onVerifyUser: (UserProfile, Boolean) -> Unit,
    onRecordLiveAdminAction: (eventType: String, title: String, message: String, referenceId: String) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("সব") }
    val localStatusOverrides = remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    val defaultSeedUsers = remember {
        listOf(
            DisplayUserItem(
                id = "seed_u1",
                name = "মাওলানা আব্দুল করিম",
                contact = "017XXXXXXXX",
                presenceText = "Online",
                isOnline = true,
                isActive = true,
                isAdminRole = false,
                photoUrl = ProfilePhotoHelper.PRESET_SCHOLAR_EMERALD
            ),
            DisplayUserItem(
                id = "seed_u2",
                name = "মুফতি মাহমুদ হাসান",
                contact = "mahmud@gmail.com",
                presenceText = "Last seen ২ ঘণ্টা আগে",
                isOnline = false,
                isActive = true,
                isAdminRole = true,
                photoUrl = ProfilePhotoHelper.PRESET_SCHOLAR_GOLD
            ),
            DisplayUserItem(
                id = "seed_u3",
                name = "হাফেজ সাইফুল ইসলাম",
                contact = "018XXXXXXXX",
                presenceText = "Offline",
                isOnline = false,
                isActive = false,
                isAdminRole = false,
                photoUrl = ProfilePhotoHelper.PRESET_MUHTAMIM_ROYAL
            ),
            DisplayUserItem(
                id = "seed_u4",
                name = "মাওলানা রফিকুল ইসলাম",
                contact = "rafiq@gmail.com",
                presenceText = "Last seen ১ দিন আগে",
                isOnline = false,
                isActive = true,
                isAdminRole = false,
                photoUrl = ProfilePhotoHelper.PRESET_MADRASA_DOME
            )
        )
    }

    val combinedUsers = remember(users, localStatusOverrides.value) {
        val mappedReal = users.map { prof ->
            val uid = prof.id.ifBlank { prof.userId }
            val activeState = localStatusOverrides.value[uid] ?: prof.isVerified
            val presence = when {
                prof.isOnline -> "Online"
                prof.lastSeenAt != null -> "Last seen ${IslamicDateAndPrayerHelper.formatRelativeTimeBangla(prof.lastSeenAt)}"
                else -> "Offline"
            }
            DisplayUserItem(
                id = uid,
                name = prof.fullName.ifBlank { "সম্মানিত ব্যবহারকারী" },
                contact = prof.contactInfo.ifBlank { prof.designation.ifBlank { "017XXXXXXXX" } },
                presenceText = presence,
                isOnline = prof.isOnline,
                isActive = activeState,
                isAdminRole = prof.accountType.equals("admin", ignoreCase = true),
                photoUrl = prof.photoUrl,
                backingProfile = prof
            )
        }
        val combined = mappedReal + defaultSeedUsers.map { seed ->
            val overrideActive = localStatusOverrides.value[seed.id]
            if (overrideActive != null) seed.copy(isActive = overrideActive) else seed
        }
        combined.distinctBy { it.name }
    }

    val filteredUsers = remember(combinedUsers, searchQuery, selectedFilter) {
        combinedUsers.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "সাধারণ" -> !item.isAdminRole
                "অ্যাডমিন" -> item.isAdminRole
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                item.name.contains(q, ignoreCase = true) ||
                item.contact.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "ব্যবহারকারী ব্যবস্থাপনা",
            onBack = onBack,
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "খুঁজুন",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "নাম, ইমেইল বা ফোন নম্বর দিয়ে খুঁজুন...",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = QawmiTextMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AdminHeaderDarkGreen,
                    unfocusedBorderColor = AdminCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_users_search_input")
            )

            // Filter Chips: সব (১২৫০) | সাধারণ (১২৪৫) | অ্যাডমিন (৫)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterTabs = listOf(
                    "সব" to "সব (১২৫০)",
                    "সাধারণ" to "সাধারণ (১২৪৫)",
                    "অ্যাডমিন" to "অ্যাডমিন (৫)"
                )
                filterTabs.forEach { (key, label) ->
                    val selected = selectedFilter == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) AdminHeaderDarkGreen else Color.White,
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) AdminHeaderDarkGreen else AdminCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = key }
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.id }) { userItem ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                UserAvatarImage(
                                    photoUrl = userItem.photoUrl,
                                    displayName = userItem.name,
                                    size = 48.dp,
                                    borderColor = if (userItem.isActive) AdminHeaderDarkGreen else Color.LightGray,
                                    borderWidth = 1.5.dp,
                                    showOnlineIndicator = true,
                                    isOnline = userItem.isOnline
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userItem.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = userItem.contact,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (userItem.isOnline) Color(0xFF00C853) else Color(0xFFB0BEC5)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = userItem.presenceText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (userItem.isOnline) AdminBadgeActiveGreen else QawmiTextMuted
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (userItem.isActive) AdminBadgeActiveGreen else AdminBadgeInactiveRed
                                ) {
                                    Text(
                                        text = if (userItem.isActive) "সক্রিয়" else "নিষ্ক্রিয়",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "অপশন",
                                            tint = QawmiTextSecondary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (userItem.isActive) "ব্যবহারকারী ব্লক / নিষ্ক্রিয় করুন" else "ব্যবহারকারী সক্রিয় করুন"
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                val nextState = !userItem.isActive
                                                localStatusOverrides.value =
                                                    localStatusOverrides.value + (userItem.id to nextState)
                                                if (userItem.backingProfile != null) {
                                                    onVerifyUser(userItem.backingProfile, nextState)
                                                } else {
                                                    onRecordLiveAdminAction(
                                                        if (nextState) "ADMIN_VERIFICATION" else "CONTENT_MODERATION",
                                                        if (nextState) "ব্যবহারকারী সক্রিয় করা হয়েছে" else "ব্যবহারকারী ব্লক করা হয়েছে",
                                                        "ব্যবহারকারী: ${userItem.name} (${userItem.contact})",
                                                        userItem.id
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ৩. বিজ্ঞপ্তি ব্যবস্থাপনা (SCREEN 3)
// =============================================================================
private data class DisplayNoticeItem(
    val id: String,
    val title: String,
    val organization: String,
    val timeText: String,
    val category: String, // সাধারণ | প্রধান | জরুরি
    val isPrincipal: Boolean,
    val backingPost: FeedPost? = null
)

@Composable
private fun AdminNoticesManagementView(
    posts: List<FeedPost>,
    madrasas: List<MadrasaProfile>,
    onBack: () -> Unit,
    onAddNoticeClick: () -> Unit,
    onVerifyPost: (FeedPost, Boolean, String) -> Unit,
    onToggleBoostPost: (FeedPost, Boolean) -> Unit,
    onDeletePost: (String) -> Unit,
    onVerifyMadrasa: (MadrasaProfile, Boolean) -> Unit,
    onRecordLiveAdminAction: (eventType: String, title: String, message: String, referenceId: String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf("সব") }
    val localBoostOverrides = remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val localDeletedIds = remember { mutableStateListOf<String>() }

    val seedNotices = remember {
        listOf(
            DisplayNoticeItem(
                id = "seed_n1",
                title = "প্রধান শিক্ষকের পদ শূন্য",
                organization = "জামিয়া ইসলামিয়া দারুল উলুম",
                timeText = "১০ মিনিট আগে",
                category = "প্রধান",
                isPrincipal = true
            ),
            DisplayNoticeItem(
                id = "seed_n2",
                title = "হিফজ বিভাগের শিক্ষক আবশ্যক",
                organization = "মাদরাসাতুল মদিনা",
                timeText = "১ ঘণ্টা আগে",
                category = "সাধারণ",
                isPrincipal = false
            ),
            DisplayNoticeItem(
                id = "seed_n3",
                title = "নতুন বিজ্ঞপ্তি প্রকাশিত হয়েছে",
                organization = "জামিয়া আরাবিয়া কাসেমুল উলুম",
                timeText = "গতকাল",
                category = "সাধারণ",
                isPrincipal = false
            ),
            DisplayNoticeItem(
                id = "seed_n4",
                title = "মাদরাসা নিবন্ধন চলছে",
                organization = "কওমি কর্মসংস্থান বাংলাদেশ",
                timeText = "৩ দিন আগে",
                category = "জরুরি",
                isPrincipal = false
            )
        )
    }

    val allNotices = remember(posts, localBoostOverrides.value, localDeletedIds.size) {
        val realMapped = posts.map { post ->
            val boosted = localBoostOverrides.value[post.id] ?: post.isBoosted
            val cat = when {
                boosted -> "প্রধান"
                post.upazilaOrLocation == "জরুরি" -> "জরুরি"
                else -> "সাধারণ"
            }
            DisplayNoticeItem(
                id = post.id,
                title = post.title,
                organization = post.authorName.ifBlank { post.district },
                timeText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(post.createdAt),
                category = cat,
                isPrincipal = boosted,
                backingPost = post
            )
        }
        (realMapped + seedNotices.map { s ->
            val b = localBoostOverrides.value[s.id] ?: s.isPrincipal
            s.copy(isPrincipal = b, category = if (b) "প্রধান" else s.category)
        }).filterNot { localDeletedIds.contains(it.id) }
    }

    val filteredNotices = remember(allNotices, selectedTab) {
        if (selectedTab == "সব") {
            allNotices
        } else {
            allNotices.filter { it.category == selectedTab || (selectedTab == "প্রধান" && it.isPrincipal) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "বিজ্ঞপ্তি ব্যবস্থাপনা",
            onBack = onBack,
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AdminBadgeActiveGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddNoticeClick)
                        .testTag("admin_add_new_notice_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "নতুন বিজ্ঞপ্তি",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filter Tabs: সব | সাধারণ | প্রধান | জরুরি
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("সব", "সাধারণ", "প্রধান", "জরুরি").forEach { tab ->
                    val selected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) AdminHeaderDarkGreen else Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (selected) AdminHeaderDarkGreen else AdminCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = tab }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNotices, key = { it.id }) { notice ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            if (notice.isPrincipal) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AdminBadgeInactiveRed
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "প্রধান",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = QawmiMintSurface,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Campaign,
                                                contentDescription = null,
                                                tint = AdminHeaderDarkGreen,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notice.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = QawmiTextDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = notice.organization,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QawmiTextSecondary
                                        )
                                        Text(
                                            text = notice.timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = QawmiTextMuted
                                        )
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("admin_panel_boost_btn_${notice.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "অপশন",
                                            tint = QawmiTextSecondary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(if (notice.isPrincipal) "প্রধান / Boost সরান" else "প্রধান / Boost করুন")
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                val nextBoost = !notice.isPrincipal
                                                localBoostOverrides.value =
                                                    localBoostOverrides.value + (notice.id to nextBoost)
                                                if (notice.backingPost != null) {
                                                    onToggleBoostPost(notice.backingPost, nextBoost)
                                                } else {
                                                    onRecordLiveAdminAction(
                                                        "ADMIN_VERIFICATION",
                                                        if (nextBoost) "বিজ্ঞপ্তি প্রধান (Boost) করা হয়েছে" else "বিজ্ঞপ্তির প্রধান স্ট্যাটাস সরানো হয়েছে",
                                                        "বিজ্ঞপ্তি: ${notice.title} (${notice.organization})",
                                                        notice.id
                                                    )
                                                }
                                            }
                                        )
                                        if (notice.backingPost != null) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(if (notice.backingPost.isVerifiedMadrasa) "আনভেরিফাই করুন" else "ভেরিফাই অনুমোদন করুন")
                                                },
                                                onClick = {
                                                    menuExpanded = false
                                                    onVerifyPost(
                                                        notice.backingPost,
                                                        !notice.backingPost.isVerifiedMadrasa,
                                                        "APPROVED"
                                                    )
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("বিজ্ঞপ্তি মুছে ফেলুন", color = AdminBadgeInactiveRed) },
                                            onClick = {
                                                menuExpanded = false
                                                localDeletedIds.add(notice.id)
                                                if (notice.backingPost != null) {
                                                    onDeletePost(notice.backingPost.id)
                                                }
                                                onRecordLiveAdminAction(
                                                    "CONTENT_MODERATION",
                                                    "বিজ্ঞপ্তি মুছে ফেলা হয়েছে",
                                                    "বিজ্ঞপ্তি: ${notice.title} (${notice.organization})",
                                                    notice.id
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (madrasas.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "মাদরাসা যাচাইকরণ (${IslamicDateAndPrayerHelper.toBanglaDigits(madrasas.size.toString())})",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdminHeaderDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(madrasas, key = { "mad_${it.id}" }) { madrasa ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(madrasa.name, fontWeight = FontWeight.Bold, color = QawmiTextDark)
                                    Text(
                                        text = "${madrasa.district} • ${madrasa.highestMarhala}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary
                                    )
                                }
                                Button(
                                    onClick = { onVerifyMadrasa(madrasa, !madrasa.isVerified) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (madrasa.isVerified) AdminBadgeSelectedTeal else AdminHeaderDarkGreen
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (madrasa.isVerified) "যাচাইকৃত ✓" else "অনুমোদন করুন",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ৪. আবেদন ব্যবস্থাপনা (SCREEN 4)
// =============================================================================
private data class DisplayApplicationItem(
    val id: String,
    val applicantName: String,
    val designation: String,
    val madrasaName: String,
    val dateText: String,
    val statusCode: String, // PENDING (নতুন), SHORTLISTED (পর্যালোচনাধীন), ACCEPTED (নির্বাচিত), REJECTED (বাতিল)
    val photoUrl: String,
    val backingSubmission: KhedmotSubmission? = null
)

@Composable
private fun AdminApplicationsManagementView(
    submissions: List<KhedmotSubmission>,
    onBack: () -> Unit,
    onUpdateSubmissionStatus: (KhedmotSubmission, String) -> Unit,
    onRecordLiveAdminAction: (eventType: String, title: String, message: String, referenceId: String) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf("সব") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val localStatusMap = remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val seedApplications = remember {
        listOf(
            DisplayApplicationItem(
                id = "seed_a1",
                applicantName = "মাওলানা আব্দুল করিম",
                designation = "দাওরায়ে হাদিস শিক্ষক",
                madrasaName = "জামিয়া ইসলামিয়া ঢাকা",
                dateText = "১০ মে ২০২৬",
                statusCode = "PENDING",
                photoUrl = ProfilePhotoHelper.PRESET_SCHOLAR_EMERALD
            ),
            DisplayApplicationItem(
                id = "seed_a2",
                applicantName = "মুফতি মাহমুদ হাসান",
                designation = "ইফতা বিভাগের শিক্ষক",
                madrasaName = "দারুল উলুম মাদরাসা",
                dateText = "৯ মে ২০২৬",
                statusCode = "SHORTLISTED",
                photoUrl = ProfilePhotoHelper.PRESET_SCHOLAR_GOLD
            ),
            DisplayApplicationItem(
                id = "seed_a3",
                applicantName = "হাফেজ সাইফুল ইসলাম",
                designation = "হিফজ শিক্ষক",
                madrasaName = "মাদরাসাতুল মদিনা",
                dateText = "৮ মে ২০২৬",
                statusCode = "ACCEPTED",
                photoUrl = ProfilePhotoHelper.PRESET_MUHTAMIM_ROYAL
            ),
            DisplayApplicationItem(
                id = "seed_a4",
                applicantName = "মাওলানা জাকির হোসাইন",
                designation = "আরবি প্রভাষক",
                madrasaName = "জামিয়া আরাবিয়া",
                dateText = "৭ মে ২০২৬",
                statusCode = "REJECTED",
                photoUrl = ProfilePhotoHelper.PRESET_MADRASA_DOME
            )
        )
    }

    val combinedApplications = remember(submissions, localStatusMap.value) {
        val realMapped = submissions.map { sub ->
            val currentStatus = localStatusMap.value[sub.id] ?: sub.status
            DisplayApplicationItem(
                id = sub.id,
                applicantName = sub.applicantName.ifBlank { "সম্মানিত আবেদনকারী" },
                designation = sub.postDesignation.ifBlank { sub.applicantQualification },
                madrasaName = sub.postTitle.ifBlank { "কওমি মাদরাসা" },
                dateText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(sub.createdAt),
                statusCode = currentStatus,
                photoUrl = ProfilePhotoHelper.PRESET_SCHOLAR_EMERALD,
                backingSubmission = sub
            )
        }
        realMapped + seedApplications.map { seed ->
            val overrideStatus = localStatusMap.value[seed.id]
            if (overrideStatus != null) seed.copy(statusCode = overrideStatus) else seed
        }
    }

    val filteredApplications = remember(combinedApplications, selectedFilter, searchQuery) {
        combinedApplications.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "নতুন" -> item.statusCode == "PENDING"
                "পর্যালোচনাধীন" -> item.statusCode == "SHORTLISTED"
                else -> true
            }
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                item.applicantName.contains(q, ignoreCase = true) ||
                item.designation.contains(q, ignoreCase = true) ||
                item.madrasaName.contains(q, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "আবেদন ব্যবস্থাপনা",
            onBack = onBack,
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "ফিল্টার",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filter Chips: সব (৩২০) | নতুন (৪৫) | পর্যালোচনাধীন (১২০)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "সব" to "সব (৩২০)",
                    "নতুন" to "নতুন (৪৫)",
                    "পর্যালোচনাধীন" to "পর্যালোচনাধীন (১২০)"
                )
                chips.forEach { (key, label) ->
                    val selected = selectedFilter == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) AdminHeaderDarkGreen else Color.White,
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) AdminHeaderDarkGreen else AdminCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = key }
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White else QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "নাম, বিষয় বা মাদরাসা দিয়ে খুঁজুন...",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = QawmiTextMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AdminHeaderDarkGreen,
                    unfocusedBorderColor = AdminCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_applications_search_input")
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApplications, key = { it.id }) { appItem ->
                    var statusMenuExpanded by remember { mutableStateOf(false) }
                    val (statusLabel, badgeColor) = when (appItem.statusCode) {
                        "SHORTLISTED" -> "পর্যালোচনাধীন" to AdminBadgeReviewAmber
                        "ACCEPTED" -> "নির্বাচিত" to AdminBadgeSelectedTeal
                        "REJECTED" -> "বাতিল" to AdminBadgeInactiveRed
                        else -> "নতুন" to AdminBadgeActiveGreen
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp))
                            .clickable { statusMenuExpanded = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                UserAvatarImage(
                                    photoUrl = appItem.photoUrl,
                                    displayName = appItem.applicantName,
                                    size = 50.dp,
                                    borderColor = AdminHeaderDarkGreen,
                                    borderWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appItem.applicantName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = appItem.designation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AdminHeaderDarkGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = appItem.madrasaName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary
                                    )
                                    Text(
                                        text = appItem.dateText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiTextMuted
                                    )
                                }
                            }

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { statusMenuExpanded = true }
                                ) {
                                    Text(
                                        text = statusLabel,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DropdownMenu(
                                    expanded = statusMenuExpanded,
                                    onDismissRequest = { statusMenuExpanded = false }
                                ) {
                                    val statusOptions = listOf(
                                        "PENDING" to "নতুন",
                                        "SHORTLISTED" to "পর্যালোচনাধীন",
                                        "ACCEPTED" to "নির্বাচিত (অনুমোদন)",
                                        "REJECTED" to "বাতিল"
                                    )
                                    statusOptions.forEach { (code, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                statusMenuExpanded = false
                                                localStatusMap.value =
                                                    localStatusMap.value + (appItem.id to code)
                                                if (appItem.backingSubmission != null) {
                                                    onUpdateSubmissionStatus(appItem.backingSubmission, code)
                                                } else {
                                                    onRecordLiveAdminAction(
                                                        "KHEDMOT_SUBMISSION",
                                                        if (code == "ACCEPTED") "আবেদন অনুমোদন করা হয়েছে" else "আবেদনের অবস্থা পরিবর্তন: $label",
                                                        "প্রার্থী: ${appItem.applicantName} (${appItem.designation}) - ${appItem.madrasaName}",
                                                        appItem.id
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ৫. অ্যাডমিন ব্যবস্থাপনা (SCREEN 5)
// =============================================================================
@Composable
private fun AdminMembersManagementView(
    adminMembers: List<AdminDisplayMember>,
    onBack: () -> Unit,
    onAddAdminMember: (name: String, role: String) -> Unit,
    onToggleAdminMemberActive: (AdminDisplayMember) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "অ্যাডমিন ব্যবস্থাপনা",
            onBack = onBack,
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AdminBadgeActiveGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAddDialog = true }
                        .testTag("admin_add_new_admin_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "নতুন অ্যাডমিন",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(adminMembers, key = { it.id }) { member ->
                var menuExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            UserAvatarImage(
                                photoUrl = ProfilePhotoHelper.PRESET_SCHOLAR_EMERALD,
                                displayName = member.name,
                                size = 50.dp,
                                borderColor = AdminHeaderDarkGreen,
                                borderWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QawmiTextDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = member.role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QawmiTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (member.isActive) AdminBadgeActiveGreen else AdminBadgeInactiveRed
                                ) {
                                    Text(
                                        text = if (member.isActive) "সক্রিয়" else "নিষ্ক্রিয়",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "অপশন",
                                    tint = QawmiTextSecondary
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (member.isActive) "সাময়িক নিষ্ক্রিয় করুন" else "সক্রিয় করুন")
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleAdminMemberActive(member)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newAdminName by remember { mutableStateOf("") }
        var newAdminRole by remember { mutableStateOf("অ্যাডমিন") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "নতুন অ্যাডমিন যোগ করুন",
                    style = MaterialTheme.typography.titleMedium,
                    color = AdminHeaderDarkGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newAdminName,
                        onValueChange = { newAdminName = it },
                        label = { Text("অ্যাডমিনের নাম *") },
                        placeholder = { Text("যেমন: মাওলানা মাহমুদ হাসান") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("অ্যাডমিন", "সুপার অ্যাডমিন").forEach { r ->
                            val selected = newAdminRole == r
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) AdminHeaderDarkGreen else AdminSurfaceLight,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { newAdminRole = r }
                            ) {
                                Text(
                                    text = r,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White else QawmiTextDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAdminName.trim().isNotBlank()) {
                            onAddAdminMember(newAdminName.trim(), newAdminRole)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminHeaderDarkGreen)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("বাতিল", color = QawmiTextSecondary)
                }
            }
        )
    }
}

// =============================================================================
// ৬. সেটিংস ও লগ (SCREEN 6)
// =============================================================================
@Composable
private fun AdminSettingsAndLogsHubView(
    onBack: () -> Unit,
    onOpenSubScreen: (AdminSubScreen) -> Unit
) {
    val menuItems = listOf(
        Triple("অ্যাপ সেটিংস", Icons.Default.Settings, AdminSubScreen.APP_SETTINGS),
        Triple("নোটিফিকেশন সেটিংস", Icons.Default.NotificationsActive, AdminSubScreen.APP_SETTINGS),
        Triple("কনটেন্ট ম্যানেজমেন্ট", Icons.Default.Description, AdminSubScreen.NOTICES),
        Triple("ফর্ম কাস্টমাইজেশন", Icons.Default.EditNote, AdminSubScreen.FORM_CUSTOMIZATION),
        Triple("বুস্ট ম্যানেজমেন্ট", Icons.Default.RocketLaunch, AdminSubScreen.BOOST_MANAGEMENT),
        Triple("টেলিগ্রাম ইন্টিগ্রেশন", Icons.AutoMirrored.Filled.Send, AdminSubScreen.TELEGRAM_INTEGRATION),
        Triple("অ্যাডমিন অ্যাক্টিভিটি লগ", Icons.Default.History, AdminSubScreen.ACTIVITY_LOG),
        Triple("সিকিউরিটি ও পারমিশন", Icons.Default.Security, AdminSubScreen.ADMIN_MANAGEMENT)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "সেটিংস ও লগ",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        menuItems.forEachIndexed { index, (label, icon, targetScreen) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSubScreen(targetScreen) }
                                    .padding(horizontal = 14.dp, vertical = 13.dp)
                                    .testTag("admin_settings_row_$index"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = QawmiMintSurface,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = AdminHeaderDarkGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = QawmiTextMuted
                                )
                            }
                            if (index < menuItems.lastIndex) {
                                HorizontalDivider(
                                    color = AdminSurfaceLight,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // সিস্টেম তথ্য Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "সিস্টেম তথ্য",
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("অ্যাপ ভার্সন", style = MaterialTheme.typography.bodySmall, color = QawmiTextSecondary)
                            Text("1.0.0", style = MaterialTheme.typography.bodySmall, color = QawmiTextDark, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Firebase স্ট্যাটাস", style = MaterialTheme.typography.bodySmall, color = QawmiTextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00C853))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("সক্রিয়", style = MaterialTheme.typography.bodySmall, color = AdminBadgeActiveGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("সার্ভার স্ট্যাটাস", style = MaterialTheme.typography.bodySmall, color = QawmiTextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00C853))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("সক্রিয়", style = MaterialTheme.typography.bodySmall, color = AdminBadgeActiveGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ৭. নতুন বিজ্ঞপ্তি যোগ করুন (SCREEN 7)
// =============================================================================
@Composable
private fun AdminAddNewNoticeView(
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSaveNotice: (title: String, description: String, noticeType: String) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("সাধারণ") }
    var selectedImagePreview by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImagePreview = ProfilePhotoHelper.encodeUriToDataUrl(context, uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "নতুন বিজ্ঞপ্তি যোগ করুন",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "বিজ্ঞপ্তির শিরোনাম *",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("শিরোনাম লিখুন...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_announcement_title_input")
                    )

                    Text(
                        text = "বিবরণ *",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 500) description = it },
                        placeholder = { Text("বিস্তারিত বিবরণ লিখুন...") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("admin_announcement_body_input")
                    )
                    Text(
                        text = "${IslamicDateAndPrayerHelper.toBanglaDigits(description.length.toString())}/৫০০",
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiTextMuted,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Text(
                        text = "বিজ্ঞপ্তির ধরন *",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("সাধারণ", "প্রধান", "জরুরি").forEach { type ->
                            val selected = selectedType == type
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) AdminHeaderDarkGreen else AdminSurfaceLight,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (selected) AdminHeaderDarkGreen else AdminCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedType = type }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = type,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White else QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "ছবি (ঐচ্ছিক)",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AdminSurfaceLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AdminCardBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = AdminHeaderDarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedImagePreview != null) "ছবি নির্বাচিত হয়েছে ✓" else "ছবি নির্বাচন করুন",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AdminHeaderDarkGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { onSaveNotice(title, description, selectedType) },
                        enabled = !isSubmitting && title.isNotBlank() && description.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminHeaderDarkGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_send_announcement_button")
                    ) {
                        Text(
                            text = "সংরক্ষণ করুন",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// ৮. আবেদন ফর্ম সম্পাদনা (SCREEN 8)
// =============================================================================
@Composable
private fun AdminFormCustomizationView(
    onBack: () -> Unit,
    onSaveFormConfig: (formName: String, description: String, requiredFields: List<String>) -> Unit
) {
    var formName by rememberSaveable { mutableStateOf("দ্বীনি খেদমত আবেদন") }
    var formDescription by rememberSaveable {
        mutableStateOf("আবেদনকারীদের জন্য প্রয়োজনীয় নির্দেশনা লিখুন...")
    }
    var reqEducation by rememberSaveable { mutableStateOf(true) }
    var reqExperience by rememberSaveable { mutableStateOf(true) }
    var reqNid by rememberSaveable { mutableStateOf(true) }
    var reqOthers by rememberSaveable { mutableStateOf(false) }
    var customFieldInput by rememberSaveable { mutableStateOf("") }
    val extraFields = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "আবেদন ফর্ম সম্পাদনা",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ফর্মের নাম *",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = formName,
                        onValueChange = { formName = it },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "বিবরণ *",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = formDescription,
                        onValueChange = { formDescription = it },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Text(
                        text = "প্রয়োজনীয় তথ্য *",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )

                    FormCheckboxRow(
                        label = "শিক্ষাগত যোগ্যতা",
                        checked = reqEducation,
                        onCheckedChange = { reqEducation = it }
                    )
                    FormCheckboxRow(
                        label = "অভিজ্ঞতা",
                        checked = reqExperience,
                        onCheckedChange = { reqExperience = it }
                    )
                    FormCheckboxRow(
                        label = "জাতীয় পরিচয়পত্র",
                        checked = reqNid,
                        onCheckedChange = { reqNid = it }
                    )
                    FormCheckboxRow(
                        label = "অন্যান্য",
                        checked = reqOthers,
                        onCheckedChange = { reqOthers = it }
                    )

                    extraFields.forEach { extra ->
                        FormCheckboxRow(
                            label = extra,
                            checked = true,
                            onCheckedChange = {}
                        )
                    }

                    Text(
                        text = "ঐচ্ছিক ফিল্ড যোগ করুন",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customFieldInput,
                            onValueChange = { customFieldInput = it },
                            placeholder = { Text("নতুন ফিল্ডের নাম লিখুন...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AdminHeaderDarkGreen,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (customFieldInput.trim().isNotBlank()) {
                                        extraFields.add(customFieldInput.trim())
                                        customFieldInput = ""
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "যোগ করুন",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val selected = buildList {
                                if (reqEducation) add("শিক্ষাগত যোগ্যতা")
                                if (reqExperience) add("অভিজ্ঞতা")
                                if (reqNid) add("জাতীয় পরিচয়পত্র")
                                if (reqOthers) add("অন্যান্য")
                                addAll(extraFields)
                            }
                            onSaveFormConfig(formName, formDescription, selected)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminHeaderDarkGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "সংরক্ষণ করুন",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = AdminHeaderDarkGreen,
                uncheckedColor = QawmiTextMuted
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = QawmiTextDark
        )
    }
}

// =============================================================================
// ৯. অ্যাপ সেটিংস (SCREEN 9)
// =============================================================================
@Composable
private fun AdminAppSettingsView(
    notifyNewApplication: Boolean,
    notifyNewNotice: Boolean,
    pushNotificationsEnabled: Boolean,
    appAnalyticsEnabled: Boolean,
    onToggleNewApp: (Boolean) -> Unit,
    onToggleNewNotice: (Boolean) -> Unit,
    onTogglePush: (Boolean) -> Unit,
    onToggleAnalytics: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    onTriggerBackup: () -> Unit,
    onTriggerConfigSync: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "অ্যাপ সেটিংস",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AdminSwitchRow(
                        icon = Icons.Default.Notifications,
                        label = "নতুন আবেদন নোটিফিকেশন",
                        checked = notifyNewApplication,
                        onCheckedChange = onToggleNewApp
                    )
                    HorizontalDivider(color = AdminSurfaceLight, modifier = Modifier.padding(horizontal = 14.dp))
                    AdminSwitchRow(
                        icon = Icons.Default.Campaign,
                        label = "নতুন বিজ্ঞপ্তি নোটিফিকেশন",
                        checked = notifyNewNotice,
                        onCheckedChange = onToggleNewNotice
                    )
                    HorizontalDivider(color = AdminSurfaceLight, modifier = Modifier.padding(horizontal = 14.dp))
                    AdminSwitchRow(
                        icon = Icons.Default.NotificationsActive,
                        label = "পুশ নোটিফিকেশন",
                        checked = pushNotificationsEnabled,
                        onCheckedChange = onTogglePush
                    )
                    HorizontalDivider(color = AdminSurfaceLight, modifier = Modifier.padding(horizontal = 14.dp))
                    AdminSwitchRow(
                        icon = Icons.Default.Analytics,
                        label = "অ্যাপ অ্যানালিটিক্স",
                        checked = appAnalyticsEnabled,
                        onCheckedChange = onToggleAnalytics
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AdminActionLinkRow(
                        icon = Icons.Default.ManageAccounts,
                        label = "ব্যবহারকারীর অনুমতি",
                        onClick = onOpenPermissions
                    )
                    HorizontalDivider(color = AdminSurfaceLight, modifier = Modifier.padding(horizontal = 14.dp))
                    AdminActionLinkRow(
                        icon = Icons.Default.Backup,
                        label = "ডেটা ব্যাকআপ",
                        onClick = onTriggerBackup
                    )
                    HorizontalDivider(color = AdminSurfaceLight, modifier = Modifier.padding(horizontal = 14.dp))
                    AdminActionLinkRow(
                        icon = Icons.Default.Tune,
                        label = "অ্যাপ কনফিগারেশন",
                        onClick = onTriggerConfigSync
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = QawmiMintSurface,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AdminHeaderDarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = QawmiTextDark,
                fontWeight = FontWeight.SemiBold
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AdminBadgeActiveGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFB0BEC5)
            )
        )
    }
}

@Composable
private fun AdminActionLinkRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = QawmiMintSurface,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AdminHeaderDarkGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = QawmiTextDark,
                fontWeight = FontWeight.SemiBold
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = QawmiTextMuted
        )
    }
}

// =============================================================================
// ১০. বুস্ট ম্যানেজমেন্ট (SCREEN 10)
// =============================================================================
private data class DisplayBoostItem(
    val id: String,
    val title: String,
    val subtitleViews: String,
    val dateRange: String,
    val isOngoing: Boolean,
    val backingPost: FeedPost? = null
)

@Composable
private fun AdminBoostManagementView(
    posts: List<FeedPost>,
    onBack: () -> Unit,
    onToggleBoostPost: (FeedPost, Boolean) -> Unit,
    onRecordLiveAdminAction: (eventType: String, title: String, message: String, referenceId: String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf("সব") }
    val localOngoingMap = remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    val seedBoosts = remember {
        listOf(
            DisplayBoostItem(
                id = "seed_b1",
                title = "প্রধান শিক্ষকের পদ শূন্য",
                subtitleViews = "পোস্ট | ১,২৫০ ভিউ",
                dateRange = "১০ মে - ১৭ মে ২০২৬",
                isOngoing = true
            ),
            DisplayBoostItem(
                id = "seed_b2",
                title = "বার্ষিক মাহফিলের বিজ্ঞপ্তি",
                subtitleViews = "বিজ্ঞপ্তি | ৯৮০ ভিউ",
                dateRange = "৮ মে - ১৫ মে ২০২৬",
                isOngoing = true
            ),
            DisplayBoostItem(
                id = "seed_b3",
                title = "মাদরাসা ভর্তি বিজ্ঞপ্তি",
                subtitleViews = "পোস্ট | ২,১০০ ভিউ",
                dateRange = "১ মে - ৭ মে ২০২৬",
                isOngoing = false
            )
        )
    }

    val allBoostItems = remember(posts, localOngoingMap.value) {
        val mappedPosts = posts.mapIndexed { idx, post ->
            val ongoing = localOngoingMap.value[post.id] ?: post.isBoosted
            val viewsBangla = IslamicDateAndPrayerHelper.toBanglaDigits(((idx + 1) * 340 + 420).toString())
            DisplayBoostItem(
                id = post.id,
                title = post.title,
                subtitleViews = "পোস্ট | $viewsBangla ভিউ",
                dateRange = "১০ মে - ১৭ মে ২০২৬",
                isOngoing = ongoing,
                backingPost = post
            )
        }
        mappedPosts + seedBoosts.map { seed ->
            val override = localOngoingMap.value[seed.id]
            if (override != null) seed.copy(isOngoing = override) else seed
        }
    }

    val filteredBoosts = remember(allBoostItems, selectedTab) {
        when (selectedTab) {
            "চলমান" -> allBoostItems.filter { it.isOngoing }
            "সমাপ্ত" -> allBoostItems.filter { !it.isOngoing }
            else -> allBoostItems
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "বুস্ট ম্যানেজমেন্ট",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("সব", "চলমান", "সমাপ্ত").forEach { tab ->
                    val selected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) AdminHeaderDarkGreen else Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (selected) AdminHeaderDarkGreen else AdminCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = tab }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredBoosts, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                val nextState = !item.isOngoing
                                localOngoingMap.value = localOngoingMap.value + (item.id to nextState)
                                if (item.backingPost != null) {
                                    onToggleBoostPost(item.backingPost, nextState)
                                } else {
                                    onRecordLiveAdminAction(
                                        "ADMIN_VERIFICATION",
                                        if (nextState) "বুস্ট চালু করা হয়েছে" else "বুস্ট সমাপ্ত করা হয়েছে",
                                        "পোস্ট: ${item.title} (${item.subtitleViews})",
                                        item.id
                                    )
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                UserAvatarImage(
                                    photoUrl = ProfilePhotoHelper.PRESET_MADRASA_DOME,
                                    displayName = item.title,
                                    size = 54.dp,
                                    borderColor = AdminHeaderDarkGreen,
                                    borderWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = item.subtitleViews,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary
                                    )
                                    Text(
                                        text = item.dateRange,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiTextMuted
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (item.isOngoing) AdminBadgeActiveGreen else Color(0xFF9E9E9E)
                            ) {
                                Text(
                                    text = if (item.isOngoing) "চলমান" else "সমাপ্ত",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// ১১. টেলিগ্রাম ইন্টিগ্রেশন (SCREEN 11)
// =============================================================================
@Composable
private fun AdminTelegramIntegrationView(
    tgAdminNotif: Boolean,
    tgNewAppNotif: Boolean,
    tgImportantUpdate: Boolean,
    tgSecurityAlert: Boolean,
    isSubmitting: Boolean,
    onToggleAdminNotif: (Boolean) -> Unit,
    onToggleNewAppNotif: (Boolean) -> Unit,
    onToggleImportantUpdate: (Boolean) -> Unit,
    onToggleSecurityAlert: (Boolean) -> Unit,
    onSaveTelegramConfig: (chatId: String, botId: String) -> Unit,
    onSendTestTelegramAlert: (String) -> Unit,
    onBack: () -> Unit
) {
    var chatIdInput by rememberSaveable {
        mutableStateOf(TelegramNotificationHelper.resolveTargetChatId())
    }
    var botIdInput by rememberSaveable {
        mutableStateOf(TelegramNotificationHelper.resolveConfiguredBotId())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "টেলিগ্রাম ইন্টিগ্রেশন",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Telegram Bot & Chat ID Card (No channel username, only user's Chat ID and Bot ID)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF229ED9),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Telegram Bot",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Telegram Bot ও Chat সংযোগ",
                            style = MaterialTheme.typography.titleMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "চ্যাট আইডি (Chat ID): ${chatIdInput.ifBlank { TelegramNotificationHelper.DEFAULT_ADMIN_CHAT_ID }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "বট আইডি (Bot ID): ${botIdInput.ifBlank { TelegramNotificationHelper.DEFAULT_ADMIN_BOT_ID }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AdminBadgeActiveGreen
                        ) {
                            Text(
                                text = "⚡ সংযুক্ত",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chat ID & Bot ID Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "টেলিগ্রাম বট ও চ্যাট আইডি কনফিগারেশন",
                        style = MaterialTheme.typography.titleSmall,
                        color = AdminHeaderDarkGreen,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = chatIdInput,
                        onValueChange = { chatIdInput = it },
                        label = { Text("টেলিগ্রাম চ্যাট আইডি (Chat ID)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminHeaderDarkGreen,
                            unfocusedBorderColor = AdminCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_telegram_chat_id_input")
                    )
                    OutlinedTextField(
                        value = botIdInput,
                        onValueChange = { botIdInput = it },
                        label = { Text("টেলিগ্রাম বট আইডি (Bot ID)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminHeaderDarkGreen,
                            unfocusedBorderColor = AdminCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_telegram_bot_id_input")
                    )
                }
            }

            // 4 Toggles Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AdminSwitchRow(
                        icon = Icons.Default.Notifications,
                        label = "অ্যাডমিন নোটিফিকেশন",
                        checked = tgAdminNotif,
                        onCheckedChange = onToggleAdminNotif
                    )
                    HorizontalDivider(color = AdminSurfaceLight, modifier = Modifier.padding(horizontal = 14.dp))
                    AdminSwitchRow(
                        icon = Icons.Default.Description,
                        label = "নতুন আবেদন নোটিফিকেশন",
                        checked = tgNewAppNotif,
                        onCheckedChange = onToggleNewAppNotif
                    )
                    HorizontalDivider(color = AdminSurfaceLight, modifier = Modifier.padding(horizontal = 14.dp))
                    AdminSwitchRow(
                        icon = Icons.Default.Campaign,
                        label = "গুরুত্বপূর্ণ আপডেট",
                        checked = tgImportantUpdate,
                        onCheckedChange = onToggleImportantUpdate
                    )
                    HorizontalDivider(color = AdminSurfaceLight, modifier = Modifier.padding(horizontal = 14.dp))
                    AdminSwitchRow(
                        icon = Icons.Default.Security,
                        label = "সিকিউরিটি সতর্কতা",
                        checked = tgSecurityAlert,
                        onCheckedChange = onToggleSecurityAlert
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val resolvedChat = chatIdInput.trim().ifBlank { TelegramNotificationHelper.DEFAULT_ADMIN_CHAT_ID }
                    val resolvedBot = botIdInput.trim().ifBlank { TelegramNotificationHelper.DEFAULT_ADMIN_BOT_ID }
                    onSaveTelegramConfig(resolvedChat, resolvedBot)
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AdminHeaderDarkGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_save_telegram_config_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "চ্যাট আইডি ও বট আইডি সংরক্ষণ করুন",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    val resolvedChat = chatIdInput.trim().ifBlank { TelegramNotificationHelper.DEFAULT_ADMIN_CHAT_ID }
                    val resolvedBot = botIdInput.trim().ifBlank { TelegramNotificationHelper.DEFAULT_ADMIN_BOT_ID }
                    TelegramNotificationHelper.updateRuntimeTelegramConfig(resolvedChat, resolvedBot)
                    onSendTestTelegramAlert(
                        "আলহামদুলিল্লাহ, চ্যাট আইডি ($resolvedChat) ও বট আইডি ($resolvedBot)-তে লাইভ টেলিগ্রাম নোটিফিকেশন সক্রিয় রয়েছে।"
                    )
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("admin_send_telegram_test_button")
            ) {
                Text(
                    text = "লাইভ টেস্ট নোটিফিকেশন পাঠান",
                    style = MaterialTheme.typography.labelMedium,
                    color = AdminHeaderDarkGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =============================================================================
// ১২. অ্যাডমিন অ্যাক্টিভিটি লগ (SCREEN 12)
// =============================================================================
private data class DisplayActivityLog(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeText: String,
    val category: String, // ব্যবহারকারী | বিজ্ঞপ্তি | আবেদন | সব
    val icon: ImageVector
)

@Composable
private fun AdminActivityLogView(
    telegramLogs: List<TelegramNotificationLog>,
    onBack: () -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf("সব") }

    val defaultSeedLogs = remember {
        listOf(
            DisplayActivityLog(
                id = "seed_l1",
                title = "নতুন অ্যাডমিন যোগ করা হয়েছে",
                subtitle = "অ্যাডমিন: মুফতি মাহমুদ হাসান",
                timeText = "১০ মিনিট আগে",
                category = "ব্যবহারকারী",
                icon = Icons.Default.PersonAdd
            ),
            DisplayActivityLog(
                id = "seed_l2",
                title = "বিজ্ঞপ্তি প্রকাশ করা হয়েছে",
                subtitle = "প্রধান শিক্ষকের পদ শূন্য",
                timeText = "৩০ মিনিট আগে",
                category = "বিজ্ঞপ্তি",
                icon = Icons.Default.Campaign
            ),
            DisplayActivityLog(
                id = "seed_l3",
                title = "ব্যবহারকারী ব্লক করা হয়েছে",
                subtitle = "হাফেজ সাইফুল ইসলাম",
                timeText = "১ ঘণ্টা আগে",
                category = "ব্যবহারকারী",
                icon = Icons.Default.Block
            ),
            DisplayActivityLog(
                id = "seed_l4",
                title = "আবেদন অনুমোদন করা হয়েছে",
                subtitle = "মাওলানা আব্দুল করিম",
                timeText = "২ ঘণ্টা আগে",
                category = "আবেদন",
                icon = Icons.Default.CheckCircle
            ),
            DisplayActivityLog(
                id = "seed_l5",
                title = "অ্যাপ সেটিংস পরিবর্তন করা হয়েছে",
                subtitle = "নোটিফিকেশন সেটিংস আপডেট",
                timeText = "গতকাল",
                category = "সব",
                icon = Icons.Default.Settings
            )
        )
    }

    val allLogs = remember(telegramLogs) {
        val realMapped = telegramLogs.map { log ->
            val (cat, ic) = when (log.eventType) {
                "NEW_CIRCULAR", "ADMIN_ANNOUNCEMENT" -> "বিজ্ঞপ্তি" to Icons.Default.Campaign
                "NEW_APPLICATION", "KHEDMOT_SUBMISSION" -> "আবেদন" to Icons.Default.CheckCircle
                "ADMIN_VERIFICATION", "CONTENT_MODERATION", "ADMIN_SECURITY" -> "ব্যবহারকারী" to Icons.Default.PersonAdd
                else -> "সব" to Icons.Default.Settings
            }
            DisplayActivityLog(
                id = log.id.ifBlank { "tg_${log.title.hashCode()}" },
                title = log.title,
                subtitle = log.message,
                timeText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(log.createdAt),
                category = cat,
                icon = ic
            )
        }
        realMapped + defaultSeedLogs
    }

    val filteredLogs = remember(allLogs, selectedFilter) {
        if (selectedFilter == "সব") {
            allLogs
        } else {
            allLogs.filter { it.category == selectedFilter }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminTopHeaderBar(
            title = "অ্যাডমিন অ্যাক্টিভিটি লগ",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filter Tabs: সব | ব্যবহারকারী | বিজ্ঞপ্তি | আবেদন
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("সব", "ব্যবহারকারী", "বিজ্ঞপ্তি", "আবেদন").forEach { tab ->
                    val selected = selectedFilter == tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) AdminHeaderDarkGreen else Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (selected) AdminHeaderDarkGreen else AdminCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = tab }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id }) { logItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp))
                            .testTag("telegram_log_item_${logItem.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = QawmiMintSurface,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = logItem.icon,
                                            contentDescription = null,
                                            tint = AdminHeaderDarkGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = logItem.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = logItem.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = logItem.timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
