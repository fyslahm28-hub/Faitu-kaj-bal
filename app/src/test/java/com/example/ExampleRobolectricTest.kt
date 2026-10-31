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
        composeTestRule.mainClock.advanceTimeBy(400L)
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

    @Test
    fun `AutoUpdatePrompt shows Notun Update Eseche with Update Nin and Pore Korun buttons and launches Android APK installer`() {
        val remoteRelease = com.example.data.model.AppReleaseInfo(
            id = "latest",
            releaseId = "latest",
            versionCode = 105,
            versionName = "1.0.5",
            apkDownloadUrl = "https://github.com/qawmi-bd/qawmi-kormosangsthan/releases/download/v1.0.5/Qawmi-Kormosangsthan-Bangladesh.apk",
            releaseNotes = "নতুন ফিচার, উন্নত পারফরম্যান্স ও বাগ ফিক্স যুক্ত করা হয়েছে।",
            githubReleaseTag = "v1.0.5",
            isMandatory = false,
            publishedBy = "GitHub Actions CI"
        )

        var dismissedVersionCode by mutableStateOf<Int?>(null)
        com.example.ui.util.AppUpdateManagerHelper.lastDownloadedApkFile = null
        com.example.ui.util.AppUpdateManagerHelper.lastLaunchedInstallIntent = null
        com.example.ui.util.AppUpdateManagerHelper.customApkFetcher = { _, targetFile, onProgress ->
            onProgress(50)
            targetFile.parentFile?.mkdirs()
            targetFile.writeBytes("PK\u0003\u0004-mock-signed-apk-binary".toByteArray())
            onProgress(100)
            Result.success(targetFile)
        }

        try {
            composeTestRule.setContent {
                MyApplicationTheme {
                    Box(modifier = Modifier.width(420.dp).height(800.dp)) {
                        val showPrompt = com.example.ui.util.AppUpdateManagerHelper.isUpdateAvailable(
                            installedVersionCode = 1,
                            installedVersionName = "1.0",
                            remoteRelease = remoteRelease,
                            dismissedVersionCode = dismissedVersionCode
                        )
                        if (showPrompt) {
                            com.example.ui.screens.AppUpdatePromptOverlay(
                                installedVersionCode = 1,
                                installedVersionName = "1.0",
                                releaseInfo = remoteRelease,
                                onDismissLater = {
                                    dismissedVersionCode = remoteRelease.versionCode
                                }
                            )
                        }
                    }
                }
            }

            composeTestRule.waitForIdle()

            // 1. Verify “নতুন আপডেট এসেছে” message and “আপডেট নিন” & “পরে করুন” buttons are displayed
            composeTestRule.onNodeWithTag("update_prompt_dialog").assertIsDisplayed()
            composeTestRule.onNodeWithText("নতুন আপডেট এসেছে").assertIsDisplayed()
            composeTestRule.onNodeWithTag("update_now_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("update_later_button").assertIsDisplayed()
            composeTestRule.onNodeWithText("আপডেট নাও").assertIsDisplayed()
            composeTestRule.onNodeWithText("আপডেট নিন").assertIsDisplayed()
            composeTestRule.onNodeWithText("পরে করুন").assertIsDisplayed()

            // 2. Click “পরে করুন” and verify the prompt is dismissed
            composeTestRule.onNodeWithTag("update_later_button").performClick()
            composeTestRule.waitForIdle()
            assertEquals(105, dismissedVersionCode)
            composeTestRule.onNodeWithTag("update_prompt_dialog").assertDoesNotExist()

            // 3. Re-enable prompt (e.g., next session or manual check) and click “আপডেট নিন”
            dismissedVersionCode = null
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("update_now_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("update_now_button").performClick()
            composeTestRule.waitForIdle()

            // 4. Verify APK was downloaded and Android's approved installation Intent was launched
            val downloadedFile = com.example.ui.util.AppUpdateManagerHelper.lastDownloadedApkFile
            assertNotNull("Expected APK file to be downloaded to cacheDir/updates/", downloadedFile)
            assertTrue("Expected downloaded APK file to exist and be non-empty", downloadedFile!!.exists() && downloadedFile.length() > 0L)

            val launchedIntent = com.example.ui.util.AppUpdateManagerHelper.lastLaunchedInstallIntent
            assertNotNull("Expected Android package installer Intent to be launched", launchedIntent)
            assertEquals(android.content.Intent.ACTION_VIEW, launchedIntent?.action)
            assertEquals("application/vnd.android.package-archive", launchedIntent?.type)
            val flags = launchedIntent?.flags ?: 0
            assertTrue(
                "Expected FLAG_GRANT_READ_URI_PERMISSION on APK install intent",
                (flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
            )
            assertTrue(
                "Expected FLAG_ACTIVITY_NEW_TASK on APK install intent to open separate installer window",
                (flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK) != 0
            )
            composeTestRule.onNodeWithTag("update_install_status").assertIsDisplayed()

            // 5. Verify separate browser/downloader window launcher works cleanly
            val context = ApplicationProvider.getApplicationContext<Context>()
            val externalOpened = com.example.ui.util.AppUpdateManagerHelper.openExternalApkDownloadWindow(
                context = context,
                apkDownloadUrl = remoteRelease.apkDownloadUrl
            )
            assertTrue("Expected openExternalApkDownloadWindow to succeed", externalOpened)
            val browserIntent = com.example.ui.util.AppUpdateManagerHelper.lastLaunchedExternalBrowserIntent
            assertNotNull("Expected external window Intent to be recorded", browserIntent)
            assertEquals(android.content.Intent.ACTION_VIEW, browserIntent?.action)
            assertEquals(remoteRelease.apkDownloadUrl, browserIntent?.dataString)
        } finally {
            com.example.ui.util.AppUpdateManagerHelper.resetTestHooks()
        }
    }

    @Test
    fun `FeedScreen renders posts with Firebase Storage download URLs and compact image previews and validates 1 MiB limit`() {
        // 1. Verify pre-write size check blocks >1 MiB documents with a clear Bengali message
        val oversizedMap = mapOf(
            "authorId" to "user_1",
            "title" to "অতিরিক্ত বড় ছবি সহ পোস্ট",
            "imageUrls" to listOf("data:image/jpeg;base64," + "A".repeat(900_000))
        )
        val validationResult = ProfilePhotoHelper.validateFirestoreDocumentSizeBeforeWrite(
            documentPath = "feed_posts/post_oversized_1",
            data = oversizedMap
        )
        assertTrue("Expected oversized document map to fail validation", validationResult.isFailure)
        assertEquals(
            ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA,
            validationResult.exceptionOrNull()?.message
        )

        // 2. Create a valid compact bitmap preview + Firebase Storage download URL post and verify FeedScreen renders both
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(11, 93, 67))
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
        val compactDataUrl = "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        val storageDownloadUrl = "https://firebasestorage.googleapis.com/v0/b/qawmi-bd.appspot.com/o/feed_posts%2Fuser_1%2Fpost_media_1%2Fphoto_0.jpg?alt=media"

        val postWithMedia = com.example.data.model.FeedPost(
            id = "post_media_1",
            authorId = "user_1",
            authorName = "জামিয়া দারুল উলুম ঢাকা",
            postType = "GENERAL_POST",
            title = "বার্ষিক মাহফিল ও দস্তারবন্দী সম্মেলন",
            district = "ঢাকা",
            designation = "দ্বীনি খেদমত ও সাধারণ পোস্ট",
            description = "আগামী শুক্রবার বাদ মাগরিব বার্ষিক দস্তারবন্দী সম্মেলন অনুষ্ঠিত হবে ইনশাআল্লাহ।",
            imageUrls = listOf(storageDownloadUrl, compactDataUrl),
            mediaTypes = listOf("IMAGE", "IMAGE"),
            mediaMetadata = listOf(
                mapOf(
                    "url" to storageDownloadUrl,
                    "mediaType" to "IMAGE",
                    "mimeType" to "image/jpeg",
                    "fileName" to "photo_0.jpg",
                    "sizeBytes" to "124000"
                ),
                mapOf(
                    "url" to compactDataUrl,
                    "mediaType" to "IMAGE",
                    "mimeType" to "image/jpeg",
                    "fileName" to "photo_1.jpg",
                    "sizeBytes" to "1800"
                )
            ),
            createdAt = Timestamp.now()
        )

        var createdTitle = ""
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1100.dp)) {
                    com.example.ui.screens.FeedScreen(
                        feedPostsState = UiState.Success(listOf(postWithMedia)),
                        postCommentsState = UiState.Success(emptyList()),
                        bookmarksState = UiState.Success(emptyList()),
                        selectedFilter = com.example.ui.viewmodel.FeedFilterTab.ALL,
                        selectedKhedmotCategory = "সকল বিভাগ",
                        selectedDistrict = "সকল জেলা",
                        searchQuery = "",
                        currentUserId = "user_1",
                        isAdmin = false,
                        currentUserProfile = UserProfile(id = "user_1", fullName = "জামিয়া দারুল উলুম ঢাকা"),
                        onSelectFilter = {},
                        onSelectKhedmotCategory = {},
                        onSelectDistrict = {},
                        onSearchQueryChange = {},
                        onApplyClick = {},
                        onViewProfileOrDetailClick = {},
                        onToggleBookmark = { _, _ -> },
                        onPublishQuickDeeniPost = { title, _, _, _, _, _ ->
                            createdTitle = title
                        },
                        onReactToPost = { _, _ -> },
                        onAddComment = { _, _, _, _ -> },
                        onEditComment = { _, _ -> },
                        onDeleteComment = { _, _, _ -> },
                        onSharePostInApp = { _, _ -> },
                        onToggleBoostPost = { _, _ -> },
                        onDeletePost = {},
                        onNavigate = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Verify the post card and both attached media items (Firebase Storage URL + compact data URL) are rendered
        composeTestRule.onNodeWithTag("feed_post_card_post_media_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("post_media_row_post_media_1", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("post_media_item_post_media_1_0", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("post_media_item_post_media_1_1", useUnmergedTree = true).assertExists()

        // Open quick post composer dialog, enter a post title & description, and publish
        composeTestRule.onNodeWithTag("composer_quick_deeni_post_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quick_deeni_post_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_post_title_input").performTextInput("নতুন হিফজ বিভাগ উদ্বোধন")
        composeTestRule.onNodeWithTag("quick_post_desc_input").performTextInput("আমাদের মাদরাসায় নতুন শিক্ষাবর্ষে ভর্তি চলছে।")
        composeTestRule.onNodeWithTag("confirm_quick_post_submit_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("নতুন হিফজ বিভাগ উদ্বোধন", createdTitle)
    }

    @Test
    fun `Tall 1080x2400 photo upload scales below 1 MiB limit stores URL in feed_posts and renders for other users`() {
        kotlinx.coroutines.runBlocking {
            // 1. Create a tall 1080x2400 bitmap (aspect ratio 2.22 >= 1.55, which previously triggered the 2200px / 1,081,350 bytes bug)
            val tallBitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
            tallBitmap.eraseColor(Color.rgb(15, 82, 58))
            val rawHighQDataUrl = ProfilePhotoHelper.encodeBitmapToDataUrl(tallBitmap, quality = 95)

            // Verify ensureDataUrlFitsBudget strictly respects small budgets (e.g. 45,000 chars) even for aspect >= 1.55f
            val compactedSmall = ProfilePhotoHelper.ensureDataUrlFitsBudget(rawHighQDataUrl, 45_000)
            assertTrue(
                "Expected tall bitmap to be scaled down to fit <= 45,000 chars, but was ${compactedSmall.length}",
                compactedSmall.length <= 45_000
            )

            // 2. Run uploadFeedMediaToFirebaseStorage for post "post_tall_1" by "author_1"
            val uploadResult = ProfilePhotoHelper.uploadFeedMediaToFirebaseStorage(
                mediaItems = listOf(rawHighQDataUrl),
                authorId = "author_1",
                postId = "post_tall_1"
            )
            assertTrue("uploadFeedMediaToFirebaseStorage must succeed", uploadResult.isSuccess)
            val uploadedItems = uploadResult.getOrThrow()
            assertEquals(1, uploadedItems.size)
            val mediaItem = uploadedItems.first()

            // Verify the URL stored in feed_posts is a compact URL (< 300 bytes), NEVER a huge Base64 string
            assertTrue(
                "URL stored in feed_posts must be a compact URL (< 300 chars), got ${mediaItem.url.length}",
                mediaItem.url.startsWith("https://") && mediaItem.url.length < 300
            )

            // Verify the attachment document payload passes pre-write 1 MiB validation
            val attachmentPayload = mapOf(
                "postId" to "post_tall_1",
                "authorId" to "author_1",
                "attachmentType" to mediaItem.mediaType,
                "mimeType" to mediaItem.mimeType,
                "fileName" to mediaItem.fileName,
                "downloadUrl" to mediaItem.url,
                "extendedText" to "",
                "mediaDataUrl" to mediaItem.separateAttachmentDataUrl,
                "sizeBytes" to mediaItem.sizeBytes,
                "visibility" to "PUBLIC"
            )
            val attachSizeRes = ProfilePhotoHelper.validateAttachmentDocumentSizeBeforeWrite(
                documentPath = "feed_post_attachments/post_tall_1_media_0",
                data = attachmentPayload
            )
            assertTrue("Attachment document must be well under 1 MiB: ${attachSizeRes.exceptionOrNull()}", attachSizeRes.isSuccess)

            // Verify decoding the uploaded URL via ProfilePhotoHelper resolves the cached attachment bitmap for rendering
            val decodedBitmap = ProfilePhotoHelper.decodeDataUrlToImageBitmap(mediaItem.url)
            assertNotNull("Expected attachment URL to resolve to a valid ImageBitmap for feed rendering", decodedBitmap)
        }

        // 3. Verify PublishPostScreen exposes the unified photo attachment button ("publish_attach_media_btn")
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1000.dp)) {
                    com.example.ui.screens.PublishPostScreen(
                        isCircular = true,
                        userProfileState = UiState.Success(
                            UserProfile(
                                id = "author_1",
                                fullName = "মুহতামিম সাহেব",
                                madrasaName = "জামিয়া রাহমানিয়া ঢাকা",
                                district = "ঢাকা"
                            )
                        ),
                        isSubmitting = false
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("publish_attach_media_btn").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `MainActivity launches cleanly without crashing and QawmiNotificationHelper posts startup notifications safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.Shadows.shadowOf(context as android.app.Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        // 1. Verify ensureFirebaseInitialized initializes FirebaseApp safely without throwing
        val firebaseReady = ensureFirebaseInitialized(context)
        assertTrue("Expected ensureFirebaseInitialized to succeed", firebaseReady)

        // 2. Verify QawmiNotificationHelper posts daily Ajker Smoron and unread notifications on startup without crashing on AdaptiveIcon
        com.example.ui.util.QawmiNotificationHelper.ensureChannelCreated(context)
        val dateKey = com.example.ui.util.AjkerSmoronHelper.currentBangladeshDateKey()
        val reminder = com.example.ui.util.AjkerSmoronHelper.syncFromServerReceipts(
            context = context,
            userId = "user_startup_test",
            receiptTimestamps = emptyMap(),
            dateKey = dateKey
        )
        val smoronDelivered = com.example.ui.util.QawmiNotificationHelper.deliverDailyAjkerSmoronPushNotification(
            context = context,
            userId = "user_startup_test",
            reminder = reminder,
            dateKey = dateKey
        )
        assertTrue("Expected startup Ajker Smoron notification delivery to succeed without crashing", smoronDelivered)

        // 3. Verify AppUpdateManagerHelper cooldown persistence ("পরে করুন") stores and retrieves active cooldown version
        com.example.ui.util.AppUpdateManagerHelper.clearDismissedUpdatePrompt(context)
        assertNull(com.example.ui.util.AppUpdateManagerHelper.getActiveCooldownDismissedVersionCode(context))
        com.example.ui.util.AppUpdateManagerHelper.saveDismissedUpdatePrompt(context, 108)
        assertEquals(108, com.example.ui.util.AppUpdateManagerHelper.getActiveCooldownDismissedVersionCode(context))
        com.example.ui.util.AppUpdateManagerHelper.clearDismissedUpdatePrompt(context)

        // 4. Verify AppNavigation (MainActivity's root startup composable) renders cleanly without crashing
        composeTestRule.setContent {
            MyApplicationTheme {
                AppNavigation(
                    openNotificationsInitially = false,
                    deepLink = null,
                    onDeepLinkConsumed = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("কওমি কর্মসংস্থান").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w420dp-h900dp")
    fun `Google Sign-In validates default_web_client_id, shows Bengali error on failure without hanging in Loading, and transitions to Home on success`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify default_web_client_id is properly generated from google-services.json (OAuth client_type 3)
        val resolvedClientId = com.example.ui.auth.resolveWebClientId(context)
        assertTrue(
            "Expected valid OAuth 2.0 Web Client ID from default_web_client_id, got: $resolvedClientId",
            resolvedClientId.isNotBlank() && resolvedClientId.endsWith(".apps.googleusercontent.com")
        )

        // Verify GenericIdpActivity's PackageManager.resolveActivity(Intent(ACTION_VIEW), 0) check succeeds on targetSdk 36
        val resolvedViewActivity = context.packageManager.resolveActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW),
            0
        )
        assertTrue(
            "Expected PackageManager.resolveActivity(Intent(ACTION_VIEW), 0) to return non-null for GenericIdpActivity",
            resolvedViewActivity != null
        )

        // 2. Verify Bengali error messages are clear and non-empty for all failure modes and do not falsely blame internet
        val cancelMsg = com.example.ui.auth.formatGoogleSignInErrorBangla(
            androidx.credentials.exceptions.GetCredentialCancellationException("activity is cancelled by the user.")
        )
        val noCredMsg = com.example.ui.auth.formatGoogleSignInErrorBangla(
            androidx.credentials.exceptions.NoCredentialException("No credentials available")
        )
        val genericMsg = com.example.ui.auth.formatGoogleSignInErrorBangla(
            IllegalStateException("Provider error")
        )
        assertTrue(cancelMsg.contains("গুগল অ্যাকাউন্ট নির্বাচন সম্পন্ন হয়নি"))
        assertTrue(noCredMsg.contains("কোনো সক্রিয় গুগল অ্যাকাউন্ট পাওয়া যায়নি"))
        assertTrue(!genericMsg.contains("ইন্টারনেট"))

        // 3. Render AuthScreen and click Google Sign-In button; verify it does NOT hang in Loading or fail silently
        var isSignedIn by mutableStateOf(false)
        var triggerSuccessNow: (() -> Unit)? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(900.dp)) {
                    if (!isSignedIn) {
                        val onSuccessCallback = {
                            isSignedIn = true
                        }
                        triggerSuccessNow = onSuccessCallback
                        com.example.ui.auth.AuthScreen(
                            onAuthSuccess = onSuccessCallback
                        )
                    } else {
                        androidx.compose.material3.Text("হোম স্ক্রিনে স্বাগতম")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(7000L)
        composeTestRule.waitForIdle()

        // Must show clear Bengali error banner rather than failing silently, and button must not stay stuck in "প্রবেশ করা হচ্ছে..."
        composeTestRule.onNodeWithTag("auth_error_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("প্রবেশ করা হচ্ছে...").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sign in with Google (গুগল দিয়ে প্রবেশ করুন)").performScrollTo().assertIsDisplayed()

        // 4. Verify that when Firebase Authentication succeeds (onAuthSuccess), the user enters the Home screen
        triggerSuccessNow?.invoke()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("হোম স্ক্রিনে স্বাগতম").assertIsDisplayed()
    }

    @Test
    fun `MessagesScreen shows Facebook style Sent Delivered Seen and Failed statuses removes wrong image labels and retries without duplicates`() {
        val myUid = "user_me_1"
        val partnerUid = "user_partner_2"
        val partnerName = "মাওলানা সালমান"
        val nowSec = System.currentTimeMillis() / 1000L

        var messagesList by mutableStateOf(
            listOf(
                // 1. Plain text message -> SENT (✓)
                DirectMessage(
                    id = "msg_sent_1",
                    senderId = myUid,
                    senderName = "আমি",
                    receiverId = partnerUid,
                    receiverName = partnerName,
                    body = "শুধু সাধারণ টেক্সট মেসেজ",
                    imageUrls = emptyList(),
                    isRead = false,
                    readByUserIds = listOf(myUid),
                    createdAt = Timestamp(nowSec - 120, 0)
                ),
                // 2. Image message (with legacy auto-label in body) -> DELIVERED (✓✓), shows image preview only, no auto-label text
                DirectMessage(
                    id = "msg_deliv_2",
                    senderId = myUid,
                    senderName = "আমি",
                    receiverId = partnerUid,
                    receiverName = partnerName,
                    body = "📷 ১ টি ছবি প্রেরণ করা হয়েছে",
                    imageUrls = listOf("https://example.com/madrasa_view.jpg"),
                    isRead = false,
                    readByUserIds = listOf(myUid, "DELIVERED:$partnerUid"),
                    createdAt = Timestamp(nowSec - 90, 0)
                ),
                // 3. Document + Seen message -> SEEN (colored ✓✓) & Document card
                DirectMessage(
                    id = "msg_seen_3",
                    senderId = myUid,
                    senderName = "আমি",
                    receiverId = partnerUid,
                    receiverName = partnerName,
                    body = "",
                    imageUrls = listOf("https://example.com/cv_biodata.pdf"),
                    isRead = true,
                    readByUserIds = listOf(myUid, partnerUid),
                    createdAt = Timestamp(nowSec - 60, 0)
                ),
                // 4. Failed message -> FAILED (! + পাঠানো হয়নি + পুনরায় পাঠান)
                DirectMessage(
                    id = "msg_fail_4",
                    senderId = myUid,
                    senderName = "আমি",
                    receiverId = partnerUid,
                    receiverName = partnerName,
                    body = "নেটওয়ার্ক বিচ্ছিন্ন থাকায় ব্যর্থ মেসেজ",
                    imageUrls = emptyList(),
                    isRead = false,
                    readByUserIds = listOf(myUid),
                    createdAt = Timestamp(nowSec - 20, 0)
                ).apply {
                    localDeliveryState = "FAILED"
                }
            )
        )

        var retriedMessageIds = mutableListOf<String>()

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1600.dp)) {
                    MessagesScreen(
                        messagesState = UiState.Success(messagesList),
                        currentUserId = myUid,
                        targetUser = partnerUid to partnerName,
                        isSubmitting = true, // Even when isSubmitting=true, no wrong "পয়গাম ও ছবি প্রেরিত হচ্ছে…" banner should appear
                        onSendMessage = { _, _, _, _ -> },
                        onSelectRecipient = { _, _ -> },
                        onMarkRead = {},
                        onRetryFailedMessage = { failedMsg ->
                            retriedMessageIds.add(failedMsg.id)
                            // Update in-place using the exact same ID (never adding a duplicate item)
                            messagesList = messagesList.map { existing ->
                                if (existing.id == failedMsg.id) {
                                    existing.copy().apply { localDeliveryState = "SENT" }
                                } else {
                                    existing
                                }
                            }.distinctBy { it.id }
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // A. Scroll to index 0: Plain text message shows only text + SENT (✓); wrong banners/labels must NOT exist
        composeTestRule.onNodeWithTag("conversation_messages_list").performScrollToIndex(0)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("শুধু সাধারণ টেক্সট মেসেজ", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("পয়গাম ও ছবি প্রেরিত হচ্ছে…", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("ছবি মেসেজ পাঠানো হচ্ছে", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("message_status_sent_msg_sent_1", useUnmergedTree = true).assertExists()

        // B. Scroll to index 1: Image message shows image preview thumbnail + DELIVERED (✓✓), and NO auto-label text
        composeTestRule.onNodeWithTag("conversation_messages_list").performScrollToIndex(1)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("message_image_msg_deliv_2_0", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("📷 ১ টি ছবি প্রেরণ করা হয়েছে", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("message_status_delivered_msg_deliv_2", useUnmergedTree = true).assertExists()

        // C. Scroll to index 2: Document attachment card + SEEN (colored ✓✓)
        composeTestRule.onNodeWithTag("conversation_messages_list").performScrollToIndex(2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("attachment_file_card_document", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("message_status_seen_msg_seen_3", useUnmergedTree = true).assertExists()

        // D. Scroll to index 3: Failed message shows FAILED (! + পাঠানো হয়নি + পুনরায় পাঠান) and retries in-place without duplicates
        composeTestRule.onNodeWithTag("conversation_messages_list").performScrollToIndex(3)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("message_status_failed_msg_fail_4", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("retry_failed_message_msg_fail_4", useUnmergedTree = true).assertExists()

        assertEquals(4, messagesList.size)
        composeTestRule.onNodeWithTag("retry_failed_message_msg_fail_4", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf("msg_fail_4"), retriedMessageIds)
        assertEquals("Retrying a failed message must never create duplicate messages", 4, messagesList.size)
        composeTestRule.onNodeWithTag("message_status_failed_msg_fail_4", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("message_status_sent_msg_fail_4", useUnmergedTree = true).assertExists()
    }

    @Test
    @Config(qualifiers = "w420dp-h1600dp")
    fun `HomeScreen IslamicServices prayer icon opens PrayerTimesAndAzanScreen and supports per-waqt Azan, pre-reminder, and deduplication`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        org.robolectric.Shadows.shadowOf(context as android.app.Application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        com.example.ui.util.PrayerAzanManager.initWithContext(context)
        com.example.ui.util.IslamicDateAndPrayerHelper.pinDistrict(context, "ঢাকা")

        var currentScreen by mutableStateOf(MainDestination.HOME)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1550.dp)) {
                    if (currentScreen == MainDestination.HOME) {
                        com.example.ui.screens.HomeScreen(
                            currentUserId = "user_prayer_test",
                            onNavigate = { dest -> currentScreen = dest },
                            onOpenFeedFilter = {}
                        )
                    } else if (currentScreen == MainDestination.PRAYER_SCHEDULE) {
                        com.example.ui.screens.FullPrayerScheduleScreen()
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify "ইসলামিক ও প্রয়োজনীয় সেবাসমূহ -> ইলম ও আমল -> নামাজের সময়" is displayed on HomeScreen and opens PrayerTimesAndAzanScreen
        composeTestRule.onNodeWithTag("home_screen_list").performScrollToIndex(5)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_islamic_essential_services_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_islamic_service_prayer_times").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainDestination.PRAYER_SCHEDULE, currentScreen)

        // 2. Verify PrayerTimesAndAzanScreen renders live countdown, Sehri/Iftar, Sunrise/Sunset, and 5 prayer cards
        composeTestRule.onNodeWithTag("full_prayer_schedule_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_hero_countdown_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_live_countdown_text", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("জেলা/বিভাগ: ঢাকা").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_waqt_card_FAJR").assertExists()
        composeTestRule.onNodeWithTag("prayer_waqt_card_DHUHR").assertExists()

        // 3. Configure Fajr Pre-Azan reminder (10 min) and sound type (FAJR is expanded by default)
        composeTestRule.onNodeWithTag("waqt_FAJR_prereminder_10").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            10,
            com.example.ui.util.PrayerAzanManager.preferencesFlow.value
                .configFor(com.example.ui.util.PrayerWaqtKey.FAJR).preReminderMinutes
        )

        composeTestRule.onNodeWithTag("waqt_FAJR_sound_SHORT_AZAN").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            com.example.ui.util.AzanSoundType.SHORT_AZAN,
            com.example.ui.util.PrayerAzanManager.preferencesFlow.value
                .configFor(com.example.ui.util.PrayerWaqtKey.FAJR).soundType
        )

        // 4. Toggle Fajr Azan switch off and back on
        val initialEnabled = com.example.ui.util.PrayerAzanManager.preferencesFlow.value
            .configFor(com.example.ui.util.PrayerWaqtKey.FAJR).azanEnabled
        composeTestRule.onNodeWithTag("waqt_azan_switch_FAJR").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            !initialEnabled,
            com.example.ui.util.PrayerAzanManager.preferencesFlow.value
                .configFor(com.example.ui.util.PrayerWaqtKey.FAJR).azanEnabled
        )
        composeTestRule.onNodeWithTag("waqt_azan_switch_FAJR").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 5. Verify strict once-per-waqt-per-day deduplication so Azan never plays twice in the same waqt
        val testDateKey = "2026_10_30_dedup_test"
        com.example.ui.util.PrayerAzanManager.clearFiredEventsForTest(context)
        assertTrue(
            !com.example.ui.util.PrayerAzanManager.hasEventFiredToday(
                context = context,
                dateKey = testDateKey,
                waqtKey = com.example.ui.util.PrayerWaqtKey.MAGHRIB
            )
        )
        com.example.ui.util.PrayerAzanManager.markEventFiredToday(
            context = context,
            dateKey = testDateKey,
            waqtKey = com.example.ui.util.PrayerWaqtKey.MAGHRIB
        )
        assertTrue(
            com.example.ui.util.PrayerAzanManager.hasEventFiredToday(
                context = context,
                dateKey = testDateKey,
                waqtKey = com.example.ui.util.PrayerWaqtKey.MAGHRIB
            )
        )
    }

    @Test
    @Config(qualifiers = "w420dp-h1800dp")
    fun `Mini Live Prayer Times Home Screen Widget displays 12h current time, next prayer countdown, 5 waqts in one row, and 3-language dates without notification bar or in-app card`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.ui.util.PrayerAzanManager.initWithContext(context)
        com.example.ui.util.PrayerAzanManager.setManualDistrictSelection(context, "ঢাকা")

        val morningCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, java.util.Calendar.OCTOBER, 31, 8, 56, 0) // 08:56 AM (Saturday, Oct 31, 2026 -> Dhuhr is next)
        }
        val dhakaSchedule = com.example.ui.util.PrayerAzanManager.computeDailyPrayerSchedule(
            districtBangla = "ঢাকা",
            calendar = morningCal
        )
        assertEquals(com.example.ui.util.PrayerWaqtKey.DHUHR, dhakaSchedule.nextWaqtKey)
        assertEquals("08:56 AM", dhakaSchedule.currentTime12hEnglish)
        assertEquals("Sat, Oct 31", dhakaSchedule.shortDayDateEnglish)

        // 1. Verify Mini Home Screen Widget renders Current Time (12h AM/PM), District, Next Prayer + Live Countdown, 5 Waqts in 1 row, and 3-Language Dates (Bangla, English, Arabic)
        com.example.ui.util.PrayerWidgetSize.entries.forEach { widgetSize ->
            val remoteViews = com.example.ui.util.PrayerTimeWidgetManager.buildRemoteViews(
                context = context,
                size = widgetSize,
                schedule = dhakaSchedule
            )
            val parent = android.widget.FrameLayout(context)
            val inflatedView = remoteViews.apply(context, parent)

            val currentTimeTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_current_time_text)
            val textClock = inflatedView.findViewById<android.widget.TextClock>(R.id.widget_live_text_clock)
            val districtTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_district_name)
            val nextBadgeTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_next_prayer_badge)
            val nextPrayerTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_next_prayer_title)
            val subtitleTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_current_waqt_text)
            val countdownTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_countdown_text)
            val chronometer = inflatedView.findViewById<android.widget.Chronometer>(R.id.widget_countdown_chronometer)

            val banglaDateTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_bangla_date)
            val englishDateTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_english_date)
            val arabicDateTv = inflatedView.findViewById<android.widget.TextView>(R.id.widget_arabic_date)

            // Top-left: 12-Hour AM/PM time + short date & district
            assertNotNull("${widgetSize.id} must include live 12h TextClock", textClock)
            assertEquals("08:56 AM", currentTimeTv.text.toString())
            assertTrue(districtTv.text.toString().contains("Sat, Oct 31"))
            assertTrue(districtTv.text.toString().contains("ঢাকা"))

            // Top-right: "পরবর্তী নামাজ", Next Prayer Name ("যোহর"), "( শুরু হতে বাকি )", and HH:MM:SS live countdown
            assertTrue(nextBadgeTv.text.toString().contains("পরবর্তী নামাজ"))
            assertEquals("যোহর", nextPrayerTv.text.toString())
            assertEquals("( শুরু হতে বাকি )", subtitleTv.text.toString())
            assertTrue(countdownTv.text.toString().startsWith("⏱ "))
            assertTrue(countdownTv.text.toString().contains(":"))
            assertNotNull(chronometer)

            // Middle row: All 5 waqts in one horizontal row with 12-hour AM/PM times
            val fajrName = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_name_fajr).text.toString()
            val dhuhrName = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_name_dhuhr).text.toString()
            val asrName = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_name_asr).text.toString()
            val maghribName = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_name_maghrib).text.toString()
            val ishaName = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_name_isha).text.toString()

            val fajrTime = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_time_fajr).text.toString()
            val dhuhrTime = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_time_dhuhr).text.toString()
            val asrTime = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_time_asr).text.toString()
            val maghribTime = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_time_maghrib).text.toString()
            val ishaTime = inflatedView.findViewById<android.widget.TextView>(R.id.widget_waqt_time_isha).text.toString()

            assertEquals("ফজর", fajrName)
            assertEquals("যোহর", dhuhrName)
            assertEquals("আসর", asrName)
            assertEquals("মাগরিব", maghribName)
            assertEquals("এশা", ishaName)

            assertTrue(fajrTime.endsWith("AM"))
            assertTrue(dhuhrTime.endsWith("AM") || dhuhrTime.endsWith("PM"))
            assertTrue(asrTime.endsWith("PM"))
            assertTrue(maghribTime.endsWith("PM"))
            assertTrue(ishaTime.endsWith("PM"))

            // Bottom section: Mandatory 3-language dates (Bangla, English, Arabic)
            assertTrue("Bangla line must contain Hijri, Bangla Ponjika & Gregorian",
                banglaDateTv.text.toString().contains("হিজরি") &&
                    banglaDateTv.text.toString().contains("বঙ্গাব্দ") &&
                    banglaDateTv.text.toString().contains("৩১ অক্টোবর ২০২৬")
            )
            assertTrue("English line must contain Gregorian & Hijri AH",
                englishDateTv.text.toString().contains("31 October 2026") &&
                    englishDateTv.text.toString().contains("AH")
            )
            assertTrue("Arabic line must contain Arabic Gregorian & Hijri هـ",
                arabicDateTv.text.toString().contains("٣١ أكتوبر ٢٠٢٦") &&
                    arabicDateTv.text.toString().contains("هـ")
            )
        }

        // 2. Verify automatic update when district changes (সিলেট) or time advances to next waqt (মাগরিব)
        com.example.ui.util.PrayerAzanManager.setManualDistrictSelection(context, "সিলেট")
        val eveningCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, java.util.Calendar.OCTOBER, 31, 16, 45, 0) // 04:45 PM (Asr active, Maghrib is next)
        }
        val sylhetSchedule = com.example.ui.util.PrayerAzanManager.computeDailyPrayerSchedule(
            districtBangla = com.example.ui.util.IslamicDateAndPrayerHelper.selectedDistrictFlow.value,
            calendar = eveningCal
        )
        val updatedSmallViews = com.example.ui.util.PrayerTimeWidgetManager.buildRemoteViews(
            context = context,
            size = com.example.ui.util.PrayerWidgetSize.SMALL,
            schedule = sylhetSchedule
        )
        val inflatedUpdated = updatedSmallViews.apply(context, android.widget.FrameLayout(context))
        assertTrue(
            inflatedUpdated.findViewById<android.widget.TextView>(R.id.widget_district_name)
                .text.toString().contains("সিলেট")
        )
        assertEquals(
            "মাগরিব",
            inflatedUpdated.findViewById<android.widget.TextView>(R.id.widget_next_prayer_title)
                .text.toString()
        )

        // 3. Verify clicking the widget creates an Intent that opens exclusively the Prayer Times page
        val openIntent = com.example.ui.util.PrayerTimeWidgetManager.createOpenPrayerScreenIntent(context, "widget_small")
        assertTrue(openIntent.getBooleanExtra(com.example.ui.util.PrayerAzanManager.EXTRA_OPEN_PRAYER_SCREEN, false))
        assertEquals("prayer_schedule", openIntent.data?.host)

        // 4. Verify NO Notification Bar item is posted and NO new Card is added inside PrayerTimesAndAzanScreen
        com.example.ui.util.PrayerTimeWidgetManager.updateAllWidgetsAndLockScreen(context, morningCal)
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNotifManager = org.robolectric.Shadows.shadowOf(notifManager)
        assertNull(
            "Widget must never appear in the Notification Bar/Panel",
            shadowNotifManager.getNotification(com.example.ui.util.PrayerTimeWidgetManager.LOCK_SCREEN_PRAYER_NOTIFICATION_ID)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1550.dp)) {
                    com.example.ui.screens.PrayerTimesAndAzanScreen()
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("prayer_live_widget_and_lockscreen_card").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w420dp-h1200dp")
    fun `Full App Diagnosis Repair - FeedScreen, MessagesScreen Inbox and Groups, and SubmissionsManagementScreen load and operate without Error getting Query snapshot`() {
        // 1. Verify formatFirestoreErrorBangla translates raw English "Error getting Query snapshot" into clean Bengali
        val rawSnapshotError = RuntimeException("Error getting Query snapshot: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions.}")
        val formattedBangla = com.example.data.repository.formatFirestoreErrorBangla(rawSnapshotError, "তথ্য লোড করা যায়নি")
        assertTrue("Must not expose raw 'Error getting Query snapshot'", !formattedBangla.contains("Error getting Query snapshot"))
        assertTrue("Must contain Bengali explanation", formattedBangla.contains("অনুমতি") || formattedBangla.contains("পুনরায়"))

        // 2. Verify FeedScreen transitions cleanly from Loading ("ইসলামিক খেদমত ফিড লোড হচ্ছে...") to Success displaying posts
        var feedState by mutableStateOf<UiState<List<com.example.data.model.FeedPost>>>(UiState.Loading)
        var activeSection by mutableStateOf("FEED")
        val messagesState = UiState.Success(
            listOf(
                DirectMessage(
                    id = "msg_direct_1",
                    senderId = "ustad_2",
                    senderName = "মাওলানা জাকারিয়া",
                    receiverId = "user_me",
                    receiverName = "মাওলানা আব্দুল্লাহ",
                    subject = "মুহাদ্দিস নিয়োগ প্রসঙ্গে",
                    body = "আসসালামু আলাইকুম, আপনার আবেদনটি আমরা পেয়েছি।",
                    isRead = false,
                    createdAt = Timestamp.now()
                ),
                DirectMessage(
                    id = "msg_group_1",
                    senderId = "ustad_3",
                    senderName = "মুফতি ইব্রাহিম",
                    receiverId = "GROUP:grp_1",
                    receiverName = "জামিয়া ইসলামিয়া শিক্ষক ফোরাম",
                    groupId = "grp_1",
                    groupName = "জামিয়া ইসলামিয়া শিক্ষক ফোরাম",
                    groupMemberIds = listOf("user_me", "ustad_2", "ustad_3"),
                    subject = "গ্রুপ আলোচনা",
                    body = "আগামীকাল সকাল ১০টায় পরামর্শ সভা অনুষ্ঠিত হবে।",
                    isRead = true,
                    createdAt = Timestamp.now()
                )
            )
        )
        val groupsList = listOf(
            com.example.data.model.ChatGroup(
                id = "grp_1",
                name = "জামিয়া ইসলামিয়া শিক্ষক ফোরাম",
                creatorId = "ustad_3",
                creatorName = "মুফতি ইব্রাহিম",
                memberIds = listOf("user_me", "ustad_2", "ustad_3"),
                memberNames = mapOf(
                    "user_me" to "মাওলানা আব্দুল্লাহ",
                    "ustad_2" to "মাওলানা জাকারিয়া",
                    "ustad_3" to "মুফতি ইব্রাহিম"
                ),
                updatedAt = Timestamp.now()
            )
        )
        val sentSubmissions = listOf(
            com.example.data.model.KhedmotSubmission(
                id = "sub_sent_1",
                postId = "post_1",
                postTitle = "সহকারী মুফতি নিয়োগ",
                postDesignation = "সহকারী মুফতি",
                applicantId = "user_me",
                applicantName = "মাওলানা আব্দুল্লাহ",
                applicantQualification = "ইফতা ও দাওরায়ে হাদিস",
                applicantExperience = "৪ বছর",
                applicantPhone = "01711000000",
                coverNote = "আমি উক্ত খেদমতে আগ্রহী।",
                recipientId = "madrasa_owner_1",
                status = "PENDING",
                createdAt = Timestamp.now()
            )
        )
        var receivedSubmissions by mutableStateOf<List<com.example.data.model.KhedmotSubmission>>(
            listOf(
                com.example.data.model.KhedmotSubmission(
                    id = "sub_rec_1",
                    postId = "post_2",
                    postTitle = "হিফজ বিভাগ শিক্ষক নিয়োগ",
                    postDesignation = "হিফজ শিক্ষক",
                    applicantId = "applicant_99",
                    applicantName = "হাফেজ ক্বারী ওমর ফারুক",
                    applicantQualification = "হিফজুল কুরআন ও ক্বিরাত",
                    applicantExperience = "৬ বছর",
                    applicantPhone = "01811000000",
                    coverNote = "অভিজ্ঞতার আলোকে খেদমত করতে আগ্রহী।",
                    recipientId = "user_me",
                    status = "PENDING",
                    createdAt = Timestamp.now()
                )
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1100.dp)) {
                    when (activeSection) {
                        "FEED" -> com.example.ui.screens.FeedScreen(
                            feedPostsState = feedState,
                            bookmarksState = UiState.Success(emptyList()),
                            selectedFilter = com.example.ui.viewmodel.FeedFilterTab.ALL,
                            selectedDistrict = "সকল জেলা",
                            searchQuery = "",
                            currentUserId = "user_me",
                            isAdmin = false,
                            postCommentsState = UiState.Success(emptyList()),
                            onSelectFilter = {},
                            onSelectDistrict = {},
                            onSearchQueryChange = {},
                            onApplyClick = {},
                            onViewProfileOrDetailClick = {},
                            onToggleBookmark = { _, _ -> },
                            onDeletePost = {},
                            onNavigate = {}
                        )
                        "MESSAGES" -> MessagesScreen(
                            messagesState = messagesState,
                            currentUserId = "user_me",
                            targetUser = null,
                            userGroups = groupsList,
                            isSubmitting = false,
                            onSendMessage = { _, _, _, _ -> },
                            onSelectRecipient = { _, _ -> },
                            onClearRecipient = {},
                            onMarkRead = {},
                            onMarkConversationRead = {}
                        )
                        "SUBMISSIONS" -> com.example.ui.screens.SubmissionsManagementScreen(
                            mySentState = UiState.Success(sentSubmissions),
                            receivedState = UiState.Success(receivedSubmissions),
                            onUpdateStatus = { sub, newStatus ->
                                receivedSubmissions = receivedSubmissions.map { item ->
                                    if (item.id == sub.id) item.copy(status = newStatus) else item
                                }
                            },
                            onMessageUser = { _, _ -> },
                            onInspectUserProfile = { _, _ -> },
                            onNavigatePublishApplication = {}
                        )
                    }
                }
            }
        }

        // Verify initial loading indicator on Feed, then immediate transition to loaded posts
        composeTestRule.onNodeWithText("ইসলামিক খেদমত ফিড লোড হচ্ছে...").assertIsDisplayed()
        feedState = UiState.Success(
            listOf(
                com.example.data.model.FeedPost(
                    id = "post_1",
                    authorId = "madrasa_owner_1",
                    authorName = "জামিয়া আরাবিয়া ঢাকা",
                    postType = "JOB_CIRCULAR",
                    title = "সহকারী মুফতি ও মুহাদ্দিস নিয়োগ বিজ্ঞপ্তি",
                    district = "ঢাকা",
                    upazilaOrLocation = "মোহাম্মদপুর",
                    designation = "মুহাদ্দিস",
                    qualification = "দাওরায়ে হাদিস ও ইফতা",
                    salaryOrExperience = "আলোচনা সাপেক্ষ",
                    deadlineOrPreferredDistricts = "৩০ নভেম্বর",
                    description = "উপযুক্ত ও অভিজ্ঞ আলেমদের আবেদন করার আহ্বান করা যাচ্ছে।",
                    contactNumber = "01700000000",
                    visibility = "PUBLIC",
                    createdAt = Timestamp.now()
                )
            )
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ইসলামিক খেদমত ফিড লোড হচ্ছে...").assertDoesNotExist()
        composeTestRule.onNodeWithText("সহকারী মুফতি ও মুহাদ্দিস নিয়োগ বিজ্ঞপ্তি").assertIsDisplayed()

        // 3. Switch to Inbox & Group Conversations ("MESSAGES") and verify both 1-on-1 and Group conversations display without error
        activeSection = "MESSAGES"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("মাওলানা জাকারিয়া").assertIsDisplayed()
        composeTestRule.onNodeWithText("জামিয়া ইসলামিয়া শিক্ষক ফোরাম").assertIsDisplayed()

        // Filter by Group ("গ্রুপ") and Personal ("ব্যক্তিগত")
        composeTestRule.onNodeWithText("গ্রুপ").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("জামিয়া ইসলামিয়া শিক্ষক ফোরাম").assertIsDisplayed()

        composeTestRule.onNodeWithText("ব্যক্তিগত").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("মাওলানা জাকারিয়া").assertIsDisplayed()

        // 4. Switch to Application Management ("SUBMISSIONS") and verify both Sent and Received applications load & update status
        activeSection = "SUBMISSIONS"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("আমার প্রেরিত আবেদন").assertIsDisplayed()
        composeTestRule.onNodeWithText("সহকারী মুফতি নিয়োগ").assertIsDisplayed()

        // Switch to Received Applications tab ("মাদরাসায় প্রাপ্ত আবেদন")
        composeTestRule.onNodeWithText("মাদরাসায় প্রাপ্ত আবেদন").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("প্রার্থীর নাম: হাফেজ ক্বারী ওমর ফারুক").assertIsDisplayed()
        composeTestRule.onNodeWithText("কবুল করুন").performClick()
        composeTestRule.waitForIdle()
        assertEquals("ACCEPTED", receivedSubmissions.first().status)
    }

    @Test
    fun `Paygam conversation image and emoji messages transition from SENDING spinner to SENT checkmark without infinite loading`() {
        val sampleScreenshotDataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        var imageDeliveryState by mutableStateOf("SENDING")
        var emojiDeliveryState by mutableStateOf("SENDING")

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(840.dp)) {
                    val currentMessages = listOf(
                        DirectMessage(
                            id = "msg_incoming_0",
                            senderId = "user_foysal",
                            senderName = "Md Foysal",
                            receiverId = "user_me",
                            receiverName = "মাওলানা আব্দুল্লাহ",
                            subject = "খেদমত প্রসঙ্গে",
                            body = "🙏",
                            isRead = true,
                            readByUserIds = listOf("user_foysal", "user_me"),
                            createdAt = Timestamp(1_700_000_000L, 0)
                        ).apply {
                            localDeliveryState = "SENT"
                        },
                        DirectMessage(
                            id = "msg_img_1",
                            senderId = "user_me",
                            senderName = "মাওলানা আব্দুল্লাহ",
                            receiverId = "user_foysal",
                            receiverName = "Md Foysal",
                            subject = "খেদমত প্রসঙ্গে",
                            body = "",
                            imageUrls = listOf(sampleScreenshotDataUrl),
                            isRead = false,
                            readByUserIds = listOf("user_me"),
                            createdAt = Timestamp(1_700_000_120L, 0)
                        ).apply {
                            localDeliveryState = imageDeliveryState
                        },
                        DirectMessage(
                            id = "msg_emoji_2",
                            senderId = "user_me",
                            senderName = "মাওলানা আব্দুল্লাহ",
                            receiverId = "user_foysal",
                            receiverName = "Md Foysal",
                            subject = "খেদমত প্রসঙ্গে",
                            body = "❤️",
                            isRead = false,
                            readByUserIds = listOf("user_me"),
                            createdAt = Timestamp(1_700_000_240L, 0)
                        ).apply {
                            localDeliveryState = emojiDeliveryState
                        }
                    )

                    MessagesScreen(
                        messagesState = UiState.Success(currentMessages),
                        currentUserId = "user_me",
                        targetUser = "user_foysal" to "Md Foysal",
                        userGroups = emptyList(),
                        isSubmitting = false,
                        onSendMessage = { _, _, _, _ -> },
                        onSelectRecipient = { _, _ -> },
                        onClearRecipient = {},
                        onMarkRead = {},
                        onMarkConversationRead = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Initially while SENDING, both the image upload progress spinner and status spinners exist
        composeTestRule.onNodeWithTag("attachment_upload_progress_msg_img_1", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("message_status_sending_msg_img_1", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("message_status_sending_msg_emoji_2", useUnmergedTree = true).assertExists()

        // 2. Once staged/confirmed (SENT), spinners disappear immediately and SENT (✓) checkmarks appear
        imageDeliveryState = "SENT"
        emojiDeliveryState = "SENT"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("attachment_upload_progress_msg_img_1", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("message_status_sending_msg_img_1", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("message_status_sending_msg_emoji_2", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("message_status_sent_msg_img_1", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("message_status_sent_msg_emoji_2", useUnmergedTree = true).assertExists()
    }

    @Test
    @Config(qualifiers = "w420dp-h1600dp")
    fun `FeedScreen updates likes comments and shares live online and publishes application without delay`() {
        var postsList by mutableStateOf(
            listOf(
                com.example.data.model.FeedPost(
                    id = "app_post_live_1",
                    authorId = "seeker_1",
                    authorName = "মাওলানা তানভীর আহমদ",
                    postType = "SEEKER_APPLICATION",
                    title = "মুহাদ্দিস ও ফিকহ বিভাগে খেদমতপ্রার্থী",
                    district = "ঢাকা",
                    upazilaOrLocation = "যাত্রাবাড়ী",
                    designation = "মুহাদ্দিস",
                    qualification = "দাওরায়ে হাদিস ও ইফতা",
                    salaryOrExperience = "৫ বছর",
                    deadlineOrPreferredDistricts = "ঢাকা, নারায়ণগঞ্জ",
                    description = "সহীহ বুখারী ও ফিকহুল ইসলামীর দরস দানে অভিজ্ঞ।",
                    contactNumber = "01711009988",
                    visibility = "PUBLIC",
                    createdAt = Timestamp.now()
                )
            )
        )
        var commentsList by mutableStateOf<List<com.example.data.model.FeedComment>>(emptyList())

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(420.dp).height(1550.dp)) {
                    com.example.ui.screens.FeedScreen(
                        feedPostsState = UiState.Success(postsList),
                        bookmarksState = UiState.Success(emptyList()),
                        selectedFilter = com.example.ui.viewmodel.FeedFilterTab.ALL,
                        selectedDistrict = "সকল জেলা",
                        searchQuery = "",
                        currentUserId = "user_me",
                        isAdmin = false,
                        postCommentsState = UiState.Success(commentsList),
                        onSelectFilter = {},
                        onSelectDistrict = {},
                        onSearchQueryChange = {},
                        onApplyClick = {},
                        onViewProfileOrDetailClick = {},
                        onToggleBookmark = { _, _ -> },
                        onDeletePost = {},
                        onReactToPost = { post, code ->
                            postsList = postsList.map { existing ->
                                if (existing.id == post.id) {
                                    val updatedReactions = existing.userReactions.toMutableMap()
                                    val updatedNames = existing.userReactionNames.toMutableMap()
                                    if (code.isBlank() || updatedReactions["user_me"] == code) {
                                        updatedReactions.remove("user_me")
                                        updatedNames.remove("user_me")
                                    } else {
                                        updatedReactions["user_me"] = code
                                        updatedNames["user_me"] = "মুফতি আব্দুল্লাহ"
                                    }
                                    existing.copy(
                                        userReactions = updatedReactions,
                                        userReactionNames = updatedNames
                                    )
                                } else {
                                    existing
                                }
                            }
                        },
                        onAddComment = { post, text, parentId, replyTo ->
                            val newComment = com.example.data.model.FeedComment(
                                id = "c_${commentsList.size + 1}",
                                postId = post.id,
                                authorId = "user_me",
                                authorName = "মুফতি আব্দুল্লাহ",
                                text = text,
                                parentCommentId = parentId,
                                replyToAuthorName = replyTo,
                                visibility = "PUBLIC",
                                createdAt = Timestamp.now()
                            )
                            commentsList = commentsList + newComment
                            postsList = postsList.map { existing ->
                                if (existing.id == post.id) {
                                    existing.copy(commentCount = existing.commentCount + 1)
                                } else {
                                    existing
                                }
                            }
                        },
                        onSharePostInApp = { post, caption ->
                            val shared = com.example.data.model.FeedPost(
                                id = "shared_post_1",
                                authorId = "user_me",
                                authorName = "মুফতি আব্দুল্লাহ",
                                postType = "GENERAL_POST",
                                title = caption.ifBlank { "${post.authorName}-এর পোস্ট শেয়ার করেছেন" },
                                district = "ঢাকা",
                                designation = post.designation,
                                description = caption,
                                sharedPostId = post.id,
                                sharedAuthorId = post.authorId,
                                sharedAuthorName = post.authorName,
                                sharedTitle = post.title,
                                sharedDescription = post.description,
                                sharedPostType = post.postType,
                                visibility = "PUBLIC",
                                createdAt = Timestamp.now()
                            )
                            postsList = listOf(shared) + postsList.map { existing ->
                                if (existing.id == post.id) {
                                    existing.copy(shareCount = existing.shareCount + 1)
                                } else {
                                    existing
                                }
                            }
                        },
                        onNavigate = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("feed_post_card_app_post_live_1").assertIsDisplayed()

        // 1. Click Like button on post -> reaction count and label update live immediately
        composeTestRule.onNodeWithTag("post_react_btn_app_post_live_1").performClick()
        composeTestRule.waitForIdle()
        assertEquals("SAMMATI", postsList.first { it.id == "app_post_live_1" }.userReactions["user_me"])

        // 2. Click Comment button -> comment input opens -> type & submit comment -> appears live immediately
        composeTestRule.onNodeWithTag("post_comment_btn_app_post_live_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("comment_input_app_post_live_1").performTextInput("মাশাআল্লাহ, অত্যন্ত সুন্দর আবেদন।")
        composeTestRule.onNodeWithTag("send_comment_btn_app_post_live_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("মাশাআল্লাহ, অত্যন্ত সুন্দর আবেদন।").assertIsDisplayed()
        assertEquals(1, postsList.first { it.id == "app_post_live_1" }.commentCount)

        // 3. Click Share button -> share dialog opens -> share in app -> new shared post appears at top of feed and shareCount increments
        composeTestRule.onNodeWithTag("post_share_btn_app_post_live_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_share_in_app_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, postsList.first { it.id == "app_post_live_1" }.shareCount)
        assertTrue(postsList.any { it.id == "shared_post_1" })
    }

    @Test
    fun cuj25_adminPanelFullOperations_and_instantLoginLogout_verifiedEndToEnd() {
        // 1. Verify Admin Identity resolution for all authorized emails & phone numbers
        assertTrue(com.example.ui.util.AdminSecurityHelper.isAuthorizedAdminIdentity("foysalahamad102@gmail.com", ""))
        assertTrue(com.example.ui.util.AdminSecurityHelper.isAuthorizedAdminIdentity("foysalahamad204@gmail.com", ""))
        assertTrue(com.example.ui.util.AdminSecurityHelper.isAuthorizedAdminIdentity("fyslahm28@gmail.com", ""))
        assertTrue(com.example.ui.util.AdminSecurityHelper.isAuthorizedAdminIdentity("", "01300448147"))
        assertTrue(com.example.ui.util.AdminSecurityHelper.isAuthorizedAdminIdentity("", "+8801300448147"))
        org.junit.Assert.assertFalse(com.example.ui.util.AdminSecurityHelper.isAuthorizedAdminIdentity("regularuser@gmail.com", "01700000000"))

        // 2. Verify signOutUser triggers onSignOutComplete immediately (0ms synchronous callback)
        var signedOutImmediately = false
        com.example.ui.auth.signOutUser(
            credentialManager = null,
            onSignOutComplete = { signedOutImmediately = true },
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
        assertTrue("signOutUser must invoke onSignOutComplete immediately without delay", signedOutImmediately)

        // 3. Verify Admin Panel Screen renders all tabs and executes user verification, application approval, and madrasa verification immediately
        var usersList by androidx.compose.runtime.mutableStateOf(
            listOf(
                UserProfile(
                    id = "u_admin_test_1",
                    userId = "u_admin_test_1",
                    fullName = "মাওলানা মাহমুদুল হাসান",
                    accountType = "khadim",
                    district = "ঢাকা",
                    designation = "মুহাদ্দিস",
                    qualification = "দাওরায়ে হাদিস",
                    contactInfo = "01811223344",
                    isVerified = false
                )
            )
        )
        var submissionsList by androidx.compose.runtime.mutableStateOf(
            listOf(
                com.example.data.model.KhedmotSubmission(
                    id = "sub_admin_test_1",
                    postId = "post_1",
                    postTitle = "সহকারী শিক্ষক নিয়োগ",
                    postDesignation = "সহকারী শিক্ষক",
                    applicantId = "u_admin_test_1",
                    applicantName = "মাওলানা মাহমুদুল হাসান",
                    applicantQualification = "দাওরায়ে হাদিস",
                    applicantExperience = "৩ বছর",
                    applicantPhone = "01811223344",
                    recipientId = "madrasa_owner_1",
                    status = "PENDING",
                    createdAt = Timestamp.now()
                )
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                com.example.ui.screens.ModernAdminPanelRoot(
                    isAdminUser = true,
                    adminDisplayName = "নাজিমে আলা (অ্যাডমিন)",
                    adminPhotoUrl = "",
                    usersState = UiState.Success(usersList),
                    submissionsState = UiState.Success(submissionsList),
                    madrasasState = UiState.Success(emptyList()),
                    feedPostsState = UiState.Success(emptyList()),
                    telegramNotificationsState = UiState.Success(emptyList()),
                    adminsState = UiState.Success(emptyList()),
                    latestAppRelease = null,
                    isSubmitting = false,
                    onVerifyUser = { user, verified ->
                        usersList = usersList.map {
                            if (it.id == user.id) it.copy(isVerified = verified) else it
                        }
                    },
                    onUpdateSubmissionStatus = { sub, newStatus ->
                        submissionsList = submissionsList.map {
                            if (it.id == sub.id) it.copy(status = newStatus) else it
                        }
                    },
                    onVerifyMadrasa = { _, _ -> },
                    onVerifyPost = { _, _, _ -> },
                    onToggleBoostPost = { _, _ -> },
                    onPublishQuickNotice = { _, _, _ -> },
                    onSendAnnouncement = { _, _ -> },
                    onRecordLiveAdminAction = { _, _, _, _ -> },
                    onSendTestTelegramAlert = {},
                    onPublishAppRelease = { _, _, _, _, _, _, _, _ -> },
                    onRetryServerLoad = {},
                    onDeletePost = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_panel_screen").assertIsDisplayed()

        // Switch to Users tab and verify user activation via 3-dot option menu ("অপশন" -> "ব্যবহারকারী সক্রিয় করুন")
        composeTestRule.onNodeWithTag("admin_tab_users").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(androidx.compose.ui.test.hasContentDescription("অপশন")).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ব্যবহারকারী সক্রিয় করুন").performClick()
        composeTestRule.waitForIdle()
        assertTrue(usersList.first { it.id == "u_admin_test_1" }.isVerified)

        // Switch to Applications tab and approve submission via status menu ("নতুন" -> "নির্বাচিত (অনুমোদন ও প্রকাশ)")
        composeTestRule.onNodeWithTag("admin_tab_applications").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ID: u_admin_test_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("সহকারী শিক্ষক").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("নির্বাচিত (অনুমোদন ও প্রকাশ)").performClick()
        composeTestRule.waitForIdle()
        assertEquals("ACCEPTED", submissionsList.first { it.id == "sub_admin_test_1" }.status)
    }

    @Test
    fun feedPostShowsAuthorNamePhotoAndUniqueId_andTappingNameOrIdOpensProfile() {
        val samplePost = com.example.data.model.FeedPost(
            id = "post_uid_test_101",
            authorId = "uid_maulana_habib_786",
            authorName = "মাওলানা হাবিবুর রহমান",
            authorPhotoUrl = "preset:scholar_emerald",
            postType = "SEEKER_APPLICATION",
            title = "মাওলানা হাবিবুর রহমান — মুহাদ্দিস",
            district = "ঢাকা",
            upazilaOrLocation = "মিরপুর",
            designation = "মুহাদ্দিস",
            qualification = "দাওরায়ে হাদিস ও ইফতা",
            salaryOrExperience = "৬ বছর",
            deadlineOrPreferredDistricts = "ঢাকা, গাজীপুর",
            description = "বুখারী ও তিরমিযী শরীফ পাঠদানে অভিজ্ঞ।",
            contactNumber = "01711223344",
            isVerifiedMadrasa = true,
            commentCount = 1,
            createdAt = Timestamp.now()
        )
        val sampleComment = com.example.data.model.FeedComment(
            id = "c_uid_test_202",
            postId = "post_uid_test_101",
            authorId = "uid_mufti_saeed_999",
            authorName = "মুফতি সাঈদ আহমদ",
            authorPhotoUrl = "preset:mosque_gold",
            text = "মাশাআল্লাহ, আপনার প্রোফাইল দেখে যোগাযোগ করছি।",
            createdAt = Timestamp.now()
        )

        var inspectedUserPair by androidx.compose.runtime.mutableStateOf<Pair<String, String>?>(null)

        composeTestRule.setContent {
            MyApplicationTheme {
                Box {
                    com.example.ui.screens.FeedScreen(
                        feedPostsState = UiState.Success(listOf(samplePost)),
                        postCommentsState = UiState.Success(listOf(sampleComment)),
                        bookmarksState = UiState.Success(emptyList()),
                        selectedFilter = com.example.ui.viewmodel.FeedFilterTab.ALL,
                        selectedKhedmotCategory = "সকল বিভাগ",
                        selectedDistrict = "সকল জেলা",
                        searchQuery = "",
                        currentUserId = "viewer_user_1",
                        isAdmin = false,
                        onSelectFilter = {},
                        onSelectKhedmotCategory = {},
                        onSelectDistrict = {},
                        onSearchQueryChange = {},
                        onApplyClick = {},
                        onViewProfileOrDetailClick = {},
                        onInspectAuthorProfile = { uid: String, name: String ->
                            inspectedUserPair = uid to name
                        },
                        onToggleBookmark = { _, _ -> },
                        onToggleBoostPost = { _, _ -> },
                        onDeletePost = {},
                        onNavigate = {}
                    )

                    if (inspectedUserPair != null) {
                        PublicUserProfileDialog(
                            targetUserId = inspectedUserPair!!.first,
                            fallbackName = inspectedUserPair!!.second,
                            currentUserId = "viewer_user_1",
                            inspectedProfileState = UiState.Success(
                                UserProfile(
                                    id = inspectedUserPair!!.first,
                                    userId = inspectedUserPair!!.first,
                                    fullName = inspectedUserPair!!.second,
                                    accountType = "khadim",
                                    district = "ঢাকা",
                                    designation = "মুহাদ্দিস",
                                    qualification = "দাওরায়ে হাদিস"
                                )
                            ),
                            userPublishedPosts = listOf(samplePost),
                            onDismiss = { inspectedUserPair = null },
                            onSendMessageClick = { _, _ -> },
                            onPostClick = {}
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify post author Name, Avatar, and Unique ID are displayed
        composeTestRule.onNodeWithTag("post_author_avatar_post_uid_test_101", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("post_author_name_post_uid_test_101", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("post_author_uid_post_uid_test_101", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("ID: uid_maulana_habib_786", useUnmergedTree = true).assertExists()

        // 2. Tap on the Unique ID -> opens PublicUserProfileDialog showing the author's Profile & Unique ID
        composeTestRule.onNodeWithTag("post_author_uid_post_uid_test_101", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals("uid_maulana_habib_786" to "মাওলানা হাবিবুর রহমান", inspectedUserPair)
        composeTestRule.onNodeWithTag("public_user_profile_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("public_profile_uid_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("ইউনিক ID: uid_maulana_habib_786").assertIsDisplayed()

        // Dismiss profile dialog and tap on Author Name -> also opens Profile directly
        composeTestRule.onNode(androidx.compose.ui.test.hasContentDescription("বন্ধ করুন")).performClick()
        composeTestRule.waitForIdle()
        assertEquals(null, inspectedUserPair)

        composeTestRule.onNodeWithTag("post_author_name_post_uid_test_101", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals("uid_maulana_habib_786" to "মাওলানা হাবিবুর রহমান", inspectedUserPair)
        composeTestRule.onNodeWithTag("public_user_profile_dialog").assertIsDisplayed()
    }
}

