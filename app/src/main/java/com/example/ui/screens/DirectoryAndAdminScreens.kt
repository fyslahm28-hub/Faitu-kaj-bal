package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
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
import com.example.data.model.IslamicReaction
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
    val isPartnerTyping: Boolean,
    val partnerLastSeenAt: Timestamp?,
    val latestMessage: DirectMessage,
    val unreadCount: Int
)

private fun isPartnerCurrentlyTypingToMe(
    partnerProfile: UserProfile?,
    currentUserId: String
): Boolean {
    if (partnerProfile == null || currentUserId.isBlank()) return false
    if (partnerProfile.typingToUserId != currentUserId) return false
    val updatedAt = partnerProfile.typingUpdatedAt ?: return true
    val ageMillis = System.currentTimeMillis() - updatedAt.toDate().time
    return ageMillis <= 15_000L
}

private data class ChatThemePalette(
    val spec: String,
    val titleBangla: String,
    val subtitleBangla: String,
    val chatBackgroundColor: Color,
    val sentBubbleColor: Color,
    val sentBubbleTextColor: Color,
    val receivedBubbleColor: Color,
    val headerAccentColor: Color
)

private val PresetChatThemes = listOf(
    ChatThemePalette(
        spec = "EMERALD_CLASSIC",
        titleBangla = "সবুজ মিনার (ডিফল্ট আবহ)",
        subtitleBangla = "ঐতিহ্যবাহী কওমি সবুজ ও প্রশান্তিময় সাদা",
        chatBackgroundColor = Color(0xFFF4FAF7),
        sentBubbleColor = Color(0xFF0A4D3C),
        sentBubbleTextColor = Color.White,
        receivedBubbleColor = Color.White,
        headerAccentColor = Color(0xFF0A4D3C)
    ),
    ChatThemePalette(
        spec = "ROYAL_GOLD",
        titleBangla = "সোনালী নূর ও গম্বুজ",
        subtitleBangla = "মার্জিত সোনালী আভা ও গাঢ় বাদামি-সবুজ",
        chatBackgroundColor = Color(0xFFFDF8EE),
        sentBubbleColor = Color(0xFF5C4308),
        sentBubbleTextColor = Color.White,
        receivedBubbleColor = Color(0xFFFFFDF9),
        headerAccentColor = Color(0xFF7A590B)
    ),
    ChatThemePalette(
        spec = "MIDNIGHT_HILAL",
        titleBangla = "রাতের হিলাল ও প্রশান্তি",
        subtitleBangla = "স্নিগ্ধ নীলচে-ধূসর ও গভীর নীল পয়গাম বক্স",
        chatBackgroundColor = Color(0xFFEEF3F8),
        sentBubbleColor = Color(0xFF163A5F),
        sentBubbleTextColor = Color.White,
        receivedBubbleColor = Color.White,
        headerAccentColor = Color(0xFF163A5F)
    ),
    ChatThemePalette(
        spec = "DESERT_SAND",
        titleBangla = "মরুভূমির উষ্ণ আবহ",
        subtitleBangla = "আরবীয় মরুর কোমল বালুকা ও খেজুর রঙ",
        chatBackgroundColor = Color(0xFFFAF3EB),
        sentBubbleColor = Color(0xFF6D3B24),
        sentBubbleTextColor = Color.White,
        receivedBubbleColor = Color.White,
        headerAccentColor = Color(0xFF6D3B24)
    )
)

private fun parseHexColorSafe(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6) return fallback
    val rgb = clean.toLongOrNull(16) ?: return fallback
    return Color(0xFF000000L or rgb)
}

private fun resolveChatThemePalette(spec: String?): ChatThemePalette {
    val trimmed = spec?.trim().orEmpty()
    if (trimmed.startsWith("CUSTOM:", ignoreCase = true)) {
        val parts = trimmed.substringAfter(":").split("|")
        val bgHex = parts.getOrNull(0).orEmpty()
        val bubbleHex = parts.getOrNull(1).orEmpty()
        val accentHex = parts.getOrNull(2).orEmpty().ifBlank { bubbleHex }
        val bgColor = parseHexColorSafe(bgHex, Color(0xFFF2F7F5))
        val bubbleColor = parseHexColorSafe(bubbleHex, Color(0xFF0A4D3C))
        val accentColor = parseHexColorSafe(accentHex, bubbleColor)
        return ChatThemePalette(
            spec = trimmed,
            titleBangla = "কাস্টম আবহ",
            subtitleBangla = "আপনার নির্বাচিত কাস্টম রঙের কথোপকথন আবহ",
            chatBackgroundColor = bgColor,
            sentBubbleColor = bubbleColor,
            sentBubbleTextColor = Color.White,
            receivedBubbleColor = Color.White,
            headerAccentColor = accentColor
        )
    }
    return PresetChatThemes.find { it.spec.equals(trimmed, ignoreCase = true) }
        ?: PresetChatThemes.first()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessagesScreen(
    messagesState: UiState<List<DirectMessage>>,
    currentUserId: String,
    currentUserProfile: UserProfile? = null,
    targetUser: Pair<String, String>?,
    partnerProfiles: Map<String, UserProfile> = emptyMap(),
    activePartnerProfile: UserProfile? = null,
    isSubmitting: Boolean,
    onSendMessage: (receiverId: String, receiverName: String, subject: String, body: String) -> Unit,
    onSelectRecipient: (String, String) -> Unit,
    onClearRecipient: () -> Unit = {},
    onMarkRead: (String) -> Unit,
    onMarkConversationRead: (String) -> Unit = {},
    onTypingStatusChanged: (targetUserId: String, isTyping: Boolean) -> Unit = { _, _ -> },
    onInspectUserProfile: (String, String) -> Unit = { _, _ -> },
    onReactToMessage: (DirectMessage, String) -> Unit = { _, _ -> },
    onEditMessage: (DirectMessage, String) -> Unit = { _, _ -> },
    onDeleteMessageForMe: (DirectMessage) -> Unit = {},
    onDeleteMessageForEveryone: (DirectMessage) -> Unit = {},
    onToggleBlockUser: (targetUserId: String, block: Boolean) -> Unit = { _, _ -> },
    onSaveConversationTheme: (partnerUserId: String, themeSpec: String) -> Unit = { _, _ -> }
) {
    var receiverId by remember(targetUser) { mutableStateOf(targetUser?.first.orEmpty()) }
    var receiverName by remember(targetUser) { mutableStateOf(targetUser?.second.orEmpty()) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    // State for Long-Press Message Action Sheet, Edit Dialog, 3-Dot Menu, and Theme Picker
    var selectedMessageForAction by remember { mutableStateOf<DirectMessage?>(null) }
    var messageBeingEdited by remember { mutableStateOf<DirectMessage?>(null) }
    var editedBodyText by remember { mutableStateOf("") }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var showThemePickerDialog by remember { mutableStateOf(false) }

    // Optimistic local state for immediate UI responsiveness during tests and real-time updates
    var localThemeOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var localBlockOverrides by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var locallyDeletedForMeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var localReactionOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var localEditedBodies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var locallyDeletedForEveryoneIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Strictly deduplicate messages by id and filter out messages deleted for the current user
    val allMessages = remember(
        messagesState,
        currentUserId,
        locallyDeletedForMeIds,
        localReactionOverrides,
        localEditedBodies,
        locallyDeletedForEveryoneIds
    ) {
        val raw = (messagesState as? UiState.Success)?.data ?: emptyList()
        raw
            .filterNot { msg ->
                locallyDeletedForMeIds.contains(msg.id) ||
                    (msg.senderId == currentUserId && msg.deletedForSender) ||
                    (msg.receiverId == currentUserId && msg.deletedForReceiver)
            }
            .distinctBy { msg ->
                msg.id.ifBlank {
                    "${msg.senderId}_${msg.receiverId}_${msg.createdAt?.seconds ?: 0}_${msg.createdAt?.nanoseconds ?: 0}_${msg.body}"
                }
            }
            .map { msg ->
                var updated = msg
                localReactionOverrides[msg.id]?.let { reaction ->
                    updated = if (msg.senderId == currentUserId) {
                        updated.copy(senderReaction = reaction)
                    } else {
                        updated.copy(receiverReaction = reaction)
                    }
                }
                localEditedBodies[msg.id]?.let { newBody ->
                    updated = updated.copy(body = newBody, isEdited = true)
                }
                if (locallyDeletedForEveryoneIds.contains(msg.id)) {
                    updated = updated.copy(
                        body = "এই পয়গামটি প্রত্যাহার করা হয়েছে",
                        isDeletedForEveryone = true,
                        senderReaction = "",
                        receiverReaction = ""
                    )
                }
                updated
            }
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
                val uniqueMsgs = msgs.distinctBy { msg ->
                    msg.id.ifBlank {
                        "${msg.senderId}_${msg.receiverId}_${msg.createdAt?.seconds ?: 0}_${msg.createdAt?.nanoseconds ?: 0}_${msg.body}"
                    }
                }
                val sortedDesc = uniqueMsgs.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                val latest = sortedDesc.first()
                val liveProfile = partnerProfiles[partnerId]
                val fallbackName = uniqueMsgs.firstNotNullOfOrNull { m ->
                    if (m.senderId == partnerId) m.senderName.takeIf { it.isNotBlank() }
                    else m.receiverName.takeIf { it.isNotBlank() }
                } ?: "সম্মানিত দ্বীনি ভাই"
                val fallbackPhoto = uniqueMsgs.firstNotNullOfOrNull { m ->
                    if (m.senderId == partnerId) m.senderPhotoUrl.takeIf { it.isNotBlank() } else null
                }.orEmpty()

                val unreadFromPartner = uniqueMsgs.count {
                    it.senderId == partnerId && it.receiverId == currentUserId && !it.isRead
                }

                ConversationSummary(
                    partnerId = partnerId,
                    partnerName = liveProfile?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName,
                    partnerPhotoUrl = liveProfile?.photoUrl?.takeIf { it.isNotBlank() } ?: fallbackPhoto,
                    isPartnerOnline = liveProfile?.isOnline == true,
                    isPartnerTyping = isPartnerCurrentlyTypingToMe(liveProfile, currentUserId),
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
    val unreadInActiveChatCount = remember(activeConversationMessages, receiverId, currentUserId) {
        if (receiverId.isBlank()) 0
        else activeConversationMessages.count {
            it.senderId == receiverId && it.receiverId == currentUserId && !it.isRead
        }
    }

    LaunchedEffect(receiverId, unreadInActiveChatCount) {
        if (receiverId.isNotBlank() && unreadInActiveChatCount > 0) {
            activeConversationMessages
                .filter { it.senderId == receiverId && it.receiverId == currentUserId && !it.isRead && it.id.isNotBlank() }
                .forEach { unreadMsg -> onMarkRead(unreadMsg.id) }
            onMarkConversationRead(receiverId)
        }
    }

    // Real-time typing status broadcast to Firestore with auto-timeout
    LaunchedEffect(receiverId, body) {
        if (receiverId.isNotBlank()) {
            if (body.isNotBlank()) {
                onTypingStatusChanged(receiverId, true)
                delay(2500L)
                onTypingStatusChanged(receiverId, false)
            } else {
                onTypingStatusChanged(receiverId, false)
            }
        }
    }

    DisposableEffect(receiverId) {
        val activeReceiver = receiverId
        onDispose {
            if (activeReceiver.isNotBlank()) {
                onTypingStatusChanged(activeReceiver, false)
            }
        }
    }

    val resolvedPartnerProfile = activePartnerProfile ?: partnerProfiles[receiverId]
    val resolvedPartnerName = resolvedPartnerProfile?.fullName?.takeIf { it.isNotBlank() }
        ?: receiverName.ifBlank { "সম্মানিত প্রাপক" }
    val resolvedPartnerPhoto = resolvedPartnerProfile?.photoUrl.orEmpty()
    val isPartnerOnline = resolvedPartnerProfile?.isOnline == true
    val isPartnerTyping = isPartnerCurrentlyTypingToMe(resolvedPartnerProfile, currentUserId)
    val partnerStatusText = remember(isPartnerTyping, isPartnerOnline, resolvedPartnerProfile?.lastSeenAt) {
        if (isPartnerTyping) {
            "টাইপ করছেন… • • •"
        } else if (isPartnerOnline) {
            "অনলাইন (এখন সক্রিয়)"
        } else if (resolvedPartnerProfile?.lastSeenAt != null) {
            "অফলাইন • সর্বশেষ সক্রিয়: ${IslamicDateAndPrayerHelper.formatRelativeTimeBangla(resolvedPartnerProfile.lastSeenAt)}"
        } else {
            "অফলাইন"
        }
    }

    // Resolve conversation theme (local override -> currentUserProfile -> partnerProfile -> default)
    val activeThemeSpec = localThemeOverrides[receiverId]
        ?: currentUserProfile?.chatThemes?.get(receiverId)
        ?: resolvedPartnerProfile?.chatThemes?.get(currentUserId)
        ?: "EMERALD_CLASSIC"
    val activeThemePalette = remember(activeThemeSpec) {
        resolveChatThemePalette(activeThemeSpec)
    }

    // Resolve block status (either blocked by current user or blocked by partner)
    val isBlockedByMe = localBlockOverrides[receiverId]
        ?: (receiverId.isNotBlank() && currentUserProfile?.blockedUserIds?.contains(receiverId) == true)
    val isBlockedByPartner = receiverId.isNotBlank() &&
        resolvedPartnerProfile?.blockedUserIds?.contains(currentUserId) == true

    val chatListState = rememberLazyListState()

    // Automatically scroll to the latest message when opening a conversation or receiving/sending a new message
    LaunchedEffect(receiverId, activeConversationMessages.size, isPartnerTyping) {
        if (receiverId.isNotBlank() && activeConversationMessages.isNotEmpty()) {
            chatListState.scrollToItem(activeConversationMessages.lastIndex)
        }
    }

    // Back button inside an active conversation returns to the Inbox thread list
    BackHandler(enabled = receiverId.isNotBlank()) {
        if (receiverId.isNotBlank()) {
            onTypingStatusChanged(receiverId, false)
        }
        receiverId = ""
        receiverName = ""
        onClearRecipient()
    }

    // Long-press Message Action & Islamic Reaction Dialog
    selectedMessageForAction?.let { targetMsg ->
        val isMyMessage = targetMsg.senderId == currentUserId
        val myCurrentReaction = if (isMyMessage) targetMsg.senderReaction else targetMsg.receiverReaction
        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            modifier = Modifier.testTag("message_action_sheet"),
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Column {
                    Text(
                        text = "পয়গাম পরিচালনা ও ইসলামিক অভিবাদন",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "সুন্দর দ্বীনি অভিবাদন জানান অথবা পয়গাম পরিচালনা করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!targetMsg.isDeletedForEveryone) {
                        Text(
                            text = "ইসলামিক অভিবাদন ও দোয়া প্রতিক্রিয়া:",
                            style = MaterialTheme.typography.labelMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        // 2 rows of 3 Islamic reactions so all 6 are immediately visible without horizontal scrolling
                        val reactions = IslamicReaction.entries
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            reactions.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { reaction ->
                                        val isSelected = myCurrentReaction == reaction.code
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) QawmiMintSurface else Color(0xFFF7F9F8),
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) QawmiDeepGreen else QawmiMintBorder,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    val newCode = if (isSelected) "" else reaction.code
                                                    if (targetMsg.id.isNotBlank()) {
                                                        localReactionOverrides = localReactionOverrides + (targetMsg.id to newCode)
                                                    }
                                                    onReactToMessage(targetMsg, newCode)
                                                    selectedMessageForAction = null
                                                }
                                                .testTag("reaction_chip_${reaction.code}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(text = reaction.emoji, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = reaction.titleBangla,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = QawmiDeepGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = QawmiMintBorder)
                    }

                    // Edit option (strictly for sender when message is not retracted)
                    if (isMyMessage && !targetMsg.isDeletedForEveryone) {
                        OutlinedButton(
                            onClick = {
                                selectedMessageForAction = null
                                messageBeingEdited = targetMsg
                                editedBodyText = targetMsg.body
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("action_edit_message")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "পয়গাম সম্পাদনা করুন",
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Delete for Me (available for both sender and receiver)
                    OutlinedButton(
                        onClick = {
                            if (targetMsg.id.isNotBlank()) {
                                locallyDeletedForMeIds = locallyDeletedForMeIds + targetMsg.id
                            }
                            onDeleteMessageForMe(targetMsg)
                            selectedMessageForAction = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_delete_for_me")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = QawmiTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "নিজের জন্য মুছুন (আমার ইনবক্স থেকে)",
                            color = QawmiTextDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Delete for Everyone (strictly for sender when not already retracted)
                    if (isMyMessage && !targetMsg.isDeletedForEveryone) {
                        Button(
                            onClick = {
                                if (targetMsg.id.isNotBlank()) {
                                    locallyDeletedForEveryoneIds = locallyDeletedForEveryoneIds + targetMsg.id
                                }
                                onDeleteMessageForEveryone(targetMsg)
                                selectedMessageForAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = QawmiAlertRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("action_delete_for_everyone")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "সবার জন্য পয়গাম প্রত্যাহার করুন",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForAction = null }) {
                    Text("বন্ধ করুন", color = QawmiDeepGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Message Edit Dialog
    messageBeingEdited?.let { editingMsg ->
        AlertDialog(
            onDismissRequest = { messageBeingEdited = null },
            modifier = Modifier.testTag("edit_message_dialog"),
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "পয়গাম সম্পাদনা করুন",
                    style = MaterialTheme.typography.titleMedium,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "সম্পাদিত পয়গামটি প্রেরক ও প্রাপক উভয়ের কথোপকথনে সাথে সাথে আপডেট হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                    OutlinedTextField(
                        value = editedBodyText,
                        onValueChange = { editedBodyText = it },
                        label = { Text("সংশোধিত পয়গাম") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_message_input"),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editedBodyText.trim()
                        if (trimmed.isNotBlank()) {
                            if (editingMsg.id.isNotBlank()) {
                                localEditedBodies = localEditedBodies + (editingMsg.id to trimmed)
                            }
                            onEditMessage(editingMsg, trimmed)
                            messageBeingEdited = null
                        }
                    },
                    enabled = editedBodyText.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                    modifier = Modifier.testTag("confirm_edit_message_button")
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageBeingEdited = null }) {
                    Text("বাতিল", color = QawmiTextSecondary)
                }
            }
        )
    }

    // Conversation Theme & Custom Theme Picker Dialog
    if (showThemePickerDialog && receiverId.isNotBlank()) {
        var customBgHex by remember { mutableStateOf("#EEF7F2") }
        var customBubbleHex by remember { mutableStateOf("#115E46") }
        AlertDialog(
            onDismissRequest = { showThemePickerDialog = false },
            modifier = Modifier.testTag("chat_theme_picker_dialog"),
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Column {
                    Text(
                        text = "কথোপকথনের আবহ ও থিম নির্বাচন",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "মনোরম ইসলামিক আবহ অথবা নিজস্ব কাস্টম রঙের থিম বেছে নিন",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChatThemes.forEach { preset ->
                        val isCurrent = activeThemePalette.spec.equals(preset.spec, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = preset.chatBackgroundColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) preset.sentBubbleColor else QawmiMintBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    localThemeOverrides = localThemeOverrides + (receiverId to preset.spec)
                                    onSaveConversationTheme(receiverId, preset.spec)
                                    showThemePickerDialog = false
                                }
                                .testTag("theme_option_${preset.spec}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.titleBangla,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = preset.headerAccentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = preset.subtitleBangla,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(preset.sentBubbleColor)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = QawmiMintBorder)

                    Text(
                        text = "কাস্টম থিম নির্মাতা (নিজস্ব রঙ নির্বাচন):",
                        style = MaterialTheme.typography.labelMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val customSwatches = listOf(
                            "সবুজ-নূর" to ("#EAF6F0" to "#0D5C46"),
                            "নীল-আসমান" to ("#EDF4FA" to "#1B4965"),
                            "জয়তুন-স্বর্ণ" to ("#F7F5EB" to "#4A5D23")
                        )
                        customSwatches.forEachIndexed { idx, (label, pair) ->
                            OutlinedButton(
                                onClick = {
                                    customBgHex = pair.first
                                    customBubbleHex = pair.second
                                },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_theme_preset_$idx")
                            ) {
                                Text(label, fontSize = 10.sp, color = QawmiDeepGreen, maxLines = 1)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customBgHex,
                            onValueChange = { customBgHex = it },
                            label = { Text("পটভূমি (#HEX)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_theme_bg_input")
                        )
                        OutlinedTextField(
                            value = customBubbleHex,
                            onValueChange = { customBubbleHex = it },
                            label = { Text("পয়গাম বক্স (#HEX)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_theme_bubble_input")
                        )
                    }

                    Button(
                        onClick = {
                            val customSpec = "CUSTOM:${customBgHex.trim()}|${customBubbleHex.trim()}|${customBubbleHex.trim()}"
                            localThemeOverrides = localThemeOverrides + (receiverId to customSpec)
                            onSaveConversationTheme(receiverId, customSpec)
                            showThemePickerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_custom_theme_button")
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কাস্টম আবহ প্রয়োগ করুন")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePickerDialog = false }) {
                    Text("বন্ধ করুন", color = QawmiDeepGreen)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(activeThemePalette.chatBackgroundColor)
            .imePadding()
            .testTag("messages_screen")
    ) {
        if (receiverId.isNotBlank()) {
            // =========================================================================
            // ACTIVE CONVERSATION VIEW:
            // 1. Fixed/Sticky Top Partner Profile Header + Three-Dot Menu
            // 2. Scrollable Middle Message List (Modifier.weight(1f))
            // 3. Fixed/Sticky Bottom Text Message Composer Bar or Blocked Banner
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
                                tint = activeThemePalette.headerAccentColor
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
                                color = activeThemePalette.headerAccentColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = partnerStatusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPartnerTyping || isPartnerOnline) Color(0xFF1B5E20) else QawmiTextMuted,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.testTag("active_partner_presence_text")
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                            Text("প্রোফাইল", style = MaterialTheme.typography.labelSmall, color = activeThemePalette.headerAccentColor)
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

                        // Three-dot menu button for Conversation Theme & Block/Unblock
                        Box {
                            IconButton(
                                onClick = { showThreeDotMenu = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("chat_three_dot_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "কথোপকথন মেনু",
                                    tint = activeThemePalette.headerAccentColor
                                )
                            }
                            DropdownMenu(
                                expanded = showThreeDotMenu,
                                onDismissRequest = { showThreeDotMenu = false },
                                modifier = Modifier
                                    .background(Color.White)
                                    .testTag("chat_three_dot_dropdown_menu")
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "কথোপকথনের আবহ ও থিম পরিবর্তন",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "বর্তমান: ${activeThemePalette.titleBangla}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QawmiTextSecondary
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = QawmiDeepGreen
                                        )
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        showThemePickerDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_change_chat_theme")
                                )
                                HorizontalDivider(color = QawmiMintBorder)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isBlockedByMe) {
                                                "যোগাযোগ পুনরায় চালু করুন (আনব্লক করুন)"
                                            } else {
                                                "যোগাযোগ বিরত রাখুন (ব্লক করুন)"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isBlockedByMe) QawmiDeepGreen else QawmiAlertRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = if (isBlockedByMe) QawmiDeepGreen else QawmiAlertRed
                                        )
                                    },
                                    onClick = {
                                        val newBlockState = !isBlockedByMe
                                        localBlockOverrides = localBlockOverrides + (receiverId to newBlockState)
                                        onToggleBlockUser(receiverId, newBlockState)
                                        showThreeDotMenu = false
                                    },
                                    modifier = Modifier.testTag("menu_toggle_block_user")
                                )
                            }
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
                        .background(activeThemePalette.chatBackgroundColor)
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
                            color = activeThemePalette.headerAccentColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "নিচের বক্সে আপনার পয়গাম লিখে পাঠান। পাঠানো পয়গাম সাথে সাথে রিয়েল-টাইমে প্রাপকের ইনবক্সে পৌঁছে যাবে।",
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
                        .background(activeThemePalette.chatBackgroundColor)
                        .padding(horizontal = 12.dp)
                        .testTag("conversation_messages_list"),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = activeConversationMessages,
                        key = { msg -> msg.id.ifBlank { "${msg.senderId}_${msg.receiverId}_${msg.createdAt?.seconds ?: 0}_${msg.body.hashCode()}" } }
                    ) { chatMsg ->
                        val sentByMe = chatMsg.senderId == currentUserId
                        val senderReactionObj = IslamicReaction.fromCode(chatMsg.senderReaction)
                        val receiverReactionObj = IslamicReaction.fromCode(chatMsg.receiverReaction)
                        val activeReactions = listOfNotNull(
                            senderReactionObj?.let { "প্রেরক" to it },
                            receiverReactionObj?.let { "প্রাপক" to it }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_item_${chatMsg.id}"),
                            horizontalAlignment = if (sentByMe) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = if (sentByMe) Arrangement.End else Arrangement.Start
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
                                    color = if (chatMsg.isDeletedForEveryone) {
                                        Color(0xFFECEFF1)
                                    } else if (sentByMe) {
                                        activeThemePalette.sentBubbleColor
                                    } else {
                                        activeThemePalette.receivedBubbleColor
                                    },
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth(0.82f)
                                        .border(
                                            width = 1.dp,
                                            color = if (chatMsg.isDeletedForEveryone) {
                                                Color(0xFFCFD8DC)
                                            } else if (sentByMe) {
                                                activeThemePalette.sentBubbleColor
                                            } else {
                                                QawmiMintBorder
                                            },
                                            shape = RoundedCornerShape(
                                                topStart = 14.dp,
                                                topEnd = 14.dp,
                                                bottomStart = if (sentByMe) 14.dp else 3.dp,
                                                bottomEnd = if (sentByMe) 3.dp else 14.dp
                                            )
                                        )
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = { selectedMessageForAction = chatMsg }
                                        )
                                        .testTag("chat_message_bubble_${chatMsg.id}")
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                if (chatMsg.subject.isNotBlank() && chatMsg.subject != "খেদমত সংক্রান্ত যোগাযোগ" && !chatMsg.isDeletedForEveryone) {
                                                    Text(
                                                        text = "বিষয়: ${chatMsg.subject}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (sentByMe) QawmiGold else activeThemePalette.headerAccentColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                }
                                                Text(
                                                    text = if (chatMsg.isDeletedForEveryone) {
                                                        "এই পয়গামটি প্রত্যাহার করা হয়েছে"
                                                    } else {
                                                        chatMsg.body
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (chatMsg.isDeletedForEveryone) {
                                                        QawmiTextMuted
                                                    } else if (sentByMe) {
                                                        activeThemePalette.sentBubbleTextColor
                                                    } else {
                                                        QawmiTextDark
                                                    }
                                                )
                                            }
                                            IconButton(
                                                onClick = { selectedMessageForAction = chatMsg },
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .testTag("message_options_button_${chatMsg.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "পয়গাম অপশন",
                                                    tint = if (chatMsg.isDeletedForEveryone) {
                                                        QawmiTextMuted
                                                    } else if (sentByMe) {
                                                        Color(0xFFDDF3EC)
                                                    } else {
                                                        QawmiTextMuted
                                                    },
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (chatMsg.isEdited && !chatMsg.isDeletedForEveryone) {
                                                Text(
                                                    text = "সম্পাদিত • ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (sentByMe) QawmiGold else activeThemePalette.headerAccentColor,
                                                    modifier = Modifier.testTag("message_edited_badge_${chatMsg.id}")
                                                )
                                            }
                                            Text(
                                                text = buildString {
                                                    append(IslamicDateAndPrayerHelper.formatRelativeTimeBangla(chatMsg.createdAt))
                                                    if (sentByMe) {
                                                        append(if (chatMsg.isRead) " • ✓✓ পঠিত" else " • ✓ প্রেরিত")
                                                    }
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = if (chatMsg.isDeletedForEveryone) {
                                                    QawmiTextMuted
                                                } else if (sentByMe) {
                                                    Color(0xFFDDF3EC)
                                                } else {
                                                    QawmiTextMuted
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Islamic Reaction Badges below the bubble (distinctive pill layout, not Messenger clone)
                            if (activeReactions.isNotEmpty() && !chatMsg.isDeletedForEveryone) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(horizontal = if (sentByMe) 4.dp else 34.dp)
                                        .testTag("message_reactions_row_${chatMsg.id}")
                                ) {
                                    activeReactions.forEach { (_, reaction) ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.White,
                                            shadowElevation = 1.dp,
                                            modifier = Modifier
                                                .border(1.dp, QawmiGold, RoundedCornerShape(50))
                                                .clickable { selectedMessageForAction = chatMsg }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = reaction.emoji, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = reaction.titleBangla,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = activeThemePalette.headerAccentColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
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

            // 2.5 REAL-TIME TYPING INDICATOR BUBBLE
            if (isPartnerTyping && !isBlockedByMe && !isBlockedByPartner) {
                MessengerTypingIndicatorBubble(
                    partnerName = resolvedPartnerName,
                    partnerPhotoUrl = resolvedPartnerPhoto
                )
            }

            // 3. STICKY BOTTOM TEXT MESSAGE COMPOSER BAR OR BLOCKED STATUS BANNER
            if (isBlockedByMe || isBlockedByPartner) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = QawmiMintBorder)
                        .testTag("blocked_conversation_banner"),
                    color = Color(0xFFFFF8F6),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBlockedByMe) {
                                    "আপনি এই ব্যবহারকারীর সাথে পয়গাম আদান-প্রদান বিরত (ব্লক) রেখেছেন"
                                } else {
                                    "এই কথোপকথনে বর্তমানে পয়গাম প্রেরণ সীমাবদ্ধ রয়েছে"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = QawmiAlertRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBlockedByMe) {
                                    "পুনরায় পয়গাম পাঠাতে নিচের আনব্লক বাটনে কিংবা উপরের থ্রি-ডট মেনুতে চাপ দিন।"
                                } else {
                                    "প্রাপক যোগাযোগ পুনরায় চালু করলে আপনি পয়গাম পাঠাতে পারবেন।"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = QawmiTextSecondary
                            )
                        }
                        if (isBlockedByMe) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    localBlockOverrides = localBlockOverrides + (receiverId to false)
                                    onToggleBlockUser(receiverId, false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("unblock_partner_button")
                            ) {
                                Text("আনব্লক করুন", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else {
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
                            onValueChange = { newText ->
                                body = newText
                            },
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
                                    val messageToSend = body
                                    body = ""
                                    onTypingStatusChanged(receiverId, false)
                                    onSendMessage(receiverId, resolvedPartnerName, subject, messageToSend)
                                }
                            },
                            enabled = !isSubmitting && body.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = activeThemePalette.sentBubbleColor),
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

                                                val presenceBadge = if (thread.isPartnerTyping) {
                                                    "টাইপ করছেন… • • •"
                                                } else if (thread.isPartnerOnline) {
                                                    "অনলাইন"
                                                } else if (thread.partnerLastSeenAt != null) {
                                                    "সর্বশেষ সক্রিয়: ${IslamicDateAndPrayerHelper.formatRelativeTimeBangla(thread.partnerLastSeenAt)}"
                                                } else {
                                                    "অফলাইন"
                                                }

                                                Text(
                                                    text = presenceBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (thread.isPartnerTyping || thread.isPartnerOnline) Color(0xFF1B5E20) else QawmiTextMuted,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (thread.isPartnerTyping || thread.isPartnerOnline) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.testTag("thread_presence_${thread.partnerId}")
                                                )

                                                Spacer(modifier = Modifier.height(3.dp))

                                                Text(
                                                    text = if (thread.isPartnerTyping) {
                                                        "টাইপ করছেন… • • •"
                                                    } else if (isLatestSentByMe) {
                                                        "আপনি: ${latestMsg.body}"
                                                    } else {
                                                        latestMsg.body
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (thread.isPartnerTyping) QawmiDeepGreen else QawmiTextDark,
                                                    fontWeight = if (thread.unreadCount > 0 || thread.isPartnerTyping) FontWeight.Bold else FontWeight.Normal,
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
private fun MessengerTypingIndicatorBubble(
    partnerName: String,
    partnerPhotoUrl: String
) {
    var activeDotIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400L)
            activeDotIndex = (activeDotIndex + 1) % 3
        }
    }
    val dot1Alpha = if (activeDotIndex == 0) 1f else 0.35f
    val dot2Alpha = if (activeDotIndex == 1) 1f else 0.35f
    val dot3Alpha = if (activeDotIndex == 2) 1f else 0.35f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4FAF7))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("typing_indicator_bubble"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatarImage(
            photoUrl = partnerPhotoUrl,
            displayName = partnerName,
            size = 24.dp,
            borderColor = Color(0xFF00C853),
            borderWidth = 1.dp,
            showOnlineIndicator = false
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = 3.dp,
                bottomEnd = 14.dp
            ),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.border(
                width = 1.dp,
                color = QawmiMintBorder,
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = 3.dp,
                    bottomEnd = 14.dp
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(dot1Alpha)
                        .clip(CircleShape)
                        .background(QawmiDeepGreen)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(dot2Alpha)
                        .clip(CircleShape)
                        .background(QawmiDeepGreen)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(dot3Alpha)
                        .clip(CircleShape)
                        .background(QawmiDeepGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$partnerName টাইপ করছেন…",
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
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
