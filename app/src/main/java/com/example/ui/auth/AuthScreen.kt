package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ensureFirebaseInitialized
import com.example.ui.components.IslamicStarEmblem
import com.example.ui.components.MosqueHeroIllustration
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSoftBg
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.AdminSecurityHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val FALLBACK_WEB_CLIENT_ID =
    "629586749547-ubgo99u8c72n63t1pujt7dlof51mqn5e.apps.googleusercontent.com"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun resolveWebClientId(context: Context): String {
    val configured = runCatching {
        context.getString(R.string.default_web_client_id).trim()
    }.getOrDefault("")
    return if (configured.isNotBlank() && configured != "unconfigured_web_client_id") {
        configured
    } else {
        FALLBACK_WEB_CLIENT_ID
    }
}

private suspend fun signInFirebaseWithCredential(credential: Credential): Boolean {
    val googleIdToken = when {
        credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        credential is GoogleIdTokenCredential ->
            credential.idToken
        else -> null
    } ?: return false

    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
    Firebase.auth.signInWithCredential(authCredential).await()
    return Firebase.auth.currentUser != null
}

private suspend fun signInWithFirebaseWebOAuth(activity: Activity): Boolean {
    val pending = Firebase.auth.pendingAuthResult
    if (pending != null) {
        val result = pending.await()
        if (result?.user != null || Firebase.auth.currentUser != null) {
            return true
        }
    }
    val provider = OAuthProvider.newBuilder("google.com")
        .addCustomParameter("prompt", "select_account")
        .build()
    val authResult = Firebase.auth.startActivityForSignInWithProvider(activity, provider).await()
    return authResult?.user != null || Firebase.auth.currentUser != null
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    try {
        ensureFirebaseInitialized(context)
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
    } catch (t: Throwable) {
        Log.w("Auth", "Firebase.auth not ready during attemptAutoSignIn: ${t.message}")
        onUnauthenticated()
        return
    }
    if (credentialManager == null) {
        onUnauthenticated()
        return
    }
    val activity = context.findActivity()
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
        onUnauthenticated()
        return
    }

    val clientId = resolveWebClientId(context)
    if (clientId.isBlank()) {
        onUnauthenticated()
        return
    }

    val request = try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    } catch (t: Throwable) {
        Log.w("Auth", "Failed to build GetGoogleIdOption: ${t.message}")
        onUnauthenticated()
        return
    }

    scope.launch {
        try {
            if (activity.isFinishing || activity.isDestroyed) {
                onUnauthenticated()
                return@launch
            }
            val result = credentialManager.getCredential(activity, request)
            if (signInFirebaseWithCredential(result.credential)) {
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (t: Throwable) {
            Log.d("Auth", "Auto sign-in skipped or unavailable: ${t.message}")
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    ensureFirebaseInitialized(context)
    runCatching {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
    }

    val clientId = resolveWebClientId(context)
    val activity = context.findActivity()

    scope.launch {
        try {
            val hostContext: Context = activity ?: context

            if (credentialManager != null) {
                // Attempt 1: Explicit Sign in with Google bottom sheet / dialog option
                val signInOptionResult = runCatching {
                    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInOption)
                        .build()
                    val startMs = System.currentTimeMillis()
                    try {
                        credentialManager.getCredential(hostContext, request)
                    } catch (cancelEx: GetCredentialCancellationException) {
                        val elapsedMs = System.currentTimeMillis() - startMs
                        val msg = cancelEx.message.orEmpty()
                        val isSystemAbort = elapsedMs < 550L ||
                            msg.contains("10:") ||
                            msg.contains("16:") ||
                            msg.contains("Developer", ignoreCase = true) ||
                            msg.contains("console", ignoreCase = true) ||
                            msg.contains("configuration", ignoreCase = true)
                        if (!isSystemAbort) {
                            throw cancelEx
                        }
                        throw IllegalStateException("Credential sheet aborted by system: $msg", cancelEx)
                    }
                }

                if (signInOptionResult.isSuccess) {
                    val credential = signInOptionResult.getOrThrow().credential
                    if (signInFirebaseWithCredential(credential)) {
                        onAuthSuccess()
                        return@launch
                    }
                } else {
                    val ex = signInOptionResult.exceptionOrNull()
                    if (ex is GetCredentialCancellationException) {
                        Log.w("Auth", "Google Sign-In cancelled by user: ${ex.message}")
                        onAuthCancelled()
                        return@launch
                    }
                    Log.w("Auth", "GetSignInWithGoogleOption failed, trying GetGoogleIdOption fallback: ${ex?.message}")
                }

                // Attempt 2: GetGoogleIdOption with filterByAuthorizedAccounts = false
                val googleIdResult = runCatching {
                    runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val startMs = System.currentTimeMillis()
                    try {
                        credentialManager.getCredential(hostContext, request)
                    } catch (cancelEx: GetCredentialCancellationException) {
                        val elapsedMs = System.currentTimeMillis() - startMs
                        if (elapsedMs >= 550L) {
                            throw cancelEx
                        }
                        throw IllegalStateException("GoogleId sheet aborted by system", cancelEx)
                    }
                }

                if (googleIdResult.isSuccess) {
                    val credential = googleIdResult.getOrThrow().credential
                    if (signInFirebaseWithCredential(credential)) {
                        onAuthSuccess()
                        return@launch
                    }
                } else {
                    val ex = googleIdResult.exceptionOrNull()
                    if (ex is GetCredentialCancellationException) {
                        Log.w("Auth", "Google ID Sign-In cancelled by user: ${ex.message}")
                        onAuthCancelled()
                        return@launch
                    }
                    Log.w("Auth", "GetGoogleIdOption failed, trying Firebase OAuthProvider fallback: ${ex?.message}")
                }
            }

            // Attempt 3: Firebase OAuthProvider ("google.com") fallback (works even without Play Services CredentialManager or on custom-signed APKs)
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                val webSuccess = signInWithFirebaseWebOAuth(activity)
                if (webSuccess) {
                    onAuthSuccess()
                    return@launch
                }
            }

            onAuthError("গুগল সাইন-ইন সম্পন্ন করা যায়নি। অনুগ্রহ করে ইন্টারনেট সংযোগ ও ফোনে Google অ্যাকাউন্ট লগইন আছে কি না পরীক্ষা করুন।")
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (t: Throwable) {
            val msg = t.message.orEmpty()
            if (msg.contains("canceled", ignoreCase = true) || msg.contains("cancelled", ignoreCase = true)) {
                Log.w("Auth", "OAuth Sign-In cancelled by user: $msg")
                onAuthCancelled()
            } else {
                Log.e("Auth", "Google Sign-In failed", t)
                onAuthError("গুগল দিয়ে সাইন-ইন করতে সমস্যা হয়েছে: ${t.localizedMessage ?: "অনুগ্রহ করে পুনরায় চেষ্টা করুন"}")
            }
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager?,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    try {
        Firebase.auth.signOut()
    } catch (t: Throwable) {
        Log.e("Auth", "Firebase signOut error", t)
    }
    scope.launch {
        try {
            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
        } catch (t: Throwable) {
            Log.e("Auth", "Failed to clear credential state", t)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        ensureFirebaseInitialized(context)
        runCatching {
            if (Firebase.auth.currentUser != null) {
                onAuthSuccess()
                return@LaunchedEffect
            }
            val pending = Firebase.auth.pendingAuthResult
            if (pending != null) {
                isLoading = true
                val res = pending.await()
                isLoading = false
                if (res?.user != null || Firebase.auth.currentUser != null) {
                    onAuthSuccess()
                }
            }
        }.onFailure {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QawmiSoftBg)
    ) {
        // Top Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(QawmiDarkGreen, QawmiDeepGreen)
                    )
                )
                .statusBarsPadding()
                .padding(vertical = 22.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IslamicStarEmblem(size = 24.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "কওমি কর্মসংস্থান",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    IslamicStarEmblem(size = 24.dp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "বাংলাদেশের কওমি মাদরাসা খেদমতের বিশ্বস্ত মাধ্যম",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QawmiGold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MosqueHeroIllustration(
                        modifier = Modifier
                            .size(width = 100.dp, height = 84.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ",
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "উস্তায ও মুদাররিস খুঁজুন, খেদমতের বিজ্ঞপ্তি দিন এবং দ্বীনি খেদমত এগিয়ে নিন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary
                        )
                    }
                }
            }

            // Features & Google Sign-In Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = QawmiDeepGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "নিরাপদ প্রবেশ",
                                tint = QawmiGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "দ্বীনি খেদমত প্ল্যাটফর্মে স্বাগতম",
                        style = MaterialTheme.typography.titleLarge,
                        color = QawmiTextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "খেদমতের আবেদন প্রকাশ, মাদরাসার নিয়োগ বিজ্ঞপ্তি, সরাসরি মুযাকারা (মেসেজিং) এবং যাচাইকৃত মাদরাসার তথ্য দেখতে আপনার গুগল অ্যাকাউন্ট দিয়ে প্রবেশ করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureBulletRow("নিজের ইলমি যোগ্যতা ও তাজরিবা (অভিজ্ঞতা) দিয়ে আবেদন প্রকাশ")
                    FeatureBulletRow("মাদরাসার খেদমতের বিজ্ঞপ্তি প্রকাশ ও আবেদন গ্রহণ")
                    FeatureBulletRow("মুহতামিম ও খেদমতপ্রার্থীদের মাঝে সরাসরি পয়গাম ও নোটিফিকেশন")

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (infoMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = QawmiMintSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = infoMessage!!,
                                color = QawmiDeepGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            infoMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QawmiDeepGreen,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("প্রবেশ করা হচ্ছে...")
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                tint = QawmiGold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google (গুগল দিয়ে প্রবেশ করুন)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Footer Quote
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = QawmiMintSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = QawmiDeepGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "“কওমি মাদরাসা খেদমতেই আখেরাতের সফলতার পথ”",
                        style = MaterialTheme.typography.labelLarge,
                        color = QawmiDeepGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBulletRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = QawmiDeepGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = QawmiTextDark
        )
    }
}
