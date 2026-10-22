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
}


