package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookmarkItem
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotCategoryHelper
import com.example.data.model.UserProfile
import com.example.ui.components.ChatAttachmentImage
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.QawmiCardWhite
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiGoldLight
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSeekerBlue
import com.example.ui.theme.QawmiSeekerBlueLight
import com.example.ui.theme.QawmiSoftBg
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextMuted
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.theme.QawmiVerifiedGreen
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.util.ProfilePhotoHelper
import com.example.ui.viewmodel.FeedFilterTab
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.UiState
import com.google.firebase.Timestamp

data class FeedReactionOption(
    val code: String,
    val emoji: String,
    val labelBangla: String
)

val islamicFeedReactions: List<FeedReactionOption> = listOf(
    FeedReactionOption("SAMMATI", "👍", "লাইক"),
    FeedReactionOption("MUHABBAT", "❤️", "মুহাব্বত"),
    FeedReactionOption("DUA", "🤲", "দোয়া"),
    FeedReactionOption("SUNDOR", "✨", "মাশাআল্লাহ"),
    FeedReactionOption("DEENI_PRERONA", "🕌", "দ্বীনি প্রেরণা")
)

fun resolveReactionOption(code: String): FeedReactionOption {
    return islamicFeedReactions.firstOrNull { it.code == code }
        ?: FeedReactionOption("SAMMATI", "👍", "লাইক")
}

private val bangladeshDistrictFilters = listOf(
    "সকল জেলা", "ঢাকা", "চট্টগ্রাম", "কুমিল্লা", "সিলেট",
    "নোয়াখালী", "ময়মনসিংহ", "রাজশাহী", "খুলনা", "বরিশাল", "রংপুর", "গাজীপুর", "নারায়ণগঞ্জ"
)

@Composable
fun FeedScreen(
    feedPostsState: UiState<List<FeedPost>>,
    bookmarksState: UiState<List<BookmarkItem>>,
    selectedFilter: FeedFilterTab,
    selectedDistrict: String,
    searchQuery: String,
    currentUserId: String,
    isAdmin: Boolean = false,
    authorProfiles: Map<String, UserProfile> = emptyMap(),
    currentUserProfile: UserProfile? = null,
    selectedKhedmotCategory: String = "সকল বিভাগ",
    postCommentsState: UiState<List<FeedComment>> = UiState.Success(emptyList()),
    onSelectFilter: (FeedFilterTab) -> Unit,
    onSelectDistrict: (String) -> Unit,
    onSelectKhedmotCategory: (String) -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onApplyClick: (FeedPost) -> Unit,
    onViewProfileOrDetailClick: (FeedPost) -> Unit,
    onInspectAuthorProfile: (userId: String, fallbackName: String) -> Unit = { _, _ -> },
    onToggleBookmark: (FeedPost, Boolean) -> Unit,
    onToggleBoostPost: (FeedPost, Boolean) -> Unit = { _, _ -> },
    onDeletePost: (String) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    onReactToPost: (FeedPost, String) -> Unit = { _, _ -> },
    onAddComment: (post: FeedPost, text: String, parentCommentId: String, replyToAuthorName: String) -> Unit = { _, _, _, _ -> },
    onEditComment: (commentId: String, newText: String) -> Unit = { _, _ -> },
    onDeleteComment: (commentId: String, postId: String, currentCommentCount: Int) -> Unit = { _, _, _ -> },
    onSharePostInApp: (post: FeedPost, caption: String) -> Unit = { _, _ -> },
    onPublishQuickDeeniPost: (
        title: String,
        description: String,
        district: String,
        categories: List<String>,
        customCategory: String,
        imageUrls: List<String>
    ) -> Unit = { _, _, _, _, _, _ -> },
    onEditOwnPost: (
        post: FeedPost,
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String,
        khedmotCategories: List<String>,
        customCategory: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onEditOwnPostWithMedia: ((
        post: FeedPost,
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String,
        khedmotCategories: List<String>,
        customCategory: String,
        imageUrls: List<String>
    ) -> Unit)? = null,
    onReportPost: (post: FeedPost, reason: String) -> Unit = { _, _ -> },
    onRetryFeed: () -> Unit = {},
    onShowFeedback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val bookmarkedPostIds = remember(bookmarksState) {
        (bookmarksState as? UiState.Success)?.data?.map { it.postId }?.toSet() ?: emptySet()
    }
    val allComments = remember(postCommentsState) {
        (postCommentsState as? UiState.Success)?.data ?: emptyList()
    }
    val commentsByPostId = remember(allComments) {
        allComments.groupBy { it.postId }
    }

    var visiblePostLimit by remember { mutableIntStateOf(15) }
    var showQuickComposerDialog by remember { mutableStateOf(false) }
    var activeSharePost by remember { mutableStateOf<FeedPost?>(null) }
    var activeReactorsModalPost by remember { mutableStateOf<FeedPost?>(null) }
    var activeEditPost by remember { mutableStateOf<FeedPost?>(null) }
    var activeReportPost by remember { mutableStateOf<FeedPost?>(null) }
    var expandedCommentsPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QawmiSoftBg)
            .testTag("feed_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        when (feedPostsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = QawmiDeepGreen)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ইসলামিক খেদমত ফিড লোড হচ্ছে...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QawmiTextSecondary
                        )
                    }
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                            .testTag("feed_error_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = feedPostsState.message,
                                color = Color(0xFFB71C1C),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = onRetryFeed,
                                colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                modifier = Modifier.testTag("feed_retry_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পুনরায় চেষ্টা করুন")
                            }
                        }
                    }
                }
            }

            is UiState.Success -> {
                val filteredPosts = remember(
                    feedPostsState.data,
                    selectedFilter,
                    selectedDistrict,
                    selectedKhedmotCategory,
                    searchQuery
                ) {
                    feedPostsState.data
                        .distinctBy { it.id }
                        .filter { post ->
                            val matchesTab = when (selectedFilter) {
                                FeedFilterTab.ALL -> true
                                FeedFilterTab.JOB_CIRCULAR -> post.postType == "JOB_CIRCULAR"
                                FeedFilterTab.APPLICATIONS -> post.postType == "SEEKER_APPLICATION"
                                FeedFilterTab.GENERAL_POSTS -> post.postType == "GENERAL_POST"
                                FeedFilterTab.DISTRICT -> {
                                    if (selectedDistrict == "সকল জেলা") true
                                    else post.district.contains(selectedDistrict, ignoreCase = true) ||
                                        post.deadlineOrPreferredDistricts.contains(selectedDistrict, ignoreCase = true)
                                }
                            }
                            val matchesDistrict = if (selectedDistrict == "সকল জেলা") {
                                true
                            } else {
                                post.district.contains(selectedDistrict, ignoreCase = true) ||
                                    post.deadlineOrPreferredDistricts.contains(selectedDistrict, ignoreCase = true)
                            }
                            val matchesKhedmotCat = KhedmotCategoryHelper.matchesCategoryFilter(
                                post = post,
                                selectedCategory = selectedKhedmotCategory
                            )
                            val q = searchQuery.trim()
                            val matchesQuery = if (q.isEmpty()) {
                                true
                            } else {
                                post.title.contains(q, ignoreCase = true) ||
                                    post.authorName.contains(q, ignoreCase = true) ||
                                    post.designation.contains(q, ignoreCase = true) ||
                                    post.district.contains(q, ignoreCase = true) ||
                                    post.qualification.contains(q, ignoreCase = true) ||
                                    post.description.contains(q, ignoreCase = true) ||
                                    post.customCategory.contains(q, ignoreCase = true) ||
                                    post.khedmotCategories.any { it.contains(q, ignoreCase = true) }
                            }
                            matchesTab && matchesDistrict && matchesKhedmotCat && matchesQuery
                        }
                }

                val paginatedPosts = remember(filteredPosts, visiblePostLimit) {
                    filteredPosts.take(visiblePostLimit)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("feed_posts_lazy_column"),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Top Facebook-Style Islamic Feed Composer Card
                    item(key = "feed_top_composer") {
                        FacebookStyleFeedComposerCard(
                            currentUserProfile = currentUserProfile,
                            onOpenQuickComposer = { showQuickComposerDialog = true },
                            onNavigatePublishApplication = { onNavigate(MainDestination.PUBLISH_APPLICATION) },
                            onNavigatePublishCircular = { onNavigate(MainDestination.PUBLISH_CIRCULAR) }
                        )
                    }

                    // 2. Search Bar, Post Type Filter Tabs & Khedmot Category Filter Chips
                    item(key = "feed_filters_header") {
                        FeedSearchAndFiltersSection(
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            selectedDistrict = selectedDistrict,
                            selectedKhedmotCategory = selectedKhedmotCategory,
                            totalMatchingCount = filteredPosts.size,
                            onSearchQueryChange = onSearchQueryChange,
                            onSelectFilter = onSelectFilter,
                            onSelectDistrict = onSelectDistrict,
                            onSelectKhedmotCategory = onSelectKhedmotCategory
                        )
                    }

                    // 3. Empty State or Feed Posts
                    if (filteredPosts.isEmpty()) {
                        item(key = "feed_empty_state") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(18.dp))
                                    .testTag("feed_empty_state_card"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = QawmiCardWhite)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "এই বিভাগে এখনো কোনো পোস্ট বা খেদমত বিজ্ঞপ্তি নেই",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "সর্বপ্রথম আপনার মাদরাসার নিয়োগ বিজ্ঞপ্তি, খেদমতের আবেদন বা উপকারী দ্বীনি আলোচনা প্রকাশ করুন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { onNavigate(MainDestination.PUBLISH_CIRCULAR) },
                                            colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                                            shape = RoundedCornerShape(22.dp)
                                        ) {
                                            Text("বিজ্ঞপ্তি দিন", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { onNavigate(MainDestination.PUBLISH_APPLICATION) },
                                            shape = RoundedCornerShape(22.dp)
                                        ) {
                                            Text("আবেদন প্রকাশ করুন", fontSize = 12.sp, color = QawmiDeepGreen)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(paginatedPosts, key = { it.id.ifBlank { it.hashCode().toString() } }) { post ->
                            val liveAuthor = if (post.authorId == currentUserId) {
                                currentUserProfile
                            } else {
                                authorProfiles[post.authorId]
                            }
                            val resolvedPost = post.copy(
                                authorName = liveAuthor?.fullName?.takeIf { it.isNotBlank() } ?: post.authorName,
                                authorPhotoUrl = liveAuthor?.photoUrl?.takeIf { it.isNotBlank() } ?: post.authorPhotoUrl
                            )
                            val postComments = commentsByPostId[post.id] ?: emptyList()
                            val isCommentsOpen = expandedCommentsPostIds.contains(post.id)

                            FacebookStyleIslamicPostCard(
                                post = resolvedPost,
                                isBookmarked = bookmarkedPostIds.contains(post.id),
                                isOwnPost = post.authorId == currentUserId,
                                isAdmin = isAdmin,
                                currentUserId = currentUserId,
                                postComments = postComments,
                                isCommentsExpanded = isCommentsOpen,
                                onToggleCommentsExpanded = {
                                    expandedCommentsPostIds = if (isCommentsOpen) {
                                        expandedCommentsPostIds - post.id
                                    } else {
                                        expandedCommentsPostIds + post.id
                                    }
                                },
                                onPrimaryActionClick = {
                                    if (resolvedPost.postType == "JOB_CIRCULAR") {
                                        onApplyClick(resolvedPost)
                                    } else {
                                        onViewProfileOrDetailClick(resolvedPost)
                                    }
                                },
                                onInspectAuthorClick = {
                                    onInspectAuthorProfile(
                                        resolvedPost.authorId,
                                        resolvedPost.authorName.ifBlank { resolvedPost.title }
                                    )
                                },
                                onToggleBookmark = {
                                    onToggleBookmark(resolvedPost, bookmarkedPostIds.contains(post.id))
                                },
                                onToggleBoost = {
                                    onToggleBoostPost(resolvedPost, !resolvedPost.isBoosted)
                                },
                                onEditOwnPostClick = {
                                    activeEditPost = resolvedPost
                                },
                                onDeletePost = {
                                    onDeletePost(post.id)
                                },
                                onReportPostClick = {
                                    activeReportPost = resolvedPost
                                },
                                onReactToPost = { reactionCode ->
                                    onReactToPost(resolvedPost, reactionCode)
                                },
                                onOpenReactorsModal = {
                                    activeReactorsModalPost = resolvedPost
                                },
                                onOpenShareDialog = {
                                    activeSharePost = resolvedPost
                                },
                                onAddComment = { text, parentId, replyToName ->
                                    onAddComment(resolvedPost, text, parentId, replyToName)
                                },
                                onEditComment = onEditComment,
                                onDeleteComment = { commentId ->
                                    onDeleteComment(
                                        commentId,
                                        resolvedPost.id,
                                        maxOf(resolvedPost.commentCount, postComments.size)
                                    )
                                }
                            )
                        }

                        if (filteredPosts.size > visiblePostLimit) {
                            item(key = "feed_load_more_item") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { visiblePostLimit += 15 },
                                        shape = RoundedCornerShape(24.dp),
                                        border = BorderStroke(1.2.dp, QawmiDeepGreen),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("feed_load_more_button")
                                    ) {
                                        Text(
                                            text = "আরও পোস্ট দেখুন (${IslamicDateAndPrayerHelper.toBanglaDigits((filteredPosts.size - visiblePostLimit).toString())}টি বাকি)",
                                            color = QawmiDeepGreen,
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

    // Quick Islamic Feed Composer Dialog
    if (showQuickComposerDialog) {
        QuickDeeniPostComposerDialog(
            defaultDistrict = currentUserProfile?.district.orEmpty(),
            onDismiss = { showQuickComposerDialog = false },
            onSubmit = { title, desc, dist, cats, customCat, images ->
                showQuickComposerDialog = false
                onPublishQuickDeeniPost(title, desc, dist, cats, customCat, images)
            }
        )
    }

    // Who Reacted Modal
    if (activeReactorsModalPost != null) {
        val targetPost = activeReactorsModalPost!!
        PostReactorsDialog(
            post = targetPost,
            onDismiss = { activeReactorsModalPost = null },
            onInspectUser = { uid, name ->
                activeReactorsModalPost = null
                onInspectAuthorProfile(uid, name)
            }
        )
    }

    // Share Post Modal (In-App Feed Share + Copy Link + External Android Share)
    if (activeSharePost != null) {
        val postToShare = activeSharePost!!
        ShareFeedPostDialog(
            post = postToShare,
            onDismiss = { activeSharePost = null },
            onShareInApp = { caption ->
                activeSharePost = null
                onSharePostInApp(postToShare, caption)
            },
            onCopyPostText = {
                val formatted = buildShareablePostText(postToShare)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("কওমি কর্মসংস্থান পোস্ট", formatted))
                activeSharePost = null
                onShowFeedback("পোস্টের তথ্য কপি করা হয়েছে।")
            },
            onExternalShare = {
                val formatted = buildShareablePostText(postToShare)
                activeSharePost = null
                try {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, postToShare.title)
                        putExtra(Intent.EXTRA_TEXT, formatted)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "শেয়ার করুন").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
            }
        )
    }

    // Edit Own Post Dialog
    if (activeEditPost != null) {
        val editingPost = activeEditPost!!
        EditOwnFeedPostDialog(
            post = editingPost,
            onDismiss = { activeEditPost = null },
            onSave = { title, dist, loc, desig, qual, salExp, deadlPref, desc, phone, cats, customCat, images ->
                activeEditPost = null
                if (onEditOwnPostWithMedia != null) {
                    onEditOwnPostWithMedia(
                        editingPost,
                        title,
                        dist,
                        loc,
                        desig,
                        qual,
                        salExp,
                        deadlPref,
                        desc,
                        phone,
                        cats,
                        customCat,
                        images
                    )
                } else {
                    onEditOwnPost(
                        editingPost,
                        title,
                        dist,
                        loc,
                        desig,
                        qual,
                        salExp,
                        deadlPref,
                        desc,
                        phone,
                        cats,
                        customCat
                    )
                }
            }
        )
    }

    // Report Post Dialog
    if (activeReportPost != null) {
        val reportingPost = activeReportPost!!
        ReportFeedPostDialog(
            post = reportingPost,
            onDismiss = { activeReportPost = null },
            onSubmitReport = { reason ->
                activeReportPost = null
                onReportPost(reportingPost, reason)
            }
        )
    }
    }
}

@Composable
private fun FacebookStyleFeedComposerCard(
    currentUserProfile: UserProfile?,
    onOpenQuickComposer: () -> Unit,
    onNavigatePublishApplication: () -> Unit,
    onNavigatePublishCircular: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp))
            .testTag("feed_composer_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatarImage(
                    photoUrl = currentUserProfile?.photoUrl.orEmpty(),
                    displayName = currentUserProfile?.fullName?.ifBlank { "আ" } ?: "আ",
                    size = 42.dp,
                    borderColor = QawmiGold,
                    borderWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onOpenQuickComposer)
                        .testTag("feed_open_composer_button"),
                    shape = RoundedCornerShape(24.dp),
                    color = QawmiSoftBg,
                    border = BorderStroke(1.dp, QawmiMintBorder)
                ) {
                    Text(
                        text = "আপনার দ্বীনি খেদমত বা প্রয়োজনীয় তথ্য শেয়ার করুন...",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = QawmiMintBorder.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ComposerQuickActionButton(
                    icon = Icons.Default.PhotoLibrary,
                    label = "দ্বীনি পোস্ট",
                    tint = QawmiDeepGreen,
                    testTag = "composer_quick_deeni_post_btn",
                    onClick = onOpenQuickComposer
                )
                ComposerQuickActionButton(
                    icon = Icons.Default.EditNote,
                    label = "আবেদন প্রকাশ",
                    tint = QawmiSeekerBlue,
                    testTag = "composer_quick_application_btn",
                    onClick = onNavigatePublishApplication
                )
                ComposerQuickActionButton(
                    icon = Icons.Default.Campaign,
                    label = "নিয়োগ বিজ্ঞপ্তি",
                    tint = Color(0xFFB45309),
                    testTag = "composer_quick_circular_btn",
                    onClick = onNavigatePublishCircular
                )
            }
        }
    }
}

@Composable
private fun ComposerQuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = QawmiTextDark,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FeedSearchAndFiltersSection(
    searchQuery: String,
    selectedFilter: FeedFilterTab,
    selectedDistrict: String,
    selectedKhedmotCategory: String,
    totalMatchingCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (FeedFilterTab) -> Unit,
    onSelectDistrict: (String) -> Unit,
    onSelectKhedmotCategory: (String) -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feed_search_input"),
                placeholder = {
                    Text(
                        text = "মাদরাসা, নূরানী, নাজেরা, দরসিয়াত, ইমাম, জেলা বা যোগ্যতা খুঁজুন...",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "খুঁজুন",
                        tint = QawmiDeepGreen
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "মুছুন", tint = QawmiTextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QawmiDeepGreen,
                    unfocusedBorderColor = QawmiMintBorder,
                    focusedContainerColor = QawmiMintSurface.copy(alpha = 0.35f),
                    unfocusedContainerColor = QawmiSoftBg
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Post Type Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedTabChip(
                    label = "সব পোস্ট",
                    selected = selectedFilter == FeedFilterTab.ALL,
                    testTag = "feed_filter_all",
                    onClick = { onSelectFilter(FeedFilterTab.ALL) }
                )
                FeedTabChip(
                    label = "নিয়োগ বিজ্ঞপ্তি",
                    selected = selectedFilter == FeedFilterTab.JOB_CIRCULAR,
                    testTag = "feed_filter_circular",
                    onClick = { onSelectFilter(FeedFilterTab.JOB_CIRCULAR) }
                )
                FeedTabChip(
                    label = "আবেদন",
                    selected = selectedFilter == FeedFilterTab.APPLICATIONS,
                    testTag = "feed_filter_applications",
                    onClick = { onSelectFilter(FeedFilterTab.APPLICATIONS) }
                )
                FeedTabChip(
                    label = "দ্বীনি আলোচনা",
                    selected = selectedFilter == FeedFilterTab.GENERAL_POSTS,
                    testTag = "feed_filter_general",
                    onClick = { onSelectFilter(FeedFilterTab.GENERAL_POSTS) }
                )
                FeedTabChip(
                    label = if (selectedDistrict == "সকল জেলা") "জেলা" else "জেলা: $selectedDistrict",
                    selected = selectedFilter == FeedFilterTab.DISTRICT || selectedDistrict != "সকল জেলা",
                    testTag = "feed_filter_district",
                    onClick = { onSelectFilter(FeedFilterTab.DISTRICT) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Khedmot Category Filter Row (নূরানী, নাজেরা, দরসিয়াত, ইমাম, মুয়াজ্জিন, খতিব, অন্যান্য)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("feed_category_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বিভাগ:",
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                val catFilters = listOf("সকল বিভাগ") + KhedmotCategoryHelper.allCategories
                catFilters.forEach { cat ->
                    val isSelected = selectedKhedmotCategory == cat
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectKhedmotCategory(cat) }
                            .testTag("feed_cat_filter_$cat"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) QawmiSeekerBlue else QawmiMintSurface.copy(alpha = 0.6f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) QawmiSeekerBlue else QawmiMintBorder
                        )
                    ) {
                        Text(
                            text = cat,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else QawmiDeepGreen,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }
            }

            // District sub-selector when DISTRICT tab is active
            if (selectedFilter == FeedFilterTab.DISTRICT) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bangladeshDistrictFilters.forEach { district ->
                        val isDistSelected = selectedDistrict == district
                        FilterChip(
                            selected = isDistSelected,
                            onClick = { onSelectDistrict(district) },
                            label = {
                                Text(
                                    text = district,
                                    fontSize = 11.sp,
                                    fontWeight = if (isDistSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = QawmiGoldLight,
                                selectedLabelColor = QawmiDarkGreen
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTabChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) QawmiDeepGreen else QawmiMintSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) QawmiDeepGreen else QawmiMintBorder
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else QawmiDeepGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun FacebookStyleIslamicPostCard(
    post: FeedPost,
    isBookmarked: Boolean,
    isOwnPost: Boolean,
    isAdmin: Boolean = false,
    currentUserId: String = "",
    postComments: List<FeedComment> = emptyList(),
    isCommentsExpanded: Boolean = false,
    onToggleCommentsExpanded: () -> Unit = {},
    onPrimaryActionClick: () -> Unit,
    onInspectAuthorClick: () -> Unit = {},
    onToggleBookmark: () -> Unit,
    onToggleBoost: () -> Unit = {},
    onEditOwnPostClick: () -> Unit = {},
    onDeletePost: () -> Unit,
    onReportPostClick: () -> Unit = {},
    onReactToPost: (String) -> Unit = {},
    onOpenReactorsModal: () -> Unit = {},
    onOpenShareDialog: () -> Unit = {},
    onAddComment: (text: String, parentCommentId: String, replyToAuthorName: String) -> Unit = { _, _, _ -> },
    onEditComment: (commentId: String, newText: String) -> Unit = { _, _ -> },
    onDeleteComment: (commentId: String) -> Unit = {}
) {
    val isCircular = post.postType == "JOB_CIRCULAR"
    val isApplication = post.postType == "SEEKER_APPLICATION"
    val isGeneral = post.postType == "GENERAL_POST"

    var showMoreMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val myReactionCode = post.userReactions[currentUserId].orEmpty()
    val totalReactions = post.userReactions.size
    val effectiveCommentCount = maxOf(post.commentCount, postComments.size)
    val categoryBadges = remember(post.khedmotCategories, post.customCategory) {
        KhedmotCategoryHelper.resolveDisplayBadges(post)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .border(
                width = if (post.isBoosted) 2.dp else 1.dp,
                color = if (post.isBoosted) QawmiGold else QawmiMintBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("feed_post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QawmiCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (post.isBoosted) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Admin Boosted Top Banner
            if (post.isBoosted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Boosted",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "বিশেষভাবে নির্বাচিত খেদমত পোস্ট (Boosted)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "শীর্ষ পোস্ট",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // 1. Facebook-Style Post Header (Author Avatar, Name, Verified Badge, Time, Type Badge, Menu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onInspectAuthorClick)
                            .testTag("post_author_header_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatarImage(
                            photoUrl = post.authorPhotoUrl,
                            displayName = post.authorName.ifBlank { post.title },
                            size = 44.dp,
                            borderColor = if (isCircular) QawmiGold else QawmiSeekerBlue.copy(alpha = 0.6f),
                            borderWidth = 1.5.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = post.authorName.ifBlank { post.title },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QawmiTextDark,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Verified badge ONLY if explicitly verified by admin
                                if (post.isVerifiedMadrasa) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "যাচাইকৃত",
                                        tint = QawmiVerifiedGreen,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .testTag("verified_badge_${post.id}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatRelativeBanglaTime(post.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiTextMuted,
                                    fontSize = 11.sp
                                )
                                if (post.isEdited) {
                                    Text(
                                        text = "• সম্পাদিত",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                if (post.district.isNotBlank()) {
                                    Text(
                                        text = "• ${post.district}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiDeepGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Post Type Badge + Bookmark + 3-Dot Menu
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isCircular -> QawmiGoldLight.copy(alpha = 0.55f)
                                isApplication -> QawmiSeekerBlueLight
                                else -> QawmiMintSurface
                            }
                        ) {
                            Text(
                                text = when {
                                    isCircular -> "নিয়োগ বিজ্ঞপ্তি"
                                    isApplication -> "খেদমত আবেদন"
                                    else -> "দ্বীনি পোস্ট"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isCircular -> QawmiDarkGreen
                                    isApplication -> QawmiSeekerBlue
                                    else -> QawmiDeepGreen
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("bookmark_post_${post.id}")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "সংরক্ষণ",
                                tint = if (isBookmarked) QawmiDeepGreen else QawmiTextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("post_more_menu_${post.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "অপশন",
                                    tint = QawmiTextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                if (isOwnPost) {
                                    DropdownMenuItem(
                                        text = { Text("পোস্ট সম্পাদনা করুন") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            onEditOwnPostClick()
                                        },
                                        modifier = Modifier.testTag("menu_edit_post_${post.id}")
                                    )
                                }
                                if (isOwnPost || isAdmin) {
                                    DropdownMenuItem(
                                        text = { Text("পোস্ট মুছে ফেলুন", color = Color(0xFFC62828)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFC62828)
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            onDeletePost()
                                        },
                                        modifier = Modifier.testTag("menu_delete_post_${post.id}")
                                    )
                                }
                                if (isAdmin) {
                                    DropdownMenuItem(
                                        text = { Text(if (post.isBoosted) "Boost সরান" else "পোস্ট Boost করুন") },
                                        leadingIcon = {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFB45309))
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            onToggleBoost()
                                        },
                                        modifier = Modifier.testTag("boost_toggle_${post.id}")
                                    )
                                }
                                if (!isOwnPost) {
                                    DropdownMenuItem(
                                        text = { Text("প্রকাশকের প্রোফাইল দেখুন") },
                                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            onInspectAuthorClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("পোস্ট রিপোর্ট করুন") },
                                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFD84315)) },
                                        onClick = {
                                            showMoreMenu = false
                                            onReportPostClick()
                                        },
                                        modifier = Modifier.testTag("menu_report_post_${post.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Khedmot Category Chips (নূরানী, নাজেরা, দরসিয়াত, ইমাম, মুয়াজ্জিন, খতিব, অন্যান্য)
                if (categoryBadges.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                            .testTag("post_categories_row_${post.id}"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categoryBadges.forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = QawmiMintSurface,
                                border = BorderStroke(1.dp, QawmiDeepGreen.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "✦ $badge",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Post Title & Main Content
                if (post.title.isNotBlank() && post.title != post.authorName) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Structured Details Box for Job Circulars & Seeker Applications
                if (!isGeneral) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = QawmiMintSurface.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, QawmiMintBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (post.designation.isNotBlank()) {
                                DetailRowItem(
                                    icon = Icons.Default.WorkOutline,
                                    label = if (isCircular) "খেদমতের পদ/বিভাগ:" else "কাঙ্ক্ষিত খেদমত:",
                                    value = post.designation,
                                    highlight = true
                                )
                            }
                            if (post.qualification.isNotBlank()) {
                                DetailRowItem(
                                    icon = Icons.Default.School,
                                    label = "ইলমি যোগ্যতা:",
                                    value = post.qualification
                                )
                            }
                            if (post.salaryOrExperience.isNotBlank()) {
                                DetailRowItem(
                                    icon = Icons.AutoMirrored.Filled.Assignment,
                                    label = if (isCircular) "মাসিক হাদিয়া/বেতন:" else "খেদমতের অভিজ্ঞতা:",
                                    value = post.salaryOrExperience
                                )
                            }
                            if (post.deadlineOrPreferredDistricts.isNotBlank()) {
                                DetailRowItem(
                                    icon = Icons.Default.Schedule,
                                    label = if (isCircular) "আবেদনের শেষ তারিখ:" else "পছন্দের জেলা:",
                                    value = post.deadlineOrPreferredDistricts
                                )
                            }
                            if (post.upazilaOrLocation.isNotBlank()) {
                                DetailRowItem(
                                    icon = Icons.Default.LocationOn,
                                    label = "অবস্থান:",
                                    value = "${post.upazilaOrLocation}${if (post.district.isNotBlank() && !post.upazilaOrLocation.contains(post.district)) ", ${post.district}" else ""}"
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Description with "আরও দেখুন" toggle
                if (post.description.isNotBlank()) {
                    val isLongText = post.description.length > 220
                    Text(
                        text = if (isLongText && !isDescriptionExpanded) {
                            post.description.take(220) + "..."
                        } else {
                            post.description
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiTextDark,
                        lineHeight = 20.sp
                    )
                    if (isLongText) {
                        Text(
                            text = if (isDescriptionExpanded) "সংক্ষিপ্ত করুন" else "আরও দেখুন",
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { isDescriptionExpanded = !isDescriptionExpanded },
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiSeekerBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Attached Media (Images / Videos uploaded to Firebase Storage or compact preview URLs)
                if (post.imageUrls.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                            .testTag("post_media_row_${post.id}"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        post.imageUrls.forEachIndexed { index, mediaUrl ->
                            val mediaType = post.mediaTypes.getOrNull(index)
                                ?: if (mediaUrl.startsWith("data:video/", ignoreCase = true) ||
                                    mediaUrl.contains(".mp4", ignoreCase = true)
                                ) "VIDEO" else "IMAGE"
                            Box(
                                modifier = Modifier
                                    .height(170.dp)
                                    .width(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(12.dp))
                                    .testTag("post_media_item_${post.id}_$index")
                            ) {
                                ChatAttachmentImage(
                                    imageUrl = mediaUrl,
                                    contentDescription = if (mediaType == "VIDEO") "পোস্টের ভিডিও" else "পোস্টের ছবি",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = QawmiDeepGreen.copy(alpha = 0.82f)
                                ) {
                                    Text(
                                        text = if (mediaType == "VIDEO") "ভিডিও" else "ছবি",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Shared Post Embedded Preview Card
                if (post.sharedPostId.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("shared_post_preview_${post.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = QawmiSoftBg,
                        border = BorderStroke(1.dp, QawmiGold.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = QawmiDeepGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "মূল প্রকাশক: ${post.sharedAuthorName.ifBlank { post.sharedTitle }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (post.sharedTitle.isNotBlank()) {
                                Text(
                                    text = post.sharedTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = QawmiTextDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (post.sharedDescription.isNotBlank()) {
                                Text(
                                    text = post.sharedDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QawmiTextSecondary,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Primary Khedmot Action Button (Apply or View Candidate Profile)
                if (!isGeneral) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPrimaryActionClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("feed_primary_action_${post.id}"),
                            shape = RoundedCornerShape(19.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCircular) QawmiDeepGreen else QawmiSeekerBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isCircular) "খেদমতে আবেদন করুন" else "প্রার্থীর বিস্তারিত ও যোগাযোগ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        OutlinedButton(
                            onClick = onInspectAuthorClick,
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("feed_inspect_author_${post.id}"),
                            shape = RoundedCornerShape(19.dp),
                            border = BorderStroke(1.dp, QawmiMintBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "প্রোফাইল",
                                style = MaterialTheme.typography.labelMedium,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 4. Reaction Picker Bar (Animated when user taps/holds Like or Reaction selector)
                AnimatedVisibility(visible = showReactionPicker) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("reaction_picker_bar_${post.id}"),
                        shape = RoundedCornerShape(24.dp),
                        color = QawmiMintSurface,
                        border = BorderStroke(1.dp, QawmiGold.copy(alpha = 0.6f)),
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            islamicFeedReactions.forEach { option ->
                                val isSelected = myReactionCode == option.code
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            showReactionPicker = false
                                            onReactToPost(option.code)
                                        }
                                        .testTag("post_reaction_option_${post.id}_${option.code}"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) QawmiDeepGreen else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = option.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = option.labelBangla,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White else QawmiTextDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Engagement Summary Bar (Reactions Count + Comments Count + Shares Count)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("post_engagement_summary_${post.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reactions Summary (Clickable to view who reacted)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = totalReactions > 0, onClick = onOpenReactorsModal)
                            .padding(vertical = 2.dp)
                            .testTag("post_reactions_summary_click_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val distinctEmojis = post.userReactions.values
                            .distinct()
                            .map { resolveReactionOption(it).emoji }
                            .take(3)
                        if (distinctEmojis.isNotEmpty()) {
                            Text(
                                text = distinctEmojis.joinToString(""),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = if (totalReactions > 0) {
                                "${IslamicDateAndPrayerHelper.toBanglaDigits(totalReactions.toString())} জন রিঅ্যাক্ট করেছেন"
                            } else {
                                "প্রথম রিঅ্যাক্ট করুন"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiTextSecondary
                        )
                    }

                    // Comment & Share Counters
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${IslamicDateAndPrayerHelper.toBanglaDigits(effectiveCommentCount.toString())}টি কমেন্ট",
                            modifier = Modifier
                                .clickable(onClick = onToggleCommentsExpanded)
                                .testTag("post_comment_count_label_${post.id}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${IslamicDateAndPrayerHelper.toBanglaDigits(post.shareCount.toString())} শেয়ার",
                            modifier = Modifier.clickable(onClick = onOpenShareDialog),
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = QawmiMintBorder.copy(alpha = 0.7f)
                )

                // 6. Facebook-Style 3-Button Social Action Bar (রিঅ্যাক্ট / কমেন্ট / শেয়ার)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_social_action_bar_${post.id}"),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeReactionOption = if (myReactionCode.isNotBlank()) {
                        resolveReactionOption(myReactionCode)
                    } else null

                    // Button 1: Like / React
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (myReactionCode.isBlank() && !showReactionPicker) {
                                    onReactToPost("SAMMATI")
                                }
                                showReactionPicker = !showReactionPicker
                            }
                            .testTag("post_react_btn_${post.id}"),
                        color = if (activeReactionOption != null) QawmiMintSurface else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeReactionOption != null) {
                                Text(text = activeReactionOption.emoji, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = activeReactionOption.labelBangla,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ThumbUpOffAlt,
                                    contentDescription = "লাইক",
                                    tint = QawmiTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "লাইক / দোয়া",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QawmiTextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Button 2: Comment
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleCommentsExpanded)
                            .testTag("post_comment_btn_${post.id}"),
                        color = if (isCommentsExpanded) QawmiMintSurface.copy(alpha = 0.6f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "কমেন্ট",
                                tint = if (isCommentsExpanded) QawmiDeepGreen else QawmiTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "কমেন্ট",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCommentsExpanded) QawmiDeepGreen else QawmiTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Button 3: Share
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenShareDialog)
                            .testTag("post_share_btn_${post.id}"),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "শেয়ার",
                                tint = QawmiTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "শেয়ার",
                                style = MaterialTheme.typography.labelMedium,
                                color = QawmiTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 7. Expandable Inline Comments & Replies Section
                AnimatedVisibility(visible = isCommentsExpanded) {
                    PostCommentsSection(
                        postId = post.id,
                        currentUserId = currentUserId,
                        isAdmin = isAdmin,
                        comments = postComments,
                        onAddComment = onAddComment,
                        onEditComment = onEditComment,
                        onDeleteComment = onDeleteComment
                    )
                }
            }
        }
    }
}

@Composable
private fun PostCommentsSection(
    postId: String,
    currentUserId: String,
    isAdmin: Boolean,
    comments: List<FeedComment>,
    onAddComment: (text: String, parentCommentId: String, replyToAuthorName: String) -> Unit,
    onEditComment: (commentId: String, newText: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit
) {
    var commentInput by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<FeedComment?>(null) }
    var editingComment by remember { mutableStateOf<FeedComment?>(null) }
    var editingText by remember { mutableStateOf("") }

    val topLevelComments = remember(comments) {
        comments.filter { it.parentCommentId.isBlank() }
    }
    val repliesByParentId = remember(comments) {
        comments.filter { it.parentCommentId.isNotBlank() }.groupBy { it.parentCommentId }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(QawmiSoftBg, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("post_comments_section_$postId"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (comments.isEmpty()) {
            Text(
                text = "এখনো কোনো কমেন্ট নেই। প্রথম কমেন্ট করে আপনার মতামত জানান।",
                style = MaterialTheme.typography.bodySmall,
                color = QawmiTextMuted
            )
        } else {
            topLevelComments.forEach { comment ->
                CommentItemRow(
                    comment = comment,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    isReply = false,
                    onReplyClick = { replyingToComment = comment },
                    onEditClick = {
                        editingComment = comment
                        editingText = comment.text
                    },
                    onDeleteClick = { onDeleteComment(comment.id) }
                )

                val replies = repliesByParentId[comment.id] ?: emptyList()
                replies.forEach { reply ->
                    Box(modifier = Modifier.padding(start = 26.dp)) {
                        CommentItemRow(
                            comment = reply,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            isReply = true,
                            onReplyClick = { replyingToComment = comment },
                            onEditClick = {
                                editingComment = reply
                                editingText = reply.text
                            },
                            onDeleteClick = { onDeleteComment(reply.id) }
                        )
                    }
                }
            }
        }

        // Reply Target Banner
        if (replyingToComment != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = QawmiMintSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "উত্তর দিচ্ছেন: ${replyingToComment!!.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { replyingToComment = null },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "বাতিল", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // New Comment / Reply Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = commentInput,
                onValueChange = { commentInput = it },
                placeholder = {
                    Text(
                        text = if (replyingToComment != null) "উত্তর লিখুন..." else "কমেন্ট লিখুন...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("comment_input_$postId"),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = QawmiDeepGreen,
                    unfocusedBorderColor = QawmiMintBorder
                )
            )
            IconButton(
                onClick = {
                    val trimmed = commentInput.trim()
                    if (trimmed.isNotBlank()) {
                        onAddComment(
                            trimmed,
                            replyingToComment?.id.orEmpty(),
                            replyingToComment?.authorName.orEmpty()
                        )
                        commentInput = ""
                        replyingToComment = null
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(QawmiDeepGreen)
                    .size(38.dp)
                    .testTag("send_comment_btn_$postId")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "কমেন্ট পাঠান",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (editingComment != null) {
        val targetComment = editingComment!!
        AlertDialog(
            onDismissRequest = { editingComment = null },
            modifier = Modifier.testTag("edit_comment_dialog"),
            title = { Text("কমেন্ট সম্পাদনা করুন", fontWeight = FontWeight.Bold, color = QawmiDeepGreen) },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    label = { Text("কমেন্ট") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_comment_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingText.trim().isNotBlank()) {
                            onEditComment(targetComment.id, editingText.trim())
                            editingComment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                    modifier = Modifier.testTag("confirm_edit_comment_btn")
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingComment = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun CommentItemRow(
    comment: FeedComment,
    currentUserId: String,
    isAdmin: Boolean,
    isReply: Boolean,
    onReplyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOwnComment = comment.authorId == currentUserId
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comment_item_${comment.id}"),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, QawmiMintBorder.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatarImage(
                        photoUrl = comment.authorPhotoUrl,
                        displayName = comment.authorName,
                        size = if (isReply) 24.dp else 28.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = buildString {
                                append(formatRelativeBanglaTime(comment.createdAt))
                                if (comment.isEdited) append(" • সম্পাদিত")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (comment.replyToAuthorName.isNotBlank()) {
                Text(
                    text = "@${comment.replyToAuthorName} ",
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiSeekerBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodySmall,
                color = QawmiTextDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "উত্তর দিন",
                    modifier = Modifier
                        .clickable(onClick = onReplyClick)
                        .testTag("reply_comment_btn_${comment.id}"),
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
                if (isOwnComment) {
                    Text(
                        text = "এডিট",
                        modifier = Modifier
                            .clickable(onClick = onEditClick)
                            .testTag("edit_comment_btn_${comment.id}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiSeekerBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isOwnComment || isAdmin) {
                    Text(
                        text = "মুছুন",
                        modifier = Modifier
                            .clickable(onClick = onDeleteClick)
                            .testTag("delete_comment_btn_${comment.id}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlight) QawmiDeepGreen else QawmiTextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = QawmiTextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) QawmiDeepGreen else QawmiTextDark,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickDeeniPostComposerDialog(
    defaultDistrict: String,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        district: String,
        categories: List<String>,
        customCategory: String,
        imageUrls: List<String>
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var district by remember { mutableStateOf(defaultDistrict) }
    var selectedCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var customCategory by remember { mutableStateOf("") }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (attachedImages.size >= ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE) {
                errorMsg = "একটি পোস্টে সর্বোচ্চ ৫টি ছবি বা মিডিয়া যুক্ত করা যাবে।"
            } else {
                val encodeRes = ProfilePhotoHelper.encodeFeedMediaUri(
                    context = context,
                    uri = uri,
                    existingCount = attachedImages.size
                )
                encodeRes.onSuccess { encoded ->
                    attachedImages = attachedImages + encoded
                    errorMsg = null
                }.onFailure { err ->
                    errorMsg = err.localizedMessage
                        ?: "নির্বাচিত ফাইলটি অনেক বড় বা অসমর্থিত। অনুগ্রহ করে ছোট আকারের ছবি বা ভিডিও নির্বাচন করুন।"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("quick_deeni_post_dialog"),
        title = {
            Text(
                text = "দ্বীনি খেদমত বা আলোচনা পোস্ট করুন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QawmiDeepGreen
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMsg = null
                    },
                    label = { Text("পোস্টের শিরোনাম *") },
                    placeholder = { Text("যেমন: দাওরায়ে হাদীস পরীক্ষার প্রস্তুতি পরামর্শ") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_post_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        errorMsg = null
                    },
                    label = { Text("বিস্তারিত বিবরণ বা দ্বীনি নসিহত *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("quick_post_desc_input")
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("জেলা (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "সংশ্লিষ্ট খেদমত বিভাগ (ঐচ্ছিক - একাধিক টিক দেওয়া যাবে):",
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KhedmotCategoryHelper.allCategories.forEach { cat ->
                        val isChecked = cat in selectedCategories
                        FilterChip(
                            selected = isChecked,
                            onClick = {
                                selectedCategories = if (isChecked) selectedCategories - cat else selectedCategories + cat
                            },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                if (KhedmotCategoryHelper.CAT_OTHER in selectedCategories) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("অন্যান্য বিভাগের নাম লিখুন *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.testTag("quick_post_attach_media_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ছবি যুক্ত করুন (${IslamicDateAndPrayerHelper.toBanglaDigits(attachedImages.size.toString())}/৫)",
                            fontSize = 12.sp
                        )
                    }
                }

                if (attachedImages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachedImages.forEachIndexed { idx, img ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(8.dp))
                            ) {
                                ChatAttachmentImage(
                                    imageUrl = img,
                                    contentDescription = "সংযুক্ত ছবি",
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        attachedImages = attachedImages.filterIndexed { i, _ -> i != idx }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "মুছুন",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isBlank() && description.trim().isBlank()) {
                        errorMsg = "অনুগ্রহ করে শিরোনাম বা বিবরণ লিখুন।"
                        return@Button
                    }
                    if (KhedmotCategoryHelper.CAT_OTHER in selectedCategories && customCategory.trim().isBlank()) {
                        errorMsg = "‘অন্যান্য’ নির্বাচন করলে বিভাগের নাম লিখুন।"
                        return@Button
                    }
                    onSubmit(
                        title.trim().ifBlank { description.trim().take(60) },
                        description.trim(),
                        district.trim(),
                        selectedCategories,
                        customCategory.trim(),
                        attachedImages
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                modifier = Modifier.testTag("confirm_quick_post_submit_btn")
            ) {
                Text("ফিডে প্রকাশ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun PostReactorsDialog(
    post: FeedPost,
    onDismiss: () -> Unit,
    onInspectUser: (uid: String, name: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("post_reactors_dialog"),
        title = {
            Text(
                text = "যাঁরা রিঅ্যাক্ট করেছেন (${IslamicDateAndPrayerHelper.toBanglaDigits(post.userReactions.size.toString())})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QawmiDeepGreen
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                post.userReactions.entries.forEach { (uid, code) ->
                    val option = resolveReactionOption(code)
                    val displayName = post.userReactionNames[uid]?.takeIf { it.isNotBlank() } ?: "সম্মানিত দ্বীনি ভাই"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onInspectUser(uid, displayName) },
                        shape = RoundedCornerShape(10.dp),
                        color = QawmiSoftBg
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = QawmiTextDark
                            )
                            Text(
                                text = "${option.emoji} ${option.labelBangla}",
                                style = MaterialTheme.typography.labelMedium,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@Composable
private fun ShareFeedPostDialog(
    post: FeedPost,
    onDismiss: () -> Unit,
    onShareInApp: (caption: String) -> Unit,
    onCopyPostText: () -> Unit,
    onExternalShare: () -> Unit
) {
    var shareCaption by remember {
        mutableStateOf("দ্বীনি খেদমতের এই গুরুত্বপূর্ণ পোস্টটি সবার দৃষ্টি আকর্ষণ করছি।")
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(onClick = onDismiss)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false, onClick = {})
                .testTag("post_share_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = QawmiCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "পোস্ট শেয়ার করুন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = QawmiDeepGreen
                )
                Text(
                    text = "মূল পোস্ট: ${post.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = QawmiTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = shareCaption,
                    onValueChange = { shareCaption = it },
                    label = { Text("আপনার মন্তব্য / ক্যাপশন যুক্ত করুন") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_post_caption_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopyPostText,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_copy_text_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কপি করুন", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onExternalShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_external_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("অন্য অ্যাপে", fontSize = 12.sp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onShareInApp(shareCaption) },
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                        modifier = Modifier.testTag("confirm_share_in_app_btn")
                    ) {
                        Text("ফিডে শেয়ার করুন")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditOwnFeedPostDialog(
    post: FeedPost,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String,
        khedmotCategories: List<String>,
        customCategory: String,
        imageUrls: List<String>
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(post.title) }
    var district by remember { mutableStateOf(post.district) }
    var location by remember { mutableStateOf(post.upazilaOrLocation) }
    var designation by remember { mutableStateOf(post.designation) }
    var qualification by remember { mutableStateOf(post.qualification) }
    var salaryOrExp by remember { mutableStateOf(post.salaryOrExperience) }
    var deadlineOrPref by remember { mutableStateOf(post.deadlineOrPreferredDistricts) }
    var description by remember { mutableStateOf(post.description) }
    var contactNumber by remember { mutableStateOf(post.contactNumber) }
    var selectedCategories by remember { mutableStateOf(post.khedmotCategories) }
    var customCategory by remember { mutableStateOf(post.customCategory) }
    var attachedImages by remember { mutableStateOf(post.imageUrls) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (attachedImages.size >= ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE) {
                errorMsg = "একটি পোস্টে সর্বোচ্চ ৫টি ছবি যুক্ত করা যাবে।"
            } else {
                val encodeRes = ProfilePhotoHelper.encodeFeedMediaUri(
                    context = context,
                    uri = uri,
                    existingCount = attachedImages.size
                )
                encodeRes.onSuccess { encoded ->
                    attachedImages = attachedImages + encoded
                    errorMsg = null
                }.onFailure { err ->
                    errorMsg = err.localizedMessage ?: "ছবি যুক্ত করা সম্ভব হয়নি।"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("edit_own_post_dialog"),
        title = {
            Text(
                text = "পোস্ট সম্পাদনা করুন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QawmiDeepGreen
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("শিরোনাম / নাম") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_post_title_input")
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("জেলা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("খেদমতের পদ / বিভাগ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "খেদমতের ক্যাটাগরি:",
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KhedmotCategoryHelper.allCategories.forEach { cat ->
                        val isChecked = cat in selectedCategories
                        FilterChip(
                            selected = isChecked,
                            onClick = {
                                selectedCategories = if (isChecked) selectedCategories - cat else selectedCategories + cat
                            },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
                if (KhedmotCategoryHelper.CAT_OTHER in selectedCategories) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("অন্যান্য খেদমতের ধরন") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("বিস্তারিত বিবরণ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("edit_post_desc_input")
                )

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = attachedImages.size < ProfilePhotoHelper.MAX_CHAT_IMAGES_PER_MESSAGE,
                    modifier = Modifier.testTag("edit_post_attach_media_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ছবি যুক্ত করুন (${IslamicDateAndPrayerHelper.toBanglaDigits(attachedImages.size.toString())}/৫)",
                        fontSize = 12.sp
                    )
                }

                if (attachedImages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachedImages.forEachIndexed { idx, img ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(8.dp))
                            ) {
                                ChatAttachmentImage(
                                    imageUrl = img,
                                    contentDescription = "সংযুক্ত ছবি",
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        attachedImages = attachedImages.filterIndexed { i, _ -> i != idx }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "মুছুন",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg.orEmpty(),
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        district,
                        location,
                        designation,
                        qualification,
                        salaryOrExp,
                        deadlineOrPref,
                        description,
                        contactNumber,
                        selectedCategories,
                        customCategory,
                        attachedImages
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen),
                modifier = Modifier.testTag("confirm_edit_post_btn")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun ReportFeedPostDialog(
    post: FeedPost,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("অসত্য বা বিভ্রান্তিকর তথ্য") }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("report_post_dialog"),
        title = {
            Text(
                text = "পোস্ট রিপোর্ট করুন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "পোস্ট: ${post.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = QawmiTextSecondary
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("রিপোর্টের কারণ লিখুন") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_post_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitReport(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                modifier = Modifier.testTag("confirm_report_post_btn")
            ) {
                Text("রিপোর্ট পাঠান")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

private fun formatRelativeBanglaTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "এইমাত্র"
    val nowMs = System.currentTimeMillis()
    val postMs = timestamp.toDate().time
    val diffMinutes = ((nowMs - postMs) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMinutes < 1L -> "এইমাত্র"
        diffMinutes < 60L -> "${IslamicDateAndPrayerHelper.toBanglaDigits(diffMinutes.toString())} মিনিট আগে"
        diffMinutes < 1440L -> {
            val hours = diffMinutes / 60L
            "${IslamicDateAndPrayerHelper.toBanglaDigits(hours.toString())} ঘণ্টা আগে"
        }
        else -> {
            val days = diffMinutes / 1440L
            "${IslamicDateAndPrayerHelper.toBanglaDigits(days.toString())} দিন আগে"
        }
    }
}

private fun buildShareablePostText(post: FeedPost): String {
    val badges = KhedmotCategoryHelper.resolveDisplayBadges(post)
    return buildString {
        appendLine("🕌 কওমি কর্মসংস্থান বাংলাদেশ")
        appendLine("শিরোনাম: ${post.title}")
        if (post.authorName.isNotBlank()) appendLine("প্রকাশক: ${post.authorName}")
        if (post.designation.isNotBlank()) appendLine("খেদমতের পদ/বিভাগ: ${post.designation}")
        if (badges.isNotEmpty()) appendLine("ক্যাটাগরি: ${badges.joinToString(", ")}")
        if (post.district.isNotBlank()) appendLine("জেলা: ${post.district}")
        if (post.qualification.isNotBlank()) appendLine("যোগ্যতা: ${post.qualification}")
        if (post.contactNumber.isNotBlank()) appendLine("যোগাযোগ: ${post.contactNumber}")
        if (post.description.isNotBlank()) {
            appendLine()
            appendLine(post.description)
        }
    }.trim()
}
