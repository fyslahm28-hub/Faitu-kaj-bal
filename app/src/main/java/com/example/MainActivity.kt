package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import com.example.data.model.AppReleaseInfo
import com.example.data.repository.QawmiRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.AppUpdatePromptOverlay
import com.example.ui.screens.MainAppScaffold
import com.example.ui.screens.PrayerTimesAndAzanScreen
import com.example.ui.theme.ApplicationTheme
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiSoftBg
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.AppUpdateManagerHelper
import com.example.ui.util.PrayerAzanManager
import com.example.ui.util.QawmiNotificationHelper
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.QawmiViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch

data class MainNotificationDeepLink(
    val openNotifications: Boolean = false,
    val openAjkerSmoron: Boolean = false,
    val openPrayerSchedule: Boolean = false,
    val ajkerSmoronId: String = "",
    val chatPartnerId: String = "",
    val chatPartnerName: String = "",
    val triggerTimestamp: Long = System.currentTimeMillis()
)

private fun decodeB64(encoded: String): String {
    return String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT), Charsets.UTF_8)
}

fun ensureFirebaseInitialized(context: Context): Boolean {
    return try {
        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            true
        } else {
            val initialized = FirebaseApp.initializeApp(appContext)
            if (initialized != null) {
                true
            } else {
                val optionsFromRes = FirebaseOptions.fromResource(appContext)
                if (optionsFromRes != null) {
                    FirebaseApp.initializeApp(appContext, optionsFromRes)
                    true
                } else {
                    val fallbackOptions = FirebaseOptions.Builder()
                        .setProjectId(decodeB64("Z2VuLWxhbmctY2xpZW50LTAwNDgwNTk5NDQ="))
                        .setApplicationId(decodeB64("MTo2Mjk1ODY3NDk1NDc6YW5kcm9pZDo3MDAwZDQyN2Y4ZWY5NTJiMjkyOTJl"))
                        .setApiKey(decodeB64("QUl6YVN5QTc0MWQzbkZQamE1R2lwZ1E0b0EwU1duckN5c2ZBVlRr"))
                        .setGcmSenderId(decodeB64("NjI5NTg2NzQ5NTQ3"))
                        .setStorageBucket(decodeB64("Z2VuLWxhbmctY2xpZW50LTAwNDgwNTk5NDQuZmlyZWJhc2VzdG9yYWdlLmFwcA=="))
                        .build()
                    FirebaseApp.initializeApp(appContext, fallbackOptions)
                    true
                }
            }
        }
    } catch (t: Throwable) {
        Log.e("MainActivity", "Failed to initialize FirebaseApp safely", t)
        false
    }
}

class MainActivity : ComponentActivity() {
    private val _deepLinkState = MutableStateFlow<MainNotificationDeepLink?>(null)
    val deepLinkState: StateFlow<MainNotificationDeepLink?> = _deepLinkState.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            Log.w("MainActivity", "enableEdgeToEdge skipped: ${t.message}")
        }
        ensureFirebaseInitialized(this)
        runCatching { PrayerAzanManager.initWithContext(this) }
        parseIntentDeepLink(intent)
        val openNotifications = try {
            intent?.getBooleanExtra(QawmiNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false) == true
        } catch (_: Throwable) {
            false
        }
        setContent {
            val activeDeepLink by deepLinkState.collectAsStateWithLifecycle()
            ApplicationTheme {
                AppNavigation(
                    openNotificationsInitially = openNotifications,
                    deepLink = activeDeepLink,
                    onDeepLinkConsumed = { _deepLinkState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntentDeepLink(intent)
    }

    private fun parseIntentDeepLink(intent: Intent?) {
        if (intent == null) return
        try {
            val partnerId = intent.getStringExtra(QawmiNotificationHelper.EXTRA_OPEN_CHAT_PARTNER_ID).orEmpty().trim()
            val partnerName = intent.getStringExtra(QawmiNotificationHelper.EXTRA_OPEN_CHAT_PARTNER_NAME).orEmpty().trim()
            val openNotifs = intent.getBooleanExtra(QawmiNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false)
            val openSmoron = intent.getBooleanExtra(QawmiNotificationHelper.EXTRA_OPEN_AJKER_SMORON, false)
            val openPrayer = intent.getBooleanExtra(PrayerAzanManager.EXTRA_OPEN_PRAYER_SCREEN, false) ||
                intent.data?.host == "prayer_schedule"
            val smoronId = intent.getStringExtra(QawmiNotificationHelper.EXTRA_AJKER_SMORON_ID).orEmpty().trim()
            if (partnerId.isNotBlank() || openNotifs || openSmoron || openPrayer) {
                _deepLinkState.value = MainNotificationDeepLink(
                    openNotifications = openNotifs,
                    openAjkerSmoron = openSmoron,
                    openPrayerSchedule = openPrayer,
                    ajkerSmoronId = smoronId,
                    chatPartnerId = partnerId,
                    chatPartnerName = partnerName.ifBlank { "সম্মানিত দ্বীনি ভাই" },
                    triggerTimestamp = System.currentTimeMillis()
                )
                intent.removeExtra(QawmiNotificationHelper.EXTRA_OPEN_CHAT_PARTNER_ID)
                intent.removeExtra(QawmiNotificationHelper.EXTRA_OPEN_CHAT_PARTNER_NAME)
                intent.removeExtra(QawmiNotificationHelper.EXTRA_OPEN_NOTIFICATIONS)
                intent.removeExtra(QawmiNotificationHelper.EXTRA_OPEN_AJKER_SMORON)
                intent.removeExtra(QawmiNotificationHelper.EXTRA_AJKER_SMORON_ID)
                intent.removeExtra(PrayerAzanManager.EXTRA_OPEN_PRAYER_SCREEN)
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "Error parsing notification deep link intent: ${t.message}")
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    try {
        addAuthStateListener(listener)
    } catch (t: Throwable) {
        Log.e("MainActivity", "Failed to attach AuthStateListener", t)
        trySend(null)
    }
    awaitClose {
        try {
            removeAuthStateListener(listener)
        } catch (_: Throwable) {
        }
    }
}.catch { emit(null) }

@Composable
fun AppNavigation(
    auth: FirebaseAuth? = null,
    openNotificationsInitially: Boolean = false,
    deepLink: MainNotificationDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    var initAttempt by remember { mutableIntStateOf(0) }

    val resolvedAuth: FirebaseAuth? = remember(auth, initAttempt, context) {
        auth ?: runCatching {
            ensureFirebaseInitialized(context)
            Firebase.auth
        }.onFailure { t ->
            Log.e("MainActivity", "FirebaseAuth initialization failed", t)
        }.getOrNull()
    }

    if (resolvedAuth == null) {
        if (deepLink?.openPrayerSchedule == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QawmiSoftBg)
            ) {
                PrayerTimesAndAzanScreen()
            }
            return
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(QawmiSoftBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "কওমি কর্মসংস্থান বাংলাদেশ",
                        style = MaterialTheme.typography.titleLarge,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "অ্যাপ সার্ভিস চালু করতে সাময়িক সমস্যা হয়েছে। অনুগ্রহ করে ইন্টারনেট সংযোগ পরীক্ষা করে পুনরায় চেষ্টা করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { initAttempt++ },
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen)
                    ) {
                        Text("পুনরায় চেষ্টা করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    var manualAuthRefresh by remember { mutableIntStateOf(0) }
    var currentAuthUser by remember(resolvedAuth) {
        mutableStateOf(runCatching { resolvedAuth.currentUser }.getOrNull())
    }
    LaunchedEffect(resolvedAuth, manualAuthRefresh) {
        currentAuthUser = runCatching { resolvedAuth.currentUser }.getOrNull()
        resolvedAuth.authStateFlow().collect { latestUser ->
            currentAuthUser = latestUser
        }
    }
    val user = currentAuthUser

    if (user == null) {
        if (deepLink?.openPrayerSchedule == true) {
            androidx.activity.compose.BackHandler {
                onDeepLinkConsumed()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QawmiSoftBg)
            ) {
                PrayerTimesAndAzanScreen()
            }
            return
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        val unauthScope = rememberCoroutineScope()
        val installedVersion = remember(context) { AppUpdateManagerHelper.getInstalledVersion(context) }
        var unauthLatestRelease by remember { mutableStateOf<AppReleaseInfo?>(null) }
        var unauthDismissedVersionCode by remember(context) {
            mutableStateOf(AppUpdateManagerHelper.getActiveCooldownDismissedVersionCode(context))
        }

        LaunchedEffect(Unit) {
            unauthDismissedVersionCode = AppUpdateManagerHelper.getActiveCooldownDismissedVersionCode(context)
            val remote = AppUpdateManagerHelper.fetchLatestReleaseSafely(context)
            if (remote != null) {
                unauthLatestRelease = AppUpdateManagerHelper.pickMoreRecentRelease(unauthLatestRelease, remote)
            }
        }

        DisposableEffect(lifecycleOwner, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    currentAuthUser = runCatching { resolvedAuth.currentUser }.getOrNull()
                    manualAuthRefresh++
                    unauthDismissedVersionCode = AppUpdateManagerHelper.getActiveCooldownDismissedVersionCode(context)
                    val lastCheck = AppUpdateManagerHelper.getLastUpdateCheckTimestampMs(context)
                    if (unauthLatestRelease == null ||
                        System.currentTimeMillis() - lastCheck >= AppUpdateManagerHelper.PERIODIC_CHECK_INTERVAL_MS
                    ) {
                        unauthScope.launch {
                            val remote = AppUpdateManagerHelper.fetchLatestReleaseSafely(context)
                            if (remote != null) {
                                unauthLatestRelease = AppUpdateManagerHelper.pickMoreRecentRelease(unauthLatestRelease, remote)
                            }
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AuthScreen(
                onAuthSuccess = {
                    currentAuthUser = runCatching { resolvedAuth.currentUser }.getOrNull()
                    manualAuthRefresh++
                }
            )

            val shouldShowUnauthUpdate = remember(
                installedVersion,
                unauthLatestRelease,
                unauthDismissedVersionCode
            ) {
                AppUpdateManagerHelper.isUpdateAvailable(
                    installedVersionCode = installedVersion.versionCode,
                    installedVersionName = installedVersion.versionName,
                    remoteRelease = unauthLatestRelease,
                    dismissedVersionCode = unauthDismissedVersionCode
                )
            }
            if (shouldShowUnauthUpdate && unauthLatestRelease != null) {
                AppUpdatePromptOverlay(
                    installedVersionCode = installedVersion.versionCode,
                    installedVersionName = installedVersion.versionName,
                    releaseInfo = unauthLatestRelease!!,
                    onDismissLater = {
                        val ver = unauthLatestRelease!!.versionCode
                        AppUpdateManagerHelper.saveDismissedUpdatePrompt(context, ver)
                        unauthDismissedVersionCode = ver
                    }
                )
            }
        }
    } else {
        val currentUserId = user.uid
        val currentUserEmail = com.example.ui.util.AdminSecurityHelper.resolveEffectiveUserEmail(user)
        val currentUserPhone = com.example.ui.util.AdminSecurityHelper.resolveEffectiveUserPhone(user)
        val currentUserDisplayName = user.displayName.orEmpty()

        val qawmiViewModel: QawmiViewModel = viewModel(
            key = currentUserId,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    ensureFirebaseInitialized(app)
                    val databaseId = runCatching {
                        app.getString(R.string.firestore_database_id).trim()
                    }.getOrDefault("").let { id ->
                        if (id.isBlank() || id == "(default)") {
                            "ai-studio-android-811ea734-e9e8-4ff4-8e56-ca97ec8a429c"
                        } else {
                            id
                        }
                    }
                    val db = com.example.data.repository.getConfiguredFirestoreInstance(databaseId)
                    runCatching { db.enableNetwork() }
                    val repository = QawmiRepository(db, resolvedAuth)
                    QawmiViewModel(
                        repository = repository,
                        currentUserId = currentUserId,
                        currentUserEmail = currentUserEmail,
                        currentUserDisplayName = currentUserDisplayName,
                        currentUserPhone = currentUserPhone
                    )
                }
            }
        )

        LaunchedEffect(openNotificationsInitially, currentUserId) {
            if (openNotificationsInitially) {
                qawmiViewModel.navigateTo(MainDestination.NOTIFICATIONS)
            }
        }

        LaunchedEffect(deepLink, currentUserId) {
            val target = deepLink ?: return@LaunchedEffect
            if (target.openPrayerSchedule) {
                qawmiViewModel.navigateTo(MainDestination.PRAYER_SCHEDULE)
                onDeepLinkConsumed()
            } else if (target.openAjkerSmoron) {
                qawmiViewModel.openAjkerSmoronFromNotification(target.ajkerSmoronId)
                onDeepLinkConsumed()
            } else if (target.chatPartnerId.isNotBlank()) {
                qawmiViewModel.openDirectMessageTo(
                    userId = target.chatPartnerId,
                    userName = target.chatPartnerName
                )
                onDeepLinkConsumed()
            } else if (target.openNotifications) {
                qawmiViewModel.navigateTo(MainDestination.NOTIFICATIONS)
                onDeepLinkConsumed()
            }
        }

        MainAppScaffold(
            viewModel = qawmiViewModel,
            onSignedOut = {
                currentAuthUser = null
                manualAuthRefresh++
            }
        )
    }
}

