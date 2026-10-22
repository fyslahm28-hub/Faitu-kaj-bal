package com.example.ui.screens

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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.BookmarkItem
import com.example.data.model.DirectMessage
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotSubmission
import com.example.data.model.MadrasaProfile
import com.example.ui.components.MadrasaThumbIllustration
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSeekerBlue
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextMuted
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.theme.QawmiVerifiedGreen
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.UiState

import androidx.compose.material.icons.filled.LockReset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.TelegramNotificationLog
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.QawmiAlertRed
import com.example.ui.theme.QawmiSoftBg
import com.example.ui.util.AdminSecurityHelper
import com.example.ui.util.TelegramNotificationHelper
import com.google.firebase.Timestamp

@Composable
fun MadrasaDirectoryScreen(
    madrasasState: UiState<List<MadrasaProfile>>,
    onlyVerified: Boolean,
    currentUserId: String = "",
    onRegisterMadrasaClick: () -> Unit,
    onMessageOwnerClick: (String, String) -> Unit,
    onInspectOwnerProfile: (String, String) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(if (onlyVerified) "verified_madrasas_screen" else "madrasa_directory_screen")
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (onlyVerified) "যাচাইকৃত কওমি মাদরাসাসমূহ" else "কওমি মাদরাসা ডিরেক্টরি",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (onlyVerified) {
                            "কর্তৃপক্ষ কর্তৃক যাচাইকৃত নির্ভরযোগ্য মাদরাসাসমূহের তালিকা"
                        } else {
                            "সকল নিবন্ধিত মাদরাসার মারহালা, অবস্থান ও বিশেষ সুবিধা"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRegisterMadrasaClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_madrasa_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("মাদরাসা যুক্ত করুন", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        when (madrasasState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QawmiDeepGreen)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(madrasasState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val list = remember(madrasasState.data, onlyVerified) {
                    if (onlyVerified) madrasasState.data.filter { it.isVerified } else madrasasState.data
                }
                if (list.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (onlyVerified) Icons.Default.VerifiedUser else Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (onlyVerified) "এখনো কোনো যাচাইকৃত মাদরাসা তালিকাভুক্ত হয়নি" else "এখনো কোনো মাদরাসা নিবন্ধিত হয়নি",
                                style = MaterialTheme.typography.titleMedium,
                                color = QawmiTextDark,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "উপরের 'মাদরাসা যুক্ত করুন' বাটনে চাপ দিয়ে আপনার মাদরাসার তথ্য নিবন্ধন করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = QawmiTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list, key = { it.id }) { madrasa ->
                            val isOwnMadrasa = madrasa.ownerId == currentUserId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    MadrasaThumbIllustration()
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = madrasa.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = QawmiTextDark,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (madrasa.isVerified) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "যাচাইকৃত",
                                                    tint = QawmiVerifiedGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "অবস্থান: ${madrasa.address.ifBlank { madrasa.district }}, ${madrasa.district}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QawmiTextSecondary
                                        )
                                        if (madrasa.highestMarhala.isNotBlank()) {
                                            Text(
                                                text = "সর্বোচ্চ মারহালা: ${madrasa.highestMarhala}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = QawmiTextSecondary
                                            )
                                        }
                                        if (madrasa.facilities.isNotBlank()) {
                                            Text(
                                                text = "বিশেষ সুবিধা: ${madrasa.facilities}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = QawmiTextSecondary
                                            )
                                        }
                                        if (madrasa.studentCount.isNotBlank()) {
                                            Text(
                                                text = "তালিবে ইলম সংখ্যা: ${madrasa.studentCount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = QawmiTextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!isOwnMadrasa && madrasa.ownerId.isNotBlank()) {
                                                Button(
                                                    onClick = { onMessageOwnerClick(madrasa.ownerId, madrasa.name) },
                                                    shape = RoundedCornerShape(50),
                                                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("পয়গাম পাঠান", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if (madrasa.ownerId.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = { onInspectOwnerProfile(madrasa.ownerId, madrasa.name) },
                                                    shape = RoundedCornerShape(50),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("প্রোফাইল", style = MaterialTheme.typography.labelSmall, color = QawmiDeepGreen)
                                                }
                                            }
                                            if (madrasa.contactPhone.isNotBlank()) {
                                                Text(
                                                    text = "📞 ${madrasa.contactPhone}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = QawmiDeepGreen,
                                                    fontWeight = FontWeight.SemiBold
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
    }
}

@Composable
fun BookmarksScreen(
    bookmarksState: UiState<List<BookmarkItem>>,
    feedPostsState: UiState<List<FeedPost>>,
    onRemoveBookmark: (FeedPost) -> Unit,
    onApplyOrDetailClick: (FeedPost) -> Unit
) {
    val allPosts = (feedPostsState as? UiState.Success)?.data ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("bookmarks_screen")
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = QawmiDeepGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "পছন্দের খেদমত (সংরক্ষিত তালিকা)",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "আপনার সংরক্ষিত নিয়োগ বিজ্ঞপ্তি ও আবেদনসমূহ",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (bookmarksState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QawmiDeepGreen)
                }
            }
            is UiState.Error -> {
                Text(bookmarksState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                if (bookmarksState.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "আপনি এখনো কোনো খেদমত সংরক্ষণ করেননি।\nফিড থেকে বুকমার্ক আইকনে চাপ দিয়ে পছন্দের খেদমত সংরক্ষণ করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QawmiTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bookmarksState.data, key = { it.id }) { item ->
                            val matchingPost = allPosts.find { it.id == item.postId } ?: FeedPost(
                                id = item.postId,
                                authorId = item.authorId,
                                authorName = item.title,
                                postType = item.postType,
                                title = item.title,
                                district = item.district,
                                designation = item.designation,
                                qualification = item.qualification,
                                salaryOrExperience = item.salaryOrExperience,
                                deadlineOrPreferredDistricts = item.deadlineOrPreferredDistricts
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = QawmiTextDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { onRemoveBookmark(matchingPost) }) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "সরান",
                                                tint = Color(0xFFC62828)
                                            )
                                        }
                                    }
                                    Text("জেলা: ${item.district} | পদ: ${item.designation}", style = MaterialTheme.typography.bodySmall)
                                    Text("যোগ্যতা: ${item.qualification}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onApplyOrDetailClick(matchingPost) },
                                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("বিস্তারিত / আবেদন করুন", style = MaterialTheme.typography.labelMedium)
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

@Composable
fun SubmissionsManagementScreen(
    mySentState: UiState<List<KhedmotSubmission>>,
    receivedState: UiState<List<KhedmotSubmission>>,
    onUpdateStatus: (KhedmotSubmission, String) -> Unit,
    onMessageUser: (String, String) -> Unit,
    onInspectUserProfile: (String, String) -> Unit = { _, _ -> },
    onNavigatePublishApplication: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = আমার প্রেরিত আবেদন, 1 = মাদরাসায় প্রাপ্ত আবেদন

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("submissions_management_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("আমার প্রেরিত আবেদন") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = QawmiDeepGreen,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("মাদরাসায় প্রাপ্ত আবেদন") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = QawmiDeepGreen,
                    selectedLabelColor = Color.White
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onNavigatePublishApplication,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("+ নতুন আবেদন পোস্ট", style = MaterialTheme.typography.labelSmall, color = QawmiDeepGreen)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val activeState = if (selectedTab == 0) mySentState else receivedState

        when (activeState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QawmiDeepGreen)
                }
            }
            is UiState.Error -> {
                Text(activeState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                if (activeState.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedTab == 0) {
                                "আপনি এখনো কোনো নিয়োগ বিজ্ঞপ্তিতে সরাসরি আবেদন পাঠাননি।"
                            } else {
                                "আপনার প্রকাশিত বিজ্ঞপ্তিতে এখনো কোনো আবেদন জমা পড়েনি।"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = QawmiTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(activeState.data, key = { it.id }) { sub ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sub.postTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = QawmiTextDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val statusLabel = when (sub.status) {
                                            "SHORTLISTED" -> "মুযাকারার জন্য নির্বাচিত"
                                            "ACCEPTED" -> "কবুল / চূড়ান্ত"
                                            "REJECTED" -> "প্রত্যাখ্যাত"
                                            else -> "বিবেচনাধীন"
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = QawmiMintSurface
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text("খেদমতের পদ: ${sub.postDesignation}", style = MaterialTheme.typography.bodySmall)
                                    Text("প্রার্থীর নাম: ${sub.applicantName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("যোগ্যতা: ${sub.applicantQualification} | অভিজ্ঞতা: ${sub.applicantExperience}", style = MaterialTheme.typography.bodySmall)
                                    Text("যোগাযোগ: ${sub.applicantPhone}", style = MaterialTheme.typography.bodySmall)
                                    if (sub.coverNote.isNotBlank()) {
                                        Text("আরযি: ${sub.coverNote}", style = MaterialTheme.typography.bodySmall, color = QawmiTextSecondary)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (selectedTab == 1) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onUpdateStatus(sub, "SHORTLISTED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = QawmiSeekerBlue),
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("মুযাকারায় ডাকুন", style = MaterialTheme.typography.labelSmall)
                                            }
                                            Button(
                                                onClick = { onUpdateStatus(sub, "ACCEPTED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("কবুল করুন", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = { onInspectUserProfile(sub.applicantId, sub.applicantName) },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("প্রোফাইল", style = MaterialTheme.typography.labelSmall, color = QawmiDeepGreen)
                                            }
                                            OutlinedButton(
                                                onClick = { onMessageUser(sub.applicantId, sub.applicantName) },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("মেসেজ", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    } else {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { onInspectUserProfile(sub.recipientId, sub.postTitle) },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("মাদরাসা প্রোফাইল", style = MaterialTheme.typography.labelSmall, color = QawmiDeepGreen)
                                            }
                                            OutlinedButton(
                                                onClick = { onMessageUser(sub.recipientId, sub.postTitle) },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("মাদরাসা কর্তৃপক্ষকে মেসেজ পাঠান", style = MaterialTheme.typography.labelSmall)
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
    }
}

private data class ConversationSummary(
    val partnerId: String,
    val partnerName: String,
    val partnerPhotoUrl: String,
    val isPartnerOnline: Boolean,
    val partnerLastSeenAt: Timestamp?,
    val latestMessage: DirectMessage,
    val unreadCount: Int
)

@Composable
fun MessagesScreen(
    messagesState: UiState<List<DirectMessage>>,
    currentUserId: String,
    targetUser: Pair<String, String>?,
    partnerProfiles: Map<String, UserProfile> = emptyMap(),
    activePartnerProfile: UserProfile? = null,
    isSubmitting: Boolean,
    onSendMessage: (receiverId: String, receiverName: String, subject: String, body: String) -> Unit,
    onSelectRecipient: (String, String) -> Unit,
    onClearRecipient: () -> Unit = {},
    onMarkRead: (String) -> Unit,
    onMarkConversationRead: (String) -> Unit = {},
    onInspectUserProfile: (String, String) -> Unit = { _, _ -> }
) {
    var receiverId by remember(targetUser) { mutableStateOf(targetUser?.first.orEmpty()) }
    var receiverName by remember(targetUser) { mutableStateOf(targetUser?.second.orEmpty()) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    val allMessages = remember(messagesState) {
        (messagesState as? UiState.Success)?.data ?: emptyList()
    }

    val totalUnreadMessagesCount = remember(allMessages, currentUserId) {
        allMessages.count { it.receiverId == currentUserId && !it.isRead }
    }

    // Group messages into Conversation Threads by partnerId
    val conversationThreads = remember(allMessages, currentUserId, partnerProfiles) {
        allMessages
            .groupBy { msg ->
                if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
            }
            .filterKeys { it.isNotBlank() && it != currentUserId }
            .map { (partnerId, msgs) ->
                val sortedDesc = msgs.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                val latest = sortedDesc.first()
                val liveProfile = partnerProfiles[partnerId]
                val fallbackName = msgs.firstNotNullOfOrNull { m ->
                    if (m.senderId == partnerId) m.senderName.takeIf { it.isNotBlank() }
                    else m.receiverName.takeIf { it.isNotBlank() }
                } ?: "সম্মানিত দ্বীনি ভাই"
                val fallbackPhoto = msgs.firstNotNullOfOrNull { m ->
                    if (m.senderId == partnerId) m.senderPhotoUrl.takeIf { it.isNotBlank() } else null
                }.orEmpty()

                val unreadFromPartner = msgs.count { it.senderId == partnerId && it.receiverId == currentUserId && !it.isRead }

                ConversationSummary(
                    partnerId = partnerId,
                    partnerName = liveProfile?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName,
                    partnerPhotoUrl = liveProfile?.photoUrl?.takeIf { it.isNotBlank() } ?: fallbackPhoto,
                    isPartnerOnline = liveProfile?.isOnline == true,
                    partnerLastSeenAt = liveProfile?.lastSeenAt,
                    latestMessage = latest,
                    unreadCount = unreadFromPartner
                )
            }
            .sortedByDescending { it.latestMessage.createdAt ?: Timestamp(0, 0) }
    }

    // Active conversation messages sorted chronologically (oldest -> newest) for two-way chat view
    val activeConversationMessages = remember(allMessages, currentUserId, receiverId) {
        if (receiverId.isBlank()) {
            emptyList()
        } else {
            allMessages
                .filter {
                    (it.senderId == currentUserId && it.receiverId == receiverId) ||
                        (it.senderId == receiverId && it.receiverId == currentUserId)
                }
                .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    // Automatically mark unread incoming messages in the open conversation as read
    LaunchedEffect(receiverId, activeConversationMessages.size) {
        if (receiverId.isNotBlank()) {
            val hasUnread = activeConversationMessages.any {
                it.senderId == receiverId && it.receiverId == currentUserId && !it.isRead
            }
            if (hasUnread) {
                onMarkConversationRead(receiverId)
            }
        }
    }

    val resolvedPartnerProfile = activePartnerProfile ?: partnerProfiles[receiverId]
    val resolvedPartnerName = resolvedPartnerProfile?.fullName?.takeIf { it.isNotBlank() }
        ?: receiverName.ifBlank { "সম্মানিত প্রাপক" }
    val resolvedPartnerPhoto = resolvedPartnerProfile?.photoUrl.orEmpty()
    val isPartnerOnline = resolvedPartnerProfile?.isOnline == true
    val partnerStatusText = remember(isPartnerOnline, resolvedPartnerProfile?.lastSeenAt) {
        if (isPartnerOnline) {
            "অনলাইন (এখন সক্রিয়)"
        } else if (resolvedPartnerProfile?.lastSeenAt != null) {
            "অফলাইন • সর্বশেষ সক্রিয়: ${IslamicDateAndPrayerHelper.formatRelativeTimeBangla(resolvedPartnerProfile.lastSeenAt)}"
        } else {
            "অফলাইন"
        }
    }

    val chatListState = rememberLazyListState()

    // Automatically scroll to the latest message when opening a conversation or receiving/sending a new message
    LaunchedEffect(receiverId, activeConversationMessages.size) {
        if (receiverId.isNotBlank() && activeConversationMessages.isNotEmpty()) {
            chatListState.scrollToItem(activeConversationMessages.lastIndex)
        }
    }

    // Back button inside an active conversation returns to the Inbox thread list
    BackHandler(enabled = receiverId.isNotBlank()) {
        receiverId = ""
        receiverName = ""
        onClearRecipient()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QawmiSoftBg)
            .testTag("messages_screen")
    ) {
        if (receiverId.isNotBlank()) {
            // =========================================================================
            // ACTIVE CONVERSATION VIEW:
            // 1. Fixed/Sticky Top Partner Profile Header
            // 2. Scrollable Middle Message List (Modifier.weight(1f))
            // 3. Fixed/Sticky Bottom Text Message Composer Bar
            // =========================================================================

            // 1. STICKY TOP PARTNER HEADER
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = QawmiMintBorder)
                    .testTag("active_chat_partner_header"),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onInspectUserProfile(receiverId, resolvedPartnerName) }
                    ) {
                        IconButton(
                            onClick = {
                                receiverId = ""
                                receiverName = ""
                                onClearRecipient()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("back_to_inbox_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ইনবক্সে ফিরুন",
                                tint = QawmiDeepGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        UserAvatarImage(
                            photoUrl = resolvedPartnerPhoto,
                            displayName = resolvedPartnerName,
                            size = 44.dp,
                            borderColor = if (isPartnerOnline) Color(0xFF00C853) else QawmiMintBorder,
                            borderWidth = 1.5.dp,
                            showOnlineIndicator = true,
                            isOnline = isPartnerOnline
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = resolvedPartnerName,
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = partnerStatusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPartnerOnline) Color(0xFF1B5E20) else QawmiTextMuted,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.testTag("active_partner_presence_text")
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onInspectUserProfile(receiverId, resolvedPartnerName) },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("inspect_partner_profile_button")
                        ) {
                            Text("প্রোফাইল", style = MaterialTheme.typography.labelSmall, color = QawmiDeepGreen)
                        }
                        OutlinedButton(
                            onClick = {
                                receiverId = ""
                                receiverName = ""
                                onClearRecipient()
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("return_inbox_button")
                        ) {
                            Text("ইনবক্স", style = MaterialTheme.typography.labelSmall, color = QawmiTextSecondary)
                        }
                    }
                }
            }

            // 2. SCROLLABLE MIDDLE MESSAGE HISTORY LIST (ONLY THIS SECTION SCROLLS)
            if (activeConversationMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp)
                        .testTag("conversation_messages_list"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        UserAvatarImage(
                            photoUrl = resolvedPartnerPhoto,
                            displayName = resolvedPartnerName,
                            size = 64.dp,
                            borderColor = QawmiGold,
                            borderWidth = 2.dp,
                            showOnlineIndicator = true,
                            isOnline = isPartnerOnline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$resolvedPartnerName-এর সাথে কথোপকথন শুরু করুন",
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "নিচের বক্সে আপনার পয়গাম লিখে পাঠান। পাঠানো মেসেজ সাথে সাথে রিয়েল-টাইমে প্রাপকের ইনবক্সে পৌঁছে যাবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF4FAF7))
                        .padding(horizontal = 12.dp)
                        .testTag("conversation_messages_list"),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = activeConversationMessages,
                        key = { msg -> msg.id.ifBlank { "${msg.senderId}_${msg.receiverId}_${msg.createdAt?.seconds ?: 0}_${msg.body.hashCode()}" } }
                    ) { chatMsg ->
                        val sentByMe = chatMsg.senderId == currentUserId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_item_${chatMsg.id}"),
                            horizontalArrangement = if (sentByMe) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (!sentByMe) {
                                UserAvatarImage(
                                    photoUrl = resolvedPartnerPhoto,
                                    displayName = resolvedPartnerName,
                                    size = 28.dp,
                                    borderColor = QawmiMintBorder,
                                    borderWidth = 1.dp,
                                    modifier = Modifier.clickable {
                                        onInspectUserProfile(receiverId, resolvedPartnerName)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (sentByMe) 14.dp else 3.dp,
                                    bottomEnd = if (sentByMe) 3.dp else 14.dp
                                ),
                                color = if (sentByMe) QawmiDeepGreen else Color.White,
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .border(
                                        width = 1.dp,
                                        color = if (sentByMe) QawmiDeepGreen else QawmiMintBorder,
                                        shape = RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (sentByMe) 14.dp else 3.dp,
                                            bottomEnd = if (sentByMe) 3.dp else 14.dp
                                        )
                                    )
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    if (chatMsg.subject.isNotBlank() && chatMsg.subject != "খেদমত সংক্রান্ত যোগাযোগ") {
                                        Text(
                                            text = "বিষয়: ${chatMsg.subject}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sentByMe) QawmiGold else QawmiDeepGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = chatMsg.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (sentByMe) Color.White else QawmiTextDark
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = buildString {
                                                append(IslamicDateAndPrayerHelper.formatRelativeTimeBangla(chatMsg.createdAt))
                                                if (sentByMe) {
                                                    append(if (chatMsg.isRead) " • ✓✓ পঠিত" else " • ✓ প্রেরিত")
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = if (sentByMe) Color(0xFFDDF3EC) else QawmiTextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. STICKY BOTTOM TEXT MESSAGE COMPOSER BAR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = QawmiMintBorder)
                    .testTag("sticky_chat_composer_bar"),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        placeholder = { Text("আপনার পয়গাম / মেসেজ লিখুন...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_body_input"),
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp)
                    )
                    Button(
                        onClick = {
                            if (body.isNotBlank()) {
                                onSendMessage(receiverId, resolvedPartnerName, subject, body)
                                body = ""
                            }
                        },
                        enabled = !isSubmitting && body.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "পয়গাম পাঠান",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পাঠান", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // =========================================================================
            // INBOX CONVERSATION LIST VIEW (When no specific conversation is open)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. INBOX SUMMARY BANNER WITH UNREAD MESSAGE COUNT
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "পয়গাম ইনবক্স ও রিয়েল-টাইম কথোপকথন",
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "মোট কথোপকথন: ${IslamicDateAndPrayerHelper.toBanglaDigits(conversationThreads.size.toString())} টি • অপঠিত পয়গাম: ${IslamicDateAndPrayerHelper.toBanglaDigits(totalUnreadMessagesCount.toString())} টি",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiTextSecondary
                            )
                        }
                        if (totalUnreadMessagesCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = QawmiAlertRed,
                                modifier = Modifier.testTag("unread_messages_count_badge")
                            ) {
                                Text(
                                    text = "${IslamicDateAndPrayerHelper.toBanglaDigits(totalUnreadMessagesCount.toString())} অপঠিত",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "কথোপকথনের তালিকা (ইনবক্স)",
                    style = MaterialTheme.typography.titleSmall,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                when (messagesState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = QawmiDeepGreen)
                        }
                    }
                    is UiState.Error -> {
                        Text(messagesState.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        if (conversationThreads.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "এখনো কোনো পয়গাম আদান-প্রদান হয়নি।\nফিড, আবেদন বা মাদরাসা তালিকা থেকে যেকোনো প্রাপকের প্রোফাইলে গিয়ে মেসেজ পাঠাতে পারবেন।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = QawmiTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("inbox_threads_list")
                            ) {
                                items(conversationThreads, key = { it.partnerId }) { thread ->
                                    val latestMsg = thread.latestMessage
                                    val isLatestSentByMe = latestMsg.senderId == currentUserId
                                    val presenceBadge = if (thread.isPartnerOnline) {
                                        "অনলাইন"
                                    } else if (thread.partnerLastSeenAt != null) {
                                        "সর্বশেষ সক্রিয়: ${IslamicDateAndPrayerHelper.formatRelativeTimeBangla(thread.partnerLastSeenAt)}"
                                    } else {
                                        "অফলাইন"
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (thread.unreadCount > 0) 1.5.dp else 1.dp,
                                                color = if (thread.unreadCount > 0) QawmiDeepGreen else QawmiMintBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                if (!isLatestSentByMe && !latestMsg.isRead) {
                                                    onMarkRead(latestMsg.id)
                                                }
                                                onMarkConversationRead(thread.partnerId)
                                                receiverId = thread.partnerId
                                                receiverName = thread.partnerName
                                                onSelectRecipient(thread.partnerId, thread.partnerName)
                                            }
                                            .testTag("inbox_thread_${thread.partnerId}"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (thread.unreadCount > 0) QawmiMintSurface else Color.White
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            UserAvatarImage(
                                                photoUrl = thread.partnerPhotoUrl,
                                                displayName = thread.partnerName,
                                                size = 48.dp,
                                                borderColor = if (thread.isPartnerOnline) Color(0xFF00C853) else QawmiMintBorder,
                                                borderWidth = 1.5.dp,
                                                showOnlineIndicator = true,
                                                isOnline = thread.isPartnerOnline,
                                                modifier = Modifier.clickable {
                                                    onInspectUserProfile(thread.partnerId, thread.partnerName)
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = thread.partnerName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = QawmiDeepGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(latestMsg.createdAt),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = QawmiTextMuted
                                                    )
                                                }

                                                Text(
                                                    text = presenceBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (thread.isPartnerOnline) Color(0xFF1B5E20) else QawmiTextMuted,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (thread.isPartnerOnline) FontWeight.Bold else FontWeight.Normal
                                                )

                                                Spacer(modifier = Modifier.height(3.dp))

                                                Text(
                                                    text = if (isLatestSentByMe) "আপনি: ${latestMsg.body}" else latestMsg.body,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = QawmiTextDark,
                                                    fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 2
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "কথোপকথন খুলতে ট্যাপ করুন",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = QawmiSeekerBlue
                                                    )
                                                    if (thread.unreadCount > 0) {
                                                        Surface(
                                                            shape = RoundedCornerShape(50),
                                                            color = QawmiDeepGreen,
                                                            modifier = Modifier.testTag("thread_unread_badge_${thread.partnerId}")
                                                        ) {
                                                            Text(
                                                                text = "${IslamicDateAndPrayerHelper.toBanglaDigits(thread.unreadCount.toString())} টি নতুন",
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color.White,
                                                                fontSize = 10.sp,
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<AppNotification>>,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit
) {
    val unreadCount = remember(notificationsState) {
        (notificationsState as? UiState.Success)?.data?.count { !it.isRead } ?: 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("notifications_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "সকল নোটিফিকেশন ও ইত্তেলা",
                    style = MaterialTheme.typography.titleMedium,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (unreadCount > 0) {
                        "অপঠিত নোটিফিকেশন: ${IslamicDateAndPrayerHelper.toBanglaDigits(unreadCount.toString())} টি"
                    } else {
                        "আলহামদুলিল্লাহ, সকল নোটিফিকেশন পঠিত হয়েছে"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unreadCount > 0) QawmiDeepGreen else QawmiTextSecondary,
                    fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            if (unreadCount > 0) {
                OutlinedButton(
                    onClick = onMarkAllRead,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("mark_all_notifications_read_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = QawmiDeepGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "সব পঠিত করুন",
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        when (notificationsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QawmiDeepGreen)
                }
            }
            is UiState.Error -> {
                Text(notificationsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                if (notificationsState.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "আপনার কোনো নোটিফিকেশন নেই।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = QawmiTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "নতুন নিয়োগ বিজ্ঞপ্তি, আবেদন, পয়গাম বা অ্যাডমিনের ঘোষণা আসলে এখানে তাৎক্ষণিক দেখা যাবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = QawmiTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notificationsState.data, key = { it.id }) { notif ->
                            val typeLabel = when (notif.type) {
                                "NEW_CIRCULAR" -> "নতুন নিয়োগ বিজ্ঞপ্তি"
                                "NEW_APPLICATION" -> "নতুন খেদমত আবেদন"
                                "APPLICATION" -> "আবেদন সংক্রান্ত"
                                "MESSAGE" -> "পয়গাম / মেসেজ"
                                "ANNOUNCEMENT" -> "অ্যাডমিন ঘোষণা"
                                "VERIFICATION" -> "যাচাইকরণ"
                                "BOOST" -> "Boosted বিজ্ঞপ্তি"
                                else -> "ইত্তেলা"
                            }
                            val badgeColor = when (notif.type) {
                                "ANNOUNCEMENT", "BOOST" -> QawmiDarkGreen
                                "NEW_APPLICATION", "MESSAGE" -> QawmiSeekerBlue
                                else -> QawmiDeepGreen
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (!notif.isRead) 1.5.dp else 1.dp,
                                        color = if (!notif.isRead) QawmiDeepGreen else QawmiMintBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onNotificationClick(notif) }
                                    .testTag("notification_card_${notif.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!notif.isRead) QawmiMintSurface else Color.White
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = badgeColor
                                            ) {
                                                Text(
                                                    text = typeLabel,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (!notif.isRead) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFD32F2F))
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "অপঠিত",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFD32F2F),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(notif.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = QawmiTextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = QawmiDeepGreen,
                                        fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextDark
                                    )
                                    if (notif.senderName.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "প্রেরক: ${notif.senderName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = QawmiTextSecondary
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

@Composable
fun AdminPanelScreen(
    isAdminUser: Boolean,
    adminEmail: String = "",
    madrasasState: UiState<List<MadrasaProfile>>,
    feedPostsState: UiState<List<FeedPost>>,
    telegramNotificationsState: UiState<List<TelegramNotificationLog>> = UiState.Success(emptyList()),
    isSubmitting: Boolean = false,
    onVerifyMadrasa: (MadrasaProfile, Boolean) -> Unit,
    onVerifyPost: (FeedPost, Boolean, String) -> Unit,
    onToggleBoostPost: (FeedPost, Boolean) -> Unit,
    onSendAnnouncement: (title: String, message: String) -> Unit,
    onRequestPasswordReset: (email: String) -> Unit = {},
    onSendTestTelegramAlert: (customMessage: String) -> Unit = {},
    onDeletePost: (String) -> Unit
) {
    val context = LocalContext.current
    var announcementTitle by remember { mutableStateOf("") }
    var announcementBody by remember { mutableStateOf("") }
    var resetEmailInput by remember(adminEmail) {
        mutableStateOf(adminEmail.ifBlank { "fyslahm28@gmail.com" })
    }
    var customTelegramMessage by remember { mutableStateOf("") }
    val isBotConfigured = remember { TelegramNotificationHelper.isTelegramBotConfigured() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("admin_panel_screen")
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = QawmiDeepGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "অ্যাডমিন ও তদারকি প্যানেল (নাজিমে আলা)",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAdminUser) {
                            "যাচাইকৃত অ্যাডমিন: ${adminEmail.ifBlank { "নাজিমে আলা" }} • মোবাইল: ${AdminSecurityHelper.ADMIN_MOBILE_NUMBER}"
                        } else {
                            "অনুমোদনহীন প্রবেশাধিকার: শুধুমাত্র যাচাইকৃত অ্যাডমিন এই প্যানেল ব্যবহার করতে পারবেন।"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAdminUser) QawmiDeepGreen else Color(0xFFC62828),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isAdminUser) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEF9A9A), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "অ্যাডমিন অ্যাক্সেস সংরক্ষিত",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "শুধুমাত্র অনুমোদিত অ্যাডমিন অ্যাকাউন্ট (Firebase Authentication ও সার্ভার-সাইড Security Rules দ্বারা যাচাইকৃত) দিয়ে লগইন করলে অ্যাডমিন প্যানেল ব্যবহার করা যাবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextDark
                    )
                }
            }
            return@Column
        }

        val madrasas = (madrasasState as? UiState.Success)?.data ?: emptyList()
        val posts = (feedPostsState as? UiState.Success)?.data ?: emptyList()
        val telegramLogs = (telegramNotificationsState as? UiState.Success)?.data ?: emptyList()

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 1. ADMIN IDENTITY, SECURITY & PASSWORD RECOVERY CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "১. অনুমোদিত অ্যাডমিন পরিচয় ও পাসওয়ার্ড পুনরুদ্ধার",
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = QawmiDeepGreen
                            ) {
                                Text(
                                    text = "SUPER_ADMIN ✓",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = QawmiSoftBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "সার্ভার-যাচাইকৃত অনুমোদিত অ্যাডমিন ইমেইলসমূহ:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                AdminSecurityHelper.AUTHORIZED_ADMIN_EMAILS.forEach { mail ->
                                    Text(
                                        text = "• $mail",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextDark,
                                        fontWeight = if (mail.equals(adminEmail, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Text(
                                    text = "অ্যাডমিন মোবাইল নম্বর: ${AdminSecurityHelper.ADMIN_MOBILE_NUMBER}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = resetEmailInput,
                            onValueChange = { resetEmailInput = it },
                            label = { Text("পাসওয়ার্ড পুনরুদ্ধারের জন্য নিবন্ধিত অ্যাডমিন ইমেইল") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_panel_reset_email_input")
                        )

                        Button(
                            onClick = { onRequestPasswordReset(resetEmailInput) },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = QawmiSeekerBlue),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("admin_panel_reset_password_button")
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Password Reset Link পাঠান")
                        }
                    }
                }
            }

            // 2. TELEGRAM NOTIFICATION SYSTEM & REAL-TIME DISPATCH CENTER
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiDeepGreen, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "২. টেলিগ্রাম নোটিফিকেশন সিস্টেম ও ইত্তেলা লগ",
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isBotConfigured) QawmiVerifiedGreen else QawmiDarkGreen
                            ) {
                                Text(
                                    text = if (isBotConfigured) "Bot API সংযুক্ত ✓" else "Firestore Queue সক্রিয়",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = buildString {
                                append("প্রধান অ্যাডমিন টেলিগ্রাম আইডি: ${TelegramNotificationHelper.DEFAULT_TELEGRAM_CHANNEL}")
                                if (isBotConfigured) {
                                    append(" • Bot Token ও Chat ID সংযুক্ত ও সচল ✓")
                                }
                                append("\nঅ্যাডমিন লগইন/লগআউট, বিজ্ঞপ্তি, আবেদন, মাদরাসা যাচাই ও নিরাপত্তা অডিট লগ স্বয়ংক্রিয়ভাবে বটের মাধ্যমে সরাসরি আপনার ইনবক্সে প্রেরিত হচ্ছে (হটলাইন: ${AdminSecurityHelper.ADMIN_MOBILE_NUMBER})।")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary
                        )

                        OutlinedTextField(
                            value = customTelegramMessage,
                            onValueChange = { customTelegramMessage = it },
                            label = { Text("প্রধান অ্যাডমিনের টেলিগ্রামে তাৎক্ষণিক বার্তা (ঐচ্ছিক)") },
                            placeholder = { Text("যেমন: নতুন বিজ্ঞপ্তি ও মাদরাসা যাচাইকরণ সম্পন্ন হয়েছে...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_telegram_custom_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { TelegramNotificationHelper.openTelegramChannel(context) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("admin_open_telegram_channel_button")
                            ) {
                                Text(
                                    text = TelegramNotificationHelper.DEFAULT_TELEGRAM_CHANNEL,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    onSendTestTelegramAlert(customTelegramMessage)
                                    customTelegramMessage = ""
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("admin_send_telegram_test_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("টেলিগ্রাম ইত্তেলা পাঠান")
                            }
                        }

                        if (telegramLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "সাম্প্রতিক টেলিগ্রাম ইত্তেলা সমূহ (${IslamicDateAndPrayerHelper.toBanglaDigits(telegramLogs.size.toString())} টি):",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                            telegramLogs.take(5).forEach { tgLog ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = QawmiSoftBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                                        .testTag("telegram_log_item_${tgLog.id}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = tgLog.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (tgLog.deliveryStatus == "DELIVERED") QawmiVerifiedGreen else QawmiGold
                                            ) {
                                                Text(
                                                    text = if (tgLog.deliveryStatus == "DELIVERED") "DELIVERED" else "QUEUED",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.5.sp,
                                                    color = if (tgLog.deliveryStatus == "DELIVERED") Color.White else QawmiTextDark,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = tgLog.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QawmiTextDark
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${tgLog.actorName} • ${IslamicDateAndPrayerHelper.formatRelativeTimeBangla(tgLog.createdAt)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QawmiTextMuted
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    val formatted = TelegramNotificationHelper.formatTelegramEventMessage(
                                                        eventType = tgLog.eventType,
                                                        title = tgLog.title,
                                                        message = tgLog.message,
                                                        actorName = tgLog.actorName,
                                                        targetPhone = tgLog.targetPhone
                                                    )
                                                    TelegramNotificationHelper.shareAlertToTelegram(context, formatted)
                                                },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text(
                                                    text = "টেলিগ্রামে ফরোয়ার্ড",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = QawmiDeepGreen
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

            // 3. ADMIN BROADCAST ANNOUNCEMENT CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiDeepGreen, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "৩. সকল ব্যবহারকারীর জন্য অ্যাডমিনের ঘোষণা (Push ও ইন-অ্যাপ ইত্তেলা)",
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = announcementTitle,
                            onValueChange = { announcementTitle = it },
                            label = { Text("ঘোষণার শিরোনাম *") },
                            placeholder = { Text("যেমন: নতুন শিক্ষাবর্ষের খেদমত বিজ্ঞপ্তি সংক্রান্ত জরুরি এলান") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_announcement_title_input")
                        )
                        OutlinedTextField(
                            value = announcementBody,
                            onValueChange = { announcementBody = it },
                            label = { Text("বিস্তারিত ঘোষণা / বার্তা *") },
                            placeholder = { Text("সকল মুহতামিম ও মুদাররিস ভাইদের অবগতির জন্য জানানো যাচ্ছে যে...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                                .testTag("admin_announcement_body_input")
                        )
                        Button(
                            onClick = {
                                onSendAnnouncement(announcementTitle, announcementBody)
                                announcementTitle = ""
                                announcementBody = ""
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("admin_send_announcement_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ঘোষণা প্রকাশ করুন")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "প্রকাশিত বিজ্ঞপ্তি ও Boost নিয়ন্ত্রণ (${posts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            items(posts, key = { "adm_p_${it.id}" }) { post ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (post.isBoosted) 2.dp else 1.dp,
                            color = if (post.isBoosted) QawmiGold else QawmiMintBorder,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (post.isBoosted) Color(0xFFFFFDF6) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(post.title, fontWeight = FontWeight.Bold, color = QawmiTextDark)
                                    if (post.isBoosted) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = QawmiDarkGreen
                                        ) {
                                            Text(
                                                text = "★ Boosted",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QawmiGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "পদ: ${post.designation} (${post.district})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QawmiTextSecondary
                                )
                            }
                            IconButton(onClick = { onDeletePost(post.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = Color(0xFFC62828))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onToggleBoostPost(post, !post.isBoosted) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (post.isBoosted) QawmiDarkGreen else QawmiGold,
                                    contentColor = if (post.isBoosted) Color.White else QawmiTextDark
                                ),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("admin_panel_boost_btn_${post.id}")
                            ) {
                                Text(
                                    text = if (post.isBoosted) "★ Boost সরান" else "★ ফিডে Boost করুন",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { onVerifyPost(post, !post.isVerifiedMadrasa, "APPROVED") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (post.isVerifiedMadrasa) QawmiSeekerBlue else QawmiDeepGreen
                                ),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (post.isVerifiedMadrasa) "আনভেরিফাই" else "ভেরিফাই ব্যাজ দিন",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "মাদরাসা যাচাইকরণ তালিকা (${madrasas.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            items(madrasas, key = { "adm_m_${it.id}" }) { madrasa ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
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
                            Text("জেলা: ${madrasa.district} | মারহালা: ${madrasa.highestMarhala}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (madrasa.isVerified) "অবস্থা: যাচাইকৃত ✓" else "অবস্থা: অযাচাইকৃত",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (madrasa.isVerified) QawmiVerifiedGreen else QawmiTextMuted
                            )
                        }
                        Button(
                            onClick = { onVerifyMadrasa(madrasa, !madrasa.isVerified) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (madrasa.isVerified) QawmiSeekerBlue else QawmiDeepGreen
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (madrasa.isVerified) "বাতিল" else "যাচাইকৃত করুন",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullPrayerScheduleScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("full_prayer_schedule_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "বাংলাদেশের বিভাগওয়ারী নামাজের চিরস্থায়ী সময়সূচী",
                style = MaterialTheme.typography.titleMedium,
                color = QawmiTextDark,
                fontWeight = FontWeight.Bold
            )
        }
        items(IslamicDateAndPrayerHelper.divisionsSchedule) { divSchedule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiDarkGreen)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = QawmiGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "বিভাগ: ${divSchedule.divisionBangla}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
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
                                    .border(1.dp, Color(0xFF1D6E57), RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0A4B39)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(waqt.nameBangla, style = MaterialTheme.typography.labelSmall, color = Color(0xFFDDF3EC))
                                    Text(waqt.timeBangla, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(waqt.period, style = MaterialTheme.typography.labelSmall, color = Color(0xFFB5DCD0), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
