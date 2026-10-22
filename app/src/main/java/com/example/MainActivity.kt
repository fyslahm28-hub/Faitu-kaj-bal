package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.QawmiRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MainAppScaffold
import com.example.ui.theme.ApplicationTheme
import com.example.ui.util.QawmiNotificationHelper
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.QawmiViewModel
import com.google.firebase.Firebase
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

data class MainNotificationDeepLink(
    val openNotifications: Boolean = false,
    val chatPartnerId: String = "",
    val chatPartnerName: String = "",
    val triggerTimestamp: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    private val _deepLinkState = MutableStateFlow<MainNotificationDeepLink?>(null)
    val deepLinkState: StateFlow<MainNotificationDeepLink?> = _deepLinkState.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        parseIntentDeepLink(intent)
        val openNotifications = intent?.getBooleanExtra(QawmiNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false) == true
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
        val partnerId = intent.getStringExtra(QawmiNotificationHelper.EXTRA_OPEN_CHAT_PARTNER_ID).orEmpty().trim()
        val partnerName = intent.getStringExtra(QawmiNotificationHelper.EXTRA_OPEN_CHAT_PARTNER_NAME).orEmpty().trim()
        val openNotifs = intent.getBooleanExtra(QawmiNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false)
        if (partnerId.isNotBlank() || openNotifs) {
            _deepLinkState.value = MainNotificationDeepLink(
                openNotifications = openNotifs,
                chatPartnerId = partnerId,
                chatPartnerName = partnerName.ifBlank { "সম্মানিত দ্বীনি ভাই" },
                triggerTimestamp = System.currentTimeMillis()
            )
            intent.removeExtra(QawmiNotificationHelper.EXTRA_OPEN_CHAT_PARTNER_ID)
            intent.removeExtra(QawmiNotificationHelper.EXTRA_OPEN_CHAT_PARTNER_NAME)
            intent.removeExtra(QawmiNotificationHelper.EXTRA_OPEN_NOTIFICATIONS)
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    openNotificationsInitially: Boolean = false,
    deepLink: MainNotificationDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                // AuthStateListener automatically updates currentUser
            }
        )
    } else {
        val currentUserId = user.uid
        val currentUserEmail = user.email.orEmpty()
        val currentUserDisplayName = user.displayName.orEmpty()

        val qawmiViewModel: QawmiViewModel = viewModel(
            key = currentUserId,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    val repository = QawmiRepository(db, auth)
                    QawmiViewModel(
                        repository = repository,
                        currentUserId = currentUserId,
                        currentUserEmail = currentUserEmail,
                        currentUserDisplayName = currentUserDisplayName
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
            if (target.chatPartnerId.isNotBlank()) {
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
                // AuthStateListener handles transition back to AuthScreen
            }
        )
    }
}
