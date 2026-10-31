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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import com.example.data.model.AdminRegistryEntry
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
    adminsState: UiState<List<AdminRegistryEntry>> = UiState.Success(emptyList()),
    latestAppRelease: com.example.data.model.AppReleaseInfo? = null,
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
    onPublishAppRelease: (
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        apkSha256: String,
        apkFileSizeBytes: Long,
        releaseNotes: String,
        isMandatory: Boolean,
        releaseStatus: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSaveAdminMember: (memberId: String, name: String, role: String, isActive: Boolean) -> Unit = { _, _, _, _ -> },
    onInspectUserProfile: (String, String) -> Unit = { _, _ -> },
    onRetryServerLoad: () -> Unit = {},
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
    val realAdminEntries = (adminsState as? UiState.Success)?.data ?: emptyList()

    val serverErrorMessage = remember(
        usersState,
        submissionsState,
        madrasasState,
        feedPostsState,
        telegramNotificationsState,
        adminsState
    ) {
        listOfNotNull(
            (usersState as? UiState.Error)?.message,
            (submissionsState as? UiState.Error)?.message,
            (madrasasState as? UiState.Error)?.message,
            (feedPostsState as? UiState.Error)?.message,
            (telegramNotificationsState as? UiState.Error)?.message,
            (adminsState as? UiState.Error)?.message
        ).firstOrNull()
    }

    // Real server-backed Admin list (never exposes raw Gmail addresses)
    val localAddedAdmins = remember { mutableStateListOf<AdminDisplayMember>() }
    val localAdminStatusOverrides = remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val adminMembers = remember(realAdminEntries, realUsers, localAddedAdmins.size, localAdminStatusOverrides.value) {
        val fromRegistry = realAdminEntries.mapIndexed { idx, entry ->
            val id = entry.adminId.ifBlank { entry.id.ifBlank { "reg_$idx" } }
            val matchedUser = realUsers.find { it.id == entry.adminId || it.userId == entry.adminId }
            val safeName = entry.name.takeIf { it.isNotBlank() && !it.contains("@") }
                ?: matchedUser?.fullName?.takeIf { it.isNotBlank() && !it.contains("@") }
                ?: "অনুমোদিত অ্যাডমিন #${IslamicDateAndPrayerHelper.toBanglaDigits((idx + 1).toString())}"
            val roleLabel = if (entry.role.contains("SUPER", ignoreCase = true) || entry.role.contains("সুপার") || entry.role.contains("নাজিমে", ignoreCase = true)) {
                "সুপার অ্যাডমিন"
            } else {
                "অ্যাডমিন"
            }
            val active = localAdminStatusOverrides.value[id] ?: (entry.isActive && entry.verifiedByAuthToken)
            AdminDisplayMember(
                id = id,
                name = safeName,
                role = roleLabel,
                isActive = active
            )
        }
        val fromUsers = realUsers
            .filter { it.accountType.equals("admin", ignoreCase = true) }
            .map { u ->
                val id = u.id.ifBlank { u.userId }
                val active = localAdminStatusOverrides.value[id] ?: u.isVerified
                AdminDisplayMember(
                    id = id,
                    name = u.fullName.takeIf { it.isNotBlank() && !it.contains("@") } ?: "অ্যাডমিন",
                    role = "অ্যাডমিন",
                    isActive = active
                )
            }
        val remoteIds = (fromRegistry.map { it.id } + fromUsers.map { it.id }).toSet()
        val addedMapped = localAddedAdmins
            .filter { it.id !in remoteIds }
            .map { item ->
                val active = localAdminStatusOverrides.value[item.id] ?: item.isActive
                item.copy(isActive = active)
            }
        (fromRegistry + addedMapped + fromUsers).distinctBy { it.id }
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
        if (serverErrorMessage != null) {
            AdminServerErrorBanner(
                errorMessage = serverErrorMessage,
                onRetry = onRetryServerLoad
            )
        }

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
                        posts = realPosts,
                        users = realUsers,
                        onBack = { handleBack() },
                        onUpdateSubmissionStatus = onUpdateSubmissionStatus,
                        onVerifyPost = onVerifyPost,
                        onInspectUserProfile = onInspectUserProfile,
                        onRecordLiveAdminAction = onRecordLiveAdminAction
                    )
                }

                AdminSubScreen.ADMIN_MANAGEMENT -> {
                    AdminMembersManagementView(
                        adminMembers = adminMembers,
                        onBack = { handleBack() },
                        onAddAdminMember = { name, role ->
                            val newId = "adm_${System.currentTimeMillis()}"
                            localAddedAdmins.add(
                                0,
                                AdminDisplayMember(
                                    id = newId,
                                    name = name,
                                    role = role,
                                    isActive = true
                                )
                            )
                            onSaveAdminMember(newId, name, role, true)
                        },
                        onToggleAdminMemberActive = { member ->
                            val nextState = !member.isActive
                            localAdminStatusOverrides.value =
                                localAdminStatusOverrides.value + (member.id to nextState)
                            onSaveAdminMember(member.id, member.name, member.role, nextState)
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
                        onRecordLiveAdminAction = onRecordLiveAdminAction,
                        onSaveFormConfig = { formName, desc, fields ->
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "আবেদন ফর্ম ও নিয়ম-নীতি সম্পাদনা করা হয়েছে",
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
                        latestAppRelease = latestAppRelease,
                        isSubmitting = isSubmitting,
                        onToggleNewApp = { enabled ->
                            notifyNewApplication = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপের নিয়ম-নীতি ও সেটিংস পরিবর্তন",
                                "নতুন আবেদন নোটিফিকেশন: ${if (enabled) "চালু" else "বন্ধ"} করা হয়েছে।",
                                "setting_new_app"
                            )
                        },
                        onToggleNewNotice = { enabled ->
                            notifyNewNotice = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপের নিয়ম-নীতি ও সেটিংস পরিবর্তন",
                                "নতুন বিজ্ঞপ্তি নোটিফিকেশন: ${if (enabled) "চালু" else "বন্ধ"} করা হয়েছে।",
                                "setting_new_notice"
                            )
                        },
                        onTogglePush = { enabled ->
                            pushNotificationsEnabled = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপের নিয়ম-নীতি ও সেটিংস পরিবর্তন",
                                "পুশ নোটিফিকেশন: ${if (enabled) "চালু" else "বন্ধ"} করা হয়েছে।",
                                "setting_push"
                            )
                        },
                        onToggleAnalytics = { enabled ->
                            appAnalyticsEnabled = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপের নিয়ম-নীতি ও সেটিংস পরিবর্তন",
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
                        onPublishAppRelease = { verCode, verName, apkUrl, sha256, sizeBytes, notes, mandatory, status ->
                            onPublishAppRelease(
                                verCode,
                                verName,
                                apkUrl,
                                sha256,
                                sizeBytes,
                                notes,
                                mandatory,
                                status
                            )
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "অ্যাপ আপডেট সংস্করণ কনফিগারেশন ($verName / কোড $verCode)",
                                "অবস্থা: $status | বাধ্যতামূলক: ${if (mandatory) "হ্যাঁ" else "না"} | লিংক: $apkUrl",
                                "app_release_$verCode"
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
                        telegramLogs = realTelegramLogs,
                        onToggleAdminNotif = { enabled ->
                            tgAdminNotif = enabled
                            onRecordLiveAdminAction(
                                if (enabled) "ADMIN_SETTINGS_CHANGE" else "ADMIN_SECURITY",
                                "টেলিগ্রাম নোটিফিকেশন নীতি পরিবর্তন",
                                "অ্যাডমিন নোটিফিকেশন অপশনটি ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয় করার চেষ্টা/পরিবর্তন"} করা হয়েছে।",
                                "tg_admin_notif"
                            )
                        },
                        onToggleNewAppNotif = { enabled ->
                            tgNewAppNotif = enabled
                            onRecordLiveAdminAction(
                                if (enabled) "ADMIN_SETTINGS_CHANGE" else "ADMIN_SECURITY",
                                "টেলিগ্রাম নোটিফিকেশন নীতি পরিবর্তন",
                                "নতুন আবেদন নোটিফিকেশন অপশনটি ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয় করার চেষ্টা/পরিবর্তন"} করা হয়েছে।",
                                "tg_app_notif"
                            )
                        },
                        onToggleImportantUpdate = { enabled ->
                            tgImportantUpdate = enabled
                            onRecordLiveAdminAction(
                                if (enabled) "ADMIN_SETTINGS_CHANGE" else "ADMIN_SECURITY",
                                "টেলিগ্রাম নোটিফিকেশন নীতি পরিবর্তন",
                                "গুরুত্বপূর্ণ আপডেট অপশনটি ${if (enabled) "সক্রিয়" else "নিষ্ক্রিয় করার চেষ্টা/পরিবর্তন"} করা হয়েছে।",
                                "tg_important_update"
                            )
                        },
                        onToggleSecurityAlert = { enabled ->
                            tgSecurityAlert = enabled
                            onRecordLiveAdminAction(
                                "ADMIN_SECURITY",
                                "সিকিউরিটি সতর্কতা অপশন পরিবর্তন",
                                "অ্যাডমিন প্যানেলে সিকিউরিটি সতর্কতা সুইচটি ${if (enabled) "সক্রিয়" else "বন্ধ করার চেষ্টা"} করা হয়েছে (অটো-বট মনিটরিং সর্বদা সচল)।",
                                "tg_security_alert"
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
    val totalUsersText = IslamicDateAndPrayerHelper.toBanglaDigits(users.size.toString())
    val totalSubmissionsText = IslamicDateAndPrayerHelper.toBanglaDigits(submissions.size.toString())
    val totalNoticesText = IslamicDateAndPrayerHelper.toBanglaDigits(posts.size.toString())
    val totalActivityAndSupportText = IslamicDateAndPrayerHelper.toBanglaDigits(telegramLogs.size.toString())

    val supportLogs = remember(telegramLogs) {
        telegramLogs.filter {
            it.referenceId.startsWith("SUPPORT_TICKET") ||
                it.title.startsWith("[অভিযোগ") ||
                it.title.startsWith("[সমস্যা") ||
                it.title.startsWith("[পরামর্শ") ||
                it.title.startsWith("[সহায়তা")
        }
    }

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
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_dashboard_lazy_column"),
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

            // 2x2 Stat Grid Cards (100% Real Server Counts)
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
                            valueText = totalActivityAndSupportText,
                            labelText = "লগ ও অভিযোগ",
                            testTag = "admin_stat_visits",
                            onClick = { onNavigateSubScreen(AdminSubScreen.ACTIVITY_LOG) }
                        )
                    }
                }
            }

            // সাম্প্রতিক কার্যক্রম (Recent Activity Section — 100% Real Server Data)
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

                        val latestSupport = supportLogs.firstOrNull()
                        val latestSub = submissions.firstOrNull()
                        val latestUser = users.firstOrNull()
                        val latestPost = posts.firstOrNull()

                        if (latestSupport == null && latestSub == null && latestUser == null && latestPost == null) {
                            Text(
                                text = "কোনো তথ্য নেই",
                                style = MaterialTheme.typography.bodyMedium,
                                color = QawmiTextMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .testTag("admin_dashboard_empty_activity")
                            )
                        } else {
                            if (latestSupport != null) {
                                DashboardRecentActivityRow(
                                    icon = Icons.Default.SupportAgent,
                                    title = latestSupport.title.ifBlank { "সহায়তা ও অভিযোগ বার্তা" },
                                    subtitle = latestSupport.message,
                                    timeText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(latestSupport.createdAt),
                                    onClick = { onNavigateSubScreen(AdminSubScreen.ACTIVITY_LOG) }
                                )
                                if (latestSub != null || latestUser != null || latestPost != null) {
                                    HorizontalDivider(color = AdminSurfaceLight)
                                }
                            }

                            if (latestSub != null) {
                                DashboardRecentActivityRow(
                                    icon = Icons.Default.Description,
                                    title = "নতুন আবেদন",
                                    subtitle = "${latestSub.applicantName} - ${latestSub.postDesignation}",
                                    timeText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(latestSub.createdAt),
                                    onClick = { onNavigateTab(AdminSubScreen.APPLICATIONS) }
                                )
                                if (latestUser != null || latestPost != null) {
                                    HorizontalDivider(color = AdminSurfaceLight)
                                }
                            }

                            if (latestUser != null) {
                                DashboardRecentActivityRow(
                                    icon = Icons.Default.People,
                                    title = "নতুন ব্যবহারকারী",
                                    subtitle = latestUser.fullName.ifBlank { "সম্মানিত ব্যবহারকারী" },
                                    timeText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(latestUser.createdAt),
                                    onClick = { onNavigateTab(AdminSubScreen.USERS) }
                                )
                                if (latestPost != null) {
                                    HorizontalDivider(color = AdminSurfaceLight)
                                }
                            }

                            if (latestPost != null) {
                                DashboardRecentActivityRow(
                                    icon = Icons.Default.Campaign,
                                    title = "নতুন বিজ্ঞপ্তি",
                                    subtitle = latestPost.title,
                                    timeText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(latestPost.createdAt),
                                    onClick = { onNavigateTab(AdminSubScreen.NOTICES) }
                                )
                            }
                        }
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
                        icon = Icons.Default.SupportAgent,
                        label = "অভিযোগ ও সহায়তা",
                        onClick = { onNavigateSubScreen(AdminSubScreen.ACTIVITY_LOG) }
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
private fun AdminServerErrorBanner(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .border(1.dp, Color(0xFFEF9A9A), RoundedCornerShape(12.dp))
            .testTag("admin_server_error_banner"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFEBEE)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRetry)
                    .testTag("admin_retry_server_button"),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFC62828)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "পুনরায় চেষ্টা করুন",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminEmptyDataCard(
    message: String = "কোনো তথ্য নেই"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AdminCardBorder, RoundedCornerShape(14.dp))
            .testTag("admin_empty_state_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = QawmiTextMuted,
                fontWeight = FontWeight.SemiBold
            )
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

    val combinedUsers = remember(users, localStatusOverrides.value) {
        val nowMillis = System.currentTimeMillis()
        users.map { prof ->
            val uid = prof.id.ifBlank { prof.userId }
            val activeState = localStatusOverrides.value[uid] ?: prof.isVerified
            val currentlyOnline = prof.isCurrentlyOnline(nowMillis)
            val effectiveLastSeen = prof.effectiveLastSeenTimestamp()
            val presence = when {
                currentlyOnline -> "Online"
                effectiveLastSeen != null -> "Last seen ${IslamicDateAndPrayerHelper.formatRelativeTimeBangla(effectiveLastSeen)}"
                else -> "Offline"
            }
            DisplayUserItem(
                id = uid,
                name = prof.fullName.ifBlank { "সম্মানিত ব্যবহারকারী" },
                contact = prof.contactInfo.ifBlank { prof.designation.ifBlank { "যোগাযোগের তথ্য নেই" } },
                presenceText = presence,
                isOnline = currentlyOnline,
                isActive = activeState,
                isAdminRole = prof.accountType.equals("admin", ignoreCase = true),
                photoUrl = prof.photoUrl,
                backingProfile = prof
            )
        }
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

            // Filter Chips: Real Server Counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalUsersBangla = IslamicDateAndPrayerHelper.toBanglaDigits(combinedUsers.size.toString())
                val generalUsersBangla = IslamicDateAndPrayerHelper.toBanglaDigits(
                    combinedUsers.count { !it.isAdminRole }.toString()
                )
                val adminUsersBangla = IslamicDateAndPrayerHelper.toBanglaDigits(
                    combinedUsers.count { it.isAdminRole }.toString()
                )
                val filterTabs = listOf(
                    "সব" to "সব ($totalUsersBangla)",
                    "সাধারণ" to "সাধারণ ($generalUsersBangla)",
                    "অ্যাডমিন" to "অ্যাডমিন ($adminUsersBangla)"
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
                if (filteredUsers.isEmpty()) {
                    item {
                        AdminEmptyDataCard(message = "কোনো তথ্য নেই")
                    }
                }
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

    val allNotices = remember(posts, localBoostOverrides.value, localDeletedIds.size) {
        posts.map { post ->
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
        }.filterNot { localDeletedIds.contains(it.id) }
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
                if (filteredNotices.isEmpty()) {
                    item {
                        AdminEmptyDataCard(message = "কোনো তথ্য নেই")
                    }
                }
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
    val applicantId: String = "",
    val backingSubmission: KhedmotSubmission? = null,
    val backingPost: FeedPost? = null
)

@Composable
private fun AdminApplicationsManagementView(
    submissions: List<KhedmotSubmission>,
    posts: List<FeedPost> = emptyList(),
    users: List<UserProfile> = emptyList(),
    onBack: () -> Unit,
    onUpdateSubmissionStatus: (KhedmotSubmission, String) -> Unit,
    onVerifyPost: (FeedPost, Boolean, String) -> Unit = { _, _, _ -> },
    onInspectUserProfile: (String, String) -> Unit = { _, _ -> },
    onRecordLiveAdminAction: (eventType: String, title: String, message: String, referenceId: String) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf("সব") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val localStatusMap = remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val userPhotosById = remember(users) {
        users.associate { u ->
            (u.id.ifBlank { u.userId }) to u.photoUrl
        }
    }

    val combinedApplications = remember(submissions, posts, userPhotosById, localStatusMap.value) {
        val submissionItems = submissions.map { sub ->
            val currentStatus = localStatusMap.value[sub.id] ?: sub.status
            val resolvedPhoto = userPhotosById[sub.applicantId].orEmpty()
            DisplayApplicationItem(
                id = sub.id,
                applicantName = sub.applicantName.ifBlank { "সম্মানিত আবেদনকারী" },
                designation = sub.postDesignation.ifBlank { sub.applicantQualification },
                madrasaName = sub.postTitle.ifBlank { "কওমি মাদরাসা" },
                dateText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(sub.createdAt),
                statusCode = currentStatus,
                photoUrl = resolvedPhoto,
                applicantId = sub.applicantId,
                backingSubmission = sub
            )
        }
        val representedPostIds = submissions.mapNotNull { it.postId.takeIf { pid -> pid.isNotBlank() } }.toSet()
        val standaloneApplicationPosts = posts
            .filter { it.postType == "SEEKER_APPLICATION" && it.id !in representedPostIds }
            .map { post ->
                val defaultStatus = when {
                    post.approvalStatus.equals("REJECTED", ignoreCase = true) -> "REJECTED"
                    post.isVerifiedMadrasa -> "ACCEPTED"
                    else -> "PENDING"
                }
                val itemKey = "post_app_${post.id}"
                val currentStatus = localStatusMap.value[itemKey] ?: defaultStatus
                val resolvedPhoto = post.authorPhotoUrl.ifBlank { userPhotosById[post.authorId].orEmpty() }
                DisplayApplicationItem(
                    id = itemKey,
                    applicantName = post.authorName.ifBlank { post.title },
                    designation = post.designation.ifBlank { post.qualification },
                    madrasaName = post.district.ifBlank { "সকল জেলা" },
                    dateText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(post.createdAt),
                    statusCode = currentStatus,
                    photoUrl = resolvedPhoto,
                    applicantId = post.authorId,
                    backingPost = post
                )
            }
        submissionItems + standaloneApplicationPosts
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
                item.applicantId.contains(q, ignoreCase = true) ||
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
            // Filter Chips: Real Server Counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalAppsBangla = IslamicDateAndPrayerHelper.toBanglaDigits(combinedApplications.size.toString())
                val pendingAppsBangla = IslamicDateAndPrayerHelper.toBanglaDigits(
                    combinedApplications.count { it.statusCode == "PENDING" }.toString()
                )
                val shortlistedAppsBangla = IslamicDateAndPrayerHelper.toBanglaDigits(
                    combinedApplications.count { it.statusCode == "SHORTLISTED" }.toString()
                )
                val chips = listOf(
                    "সব" to "সব ($totalAppsBangla)",
                    "নতুন" to "নতুন ($pendingAppsBangla)",
                    "পর্যালোচনাধীন" to "পর্যালোচনাধীন ($shortlistedAppsBangla)"
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
                if (filteredApplications.isEmpty()) {
                    item {
                        AdminEmptyDataCard(message = "কোনো তথ্য নেই")
                    }
                }
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
                                    borderWidth = 1.5.dp,
                                    modifier = Modifier.clickable(enabled = appItem.applicantId.isNotBlank()) {
                                        onInspectUserProfile(appItem.applicantId, appItem.applicantName)
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appItem.applicantName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable(enabled = appItem.applicantId.isNotBlank()) {
                                                onInspectUserProfile(appItem.applicantId, appItem.applicantName)
                                            }
                                            .testTag("admin_app_applicant_name_${appItem.id}")
                                    )
                                    if (appItem.applicantId.isNotBlank()) {
                                        Text(
                                            text = "ID: ${appItem.applicantId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AdminHeaderDarkGreen,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .clickable {
                                                    onInspectUserProfile(appItem.applicantId, appItem.applicantName)
                                                }
                                                .testTag("admin_app_applicant_uid_${appItem.id}")
                                        )
                                    }
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
                                        .testTag("admin_app_status_badge_${appItem.id}")
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
                                        "ACCEPTED" to "নির্বাচিত (অনুমোদন ও প্রকাশ)",
                                        "REJECTED" to "বাতিল"
                                    )
                                    statusOptions.forEach { (code, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                statusMenuExpanded = false
                                                localStatusMap.value =
                                                    localStatusMap.value + (appItem.id to code)
                                                when {
                                                    appItem.backingSubmission != null -> {
                                                        onUpdateSubmissionStatus(appItem.backingSubmission, code)
                                                    }
                                                    appItem.backingPost != null -> {
                                                        val approvedStatus = when (code) {
                                                            "ACCEPTED" -> "APPROVED"
                                                            "REJECTED" -> "REJECTED"
                                                            else -> "PENDING"
                                                        }
                                                        onVerifyPost(
                                                            appItem.backingPost,
                                                            code == "ACCEPTED",
                                                            approvedStatus
                                                        )
                                                    }
                                                    else -> {
                                                        onRecordLiveAdminAction(
                                                            "KHEDMOT_SUBMISSION",
                                                            if (code == "ACCEPTED") "আবেদন অনুমোদন করা হয়েছে" else "আবেদনের অবস্থা পরিবর্তন: $label",
                                                            "প্রার্থী: ${appItem.applicantName} (${appItem.designation}) - ${appItem.madrasaName}",
                                                            appItem.id
                                                        )
                                                    }
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
            if (adminMembers.isEmpty()) {
                item {
                    AdminEmptyDataCard(message = "কোনো তথ্য নেই")
                }
            }
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
    onRecordLiveAdminAction: (eventType: String, title: String, message: String, referenceId: String) -> Unit,
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
                        onCheckedChange = {
                            reqEducation = it
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "আবেদন ফর্মের শর্ত পরিবর্তন",
                                "'শিক্ষাগত যোগ্যতা' ফিল্ডটি ${if (it) "বাধ্যতামূলক" else "ঐচ্ছিক"} করা হয়েছে।",
                                "form_field_education"
                            )
                        }
                    )
                    FormCheckboxRow(
                        label = "অভিজ্ঞতা",
                        checked = reqExperience,
                        onCheckedChange = {
                            reqExperience = it
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "আবেদন ফর্মের শর্ত পরিবর্তন",
                                "'অভিজ্ঞতা' ফিল্ডটি ${if (it) "বাধ্যতামূলক" else "ঐচ্ছিক"} করা হয়েছে।",
                                "form_field_experience"
                            )
                        }
                    )
                    FormCheckboxRow(
                        label = "জাতীয় পরিচয়পত্র",
                        checked = reqNid,
                        onCheckedChange = {
                            reqNid = it
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "আবেদন ফর্মের শর্ত পরিবর্তন",
                                "'জাতীয় পরিচয়পত্র' ফিল্ডটি ${if (it) "বাধ্যতামূলক" else "ঐচ্ছিক"} করা হয়েছে।",
                                "form_field_nid"
                            )
                        }
                    )
                    FormCheckboxRow(
                        label = "অন্যান্য",
                        checked = reqOthers,
                        onCheckedChange = {
                            reqOthers = it
                            onRecordLiveAdminAction(
                                "ADMIN_SETTINGS_CHANGE",
                                "আবেদন ফর্মের শর্ত পরিবর্তন",
                                "'অন্যান্য' ফিল্ডটি ${if (it) "বাধ্যতামূলক" else "ঐচ্ছিক"} করা হয়েছে।",
                                "form_field_others"
                            )
                        }
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
                                    val newField = customFieldInput.trim()
                                    if (newField.isNotBlank()) {
                                        extraFields.add(newField)
                                        customFieldInput = ""
                                        onRecordLiveAdminAction(
                                            "ADMIN_SETTINGS_CHANGE",
                                            "আবেদন ফর্মে নতুন ফিল্ড সংযোজন",
                                            "আবেদন ফর্মে নতুন ফিল্ড '$newField' যোগ করা হয়েছে।",
                                            "form_field_custom_add"
                                        )
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
    latestAppRelease: com.example.data.model.AppReleaseInfo? = null,
    isSubmitting: Boolean = false,
    onToggleNewApp: (Boolean) -> Unit,
    onToggleNewNotice: (Boolean) -> Unit,
    onTogglePush: (Boolean) -> Unit,
    onToggleAnalytics: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    onTriggerBackup: () -> Unit,
    onTriggerConfigSync: () -> Unit,
    onPublishAppRelease: (
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        apkSha256: String,
        apkFileSizeBytes: Long,
        releaseNotes: String,
        isMandatory: Boolean,
        releaseStatus: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onBack: () -> Unit
) {
    var versionNameInput by rememberSaveable(latestAppRelease?.versionName) {
        mutableStateOf(latestAppRelease?.versionName?.takeIf { it.isNotBlank() } ?: "1.0.2")
    }
    var versionCodeInput by rememberSaveable(latestAppRelease?.versionCode) {
        mutableStateOf((latestAppRelease?.versionCode?.takeIf { it > 0 } ?: 2).toString())
    }
    var apkUrlInput by rememberSaveable(latestAppRelease?.apkDownloadUrl) {
        mutableStateOf(
            latestAppRelease?.apkDownloadUrl?.takeIf { it.isNotBlank() }
                ?: "https://github.com/qawmi-bd/qawmi-kormosangsthan/releases/latest/download/Qawmi-Kormosangsthan-Bangladesh.apk"
        )
    }
    var apkSha256Input by rememberSaveable(latestAppRelease?.apkSha256) {
        mutableStateOf(latestAppRelease?.apkSha256.orEmpty())
    }
    var apkFileSizeInput by rememberSaveable(latestAppRelease?.apkFileSizeBytes) {
        mutableStateOf(
            latestAppRelease?.apkFileSizeBytes?.takeIf { it > 0L }?.toString().orEmpty()
        )
    }
    var releaseNotesInput by rememberSaveable(latestAppRelease?.releaseNotes) {
        mutableStateOf(
            latestAppRelease?.releaseNotes?.takeIf { it.isNotBlank() }
                ?: "নতুন ফিচার, উন্নত নিরাপত্তা ও পারফরম্যান্স যুক্ত করা হয়েছে।"
        )
    }
    var isMandatoryInput by rememberSaveable(latestAppRelease?.isMandatory) {
        mutableStateOf(latestAppRelease?.isMandatory ?: false)
    }
    var releaseStatusInput by rememberSaveable(latestAppRelease?.releaseStatus, latestAppRelease?.isPublished) {
        mutableStateOf(
            when {
                latestAppRelease == null -> "PUBLISHED"
                !latestAppRelease.isPublished && latestAppRelease.releaseStatus == "PUBLISHED" -> "PAUSED"
                latestAppRelease.releaseStatus.uppercase() in setOf("PUBLISHED", "PAUSED", "DISABLED") ->
                    latestAppRelease.releaseStatus.uppercase()
                else -> "PUBLISHED"
            }
        )
    }
    var validationMessage by remember { mutableStateOf<String?>(null) }

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

            // অ্যাপ আপডেট ও সংস্করণ নিয়ন্ত্রণ (Firebase /app_releases/latest)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp))
                    .testTag("admin_app_update_control_card"),
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
                        text = "অ্যাপ আপডেট ও সংস্করণ নিয়ন্ত্রণ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AdminHeaderDarkGreen
                    )
                    Text(
                        text = "সর্বশেষ সংস্করণ, নিরাপদ HTTPS ডাউনলোড লিংক, SHA-256 চেকসাম এবং রিলিজের অবস্থা নির্ধারণ করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = versionNameInput,
                            onValueChange = { versionNameInput = it },
                            label = { Text("সংস্করণের নাম *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_update_version_name_input")
                        )
                        OutlinedTextField(
                            value = versionCodeInput,
                            onValueChange = { versionCodeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("সংস্করণ কোড *") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_update_version_code_input")
                        )
                    }

                    OutlinedTextField(
                        value = apkUrlInput,
                        onValueChange = { apkUrlInput = it },
                        label = { Text("APK ডাউনলোড লিংক (HTTPS) *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_apk_url_input")
                    )

                    OutlinedTextField(
                        value = apkSha256Input,
                        onValueChange = { apkSha256Input = it.trim() },
                        label = { Text("APK SHA-256 Checksum (৬৪ অক্ষরের Hex)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_sha256_input")
                    )

                    OutlinedTextField(
                        value = apkFileSizeInput,
                        onValueChange = { apkFileSizeInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("APK ফাইল সাইজ (বাইট - ঐচ্ছিক)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_file_size_input")
                    )

                    OutlinedTextField(
                        value = releaseNotesInput,
                        onValueChange = { releaseNotesInput = it },
                        label = { Text("রিলিজ নোট / পরিবর্তনের বিবরণ *") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_release_notes_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "বাধ্যতামূলক আপডেট (Mandatory)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = QawmiTextDark
                            )
                            Text(
                                text = if (isMandatoryInput) "বাধ্যতামূলক (সকল ইউজারের জন্য জরুরি)" else "ঐচ্ছিক (ইউজার 'পরে করুন' চাপতে পারবেন)",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiTextSecondary
                            )
                        }
                        Switch(
                            checked = isMandatoryInput,
                            onCheckedChange = { isMandatoryInput = it },
                            modifier = Modifier.testTag("admin_update_mandatory_switch")
                        )
                    }

                    Text(
                        text = "রিলিজের অবস্থা:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = QawmiTextDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusOptions = listOf(
                            "PUBLISHED" to "প্রকাশিত",
                            "PAUSED" to "সাময়িক স্থগিত",
                            "DISABLED" to "বন্ধ"
                        )
                        statusOptions.forEach { (statusCode, statusLabel) ->
                            val selected = releaseStatusInput == statusCode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) AdminHeaderDarkGreen else QawmiMintSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { releaseStatusInput = statusCode }
                                    .testTag("admin_update_status_${statusCode.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else QawmiTextDark
                                    )
                                }
                            }
                        }
                    }

                    if (validationMessage != null) {
                        Text(
                            text = validationMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828),
                            modifier = Modifier.testTag("admin_update_validation_message")
                        )
                    }

                    Button(
                        onClick = {
                            val parsedCode = versionCodeInput.trim().toIntOrNull() ?: 0
                            val parsedSize = apkFileSizeInput.trim().toLongOrNull() ?: 0L
                            val trimmedName = versionNameInput.trim()
                            val trimmedUrl = apkUrlInput.trim()
                            val trimmedSha = com.example.ui.util.AppUpdateManagerHelper.normalizeSha256(apkSha256Input)
                            when {
                                parsedCode < 1 -> {
                                    validationMessage = "সঠিক সংস্করণ কোড (ন্যূনতম ১) প্রদান করুন।"
                                }
                                trimmedName.isBlank() -> {
                                    validationMessage = "সঠিক সংস্করণের নাম লিখুন।"
                                }
                                !com.example.ui.util.AppUpdateManagerHelper.isTrustedHttpsApkUrl(trimmedUrl) -> {
                                    validationMessage = "শুধুমাত্র বৈধ HTTPS APK ডাউনলোড লিংক (https://...) প্রদান করুন।"
                                }
                                trimmedSha.isNotBlank() && !com.example.ui.util.AppUpdateManagerHelper.isValidSha256Hex(trimmedSha) -> {
                                    validationMessage = "SHA-256 চেকসাম অবশ্যই ৬৪ অক্ষরের বৈধ Hex হতে হবে।"
                                }
                                else -> {
                                    validationMessage = null
                                    onPublishAppRelease(
                                        parsedCode,
                                        trimmedName,
                                        trimmedUrl,
                                        trimmedSha,
                                        parsedSize,
                                        releaseNotesInput.trim(),
                                        isMandatoryInput,
                                        releaseStatusInput
                                    )
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = AdminHeaderDarkGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_publish_update_btn")
                    ) {
                        Text(
                            text = "আপডেট তথ্য সংরক্ষণ করুন",
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

    val allBoostItems = remember(posts, localOngoingMap.value) {
        posts.map { post ->
            val ongoing = localOngoingMap.value[post.id] ?: post.isBoosted
            DisplayBoostItem(
                id = post.id,
                title = post.title,
                subtitleViews = "${post.designation.ifBlank { "বিজ্ঞপ্তি" }} | ${post.district.ifBlank { "বাংলাদেশ" }}",
                dateRange = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(post.createdAt),
                isOngoing = ongoing,
                backingPost = post
            )
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
                if (filteredBoosts.isEmpty()) {
                    item {
                        AdminEmptyDataCard(message = "কোনো তথ্য নেই")
                    }
                }
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
    telegramLogs: List<TelegramNotificationLog> = emptyList(),
    onToggleAdminNotif: (Boolean) -> Unit,
    onToggleNewAppNotif: (Boolean) -> Unit,
    onToggleImportantUpdate: (Boolean) -> Unit,
    onToggleSecurityAlert: (Boolean) -> Unit,
    onSendTestTelegramAlert: (String) -> Unit,
    onBack: () -> Unit
) {
    val fittedChatId = TelegramNotificationHelper.resolveTargetChatId()
    val fittedBotId = TelegramNotificationHelper.resolveConfiguredBotId()
    val isTokenConfigured = remember { TelegramNotificationHelper.isTelegramBotConfigured() }
    val coroutineScope = rememberCoroutineScope()

    var isCheckingGetMe by remember { mutableStateOf(false) }
    var getMeDiagResult by remember {
        mutableStateOf<TelegramNotificationHelper.TelegramBotDiagnosticResult?>(null)
    }

    LaunchedEffect(isTokenConfigured) {
        if (isTokenConfigured && getMeDiagResult == null && !isCheckingGetMe) {
            isCheckingGetMe = true
            getMeDiagResult = TelegramNotificationHelper.verifyBotConnectionWithGetMe()
            isCheckingGetMe = false
        }
    }

    val deliveredCount = remember(telegramLogs) { telegramLogs.count { it.deliveryStatus == "DELIVERED" } }
    val queuedCount = remember(telegramLogs) { telegramLogs.count { it.deliveryStatus == "QUEUED" } }
    val failedCount = remember(telegramLogs) { telegramLogs.count { it.deliveryStatus == "FAILED" } }

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
            // Permanently Fitted Telegram Bot & Chat ID Status Card (No editable input boxes)
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
                            text = "Telegram Bot অটো-সিকিউরিটি সংযোগ",
                            style = MaterialTheme.typography.titleMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "চ্যাট আইডি (Chat ID): $fittedChatId",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "বট আইডি (Bot ID): $fittedBotId",
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
                                text = "🔒 অ্যাপের ভেতরে স্থায়ীভাবে ফিট ও সক্রিয়",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Live Telegram API Diagnostic & Real Delivery Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (getMeDiagResult?.getMeVerified == true) AdminBadgeActiveGreen
                        else if (getMeDiagResult != null || !isTokenConfigured) AdminBadgeReviewAmber
                        else AdminCardBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("admin_telegram_diagnostic_card"),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বাস্তব API সংযোগ ও ডেলিভারি ডায়াগনস্টিক",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdminHeaderDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                getMeDiagResult?.getMeVerified == true -> AdminBadgeActiveGreen
                                !isTokenConfigured || getMeDiagResult?.getMeVerified == false -> AdminBadgeInactiveRed
                                else -> AdminBadgeReviewAmber
                            }
                        ) {
                            Text(
                                text = when {
                                    getMeDiagResult?.getMeVerified == true -> "getMe: OK (200)"
                                    !isTokenConfigured -> "টোকেন কনফিগার নেই"
                                    getMeDiagResult != null -> "HTTP ${getMeDiagResult?.getMeHttpCode ?: 401} ত্রুটি"
                                    else -> "যাচাই প্রস্তুত"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Real-time Delivery Counts Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = QawmiMintSurface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = IslamicDateAndPrayerHelper.toBanglaDigits(deliveredCount.toString()),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AdminBadgeActiveGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "সফল (DELIVERED)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiTextDark
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF8E7),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = IslamicDateAndPrayerHelper.toBanglaDigits(queuedCount.toString()),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AdminBadgeReviewAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "পেন্ডিং (QUEUED)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiTextDark
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFDECEC),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = IslamicDateAndPrayerHelper.toBanglaDigits(failedCount.toString()),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AdminBadgeInactiveRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ব্যর্থ (FAILED)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiTextDark
                                )
                            }
                        }
                    }

                    val statusExplanation = getMeDiagResult?.diagnosticMessageBangla
                        ?: if (!isTokenConfigured) {
                            if (deliveredCount > 0) {
                                "সতর্কতা: পূর্বে টেলিগ্রাম বটে মেসেজ পৌঁছেছে ($deliveredCount টি সফল), কিন্তু বর্তমান ইনস্টল করা অ্যাপ বিল্ডে TELEGRAM_BOT_TOKEN সংযুক্ত নেই (যেমন GitHub Actions বিল্ডে Secret না থাকলে ডিফল্ট প্লেসহোল্ডার ব্যবহৃত হয়)। এই বিল্ড থেকে সরাসরি ডেলিভারি সচল রাখতে বিল্ড পরিবেশে TELEGRAM_BOT_TOKEN নিশ্চিত করুন।"
                            } else {
                                "সতর্কতা: অ্যাপের পরিবেশে TELEGRAM_BOT_TOKEN কনফিগার করা নেই। AI Studio-এর Secrets প্যানেলে বট ৮৯৮২৮৫৩১৯৭-এর সক্রিয় টোকেন সেট করুন।"
                            }
                        } else {
                            "বট টোকেন কনফিগার করা আছে। নিচের 'বট সংযোগ যাচাই করুন (getMe API)' বাটনে ট্যাপ করে টেলিগ্রাম সার্ভারের সরাসরি রেসপন্স পরীক্ষা করুন।"
                        }

                    Text(
                        text = statusExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (getMeDiagResult?.getMeVerified == true) AdminHeaderDarkGreen else QawmiTextDark,
                        modifier = Modifier.testTag("admin_telegram_diagnostic_message")
                    )

                    OutlinedButton(
                        onClick = {
                            if (!isCheckingGetMe) {
                                isCheckingGetMe = true
                                coroutineScope.launch {
                                    getMeDiagResult = TelegramNotificationHelper.verifyBotConnectionWithGetMe()
                                    isCheckingGetMe = false
                                }
                            }
                        },
                        enabled = !isCheckingGetMe && !isSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_verify_telegram_getme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AdminHeaderDarkGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCheckingGetMe) "Telegram getMe যাচাই হচ্ছে..." else "বট সংযোগ যাচাই করুন (getMe API)",
                            style = MaterialTheme.typography.labelLarge,
                            color = AdminHeaderDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Read-Only Auto-Alert Policy Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiMintSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AdminHeaderDarkGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "স্বয়ংক্রিয় অ্যাডমিন নজরদারি ও অটো-বট ইত্তেলা",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdminHeaderDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "টেলিগ্রাম চ্যাট আইডি ($fittedChatId) ও বট টোকেন অ্যাপের ভেতরে স্থায়ীভাবে সংযুক্ত রয়েছে। অ্যাডমিন প্যানেল থেকে যেকোনো অ্যাডমিন অ্যাপের কোনো নিয়ম-নীতি পরিবর্তন, পোস্ট/ব্যবহারকারী নিয়ন্ত্রণ বা যেকোনো কার্যকলাপ করলে সাথে সাথে স্বয়ংক্রিয় বার্তা (Auto Message) বটে চলে যাবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextDark
                        )
                    }
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
                    onSendTestTelegramAlert(
                        "আলহামদুলিল্লাহ, অ্যাপের ভেতরে স্থায়ীভাবে সংযুক্ত চ্যাট আইডি ($fittedChatId) ও বট আইডি ($fittedBotId)-তে স্বয়ংক্রিয় টেলিগ্রাম অ্যালার্ট সিস্টেম সক্রিয় রয়েছে।"
                    )
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AdminHeaderDarkGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_send_telegram_test_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "লাইভ টেস্ট নোটিফিকেশন পাঠান",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
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
    val deliveryStatus: String, // DELIVERED | QUEUED | FAILED
    val icon: ImageVector
)

@Composable
private fun AdminActivityLogView(
    telegramLogs: List<TelegramNotificationLog>,
    onBack: () -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf("সব") }

    val allLogs = remember(telegramLogs) {
        telegramLogs.map { log ->
            val isSupportOrComplaint = log.referenceId.startsWith("SUPPORT_TICKET") ||
                log.title.startsWith("[অভিযোগ") ||
                log.title.startsWith("[সমস্যা") ||
                log.title.startsWith("[পরামর্শ") ||
                log.title.startsWith("[সহায়তা") ||
                log.title.startsWith("[সহায়তা")
            val (cat, ic) = when {
                isSupportOrComplaint -> "অভিযোগ/সহায়তা" to Icons.Default.SupportAgent
                log.eventType == "NEW_CIRCULAR" || log.eventType == "ADMIN_ANNOUNCEMENT" -> "বিজ্ঞপ্তি" to Icons.Default.Campaign
                log.eventType == "NEW_APPLICATION" || log.eventType == "KHEDMOT_SUBMISSION" -> "আবেদন" to Icons.Default.CheckCircle
                log.eventType == "ADMIN_VERIFICATION" || log.eventType == "CONTENT_MODERATION" || log.eventType == "ADMIN_SECURITY" -> "ব্যবহারকারী" to Icons.Default.PersonAdd
                else -> "সব" to Icons.Default.Settings
            }
            val subText = buildString {
                if (log.message.isNotBlank()) append(log.message)
                if (log.actorName.isNotBlank()) append(" • প্রেরক: ${log.actorName}")
                if (log.targetPhone.isNotBlank()) append(" • যোগাযোগ: ${log.targetPhone}")
            }
            DisplayActivityLog(
                id = log.id.ifBlank { "tg_${log.title.hashCode()}_${log.createdAt?.seconds ?: 0}" },
                title = log.title,
                subtitle = subText,
                timeText = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(log.createdAt),
                category = cat,
                deliveryStatus = log.deliveryStatus.ifBlank { "QUEUED" },
                icon = ic
            )
        }
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
            title = "অ্যাডমিন অ্যাক্টিভিটি লগ ও অভিযোগ",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filter Tabs: সব | অভিযোগ/সহায়তা | ব্যবহারকারী | বিজ্ঞপ্তি | আবেদন
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("সব", "অভিযোগ/সহায়তা", "ব্যবহারকারী", "বিজ্ঞপ্তি", "আবেদন").forEach { tab ->
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
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else QawmiTextDark,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item {
                        AdminEmptyDataCard(message = "কোনো তথ্য নেই")
                    }
                }
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = logItem.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = QawmiTextDark,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val (statusText, statusColor) = when (logItem.deliveryStatus) {
                                            "DELIVERED" -> "DELIVERED ✓" to AdminBadgeActiveGreen
                                            "FAILED" -> "FAILED ✕" to AdminBadgeInactiveRed
                                            else -> "PENDING ⏳" to AdminBadgeReviewAmber
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusColor
                                        ) {
                                            Text(
                                                text = statusText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = logItem.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary,
                                        maxLines = 3,
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
