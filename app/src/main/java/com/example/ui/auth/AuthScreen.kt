package com.example.ui.auth

import android.app.Activity
import android.content.Context
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Login
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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
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
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In কনফিগারেশন পাওয়া যায়নি (default_web_client_id অনুপস্থিত)")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("অপ্রত্যাশিত ক্রেডেনশিয়াল ধরন")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "গুগল সাইন-ইন সম্পন্ন করা যায়নি")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
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
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Admin Password Recovery section state
    var showAdminRecovery by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
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
                                imageVector = Icons.Default.Login,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Admin Security & Password Recovery Toggle
                    OutlinedButton(
                        onClick = { showAdminRecovery = !showAdminRecovery },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_password_reset_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showAdminRecovery) "অ্যাডমিন পুনরুদ্ধার প্যানেল বন্ধ করুন" else "অ্যাডমিন অ্যাক্সেস ও পাসওয়ার্ড পুনরুদ্ধার",
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showAdminRecovery) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = QawmiSoftBg)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "অ্যাডমিন পাসওয়ার্ড পুনরুদ্ধার (Firebase Auth Reset Link)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "অনুমোদিত অ্যাডমিন অ্যাকাউন্ট দিয়ে যাচাইকৃত লগইন করলে স্বয়ংক্রিয়ভাবে অ্যাডমিন প্যানেল সক্রিয় হবে। পাসওয়ার্ড ভুলে গেলে নিবন্ধিত অ্যাডমিন ইমেইলে নিরাপদ Password Reset Link পাঠান:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QawmiTextSecondary
                                )

                                // Quick chips for the 3 authorized admin emails
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AdminSecurityHelper.AUTHORIZED_ADMIN_EMAILS.forEach { adminMail ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (recoveryEmail.equals(adminMail, ignoreCase = true)) QawmiMintSurface else Color.White,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.dp,
                                                    if (recoveryEmail.equals(adminMail, ignoreCase = true)) QawmiDeepGreen else QawmiMintBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { recoveryEmail = adminMail }
                                        ) {
                                            Text(
                                                text = "অনুমোদিত ইমেইল: $adminMail",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = recoveryEmail,
                                    onValueChange = { recoveryEmail = it },
                                    label = { Text("নিবন্ধিত অ্যাডমিন ইমেইল লিখুন") },
                                    placeholder = { Text("foysalahamad102@gmail.com") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_reset_email_input")
                                )

                                Button(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        val normalized = AdminSecurityHelper.normalizeEmail(recoveryEmail)
                                        if (!AdminSecurityHelper.isAuthorizedAdminEmail(normalized)) {
                                            errorMessage = "শুধুমাত্র অনুমোদিত অ্যাডমিন ইমেইলে (foysalahamad102@gmail.com, foysalahamad204@gmail.com, fyslahm28@gmail.com) পাসওয়ার্ড রিসেট লিংক পাঠানো যাবে। শুধু মোবাইল নম্বর বা অন্য ইমেইল গ্রহণযোগ্য নয়।"
                                        } else {
                                            isSendingReset = true
                                            scope.launch {
                                                try {
                                                    Firebase.auth.sendPasswordResetEmail(normalized).await()
                                                    infoMessage = "আলহামদুলিল্লাহ, নিবন্ধিত অ্যাডমিন ইমেইল ($normalized)-এ Firebase Authentication থেকে নিরাপদ Password Reset Link পাঠানো হয়েছে।"
                                                } catch (e: Exception) {
                                                    errorMessage = e.localizedMessage
                                                        ?: "পাসওয়ার্ড রিসেট লিংক পাঠাতে ত্রুটি হয়েছে।"
                                                } finally {
                                                    isSendingReset = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSendingReset,
                                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("send_admin_reset_email_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockReset,
                                        contentDescription = null,
                                        tint = QawmiGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSendingReset) "লিংক পাঠানো হচ্ছে..." else "Password Reset Link পাঠান",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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
