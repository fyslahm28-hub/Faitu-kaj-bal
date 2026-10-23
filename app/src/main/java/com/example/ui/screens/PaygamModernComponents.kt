package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatGroup
import com.example.data.model.DirectMessage
import com.example.data.model.IslamicReaction
import com.example.data.model.ReplyQuoteInfo
import com.example.data.model.UserProfile
import com.example.ui.components.ChatAttachmentImage
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.QawmiAlertRed
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextMuted
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.util.ProfilePhotoHelper
import kotlinx.coroutines.launch

val PaygamComposerEmojis = listOf(
    "👍", "❤️", "😂", "😮", "😢", "🙏",
    "🤲", "🌙", "✨", "🕌", "😊", "🥰",
    "😇", "🤔", "😭", "🎉", "🔥", "💯",
    "✅", "🌸", "📖", "🕊️", "🌿", "⭐"
)

fun copyMessageTextToClipboard(context: Context, text: String): Boolean {
    if (text.isBlank()) return false
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        val clip = ClipData.newPlainText("পয়গাম", text)
        clipboard.setPrimaryClip(clip)
        true
    } catch (_: Exception) {
        false
    }
}

fun shareImageOrTextFromViewer(context: Context, senderName: String, imageUrl: String) {
    try {
        val shareText = if (imageUrl.startsWith("http", ignoreCase = true)) {
            "$senderName কর্তৃক প্রেরিত ছবি:\n$imageUrl"
        } else {
            "$senderName কর্তৃক প্রেরিত পয়গামের ছবি (কওমি কর্মসংস্থান বাংলাদেশ)"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "পয়গামের ছবি")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "শেয়ার করুন").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

/**
 * Reaction Details Modal ("রিঅ্যাকশন • মোট X জন") matching the user's reference design.
 * Shows every user who reacted to the message with their avatar, name, and reaction emoji.
 */
@Composable
fun PaygamReactionDetailsDialog(
    message: DirectMessage,
    currentUserId: String,
    currentUserProfile: UserProfile?,
    partnerProfiles: Map<String, UserProfile>,
    activeGroup: ChatGroup?,
    onRemoveOwnReaction: () -> Unit,
    onDismiss: () -> Unit
) {
    val mergedReactions = remember(message) { message.mergedUserReactions() }
    val totalCount = mergedReactions.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(22.dp))
                .testTag("reaction_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "রিঅ্যাকশন • মোট ${IslamicDateAndPrayerHelper.toBanglaDigits(totalCount.toString())} জন",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F3))
                            .testTag("close_reaction_details_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = QawmiTextDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = QawmiMintBorder)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    mergedReactions.entries.forEach { (reactorId, codeOrEmoji) ->
                        val isMe = reactorId == currentUserId
                        val reactorProfile = if (isMe) currentUserProfile else partnerProfiles[reactorId]
                        val reactorName = when {
                            isMe -> {
                                val myName = currentUserProfile?.fullName?.takeIf { it.isNotBlank() }
                                    ?: if (message.senderId == currentUserId) message.senderName else ""
                                if (myName.isNotBlank()) "$myName (আপনি)" else "আপনি"
                            }
                            reactorId == message.senderId && message.senderName.isNotBlank() -> message.senderName
                            reactorId == message.receiverId && !message.receiverId.startsWith("GROUP:") && message.receiverName.isNotBlank() -> message.receiverName
                            activeGroup?.memberNames?.get(reactorId)?.isNotBlank() == true -> activeGroup.memberNames[reactorId].orEmpty()
                            reactorProfile?.fullName?.isNotBlank() == true -> reactorProfile.fullName
                            else -> "সম্মানিত সদস্য"
                        }
                        val reactorPhoto = when {
                            isMe -> currentUserProfile?.photoUrl.orEmpty()
                            reactorProfile?.photoUrl?.isNotBlank() == true -> reactorProfile.photoUrl
                            reactorId == message.senderId -> message.senderPhotoUrl
                            else -> ""
                        }
                        val resolvedEmoji = IslamicReaction.resolveEmoji(codeOrEmoji)
                        val resolvedTitle = IslamicReaction.resolveBanglaTitle(codeOrEmoji)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = isMe) {
                                    onRemoveOwnReaction()
                                    onDismiss()
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .testTag("reactor_row_$reactorId"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                UserAvatarImage(
                                    photoUrl = reactorPhoto,
                                    displayName = reactorName,
                                    size = 42.dp,
                                    borderColor = QawmiMintBorder,
                                    borderWidth = 1.dp,
                                    showOnlineIndicator = false
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = reactorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isMe) "$resolvedTitle • সরাতে ট্যাপ করুন" else resolvedTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiTextSecondary
                                    )
                                }
                            }
                            Text(
                                text = resolvedEmoji,
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QawmiMintSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiMintBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "মেসেজে দীর্ঘক্ষণ চাপ দিয়ে রিঅ্যাকশন যোগ বা পরিবর্তন করুন",
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

/**
 * Delete Confirmation Dialog ("মেসেজ ডিলিট করুন") matching the user's reference design.
 * Provides two radio options:
 * 1. শুধু আমার জন্য
 * 2. সবার জন্য (সকলের কাছে মুছে যাবে) [Only available for the original sender]
 */
@Composable
fun PaygamDeleteConfirmationDialog(
    message: DirectMessage,
    isMyMessage: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit
) {
    val canDeleteForEveryone = isMyMessage && !message.isDeletedForEveryone
    var deleteForEveryoneSelected by remember(canDeleteForEveryone) {
        mutableStateOf(canDeleteForEveryone)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(22.dp))
                .testTag("delete_message_confirm_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = QawmiAlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "মেসেজ ডিলিট করুন",
                            style = MaterialTheme.typography.titleMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "আপনি কীভাবে মেসেজটি মুছে ফেলতে চান?",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary
                        )
                    }
                }

                // Option 1: শুধু আমার জন্য
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!deleteForEveryoneSelected) QawmiMintSurface else Color(0xFFF8FAF9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (!deleteForEveryoneSelected) 1.5.dp else 1.dp,
                            color = if (!deleteForEveryoneSelected) QawmiDeepGreen else QawmiMintBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { deleteForEveryoneSelected = false }
                        .testTag("delete_radio_for_me")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (!deleteForEveryoneSelected) {
                                Icons.Default.RadioButtonChecked
                            } else {
                                Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = if (!deleteForEveryoneSelected) QawmiDeepGreen else QawmiTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "শুধু আমার জন্য মুছুন",
                                style = MaterialTheme.typography.bodyMedium,
                                color = QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "শুধুমাত্র আপনার চ্যাট তালিকা থেকে মেসেজটি মুছে যাবে",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiTextSecondary
                            )
                        }
                    }
                }

                // Option 2: সবার জন্য (সকলের কাছে মুছে যাবে) — strictly for sender
                if (canDeleteForEveryone) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (deleteForEveryoneSelected) Color(0xFFFFF5F5) else Color(0xFFF8FAF9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (deleteForEveryoneSelected) 1.5.dp else 1.dp,
                                color = if (deleteForEveryoneSelected) QawmiAlertRed else QawmiMintBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { deleteForEveryoneSelected = true }
                            .testTag("delete_radio_for_everyone")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (deleteForEveryoneSelected) {
                                    Icons.Default.RadioButtonChecked
                                } else {
                                    Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (deleteForEveryoneSelected) QawmiAlertRed else QawmiTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "সবার জন্য মুছুন (সকলের কাছে মুছে যাবে)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = QawmiTextDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "কথোপকথনের সকল সদস্যের কাছ থেকে মেসেজটি প্রত্যাহার হবে",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiTextSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_delete_dialog_button")
                    ) {
                        Text("বাতিল", color = QawmiTextDark, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            if (deleteForEveryoneSelected && canDeleteForEveryone) {
                                onDeleteForEveryone()
                            } else {
                                onDeleteForMe()
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = QawmiAlertRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_delete_dialog_button")
                    ) {
                        Text("ডিলিট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen Messenger-Style Image Viewer matching the user's reference image.
 * - Opens in full-screen dark mode (NOT a small white card).
 * - Top bar: Back arrow, sender avatar, sender name, timestamp, Download, Share, and 3-dot menu.
 * - Center: HorizontalPager with swipe left/right, `<` `>` arrows, double-tap zoom, and natural aspect ratio (`ContentScale.Fit`).
 * - Bottom: Thumbnail strip with green active border + `১/৫` counter pill.
 */
@Composable
fun PaygamFullScreenImageViewerDialog(
    images: List<String>,
    initialIndex: Int,
    senderName: String,
    senderPhotoUrl: String,
    timestampText: String,
    imageSaveFeedback: String?,
    onSaveSingleImage: (String, Int) -> Unit,
    onSaveAllImages: (List<String>) -> Unit,
    onShareImage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val safeInitial = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { images.size.coerceAtLeast(1) }
    )
    val coroutineScope = rememberCoroutineScope()
    var showViewerMenu by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        zoomScale = 1f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    val currentIndex = pagerState.currentPage.coerceIn(0, (images.size - 1).coerceAtLeast(0))
    val currentImageUrl = images.getOrElse(currentIndex) { "" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07110D))
                .testTag("fullscreen_image_viewer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 1. TOP BAR: Back + Sender Avatar/Name/Time + Download + Share + 3-Dot Menu
                Surface(
                    color = Color(0xDD0A1F17),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_fullscreen_viewer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "বন্ধ করুন",
                                    tint = Color.White
                                )
                            }
                            UserAvatarImage(
                                photoUrl = senderPhotoUrl,
                                displayName = senderName.ifBlank { "প্রেরক" },
                                size = 38.dp,
                                borderColor = QawmiGold,
                                borderWidth = 1.dp,
                                showOnlineIndicator = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = senderName.ifBlank { "সম্মানিত প্রেরক" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = timestampText.ifBlank { "এইমাত্র" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB2DFDB),
                                    maxLines = 1
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x33FFFFFF),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable {
                                        when {
                                            zoomScale < 1.8f -> {
                                                zoomScale = 2f
                                            }
                                            zoomScale < 2.8f -> {
                                                zoomScale = 3f
                                            }
                                            else -> {
                                                zoomScale = 1f
                                                panOffsetX = 0f
                                                panOffsetY = 0f
                                            }
                                        }
                                    }
                                    .testTag("toggle_viewer_zoom_button")
                            ) {
                                Text(
                                    text = when {
                                        zoomScale >= 2.8f -> "🔍 ৩x"
                                        zoomScale >= 1.8f -> "🔍 ২x"
                                        else -> "🔍 জুম"
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            IconButton(
                                onClick = { onSaveSingleImage(currentImageUrl, currentIndex + 1) },
                                modifier = Modifier.testTag("save_single_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "গ্যালারিতে সংরক্ষণ",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { onShareImage(currentImageUrl) },
                                modifier = Modifier.testTag("share_viewer_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "শেয়ার করুন",
                                    tint = Color.White
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { showViewerMenu = true },
                                    modifier = Modifier.testTag("viewer_three_dot_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "আরও অপশন",
                                        tint = Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = showViewerMenu,
                                    onDismissRequest = { showViewerMenu = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("গ্যালারিতে সংরক্ষণ করুন", color = QawmiDeepGreen, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = QawmiDeepGreen) },
                                        onClick = {
                                            showViewerMenu = false
                                            onSaveSingleImage(currentImageUrl, currentIndex + 1)
                                        }
                                    )
                                    if (images.size > 1) {
                                        DropdownMenuItem(
                                            text = { Text("সবগুলো সংরক্ষণ করুন (${IslamicDateAndPrayerHelper.toBanglaDigits(images.size.toString())}টি)", color = QawmiDeepGreen, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = QawmiDeepGreen) },
                                            onClick = {
                                                showViewerMenu = false
                                                onSaveAllImages(images)
                                            },
                                            modifier = Modifier.testTag("save_all_images_button")
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("পুনরায় লোড করুন", color = QawmiTextDark) },
                                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = QawmiTextDark) },
                                        onClick = {
                                            showViewerMenu = false
                                            reloadNonce++
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. CENTER FULL-SCREEN IMAGE PAGER (Natural aspect ratio, double-tap zoom, left/right navigation)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = zoomScale <= 1.05f,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val pageUrl = images.getOrElse(pageIdx) { "" }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .pointerInput(pageIdx) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (zoomScale > 2.4f) {
                                                zoomScale = 1f
                                                panOffsetX = 0f
                                                panOffsetY = 0f
                                            } else if (zoomScale > 1.2f) {
                                                zoomScale = 3f
                                            } else {
                                                zoomScale = 2f
                                            }
                                        }
                                    )
                                }
                                .pointerInput(pageIdx, zoomScale > 1.02f) {
                                    if (zoomScale > 1.02f) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                            zoomScale = newScale
                                            if (newScale <= 1.02f) {
                                                panOffsetX = 0f
                                                panOffsetY = 0f
                                            } else {
                                                val maxPanX = (size.width * (newScale - 1f)) / 2f
                                                val maxPanY = (size.height * (newScale - 1f)) / 2f
                                                panOffsetX = (panOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                                panOffsetY = (panOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (pageUrl.isBlank()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ছবি লোড করা যায়নি",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(onClick = { reloadNonce++ }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("পুনরায় চেষ্টা করুন", color = Color.White)
                                    }
                                }
                            } else {
                                key(pageUrl, reloadNonce) {
                                    ChatAttachmentImage(
                                        imageUrl = pageUrl,
                                        contentDescription = "পূর্ণ ছবি ${pageIdx + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = if (pageIdx == currentIndex) zoomScale else 1f,
                                                scaleY = if (pageIdx == currentIndex) zoomScale else 1f,
                                                translationX = if (pageIdx == currentIndex) panOffsetX else 0f,
                                                translationY = if (pageIdx == currentIndex) panOffsetY else 0f
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Left / Right Arrow overlays when multiple images exist
                    if (images.size > 1 && currentIndex > 0) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentIndex - 1)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 10.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .testTag("viewer_prev_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "পূর্ববর্তী ছবি",
                                tint = Color.White
                            )
                        }
                    }
                    if (images.size > 1 && currentIndex < images.lastIndex) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentIndex + 1)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .testTag("viewer_next_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "পরবর্তী ছবি",
                                tint = Color.White
                            )
                        }
                    }

                    // Feedback toast overlay when saving image
                    if (!imageSaveFeedback.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = QawmiDeepGreen,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = imageSaveFeedback,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // 3. BOTTOM THUMBNAIL STRIP & COUNTER (১/৫)
                Surface(
                    color = Color(0xEE0A1F17),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                images.forEachIndexed { idx, thumb ->
                                    val isCurrent = idx == currentIndex
                                    ChatAttachmentImage(
                                        imageUrl = thumb,
                                        contentDescription = "থাম্বনেইল ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(if (isCurrent) 54.dp else 46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isCurrent) 2.5.dp else 1.dp,
                                                color = if (isCurrent) Color(0xFF00E676) else Color(0x55FFFFFF),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(idx)
                                                }
                                            }
                                            .testTag("viewer_thumbnail_$idx")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x33FFFFFF),
                                modifier = Modifier.testTag("viewer_counter_badge")
                            ) {
                                Text(
                                    text = "${IslamicDateAndPrayerHelper.toBanglaDigits((currentIndex + 1).toString())}/${IslamicDateAndPrayerHelper.toBanglaDigits(images.size.toString())}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quoted Reply Banner shown above the composer when replying to a message.
 */
@Composable
fun PaygamReplyComposerBanner(
    replyingTo: DirectMessage,
    onCancelReply: () -> Unit
) {
    val snippet = remember(replyingTo) {
        if (replyingTo.imageUrls.isNotEmpty() && replyingTo.body.startsWith("📷")) {
            "📷 ছবি (${IslamicDateAndPrayerHelper.toBanglaDigits(replyingTo.imageUrls.size.toString())}টি)"
        } else {
            replyingTo.body.ifBlank { "📷 ছবি" }
        }
    }
    Surface(
        color = Color(0xFFEAF7F0),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(12.dp))
            .testTag("reply_composer_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(QawmiDeepGreen)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${replyingTo.senderName.ifBlank { "ব্যবহারকারী" }}-এর মেসেজের উত্তর",
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onCancelReply,
                modifier = Modifier
                    .size(26.dp)
                    .testTag("cancel_reply_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "উত্তর বাতিল করুন",
                    tint = QawmiTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Quoted Reply Card displayed inside a message bubble when `msg.parsedReplyQuote()` is present.
 */
@Composable
fun PaygamBubbleQuotedReplyCard(
    quote: ReplyQuoteInfo,
    sentByMe: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (sentByMe) Color(0x26000000) else Color(0xFFF1F7F4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .testTag("message_quoted_reply_box")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (sentByMe) QawmiGold else QawmiDeepGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quote.replyToSenderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sentByMe) QawmiGold else QawmiDeepGreen,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = quote.replyToSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (sentByMe) Color(0xFFE0F2F1) else QawmiTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Toggleable Emoji Picker Panel above the message composer.
 */
@Composable
fun PaygamEmojiPickerStrip(
    onEmojiSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Color(0xFFF4FAF7),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QawmiMintBorder)
            .testTag("emoji_picker_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ইমোজি ও অভিবাদন যুক্ত করুন",
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = QawmiTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(PaygamComposerEmojis) { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(38.dp)
                            .border(1.dp, QawmiMintBorder, CircleShape)
                            .clickable { onEmojiSelected(emoji) }
                            .testTag("composer_emoji_$emoji")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 20.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
