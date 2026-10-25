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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChatGroup
import com.example.data.model.ReplyQuoteInfo
import com.example.data.model.TelegramNotificationLog
import com.example.data.model.UserProfile
import com.example.ui.components.ChatAttachmentImage
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.QawmiAlertRed
import com.example.ui.theme.QawmiSoftBg
import com.example.ui.util.AdminSecurityHelper
import com.example.ui.util.ProfilePhotoHelper
import com.example.ui.util.QawmiNotificationHelper
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
    val unreadCount: Int,
    val isGroup: Boolean = false,
    val groupMemberCount: Int = 0
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
    userGroups: List<ChatGroup> = emptyList(),
    initialSelectedImages: List<String> = emptyList(),
    isSubmitting: Boolean,
    onSendMessage: (receiverId: String, receiverName: String, subject: String, body: String) -> Unit,
    onSendMessageWithImages: (
        receiverId: String,
        receiverName: String,
        subject: String,
        body: String,
        imageUrls: List<String>,
        groupId: String,
        groupName: String,
        groupMemberIds: List<String>
    ) -> Unit = { recId, recName, subj, textBody, _, _, _, _ ->
        onSendMessage(recId, recName, subj, textBody)
    },
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
    onSaveConversationTheme: (partnerUserId: String, themeSpec: String) -> Unit = { _, _ -> },
    onCreateGroup: (
        name: String,
        photoUrl: String,
        initialMemberIds: List<String>,
        initialMemberNames: Map<String, String>
    ) -> Unit = { _, _, _, _ -> },
    onUpdateGroupDetails: (groupId: String, newName: String, newPhotoUrl: String) -> Unit = { _, _, _ -> },
    onAddGroupMember: (groupId: String, memberId: String, memberName: String) -> Unit = { _, _, _ -> },
    onRemoveGroupMember: (groupId: String, memberId: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var receiverId by remember(targetUser) { mutableStateOf(targetUser?.first.orEmpty()) }
    var receiverName by remember(targetUser) { mutableStateOf(targetUser?.second.orEmpty()) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    // 1-5 selected images before sending (supports preview and removing individual images)
    var selectedImages by remember(initialSelectedImages) {
        mutableStateOf(initialSelectedImages.take(ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE))
    }
    var photoLimitWarning by remember { mutableStateOf<String?>(null) }

    // State for Long-Press Message Action Sheet, Edit Dialog, Delete Confirmation, Reaction Details, Reply, Emoji Picker, 3-Dot Menu, Theme Picker, Group Dialogs, and Image Viewer
    var selectedMessageForAction by remember { mutableStateOf<DirectMessage?>(null) }
    var messageBeingEdited by remember { mutableStateOf<DirectMessage?>(null) }
    var editedBodyText by remember { mutableStateOf("") }
    var messagePendingDeleteDialog by remember { mutableStateOf<DirectMessage?>(null) }
    var messageForReactionDetails by remember { mutableStateOf<DirectMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var showInboxSettingsMenu by remember { mutableStateOf(false) }
    var hideMessagePreviewInNotif by remember {
        mutableStateOf(QawmiNotificationHelper.isMessagePreviewHidden(context))
    }
    var inboxSearchQuery by remember { mutableStateOf("") }
    var inboxFilterTab by remember { mutableStateOf(0) } // 0 = সব, 1 = ব্যক্তিগত, 2 = গ্রুপ, 3 = অপঠিত
    var showThemePickerDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showGroupManagementDialog by remember { mutableStateOf(false) }
    var fullScreenViewerImages by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var fullScreenViewerSourceMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var imageSaveFeedback by remember { mutableStateOf<String?>(null) }
    var transientActionToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(transientActionToast) {
        if (!transientActionToast.isNullOrBlank()) {
            delay(2600L)
            transientActionToast = null
        }
    }

    // Optimistic local state for immediate UI responsiveness during tests and real-time updates
    var localThemeOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var localBlockOverrides by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var locallyDeletedForMeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var localReactionOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var localEditedBodies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var locallyDeletedForEveryoneIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var localGroupsMap by remember { mutableStateOf<Map<String, ChatGroup>>(emptyMap()) }

    // Merge server groups with optimistic local groups
    val effectiveGroups = remember(userGroups, localGroupsMap) {
        val merged = userGroups.associateBy { it.id }.toMutableMap()
        localGroupsMap.forEach { (id, grp) -> merged[id] = grp }
        merged.values.toList()
    }

    // Multi-photo picker (1-5 images max) for chat composer
    val chatMultiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remainingSlots = (ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE - selectedImages.size).coerceAtLeast(0)
            if (remainingSlots <= 0) {
                photoLimitWarning = "একবারে সর্বোচ্চ ৫টি ছবি নির্বাচন করা যাবে।"
            } else {
                val updatedList = ProfilePhotoHelper.processSelectedChatImages(
                    context = context,
                    uris = uris.take(remainingSlots),
                    existingImages = selectedImages
                )
                if (uris.size > remainingSlots) {
                    photoLimitWarning = "সর্বোচ্চ ৫টি ছবি নির্বাচন করা হয়েছে (৫টির বেশি নির্বাচন করা যাবে না)।"
                } else {
                    photoLimitWarning = null
                }
                selectedImages = updatedList.take(ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE)
            }
        }
    }

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
                    (msg.receiverId == currentUserId && msg.deletedForReceiver) ||
                    msg.deletedForUserIds.contains(currentUserId)
            }
            .distinctBy { msg ->
                msg.id.ifBlank {
                    "${msg.senderId}_${msg.receiverId}_${msg.createdAt?.seconds ?: 0}_${msg.createdAt?.nanoseconds ?: 0}_${msg.body}"
                }
            }
            .map { msg ->
                var updated = msg
                localReactionOverrides[msg.id]?.let { reaction ->
                    val updatedUserReactions = updated.userReactions.toMutableMap()
                    if (reaction.isBlank()) {
                        updatedUserReactions.remove(currentUserId)
                    } else {
                        updatedUserReactions[currentUserId] = reaction
                    }
                    updated = if (msg.senderId == currentUserId) {
                        updated.copy(senderReaction = reaction, userReactions = updatedUserReactions)
                    } else {
                        updated.copy(receiverReaction = reaction, userReactions = updatedUserReactions)
                    }
                }
                localEditedBodies[msg.id]?.let { newBody ->
                    updated = updated.copy(body = newBody, isEdited = true)
                }
                if (locallyDeletedForEveryoneIds.contains(msg.id)) {
                    updated = updated.copy(
                        body = "এই পয়গামটি প্রত্যাহার করা হয়েছে",
                        imageUrls = emptyList(),
                        isDeletedForEveryone = true,
                        senderReaction = "",
                        receiverReaction = "",
                        userReactions = emptyMap()
                    )
                }
                updated
            }
    }

    val totalUnreadMessagesCount = remember(allMessages, currentUserId) {
        allMessages.count { it.isUnreadForUser(currentUserId) && !it.isDeletedForEveryone }
    }

    // Group messages into Conversation Threads (both 1-on-1 and Group conversations)
    val conversationThreads = remember(allMessages, currentUserId, partnerProfiles, effectiveGroups) {
        val directThreads = allMessages
            .filter { it.groupId.isBlank() && !it.receiverId.startsWith("GROUP:") }
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
                    it.isUnreadForUser(currentUserId) && !it.isDeletedForEveryone
                }

                ConversationSummary(
                    partnerId = partnerId,
                    partnerName = liveProfile?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName,
                    partnerPhotoUrl = liveProfile?.photoUrl?.takeIf { it.isNotBlank() } ?: fallbackPhoto,
                    isPartnerOnline = liveProfile?.isOnline == true,
                    isPartnerTyping = isPartnerCurrentlyTypingToMe(liveProfile, currentUserId),
                    partnerLastSeenAt = liveProfile?.lastSeenAt,
                    latestMessage = latest,
                    unreadCount = unreadFromPartner,
                    isGroup = false,
                    groupMemberCount = 2
                )
            }

        val groupMessagesByGroupId = allMessages
            .filter { it.groupId.isNotBlank() || it.receiverId.startsWith("GROUP:") }
            .groupBy { msg ->
                msg.groupId.ifBlank { msg.receiverId.removePrefix("GROUP:") }
            }

        val allGroupIds = (effectiveGroups.map { it.id } + groupMessagesByGroupId.keys)
            .filter { it.isNotBlank() }
            .distinct()

        val groupThreads = allGroupIds.mapNotNull { gId ->
            val groupObj = effectiveGroups.find { it.id == gId }
            val msgs = groupMessagesByGroupId[gId].orEmpty()
            if (groupObj == null && msgs.isEmpty()) return@mapNotNull null
            val latest = msgs.maxByOrNull { it.createdAt ?: Timestamp(0, 0) }
                ?: DirectMessage(
                    id = "group_init_$gId",
                    senderId = groupObj?.creatorId.orEmpty(),
                    senderName = groupObj?.creatorName.orEmpty(),
                    receiverId = "GROUP:$gId",
                    receiverName = groupObj?.name.orEmpty(),
                    groupId = gId,
                    groupName = groupObj?.name.orEmpty(),
                    body = "গ্রুপ কথোপকথন শুরু করুন",
                    isRead = true,
                    createdAt = groupObj?.updatedAt ?: groupObj?.createdAt
                )
            val groupDisplayName = groupObj?.name?.takeIf { it.isNotBlank() }
                ?: msgs.firstNotNullOfOrNull { it.groupName.takeIf { n -> n.isNotBlank() } }
                ?: "দ্বীনি মুযাকারা গ্রুপ"
            val groupPhoto = groupObj?.photoUrl.orEmpty()
            val memberCount = groupObj?.memberIds?.size
                ?: msgs.firstOrNull()?.groupMemberIds?.size
                ?: 1
            val unreadInGroup = msgs.count {
                it.isUnreadForUser(currentUserId) && !it.isDeletedForEveryone
            }

            ConversationSummary(
                partnerId = "GROUP:$gId",
                partnerName = groupDisplayName,
                partnerPhotoUrl = groupPhoto,
                isPartnerOnline = true,
                isPartnerTyping = false,
                partnerLastSeenAt = latest.createdAt,
                latestMessage = latest,
                unreadCount = unreadInGroup,
                isGroup = true,
                groupMemberCount = memberCount
            )
        }

        (groupThreads + directThreads).sortedByDescending { it.latestMessage.createdAt ?: Timestamp(0, 0) }
    }

    val filteredConversationThreads = remember(conversationThreads, inboxSearchQuery, inboxFilterTab) {
        val query = inboxSearchQuery.trim()
        conversationThreads
            .filter { thread ->
                when (inboxFilterTab) {
                    1 -> !thread.isGroup
                    2 -> thread.isGroup
                    3 -> thread.unreadCount > 0
                    else -> true
                }
            }
            .filter { thread ->
                if (query.isBlank()) true
                else {
                    thread.partnerName.contains(query, ignoreCase = true) ||
                        thread.latestMessage.body.contains(query, ignoreCase = true) ||
                        thread.latestMessage.senderName.contains(query, ignoreCase = true)
                }
            }
    }

    val isGroupConversation = receiverId.startsWith("GROUP:", ignoreCase = true)
    val activeGroupId = if (isGroupConversation) receiverId.substringAfter(":", receiverId) else ""
    val activeGroup: ChatGroup? = remember(isGroupConversation, activeGroupId, effectiveGroups, receiverName, currentUserId) {
        if (!isGroupConversation) null
        else effectiveGroups.find { it.id == activeGroupId } ?: ChatGroup(
            id = activeGroupId,
            name = receiverName.ifBlank { "দ্বীনি মুযাকারা গ্রুপ" },
            creatorId = currentUserId,
            memberIds = listOf(currentUserId)
        )
    }

    // Active conversation messages sorted chronologically (oldest -> newest) for two-way or group chat view
    val activeConversationMessages = remember(allMessages, currentUserId, receiverId, isGroupConversation, activeGroupId) {
        if (receiverId.isBlank()) {
            emptyList()
        } else if (isGroupConversation) {
            allMessages
                .filter { msg ->
                    msg.groupId == activeGroupId || msg.receiverId.equals("GROUP:$activeGroupId", ignoreCase = true)
                }
                .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        } else {
            allMessages
                .filter {
                    it.groupId.isBlank() && (
                        (it.senderId == currentUserId && it.receiverId == receiverId) ||
                            (it.senderId == receiverId && it.receiverId == currentUserId)
                        )
                }
                .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    // Automatically mark unread incoming messages in the open conversation as read (both 1-on-1 and Group)
    val unreadInActiveChatCount = remember(activeConversationMessages, receiverId, currentUserId) {
        if (receiverId.isBlank()) 0
        else activeConversationMessages.count {
            it.isUnreadForUser(currentUserId) && !it.isDeletedForEveryone
        }
    }

    LaunchedEffect(receiverId, unreadInActiveChatCount, isGroupConversation) {
        if (receiverId.isNotBlank()) {
            QawmiNotificationHelper.cancelConversationNotification(context, receiverId, currentUserId)
            if (unreadInActiveChatCount > 0) {
                if (!isGroupConversation) {
                    activeConversationMessages
                        .filter { it.senderId == receiverId && it.receiverId == currentUserId && !it.isRead && it.id.isNotBlank() }
                        .forEach { unreadMsg -> onMarkRead(unreadMsg.id) }
                }
                onMarkConversationRead(receiverId)
            }
        }
    }

    // Real-time typing status broadcast to Firestore with auto-timeout (for 1-on-1 chats)
    LaunchedEffect(receiverId, body, isGroupConversation) {
        if (receiverId.isNotBlank() && !isGroupConversation) {
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
            if (activeReceiver.isNotBlank() && !activeReceiver.startsWith("GROUP:")) {
                onTypingStatusChanged(activeReceiver, false)
            }
        }
    }

    val resolvedPartnerProfile = if (isGroupConversation) null else (activePartnerProfile ?: partnerProfiles[receiverId])
    val resolvedPartnerName = if (isGroupConversation) {
        activeGroup?.name?.takeIf { it.isNotBlank() } ?: receiverName.ifBlank { "দ্বীনি মুযাকারা গ্রুপ" }
    } else {
        resolvedPartnerProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: receiverName.ifBlank { "সম্মানিত প্রাপক" }
    }
    val resolvedPartnerPhoto = if (isGroupConversation) {
        activeGroup?.photoUrl.orEmpty()
    } else {
        resolvedPartnerProfile?.photoUrl.orEmpty()
    }
    val isPartnerOnline = if (isGroupConversation) true else (resolvedPartnerProfile?.isOnline == true)
    val isPartnerTyping = if (isGroupConversation) false else isPartnerCurrentlyTypingToMe(resolvedPartnerProfile, currentUserId)
    val partnerStatusText = remember(isGroupConversation, activeGroup, isPartnerTyping, isPartnerOnline, resolvedPartnerProfile?.lastSeenAt) {
        if (isGroupConversation) {
            val count = activeGroup?.memberIds?.size ?: 1
            "গ্রুপ কথোপকথন • সদস্য: ${IslamicDateAndPrayerHelper.toBanglaDigits(count.toString())} জন"
        } else if (isPartnerTyping) {
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

    // Resolve block status (either blocked by current user or blocked by partner; applies to 1-on-1)
    val isBlockedByMe = !isGroupConversation && (
        localBlockOverrides[receiverId]
            ?: (receiverId.isNotBlank() && currentUserProfile?.blockedUserIds?.contains(receiverId) == true)
        )
    val isBlockedByPartner = !isGroupConversation && receiverId.isNotBlank() &&
        resolvedPartnerProfile?.blockedUserIds?.contains(currentUserId) == true

    val chatListState = rememberLazyListState()
    var lastOpenedConversationId by remember { mutableStateOf("") }

    // Smart auto-scroll: scroll to bottom when opening a conversation, when sending a message,
    // or when already near the bottom of the list (do not yank user while reading older messages)
    LaunchedEffect(receiverId, activeConversationMessages.size) {
        if (receiverId.isNotBlank() && activeConversationMessages.isNotEmpty()) {
            val isOpeningConversation = lastOpenedConversationId != receiverId
            val latestMsg = activeConversationMessages.lastOrNull()
            val isLatestSentByMe = latestMsg?.senderId == currentUserId
            val lastVisibleIdx = chatListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val isNearBottom = lastVisibleIdx >= (activeConversationMessages.lastIndex - 3).coerceAtLeast(0)
            if (isOpeningConversation || isLatestSentByMe || isNearBottom || activeConversationMessages.size <= 3) {
                chatListState.scrollToItem(activeConversationMessages.lastIndex)
                lastOpenedConversationId = receiverId
            }
        }
    }

    // Back button inside an active conversation returns to the Paygam thread list
    BackHandler(enabled = receiverId.isNotBlank()) {
        if (receiverId.isNotBlank() && !isGroupConversation) {
            onTypingStatusChanged(receiverId, false)
        }
        receiverId = ""
        receiverName = ""
        selectedImages = emptyList()
        replyingToMessage = null
        showEmojiPicker = false
        onClearRecipient()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    // =========================================================================
    // 1. SLEEK LONG-PRESS REACTION BAR (👍 ❤️ 😂 😮 😢 🙏 + 🤲 🌙 ✨ 🕌) + DARK EMERALD CONTEXT MENU
    // =========================================================================
    selectedMessageForAction?.let { targetMsg ->
        val isMyMessage = targetMsg.senderId == currentUserId
        val mergedReactions = targetMsg.mergedUserReactions()
        val myCurrentReaction = mergedReactions[currentUserId]
            ?: if (isMyMessage) targetMsg.senderReaction else targetMsg.receiverReaction

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .background(Color(0x88000000))
                .clickable(onClick = { selectedMessageForAction = null })
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {})
                    .testTag("message_action_sheet"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sleek side-by-side Reaction Symbols ONLY (👍 ❤️ 😂 😮 😢 🙏 + Islamic reactions; no text labels)
                if (!targetMsg.isDeletedForEveryone) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .border(1.5.dp, QawmiGold, RoundedCornerShape(24.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IslamicReaction.entries.chunked(6).forEach { rowReactions ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowReactions.forEach { reaction ->
                                        val isSelected = myCurrentReaction.equals(reaction.code, ignoreCase = true) ||
                                            IslamicReaction.resolveEmoji(myCurrentReaction) == reaction.emoji
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) QawmiMintSurface else Color.Transparent
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
                                            Text(
                                                text = reaction.emoji,
                                                fontSize = 21.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modern Dark Emerald Context Menu (matching reference image: উত্তর দিন, কপি করুন, এডিট করুন, ডিলিট করুন)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0A3326),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiGold.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!targetMsg.isDeletedForEveryone) {
                            // উত্তর দিন (Reply)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF114B38),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        replyingToMessage = targetMsg
                                        selectedMessageForAction = null
                                    }
                                    .testTag("action_reply_message")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "উত্তর দিন",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // কপি করুন (Copy)
                            if (targetMsg.body.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF114B38),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val copied = copyMessageTextToClipboard(context, targetMsg.body)
                                            if (copied) {
                                                transientActionToast = "পয়গাম কপি করা হয়েছে"
                                            }
                                            selectedMessageForAction = null
                                        }
                                        .testTag("action_copy_message")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "কপি করুন",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // এডিট করুন (Edit own message)
                        if (isMyMessage && !targetMsg.isDeletedForEveryone) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF114B38),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMessageForAction = null
                                        messageBeingEdited = targetMsg
                                        editedBodyText = targetMsg.body
                                    }
                                    .testTag("action_edit_message")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = QawmiGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "এডিট করুন (পয়গাম সম্পাদনা)",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // ডিলিট করুন (Opens Confirmation Dialog with Radio Options)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF114B38),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMessageForAction = null
                                    messagePendingDeleteDialog = targetMsg
                                }
                                .testTag("action_open_delete_dialog")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8A80),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ডিলিট করুন (নিশ্চিতকরণ অপশন)",
                                    color = Color(0xFFFFCDD2),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0x33FFFFFF))

                        // Quick Direct Delete Buttons (also preserves backward compatibility with existing tests)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (targetMsg.id.isNotBlank()) {
                                        locallyDeletedForMeIds = locallyDeletedForMeIds + targetMsg.id
                                    }
                                    onDeleteMessageForMe(targetMsg)
                                    selectedMessageForAction = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("action_delete_for_me")
                            ) {
                                Text(
                                    text = "শুধু আমার জন্য মুছুন",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

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
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("action_delete_for_everyone")
                                ) {
                                    Text(
                                        text = "সবার জন্য মুছুন",
                                        color = Color.White,
                                        fontSize = 11.sp,
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

    // =========================================================================
    // 1.5 REACTION DETAILS DIALOG (রিঅ্যাকশন • মোট X জন)
    // =========================================================================
    messageForReactionDetails?.let { targetMsg ->
        PaygamReactionDetailsDialog(
            message = targetMsg,
            currentUserId = currentUserId,
            currentUserProfile = currentUserProfile,
            partnerProfiles = partnerProfiles,
            activeGroup = activeGroup,
            onRemoveOwnReaction = {
                if (targetMsg.id.isNotBlank()) {
                    localReactionOverrides = localReactionOverrides + (targetMsg.id to "")
                }
                onReactToMessage(targetMsg, "")
            },
            onDismiss = { messageForReactionDetails = null }
        )
    }

    // =========================================================================
    // 1.8 DELETE CONFIRMATION DIALOG WITH RADIO OPTIONS (মেসেজ ডিলিট করুন)
    // =========================================================================
    messagePendingDeleteDialog?.let { targetMsg ->
        PaygamDeleteConfirmationDialog(
            message = targetMsg,
            isMyMessage = targetMsg.senderId == currentUserId,
            onDeleteForMe = {
                if (targetMsg.id.isNotBlank()) {
                    locallyDeletedForMeIds = locallyDeletedForMeIds + targetMsg.id
                }
                onDeleteMessageForMe(targetMsg)
            },
            onDeleteForEveryone = {
                if (targetMsg.id.isNotBlank()) {
                    locallyDeletedForEveryoneIds = locallyDeletedForEveryoneIds + targetMsg.id
                }
                onDeleteMessageForEveryone(targetMsg)
            },
            onDismiss = { messagePendingDeleteDialog = null }
        )
    }

    // =========================================================================
    // 2. MESSAGE EDIT DIALOG (মেসেজ এডিট করুন + X/1000 counter)
    // =========================================================================
    messageBeingEdited?.let { editingMsg ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .background(Color(0x88000000))
                .clickable(onClick = { messageBeingEdited = null })
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {})
                    .testTag("edit_message_dialog"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "মেসেজ এডিট করুন",
                                style = MaterialTheme.typography.titleMedium,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { messageBeingEdited = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "বন্ধ করুন",
                                tint = QawmiTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "সম্পাদিত মেসেজে 'সম্পাদিত' লেখা প্রদর্শিত হবে এবং রিয়েল-টাইমে আপডেট হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                    OutlinedTextField(
                        value = editedBodyText,
                        onValueChange = { if (it.length <= 1000) editedBodyText = it },
                        label = { Text("সংশোধিত মেসেজ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_message_input"),
                        maxLines = 5
                    )
                    Text(
                        text = "${IslamicDateAndPrayerHelper.toBanglaDigits(editedBodyText.length.toString())}/১০০০",
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiTextMuted,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { messageBeingEdited = null }) {
                            Text("বাতিল", color = QawmiTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                            Text("সংরক্ষণ")
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 3. CONVERSATION THEME & CUSTOM THEME PICKER DIALOG
    // =========================================================================
    if (showThemePickerDialog && receiverId.isNotBlank()) {
        var customBgHex by remember { mutableStateOf("#EEF7F2") }
        var customBubbleHex by remember { mutableStateOf("#115E46") }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .background(Color(0x88000000))
                .clickable(onClick = { showThemePickerDialog = false })
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {})
                    .testTag("chat_theme_picker_dialog"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showThemePickerDialog = false }) {
                            Text("বন্ধ করুন", color = QawmiDeepGreen)
                        }
                    }
                }
            }
        }
    }
    // =========================================================================
    // 4. CREATE GROUP DIALOG (গ্রুপ তৈরি, নাম ও ছবি নির্ধারণ, সদস্য নির্বাচন)
    // =========================================================================
    if (showCreateGroupDialog) {
        var newGroupName by remember { mutableStateOf("") }
        var newGroupPhotoUrl by remember { mutableStateOf(ProfilePhotoHelper.PRESET_MADRASA_DOME) }
        var selectedMembers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var customMemberName by remember { mutableStateOf("") }

        val groupPhotoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val encoded = ProfilePhotoHelper.encodeUriToDataUrl(context, uri)
                if (!encoded.isNullOrBlank()) {
                    newGroupPhotoUrl = encoded
                }
            }
        }

        val knownContacts = remember(partnerProfiles, allMessages, currentUserId) {
            val map = mutableMapOf<String, String>()
            partnerProfiles.forEach { (uid, prof) ->
                if (uid != currentUserId && !uid.startsWith("GROUP:") && prof.fullName.isNotBlank()) {
                    map[uid] = prof.fullName
                }
            }
            allMessages.forEach { msg ->
                if (msg.senderId != currentUserId && msg.senderId.isNotBlank() && msg.senderName.isNotBlank()) {
                    map.putIfAbsent(msg.senderId, msg.senderName)
                }
                if (msg.receiverId != currentUserId && msg.receiverId.isNotBlank() && !msg.receiverId.startsWith("GROUP:") && msg.receiverName.isNotBlank()) {
                    map.putIfAbsent(msg.receiverId, msg.receiverName)
                }
            }
            map
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .background(Color(0x88000000))
                .clickable(onClick = { showCreateGroupDialog = false })
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {})
                    .testTag("create_group_dialog"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "নতুন গ্রুপ কথোপকথন খুলুন",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )

                    // Group Avatar & Photo Picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatarImage(
                            photoUrl = newGroupPhotoUrl,
                            displayName = newGroupName.ifBlank { "গ্রুপ" },
                            size = 56.dp,
                            borderColor = QawmiGold,
                            borderWidth = 2.dp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    groupPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("pick_group_photo_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = QawmiDeepGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("গ্রুপের ছবি নির্বাচন করুন", style = MaterialTheme.typography.labelSmall, color = QawmiDeepGreen)
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProfilePhotoHelper.presetAvatars.forEach { (presetKey, presetLabel) ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (newGroupPhotoUrl == presetKey) QawmiMintSurface else Color(0xFFF5F5F5),
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                if (newGroupPhotoUrl == presetKey) QawmiDeepGreen else QawmiMintBorder,
                                                RoundedCornerShape(50)
                                            )
                                            .clickable { newGroupPhotoUrl = presetKey }
                                    ) {
                                        Text(
                                            text = presetLabel,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            color = QawmiDeepGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("গ্রুপের নাম *") },
                        placeholder = { Text("যেমন: দাওরায়ে হাদিস মুযাকারা") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input")
                    )

                    Text(
                        text = "গ্রুপে সদস্য যুক্ত করুন:",
                        style = MaterialTheme.typography.labelMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )

                    if (knownContacts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            knownContacts.entries.forEach { (uid, uName) ->
                                val isAdded = selectedMembers.containsKey(uid)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isAdded) QawmiMintSurface else Color(0xFFF9FBF9),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, if (isAdded) QawmiDeepGreen else QawmiMintBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedMembers = if (isAdded) {
                                                selectedMembers - uid
                                            } else {
                                                selectedMembers + (uid to uName)
                                            }
                                        }
                                        .testTag("select_group_member_$uid")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(uName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = QawmiTextDark)
                                        Text(
                                            text = if (isAdded) "✓ যুক্ত" else "+ যুক্ত করুন",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isAdded) QawmiDeepGreen else QawmiSeekerBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customMemberName,
                            onValueChange = { customMemberName = it },
                            label = { Text("সদস্যের নাম লিখে যুক্ত করুন") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_group_member_name_input")
                        )
                        Button(
                            onClick = {
                                val trimmedName = customMemberName.trim()
                                if (trimmedName.isNotBlank()) {
                                    val generatedId = "member_${trimmedName.hashCode().toUInt()}"
                                    selectedMembers = selectedMembers + (generatedId to trimmedName)
                                    customMemberName = ""
                                }
                            },
                            enabled = customMemberName.trim().isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_candidate_member_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (selectedMembers.isNotEmpty()) {
                        Text(
                            text = "নির্বাচিত সদস্য (${IslamicDateAndPrayerHelper.toBanglaDigits(selectedMembers.size.toString())} জন): ${selectedMembers.values.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateGroupDialog = false }) {
                            Text("বাতিল", color = QawmiTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmedGroupName = newGroupName.trim()
                                if (trimmedGroupName.isNotBlank()) {
                                    val myName = currentUserProfile?.fullName?.takeIf { it.isNotBlank() } ?: "গ্রুপ অ্যাডমিন"
                                    val allMemberIds = (listOf(currentUserId) + selectedMembers.keys).distinct()
                                    val allMemberNames = selectedMembers + (currentUserId to myName)
                                    val tempGroupId = "grp_${System.currentTimeMillis()}"
                                    val optimisticGroup = ChatGroup(
                                        id = tempGroupId,
                                        name = trimmedGroupName,
                                        photoUrl = newGroupPhotoUrl,
                                        creatorId = currentUserId,
                                        creatorName = myName,
                                        memberIds = allMemberIds,
                                        memberNames = allMemberNames,
                                        createdAt = Timestamp.now(),
                                        updatedAt = Timestamp.now()
                                    )
                                    localGroupsMap = localGroupsMap + (tempGroupId to optimisticGroup)
                                    receiverId = "GROUP:$tempGroupId"
                                    receiverName = trimmedGroupName
                                    onCreateGroup(trimmedGroupName, newGroupPhotoUrl, allMemberIds, allMemberNames)
                                    showCreateGroupDialog = false
                                }
                            },
                            enabled = newGroupName.trim().isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                            modifier = Modifier.testTag("confirm_create_group_button")
                        ) {
                            Text("গ্রুপ তৈরি করুন")
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 5. GROUP MANAGEMENT DIALOG (নাম ও ছবি পরিবর্তন, সদস্য যুক্ত ও বাদ দেওয়া)
    // =========================================================================
    if (showGroupManagementDialog && isGroupConversation && activeGroup != null) {
        var editedGroupName by remember(activeGroup) { mutableStateOf(activeGroup.name) }
        var editedGroupPhoto by remember(activeGroup) { mutableStateOf(activeGroup.photoUrl) }
        var newMemberNameInput by remember { mutableStateOf("") }

        val editGroupPhotoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val encoded = ProfilePhotoHelper.encodeUriToDataUrl(context, uri)
                if (!encoded.isNullOrBlank()) {
                    editedGroupPhoto = encoded
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .background(Color(0x88000000))
                .clickable(onClick = { showGroupManagementDialog = false })
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false, onClick = {})
                    .testTag("group_management_dialog"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "গ্রুপ পরিচালনা ও সদস্য তালিকা",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserAvatarImage(
                            photoUrl = editedGroupPhoto,
                            displayName = editedGroupName,
                            size = 54.dp,
                            borderColor = QawmiGold,
                            borderWidth = 2.dp
                        )
                        OutlinedButton(
                            onClick = {
                                editGroupPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("change_group_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = QawmiDeepGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("গ্রুপ ছবি পরিবর্তন", style = MaterialTheme.typography.labelSmall, color = QawmiDeepGreen)
                        }
                    }

                    OutlinedTextField(
                        value = editedGroupName,
                        onValueChange = { editedGroupName = it },
                        label = { Text("গ্রুপের নাম") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_group_name_input")
                    )

                    Button(
                        onClick = {
                            val trimmed = editedGroupName.trim()
                            if (trimmed.isNotBlank()) {
                                val updatedGrp = activeGroup.copy(name = trimmed, photoUrl = editedGroupPhoto)
                                localGroupsMap = localGroupsMap + (activeGroup.id to updatedGrp)
                                receiverName = trimmed
                                onUpdateGroupDetails(activeGroup.id, trimmed, editedGroupPhoto)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_group_details_button")
                    ) {
                        Text("নাম ও ছবি সংরক্ষণ করুন")
                    }

                    HorizontalDivider(color = QawmiMintBorder)

                    Text(
                        text = "বর্তমান সদস্যবৃন্দ (${IslamicDateAndPrayerHelper.toBanglaDigits(activeGroup.memberIds.size.toString())} জন):",
                        style = MaterialTheme.typography.labelMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )

                    activeGroup.memberIds.forEach { memberId ->
                        val memberName = activeGroup.memberNames[memberId]
                            ?: partnerProfiles[memberId]?.fullName
                            ?: if (memberId == currentUserId) (currentUserProfile?.fullName ?: "আমি") else memberId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF7FAF8),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (memberId == currentUserId) "$memberName (আপনি)" else memberName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = QawmiTextDark,
                                    modifier = Modifier.weight(1f)
                                )
                                if (memberId != currentUserId && activeGroup.memberIds.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val updatedIds = activeGroup.memberIds.filterNot { it == memberId }
                                            val updatedNames = activeGroup.memberNames.toMutableMap().apply { remove(memberId) }
                                            localGroupsMap = localGroupsMap + (
                                                activeGroup.id to activeGroup.copy(
                                                    memberIds = updatedIds,
                                                    memberNames = updatedNames
                                                )
                                                )
                                            onRemoveGroupMember(activeGroup.id, memberId)
                                        },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .testTag("remove_group_member_$memberId")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonRemove,
                                            contentDescription = "সদস্য বাদ দিন",
                                            tint = QawmiAlertRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newMemberNameInput,
                            onValueChange = { newMemberNameInput = it },
                            label = { Text("নতুন সদস্যের নাম") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_member_name_input")
                        )
                        Button(
                            onClick = {
                                val mName = newMemberNameInput.trim()
                                if (mName.isNotBlank()) {
                                    val mId = partnerProfiles.entries.find { it.value.fullName == mName }?.key
                                        ?: "member_${mName.hashCode().toUInt()}"
                                    val updatedIds = (activeGroup.memberIds + mId).distinct()
                                    val updatedNames = activeGroup.memberNames + (mId to mName)
                                    localGroupsMap = localGroupsMap + (
                                        activeGroup.id to activeGroup.copy(
                                            memberIds = updatedIds,
                                            memberNames = updatedNames
                                        )
                                        )
                                    onAddGroupMember(activeGroup.id, mId, mName)
                                    newMemberNameInput = ""
                                }
                            },
                            enabled = newMemberNameInput.trim().isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("confirm_add_group_member_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "সদস্য যুক্ত করুন", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("যুক্ত করুন", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showGroupManagementDialog = false },
                            modifier = Modifier.testTag("close_group_management_button")
                        ) {
                            Text("বন্ধ করুন", color = QawmiDeepGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 6. FULL-SCREEN IMAGE VIEWER & SAVE TO GALLERY DIALOG (Messenger-style)
    // =========================================================================
    fullScreenViewerImages?.let { (images, initialIndex) ->
        val srcMsg = fullScreenViewerSourceMessage
        val viewerSenderName = srcMsg?.senderName?.takeIf { it.isNotBlank() } ?: resolvedPartnerName
        val viewerSenderPhoto = srcMsg?.let { msg ->
            if (msg.senderId == currentUserId) {
                currentUserProfile?.photoUrl.orEmpty()
            } else {
                partnerProfiles[msg.senderId]?.photoUrl?.takeIf { it.isNotBlank() }
                    ?: msg.senderPhotoUrl.ifBlank { resolvedPartnerPhoto }
            }
        } ?: resolvedPartnerPhoto
        val viewerTimestamp = srcMsg?.createdAt?.let { IslamicDateAndPrayerHelper.formatRelativeTimeBangla(it) } ?: "এখন"

        PaygamFullScreenImageViewerDialog(
            images = images,
            initialIndex = initialIndex,
            senderName = viewerSenderName,
            senderPhotoUrl = viewerSenderPhoto,
            timestampText = viewerTimestamp,
            imageSaveFeedback = imageSaveFeedback,
            onSaveSingleImage = { imgUrl, idxOneBased ->
                val res = ProfilePhotoHelper.saveImageToDevice(context, imgUrl, idxOneBased)
                imageSaveFeedback = res.getOrElse { "ছবি সংরক্ষণে ত্রুটি হয়েছে" }
            },
            onSaveAllImages = { allImgs ->
                val saved = ProfilePhotoHelper.saveMultipleImagesToDevice(context, allImgs)
                imageSaveFeedback = "আলহামদুলিল্লাহ, ${IslamicDateAndPrayerHelper.toBanglaDigits(saved.toString())}টি ছবি গ্যালারিতে সংরক্ষিত হয়েছে"
            },
            onShareImage = { imgUrl ->
                val res = ProfilePhotoHelper.saveImageToDevice(context, imgUrl, initialIndex + 1)
                imageSaveFeedback = res.fold(
                    onSuccess = { "শেয়ার করার জন্য ছবিটি গ্যালারিতে প্রস্তুত করা হয়েছে" },
                    onFailure = { "ছবি শেয়ার প্রস্তুতিতে ত্রুটি হয়েছে" }
                )
            },
            onDismiss = {
                fullScreenViewerImages = null
                fullScreenViewerSourceMessage = null
                imageSaveFeedback = null
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
        if (transientActionToast != null) {
            Surface(
                color = QawmiDeepGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transientActionToast.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { transientActionToast = null }) {
                        Text("ঠিক আছে", color = QawmiGold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (receiverId.isNotBlank()) {
            // =========================================================================
            // ACTIVE CONVERSATION VIEW:
            // 1. Clean Top Header (NO "ইনবক্স" text; tapping photo & name opens Profile or Group Info)
            // 2. Scrollable Middle Message List (Modifier.weight(1f))
            // 3. Sticky Bottom Composer with Reply Banner, Emoji Picker & Photo Picker (1-5 photos)
            // =========================================================================

            // 1. STICKY TOP PARTNER / GROUP HEADER
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
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                if (!isGroupConversation) {
                                    onTypingStatusChanged(receiverId, false)
                                }
                                receiverId = ""
                                receiverName = ""
                                selectedImages = emptyList()
                                replyingToMessage = null
                                showEmojiPicker = false
                                onClearRecipient()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("back_to_inbox_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = activeThemePalette.headerAccentColor
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        // Tapping the person's photo and name header opens their profile (or Group Management for groups)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isGroupConversation) {
                                        showGroupManagementDialog = true
                                    } else {
                                        onInspectUserProfile(receiverId, resolvedPartnerName)
                                    }
                                }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                                .testTag("partner_header_profile_click_area")
                        ) {
                            UserAvatarImage(
                                photoUrl = resolvedPartnerPhoto,
                                displayName = resolvedPartnerName,
                                size = 44.dp,
                                borderColor = if (isPartnerOnline) Color(0xFF00C853) else QawmiMintBorder,
                                borderWidth = 1.5.dp,
                                showOnlineIndicator = !isGroupConversation,
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
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isGroupConversation) {
                            IconButton(
                                onClick = { showGroupManagementDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("group_info_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "গ্রুপ সদস্য ও সেটিংস",
                                    tint = activeThemePalette.headerAccentColor
                                )
                            }
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
                                if (isGroupConversation) {
                                    HorizontalDivider(color = QawmiMintBorder)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "গ্রুপ তথ্য ও সদস্য ব্যবস্থাপনা",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Group,
                                                contentDescription = null,
                                                tint = QawmiDeepGreen
                                            )
                                        },
                                        onClick = {
                                            showThreeDotMenu = false
                                            showGroupManagementDialog = true
                                        },
                                        modifier = Modifier.testTag("menu_manage_group")
                                    )
                                } else {
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
                            showOnlineIndicator = !isGroupConversation,
                            isOnline = isPartnerOnline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$resolvedPartnerName-এ পয়গাম ও কথোপকথন শুরু করুন",
                            style = MaterialTheme.typography.titleSmall,
                            color = activeThemePalette.headerAccentColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "নিচের বক্সে পয়গাম লিখে অথবা ১-৫টি ছবি যুক্ত করে পাঠান।",
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
                        val mergedReactionsMap = chatMsg.mergedUserReactions()
                        val activeReactionEmojis = mergedReactionsMap.values
                            .map { IslamicReaction.resolveEmoji(it) }
                            .filter { it.isNotBlank() }
                            .distinct()
                        val totalReactionCount = mergedReactionsMap.size
                        val replyQuote = chatMsg.parsedReplyQuote()
                        val cleanSubject = chatMsg.displaySubject()

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
                                    val senderAvatar = if (isGroupConversation) {
                                        partnerProfiles[chatMsg.senderId]?.photoUrl?.takeIf { it.isNotBlank() }
                                            ?: chatMsg.senderPhotoUrl
                                    } else {
                                        resolvedPartnerPhoto
                                    }
                                    val senderDisplay = if (isGroupConversation) {
                                        chatMsg.senderName.ifBlank { "সদস্য" }
                                    } else {
                                        resolvedPartnerName
                                    }
                                    UserAvatarImage(
                                        photoUrl = senderAvatar,
                                        displayName = senderDisplay,
                                        size = 28.dp,
                                        borderColor = QawmiMintBorder,
                                        borderWidth = 1.dp,
                                        modifier = Modifier.clickable {
                                            if (chatMsg.senderId.isNotBlank()) {
                                                onInspectUserProfile(chatMsg.senderId, senderDisplay)
                                            }
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
                                        if (isGroupConversation && !sentByMe && chatMsg.senderName.isNotBlank()) {
                                            Text(
                                                text = chatMsg.senderName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = activeThemePalette.headerAccentColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }

                                        // Quoted Reply Preview Card inside Bubble
                                        if (replyQuote != null && !chatMsg.isDeletedForEveryone) {
                                            PaygamBubbleQuotedReplyCard(
                                                quote = replyQuote,
                                                sentByMe = sentByMe
                                            )
                                        }

                                        // Attached Images (1-5 photos) inside the message bubble
                                        if (chatMsg.imageUrls.isNotEmpty() && !chatMsg.isDeletedForEveryone) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = if (chatMsg.body.isNotBlank()) 6.dp else 2.dp)
                                                    .testTag("message_images_grid_${chatMsg.id}")
                                            ) {
                                                chatMsg.imageUrls.chunked(2).forEachIndexed { rowIdx, rowImgs ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        rowImgs.forEachIndexed { colIdx, imgUrl ->
                                                            val globalIdx = rowIdx * 2 + colIdx
                                                            val isSinglePhoto = chatMsg.imageUrls.size == 1
                                                            ChatAttachmentImage(
                                                                imageUrl = imgUrl,
                                                                contentDescription = "পয়গামের ছবি ${globalIdx + 1}",
                                                                contentScale = if (isSinglePhoto) ContentScale.Fit else ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .then(
                                                                        if (isSinglePhoto) {
                                                                            Modifier.heightIn(min = 190.dp, max = 310.dp)
                                                                        } else {
                                                                            Modifier.height(145.dp)
                                                                        }
                                                                    )
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .background(Color(0xFFF7FBF9))
                                                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                                                                    .clickable {
                                                                        fullScreenViewerSourceMessage = chatMsg
                                                                        fullScreenViewerImages = chatMsg.imageUrls to globalIdx
                                                                    }
                                                                    .testTag("message_image_${chatMsg.id}_$globalIdx")
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                if (cleanSubject.isNotBlank() && cleanSubject != "খেদমত সংক্রান্ত যোগাযোগ" && !chatMsg.isDeletedForEveryone) {
                                                    Text(
                                                        text = "বিষয়: $cleanSubject",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (sentByMe) QawmiGold else activeThemePalette.headerAccentColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                }
                                                if (chatMsg.isDeletedForEveryone || chatMsg.body.isNotBlank()) {
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
                                                    if (sentByMe && !isGroupConversation) {
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

                            // Reaction Symbols below the bubble (tapping opens Reaction Details Modal)
                            if (activeReactionEmojis.isNotEmpty() && !chatMsg.isDeletedForEveryone) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .padding(horizontal = if (sentByMe) 4.dp else 34.dp)
                                        .border(1.dp, QawmiGold, RoundedCornerShape(50))
                                        .clickable { messageForReactionDetails = chatMsg }
                                        .testTag("message_reactions_row_${chatMsg.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        activeReactionEmojis.forEach { emojiStr ->
                                            Text(text = emojiStr, fontSize = 14.sp)
                                        }
                                        if (totalReactionCount > 1) {
                                            Text(
                                                text = IslamicDateAndPrayerHelper.toBanglaDigits(totalReactionCount.toString()),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
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

            // 3. STICKY BOTTOM TEXT & PHOTO COMPOSER BAR OR BLOCKED STATUS BANNER
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
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Reply Composer Banner when replying to a message
                        replyingToMessage?.let { replyTarget ->
                            PaygamReplyComposerBanner(
                                replyingTo = replyTarget,
                                onCancelReply = { replyingToMessage = null }
                            )
                        }

                        // Toggleable Emoji Picker Panel
                        if (showEmojiPicker) {
                            PaygamEmojiPickerStrip(
                                onEmojiSelected = { emoji ->
                                    body += emoji
                                },
                                onClose = { showEmojiPicker = false }
                            )
                        }

                        // Upload / Sending Progress Indicator
                        if (isSubmitting) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(QawmiMintSurface)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("message_sending_progress_banner"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = QawmiDeepGreen,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "পয়গাম ও ছবি প্রেরিত হচ্ছে…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Selected Images Preview Strip (1-5 photos) with individual Remove button before sending
                        if (selectedImages.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(QawmiMintSurface)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("selected_images_preview_row")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "নির্বাচিত ছবি (${IslamicDateAndPrayerHelper.toBanglaDigits(selectedImages.size.toString())}/৫)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiDeepGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (photoLimitWarning != null) {
                                        Text(
                                            text = photoLimitWarning.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = QawmiAlertRed,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedImages.forEachIndexed { idx, imgData ->
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .testTag("selected_image_preview_$idx")
                                        ) {
                                            ChatAttachmentImage(
                                                imageUrl = imgData,
                                                contentDescription = "নির্বাচিত ছবি ${idx + 1}",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(1.dp, QawmiDeepGreen, RoundedCornerShape(10.dp))
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = QawmiAlertRed,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                                    .size(20.dp)
                                                    .clickable {
                                                        selectedImages = selectedImages.filterIndexed { i, _ -> i != idx }
                                                        photoLimitWarning = null
                                                    }
                                                    .testTag("remove_selected_image_$idx")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "ছবি বাদ দিন",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Emoji Picker Toggle Button
                            IconButton(
                                onClick = { showEmojiPicker = !showEmojiPicker },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (showEmojiPicker) QawmiGold.copy(alpha = 0.25f) else QawmiMintSurface)
                                    .testTag("toggle_emoji_picker_button")
                            ) {
                                Text(text = "😊", fontSize = 18.sp)
                            }

                            // Photo Picker Icon Button (opens phone gallery directly, up to 5 photos)
                            IconButton(
                                onClick = {
                                    if (selectedImages.size >= ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE) {
                                        photoLimitWarning = "একবারে সর্বোচ্চ ৫টি ছবি নির্বাচন করা যাবে।"
                                    } else {
                                        chatMultiplePhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(QawmiMintSurface)
                                    .testTag("attach_photos_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "গ্যালারি থেকে ছবি যুক্ত করুন",
                                    tint = activeThemePalette.sentBubbleColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            OutlinedTextField(
                                value = body,
                                onValueChange = { newText ->
                                    body = newText
                                },
                                placeholder = { Text("আপনার পয়গাম লিখুন...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("message_body_input"),
                                maxLines = 4,
                                shape = RoundedCornerShape(22.dp)
                            )

                            val canSend = !isSubmitting && (body.isNotBlank() || selectedImages.isNotEmpty())
                            Button(
                                onClick = {
                                    if (body.isNotBlank() || selectedImages.isNotEmpty()) {
                                        val messageToSend = body
                                        val imagesToSend = selectedImages.take(ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE)
                                        val effectiveSubject = replyingToMessage?.let { target ->
                                            val snippet = target.body.ifBlank {
                                                if (target.imageUrls.isNotEmpty()) "📷 ছবি" else "পয়গাম"
                                            }
                                            ReplyQuoteInfo.encodeSubject(
                                                replyToMessageId = target.id,
                                                replyToSenderName = target.senderName.ifBlank { resolvedPartnerName },
                                                replyToSnippet = snippet
                                            )
                                        } ?: subject
                                        body = ""
                                        selectedImages = emptyList()
                                        photoLimitWarning = null
                                        replyingToMessage = null
                                        showEmojiPicker = false
                                        if (!isGroupConversation) {
                                            onTypingStatusChanged(receiverId, false)
                                        }
                                        onSendMessageWithImages(
                                            receiverId,
                                            resolvedPartnerName,
                                            effectiveSubject,
                                            messageToSend,
                                            imagesToSend,
                                            activeGroupId,
                                            if (isGroupConversation) resolvedPartnerName else "",
                                            activeGroup?.memberIds ?: emptyList()
                                        )
                                    }
                                },
                                enabled = canSend,
                                colors = ButtonDefaults.buttonColors(containerColor = activeThemePalette.sentBubbleColor),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "পয়গাম পাঠান",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পাঠান", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // PAYGAM CONVERSATION LIST VIEW (Personal & Group Conversations)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. SUMMARY BANNER WITH UNREAD COUNT, CREATE GROUP & PRIVACY MENU
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "পয়গাম ও গ্রুপ কথোপকথন",
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                            Button(
                                onClick = { showCreateGroupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("create_group_button")
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("নতুন গ্রুপ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Box {
                                IconButton(
                                    onClick = { showInboxSettingsMenu = true },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("inbox_settings_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "পয়গাম সেটিংস",
                                        tint = QawmiDeepGreen
                                    )
                                }
                                DropdownMenu(
                                    expanded = showInboxSettingsMenu,
                                    onDismissRequest = { showInboxSettingsMenu = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "নোটিফিকেশনে মেসেজ প্রিভিউ লুকান",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = QawmiDeepGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (hideMessagePreviewInNotif) "চালু (শুধু 'নতুন মেসেজ এসেছে' দেখাবে)" else "বন্ধ (প্রেরকের নাম ও পয়গাম দেখাবে)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = QawmiTextSecondary
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (hideMessagePreviewInNotif) QawmiDeepGreen else QawmiMintBorder
                                            )
                                        },
                                        onClick = {
                                            val newVal = !hideMessagePreviewInNotif
                                            hideMessagePreviewInNotif = newVal
                                            QawmiNotificationHelper.setMessagePreviewHidden(context, newVal)
                                            showInboxSettingsMenu = false
                                        },
                                        modifier = Modifier.testTag("toggle_notification_preview_privacy")
                                    )
                                    HorizontalDivider(color = QawmiMintBorder)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "নতুন গ্রুপ খুলুন",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = QawmiDeepGreen)
                                        },
                                        onClick = {
                                            showInboxSettingsMenu = false
                                            showCreateGroupDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. SEARCH BAR & FILTER CHIPS (ব্যবহারকারীর নাম বা বার্তা খুঁজুন... + সব / ব্যক্তিগত / গ্রুপ / অপঠিত)
                OutlinedTextField(
                    value = inboxSearchQuery,
                    onValueChange = { inboxSearchQuery = it },
                    placeholder = {
                        Text(
                            text = "ব্যবহারকারীর নাম বা বার্তা খুঁজুন...",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "খুঁজুন",
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (inboxSearchQuery.isNotBlank()) {
                            IconButton(onClick = { inboxSearchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "মুছুন", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inbox_search_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                val unreadConversationsCount = remember(conversationThreads) {
                    conversationThreads.count { it.unreadCount > 0 }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterOptions = listOf(
                        Triple(0, "ALL", "সব (${IslamicDateAndPrayerHelper.toBanglaDigits(conversationThreads.size.toString())})"),
                        Triple(1, "PERSONAL", "ব্যক্তিগত"),
                        Triple(2, "GROUP", "গ্রুপ"),
                        Triple(3, "UNREAD", "অপঠিত (${IslamicDateAndPrayerHelper.toBanglaDigits(unreadConversationsCount.toString())})")
                    )
                    filterOptions.forEach { (tabIdx, tabCode, tabLabel) ->
                        val selected = inboxFilterTab == tabIdx
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) QawmiDeepGreen else Color.White,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (selected) QawmiDeepGreen else QawmiMintBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { inboxFilterTab = tabIdx }
                                .testTag("inbox_filter_chip_$tabCode")
                        ) {
                            Text(
                                text = tabLabel,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "কথোপকথনের তালিকা",
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
                        if (filteredConversationThreads.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (inboxSearchQuery.isNotBlank() || inboxFilterTab != 0) {
                                        "আপনার অনুসন্ধান বা ফিল্টার অনুযায়ী কোনো কথোপকথন পাওয়া যায়নি।"
                                    } else {
                                        "এখনো কোনো পয়গাম আদান-প্রদান হয়নি।\nউপরের 'নতুন গ্রুপ' বাটনে চাপ দিয়ে গ্রুপ খুলুন অথবা যেকোনো প্রাপকের প্রোফাইলে গিয়ে পয়গাম পাঠান।"
                                    },
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
                                items(filteredConversationThreads, key = { it.partnerId }) { thread ->
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
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            UserAvatarImage(
                                                photoUrl = thread.partnerPhotoUrl,
                                                displayName = thread.partnerName,
                                                size = 50.dp,
                                                borderColor = if (thread.isGroup) QawmiGold else if (thread.isPartnerOnline) Color(0xFF00C853) else QawmiMintBorder,
                                                borderWidth = 1.5.dp,
                                                showOnlineIndicator = !thread.isGroup,
                                                isOnline = thread.isPartnerOnline,
                                                modifier = Modifier.clickable {
                                                    if (!thread.isGroup) {
                                                        onInspectUserProfile(thread.partnerId, thread.partnerName)
                                                    }
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
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(latestMsg.createdAt),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (thread.unreadCount > 0) QawmiDeepGreen else QawmiTextMuted,
                                                        fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }

                                                val presenceBadge = if (thread.isGroup) {
                                                    "গ্রুপ কথোপকথন • সদস্য: ${IslamicDateAndPrayerHelper.toBanglaDigits(thread.groupMemberCount.toString())} জন"
                                                } else if (thread.isPartnerTyping) {
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

                                                val previewBody = if (latestMsg.isDeletedForEveryone) {
                                                    "এই পয়গামটি প্রত্যাহার করা হয়েছে"
                                                } else if (latestMsg.imageUrls.isNotEmpty() && latestMsg.body.isBlank()) {
                                                    "📷 ${IslamicDateAndPrayerHelper.toBanglaDigits(latestMsg.imageUrls.size.toString())}টি ছবি"
                                                } else {
                                                    latestMsg.body
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (thread.isPartnerTyping) {
                                                            "টাইপ করছেন… • • •"
                                                        } else if (isLatestSentByMe) {
                                                            "আপনি: $previewBody"
                                                        } else {
                                                            previewBody
                                                        },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (thread.isPartnerTyping) QawmiDeepGreen else QawmiTextDark,
                                                        fontWeight = if (thread.unreadCount > 0 || thread.isPartnerTyping) FontWeight.Bold else FontWeight.Normal,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    if (thread.unreadCount > 0) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        // Green circular unread message count badge on the right (matching reference image)
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color(0xFF00A859),
                                                            modifier = Modifier
                                                                .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                                                                .testTag("thread_unread_badge_${thread.partnerId}")
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = IslamicDateAndPrayerHelper.toBanglaDigits(thread.unreadCount.toString()),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color.White,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.ExtraBold
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
    adminDisplayName: String = "অ্যাডমিন",
    adminPhotoUrl: String = "",
    usersState: UiState<List<UserProfile>> = UiState.Success(emptyList()),
    submissionsState: UiState<List<KhedmotSubmission>> = UiState.Success(emptyList()),
    madrasasState: UiState<List<MadrasaProfile>>,
    feedPostsState: UiState<List<FeedPost>>,
    telegramNotificationsState: UiState<List<TelegramNotificationLog>> = UiState.Success(emptyList()),
    adminsState: UiState<List<com.example.data.model.AdminRegistryEntry>> = UiState.Success(emptyList()),
    latestAppRelease: com.example.data.model.AppReleaseInfo? = null,
    isSubmitting: Boolean = false,
    onRetryServerLoad: () -> Unit = {},
    onVerifyUser: (UserProfile, Boolean) -> Unit = { _, _ -> },
    onUpdateSubmissionStatus: (KhedmotSubmission, String) -> Unit = { _, _ -> },
    onVerifyMadrasa: (MadrasaProfile, Boolean) -> Unit,
    onVerifyPost: (FeedPost, Boolean, String) -> Unit,
    onToggleBoostPost: (FeedPost, Boolean) -> Unit,
    onSendAnnouncement: (title: String, message: String) -> Unit,
    onPublishQuickNotice: (title: String, description: String, noticeType: String) -> Unit = { title, desc, _ ->
        onSendAnnouncement(title, desc)
    },
    onRecordLiveAdminAction: (eventType: String, title: String, message: String, referenceId: String) -> Unit = { _, _, _, _ -> },
    onRequestPasswordReset: (email: String) -> Unit = {},
    onSendTestTelegramAlert: (customMessage: String) -> Unit = {},
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
    onDeletePost: (String) -> Unit
) {
    ModernAdminPanelRoot(
        isAdminUser = isAdminUser,
        adminDisplayName = adminDisplayName,
        adminPhotoUrl = adminPhotoUrl,
        usersState = usersState,
        submissionsState = submissionsState,
        madrasasState = madrasasState,
        feedPostsState = feedPostsState,
        telegramNotificationsState = telegramNotificationsState,
        adminsState = adminsState,
        latestAppRelease = latestAppRelease,
        isSubmitting = isSubmitting,
        onRetryServerLoad = onRetryServerLoad,
        onVerifyUser = onVerifyUser,
        onUpdateSubmissionStatus = onUpdateSubmissionStatus,
        onVerifyMadrasa = onVerifyMadrasa,
        onVerifyPost = onVerifyPost,
        onToggleBoostPost = onToggleBoostPost,
        onPublishQuickNotice = onPublishQuickNotice,
        onSendAnnouncement = onSendAnnouncement,
        onRecordLiveAdminAction = onRecordLiveAdminAction,
        onSendTestTelegramAlert = onSendTestTelegramAlert,
        onPublishAppRelease = onPublishAppRelease,
        onDeletePost = onDeletePost
    )
}


@Composable
fun FullPrayerScheduleScreen() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        IslamicDateAndPrayerHelper.initWithContext(context)
    }

    val selectedDistrict by IslamicDateAndPrayerHelper.selectedDistrictFlow.collectAsState()
    val pinnedDistrict by IslamicDateAndPrayerHelper.pinnedDistrictFlow.collectAsState()
    val favoriteDistricts by IslamicDateAndPrayerHelper.favoriteDistrictsFlow.collectAsState()
    var showDistrictPicker by rememberSaveable { mutableStateOf(false) }

    val selectedSchedule = remember(selectedDistrict) {
        IslamicDateAndPrayerHelper.getScheduleForDistrict(selectedDistrict)
    }
    val divisionsAndFavorites = remember(selectedDistrict, pinnedDistrict, favoriteDistricts) {
        val orderedNames = (listOf(selectedDistrict, pinnedDistrict) +
            favoriteDistricts +
            listOf("ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী", "খুলনা", "বরিশাল", "রংপুর", "ময়মনসিংহ"))
            .distinct()
        orderedNames.map { IslamicDateAndPrayerHelper.getScheduleForDistrict(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("full_prayer_schedule_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "বাংলাদেশের জেলা ও বিভাগওয়ারী নামাজের সময়সূচী",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "হানাফি মাজহাব অনুযায়ী প্রতিটি ওয়াক্তের শুরু ও শেষ সময় • পিন করা জেলা: $pinnedDistrict",
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiTextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDistrictPicker = true }
                        .testTag("full_schedule_change_district_button"),
                    shape = RoundedCornerShape(10.dp),
                    color = QawmiDeepGreen
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = QawmiGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "জেলা নির্বাচন (৬৪)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Quick-switch chips for Pinned & Favorite Districts
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                favoriteDistricts.forEach { dist ->
                    val isSelected = dist == selectedDistrict
                    val isPinned = dist == pinnedDistrict
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { IslamicDateAndPrayerHelper.selectDistrict(context, dist) }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) QawmiDeepGreen else QawmiMintBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .testTag("schedule_fav_chip_$dist"),
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) QawmiDeepGreen else QawmiMintSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPinned) "📌 $dist" else dist,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        items(divisionsAndFavorites, key = { it.divisionBangla }) { divSchedule ->
            val isPinned = divSchedule.divisionBangla == pinnedDistrict
            val isSelected = divSchedule.divisionBangla == selectedDistrict
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) QawmiGold else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        IslamicDateAndPrayerHelper.selectDistrict(context, divSchedule.divisionBangla)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiDarkGreen)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = QawmiGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "জেলা/বিভাগ: ${divSchedule.divisionBangla}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    IslamicDateAndPrayerHelper.pinDistrict(context, divSchedule.divisionBangla)
                                }
                                .testTag("schedule_pin_btn_${divSchedule.divisionBangla}"),
                            shape = RoundedCornerShape(50),
                            color = if (isPinned) QawmiGold else Color(0xFF165B48)
                        ) {
                            Text(
                                text = if (isPinned) "📌 পিন করা জেলা" else "📌 পিন করুন",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPinned) QawmiTextDark else Color(0xFFDDF3EC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Current Waqt, Next Prayer, Sunrise & Sunset row
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1D6E57), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0A4B39)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${divSchedule.currentWaqtNameBangla} (${divSchedule.currentWaqtRangeBangla})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "সূর্যোদয়: ${divSchedule.sunriseBangla}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDDF3EC),
                                    fontSize = 10.5.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${divSchedule.nextWaqtNameBangla} • শুরু: ${divSchedule.nextWaqtStartBangla}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "সূর্যাস্ত: ${divSchedule.sunsetBangla}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB5DCD0),
                                    fontSize = 10.5.sp
                                )
                            }
                        }
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
                                    .border(
                                        width = if (waqt.isCurrentWaqt) 1.5.dp else 1.dp,
                                        color = if (waqt.isCurrentWaqt) QawmiGold else Color(0xFF1D6E57),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                color = if (waqt.isCurrentWaqt) Color(0xFF125A45) else Color(0xFF0A4B39)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = waqt.nameBangla,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (waqt.isCurrentWaqt) QawmiGold else Color(0xFFDDF3EC),
                                        fontWeight = if (waqt.isCurrentWaqt) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "শুরু ${waqt.timeBangla}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = "শেষ ${waqt.endTimeBangla}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFDDF3EC),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = waqt.period,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB5DCD0),
                                        fontSize = 9.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDistrictPicker) {
        PrayerDistrictSelectionDialog(
            selectedDistrict = selectedDistrict,
            pinnedDistrict = pinnedDistrict,
            favoriteDistricts = favoriteDistricts,
            onSelectDistrict = { dist ->
                IslamicDateAndPrayerHelper.selectDistrict(context, dist)
            },
            onPinDistrict = { dist ->
                IslamicDateAndPrayerHelper.pinDistrict(context, dist)
            },
            onToggleFavoriteDistrict = { dist ->
                IslamicDateAndPrayerHelper.toggleFavoriteDistrict(context, dist)
            },
            onDismiss = { showDistrictPicker = false }
        )
    }
}
