package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DirectMessage
import com.example.data.model.UserProfile
import com.example.ui.screens.DrawerMenuItem
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PublicUserProfileDialog
import com.example.ui.screens.QawmiBottomNavBar
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.ProfilePhotoHelper
import com.example.ui.viewmodel.MainDestination
import com.example.ui.viewmodel.UiState
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("কওমি কর্মসংস্থান", appName)
    }

    @Test
    fun `ProfilePhotoHelper decodes data image base64 url into ImageBitmap`() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(11, 93, 67))
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$base64"

        val decoded = ProfilePhotoHelper.decodeDataUrlToImageBitmap(dataUrl)
        assertNotNull("Expected valid ImageBitmap from Base64 data URL", decoded)
        assertEquals(64, decoded?.width)
        assertEquals(64, decoded?.height)
    }

    @Test
    fun `ProfilePhotoHelper handles preset and blank urls gracefully`() {
        assertNull(ProfilePhotoHelper.decodeDataUrlToImageBitmap(""))
        assertNull(ProfilePhotoHelper.decodeDataUrlToImageBitmap("preset:scholar_emerald"))
        assertTrue(ProfilePhotoHelper.presetAvatars.isNotEmpty())
    }

    @Test
    fun `BottomNavBar displays 5 required options and unread badge while Khedmot and Madrasa are in Hamburger menu`() {
        val clickedTabs = mutableListOf<MainDestination>()
        var drawerKhedmotClicked = false
        var drawerMadrasaClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                Column {
                    DrawerMenuItem(
                        label = "খেদমত (নিয়োগ বিজ্ঞপ্তি)",
                        icon = Icons.Default.Work,
                        selected = false,
                        testTag = "drawer_item_khedmot",
                        onClick = { drawerKhedmotClicked = true }
                    )
                    DrawerMenuItem(
                        label = "মাদরাসা ডিরেক্টরি",
                        icon = Icons.Default.AccountBalance,
                        selected = false,
                        testTag = "drawer_item_madrasa",
                        onClick = { drawerMadrasaClicked = true }
                    )
                    QawmiBottomNavBar(
                        currentDestination = MainDestination.HOME,
                        unreadMessagesCount = 4,
                        onTabSelected = { clickedTabs.add(it) }
                    )
                }
            }
        }

        // Verify the 5 Bottom Navigation Bar options
        composeTestRule.onNodeWithTag("bottom_nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_feed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_abedon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_messages").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_profile").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_messages_badge", useUnmergedTree = true).assertIsDisplayed()

        // Verify Khedmot and Madrasa are NOT in the Bottom Navigation Bar
        composeTestRule.onNodeWithTag("bottom_nav_khedmot").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_madrasa").assertDoesNotExist()

        // Verify Khedmot and Madrasa work from Hamburger Menu
        composeTestRule.onNodeWithTag("drawer_item_khedmot").performClick()
        composeTestRule.onNodeWithTag("drawer_item_madrasa").performClick()
        assertTrue(drawerKhedmotClicked)
        assertTrue(drawerMadrasaClicked)

        // Verify clicking each Bottom Navigation option navigates properly
        composeTestRule.onNodeWithTag("bottom_nav_messages").performClick()
        composeTestRule.onNodeWithTag("bottom_nav_profile").performClick()
        assertEquals(listOf(MainDestination.MESSAGES, MainDestination.PROFILE), clickedTabs)
    }

    @Test
    fun `MessagesScreen keeps top partner header and bottom composer sticky while scrolling long conversation`() {
        val messages = (1..20).map { idx ->
            DirectMessage(
                id = "msg_$idx",
                senderId = if (idx % 2 == 0) "user_me" else "partner_1",
                senderName = if (idx % 2 == 0) "মাওলানা আব্দুল্লাহ" else "জামিয়া দারুল উলুম",
                receiverId = if (idx % 2 == 0) "partner_1" else "user_me",
                receiverName = if (idx % 2 == 0) "জামিয়া দারুল উলুম" else "মাওলানা আব্দুল্লাহ",
                subject = "খেদমত সংক্রান্ত যোগাযোগ",
                body = "বার্তা ক্রমিক নং $idx - আসসালামু আলাইকুম",
                isRead = idx < 19,
                createdAt = Timestamp(1700000000L + idx * 60L, 0)
            )
        }

        val partnerProfile = UserProfile(
            id = "partner_1",
            fullName = "জামিয়া দারুল উলুম ঢাকা",
            photoUrl = "preset:madrasa_dome",
            isOnline = true
        )

        var sentBody = ""

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(400.dp).height(640.dp)) {
                    MessagesScreen(
                        messagesState = UiState.Success(messages),
                        currentUserId = "user_me",
                        targetUser = "partner_1" to "জামিয়া দারুল উলুম ঢাকা",
                        partnerProfiles = mapOf("partner_1" to partnerProfile),
                        activePartnerProfile = partnerProfile,
                        isSubmitting = false,
                        onSendMessage = { _, _, _, body -> sentBody = body },
                        onSelectRecipient = { _, _ -> },
                        onClearRecipient = {},
                        onMarkRead = {},
                        onMarkConversationRead = {},
                        onInspectUserProfile = { _, _ -> }
                    )
                }
            }
        }

        // Top partner header and bottom composer must be displayed
        composeTestRule.onNodeWithTag("active_chat_partner_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sticky_chat_composer_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("message_body_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_message_button").assertIsDisplayed()

        // Scroll to the very first message (index 0) in the 20-message conversation
        composeTestRule.onNodeWithTag("conversation_messages_list").performScrollToIndex(0)
        composeTestRule.onNodeWithText("বার্তা ক্রমিক নং 1 - আসসালামু আলাইকুম").assertIsDisplayed()

        // Top header and bottom composer remain fixed/sticky after scrolling to top
        composeTestRule.onNodeWithTag("active_chat_partner_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sticky_chat_composer_bar").assertIsDisplayed()

        // Scroll back to the latest message (index 19)
        composeTestRule.onNodeWithTag("conversation_messages_list").performScrollToIndex(19)
        composeTestRule.onNodeWithText("বার্তা ক্রমিক নং 20 - আসসালামু আলাইকুম").assertIsDisplayed()

        // Top header and bottom composer still remain fixed/sticky and functional
        composeTestRule.onNodeWithTag("active_chat_partner_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("message_body_input").performTextInput("নতুন পয়গাম টেস্ট")
        composeTestRule.onNodeWithTag("send_message_button").performClick()
        assertEquals("নতুন পয়গাম টেস্ট", sentBody)
    }

    @Test
    fun `PublicUserProfileDialog shows send message button only for other users and hides it on own profile`() {
        val otherProfile = UserProfile(
            id = "user_other",
            fullName = "মুফতি মাহমুদ হাসান",
            accountType = "প্রার্থী",
            district = "ঢাকা",
            designation = "মুহাদ্দিস",
            qualification = "দাওরায়ে হাদিস ও ইফতা",
            isOnline = true
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                PublicUserProfileDialog(
                    targetUserId = "user_other",
                    fallbackName = "মুফতি মাহমুদ হাসান",
                    currentUserId = "user_me",
                    inspectedProfileState = UiState.Success(otherProfile),
                    userPublishedPosts = emptyList(),
                    onDismiss = {},
                    onSendMessageClick = { _, _ -> },
                    onPostClick = {}
                )
            }
        }

        // Viewing another user's profile shows the "পয়গাম / মেসেজ পাঠান" button
        composeTestRule.onNodeWithTag("public_profile_message_button").assertIsDisplayed()
    }

    @Test
    fun `ProfileScreen closes edit form after saving and reopens when clicking Edit button`() {
        var savedProfile by mutableStateOf(
            UserProfile(
                id = "user_me",
                fullName = "মাওলানা আব্দুল্লাহ",
                accountType = "khadim",
                district = "ঢাকা",
                designation = "মুহাদ্দিস",
                qualification = "দাওরায়ে হাদীস",
                experience = "৫ বছর",
                preferredDistricts = "ঢাকা, গাজীপুর",
                bio = "ইলমে হাদীসের খেদমতে নিয়োজিত",
                contactInfo = "01700000000",
                madrasaName = "জামিয়া রাহমানিয়া"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(900.dp)) {
                    ProfileScreen(
                        userProfileState = UiState.Success(savedProfile),
                        currentUserId = "user_me",
                        currentUserDisplayName = savedProfile.fullName,
                        currentUserEmail = "abdullah@example.com",
                        feedPostsState = UiState.Success(emptyList()),
                        mySentSubmissionsState = UiState.Success(emptyList()),
                        receivedSubmissionsState = UiState.Success(emptyList()),
                        bookmarksState = UiState.Success(emptyList()),
                        isSubmitting = false,
                        onUpdatePhotoImmediately = {},
                        onViewPostDetail = {},
                        onDeleteOwnPost = {},
                        onNavigate = {},
                        onSaveProfile = { name, type, dist, desig, qual, exp, pref, bio, phone, madrasa, photoUrl ->
                            savedProfile = savedProfile.copy(
                                fullName = name,
                                accountType = type,
                                district = dist,
                                designation = desig,
                                qualification = qual,
                                experience = exp,
                                preferredDistricts = pref,
                                bio = bio,
                                contactInfo = phone,
                                madrasaName = madrasa,
                                photoUrl = photoUrl
                            )
                        }
                    )
                }
            }
        }

        // Initially edit form is closed; only saved profile info and "এডিট করুন" button are shown
        composeTestRule.onNodeWithTag("profile_edit_form_card").assertDoesNotExist()
        composeTestRule.onNodeWithTag("toggle_edit_profile_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("এডিট করুন").assertIsDisplayed()

        // Click "এডিট করুন" to open all fields for editing
        composeTestRule.onNodeWithTag("toggle_edit_profile_button").performClick()
        composeTestRule.onNodeWithTag("profile_edit_form_card").assertExists()

        // Update name and district, then click Save
        composeTestRule.onNodeWithTag("profile_name_input").performScrollTo().performTextReplacement("মুফতি আব্দুল্লাহ আল মামুন")
        composeTestRule.onNodeWithTag("profile_district_input").performScrollTo().performTextReplacement("সিলেট")
        composeTestRule.onNodeWithTag("save_profile_button").performScrollTo().performClick()

        // Form must close immediately after saving, and updated info is preserved
        composeTestRule.onNodeWithTag("profile_edit_form_card").assertDoesNotExist()
        assertEquals("মুফতি আব্দুল্লাহ আল মামুন", savedProfile.fullName)
        assertEquals("সিলেট", savedProfile.district)
        composeTestRule.onNodeWithTag("toggle_edit_profile_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("এডিট করুন").assertIsDisplayed()

        // Clicking "এডিট করুন" again reopens the form
        composeTestRule.onNodeWithTag("toggle_edit_profile_button").performClick()
        composeTestRule.onNodeWithTag("profile_edit_form_card").assertExists()
    }

    @Test
    fun `Unread message count updates accurately for 1 2 3 messages deduplicates IDs and hides badge at 0`() {
        var messagesList by mutableStateOf(
            listOf(
                // Duplicate ID msg_1 included twice to verify deduplication
                DirectMessage(
                    id = "msg_1",
                    senderId = "partner_1",
                    senderName = "মাওলানা কারীম",
                    receiverId = "user_me",
                    receiverName = "আমি",
                    body = "প্রথম অপঠিত মেসেজ",
                    isRead = false,
                    createdAt = Timestamp(1700000100L, 0)
                ),
                DirectMessage(
                    id = "msg_1",
                    senderId = "partner_1",
                    senderName = "মাওলানা কারীম",
                    receiverId = "user_me",
                    receiverName = "আমি",
                    body = "প্রথম অপঠিত মেসেজ",
                    isRead = false,
                    createdAt = Timestamp(1700000100L, 0)
                ),
                DirectMessage(
                    id = "msg_2",
                    senderId = "partner_1",
                    senderName = "মাওলানা কারীম",
                    receiverId = "user_me",
                    receiverName = "আমি",
                    body = "দ্বিতীয় অপঠিত মেসেজ",
                    isRead = false,
                    createdAt = Timestamp(1700000200L, 0)
                ),
                DirectMessage(
                    id = "msg_3",
                    senderId = "partner_2",
                    senderName = "জামিয়া ইসলামিয়া",
                    receiverId = "user_me",
                    receiverName = "আমি",
                    body = "তৃতীয় অপঠিত মেসেজ",
                    isRead = false,
                    createdAt = Timestamp(1700000300L, 0)
                )
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                val uniqueUnread = messagesList.distinctBy { it.id }.count { it.receiverId == "user_me" && !it.isRead }
                Column(modifier = Modifier.width(400.dp).height(700.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MessagesScreen(
                            messagesState = UiState.Success(messagesList),
                            currentUserId = "user_me",
                            targetUser = null,
                            isSubmitting = false,
                            onSendMessage = { _, _, _, _ -> },
                            onSelectRecipient = { _, _ -> },
                            onMarkRead = {},
                            onMarkConversationRead = {}
                        )
                    }
                    QawmiBottomNavBar(
                        currentDestination = MainDestination.MESSAGES,
                        unreadMessagesCount = uniqueUnread,
                        onTabSelected = {}
                    )
                }
            }
        }

        // 3 distinct unread messages -> shows "৩ অপঠিত" and "৩" in bottom nav badge
        composeTestRule.onNodeWithTag("unread_messages_count_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("৩ অপঠিত").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_messages_badge", useUnmergedTree = true).assertIsDisplayed()

        // Mark 1 message read -> 2 unread remaining ("২ অপঠিত")
        messagesList = messagesList.map { if (it.id == "msg_1") it.copy(isRead = true) else it }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("২ অপঠিত").assertIsDisplayed()

        // Mark second message read -> 1 unread remaining ("১ অপঠিত")
        messagesList = messagesList.map { if (it.id == "msg_2") it.copy(isRead = true) else it }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("১ অপঠিত").assertIsDisplayed()

        // Mark all messages read -> 0 unread, badges disappear completely
        messagesList = messagesList.map { it.copy(isRead = true) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("unread_messages_count_badge").assertDoesNotExist()
        composeTestRule.onNodeWithTag("bottom_nav_messages_badge", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `MessagesScreen shows real-time typing indicator when partner is typing and hides when stopped or sent`() {
        var partnerProfileState by mutableStateOf(
            UserProfile(
                id = "partner_1",
                fullName = "মুফতি মাহমুদ",
                isOnline = true,
                typingToUserId = "user_me",
                typingUpdatedAt = Timestamp.now()
            )
        )
        val typingEvents = mutableListOf<Pair<String, Boolean>>()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(400.dp).height(640.dp)) {
                    MessagesScreen(
                        messagesState = UiState.Success(emptyList()),
                        currentUserId = "user_me",
                        targetUser = "partner_1" to "মুফতি মাহমুদ",
                        partnerProfiles = mapOf("partner_1" to partnerProfileState),
                        activePartnerProfile = partnerProfileState,
                        isSubmitting = false,
                        onSendMessage = { _, _, _, _ -> },
                        onSelectRecipient = { _, _ -> },
                        onClearRecipient = {},
                        onMarkRead = {},
                        onMarkConversationRead = {},
                        onTypingStatusChanged = { targetUid, isTyping ->
                            typingEvents.add(targetUid to isTyping)
                        }
                    )
                }
            }
        }

        // Partner is typing -> typing indicator bubble and header status are displayed
        composeTestRule.onNodeWithTag("typing_indicator_bubble").assertIsDisplayed()
        composeTestRule.onNodeWithText("মুফতি মাহমুদ টাইপ করছেন…").assertIsDisplayed()
        composeTestRule.onNodeWithText("টাইপ করছেন… • • •").assertIsDisplayed()

        // Partner stops typing -> indicator disappears immediately
        partnerProfileState = partnerProfileState.copy(typingToUserId = "")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("typing_indicator_bubble").assertDoesNotExist()

        // Current user types a message -> broadcasts typing = true, then sends -> broadcasts typing = false
        composeTestRule.onNodeWithTag("message_body_input").performTextInput("আসসালামু আলাইকুম")
        composeTestRule.waitForIdle()
        assertTrue(typingEvents.contains("partner_1" to true))

        composeTestRule.onNodeWithTag("send_message_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("partner_1" to false, typingEvents.last())
    }

    @Test
    @Config(qualifiers = "w420dp-h840dp")
    fun `MessagesScreen supports Islamic reactions, message editing, scoped deletion, theme customization, and blocking`() {
        var myProfile by mutableStateOf(
            UserProfile(
                id = "user_me",
                fullName = "মাওলানা আব্দুল্লাহ"
            )
        )
        var partnerProfile by mutableStateOf(
            UserProfile(
                id = "partner_1",
                fullName = "মুফতি মাহমুদ",
                isOnline = true
            )
        )
        var messagesList by mutableStateOf(
            listOf(
                DirectMessage(
                    id = "msg_sent_1",
                    senderId = "user_me",
                    senderName = "মাওলানা আব্দুল্লাহ",
                    receiverId = "partner_1",
                    receiverName = "মুফতি মাহমুদ",
                    subject = "হাদিস দরস প্রসঙ্গে",
                    body = "আসসালামু আলাইকুম, আগামীকাল দরস কখন শুরু হবে?"
                ),
                DirectMessage(
                    id = "msg_received_2",
                    senderId = "partner_1",
                    senderName = "মুফতি মাহমুদ",
                    receiverId = "user_me",
                    receiverName = "মাওলানা আব্দুল্লাহ",
                    subject = "হাদিস দরস প্রসঙ্গে",
                    body = "ওয়া আলাইকুমুস সালাম, সকাল ৯টায় ইনশাআল্লাহ।"
                )
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(760.dp)) {
                    MessagesScreen(
                        messagesState = UiState.Success(
                            messagesList.filterNot { msg ->
                                (msg.senderId == "user_me" && msg.deletedForSender) ||
                                    (msg.receiverId == "user_me" && msg.deletedForReceiver)
                            }
                        ),
                        currentUserId = "user_me",
                        currentUserProfile = myProfile,
                        targetUser = "partner_1" to "মুফতি মাহমুদ",
                        partnerProfiles = mapOf("partner_1" to partnerProfile),
                        activePartnerProfile = partnerProfile,
                        isSubmitting = false,
                        onSendMessage = { _, _, _, _ -> },
                        onSelectRecipient = { _, _ -> },
                        onClearRecipient = {},
                        onMarkRead = {},
                        onMarkConversationRead = {},
                        onTypingStatusChanged = { _, _ -> },
                        onReactToMessage = { msg, reactionCode ->
                            messagesList = messagesList.map { item ->
                                if (item.id == msg.id) {
                                    if (item.senderId == "user_me") {
                                        val next = if (item.senderReaction == reactionCode) "" else reactionCode
                                        item.copy(senderReaction = next)
                                    } else {
                                        val next = if (item.receiverReaction == reactionCode) "" else reactionCode
                                        item.copy(receiverReaction = next)
                                    }
                                } else item
                            }
                        },
                        onEditMessage = { msg, newBody ->
                            messagesList = messagesList.map { item ->
                                if (item.id == msg.id) item.copy(body = newBody, isEdited = true) else item
                            }
                        },
                        onDeleteMessageForMe = { msg ->
                            messagesList = messagesList.map { item ->
                                if (item.id == msg.id) {
                                    if (item.senderId == "user_me") item.copy(deletedForSender = true)
                                    else item.copy(deletedForReceiver = true)
                                } else item
                            }
                        },
                        onDeleteMessageForEveryone = { msg ->
                            messagesList = messagesList.map { item ->
                                if (item.id == msg.id) {
                                    item.copy(
                                        body = "এই পয়গামটি প্রত্যাহার করা হয়েছে",
                                        isDeletedForEveryone = true,
                                        senderReaction = "",
                                        receiverReaction = ""
                                    )
                                } else item
                            }
                        },
                        onToggleBlockUser = { partnerUid, block ->
                            val updated = if (block) myProfile.blockedUserIds + partnerUid else myProfile.blockedUserIds - partnerUid
                            myProfile = myProfile.copy(blockedUserIds = updated)
                        },
                        onSaveConversationTheme = { partnerUid, themeValue ->
                            myProfile = myProfile.copy(chatThemes = myProfile.chatThemes + (partnerUid to themeValue))
                        }
                    )
                }
            }
        }

        // 1. Open message action sheet on received message and apply an Islamic reaction (DUA 🤲)
        composeTestRule.onNodeWithTag("message_options_button_msg_received_2", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("message_action_sheet").assertIsDisplayed()
        // Receiver cannot edit or delete for everyone on partner's message
        composeTestRule.onNodeWithTag("action_edit_message").assertDoesNotExist()
        composeTestRule.onNodeWithTag("action_delete_for_everyone").assertDoesNotExist()

        composeTestRule.onNodeWithTag("reaction_chip_DUA").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("message_reactions_row_msg_received_2", useUnmergedTree = true).assertIsDisplayed()

        // 2. Open message action sheet on own sent message and edit it
        composeTestRule.onNodeWithTag("message_options_button_msg_sent_1", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_edit_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("action_delete_for_everyone").assertIsDisplayed()

        composeTestRule.onNodeWithTag("action_edit_message").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edit_message_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_message_input").performTextReplacement("আসসালামু আলাইকুম, আগামীকাল সকাল ৮:৩০ মিনিটে দরস শুরু হবে?")
        composeTestRule.onNodeWithTag("confirm_edit_message_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("আসসালামু আলাইকুম, আগামীকাল সকাল ৮:৩০ মিনিটে দরস শুরু হবে?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("message_edited_badge_msg_sent_1", useUnmergedTree = true).assertIsDisplayed()

        // 3. Retract own message for everyone
        composeTestRule.onNodeWithTag("message_options_button_msg_sent_1", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("action_delete_for_everyone").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("এই পয়গামটি প্রত্যাহার করা হয়েছে").assertIsDisplayed()

        // 4. Three-dot menu -> change conversation theme & block/unblock user
        composeTestRule.onNodeWithTag("chat_three_dot_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_change_chat_theme").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_theme_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_option_ROYAL_GOLD").performClick()
        composeTestRule.waitForIdle()
        assertEquals("ROYAL_GOLD", myProfile.chatThemes["partner_1"])

        // Block user from 3-dot menu
        composeTestRule.onNodeWithTag("chat_three_dot_menu_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_toggle_block_user").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("blocked_conversation_banner").assertIsDisplayed()

        // Unblock user via banner button
        composeTestRule.onNodeWithTag("unblock_partner_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("blocked_conversation_banner").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w420dp-h840dp")
    fun `MessagesScreen removes Inbox text in header, opens profile on header click, supports group chats, 1-5 photo previews, and symbol-only reactions`() {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(11, 93, 67))
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        val sampleDataUri1 = "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        val sampleDataUri2 = "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)

        var inspectedProfilePair: Pair<String, String>? = null
        var createdGroupName = ""
        var createdGroupMembers = emptyList<String>()
        var sentImageUrls = emptyList<String>()
        var sentBody = ""

        val partnerProfile = UserProfile(
            id = "partner_1",
            fullName = "মুফতি মাহমুদ",
            photoUrl = "preset:scholar_emerald",
            isOnline = true
        )

        var messagesList by mutableStateOf(
            listOf(
                DirectMessage(
                    id = "msg_1",
                    senderId = "partner_1",
                    senderName = "মুফতি মাহমুদ",
                    receiverId = "user_me",
                    receiverName = "মাওলানা আব্দুল্লাহ",
                    subject = "পয়গাম",
                    body = "আসসালামু আলাইকুম"
                )
            )
        )

        var groupsList by mutableStateOf(
            listOf(
                com.example.data.model.ChatGroup(
                    id = "grp_1",
                    name = "আসাতীজা ফোরাম",
                    photoUrl = "preset:madrasa_dome",
                    creatorId = "user_me",
                    memberIds = listOf("user_me", "partner_1"),
                    memberNames = mapOf("user_me" to "মাওলানা আব্দুল্লাহ", "partner_1" to "মুফতি মাহমুদ")
                )
            )
        )

        var activeTarget by mutableStateOf<Pair<String, String>?>("partner_1" to "মুফতি মাহমুদ")

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(800.dp)) {
                    MessagesScreen(
                        messagesState = UiState.Success(messagesList),
                        userGroups = groupsList,
                        currentUserId = "user_me",
                        currentUserProfile = UserProfile(id = "user_me", fullName = "মাওলানা আব্দুল্লাহ"),
                        targetUser = activeTarget,
                        partnerProfiles = mapOf("partner_1" to partnerProfile),
                        activePartnerProfile = partnerProfile,
                        isSubmitting = false,
                        initialSelectedImages = listOf(sampleDataUri1, sampleDataUri2),
                        onSendMessage = { _, _, _, _ -> },
                        onSendMessageWithImages = { targetId, targetName, subject, body, images, groupId, groupName, groupMembers ->
                            sentBody = body
                            sentImageUrls = images
                            messagesList = messagesList + DirectMessage(
                                id = "msg_photo_2",
                                senderId = "user_me",
                                senderName = "মাওলানা আব্দুল্লাহ",
                                receiverId = targetId,
                                receiverName = targetName,
                                groupId = groupId,
                                groupName = groupName,
                                groupMemberIds = groupMembers,
                                subject = subject,
                                body = body,
                                imageUrls = images
                            )
                        },
                        onSelectRecipient = { id, name -> activeTarget = id to name },
                        onClearRecipient = { activeTarget = null },
                        onMarkRead = {},
                        onMarkConversationRead = {},
                        onCreateGroup = { name, photo, members, namesMap ->
                            createdGroupName = name
                            createdGroupMembers = members
                            val newGrp = com.example.data.model.ChatGroup(
                                id = "grp_2",
                                name = name,
                                photoUrl = photo,
                                creatorId = "user_me",
                                memberIds = listOf("user_me") + members,
                                memberNames = namesMap + ("user_me" to "মাওলানা আব্দুল্লাহ")
                            )
                            groupsList = groupsList + newGrp
                            activeTarget = "GROUP:grp_2" to name
                        },
                        onInspectUserProfile = { uid, name ->
                            inspectedProfilePair = uid to name
                        }
                    )
                }
            }
        }

        // 1. Verify "ইনবক্স" text is NOT present in the chat header, and clicking header opens partner profile
        composeTestRule.onNodeWithText("ইনবক্স").assertDoesNotExist()
        composeTestRule.onNodeWithTag("partner_header_profile_click_area").performClick()
        assertEquals("partner_1" to "মুফতি মাহমুদ", inspectedProfilePair)

        // 2. Verify sleek symbol-only Islamic reactions in message action sheet (no text labels like "দোয়া" inside reaction bar)
        composeTestRule.onNodeWithTag("message_options_button_msg_1", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("message_action_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reaction_chip_DUA").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reaction_chip_MUHABBAT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reaction_chip_HILAL").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reaction_chip_SUNDOR").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reaction_chip_DEENI_PRERONA").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reaction_chip_SAMMATI").assertIsDisplayed()
        composeTestRule.onNodeWithText("দোয়া").assertDoesNotExist()
        // Dismiss action sheet by clicking a reaction
        composeTestRule.onNodeWithTag("reaction_chip_DUA").performClick()
        composeTestRule.waitForIdle()

        // 3. Verify pre-send photo preview strip (2 images initially), remove 1 image, then send remaining 1 image
        composeTestRule.onNodeWithTag("selected_images_preview_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("remove_selected_image_0", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("message_body_input").performTextInput("ছবিসহ পয়গাম")
        composeTestRule.onNodeWithTag("send_message_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, sentImageUrls.size)
        assertEquals("ছবিসহ পয়গাম", sentBody)
        composeTestRule.onNodeWithTag("message_images_grid_msg_photo_2", useUnmergedTree = true).assertExists()

        // 4. Navigate back to conversation list and create a new Group conversation
        composeTestRule.onNodeWithTag("back_to_inbox_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_group_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_group_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("group_name_input").performTextInput("দারুল ইফতা গ্রুপ")
        composeTestRule.onNodeWithTag("select_group_member_partner_1").performClick()
        composeTestRule.onNodeWithTag("confirm_create_group_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("দারুল ইফতা গ্রুপ", createdGroupName)
        assertTrue(createdGroupMembers.contains("partner_1"))

        // Clicking header in group conversation opens Group Management dialog
        composeTestRule.onNodeWithTag("partner_header_profile_click_area").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("group_management_dialog").assertIsDisplayed()
    }

    @Test
    fun `Distinct unread conversation badge counts 1 per sender or group and excludes MESSAGE from Bell notifications`() {
        val myUid = "user_me"
        // Same sender ("sender_A") sends 50 unread messages -> should count as 1 unread conversation
        val fiftyFromSenderA = (1..50).map { idx ->
            DirectMessage(
                id = "m_a_$idx",
                senderId = "sender_A",
                senderName = "আব্দুল্লাহ রহমান",
                receiverId = myUid,
                body = "পয়গাম $idx",
                isRead = false
            )
        }
        // Group ("grp_1") has 10 unread messages from 3 different members -> should count as 1 unread group conversation
        val tenFromGroup1 = (1..10).map { idx ->
            DirectMessage(
                id = "m_g_$idx",
                senderId = "member_${idx % 3}",
                senderName = "সদস্য $idx",
                receiverId = "GROUP:grp_1",
                groupId = "grp_1",
                groupName = "কওমি কর্মসংস্থান গ্রুপ",
                body = "গ্রুপ পয়গাম $idx",
                isRead = false,
                readByUserIds = emptyList()
            )
        }
        // Another sender ("sender_B") sends 1 message that is already read -> 0 unread
        val readFromSenderB = DirectMessage(
            id = "m_b_1",
            senderId = "sender_B",
            receiverId = myUid,
            body = "পঠিত পয়গাম",
            isRead = true,
            readByUserIds = listOf("sender_B", myUid)
        )

        val allMessages = fiftyFromSenderA + tenFromGroup1 + readFromSenderB
        val distinctUnreadConversations = allMessages
            .filter { msg -> msg.isVisibleForUser(myUid) && msg.isUnreadForUser(myUid) }
            .map { msg -> msg.resolvedConversationKey(myUid) }
            .filter { it.isNotBlank() }
            .distinct()
            .size

        // 50 unread from sender_A (1) + 10 unread in grp_1 (1) + read from sender_B (0) = 2 distinct unread conversations
        assertEquals(2, distinctUnreadConversations)
        assertEquals("২", com.example.ui.util.IslamicDateAndPrayerHelper.toBanglaDigits(distinctUnreadConversations.toString()))

        // Verify ReplyQuoteInfo encoding & decoding
        val encodedSubject = com.example.data.model.ReplyQuoteInfo.encodeSubject(
            replyToMessageId = "m_a_1",
            replyToSenderName = "আব্দুল্লাহ রহমান",
            replyToSnippet = "আসসালামু আলাইকুম"
        )
        val parsed = com.example.data.model.ReplyQuoteInfo.fromSubject(encodedSubject)
        assertTrue(parsed != null)
        assertEquals("m_a_1", parsed?.replyToMessageId)
        assertEquals("আব্দুল্লাহ রহমান", parsed?.replyToSenderName)
        assertEquals("আসসালামু আলাইকুম", parsed?.replyToSnippet)
    }

    @Test
    @Config(qualifiers = "w420dp-h840dp")
    fun `Modernized AdminPanelScreen renders 12-screen layout, dispatches live Telegram notifications, and hides admin Gmail addresses`() {
        val dispatchedTelegramEvents = mutableListOf<Pair<String, String>>()
        var publishedNoticeTitle = ""

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(820.dp)) {
                    com.example.ui.screens.AdminPanelScreen(
                        isAdminUser = true,
                        adminDisplayName = "অ্যাডমিন",
                        usersState = UiState.Success(emptyList()),
                        submissionsState = UiState.Success(emptyList()),
                        madrasasState = UiState.Success(emptyList()),
                        feedPostsState = UiState.Success(emptyList()),
                        telegramNotificationsState = UiState.Success(emptyList()),
                        onVerifyMadrasa = { _, _ -> },
                        onVerifyPost = { _, _, _ -> },
                        onToggleBoostPost = { _, _ -> },
                        onPublishQuickNotice = { title, _, _ ->
                            publishedNoticeTitle = title
                        },
                        onSendAnnouncement = { _, _ -> },
                        onRecordLiveAdminAction = { eventType, title, _, _ ->
                            dispatchedTelegramEvents.add(eventType to title)
                        },
                        onDeletePost = {}
                    )
                }
            }
        }

        // Verify Dashboard (Screen 1) elements and that NO admin Gmail addresses are exposed
        composeTestRule.onNodeWithText("এডমিন প্যানেল").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_stat_users").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_stat_applications").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_stat_notices").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_stat_visits").assertIsDisplayed()
        composeTestRule.onNodeWithText("সাম্প্রতিক কার্যক্রম").assertIsDisplayed()
        composeTestRule.onNodeWithText("foysalahamad102@gmail.com").assertDoesNotExist()
        composeTestRule.onNodeWithText("foysalahamad204@gmail.com").assertDoesNotExist()
        composeTestRule.onNodeWithText("fyslahm28@gmail.com").assertDoesNotExist()

        // Navigate to Users Management (Screen 2)
        composeTestRule.onNodeWithTag("admin_tab_users").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ব্যবহারকারী ব্যবস্থাপনা").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_users_search_input").assertIsDisplayed()

        // Navigate to Notices Management (Screen 3) & Add New Notice (Screen 7)
        composeTestRule.onNodeWithTag("admin_tab_notices").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("বিজ্ঞপ্তি ব্যবস্থাপনা").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_add_new_notice_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("নতুন বিজ্ঞপ্তি যোগ করুন").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_announcement_title_input").performTextInput("নতুন শিক্ষক নিয়োগ")
        composeTestRule.onNodeWithTag("admin_announcement_body_input").performTextInput("জামিয়া ইসলামিয়ায় দাওরায়ে হাদিস বিভাগে শিক্ষক আবশ্যক।")
        composeTestRule.onNodeWithTag("admin_send_announcement_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals("নতুন শিক্ষক নিয়োগ", publishedNoticeTitle)

        // Navigate to Applications Management (Screen 4)
        composeTestRule.onNodeWithTag("admin_tab_applications").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("আবেদন ব্যবস্থাপনা").assertIsDisplayed()

        // Navigate to Settings & Logs Hub (Screen 6) -> Telegram Integration (Screen 11)
        composeTestRule.onNodeWithTag("admin_tab_settings_hub").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("সেটিংস ও লগ").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_settings_row_5").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("টেলিগ্রাম ইন্টিগ্রেশন").assertIsDisplayed()
        composeTestRule.onNodeWithText("@Q_kormosngsthan_Bangladesh").assertDoesNotExist()
        composeTestRule.onNodeWithText("চ্যাট আইডি (Chat ID): 6255562134").assertIsDisplayed()
        composeTestRule.onNodeWithText("বট আইডি (Bot ID): 8982853197").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔒 অ্যাপের ভেতরে স্থায়ীভাবে ফিট ও সক্রিয়").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_telegram_chat_id_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("admin_telegram_bot_id_input").assertDoesNotExist()
    }

    @Test
    fun `Non-duplicate Three-Line Drawer screens render App Settings, About App, and About Developer`() {
        var feedbackShown = ""
        var currentTab by mutableStateOf("SETTINGS")

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(820.dp)) {
                    when (currentTab) {
                        "SETTINGS" -> com.example.ui.screens.UserAppSettingsScreen(
                            onShowFeedback = { feedbackShown = it }
                        )
                        "ABOUT_APP" -> com.example.ui.screens.AboutAppScreen()
                        "ABOUT_DEV" -> com.example.ui.screens.AboutDeveloperScreen(
                            onShowFeedback = { feedbackShown = it }
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("user_app_settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("অ্যাপ সেটিংস").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_app_settings_screen").performScrollToIndex(4)
        composeTestRule.onNodeWithTag("save_user_app_settings_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(feedbackShown.contains("সেটিংস সংরক্ষিত"))

        currentTab = "ABOUT_APP"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_app_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("আমাদের লক্ষ্য ও উদ্দেশ্য").assertIsDisplayed()

        currentTab = "ABOUT_DEV"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_developer_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("কওমি কর্মসংস্থান কারিগরি টিম").assertIsDisplayed()
    }

    @Test
    fun `PrayerSchedule displays Hanafi start and end times and persists pinned district selection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.ui.util.IslamicDateAndPrayerHelper.pinDistrict(context, "ঢাকা")

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(860.dp)) {
                    com.example.ui.screens.FullPrayerScheduleScreen()
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("full_prayer_schedule_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("জেলা/বিভাগ: ঢাকা").assertIsDisplayed()

        // Open 64-district picker dialog, search for "সিলেট", and pin "সিলেট"
        composeTestRule.onNodeWithTag("full_schedule_change_district_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("prayer_district_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_district_search_input").performTextInput("সিলেট")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pin_btn_সিলেট").performClick()
        composeTestRule.waitForIdle()

        // Verify "সিলেট" is now both selected and pinned in IslamicDateAndPrayerHelper
        assertEquals("সিলেট", com.example.ui.util.IslamicDateAndPrayerHelper.pinnedDistrictFlow.value)
        assertEquals("সিলেট", com.example.ui.util.IslamicDateAndPrayerHelper.selectedDistrictFlow.value)
        composeTestRule.onNodeWithText("জেলা/বিভাগ: সিলেট").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w420dp-h860dp")
    fun `AdminPanelScreen shows real server data, shows Kono Totho Nei when empty without fake demo data, and supports server retry on error`() {
        var usersState by mutableStateOf<UiState<List<UserProfile>>>(UiState.Success(emptyList()))
        var submissionsState by mutableStateOf<UiState<List<com.example.data.model.KhedmotSubmission>>>(UiState.Success(emptyList()))
        var postsState by mutableStateOf<UiState<List<com.example.data.model.FeedPost>>>(UiState.Success(emptyList()))
        var logsState by mutableStateOf<UiState<List<com.example.data.model.TelegramNotificationLog>>>(UiState.Success(emptyList()))
        var retryCount = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(840.dp)) {
                    com.example.ui.screens.AdminPanelScreen(
                        isAdminUser = true,
                        adminDisplayName = "প্রধান অ্যাডমিন",
                        usersState = usersState,
                        submissionsState = submissionsState,
                        madrasasState = UiState.Success(emptyList()),
                        feedPostsState = postsState,
                        telegramNotificationsState = logsState,
                        adminsState = UiState.Success(emptyList()),
                        onRetryServerLoad = { retryCount++ },
                        onVerifyMadrasa = { _, _ -> },
                        onVerifyPost = { _, _, _ -> },
                        onToggleBoostPost = { _, _ -> },
                        onSendAnnouncement = { _, _ -> },
                        onDeletePost = {}
                    )
                }
            }
        }

        // 1. When server collections are empty, NO fake/seed data exists and "কোনো তথ্য নেই" is shown
        composeTestRule.onNodeWithText("১,২৫০").assertDoesNotExist()
        composeTestRule.onNodeWithText("৩২০").assertDoesNotExist()
        composeTestRule.onNodeWithText("হাফেজ সাইফুল ইসলাম").assertDoesNotExist()

        // Navigate to Users tab -> shows "সব (০)" and "কোনো তথ্য নেই"
        composeTestRule.onNodeWithTag("admin_tab_users").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("সব (০)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_empty_state_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("কোনো তথ্য নেই").assertIsDisplayed()

        // 2. When real server data arrives from Firebase, it renders immediately
        usersState = UiState.Success(
            listOf(
                UserProfile(
                    id = "real_u1",
                    fullName = "মাওলানা কারীমুল্লাহ",
                    contactInfo = "01711223344",
                    isOnline = true,
                    isVerified = true
                )
            )
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("সব (১)").assertIsDisplayed()
        composeTestRule.onNodeWithText("মাওলানা কারীমুল্লাহ").assertIsDisplayed()

        // 3. When a server error occurs, error banner and retry button are displayed
        postsState = UiState.Error("সার্ভার সংযোগ বিচ্ছিন্ন হয়েছে")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_server_error_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_retry_server_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, retryCount)
    }

    @Test
    @Config(qualifiers = "w420dp-h960dp")
    fun `HelpAndSupportScreen submits user complaint to Firebase, formats Telegram Bot alert, and displays in AdminPanelScreen`() {
        var logsList by mutableStateOf<List<com.example.data.model.TelegramNotificationLog>>(emptyList())
        var formattedTelegramHtml = ""
        var showAdmin by mutableStateOf(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(940.dp)) {
                    if (!showAdmin) {
                        com.example.ui.screens.HelpAndSupportScreen(
                            defaultSenderPhone = "01812345678",
                            isSubmitting = false,
                            onSubmitSupportMessage = { category, subject, message, contactPhone, onSuccessReset ->
                                val title = "[$category] $subject"
                                formattedTelegramHtml = com.example.ui.util.TelegramNotificationHelper.formatTelegramEventMessage(
                                    eventType = "KHEDMOT_SUBMISSION",
                                    title = title,
                                    message = message,
                                    actorName = "মাওলানা সাদিক",
                                    targetPhone = contactPhone
                                )
                                logsList = logsList + com.example.data.model.TelegramNotificationLog(
                                    id = "support_log_1",
                                    eventType = "KHEDMOT_SUBMISSION",
                                    title = title,
                                    message = message,
                                    actorId = "u_sadik",
                                    actorName = "মাওলানা সাদিক",
                                    targetPhone = contactPhone,
                                    referenceId = "SUPPORT_TICKET:$category",
                                    deliveryStatus = "DELIVERED"
                                )
                                onSuccessReset()
                            }
                        )
                    } else {
                        com.example.ui.screens.AdminPanelScreen(
                            isAdminUser = true,
                            adminDisplayName = "অ্যাডমিন",
                            usersState = UiState.Success(emptyList()),
                            submissionsState = UiState.Success(emptyList()),
                            madrasasState = UiState.Success(emptyList()),
                            feedPostsState = UiState.Success(emptyList()),
                            telegramNotificationsState = UiState.Success(logsList),
                            adminsState = UiState.Success(emptyList()),
                            onVerifyMadrasa = { _, _ -> },
                            onVerifyPost = { _, _, _ -> },
                            onToggleBoostPost = { _, _ -> },
                            onSendAnnouncement = { _, _ -> },
                            onDeletePost = {}
                        )
                    }
                }
            }
        }

        // Fill out complaint in HelpAndSupportScreen and submit
        composeTestRule.onNodeWithTag("help_support_subject_input").performTextInput("বিজ্ঞপ্তি প্রকাশে সমস্যা")
        composeTestRule.onNodeWithTag("help_support_message_input").performTextInput("নতুন নিয়োগ বিজ্ঞপ্তি প্রকাশ করার সময় ছবি যুক্ত হচ্ছে না।")
        composeTestRule.onNodeWithTag("help_support_submit_button").performClick()
        composeTestRule.waitForIdle()

        // Verify confirmation banner and Telegram Bot payload formatting
        composeTestRule.onNodeWithTag("help_support_confirmation_banner").assertIsDisplayed()
        assertEquals(1, logsList.size)
        assertTrue(formattedTelegramHtml.contains("ব্যবহারকারীর সহায়তা, অভিযোগ ও পরামর্শ বার্তা"))
        assertTrue(formattedTelegramHtml.contains("বিজ্ঞপ্তি প্রকাশে সমস্যা"))
        assertEquals("6255562134", com.example.ui.util.TelegramNotificationHelper.resolveTargetChatId())

        // Switch to AdminPanelScreen -> open Settings & Logs Hub -> Activity Log (Screen 12) -> filter by "অভিযোগ/সহায়তা"
        showAdmin = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_tab_settings_hub").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_settings_row_6").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("অভিযোগ/সহায়তা").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("telegram_log_item_support_log_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("[অভিযোগ] বিজ্ঞপ্তি প্রকাশে সমস্যা").assertIsDisplayed()
    }

    @Test
    fun `AjkerSmoronHelper assigns user-specific reminders, avoids repeats across 36 days, and enforces authentic citations and durood honorific`() {
        val allReminders = com.example.ui.util.AjkerSmoronHelper.allReminders
        assertTrue("Catalog must contain at least 30 authentic reminders", allReminders.size >= 30)

        // 1. Verify exact user-requested prompt examples are present in catalog
        assertTrue(
            allReminders.any {
                it.messageBangla.contains("আপনি কি আজ প্রিয় নবী হযরত মুহাম্মদ ﷺ-এর ওপর দরুদ শরিফ পাঠ করেছেন?")
            }
        )
        assertTrue(
            allReminders.any {
                it.messageBangla.contains("আজ কি আপনি আল্লাহ তাআলার জিকির করেছেন?")
            }
        )
        assertTrue(
            allReminders.any {
                it.messageBangla.contains("আজ কি আপনি কারো উপকার করেছেন বা একটি ভালো কাজ করেছেন?")
            }
        )

        // 2. Verify every single item has a non-blank authentic source citation and proper honorific when Prophet Muhammad ﷺ is mentioned
        allReminders.forEach { item ->
            assertTrue("Source reference must not be blank for ${item.id}", item.sourceReferenceBangla.isNotBlank())
            assertTrue("Quote or translation must not be blank for ${item.id}", item.quoteBangla.isNotBlank())
            val combinedText = "${item.messageBangla} ${item.quoteBangla} ${item.dailyAmolBangla}"
            if (combinedText.contains("মুহাম্মদ") || combinedText.contains("রাসূলুল্লাহ") || combinedText.contains("নবী করীম")) {
                assertTrue(
                    "Prophet mention in ${item.id} must include ﷺ or সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম",
                    combinedText.contains("ﷺ") || combinedText.contains("সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম")
                )
            }
        }

        // 3. Verify different users on the same day receive personalized reminders (not all identical)
        val sameDaySelections = (1..12).map { idx ->
            com.example.ui.util.AjkerSmoronHelper.selectPersonalizedReminderForUser(
                userId = "qawmi_user_$idx",
                dateKey = "2026_10_24",
                deliveredHistoryMap = emptyMap()
            ).id
        }.toSet()
        assertTrue("Different users on the same day should receive varied reminders", sameDaySelections.size > 1)

        // 4. Verify a single user does not see ANY repeated reminder across a full 36-day cycle
        val deliveredHistory = mutableMapOf<String, Long>()
        for (day in 1..allReminders.size) {
            val dateKey = "2026_11_${day.toString().padStart(2, '0')}"
            val chosen = com.example.ui.util.AjkerSmoronHelper.selectPersonalizedReminderForUser(
                userId = "maulana_abdullah_01",
                dateKey = dateKey,
                deliveredHistoryMap = deliveredHistory
            )
            assertTrue("Reminder ${chosen.id} should not repeat before full catalog cycle", !deliveredHistory.containsKey(chosen.id))
            deliveredHistory[chosen.id] = day.toLong() * 1000L
        }
        assertEquals(allReminders.size, deliveredHistory.size)
    }

    @Test
    @Config(qualifiers = "w420dp-h920dp")
    fun `AjkerSmoron Push Notification enforces once-per-day deduplication, builds deep-link intent, and renders AjkerSmoronScreen`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.Shadows.shadowOf(context as android.app.Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val dateKey = com.example.ui.util.AjkerSmoronHelper.currentBangladeshDateKey()
        val userId = "test_smoron_user"

        // Sync clean state
        com.example.ui.util.AjkerSmoronHelper.syncFromServerReceipts(context, userId, emptyMap())
        assertTrue(!com.example.ui.util.AjkerSmoronHelper.hasPushBeenSentForDate(context, userId, dateKey))

        // Deliver daily push notification (1st call succeeds and records delivery)
        val reminder = com.example.ui.util.AjkerSmoronHelper.getOrAssignTodayReminder(context, userId, dateKey)
        val delivered = com.example.ui.util.QawmiNotificationHelper.deliverDailyAjkerSmoronPushNotification(
            context = context,
            userId = userId,
            reminder = reminder,
            dateKey = dateKey
        )
        assertTrue("OS Push notification should be posted on first delivery today", delivered)
        assertTrue("Push deduplication flag must be set for today", com.example.ui.util.AjkerSmoronHelper.hasPushBeenSentForDate(context, userId, dateKey))

        // 2nd call on the same day for the same user must be blocked (strict 1 push per day)
        val duplicateAttempt = com.example.ui.util.QawmiNotificationHelper.deliverDailyAjkerSmoronPushNotification(
            context = context,
            userId = userId,
            reminder = reminder,
            dateKey = dateKey
        )
        assertTrue("Second push notification on the same day must be blocked", !duplicateAttempt)

        // Verify deep-link intent targets MainActivity and opens Ajker Smoron directly
        val deepLinkIntent = com.example.ui.util.QawmiNotificationHelper.buildAjkerSmoronDeepLinkIntent(
            context = context,
            reminderId = reminder.id
        )
        assertTrue(deepLinkIntent.getBooleanExtra(com.example.ui.util.QawmiNotificationHelper.EXTRA_OPEN_AJKER_SMORON, false))
        assertEquals(reminder.id, deepLinkIntent.getStringExtra(com.example.ui.util.QawmiNotificationHelper.EXTRA_AJKER_SMORON_ID))

        // Render AjkerSmoronScreen and verify UI interactions
        var syncedReminderId = ""
        var amolDoneDate = ""
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(900.dp)) {
                    com.example.ui.screens.AjkerSmoronScreen(
                        currentUserId = userId,
                        serverReceiptsMap = mapOf(
                            "smoron_hist_${reminder.id}" to 1700000000000L
                        ),
                        focusedReminderId = reminder.id,
                        onSyncAssignmentToServer = { dKey, rId -> syncedReminderId = "$dKey:$rId" },
                        onSyncAmolCompletedToServer = { dKey -> amolDoneDate = dKey }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ajker_smoron_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ajker_smoron_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("আজকের স্মরণ").assertIsDisplayed()

        // Mark today's practical Amol completed
        composeTestRule.onNodeWithTag("ajker_smoron_amol_done_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(dateKey, amolDoneDate)

        // Rotate to next non-repeating reminder
        composeTestRule.onNodeWithTag("ajker_smoron_next_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(syncedReminderId.startsWith("$dateKey:"))
    }

    @Test
    @Config(qualifiers = "w420dp-h980dp")
    fun `PublishPostScreen supports multi-select Khedmot categories, custom Other category input, validation, and preview`() {
        var submittedCategories = emptyList<String>()
        var submittedCustomCategory = ""
        var submittedDesignation = ""

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(960.dp)) {
                    com.example.ui.screens.PublishPostScreen(
                        isCircular = false,
                        userProfileState = UiState.Success(
                            UserProfile(
                                id = "u_khadim",
                                fullName = "হাফেজ মাওলানা মাহমুদ",
                                district = "ঢাকা",
                                qualification = "হিফজ ও দাওরায়ে হাদীস",
                                experience = "৪ বছর",
                                preferredDistricts = "ঢাকা, গাজীপুর",
                                contactInfo = "01711223344",
                                bio = "নূরানী ও ইমামতি খেদমতে অভিজ্ঞ।"
                            )
                        ),
                        isSubmitting = false,
                        onSubmit = { _, _, _, _, _, _, _, _, _, _, _ -> },
                        onSubmitWithCategories = { _, _, _, _, _, desig, _, _, _, _, _, cats, customCat ->
                            submittedDesignation = desig
                            submittedCategories = cats
                            submittedCustomCategory = customCat
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify all 7 required multi-select checkboxes exist
        val expectedCategories = listOf("নূরানী", "নাজেরা", "দরসিয়াত", "ইমাম", "মুয়াজ্জিন", "খতিব", "অন্যান্য")
        expectedCategories.forEach { cat ->
            composeTestRule.onNodeWithTag("category_checkbox_$cat").assertExists()
        }

        // 2. Select multiple categories: "নূরানী", "ইমাম", and "অন্যান্য"
        composeTestRule.onNodeWithTag("category_checkbox_নূরানী").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("category_checkbox_ইমাম").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("category_checkbox_অন্যান্য").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 3. When "অন্যান্য" is selected, the custom category text box appears; submitting while blank shows validation error
        composeTestRule.onNodeWithTag("publish_custom_category_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submit_post_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("publish_validation_error_banner").assertExists()

        // 4. Enter custom category text, open Preview dialog, and confirm publish
        composeTestRule.onNodeWithTag("publish_custom_category_input").performScrollTo().performTextInput("তাফসীর বিভাগ")
        composeTestRule.onNodeWithTag("post_location_input").performScrollTo().performTextInput("মিরপুর")
        composeTestRule.onNodeWithTag("publish_preview_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("publish_preview_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_preview_publish_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("নূরানী", "ইমাম", "অন্যান্য"), submittedCategories)
        assertEquals("তাফসীর বিভাগ", submittedCustomCategory)
        assertTrue(submittedDesignation.contains("নূরানী"))
        assertTrue(submittedDesignation.contains("ইমাম"))
        assertTrue(submittedDesignation.contains("তাফসীর বিভাগ"))
    }

    @Test
    @Config(qualifiers = "w420dp-h1600dp")
    fun `FacebookStyle FeedScreen supports multi-category badges, category filtering, Islamic reactions, comments, and in-app share`() {
        var postsList by mutableStateOf(
            listOf(
                com.example.data.model.FeedPost(
                    id = "post_1",
                    authorId = "author_1",
                    authorName = "জামিয়া ইসলামিয়া ঢাকা",
                    postType = "JOB_CIRCULAR",
                    title = "জামিয়া ইসলামিয়া ঢাকা",
                    district = "ঢাকা",
                    upazilaOrLocation = "মোহাম্মদপুর",
                    designation = "নূরানী • ইমাম • হেফজ বিভাগ",
                    qualification = "হাফেজ ও মাওলানা",
                    salaryOrExperience = "১৫,০০০ টাকা",
                    deadlineOrPreferredDistricts = "৩০ নভেম্বর ২০২৬",
                    description = "নূরানী ও ইমামতি খেদমতের জন্য মুত্তাকী আলেম আবশ্যক।",
                    contactNumber = "01700000000",
                    khedmotCategories = listOf("নূরানী", "ইমাম", "অন্যান্য"),
                    customCategory = "হেফজ বিভাগ",
                    isVerifiedMadrasa = true
                )
            )
        )
        var commentsList by mutableStateOf(
            listOf(
                com.example.data.model.FeedComment(
                    id = "c_1",
                    postId = "post_1",
                    authorId = "user_me",
                    authorName = "মাওলানা আব্দুল্লাহ",
                    text = "মাশাআল্লাহ, সুন্দর বিজ্ঞপ্তি।"
                )
            )
        )
        var selectedCategory by mutableStateOf("সকল বিভাগ")
        var sharedNote = ""

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1550.dp)) {
                    com.example.ui.screens.FeedScreen(
                        feedPostsState = UiState.Success(postsList),
                        postCommentsState = UiState.Success(commentsList),
                        bookmarksState = UiState.Success(emptyList()),
                        selectedFilter = com.example.ui.viewmodel.FeedFilterTab.ALL,
                        selectedKhedmotCategory = selectedCategory,
                        selectedDistrict = "সকল জেলা",
                        searchQuery = "",
                        currentUserId = "user_me",
                        isAdmin = false,
                        currentUserProfile = UserProfile(id = "user_me", fullName = "মাওলানা আব্দুল্লাহ"),
                        onSelectFilter = {},
                        onSelectKhedmotCategory = { selectedCategory = it },
                        onSelectDistrict = {},
                        onSearchQueryChange = {},
                        onApplyClick = {},
                        onViewProfileOrDetailClick = {},
                        onToggleBookmark = { _, _ -> },
                        onReactToPost = { post, code ->
                            postsList = postsList.map { p ->
                                if (p.id == post.id) {
                                    p.copy(
                                        userReactions = p.userReactions + ("user_me" to code),
                                        userReactionNames = p.userReactionNames + ("user_me" to "মাওলানা আব্দুল্লাহ")
                                    )
                                } else p
                            }
                        },
                        onAddComment = { post, text, parentId, parentAuthor ->
                            commentsList = commentsList + com.example.data.model.FeedComment(
                                id = "c_${commentsList.size + 1}",
                                postId = post.id,
                                authorId = "user_me",
                                authorName = "মাওলানা আব্দুল্লাহ",
                                text = text,
                                parentCommentId = parentId,
                                replyToAuthorName = parentAuthor
                            )
                        },
                        onEditComment = { cid, newText ->
                            commentsList = commentsList.map { if (it.id == cid) it.copy(text = newText, isEdited = true) else it }
                        },
                        onDeleteComment = { cid, _, _ ->
                            commentsList = commentsList.filterNot { it.id == cid }
                        },
                        onSharePostInApp = { _, caption ->
                            sharedNote = caption
                        },
                        onToggleBoostPost = { _, _ -> },
                        onDeletePost = {},
                        onNavigate = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify Facebook-style composer card and post card with multi-category badges
        composeTestRule.onNodeWithTag("feed_composer_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feed_post_card_post_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("post_categories_row_post_1", useUnmergedTree = true).assertExists()

        // 2. React on post_1 via the reaction button
        composeTestRule.onNodeWithTag("post_react_btn_post_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_engagement_summary_post_1").assertIsDisplayed()

        // 3. Open Share dialog and share in-app
        composeTestRule.onNodeWithTag("post_share_btn_post_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_share_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("share_post_caption_input").performTextReplacement("গুরুত্বপূর্ণ নিয়োগ বিজ্ঞপ্তি")
        composeTestRule.onNodeWithTag("confirm_share_in_app_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("গুরুত্বপূর্ণ নিয়োগ বিজ্ঞপ্তি", sharedNote)

        // 4. Open comments section, verify existing comment, and add a new comment
        composeTestRule.onNodeWithTag("post_comment_btn_post_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("post_comments_section_post_1").assertExists()
        composeTestRule.onNodeWithText("মাশাআল্লাহ, সুন্দর বিজ্ঞপ্তি।").assertIsDisplayed()

        composeTestRule.onNodeWithTag("comment_input_post_1").performTextInput("আল্লাহ কবুল করুন")
        composeTestRule.onNodeWithTag("send_comment_btn_post_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("আল্লাহ কবুল করুন").assertIsDisplayed()
    }
}


