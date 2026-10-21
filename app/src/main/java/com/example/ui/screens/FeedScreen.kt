package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookmarkItem
import com.example.data.model.FeedPost
import com.example.ui.components.CandidateAvatarPlaceholder
import com.example.ui.components.MadrasaThumbIllustration
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSeekerBlue
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextMuted
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.theme.QawmiVerifiedGreen
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.viewmodel.FeedFilterTab
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.UiState

@Composable
fun FeedScreen(
    feedPostsState: UiState<List<FeedPost>>,
    bookmarksState: UiState<List<BookmarkItem>>,
    selectedFilter: FeedFilterTab,
    selectedDistrict: String,
    searchQuery: String,
    currentUserId: String,
    onSelectFilter: (FeedFilterTab) -> Unit,
    onSelectDistrict: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onApplyClick: (FeedPost) -> Unit,
    onViewProfileOrDetailClick: (FeedPost) -> Unit,
    onToggleBookmark: (FeedPost, Boolean) -> Unit,
    onDeletePost: (String) -> Unit,
    onNavigate: (MainDestination) -> Unit
) {
    var showDistrictMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    val bookmarkedPostIds = remember(bookmarksState) {
        (bookmarksState as? UiState.Success)?.data?.map { it.postId }?.toSet() ?: emptySet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("feed_screen_root")
    ) {
        // Filter Chips Row matching UI reference ("সব পোস্ট", "নিয়োগ বিজ্ঞপ্তি", "আবেদন", "জেলা", Tune icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeedChipPill(
                label = "সব পোস্ট",
                selected = selectedFilter == FeedFilterTab.ALL && selectedDistrict == "সকল জেলা",
                onClick = {
                    onSelectFilter(FeedFilterTab.ALL)
                    onSelectDistrict("সকল জেলা")
                },
                testTag = "feed_filter_all"
            )
            FeedChipPill(
                label = "নিয়োগ বিজ্ঞপ্তি",
                selected = selectedFilter == FeedFilterTab.JOB_CIRCULAR,
                onClick = { onSelectFilter(FeedFilterTab.JOB_CIRCULAR) },
                testTag = "feed_filter_circulars"
            )
            FeedChipPill(
                label = "আবেদন",
                selected = selectedFilter == FeedFilterTab.APPLICATIONS,
                onClick = { onSelectFilter(FeedFilterTab.APPLICATIONS) },
                testTag = "feed_filter_applications"
            )

            Box {
                FeedChipPill(
                    label = if (selectedDistrict == "সকল জেলা") "জেলা" else "জেলা: $selectedDistrict",
                    selected = selectedFilter == FeedFilterTab.DISTRICT || selectedDistrict != "সকল জেলা",
                    onClick = {
                        onSelectFilter(FeedFilterTab.DISTRICT)
                        showDistrictMenu = true
                    },
                    testTag = "feed_filter_district"
                )
                DropdownMenu(
                    expanded = showDistrictMenu,
                    onDismissRequest = { showDistrictMenu = false }
                ) {
                    IslamicDateAndPrayerHelper.allDistrictsBangla.forEach { districtName ->
                        DropdownMenuItem(
                            text = { Text(districtName) },
                            onClick = {
                                onSelectDistrict(districtName)
                                showDistrictMenu = false
                            }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { showSearchBar = !showSearchBar }
                    .testTag("feed_search_toggle_button"),
                shape = CircleShape,
                color = if (showSearchBar || searchQuery.isNotBlank()) QawmiDeepGreen else QawmiMintSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "ফিল্টার ও অনুসন্ধান",
                        tint = if (showSearchBar || searchQuery.isNotBlank()) Color.White else QawmiDeepGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("মাদরাসা, পদ, কিতাব বা জেলা লিখে খুঁজুন...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = QawmiDeepGreen)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .testTag("feed_search_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        when (feedPostsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = QawmiDeepGreen)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = feedPostsState.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is UiState.Success -> {
                val filteredPosts = remember(
                    feedPostsState.data,
                    selectedFilter,
                    selectedDistrict,
                    searchQuery
                ) {
                    feedPostsState.data.filter { post ->
                        val matchesType = when (selectedFilter) {
                            FeedFilterTab.ALL -> true
                            FeedFilterTab.JOB_CIRCULAR -> post.postType == "JOB_CIRCULAR"
                            FeedFilterTab.APPLICATIONS -> post.postType == "SEEKER_APPLICATION"
                            FeedFilterTab.DISTRICT -> true
                        }
                        val matchesDistrict = selectedDistrict == "সকল জেলা" ||
                            post.district.contains(selectedDistrict, ignoreCase = true) ||
                            post.deadlineOrPreferredDistricts.contains(selectedDistrict, ignoreCase = true)

                        val matchesSearch = searchQuery.isBlank() ||
                            post.title.contains(searchQuery, ignoreCase = true) ||
                            post.designation.contains(searchQuery, ignoreCase = true) ||
                            post.district.contains(searchQuery, ignoreCase = true) ||
                            post.qualification.contains(searchQuery, ignoreCase = true)

                        matchesType && matchesDistrict && matchesSearch
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("feed_posts_list"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredPosts.isEmpty()) {
                        item {
                            EmptyFeedCard(
                                onPublishCircular = { onNavigate(MainDestination.PUBLISH_CIRCULAR) },
                                onPublishApplication = { onNavigate(MainDestination.PUBLISH_APPLICATION) }
                            )
                        }
                    } else {
                        items(filteredPosts, key = { it.id }) { post ->
                            val isBookmarked = bookmarkedPostIds.contains(post.id)
                            FeedPostCardItem(
                                post = post,
                                isBookmarked = isBookmarked,
                                isOwnPost = post.authorId == currentUserId,
                                onPrimaryActionClick = {
                                    if (post.postType == "JOB_CIRCULAR") {
                                        onApplyClick(post)
                                    } else {
                                        onViewProfileOrDetailClick(post)
                                    }
                                },
                                onCardClick = { onViewProfileOrDetailClick(post) },
                                onBookmarkToggle = { onToggleBookmark(post, isBookmarked) },
                                onDeleteClick = { onDeletePost(post.id) }
                            )
                        }
                    }

                    // Bottom Inspirational Quote Banner matching the right screen of the UI reference
                    item {
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = CircleShape,
                                    color = QawmiDeepGreen
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "“কওমি মাদরাসা খেদমতেই\nআখেরাতের সফলতার পথ”",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(50),
        color = if (selected) QawmiDeepGreen else QawmiMintSurface
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else QawmiTextDark,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun FeedPostCardItem(
    post: FeedPost,
    isBookmarked: Boolean,
    isOwnPost: Boolean,
    onPrimaryActionClick: () -> Unit,
    onCardClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isCircular = post.postType == "JOB_CIRCULAR"
    val relativeTime = remember(post.createdAt) {
        IslamicDateAndPrayerHelper.formatRelativeTimeBangla(post.createdAt)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick)
            .testTag("feed_post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Thumbnail or Candidate Avatar matching UI reference
            if (isCircular) {
                MadrasaThumbIllustration()
            } else {
                CandidateAvatarPlaceholder()
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Badge + Relative Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isCircular) QawmiDeepGreen else QawmiSeekerBlue
                    ) {
                        Text(
                            text = if (isCircular) "নিয়োগ বিজ্ঞপ্তি" else "চাকরিপ্রার্থীর আবেদন",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title + Verified check if verified madrasa
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    if (post.isVerifiedMadrasa) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "যাচাইকৃত মাদরাসা",
                            tint = QawmiVerifiedGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Info Rows matching UI reference
                val locationDisplay = if (post.upazilaOrLocation.isNotBlank() && post.upazilaOrLocation != post.district) {
                    "${post.upazilaOrLocation}, ${post.district}"
                } else {
                    post.district
                }
                FeedDetailLine(
                    icon = Icons.Default.LocationOn,
                    text = locationDisplay
                )
                FeedDetailLine(
                    icon = Icons.Default.WorkOutline,
                    text = "পদ: ${post.designation}"
                )
                FeedDetailLine(
                    icon = Icons.Default.MenuBook,
                    text = "শিক্ষাগত যোগ্যতা: ${post.qualification}"
                )
                if (isCircular) {
                    FeedDetailLine(
                        icon = Icons.Default.Payments,
                        text = "বেতন: ${post.salaryOrExperience}"
                    )
                    FeedDetailLine(
                        icon = Icons.Default.CalendarToday,
                        text = "আবেদনের শেষ তারিখ: ${post.deadlineOrPreferredDistricts}"
                    )
                } else {
                    FeedDetailLine(
                        icon = Icons.Default.Payments,
                        text = "অভিজ্ঞতা: ${post.salaryOrExperience}"
                    )
                    FeedDetailLine(
                        icon = Icons.Default.CalendarToday,
                        text = "পছন্দের জেলা: ${post.deadlineOrPreferredDistricts}"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action Button + Bookmark Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPrimaryActionClick,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("post_action_btn_${post.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QawmiDeepGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isCircular) "আবেদন করুন" else "প্রোফাইল দেখুন",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwnPost) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "পোস্ট মুছুন",
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onBookmarkToggle,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("bookmark_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "পছন্দের খেদমতে সংরক্ষণ",
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedDetailLine(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = QawmiDeepGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = QawmiTextSecondary,
            fontSize = 12.5.sp
        )
    }
}

@Composable
private fun EmptyFeedCard(
    onPublishCircular: () -> Unit,
    onPublishApplication: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "এই মুহূর্তে কোনো প্রকাশিত পোস্ট পাওয়া যায়নি",
                style = MaterialTheme.typography.titleMedium,
                color = QawmiTextDark,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "দ্বীনি খেদমতের নতুন নিয়োগ বিজ্ঞপ্তি অথবা নিজের যোগ্যতা ও তাজরিবা নিয়ে প্রথম আবেদনটি প্রকাশ করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = QawmiTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPublishCircular,
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("empty_feed_publish_circular_btn")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("বিজ্ঞপ্তি দিন", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onPublishApplication,
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiSeekerBlue),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("empty_feed_publish_application_btn")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("আবেদন প্রকাশ", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
