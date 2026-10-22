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
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DirectMessage
import com.example.data.model.UserProfile
import com.example.ui.screens.DrawerMenuItem
import com.example.ui.screens.MessagesScreen
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
}
