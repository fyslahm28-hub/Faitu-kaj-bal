package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.IslamicStarEmblem
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextMuted
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.AdminSecurityHelper
import com.example.ui.util.AjkerSmoronHelper

@Composable
fun UserAppSettingsScreen(
    isOnlineVisible: Boolean = true,
    onToggleOnlineStatus: (Boolean) -> Unit = {},
    onShowFeedback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var jobCircularNotifications by rememberSaveable { mutableStateOf(true) }
    var paygamSoundNotifications by rememberSaveable { mutableStateOf(true) }
    var prayerWaqtReminder by rememberSaveable { mutableStateOf(true) }
    var ajkerSmoronNotification by rememberSaveable {
        mutableStateOf(AjkerSmoronHelper.isNotificationEnabled(context))
    }
    var onlineStatusSwitch by rememberSaveable { mutableStateOf(isOnlineVisible) }
    var showContactOnPosts by rememberSaveable { mutableStateOf(true) }
    var selectedFontScale by rememberSaveable { mutableStateOf("স্বাভাবিক") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_app_settings_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DrawerSectionHeaderCard(
                icon = Icons.Default.Settings,
                title = "অ্যাপ সেটিংস",
                subtitle = "নোটিফিকেশন, প্রাইভেসি, ফন্ট আকার এবং অ্যাপ প্রেফারেন্স নিয়ন্ত্রণ করুন"
            )
        }

        // 1. Notification Preferences
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = "নোটিফিকেশন সেটিংস",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "নতুন নিয়োগ বিজ্ঞপ্তি অ্যালার্ট",
                        subtitle = "নতুন খেদমত বা নিয়োগ বিজ্ঞপ্তি প্রকাশ হলে তাৎক্ষণিক জানান",
                        checked = jobCircularNotifications,
                        onCheckedChange = { jobCircularNotifications = it }
                    )
                    HorizontalDivider(color = QawmiMintSurface, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsToggleRow(
                        icon = Icons.Default.MailOutline,
                        title = "পয়গাম / ইনবক্স নোটিফিকেশন",
                        subtitle = "নতুন পয়গাম বা মেসেজ আসলে শব্দ ও ব্যাজ দেখান",
                        checked = paygamSoundNotifications,
                        onCheckedChange = { paygamSoundNotifications = it }
                    )
                    HorizontalDivider(color = QawmiMintSurface, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsToggleRow(
                        icon = Icons.Default.Schedule,
                        title = "নামাজের ওয়াক্ত রিমাইন্ডার",
                        subtitle = "দৈনিক ৫ ওয়াক্ত নামাজের সময়সূচী সতর্কতা",
                        checked = prayerWaqtReminder,
                        onCheckedChange = { prayerWaqtReminder = it }
                    )
                    HorizontalDivider(color = QawmiMintSurface, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsToggleRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "আজকের স্মরণ নোটিফিকেশন",
                        subtitle = "প্রতিদিন নির্ধারিত সময়ে সর্বোচ্চ ১টি ইসলামিক স্মরণবার্তা (Push Notification)",
                        checked = ajkerSmoronNotification,
                        onCheckedChange = { enabled ->
                            ajkerSmoronNotification = enabled
                            AjkerSmoronHelper.setNotificationEnabled(context, enabled)
                        }
                    )
                }
            }
        }

        // 2. Privacy & Presence Settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = "প্রাইভেসি ও অনলাইন স্ট্যাটাস",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Visibility,
                        title = "সক্রিয় (Online) স্ট্যাটাস দেখান",
                        subtitle = "ইনবক্স ও প্রোফাইলে আপনার অনলাইন উপস্থিতি প্রদর্শন করুন",
                        checked = onlineStatusSwitch,
                        onCheckedChange = {
                            onlineStatusSwitch = it
                            onToggleOnlineStatus(it)
                        }
                    )
                    HorizontalDivider(color = QawmiMintSurface, modifier = Modifier.padding(horizontal = 14.dp))
                    SettingsToggleRow(
                        icon = Icons.Default.Lock,
                        title = "আবেদনে যোগাযোগের নম্বর দৃশ্যমান রাখুন",
                        subtitle = "মাদরাসা কর্তৃপক্ষ যেন সরাসরি যোগাযোগ করতে পারেন",
                        checked = showContactOnPosts,
                        onCheckedChange = { showContactOnPosts = it }
                    )
                }
            }
        }

        // 3. Reading & Font Size Preference
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "লেখার আকার (ফন্ট সাইজ)",
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ছোট", "স্বাভাবিক", "বড়").forEach { option ->
                            val selected = selectedFontScale == option
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) QawmiDeepGreen else QawmiMintSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedFontScale = option }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White else QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Cache & Save Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onShowFeedback("আলহামদুলিল্লাহ, অ্যাপের সাময়িক ক্যাশ পরিষ্কার করা হয়েছে।")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = QawmiDeepGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ক্যাশ পরিষ্কার", color = QawmiDeepGreen, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onShowFeedback("আলহামদুলিল্লাহ, আপনার অ্যাপ সেটিংস সংরক্ষিত হয়েছে।")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("save_user_app_settings_button")
                ) {
                    Text("সেটিংস সংরক্ষণ", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                        tint = QawmiDeepGreen,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = QawmiTextSecondary,
                    fontSize = 11.5.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = QawmiDeepGreen
            )
        )
    }
}

@Composable
fun AboutAppScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_app_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DrawerSectionHeaderCard(
                icon = Icons.Default.Info,
                title = "অ্যাপ সম্পর্কে",
                subtitle = "কওমি কর্মসংস্থান বাংলাদেশ — দ্বীনি খেদমত ও যোগ্য উস্তাদ-মুহতামিম সেতুবন্ধন"
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "আমাদের লক্ষ্য ও উদ্দেশ্য",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "বাংলাদেশের কওমি মাদরাসাসমূহের শূন্য পদে যোগ্য আলেম, মুফতি, মুহাদ্দিস, ক্বারী ও হাফেজে কুরআন খুঁজে পাওয়া এবং খেদমতপ্রার্থী উস্তাদগণের জন্য উপযুক্ত দ্বীনি খেদমতের সুযোগ সহজলভ্য করার লক্ষ্যেই ‘কওমি কর্মসংস্থান বাংলাদেশ’ প্ল্যাটফর্মটি নির্মিত হয়েছে।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiTextDark,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "অ্যাপের মূল বৈশিষ্ট্যসমূহ",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    FeatureBulletRow("সারা বাংলাদেশের মাদরাসার নিয়োগ বিজ্ঞপ্তি প্রকাশ ও জেলাভিত্তিক অনুসন্ধান")
                    FeatureBulletRow("খেদমতপ্রার্থী উস্তাদগণের যোগ্যতা ও অভিজ্ঞতাসহ আবেদন প্রকাশ")
                    FeatureBulletRow("আধুনিক ও নিরাপদ পয়গাম (ইনবক্স ও গ্রুপ মেসেজিং) এবং এইচডি ছবি আদান-প্রদান")
                    FeatureBulletRow("যাচাইকৃত কওমি মাদরাসা ডিরেক্টরি ও প্রোফাইল")
                    FeatureBulletRow("বিভাগওয়ারী ৫ ওয়াক্ত নামাজের সময়সূচী, হিজরি তারিখ ও আজকের স্মরণ")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "অ্যাপ সংস্করণ (Version)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "১.০.০ (Production)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun AboutDeveloperScreen(
    isSubmitting: Boolean = false,
    onShowFeedback: (String) -> Unit = {},
    onSubmitDeveloperFeedback: (
        category: String,
        subject: String,
        message: String,
        contactPhone: String,
        onReset: () -> Unit
    ) -> Unit = { _, _, msg, _, onReset ->
        onReset()
        onShowFeedback("জাযাকাল্লাহু খাইরান! আপনার মূল্যবান পরামর্শ ডেভেলপার টিমের কাছে পাঠানো হয়েছে: $msg")
    }
) {
    var feedbackText by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_developer_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DrawerSectionHeaderCard(
                icon = Icons.Default.Code,
                title = "ডেভেলপার সম্পর্কে",
                subtitle = "কওমি কর্মসংস্থান বাংলাদেশ — কারিগরি উন্নয়ন ও সফটওয়্যার ইঞ্জিনিয়ারিং টিম"
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = QawmiDeepGreen,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = QawmiGold,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "কওমি কর্মসংস্থান কারিগরি টিম",
                                style = MaterialTheme.typography.titleMedium,
                                color = QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Android & Cloud Architecture Division",
                                style = MaterialTheme.typography.bodySmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "দ্বীনি শিক্ষাপ্রতিষ্ঠান ও উলামায়ে কেরামের খেদমতকে প্রযুক্তির মাধ্যমে আরও সহজ, নিরাপদ ও গতিশীল করতে আমাদের ডেভেলপার টিম নিরলসভাবে কাজ করে যাচ্ছে। অ্যাপটির প্রতিটি ফিচার কওমি অঙ্গনের প্রয়োজন ও আদবকে সর্বোচ্চ গুরুত্ব দিয়ে ডিজাইন করা হয়েছে।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiTextDark,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "কারিগরি কাঠামো ও নিরাপত্তা",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    FeatureBulletRow("Native Android (Kotlin ও Jetpack Compose Material 3)")
                    FeatureBulletRow("রিয়েল-টাইম ক্লাউড সিঙ্ক ও এনক্রিপ্টেড ডাটাবেস সুরক্ষা")
                    FeatureBulletRow("দ্রুতগতির পয়গাম মেসেজিং ও অপ্টিমাইজড এইচডি ফটো রেন্ডারিং")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ডেভেলপার টিমকে পরামর্শ বা মতামত পাঠান",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        placeholder = { Text("যোগাযোগের মোবাইল নম্বর (ঐচ্ছিক)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("developer_feedback_phone_input")
                    )
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("অ্যাপের মানোন্নয়নে আপনার মূল্যবান পরামর্শ লিখুন...") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("developer_feedback_message_input")
                    )
                    Button(
                        onClick = {
                            if (feedbackText.isNotBlank()) {
                                onSubmitDeveloperFeedback(
                                    "পরামর্শ",
                                    "ডেভেলপার টিমের প্রতি পরামর্শ",
                                    feedbackText.trim(),
                                    contactPhone.trim()
                                ) {
                                    feedbackText = ""
                                    contactPhone = ""
                                }
                            } else {
                                onShowFeedback("অনুগ্রহ করে আপনার পরামর্শ বা মতামত লিখুন।")
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("developer_feedback_submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSubmitting) "পাঠানো হচ্ছে..." else "পরামর্শ পাঠান",
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
fun PrivacyAndTermsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("privacy_and_terms_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DrawerSectionHeaderCard(
                icon = Icons.Default.Policy,
                title = "ব্যবহারের নীতিমালা ও গোপনীয়তা",
                subtitle = "কওমি কর্মসংস্থান বাংলাদেশ প্ল্যাটফর্মের শরয়ী আদব, শর্তাবলি ও তথ্যের সুরক্ষা নীতি"
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "১. বিজ্ঞপ্তি ও আবেদন প্রকাশের নীতিমালা",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    FeatureBulletRow("শুধুমাত্র প্রকৃত মাদরাসা বা দ্বীনি প্রতিষ্ঠানের নিয়োগ বিজ্ঞপ্তি এবং খেদমতপ্রার্থীদের সত্য তথ্যসমবলিত আবেদন প্রকাশ করা যাবে।")
                    FeatureBulletRow("কোনো ধরনের অসত্য তথ্য, প্রতারণামূলক প্রলোভন বা অপ্রাসঙ্গিক পোস্ট সম্পূর্ণ নিষিদ্ধ।")
                    FeatureBulletRow("পয়গাম / ইনবক্সে যোগাযোগের ক্ষেত্রে শরয়ী শালীনতা ও পারস্পরিক সম্মান বজায় রাখা আবশ্যক।")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "২. তথ্যের গোপনীয়তা ও সুরক্ষা",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    FeatureBulletRow("ব্যবহারকারীর ব্যক্তিগত পাসওয়ার্ড ও লগইন সেশন সম্পূর্ণ এনক্রিপ্টেড থাকে।")
                    FeatureBulletRow("ব্যক্তিগত পয়গাম বা চ্যাট শুধুমাত্র প্রেরক ও প্রাপকের মধ্যেই সীমাবদ্ধ থাকে।")
                    FeatureBulletRow("নিরাপত্তা লঙ্ঘন বা স্প্যাম প্রতিরোধে অ্যাডমিন প্যানেল যেকোনো অসঙ্গত পোস্ট বা অ্যাকাউন্ট নিয়ন্ত্রণ করার অধিকার রাখে।")
                }
            }
        }
    }
}

@Composable
fun HelpAndSupportScreen(
    defaultSenderPhone: String = "",
    isSubmitting: Boolean = false,
    onShowFeedback: (String) -> Unit = {},
    onSubmitSupportMessage: (
        category: String,
        subject: String,
        message: String,
        contactPhone: String,
        onReset: () -> Unit
    ) -> Unit = { category, subject, msg, _, onReset ->
        onReset()
        onShowFeedback("আলহামদুলিল্লাহ, আপনার $category ($subject) সার্ভারে সংরক্ষিত হয়েছে ও টেলিগ্রাম বটে পাঠানো হয়েছে: $msg")
    }
) {
    val context = LocalContext.current
    val supportCategories = remember {
        listOf("অভিযোগ", "সমস্যা", "পরামর্শ", "সহায়তা")
    }
    var selectedCategory by rememberSaveable { mutableStateOf("অভিযোগ") }
    var subjectText by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable(defaultSenderPhone) { mutableStateOf(defaultSenderPhone) }
    var messageText by rememberSaveable { mutableStateOf("") }
    var lastSubmittedConfirmation by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("help_and_support_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DrawerSectionHeaderCard(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "সহায়তা ও যোগাযোগ",
                subtitle = "বক্তব্য, অভিযোগ, কারিগরি সমস্যা ও পরামর্শ প্রেরণ এবং সরাসরি সাপোর্ট হেল্পলাইন"
            )
        }

        // ১. সহায়তা, অভিযোগ, সমস্যা বা পরামর্শ পাঠানোর ফর্ম (Firebase + Admin Panel + Telegram Bot)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp))
                    .testTag("help_support_submission_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "বক্তব্য, অভিযোগ, সমস্যা বা পরামর্শ পাঠান",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "আপনার বার্তা সরাসরি সার্ভারে সংরক্ষিত হবে, অ্যাডমিন প্যানেলে প্রদর্শিত হবে এবং তাৎক্ষণিকভাবে অ্যাডমিন টেলিগ্রাম বটে পৌঁছে যাবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary,
                        lineHeight = 18.sp
                    )

                    // Category selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        supportCategories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedCategory = cat }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) QawmiDeepGreen else QawmiMintBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .testTag("help_support_category_$cat"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) QawmiDeepGreen else QawmiMintSurface
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = subjectText,
                        onValueChange = { subjectText = it },
                        label = { Text("বিষয় / শিরোনাম") },
                        placeholder = { Text("সংক্ষেপে বিষয় লিখুন (যেমন: বিজ্ঞপ্তি বা অ্যাকাউন্ট সংক্রান্ত)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("help_support_subject_input")
                    )

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("যোগাযোগের মোবাইল নম্বর") },
                        placeholder = { Text("০১৭XXXXXXXX") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("help_support_phone_input")
                    )

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("আপনার বক্তব্য / অভিযোগ / পরামর্শ বিস্তারিত *") },
                        placeholder = { Text("এখানে বিস্তারিত লিখুন...") },
                        minLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("help_support_message_input")
                    )

                    if (lastSubmittedConfirmation.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                                .testTag("help_support_confirmation_banner"),
                            shape = RoundedCornerShape(10.dp),
                            color = QawmiMintSurface
                        ) {
                            Text(
                                text = lastSubmittedConfirmation,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (messageText.trim().isBlank()) {
                                onShowFeedback("অনুগ্রহ করে আপনার বক্তব্য, অভিযোগ, সমস্যা বা পরামর্শ বিস্তারিত লিখুন।")
                            } else {
                                val resolvedSubject = subjectText.trim().ifBlank { "$selectedCategory সংক্রান্ত বার্তা" }
                                onSubmitSupportMessage(
                                    selectedCategory,
                                    resolvedSubject,
                                    messageText.trim(),
                                    contactPhone.trim()
                                ) {
                                    val tgConfigured = com.example.ui.util.TelegramNotificationHelper.isTelegramBotConfigured()
                                    lastSubmittedConfirmation = if (tgConfigured) {
                                        "আলহামদুলিল্লাহ! আপনার '$selectedCategory' বার্তাটি সার্ভারে সংরক্ষিত হয়েছে, অ্যাডমিন প্যানেলে যুক্ত হয়েছে এবং টেলিগ্রাম বটে পাঠানো হয়েছে।"
                                    } else {
                                        "আলহামদুলিল্লাহ! আপনার '$selectedCategory' বার্তাটি সার্ভারে সংরক্ষিত হয়েছে ও অ্যাডমিন প্যানেলে যুক্ত হয়েছে (টেলিগ্রাম বট ডেলিভারি স্ট্যাটাস অ্যাডমিন লগে সংরক্ষিত)।"
                                    }
                                    subjectText = ""
                                    messageText = ""
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("help_support_submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSubmitting) "পাঠানো হচ্ছে..." else "বার্তা পাঠান",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "সচরাচর জিজ্ঞাসিত প্রশ্নোত্তর",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    FaqItemBlock(
                        question = "কীভাবে মাদরাসার নিয়োগ বিজ্ঞপ্তি প্রকাশ করবো?",
                        answer = "হোমপেজের ‘বিজ্ঞপ্তি দিন’ কার্ডে ট্যাপ করে মাদরাসার নাম, পদবি, যোগ্যতা, হাদিয়া/বেতন ও যোগাযোগের নম্বর লিখে সহজেই প্রকাশ করতে পারবেন।"
                    )
                    HorizontalDivider(color = QawmiMintSurface)
                    FaqItemBlock(
                        question = "কীভাবে খেদমতের জন্য আবেদন করবো?",
                        answer = "যেকোনো নিয়োগ বিজ্ঞপ্তির নিচে ‘আবেদন করুন’ বাটনে ট্যাপ করে অথবা হোমপেজের ‘আবেদন প্রকাশ করুন’ অপশন থেকে আপনার সিভি/যোগ্যতা প্রকাশ করতে পারবেন।"
                    )
                    HorizontalDivider(color = QawmiMintSurface)
                    FaqItemBlock(
                        question = "উস্তাদ বা মুহতামিমের সাথে সরাসরি কথা বলবো কীভাবে?",
                        answer = "যেকোনো পোস্টের প্রোফাইলে ট্যাপ করে অথবা নিচের বারের ‘ইনবক্স/পয়গাম’ অপশন থেকে সরাসরি মেসেজ ও ফটো পাঠাতে পারবেন।"
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সরাসরি হেল্পলাইন সাপোর্ট",
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "যেকোনো কারিগরি সহায়তা বা মাদরাসা ভেরিফিকেশনের জন্য আমাদের সাপোর্ট নম্বরে যোগাযোগ করুন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                    Button(
                        onClick = {
                            try {
                                val dialIntent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:${AdminSecurityHelper.ADMIN_MOBILE_NUMBER}")
                                )
                                context.startActivity(dialIntent)
                            } catch (_: Exception) {
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "হেল্পলাইনে কল করুন (${AdminSecurityHelper.ADMIN_MOBILE_NUMBER})",
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
private fun DrawerSectionHeaderCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QawmiDarkGreen)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF165B48),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = QawmiGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IslamicStarEmblem(size = 15.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFDDF3EC),
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureBulletRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = QawmiDeepGreen,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = QawmiTextDark,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun FaqItemBlock(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "• $question",
            style = MaterialTheme.typography.bodyMedium,
            color = QawmiTextDark,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodySmall,
            color = QawmiTextSecondary,
            lineHeight = 18.sp
        )
    }
}
