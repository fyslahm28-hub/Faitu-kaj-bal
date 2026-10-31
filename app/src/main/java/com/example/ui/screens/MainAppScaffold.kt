package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DomainAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppReleaseInfo
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotCategoryHelper
import com.example.data.model.UserProfile
import com.example.ui.auth.signOutUser
import com.example.ui.components.IslamicStarEmblem
import com.example.ui.components.QawmiTopBar
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSeekerBlue
import com.example.ui.theme.QawmiSoftBg
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.AjkerSmoronHelper
import com.example.ui.util.AppUpdateManagerHelper
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.util.QawmiNotificationHelper
import com.example.ui.viewmodel.FeedFilterTab
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.QawmiViewModel
import com.example.ui.viewmodel.UiState
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.launch

@Composable
fun MainAppScaffold(
    viewModel: QawmiViewModel,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* handled gracefully by QawmiNotificationHelper */ }
    )

    LaunchedEffect(Unit) {
        QawmiNotificationHelper.ensureChannelCreated(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !QawmiNotificationHelper.hasNotificationPermission(context)
        ) {
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Track real-time Online / Offline & Last Seen presence across app lifecycle
    DisposableEffect(lifecycleOwner, viewModel.currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    viewModel.onNetworkRestored()
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.setUserOnlineStatus(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Periodic foreground presence heartbeat so active users never appear offline while using the app
    LaunchedEffect(viewModel.currentUserId) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                viewModel.setUserOnlineStatus(true)
            }
        }
    }

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val selectedFeedFilter by viewModel.selectedFeedFilter.collectAsStateWithLifecycle()
    val selectedKhedmotCategory by viewModel.selectedKhedmotCategoryFilter.collectAsStateWithLifecycle()
    val selectedDistrict by viewModel.selectedDistrictFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val feedbackBanner by viewModel.feedbackBanner.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val failedMessageDraft by viewModel.failedMessageDraft.collectAsStateWithLifecycle()

    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            kotlinx.coroutines.delay(16_000L)
            viewModel.cancelSubmittingIfStuck()
        }
    }

    val isServerConnected by viewModel.isServerConnected.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val feedPostsState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val postCommentsState by viewModel.postCommentsState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val madrasasState by viewModel.madrasasState.collectAsStateWithLifecycle()
    val mySentSubmissionsState by viewModel.mySentSubmissionsState.collectAsStateWithLifecycle()
    val receivedSubmissionsState by viewModel.receivedSubmissionsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val userGroupsState by viewModel.userGroupsState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val isAdminState by viewModel.isAdminState.collectAsStateWithLifecycle()
    val telegramNotificationsState by viewModel.telegramNotificationsState.collectAsStateWithLifecycle()
    val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val adminSubmissionsState by viewModel.adminSubmissionsState.collectAsStateWithLifecycle()
    val adminRegistryListState by viewModel.adminRegistryListState.collectAsStateWithLifecycle()

    val selectedPostForApply by viewModel.selectedPostForApply.collectAsStateWithLifecycle()
    val selectedPostForDetail by viewModel.selectedPostForDetail.collectAsStateWithLifecycle()
    val messageTargetUser by viewModel.messageTargetUser.collectAsStateWithLifecycle()
    val inspectedUserTarget by viewModel.inspectedUserTarget.collectAsStateWithLifecycle()
    val inspectedUserProfileState by viewModel.inspectedUserProfileState.collectAsStateWithLifecycle()
    val activeChatPartnerProfile by viewModel.activeChatPartnerProfileState.collectAsStateWithLifecycle()
    val chatPartnerProfiles by viewModel.chatPartnerProfilesState.collectAsStateWithLifecycle()
    val smoronServerReceipts by viewModel.smoronServerReceiptsState.collectAsStateWithLifecycle()
    val focusedSmoronId by viewModel.focusedSmoronId.collectAsStateWithLifecycle()
    val latestAppRelease by viewModel.latestAppReleaseState.collectAsStateWithLifecycle()
    val dismissedUpdateVersionCode by viewModel.dismissedUpdateVersionCode.collectAsStateWithLifecycle()
    val installedVersion = remember(context) { AppUpdateManagerHelper.getInstalledVersion(context) }
    var fetchedFallbackRelease by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var resumeUpdateCheckTick by remember { mutableStateOf(0) }
    val effectiveLatestRelease = remember(latestAppRelease, fetchedFallbackRelease) {
        AppUpdateManagerHelper.pickMoreRecentRelease(latestAppRelease, fetchedFallbackRelease)
    }

    LaunchedEffect(viewModel.currentUserId, resumeUpdateCheckTick) {
        val remote = AppUpdateManagerHelper.fetchLatestReleaseSafely(context)
        if (remote != null) {
            fetchedFallbackRelease = AppUpdateManagerHelper.pickMoreRecentRelease(fetchedFallbackRelease, remote)
        }
    }

    DisposableEffect(lifecycleOwner, viewModel.currentUserId) {
        val updateResumeObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeUpdateCheckTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(updateResumeObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(updateResumeObserver)
        }
    }

    LaunchedEffect(viewModel.currentUserId, smoronServerReceipts) {
        val todayDateKey = AjkerSmoronHelper.currentBangladeshDateKey()
        val assigned = AjkerSmoronHelper.syncFromServerReceipts(
            context = context,
            userId = viewModel.currentUserId,
            receiptTimestamps = smoronServerReceipts,
            dateKey = todayDateKey
        )
        viewModel.syncSmoronAssignmentToServer(todayDateKey, assigned.id)

        if (AjkerSmoronHelper.isNotificationEnabled(context)) {
            val (prefHour, prefMin) = AjkerSmoronHelper.getPreferredNotificationTime(context)
            AjkerSmoronHelper.scheduleDailyPushAlarm(context, prefHour, prefMin)

            // If today's scheduled notification time has already arrived and no push was sent today, deliver once
            val dhakaCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
            val nowMinutes = dhakaCal.get(Calendar.HOUR_OF_DAY) * 60 + dhakaCal.get(Calendar.MINUTE)
            val targetMinutes = prefHour * 60 + prefMin
            if (nowMinutes >= targetMinutes &&
                !AjkerSmoronHelper.hasPushBeenSentForDate(context, viewModel.currentUserId, todayDateKey)
            ) {
                val delivered = QawmiNotificationHelper.deliverDailyAjkerSmoronPushNotification(
                    context = context,
                    userId = viewModel.currentUserId,
                    reminder = assigned,
                    dateKey = todayDateKey
                )
                if (delivered) {
                    viewModel.syncSmoronPushSentToServer(todayDateKey, assigned.id)
                }
            }
        }
    }

    val unreadNotificationsCount = remember(notificationsState, viewModel.currentUserId) {
        (notificationsState as? UiState.Success)?.data?.count {
            !it.isRead && it.senderId != viewModel.currentUserId && !it.type.equals("MESSAGE", ignoreCase = true)
        } ?: 0
    }

    // Requirement 8খ: Count DISTINCT unread conversations (1 per person/group regardless of how many unread messages they sent)
    val unreadDirectMessagesCount = remember(messagesState, viewModel.currentUserId) {
        val list = (messagesState as? UiState.Success)?.data ?: emptyList()
        list.distinctBy { msg ->
            msg.id.ifBlank {
                "${msg.senderId}_${msg.receiverId}_${msg.createdAt?.seconds ?: 0}_${msg.createdAt?.nanoseconds ?: 0}_${msg.body}"
            }
        }
            .filter { it.isUnreadForUser(viewModel.currentUserId) && !it.isDeletedForEveryone }
            .map { it.resolvedConversationKey(viewModel.currentUserId) }
            .filter { it.isNotBlank() }
            .distinct()
            .size
    }

    LaunchedEffect(notificationsState, viewModel.currentUserId) {
        val currentList = (notificationsState as? UiState.Success)?.data ?: return@LaunchedEffect
        QawmiNotificationHelper.deliverNewUnreadPushNotifications(
            context = context,
            currentUserId = viewModel.currentUserId,
            notifications = currentList
        )
    }

    LaunchedEffect(messagesState, userGroupsState, currentDestination, messageTargetUser, viewModel.currentUserId) {
        val currentMessages = (messagesState as? UiState.Success)?.data ?: return@LaunchedEffect
        val currentGroups = (userGroupsState as? UiState.Success)?.data ?: emptyList()
        val activeConvKey = if (currentDestination == MainDestination.MESSAGES) {
            messageTargetUser?.first
        } else {
            null
        }
        if (!activeConvKey.isNullOrBlank()) {
            QawmiNotificationHelper.cancelConversationNotification(
                context = context,
                conversationKey = activeConvKey,
                currentUserId = viewModel.currentUserId
            )
        }
        QawmiNotificationHelper.deliverNewUnreadMessageNotifications(
            context = context,
            currentUserId = viewModel.currentUserId,
            messages = currentMessages,
            chatGroups = currentGroups,
            activeConversationKey = activeConvKey
        )
    }

    // Mandatory BackHandler for custom state navigation
    BackHandler(enabled = currentDestination != MainDestination.HOME) {
        viewModel.navigateBack()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White
            ) {
                val currentProfile = (userProfileState as? UiState.Success)?.data
                val profileName = currentProfile?.fullName
                    ?.takeIf { it.isNotBlank() } ?: viewModel.currentUserDisplayName.ifBlank { "সম্মানিত দ্বীনি ভাই" }
                val profilePhoto = currentProfile?.photoUrl.orEmpty()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QawmiDarkGreen)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatarImage(
                            photoUrl = profilePhoto,
                            displayName = profileName,
                            size = 46.dp,
                            borderColor = QawmiGold,
                            borderWidth = 2.dp,
                            showOnlineIndicator = true,
                            isOnline = (currentProfile?.isOnline == true) && isServerConnected
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IslamicStarEmblem(size = 18.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "কওমি কর্মসংস্থান",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profileName,
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiGold,
                                fontWeight = FontWeight.Bold
                            )
                            if (viewModel.currentUserEmail.isNotBlank()) {
                                Text(
                                    text = viewModel.currentUserEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFDDF3EC),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    DrawerMenuItem(
                        label = "আজকের স্মরণ",
                        icon = Icons.Default.AutoAwesome,
                        selected = currentDestination == MainDestination.AJKER_SMORON,
                        testTag = "drawer_item_ajker_smoron"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.AJKER_SMORON)
                    }
                    DrawerMenuItem(
                        label = "মাদরাসা নিবন্ধন করুন",
                        icon = Icons.Default.DomainAdd,
                        selected = currentDestination == MainDestination.REGISTER_MADRASA,
                        testTag = "drawer_item_register_madrasa"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.REGISTER_MADRASA)
                    }
                    DrawerMenuItem(
                        label = "অ্যাপ সেটিংস",
                        icon = Icons.Default.Settings,
                        selected = currentDestination == MainDestination.APP_SETTINGS,
                        testTag = "drawer_item_app_settings"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.APP_SETTINGS)
                    }
                    DrawerMenuItem(
                        label = "অ্যাপ সম্পর্কে",
                        icon = Icons.Default.Info,
                        selected = currentDestination == MainDestination.ABOUT_APP,
                        testTag = "drawer_item_about_app"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.ABOUT_APP)
                    }
                    DrawerMenuItem(
                        label = "ডেভেলপার সম্পর্কে",
                        icon = Icons.Default.Code,
                        selected = currentDestination == MainDestination.ABOUT_DEVELOPER,
                        testTag = "drawer_item_about_developer"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.ABOUT_DEVELOPER)
                    }
                    DrawerMenuItem(
                        label = "ব্যবহারের নীতিমালা ও গোপনীয়তা",
                        icon = Icons.Default.Policy,
                        selected = currentDestination == MainDestination.PRIVACY_AND_TERMS,
                        testTag = "drawer_item_privacy_terms"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.PRIVACY_AND_TERMS)
                    }
                    DrawerMenuItem(
                        label = "সহায়তা ও যোগাযোগ",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        selected = currentDestination == MainDestination.HELP_AND_SUPPORT,
                        testTag = "drawer_item_help_support"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.HELP_AND_SUPPORT)
                    }
                    if (isAdminState) {
                        DrawerMenuItem(
                            label = "অ্যাডমিন প্যানেল",
                            icon = Icons.Default.AdminPanelSettings,
                            selected = currentDestination == MainDestination.ADMIN_PANEL,
                            testTag = "drawer_item_admin_panel"
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(MainDestination.ADMIN_PANEL)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("লগ-আউট (প্রস্থান)", color = Color(0xFFC62828), fontWeight = FontWeight.Bold) },
                    selected = false,
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFC62828)) },
                    onClick = {
                        scope.launch { runCatching { drawerState.close() } }
                        viewModel.setUserOnlineStatus(false)
                        signOutUser(
                            credentialManager = credentialManager,
                            onSignOutComplete = onSignedOut,
                            scope = scope
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .testTag("drawer_logout_button")
                )
            }
        }
    ) {
        val topBarTitle = when (currentDestination) {
            MainDestination.HOME -> "কওমি কর্মসংস্থান"
            MainDestination.FEED -> "ফিড"
            MainDestination.KHEDMOT -> "খেদমত (নিয়োগ বিজ্ঞপ্তি)"
            MainDestination.ABEDON -> "আবেদন ব্যবস্থাপনা"
            MainDestination.MADRASA -> "মাদরাসা"
            MainDestination.PROFILE -> "আমার প্রোফাইল"
            MainDestination.BOOKMARKS -> "পছন্দের খেদমত"
            MainDestination.VERIFIED_MADRASAS -> "যাচাইকৃত মাদরাসা"
            MainDestination.PUBLISH_CIRCULAR -> "বিজ্ঞপ্তি দিন"
            MainDestination.PUBLISH_APPLICATION -> "আবেদন প্রকাশ করুন"
            MainDestination.REGISTER_MADRASA -> "মাদরাসা নিবন্ধন"
            MainDestination.MESSAGES -> "পয়গাম"
            MainDestination.NOTIFICATIONS -> "নোটিফিকেশন"
            MainDestination.ADMIN_PANEL -> "অ্যাডমিন প্যানেল"
            MainDestination.PRAYER_SCHEDULE -> "নামাজের সময়সূচী"
            MainDestination.AJKER_SMORON -> "আজকের স্মরণ"
            MainDestination.APP_SETTINGS -> "অ্যাপ সেটিংস"
            MainDestination.ABOUT_APP -> "অ্যাপ সম্পর্কে"
            MainDestination.ABOUT_DEVELOPER -> "ডেভেলপার সম্পর্কে"
            MainDestination.PRIVACY_AND_TERMS -> "ব্যবহারের নীতিমালা ও গোপনীয়তা"
            MainDestination.HELP_AND_SUPPORT -> "সহায়তা ও যোগাযোগ"
        }

        val isRootTab = currentDestination in setOf(
            MainDestination.HOME,
            MainDestination.FEED,
            MainDestination.ABEDON,
            MainDestination.MESSAGES,
            MainDestination.PROFILE
        )

        val density = LocalDensity.current
        val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
        val hideBottomBarForActiveChatKeyboard =
            currentDestination == MainDestination.MESSAGES && isKeyboardOpen

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = QawmiSoftBg,
            topBar = {
                QawmiTopBar(
                    title = topBarTitle,
                    showTwinEmblems = currentDestination == MainDestination.HOME,
                    unreadNotificationCount = unreadNotificationsCount,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationClick = { viewModel.navigateTo(MainDestination.NOTIFICATIONS) },
                    showBackButton = !isRootTab,
                    onBackClick = { viewModel.navigateBack() }
                )
            },
            bottomBar = {
                if (!hideBottomBarForActiveChatKeyboard) {
                    QawmiBottomNavBar(
                        currentDestination = currentDestination,
                        unreadMessagesCount = unreadDirectMessagesCount,
                        onTabSelected = { tab ->
                            if (tab == MainDestination.FEED) {
                                viewModel.setFeedFilter(FeedFilterTab.ALL)
                            }
                            viewModel.navigateBottomTab(tab)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Active upload / submission progress indicator banner
                if (isSubmitting) {
                    Surface(
                        color = QawmiMintSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .testTag("upload_progress_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = QawmiDeepGreen,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "আপলোড ও প্রক্রিয়াকরণ চলছে, অনুগ্রহ করে অপেক্ষা করুন...",
                                color = QawmiDeepGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.cancelSubmittingIfStuck() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "বন্ধ করুন",
                                    tint = QawmiDeepGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Real feedback / error banners
                if (feedbackBanner != null || errorBanner != null) {
                    val isErr = errorBanner != null
                    Surface(
                        color = if (isErr) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .testTag(if (isErr) "error_banner" else "feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = errorBanner ?: feedbackBanner.orEmpty(),
                                color = if (isErr) Color(0xFFC62828) else QawmiDeepGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearBanners() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "বন্ধ করুন",
                                    tint = if (isErr) Color(0xFFC62828) else QawmiDeepGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentDestination) {
                        MainDestination.HOME -> {
                            HomeScreen(
                                currentUserId = viewModel.currentUserId,
                                onNavigate = { dest -> viewModel.navigateTo(dest) },
                                onOpenFeedFilter = { filterTab -> viewModel.openFeedWithFilter(filterTab) }
                            )
                        }

                        MainDestination.FEED -> {
                            val currentUserProfile = (userProfileState as? UiState.Success)?.data
                            FeedScreen(
                                feedPostsState = feedPostsState,
                                postCommentsState = postCommentsState,
                                bookmarksState = bookmarksState,
                                selectedFilter = selectedFeedFilter,
                                selectedKhedmotCategory = selectedKhedmotCategory,
                                selectedDistrict = selectedDistrict,
                                searchQuery = searchQuery,
                                currentUserId = viewModel.currentUserId,
                                isAdmin = isAdminState,
                                authorProfiles = chatPartnerProfiles,
                                currentUserProfile = currentUserProfile,
                                onSelectFilter = { viewModel.setFeedFilter(it) },
                                onSelectKhedmotCategory = { viewModel.setKhedmotCategoryFilter(it) },
                                onSelectDistrict = { viewModel.setDistrictFilter(it) },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onApplyClick = { post -> viewModel.openApplyDialog(post) },
                                onViewProfileOrDetailClick = { post -> viewModel.openPostDetailDialog(post) },
                                onInspectAuthorProfile = { uid, name -> viewModel.openUserProfileInspection(uid, name) },
                                onToggleBookmark = { post, bookmarked -> viewModel.toggleBookmark(post, bookmarked) },
                                onToggleBoostPost = { post, boost -> viewModel.adminToggleBoostPost(post, boost) },
                                onDeletePost = { postId -> viewModel.deletePost(postId) },
                                onNavigate = { dest -> viewModel.navigateTo(dest) },
                                onReactToPost = { post, reactionType -> viewModel.togglePostReaction(post, reactionType) },
                                onAddComment = { post, text, parentId, parentAuthor -> viewModel.addCommentToPost(post, text, parentId, parentAuthor) },
                                onEditComment = { commentId, newText -> viewModel.editPostComment(commentId, newText) },
                                onDeleteComment = { commentId, postId, currentCount -> viewModel.deletePostComment(commentId, postId, currentCount) },
                                onSharePostInApp = { post, caption -> viewModel.sharePostInApp(post, caption) },
                                onPublishQuickDeeniPost = { title, desc, dist, cats, customCat, imgUrls ->
                                    viewModel.publishFeedPost(
                                        authorName = currentUserProfile?.fullName?.ifBlank { viewModel.currentUserDisplayName } ?: viewModel.currentUserDisplayName,
                                        postType = "GENERAL_POST",
                                        title = title,
                                        district = dist,
                                        upazilaOrLocation = currentUserProfile?.district ?: "",
                                        designation = KhedmotCategoryHelper.buildCombinedDesignation("দ্বীনি আলোচনা", cats, customCat),
                                        qualification = currentUserProfile?.qualification ?: "",
                                        salaryOrExperience = currentUserProfile?.experience ?: "",
                                        deadlineOrPreferredDistricts = "",
                                        description = desc,
                                        contactNumber = currentUserProfile?.contactInfo ?: "",
                                        khedmotCategories = cats,
                                        customCategory = customCat,
                                        imageUrls = imgUrls,
                                        onSuccessNavigate = {}
                                    )
                                },
                                onEditOwnPost = { post, title, dist, loc, desig, qual, salExp, deadl, desc, phone, cats, customCat ->
                                    viewModel.updateOwnPost(
                                        post = post,
                                        title = title,
                                        district = dist,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = salExp,
                                        deadlineOrPreferredDistricts = deadl,
                                        description = desc,
                                        contactNumber = phone,
                                        khedmotCategories = cats,
                                        customCategory = customCat
                                    )
                                },
                                onEditOwnPostWithMedia = { post, title, dist, loc, desig, qual, salExp, deadl, desc, phone, cats, customCat, imgUrls ->
                                    viewModel.updateOwnPost(
                                        post = post,
                                        title = title,
                                        district = dist,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = salExp,
                                        deadlineOrPreferredDistricts = deadl,
                                        description = desc,
                                        contactNumber = phone,
                                        khedmotCategories = cats,
                                        customCategory = customCat,
                                        imageUrls = imgUrls
                                    )
                                },
                                onReportPost = { post, reason -> viewModel.reportPost(post, reason) },
                                onRetryFeed = { viewModel.retryFeedStreams() }
                            )
                        }

                        MainDestination.KHEDMOT -> {
                            val currentUserProfile = (userProfileState as? UiState.Success)?.data
                            FeedScreen(
                                feedPostsState = feedPostsState,
                                postCommentsState = postCommentsState,
                                bookmarksState = bookmarksState,
                                selectedFilter = FeedFilterTab.JOB_CIRCULAR,
                                selectedKhedmotCategory = selectedKhedmotCategory,
                                selectedDistrict = selectedDistrict,
                                searchQuery = searchQuery,
                                currentUserId = viewModel.currentUserId,
                                isAdmin = isAdminState,
                                authorProfiles = chatPartnerProfiles,
                                currentUserProfile = currentUserProfile,
                                onSelectFilter = { viewModel.setFeedFilter(it) },
                                onSelectKhedmotCategory = { viewModel.setKhedmotCategoryFilter(it) },
                                onSelectDistrict = { viewModel.setDistrictFilter(it) },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onApplyClick = { post -> viewModel.openApplyDialog(post) },
                                onViewProfileOrDetailClick = { post -> viewModel.openPostDetailDialog(post) },
                                onInspectAuthorProfile = { uid, name -> viewModel.openUserProfileInspection(uid, name) },
                                onToggleBookmark = { post, bookmarked -> viewModel.toggleBookmark(post, bookmarked) },
                                onToggleBoostPost = { post, boost -> viewModel.adminToggleBoostPost(post, boost) },
                                onDeletePost = { postId -> viewModel.deletePost(postId) },
                                onNavigate = { dest -> viewModel.navigateTo(dest) },
                                onReactToPost = { post, reactionType -> viewModel.togglePostReaction(post, reactionType) },
                                onAddComment = { post, text, parentId, parentAuthor -> viewModel.addCommentToPost(post, text, parentId, parentAuthor) },
                                onEditComment = { commentId, newText -> viewModel.editPostComment(commentId, newText) },
                                onDeleteComment = { commentId, postId, currentCount -> viewModel.deletePostComment(commentId, postId, currentCount) },
                                onSharePostInApp = { post, caption -> viewModel.sharePostInApp(post, caption) },
                                onPublishQuickDeeniPost = { title, desc, dist, cats, customCat, imgUrls ->
                                    viewModel.publishFeedPost(
                                        authorName = currentUserProfile?.fullName?.ifBlank { viewModel.currentUserDisplayName } ?: viewModel.currentUserDisplayName,
                                        postType = "GENERAL_POST",
                                        title = title,
                                        district = dist,
                                        upazilaOrLocation = currentUserProfile?.district ?: "",
                                        designation = KhedmotCategoryHelper.buildCombinedDesignation("দ্বীনি আলোচনা", cats, customCat),
                                        qualification = currentUserProfile?.qualification ?: "",
                                        salaryOrExperience = currentUserProfile?.experience ?: "",
                                        deadlineOrPreferredDistricts = "",
                                        description = desc,
                                        contactNumber = currentUserProfile?.contactInfo ?: "",
                                        khedmotCategories = cats,
                                        customCategory = customCat,
                                        imageUrls = imgUrls,
                                        onSuccessNavigate = {}
                                    )
                                },
                                onEditOwnPost = { post, title, dist, loc, desig, qual, salExp, deadl, desc, phone, cats, customCat ->
                                    viewModel.updateOwnPost(
                                        post = post,
                                        title = title,
                                        district = dist,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = salExp,
                                        deadlineOrPreferredDistricts = deadl,
                                        description = desc,
                                        contactNumber = phone,
                                        khedmotCategories = cats,
                                        customCategory = customCat
                                    )
                                },
                                onEditOwnPostWithMedia = { post, title, dist, loc, desig, qual, salExp, deadl, desc, phone, cats, customCat, imgUrls ->
                                    viewModel.updateOwnPost(
                                        post = post,
                                        title = title,
                                        district = dist,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = salExp,
                                        deadlineOrPreferredDistricts = deadl,
                                        description = desc,
                                        contactNumber = phone,
                                        khedmotCategories = cats,
                                        customCategory = customCat,
                                        imageUrls = imgUrls
                                    )
                                },
                                onReportPost = { post, reason -> viewModel.reportPost(post, reason) },
                                onRetryFeed = { viewModel.retryFeedStreams() }
                            )
                        }

                        MainDestination.ABEDON -> {
                            SubmissionsManagementScreen(
                                mySentState = mySentSubmissionsState,
                                receivedState = receivedSubmissionsState,
                                onUpdateStatus = { sub, status -> viewModel.updateReceivedSubmissionStatus(sub, status) },
                                onMessageUser = { uid, name -> viewModel.openDirectMessageTo(uid, name) },
                                onInspectUserProfile = { uid, name -> viewModel.openUserProfileInspection(uid, name) },
                                onNavigatePublishApplication = { viewModel.navigateTo(MainDestination.PUBLISH_APPLICATION) }
                            )
                        }

                        MainDestination.MADRASA -> {
                            MadrasaDirectoryScreen(
                                madrasasState = madrasasState,
                                onlyVerified = false,
                                currentUserId = viewModel.currentUserId,
                                onRegisterMadrasaClick = { viewModel.navigateTo(MainDestination.REGISTER_MADRASA) },
                                onMessageOwnerClick = { uid, name -> viewModel.openDirectMessageTo(uid, name) },
                                onInspectOwnerProfile = { uid, name -> viewModel.openUserProfileInspection(uid, name) }
                            )
                        }

                        MainDestination.VERIFIED_MADRASAS -> {
                            MadrasaDirectoryScreen(
                                madrasasState = madrasasState,
                                onlyVerified = true,
                                currentUserId = viewModel.currentUserId,
                                onRegisterMadrasaClick = { viewModel.navigateTo(MainDestination.REGISTER_MADRASA) },
                                onMessageOwnerClick = { uid, name -> viewModel.openDirectMessageTo(uid, name) },
                                onInspectOwnerProfile = { uid, name -> viewModel.openUserProfileInspection(uid, name) }
                            )
                        }

                        MainDestination.BOOKMARKS -> {
                            BookmarksScreen(
                                bookmarksState = bookmarksState,
                                feedPostsState = feedPostsState,
                                onRemoveBookmark = { post -> viewModel.toggleBookmark(post, true) },
                                onApplyOrDetailClick = { post ->
                                    if (post.postType == "JOB_CIRCULAR") {
                                        viewModel.openApplyDialog(post)
                                    } else {
                                        viewModel.openPostDetailDialog(post)
                                    }
                                }
                            )
                        }

                        MainDestination.PUBLISH_CIRCULAR -> {
                            PublishPostScreen(
                                isCircular = true,
                                userProfileState = userProfileState,
                                isSubmitting = isSubmitting,
                                onSubmit = { authorName, postType, title, district, loc, desig, qual, sal, deadl, desc, phone ->
                                    viewModel.publishFeedPost(
                                        authorName = authorName,
                                        postType = postType,
                                        title = title,
                                        district = district,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = sal,
                                        deadlineOrPreferredDistricts = deadl,
                                        description = desc,
                                        contactNumber = phone,
                                        onSuccessNavigate = {
                                            viewModel.openFeedWithFilter(FeedFilterTab.ALL)
                                        }
                                    )
                                },
                                onSubmitWithCategories = { authorName, postType, title, district, loc, desig, qual, sal, deadl, desc, phone, cats, customCat ->
                                    viewModel.publishFeedPost(
                                        authorName = authorName,
                                        postType = postType,
                                        title = title,
                                        district = district,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = sal,
                                        deadlineOrPreferredDistricts = deadl,
                                        description = desc,
                                        contactNumber = phone,
                                        khedmotCategories = cats,
                                        customCategory = customCat,
                                        onSuccessNavigate = {
                                            viewModel.openFeedWithFilter(FeedFilterTab.ALL)
                                        }
                                    )
                                },
                                onSubmitWithMedia = { authorName, postType, title, district, loc, desig, qual, sal, deadl, desc, phone, cats, customCat, imgUrls ->
                                    viewModel.publishFeedPost(
                                        authorName = authorName,
                                        postType = postType,
                                        title = title,
                                        district = district,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = sal,
                                        deadlineOrPreferredDistricts = deadl,
                                        description = desc,
                                        contactNumber = phone,
                                        khedmotCategories = cats,
                                        customCategory = customCat,
                                        imageUrls = imgUrls,
                                        onSuccessNavigate = {
                                            viewModel.openFeedWithFilter(FeedFilterTab.ALL)
                                        }
                                    )
                                }
                            )
                        }

                        MainDestination.PUBLISH_APPLICATION -> {
                            PublishPostScreen(
                                isCircular = false,
                                userProfileState = userProfileState,
                                isSubmitting = isSubmitting,
                                onSubmit = { authorName, postType, title, district, loc, desig, qual, exp, prefDist, desc, phone ->
                                    viewModel.publishFeedPost(
                                        authorName = authorName,
                                        postType = postType,
                                        title = title,
                                        district = district,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = exp,
                                        deadlineOrPreferredDistricts = prefDist,
                                        description = desc,
                                        contactNumber = phone,
                                        onSuccessNavigate = {
                                            viewModel.openFeedWithFilter(FeedFilterTab.APPLICATIONS)
                                        }
                                    )
                                },
                                onSubmitWithCategories = { authorName, postType, title, district, loc, desig, qual, exp, prefDist, desc, phone, cats, customCat ->
                                    viewModel.publishFeedPost(
                                        authorName = authorName,
                                        postType = postType,
                                        title = title,
                                        district = district,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = exp,
                                        deadlineOrPreferredDistricts = prefDist,
                                        description = desc,
                                        contactNumber = phone,
                                        khedmotCategories = cats,
                                        customCategory = customCat,
                                        onSuccessNavigate = {
                                            viewModel.openFeedWithFilter(FeedFilterTab.APPLICATIONS)
                                        }
                                    )
                                },
                                onSubmitWithMedia = { authorName, postType, title, district, loc, desig, qual, exp, prefDist, desc, phone, cats, customCat, imgUrls ->
                                    viewModel.publishFeedPost(
                                        authorName = authorName,
                                        postType = postType,
                                        title = title,
                                        district = district,
                                        upazilaOrLocation = loc,
                                        designation = desig,
                                        qualification = qual,
                                        salaryOrExperience = exp,
                                        deadlineOrPreferredDistricts = prefDist,
                                        description = desc,
                                        contactNumber = phone,
                                        khedmotCategories = cats,
                                        customCategory = customCat,
                                        imageUrls = imgUrls,
                                        onSuccessNavigate = {
                                            viewModel.openFeedWithFilter(FeedFilterTab.APPLICATIONS)
                                        }
                                    )
                                }
                            )
                        }

                        MainDestination.REGISTER_MADRASA -> {
                            RegisterMadrasaScreen(
                                isSubmitting = isSubmitting,
                                onSubmit = { name, dist, addr, year, marhala, fac, count, phone ->
                                    viewModel.registerMadrasa(
                                        name = name,
                                        district = dist,
                                        address = addr,
                                        establishedYear = year,
                                        highestMarhala = marhala,
                                        facilities = fac,
                                        studentCount = count,
                                        contactPhone = phone,
                                        onSuccessNavigate = {
                                            viewModel.navigateBottomTab(MainDestination.MADRASA)
                                        }
                                    )
                                }
                            )
                        }

                        MainDestination.PROFILE -> {
                            ProfileScreen(
                                userProfileState = userProfileState,
                                currentUserId = viewModel.currentUserId,
                                currentUserDisplayName = viewModel.currentUserDisplayName,
                                currentUserEmail = viewModel.currentUserEmail,
                                feedPostsState = feedPostsState,
                                mySentSubmissionsState = mySentSubmissionsState,
                                receivedSubmissionsState = receivedSubmissionsState,
                                bookmarksState = bookmarksState,
                                isSubmitting = isSubmitting,
                                onUpdatePhotoImmediately = { newPhotoUrl ->
                                    viewModel.updateProfilePhotoImmediately(newPhotoUrl)
                                },
                                onViewPostDetail = { post ->
                                    viewModel.openPostDetailDialog(post)
                                },
                                onDeleteOwnPost = { postId ->
                                    viewModel.deletePost(postId)
                                },
                                onNavigate = { dest ->
                                    viewModel.navigateTo(dest)
                                },
                                onSaveProfile = { name, type, dist, desig, qual, exp, pref, bio, phone, madrasa, photoUrl ->
                                    viewModel.saveProfile(
                                        fullName = name,
                                        accountType = type,
                                        district = dist,
                                        designation = desig,
                                        qualification = qual,
                                        experience = exp,
                                        preferredDistricts = pref,
                                        bio = bio,
                                        contactInfo = phone,
                                        madrasaName = madrasa,
                                        photoUrl = photoUrl
                                    )
                                }
                            )
                        }

                        MainDestination.MESSAGES -> {
                            val activeGroups = (userGroupsState as? UiState.Success)?.data ?: emptyList()
                            MessagesScreen(
                                messagesState = messagesState,
                                currentUserId = viewModel.currentUserId,
                                currentUserProfile = (userProfileState as? UiState.Success)?.data,
                                targetUser = messageTargetUser,
                                partnerProfiles = chatPartnerProfiles,
                                activePartnerProfile = activeChatPartnerProfile,
                                userGroups = activeGroups,
                                isSubmitting = isSubmitting,
                                failedMessageDraft = failedMessageDraft,
                                onClearFailedMessageDraft = { viewModel.clearFailedMessageDraft() },
                                onSendMessage = { recId, recName, subj, body ->
                                    viewModel.sendMessage(recId, recName, subj, body)
                                },
                                onSendMessageWithImages = { recId, recName, subj, body, imageUrls, groupId, groupName, groupMemberIds ->
                                    viewModel.sendMessage(
                                        receiverId = recId,
                                        receiverName = recName,
                                        subject = subj,
                                        body = body,
                                        imageUrls = imageUrls,
                                        groupId = groupId,
                                        groupName = groupName,
                                        groupMemberIds = groupMemberIds
                                    )
                                },
                                onSelectRecipient = { uid, name -> viewModel.openDirectMessageTo(uid, name) },
                                onClearRecipient = { viewModel.clearMessageTarget() },
                                onMarkRead = { msgId -> viewModel.markMessageRead(msgId) },
                                onMarkConversationRead = { partnerUid -> viewModel.markConversationRead(partnerUid) },
                                onTypingStatusChanged = { targetUid, isTyping ->
                                    viewModel.setTypingStatus(targetUid, isTyping)
                                },
                                onInspectUserProfile = { uid, name -> viewModel.openUserProfileInspection(uid, name) },
                                onReactToMessage = { msg, code -> viewModel.reactToMessage(msg, code) },
                                onEditMessage = { msg, newBody -> viewModel.editMessage(msg, newBody) },
                                onDeleteMessageForMe = { msg -> viewModel.deleteMessageForMe(msg) },
                                onDeleteMessageForEveryone = { msg -> viewModel.deleteMessageForEveryone(msg) },
                                onToggleBlockUser = { targetUid, block -> viewModel.toggleBlockUser(targetUid, block) },
                                onSaveConversationTheme = { partnerUid, themeSpec -> viewModel.saveConversationTheme(partnerUid, themeSpec) },
                                onCreateGroup = { name, photoUrl, memberIds, memberNames ->
                                    viewModel.createChatGroup(
                                        name = name,
                                        photoUrl = photoUrl,
                                        initialMemberIds = memberIds,
                                        initialMemberNames = memberNames,
                                        onCreated = { createdGroup ->
                                            viewModel.openDirectMessageTo("GROUP:${createdGroup.id}", createdGroup.name)
                                        }
                                    )
                                },
                                onUpdateGroupDetails = { groupId, newName, newPhoto ->
                                    viewModel.updateChatGroupDetails(groupId, newName, newPhoto)
                                },
                                onAddGroupMember = { groupId, memberId, memberName ->
                                    viewModel.addMemberToChatGroup(groupId, memberId, memberName)
                                },
                                onRemoveGroupMember = { groupId, memberId ->
                                    viewModel.removeMemberFromChatGroup(groupId, memberId)
                                },
                                onRetryFailedMessage = { msg ->
                                    viewModel.retryFailedMessage(msg)
                                }
                            )
                        }

                        MainDestination.NOTIFICATIONS -> {
                            NotificationsScreen(
                                notificationsState = notificationsState,
                                onNotificationClick = { notif -> viewModel.openNotificationTarget(notif) },
                                onMarkAllRead = { viewModel.markAllNotificationsRead() }
                            )
                        }

                        MainDestination.ADMIN_PANEL -> {
                            val currentUserProf = (userProfileState as? UiState.Success)?.data
                            AdminPanelScreen(
                                isAdminUser = isAdminState,
                                adminEmail = viewModel.currentUserEmail,
                                adminDisplayName = currentUserProf?.fullName?.takeIf { it.isNotBlank() }
                                    ?: viewModel.currentUserDisplayName.ifBlank { "অ্যাডমিন" },
                                adminPhotoUrl = currentUserProf?.photoUrl.orEmpty(),
                                usersState = adminUsersState,
                                submissionsState = adminSubmissionsState,
                                madrasasState = madrasasState,
                                feedPostsState = feedPostsState,
                                telegramNotificationsState = telegramNotificationsState,
                                adminsState = adminRegistryListState,
                                latestAppRelease = effectiveLatestRelease,
                                isSubmitting = isSubmitting,
                                onRetryServerLoad = { viewModel.retryAdminServerStreams() },
                                onVerifyUser = { user, ver -> viewModel.adminUpdateUserVerification(user, ver) },
                                onUpdateSubmissionStatus = { sub, status -> viewModel.adminUpdateSubmissionStatus(sub, status) },
                                onVerifyMadrasa = { madrasa, ver -> viewModel.adminVerifyMadrasa(madrasa, ver) },
                                onVerifyPost = { post, ver, status -> viewModel.adminVerifyPost(post, ver, status) },
                                onToggleBoostPost = { post, boost -> viewModel.adminToggleBoostPost(post, boost) },
                                onPublishQuickNotice = { title, desc, type ->
                                    viewModel.adminPublishQuickNotice(title, desc, type)
                                },
                                onSendAnnouncement = { title, msg -> viewModel.publishAdminAnnouncement(title, msg) },
                                onRecordLiveAdminAction = { eventType, title, msg, refId ->
                                    viewModel.adminRecordChangeNotification(eventType, title, msg, refId)
                                },
                                onRequestPasswordReset = { email -> viewModel.requestAdminPasswordReset(email) },
                                onSendTestTelegramAlert = { customMsg -> viewModel.sendTestTelegramNotification(customMsg) },
                                onPublishAppRelease = { verCode, verName, apkUrl, sha256, sizeBytes, notes, mandatory, status ->
                                    viewModel.adminPublishAppRelease(
                                        versionCode = verCode,
                                        versionName = verName,
                                        apkDownloadUrl = apkUrl,
                                        releaseNotes = notes,
                                        apkSha256 = sha256,
                                        apkFileSizeBytes = sizeBytes,
                                        isMandatory = mandatory,
                                        releaseStatus = status
                                    )
                                },
                                onSaveAdminMember = { memberId, name, role, isActive ->
                                    viewModel.adminSaveAdminMember(memberId, name, role, isActive)
                                },
                                onInspectUserProfile = { uid, name ->
                                    viewModel.openUserProfileInspection(uid, name)
                                },
                                onDeletePost = { id -> viewModel.deletePost(id) }
                            )
                        }

                        MainDestination.PRAYER_SCHEDULE -> {
                            FullPrayerScheduleScreen()
                        }

                        MainDestination.AJKER_SMORON -> {
                            AjkerSmoronScreen(
                                currentUserId = viewModel.currentUserId,
                                serverReceiptsMap = smoronServerReceipts,
                                focusedReminderId = focusedSmoronId,
                                onConsumeFocusedReminder = { viewModel.clearFocusedSmoronId() },
                                onSyncAssignmentToServer = { dateKey, remId ->
                                    viewModel.syncSmoronAssignmentToServer(dateKey, remId)
                                },
                                onSyncAmolCompletedToServer = { dateKey ->
                                    viewModel.syncSmoronAmolCompletedToServer(dateKey)
                                },
                                onShowFeedback = { msg -> viewModel.showFeedbackMessage(msg) }
                            )
                        }

                        MainDestination.APP_SETTINGS -> {
                            val currentUserProf = (userProfileState as? UiState.Success)?.data
                            UserAppSettingsScreen(
                                isOnlineVisible = currentUserProf?.isOnline ?: true,
                                onToggleOnlineStatus = { online -> viewModel.setUserOnlineStatus(online) },
                                onShowFeedback = { msg -> viewModel.showFeedbackMessage(msg) }
                            )
                        }

                        MainDestination.ABOUT_APP -> {
                            AboutAppScreen()
                        }

                        MainDestination.ABOUT_DEVELOPER -> {
                            AboutDeveloperScreen(
                                isSubmitting = isSubmitting,
                                onShowFeedback = { msg -> viewModel.showFeedbackMessage(msg) },
                                onSubmitDeveloperFeedback = { category, subject, message, contactPhone, onReset ->
                                    viewModel.submitUserSupportMessage(
                                        category = category,
                                        subject = subject,
                                        message = message,
                                        contactPhone = contactPhone,
                                        onSuccessReset = onReset
                                    )
                                }
                            )
                        }

                        MainDestination.PRIVACY_AND_TERMS -> {
                            PrivacyAndTermsScreen()
                        }

                        MainDestination.HELP_AND_SUPPORT -> {
                            val currentUserProf = (userProfileState as? UiState.Success)?.data
                            HelpAndSupportScreen(
                                defaultSenderPhone = currentUserProf?.contactInfo.orEmpty(),
                                isSubmitting = isSubmitting,
                                onShowFeedback = { msg -> viewModel.showFeedbackMessage(msg) },
                                onSubmitSupportMessage = { category, subject, message, contactPhone, onSuccessReset ->
                                    viewModel.submitUserSupportMessage(
                                        category = category,
                                        subject = subject,
                                        message = message,
                                        contactPhone = contactPhone,
                                        onSuccessReset = onSuccessReset
                                    )
                                }
                            )
                        }
                    }

                    // Auto-Update Prompt Overlay (“নতুন আপডেট উপলব্ধ” / “নতুন আপডেট এসেছে” বার্তাসহ “আপডেট নিন” ও “পরে করুন” বাটন)
                    val persistedCooldownVersion = remember(context, dismissedUpdateVersionCode, resumeUpdateCheckTick) {
                        dismissedUpdateVersionCode
                            ?: AppUpdateManagerHelper.getActiveCooldownDismissedVersionCode(context)
                    }
                    val shouldShowUpdatePrompt = remember(
                        installedVersion,
                        effectiveLatestRelease,
                        persistedCooldownVersion
                    ) {
                        AppUpdateManagerHelper.isUpdateAvailable(
                            installedVersionCode = installedVersion.versionCode,
                            installedVersionName = installedVersion.versionName,
                            remoteRelease = effectiveLatestRelease,
                            dismissedVersionCode = persistedCooldownVersion
                        )
                    }
                    if (shouldShowUpdatePrompt && effectiveLatestRelease != null) {
                        AppUpdatePromptOverlay(
                            installedVersionCode = installedVersion.versionCode,
                            installedVersionName = installedVersion.versionName,
                            releaseInfo = effectiveLatestRelease!!,
                            onDismissLater = {
                                val ver = effectiveLatestRelease!!.versionCode
                                AppUpdateManagerHelper.saveDismissedUpdatePrompt(context, ver)
                                viewModel.dismissCurrentUpdatePrompt(ver)
                            }
                        )
                    }
                }
            }
        }
    }

    // Apply to Circular Modal Dialog
    if (selectedPostForApply != null) {
        val post = selectedPostForApply!!
        val userProfile = (userProfileState as? UiState.Success)?.data
        ApplyToKhedmotDialog(
            post = post,
            initialProfile = userProfile,
            defaultName = viewModel.currentUserDisplayName,
            isSubmitting = isSubmitting,
            onDismiss = { viewModel.openApplyDialog(null) },
            onSubmit = { name, qual, exp, phone, note ->
                viewModel.submitKhedmotApplication(
                    post = post,
                    applicantName = name,
                    applicantQualification = qual,
                    applicantExperience = exp,
                    applicantPhone = phone,
                    coverNote = note
                )
            }
        )
    }

    // Post / Candidate Profile Detail Modal Dialog
    if (selectedPostForDetail != null) {
        val post = selectedPostForDetail!!
        val isOwnPost = post.authorId == viewModel.currentUserId
        val ownProfile = (userProfileState as? UiState.Success)?.data
        val liveAuthorProfile = if (isOwnPost) ownProfile else chatPartnerProfiles[post.authorId]
        val resolvedPost = post.copy(
            authorName = liveAuthorProfile?.fullName?.takeIf { it.isNotBlank() } ?: post.authorName,
            authorPhotoUrl = liveAuthorProfile?.photoUrl?.takeIf { it.isNotBlank() } ?: post.authorPhotoUrl
        )
        PostDetailDialog(
            post = resolvedPost,
            isOwnPost = isOwnPost,
            onDismiss = { viewModel.openPostDetailDialog(null) },
            onApplyClick = {
                viewModel.openPostDetailDialog(null)
                viewModel.openApplyDialog(resolvedPost)
            },
            onInspectAuthorProfileClick = {
                viewModel.openPostDetailDialog(null)
                viewModel.openUserProfileInspection(
                    userId = resolvedPost.authorId,
                    fallbackName = resolvedPost.authorName.ifBlank { resolvedPost.title }
                )
            },
            onSendMessageClick = {
                viewModel.openPostDetailDialog(null)
                viewModel.openDirectMessageTo(
                    userId = resolvedPost.authorId,
                    userName = resolvedPost.authorName.ifBlank { resolvedPost.title }
                )
            }
        )
    }

    // Public User Profile Modal Dialog (with Message button only for other users)
    if (inspectedUserTarget != null) {
        val targetUid = inspectedUserTarget!!.first
        val fallbackName = inspectedUserTarget!!.second
        val targetUserPosts = remember(feedPostsState, targetUid) {
            ((feedPostsState as? UiState.Success)?.data ?: emptyList())
                .filter { it.authorId == targetUid }
        }
        PublicUserProfileDialog(
            targetUserId = targetUid,
            fallbackName = fallbackName,
            currentUserId = viewModel.currentUserId,
            inspectedProfileState = inspectedUserProfileState,
            userPublishedPosts = targetUserPosts,
            onDismiss = { viewModel.closeUserProfileInspection() },
            onSendMessageClick = { uid, name ->
                viewModel.openDirectMessageTo(uid, name)
            },
            onPostClick = { post ->
                viewModel.closeUserProfileInspection()
                viewModel.openPostDetailDialog(post)
            }
        )
    }
}

@Composable
fun DrawerMenuItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String = "",
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        selected = selected,
        icon = { Icon(icon, contentDescription = label) },
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
    )
}

@Composable
fun QawmiBottomNavBar(
    currentDestination: MainDestination,
    unreadMessagesCount: Int = 0,
    onTabSelected: (MainDestination) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = QawmiMintBorder)
            .testTag("qawmi_bottom_nav_bar"),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "হোম",
                icon = Icons.Default.Home,
                selected = currentDestination == MainDestination.HOME,
                testTag = "bottom_nav_home",
                onClick = { onTabSelected(MainDestination.HOME) }
            )
            BottomNavItem(
                label = "ফিড",
                icon = Icons.AutoMirrored.Filled.Article,
                selected = currentDestination == MainDestination.FEED,
                testTag = "bottom_nav_feed",
                onClick = { onTabSelected(MainDestination.FEED) }
            )
            BottomNavItem(
                label = "আবেদন",
                icon = Icons.AutoMirrored.Filled.Assignment,
                selected = currentDestination == MainDestination.ABEDON,
                testTag = "bottom_nav_abedon",
                onClick = { onTabSelected(MainDestination.ABEDON) }
            )
            BottomNavItem(
                label = "ইনবক্স/পয়গাম",
                icon = Icons.Default.MailOutline,
                selected = currentDestination == MainDestination.MESSAGES,
                badgeCount = unreadMessagesCount,
                testTag = "bottom_nav_messages",
                onClick = { onTabSelected(MainDestination.MESSAGES) }
            )
            BottomNavItem(
                label = "প্রোফাইল",
                icon = Icons.Default.Person,
                selected = currentDestination == MainDestination.PROFILE,
                testTag = "bottom_nav_profile",
                onClick = { onTabSelected(MainDestination.PROFILE) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int = 0,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) QawmiMintSurface else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = QawmiDeepGreen,
                    modifier = Modifier.size(23.dp)
                )
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFC62828))
                            .padding(horizontal = 4.dp)
                            .testTag("${testTag}_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = IslamicDateAndPrayerHelper.toBanglaDigits(
                                if (badgeCount > 99) "99+" else badgeCount.toString()
                            ),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = QawmiTextDark,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ApplyToKhedmotDialog(
    post: FeedPost,
    initialProfile: UserProfile?,
    defaultName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, qual: String, exp: String, phone: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.fullName?.ifBlank { defaultName } ?: defaultName) }
    var qualification by remember { mutableStateOf(initialProfile?.qualification.orEmpty()) }
    var experience by remember { mutableStateOf(initialProfile?.experience.orEmpty()) }
    var phone by remember { mutableStateOf(initialProfile?.contactInfo.orEmpty()) }
    var note by remember { mutableStateOf("আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ, আমি উক্ত খেদমতের জন্য আগ্রহী।") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "খেদমতে আবেদন করুন: ${post.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QawmiDeepGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("পদ: ${post.designation}", style = MaterialTheme.typography.bodySmall, color = QawmiTextSecondary)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("আপনার পূর্ণ নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("ইলমি / শিক্ষাগত যোগ্যতা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    label = { Text("খেদমতের অভিজ্ঞতা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মোবাইল নম্বর *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("সংক্ষিপ্ত আরযি / বার্তা") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, qualification, experience, phone, note) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen)
            ) {
                Text("আবেদন প্রেরণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun PostDetailDialog(
    post: FeedPost,
    isOwnPost: Boolean,
    onDismiss: () -> Unit,
    onApplyClick: () -> Unit,
    onInspectAuthorProfileClick: () -> Unit,
    onSendMessageClick: () -> Unit
) {
    val isCircular = post.postType == "JOB_CIRCULAR"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatarImage(
                    photoUrl = post.authorPhotoUrl,
                    displayName = post.authorName.ifBlank { post.title },
                    size = 42.dp,
                    borderColor = QawmiGold,
                    borderWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = QawmiDeepGreen
                    )
                    if (post.authorName.isNotBlank()) {
                        Text(
                            text = "প্রকাশক: ${post.authorName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiTextSecondary
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isCircular) "ধরন: মাদরাসার নিয়োগ বিজ্ঞপ্তি" else "ধরন: খেদমতপ্রার্থীর আবেদন ও প্রোফাইল",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCircular) QawmiDeepGreen else QawmiSeekerBlue,
                    fontWeight = FontWeight.Bold
                )
                Text("অবস্থান / জেলা: ${post.upazilaOrLocation}, ${post.district}", style = MaterialTheme.typography.bodySmall)
                Text("খেদমতের পদ: ${post.designation}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("শিক্ষাগত যোগ্যতা: ${post.qualification}", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (isCircular) "হাদিয়া / বেতন: ${post.salaryOrExperience}" else "অভিজ্ঞতা: ${post.salaryOrExperience}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (isCircular) "আবেদনের শেষ তারিখ: ${post.deadlineOrPreferredDistricts}" else "পছন্দের জেলা: ${post.deadlineOrPreferredDistricts}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (post.contactNumber.isNotBlank()) {
                    Text("যোগাযোগ নম্বর: ${post.contactNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                if (post.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("বিস্তারিত বিবরণ:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(post.description, style = MaterialTheme.typography.bodySmall, color = QawmiTextSecondary)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isCircular && !isOwnPost) {
                    Button(
                        onClick = onApplyClick,
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen)
                    ) {
                        Text("আবেদন")
                    }
                }
                OutlinedButton(
                    onClick = onInspectAuthorProfileClick,
                    modifier = Modifier.testTag("post_detail_inspect_author_btn")
                ) {
                    Text("প্রোফাইল")
                }
                if (!isOwnPost && post.authorId.isNotBlank()) {
                    Button(
                        onClick = onSendMessageClick,
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiSeekerBlue),
                        modifier = Modifier.testTag("post_detail_send_message_btn")
                    ) {
                        Text("পয়গাম পাঠান")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@Composable
fun AppUpdatePromptOverlay(
    installedVersionCode: Int,
    installedVersionName: String,
    releaseInfo: AppReleaseInfo,
    onDismissLater: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember(releaseInfo.versionCode) { mutableStateOf(false) }
    var downloadProgress by remember(releaseInfo.versionCode) { mutableStateOf(0) }
    var statusMessage by remember(releaseInfo.versionCode) { mutableStateOf<String?>(null) }
    var errorMessage by remember(releaseInfo.versionCode) { mutableStateOf<String?>(null) }
    var awaitingUnknownSourcesPermission by remember(releaseInfo.versionCode) { mutableStateOf(false) }
    var verifiedReadyApkFile by remember(releaseInfo.versionCode) { mutableStateOf< java.io.File? >(null) }

    fun attemptInstallVerifiedApk(apkFile: java.io.File) {
        val installResult = AppUpdateManagerHelper.launchApkInstallation(
            context = context,
            apkFile = apkFile,
            releaseInfo = releaseInfo,
            fallbackDownloadUrl = releaseInfo.apkDownloadUrl
        )
        installResult.onSuccess {
            awaitingUnknownSourcesPermission = false
            errorMessage = null
            statusMessage = "APK যাচাই ও ডাউনলোড সম্পন্ন হয়েছে। Android ইনস্টলার চালু করা হয়েছে।"
        }.onFailure { err ->
            if (err is com.example.ui.util.UnknownSourcesPermissionRequiredException) {
                awaitingUnknownSourcesPermission = true
                verifiedReadyApkFile = apkFile
                statusMessage = "ইনস্টল অনুমতির অপেক্ষায়..."
                errorMessage = err.message
            } else {
                awaitingUnknownSourcesPermission = false
                errorMessage = err.localizedMessage
                    ?: "ইনস্টলার চালু করা যায়নি। পুনরায় চেষ্টা করুন।"
            }
        }
    }

    fun startUpdateDownloadAndInstall() {
        if (isDownloading || AppUpdateManagerHelper.isDownloadInProgress()) return
        isDownloading = true
        errorMessage = null
        awaitingUnknownSourcesPermission = false
        statusMessage = "APK ডাউনলোড ও নিরাপত্তা যাচাই শুরু হচ্ছে..."
        scope.launch {
            val downloadResult = AppUpdateManagerHelper.downloadApkWithProgress(
                context = context,
                releaseInfo = releaseInfo,
                onProgress = { pct ->
                    downloadProgress = pct
                    statusMessage = "APK ডাউনলোড হচ্ছে... $pct%"
                }
            )
            isDownloading = false
            downloadResult.onSuccess { apkFile ->
                downloadProgress = 100
                verifiedReadyApkFile = apkFile
                attemptInstallVerifiedApk(apkFile)
            }.onFailure { err ->
                verifiedReadyApkFile = null
                errorMessage = err.localizedMessage
                    ?: "APK ডাউনলোড ব্যর্থ হয়েছে। ইন্টারনেট সংযোগ যাচাই করে পুনরায় চেষ্টা করুন।"
            }
        }
    }

    // Automatically resume APK installation when the user returns from "Install unknown apps" settings
    DisposableEffect(lifecycleOwner, awaitingUnknownSourcesPermission, verifiedReadyApkFile) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                awaitingUnknownSourcesPermission &&
                verifiedReadyApkFile != null &&
                AppUpdateManagerHelper.canRequestPackageInstalls(context)
            ) {
                attemptInstallVerifiedApk(verifiedReadyApkFile!!)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .padding(20.dp)
            .testTag("update_prompt_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, QawmiGold, RoundedCornerShape(20.dp))
                .testTag("update_prompt_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(QawmiDarkGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        IslamicStarEmblem(size = 24.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "কওমি কর্মসংস্থান বাংলাদেশ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = QawmiSeekerBlue,
                            modifier = Modifier.testTag("update_prompt_app_name")
                        )
                        Text(
                            text = "নতুন আপডেট উপলব্ধ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = QawmiDeepGreen,
                            modifier = Modifier.testTag("update_prompt_title")
                        )
                        Text(
                            text = "নতুন আপডেট এসেছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary,
                            modifier = Modifier.testTag("update_prompt_subtitle")
                        )
                    }
                }

                if (releaseInfo.isMandatory) {
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, QawmiGold, RoundedCornerShape(10.dp))
                            .testTag("update_mandatory_notice")
                    ) {
                        Text(
                            text = "জরুরি ও বাধ্যতামূলক আপডেট: অ্যাপের নিরাপত্তা, ডেটা সুরক্ষা ও গুরুত্বপূর্ণ সেবা সচল রাখতে এই সংস্করণে আপডেট করা আবশ্যক। আপডেট করলে আপনার লগইন ও সকল তথ্য সম্পূর্ণ অক্ষুণ্ন থাকবে।",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = QawmiTextDark,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Surface(
                    color = QawmiMintSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiMintBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "বর্তমান সংস্করণ: $installedVersionName (কোড $installedVersionCode)",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary,
                            modifier = Modifier.testTag("update_prompt_installed_version_text")
                        )
                        Text(
                            text = "নতুন সংস্করণ: ${releaseInfo.versionName} (কোড ${releaseInfo.versionCode})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = QawmiDeepGreen,
                            modifier = Modifier.testTag("update_prompt_version_text")
                        )
                        if (releaseInfo.githubReleaseTag.isNotBlank()) {
                            Text(
                                text = "রিলিজ ট্যাগ: ${releaseInfo.githubReleaseTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiSeekerBlue
                            )
                        }
                        if (releaseInfo.apkSha256.isNotBlank()) {
                            Text(
                                text = "নিরাপত্তা চেকসাম (SHA-256): ${releaseInfo.apkSha256.take(16)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiDeepGreen,
                                modifier = Modifier.testTag("update_prompt_sha256_text")
                            )
                        }
                    }
                }

                if (releaseInfo.releaseNotes.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "নতুন আপডেটে যা থাকছে:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = QawmiTextDark
                        )
                        Text(
                            text = releaseInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary,
                            modifier = Modifier.testTag("update_prompt_release_notes")
                        )
                    }
                }

                if (isDownloading || downloadProgress > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (downloadProgress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag("update_download_progress"),
                            color = QawmiDeepGreen,
                            trackColor = QawmiMintSurface
                        )
                        Text(
                            text = statusMessage ?: "APK ডাউনলোড হচ্ছে... $downloadProgress%",
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("update_install_status")
                        )
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFC62828),
                                modifier = Modifier.testTag("update_error_message")
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (awaitingUnknownSourcesPermission && verifiedReadyApkFile != null) {
                                    OutlinedButton(
                                        onClick = {
                                            if (AppUpdateManagerHelper.canRequestPackageInstalls(context)) {
                                                attemptInstallVerifiedApk(verifiedReadyApkFile!!)
                                            } else {
                                                AppUpdateManagerHelper.openUnknownSourcesSettings(context)
                                            }
                                        },
                                        modifier = Modifier.testTag("update_grant_permission_button")
                                    ) {
                                        Text(
                                            text = "অনুমতি সেটিংস / এখন ইনস্টল করুন",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = QawmiDeepGreen
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { startUpdateDownloadAndInstall() },
                                    enabled = !isDownloading,
                                    modifier = Modifier.testTag("update_retry_button")
                                ) {
                                    Text(
                                        text = "পুনরায় চেষ্টা করুন",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        AppUpdateManagerHelper.openExternalApkDownloadWindow(
                                            context = context,
                                            apkDownloadUrl = releaseInfo.apkDownloadUrl
                                        )
                                    },
                                    modifier = Modifier.testTag("update_open_external_window_button")
                                ) {
                                    Text(
                                        text = "আলাদা উইন্ডোতে ডাউনলোড",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = QawmiDeepGreen
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissLater,
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("update_later_button")
                    ) {
                        Text(
                            text = "পরে করুন",
                            fontWeight = FontWeight.Bold,
                            color = QawmiTextDark
                        )
                    }

                    Button(
                        onClick = { startUpdateDownloadAndInstall() },
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("update_now_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isDownloading) "ডাউনলোড হচ্ছে..." else "আপডেট নাও",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (!isDownloading) {
                                Text(
                                    text = "আপডেট নিন",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

