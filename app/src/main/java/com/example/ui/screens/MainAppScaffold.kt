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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DomainAdd
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.FeedPost
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
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.util.QawmiNotificationHelper
import com.example.ui.viewmodel.FeedFilterTab
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.QawmiViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun MainAppScaffold(
    viewModel: QawmiViewModel,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
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
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Track real-time Online / Offline & Last Seen presence across app lifecycle
    DisposableEffect(lifecycleOwner, viewModel.currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    viewModel.setUserOnlineStatus(true)
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

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val selectedFeedFilter by viewModel.selectedFeedFilter.collectAsStateWithLifecycle()
    val selectedDistrict by viewModel.selectedDistrictFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val feedbackBanner by viewModel.feedbackBanner.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val feedPostsState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val madrasasState by viewModel.madrasasState.collectAsStateWithLifecycle()
    val mySentSubmissionsState by viewModel.mySentSubmissionsState.collectAsStateWithLifecycle()
    val receivedSubmissionsState by viewModel.receivedSubmissionsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val isAdminState by viewModel.isAdminState.collectAsStateWithLifecycle()
    val telegramNotificationsState by viewModel.telegramNotificationsState.collectAsStateWithLifecycle()

    val selectedPostForApply by viewModel.selectedPostForApply.collectAsStateWithLifecycle()
    val selectedPostForDetail by viewModel.selectedPostForDetail.collectAsStateWithLifecycle()
    val messageTargetUser by viewModel.messageTargetUser.collectAsStateWithLifecycle()
    val inspectedUserTarget by viewModel.inspectedUserTarget.collectAsStateWithLifecycle()
    val inspectedUserProfileState by viewModel.inspectedUserProfileState.collectAsStateWithLifecycle()
    val activeChatPartnerProfile by viewModel.activeChatPartnerProfileState.collectAsStateWithLifecycle()
    val chatPartnerProfiles by viewModel.chatPartnerProfilesState.collectAsStateWithLifecycle()

    val unreadNotificationsCount = remember(notificationsState) {
        (notificationsState as? UiState.Success)?.data?.count { !it.isRead } ?: 0
    }

    val unreadDirectMessagesCount = remember(messagesState, viewModel.currentUserId) {
        val list = (messagesState as? UiState.Success)?.data ?: emptyList()
        list.distinctBy { msg ->
            msg.id.ifBlank {
                "${msg.senderId}_${msg.receiverId}_${msg.createdAt?.seconds ?: 0}_${msg.createdAt?.nanoseconds ?: 0}_${msg.body}"
            }
        }.count {
            it.receiverId == viewModel.currentUserId && !it.isRead
        }
    }

    LaunchedEffect(notificationsState, viewModel.currentUserId) {
        val currentList = (notificationsState as? UiState.Success)?.data ?: return@LaunchedEffect
        QawmiNotificationHelper.deliverNewUnreadPushNotifications(
            context = context,
            currentUserId = viewModel.currentUserId,
            notifications = currentList
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
                            isOnline = currentProfile?.isOnline ?: true
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
                        label = "খেদমত (নিয়োগ বিজ্ঞপ্তি)",
                        icon = Icons.Default.Work,
                        selected = currentDestination == MainDestination.KHEDMOT,
                        testTag = "drawer_item_khedmot"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.setFeedFilter(FeedFilterTab.JOB_CIRCULAR)
                        viewModel.navigateTo(MainDestination.KHEDMOT)
                    }
                    DrawerMenuItem(
                        label = "মাদরাসা ডিরেক্টরি",
                        icon = Icons.Default.AccountBalance,
                        selected = currentDestination == MainDestination.MADRASA,
                        testTag = "drawer_item_madrasa"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.MADRASA)
                    }
                    DrawerMenuItem(
                        label = "যাচাইকৃত মাদরাসা",
                        icon = Icons.Default.VerifiedUser,
                        selected = currentDestination == MainDestination.VERIFIED_MADRASAS,
                        testTag = "drawer_item_verified_madrasas"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.VERIFIED_MADRASAS)
                    }
                    DrawerMenuItem(
                        label = "নিয়োগ বিজ্ঞপ্তি দিন",
                        icon = Icons.Default.Campaign,
                        selected = currentDestination == MainDestination.PUBLISH_CIRCULAR,
                        testTag = "drawer_item_publish_circular"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.PUBLISH_CIRCULAR)
                    }
                    DrawerMenuItem(
                        label = "আবেদন প্রকাশ করুন",
                        icon = Icons.Default.EditNote,
                        selected = currentDestination == MainDestination.PUBLISH_APPLICATION,
                        testTag = "drawer_item_publish_application"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.PUBLISH_APPLICATION)
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
                        label = "পছন্দের খেদমত (সংরক্ষিত)",
                        icon = Icons.Default.Bookmark,
                        selected = currentDestination == MainDestination.BOOKMARKS,
                        testTag = "drawer_item_bookmarks"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.BOOKMARKS)
                    }
                    DrawerMenuItem(
                        label = "নামাজের সময়সূচী",
                        icon = Icons.Default.Schedule,
                        selected = currentDestination == MainDestination.PRAYER_SCHEDULE,
                        testTag = "drawer_item_prayer_schedule"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.PRAYER_SCHEDULE)
                    }
                    DrawerMenuItem(
                        label = if (unreadNotificationsCount > 0) {
                            "নোটিফিকেশন (${IslamicDateAndPrayerHelper.toBanglaDigits(unreadNotificationsCount.toString())})"
                        } else {
                            "নোটিফিকেশন"
                        },
                        icon = Icons.Default.Notifications,
                        selected = currentDestination == MainDestination.NOTIFICATIONS,
                        testTag = "drawer_item_notifications"
                    ) {
                        scope.launch { drawerState.close() }
                        viewModel.navigateTo(MainDestination.NOTIFICATIONS)
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
                        scope.launch {
                            drawerState.close()
                            viewModel.setUserOnlineStatus(false)
                            viewModel.recordAdminLogoutBeforeSignOut()
                            signOutUser(
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignedOut,
                                scope = scope
                            )
                        }
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
            MainDestination.MESSAGES -> "ইনবক্স / পয়গাম"
            MainDestination.NOTIFICATIONS -> "নোটিফিকেশন"
            MainDestination.ADMIN_PANEL -> "অ্যাডমিন প্যানেল"
            MainDestination.PRAYER_SCHEDULE -> "নামাজের সময়সূচী"
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
                // Real feedback / error banners
                if (feedbackBanner != null || errorBanner != null) {
                    val isErr = errorBanner != null
                    Surface(
                        color = if (isErr) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                                onNavigate = { dest -> viewModel.navigateTo(dest) },
                                onOpenFeedFilter = { filterTab -> viewModel.openFeedWithFilter(filterTab) }
                            )
                        }

                        MainDestination.FEED -> {
                            FeedScreen(
                                feedPostsState = feedPostsState,
                                bookmarksState = bookmarksState,
                                selectedFilter = selectedFeedFilter,
                                selectedDistrict = selectedDistrict,
                                searchQuery = searchQuery,
                                currentUserId = viewModel.currentUserId,
                                isAdminUser = isAdminState,
                                onSelectFilter = { viewModel.setFeedFilter(it) },
                                onSelectDistrict = { viewModel.setDistrictFilter(it) },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onApplyClick = { post -> viewModel.openApplyDialog(post) },
                                onViewProfileOrDetailClick = { post -> viewModel.openPostDetailDialog(post) },
                                onToggleBookmark = { post, bookmarked -> viewModel.toggleBookmark(post, bookmarked) },
                                onToggleBoostPost = { post, boost -> viewModel.adminToggleBoostPost(post, boost) },
                                onDeletePost = { postId -> viewModel.deletePost(postId) },
                                onNavigate = { dest -> viewModel.navigateTo(dest) }
                            )
                        }

                        MainDestination.KHEDMOT -> {
                            FeedScreen(
                                feedPostsState = feedPostsState,
                                bookmarksState = bookmarksState,
                                selectedFilter = FeedFilterTab.JOB_CIRCULAR,
                                selectedDistrict = selectedDistrict,
                                searchQuery = searchQuery,
                                currentUserId = viewModel.currentUserId,
                                isAdminUser = isAdminState,
                                onSelectFilter = { viewModel.setFeedFilter(it) },
                                onSelectDistrict = { viewModel.setDistrictFilter(it) },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onApplyClick = { post -> viewModel.openApplyDialog(post) },
                                onViewProfileOrDetailClick = { post -> viewModel.openPostDetailDialog(post) },
                                onToggleBookmark = { post, bookmarked -> viewModel.toggleBookmark(post, bookmarked) },
                                onToggleBoostPost = { post, boost -> viewModel.adminToggleBoostPost(post, boost) },
                                onDeletePost = { postId -> viewModel.deletePost(postId) },
                                onNavigate = { dest -> viewModel.navigateTo(dest) }
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
                            MessagesScreen(
                                messagesState = messagesState,
                                currentUserId = viewModel.currentUserId,
                                currentUserProfile = (userProfileState as? UiState.Success)?.data,
                                targetUser = messageTargetUser,
                                partnerProfiles = chatPartnerProfiles,
                                activePartnerProfile = activeChatPartnerProfile,
                                isSubmitting = isSubmitting,
                                onSendMessage = { recId, recName, subj, body ->
                                    viewModel.sendMessage(recId, recName, subj, body)
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
                                onSaveConversationTheme = { partnerUid, themeSpec -> viewModel.saveConversationTheme(partnerUid, themeSpec) }
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
                            AdminPanelScreen(
                                isAdminUser = isAdminState,
                                adminEmail = viewModel.currentUserEmail,
                                madrasasState = madrasasState,
                                feedPostsState = feedPostsState,
                                telegramNotificationsState = telegramNotificationsState,
                                isSubmitting = isSubmitting,
                                onVerifyMadrasa = { madrasa, ver -> viewModel.adminVerifyMadrasa(madrasa, ver) },
                                onVerifyPost = { post, ver, status -> viewModel.adminVerifyPost(post, ver, status) },
                                onToggleBoostPost = { post, boost -> viewModel.adminToggleBoostPost(post, boost) },
                                onSendAnnouncement = { title, msg -> viewModel.publishAdminAnnouncement(title, msg) },
                                onRequestPasswordReset = { email -> viewModel.requestAdminPasswordReset(email) },
                                onSendTestTelegramAlert = { customMsg -> viewModel.sendTestTelegramNotification(customMsg) },
                                onDeletePost = { id -> viewModel.deletePost(id) }
                            )
                        }

                        MainDestination.PRAYER_SCHEDULE -> {
                            FullPrayerScheduleScreen()
                        }
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
                icon = Icons.Default.Article,
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
