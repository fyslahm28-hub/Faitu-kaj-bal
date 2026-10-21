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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@Composable
fun MadrasaDirectoryScreen(
    madrasasState: UiState<List<MadrasaProfile>>,
    onlyVerified: Boolean,
    onRegisterMadrasaClick: () -> Unit,
    onMessageOwnerClick: (String, String) -> Unit
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
                                                onClick = { onMessageUser(sub.applicantId, sub.applicantName) },
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("মেসেজ", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    } else {
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

@Composable
fun MessagesScreen(
    messagesState: UiState<List<DirectMessage>>,
    currentUserId: String,
    targetUser: Pair<String, String>?,
    isSubmitting: Boolean,
    onSendMessage: (receiverId: String, receiverName: String, subject: String, body: String) -> Unit,
    onSelectRecipient: (String, String) -> Unit,
    onMarkRead: (String) -> Unit
) {
    var receiverId by remember(targetUser) { mutableStateOf(targetUser?.first.orEmpty()) }
    var receiverName by remember(targetUser) { mutableStateOf(targetUser?.second.orEmpty()) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("messages_screen")
    ) {
        // Compose Message Box if a recipient is selected
        if (receiverId.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "প্রাপক: $receiverName",
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("বিষয় (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: খেদমতের নিয়োগ বিজ্ঞপ্তি প্রসঙ্গে") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("আপনার পয়গাম / বার্তা লিখুন *") },
                        placeholder = { Text("আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                            .testTag("message_body_input")
                    )
                    Button(
                        onClick = {
                            onSendMessage(receiverId, receiverName, subject, body)
                            body = ""
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("send_message_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পয়গাম পাঠান")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "আদান-প্রদানকৃত পয়গামসমূহ (মেসেজ ইনবক্স)",
            style = MaterialTheme.typography.titleSmall,
            color = QawmiTextDark,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

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
                if (messagesState.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "এখনো কোনো পয়গাম আদান-প্রদান হয়নি।\nফিড বা মাদরাসা তালিকা থেকে যেকোনো প্রাপককে সরাসরি মেসেজ পাঠাতে পারবেন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QawmiTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(messagesState.data, key = { it.id }) { msg ->
                            val isSentByMe = msg.senderId == currentUserId
                            val otherUid = if (isSentByMe) msg.receiverId else msg.senderId
                            val otherName = if (isSentByMe) msg.receiverName else msg.senderName

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (!isSentByMe && !msg.isRead) {
                                            onMarkRead(msg.id)
                                        }
                                        receiverId = otherUid
                                        receiverName = otherName
                                        onSelectRecipient(otherUid, otherName)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!isSentByMe && !msg.isRead) QawmiMintSurface else Color.White
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isSentByMe) "প্রাপক: ${msg.receiverName}" else "প্রেরক: ${msg.senderName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = QawmiDeepGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(msg.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = QawmiTextMuted
                                        )
                                    }
                                    if (msg.subject.isNotBlank()) {
                                        Text(
                                            text = "বিষয়: ${msg.subject}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = QawmiTextDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = QawmiTextDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "উত্তর দিতে এখানে ট্যাপ করুন",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiSeekerBlue
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

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<AppNotification>>,
    onMarkNotificationRead: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("notifications_screen")
    ) {
        Text(
            text = "সকল নোটিফিকেশন ও ইত্তেলা",
            style = MaterialTheme.typography.titleMedium,
            color = QawmiTextDark,
            fontWeight = FontWeight.Bold
        )
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
                                text = "আপনার কোনো নতুন নোটিফিকেশন নেই।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = QawmiTextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notificationsState.data, key = { it.id }) { notif ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (!notif.isRead) onMarkNotificationRead(notif.id)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!notif.isRead) QawmiMintSurface else Color.White
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = QawmiDeepGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = IslamicDateAndPrayerHelper.formatRelativeTimeBangla(notif.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = QawmiTextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextDark
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

@Composable
fun AdminPanelScreen(
    isAdminUser: Boolean,
    madrasasState: UiState<List<MadrasaProfile>>,
    feedPostsState: UiState<List<FeedPost>>,
    onVerifyMadrasa: (String, Boolean) -> Unit,
    onVerifyPost: (String, Boolean, String) -> Unit,
    onDeletePost: (String) -> Unit
) {
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
                            "আপনি অনুমোদিত অ্যাডমিন হিসেবে মাদরাসা যাচাইকরণ ও বিজ্ঞপ্তি তদারকি করতে পারবেন।"
                        } else {
                            "শুধুমাত্র অনুমোদিত অ্যাডমিন মাদরাসা যাচাইকরণ স্ট্যাটাস পরিবর্তন করতে পারেন।"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val madrasas = (madrasasState as? UiState.Success)?.data ?: emptyList()
        val posts = (feedPostsState as? UiState.Success)?.data ?: emptyList()

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
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
                        if (isAdminUser) {
                            Button(
                                onClick = { onVerifyMadrasa(madrasa.id, !madrasa.isVerified) },
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

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "প্রকাশিত ফিড পোস্ট তদারকি (${posts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            items(posts, key = { "adm_p_${it.id}" }) { post ->
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
                            Text(post.title, fontWeight = FontWeight.Bold, color = QawmiTextDark)
                            Text("পদ: ${post.designation} (${post.district})", style = MaterialTheme.typography.bodySmall)
                        }
                        if (isAdminUser) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onVerifyPost(post.id, !post.isVerifiedMadrasa, "APPROVED") },
                                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        if (post.isVerifiedMadrasa) "আনভেরিফাই" else "ভেরিফাই ব্যাজ",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(onClick = { onDeletePost(post.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "মুছুন", tint = Color(0xFFC62828))
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
