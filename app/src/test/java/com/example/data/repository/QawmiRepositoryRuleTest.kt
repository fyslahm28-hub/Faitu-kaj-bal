package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class QawmiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun findFirestoreException(throwable: Throwable?): FirebaseFirestoreException? {
        var current = throwable
        while (current != null) {
            if (current is FirebaseFirestoreException) return current
            current = current.cause
        }
        return null
    }

    @Test
    fun createFeedPost_deliversNotificationToOtherUsers_notToSelf_andSupportsReadReceipts() = runBlocking {
        val muhtamimUid = signInTestUser("muhtamim_${UUID.randomUUID()}@test.com")
        val muhtamimRepo = QawmiRepository(firestore, auth)

        val uniqueTitle = "দারুল উলুম মাদরাসা ${UUID.randomUUID()}"
        val createResult = withTimeout(5000L) {
            muhtamimRepo.createFeedPost(
                authorName = uniqueTitle,
                postType = "JOB_CIRCULAR",
                title = uniqueTitle,
                district = "কুমিল্লা",
                upazilaOrLocation = "কুমিল্লা সদর",
                designation = "সহকারী শিক্ষক (আরবি, ফিকহ)",
                qualification = "দাওরায়ে হাদীস",
                salaryOrExperience = "১২,০০০ - ১৮,০০০ টাকা",
                deadlineOrPreferredDistricts = "৩০ অক্টোবর ২০২৬",
                description = "আবাসন ও খানা মাদরাসা বহন করবে।",
                contactNumber = "01711000000"
            )
        }
        assertTrue("createFeedPost failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val postId = createResult.getOrThrow()

        // 1. Author (self) does NOT see self-generated notification
        val authorNotifs = withTimeout(4000L) {
            muhtamimRepo.observeNotifications(muhtamimUid).first()
        }
        assertFalse(
            "Self-created circular should not notify the author",
            authorNotifs.any { it.referenceId == postId }
        )

        // 2. Another user (khadim) sees the post AND receives the unread notification
        val khadimUid = signInTestUser("khadim_${UUID.randomUUID()}@test.com")
        val khadimRepo = QawmiRepository(firestore, auth)

        val khadimNotifsBeforeRead = withTimeout(4000L) {
            khadimRepo.observeNotifications(khadimUid).first { list ->
                list.any { it.referenceId == postId }
            }
        }
        val targetNotif = khadimNotifsBeforeRead.first { it.referenceId == postId }
        assertFalse("Notification should initially be unread", targetNotif.isRead)

        // 3. Khadim marks the notification as read; unread status becomes true
        val markReadRes = withTimeout(4000L) {
            khadimRepo.markNotificationAsRead(targetNotif)
        }
        assertTrue(markReadRes.isSuccess)

        val khadimNotifsAfterRead = withTimeout(4000L) {
            khadimRepo.observeNotifications(khadimUid).first { list ->
                list.any { it.id == targetNotif.id && it.isRead }
            }
        }
        assertTrue(khadimNotifsAfterRead.first { it.id == targetNotif.id }.isRead)
    }

    @Test
    fun twoUsers_profilePhotoAndPresence_persistedAndVisibleToOtherUsers_andPropagatedToPosts() = runBlocking {
        // User 1 signs in, saves profile with initial avatar, and publishes an application post
        val user1Email = "scholar1_${UUID.randomUUID()}@test.com"
        val user1Uid = signInTestUser(user1Email)
        val repo = QawmiRepository(firestore, auth)

        val initialPhoto = "preset:scholar_emerald"
        val saveRes1 = withTimeout(5000L) {
            repo.saveUserProfile(
                UserProfile(
                    userId = user1Uid,
                    fullName = "মাওলানা আব্দুল্লাহ আল মামুন",
                    accountType = "khadim",
                    district = "ঢাকা",
                    designation = "মুহাদ্দিস",
                    qualification = "দাওরায়ে হাদীস ও ইফতা",
                    experience = "৫ বছর",
                    preferredDistricts = "ঢাকা, গাজীপুর",
                    bio = "দারুল উলুম দেওবন্দ ফারেগ, বুখারী ও তিরমিযী শরীফ দরসদানে অভিজ্ঞ।",
                    contactInfo = "01811223344",
                    madrasaName = "",
                    photoUrl = initialPhoto
                )
            )
        }
        assertTrue("Initial profile save failed: ${saveRes1.exceptionOrNull()}", saveRes1.isSuccess)

        val postCreateRes = withTimeout(5000L) {
            repo.createFeedPost(
                authorName = "মাওলানা আব্দুল্লাহ আল মামুন",
                postType = "SEEKER_APPLICATION",
                title = "মাওলানা আব্দুল্লাহ আল মামুন",
                district = "ঢাকা",
                upazilaOrLocation = "মোহাম্মদপুর",
                designation = "মুহাদ্দিস",
                qualification = "দাওরায়ে হাদীস ও ইফতা",
                salaryOrExperience = "৫ বছর",
                deadlineOrPreferredDistricts = "ঢাকা, গাজীপুর",
                description = "সহীহ বুখারী ও ফিকহ দরসদানে আগ্রহী।",
                contactNumber = "01811223344",
                authorPhotoUrl = initialPhoto
            )
        }
        assertTrue(postCreateRes.isSuccess)
        val postId = postCreateRes.getOrThrow()

        // User 1 updates profile photo to a new Base64 photo and updates name, then goes offline (lastSeen updated)
        val updatedBase64Photo = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/"
        val updateRes = withTimeout(5000L) {
            repo.saveUserProfile(
                UserProfile(
                    userId = user1Uid,
                    fullName = "মুফতি আব্দুল্লাহ আল মামুন",
                    accountType = "khadim",
                    district = "ঢাকা",
                    designation = "প্রধান মুফতি ও মুহাদ্দিস",
                    qualification = "দাওরায়ে হাদীস ও তাখাসসুস ফিল ফিকহ",
                    experience = "৬ বছর",
                    preferredDistricts = "ঢাকা, নারায়ণগঞ্জ",
                    bio = "ফতোয়া বিভাগ ও হাদীস দরসদানে বিশেষজ্ঞ।",
                    contactInfo = "01811223344",
                    madrasaName = "",
                    photoUrl = updatedBase64Photo
                )
            )
        }
        assertTrue("Profile update failed: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)

        val presenceRes = withTimeout(5000L) {
            repo.updateUserPresence(isOnline = false)
        }
        assertTrue("Presence update failed: ${presenceRes.exceptionOrNull()}", presenceRes.isSuccess)

        // User 2 signs in and inspects User 1's profile and feed post
        val user2Uid = signInTestUser("muhtamim2_${UUID.randomUUID()}@test.com")
        assertTrue(user1Uid != user2Uid)

        val observedUser1Profile = withTimeout(5000L) {
            repo.observeUserProfile(user1Uid).first { it != null && it.photoUrl == updatedBase64Photo }
        }
        assertNotNull(observedUser1Profile)
        assertEquals("মুফতি আব্দুল্লাহ আল মামুন", observedUser1Profile?.fullName)
        assertEquals(updatedBase64Photo, observedUser1Profile?.photoUrl)
        assertEquals("প্রধান মুফতি ও মুহাদ্দিস", observedUser1Profile?.designation)
        assertFalse("User 1 should be seen as offline by User 2", observedUser1Profile?.isOnline ?: true)
        assertNotNull("User 1 lastSeenAt should be recorded", observedUser1Profile?.lastSeenAt)

        // Verify User 1's feed post also propagated the updated photoUrl & authorName to User 2
        val feedPostsForUser2 = withTimeout(5000L) {
            repo.observePublicFeedPosts().first { posts ->
                posts.any { it.id == postId && it.authorPhotoUrl == updatedBase64Photo }
            }
        }
        val updatedPost = feedPostsForUser2.first { it.id == postId }
        assertEquals("মুফতি আব্দুল্লাহ আল মামুন", updatedPost.authorName)
        assertEquals(updatedBase64Photo, updatedPost.authorPhotoUrl)
    }

    @Test
    fun twoUsers_realTimeMessaging_conversationHistory_unreadCount_andReadReceipts() = runBlocking {
        val userAEmail = "userA_${UUID.randomUUID()}@test.com"
        val userBEmail = "userB_${UUID.randomUUID()}@test.com"

        // Register User B first to get UID
        val userBUid = signInTestUser(userBEmail)
        val repo = QawmiRepository(firestore, auth)
        withTimeout(5000L) {
            repo.saveUserProfile(
                UserProfile(
                    userId = userBUid,
                    fullName = "মাওলানা মাহমুদুল হাসান",
                    accountType = "khadim",
                    district = "সিলেট",
                    designation = "সিনিয়র উস্তায",
                    qualification = "দাওরায়ে হাদীস",
                    experience = "৪ বছর",
                    preferredDistricts = "সিলেট",
                    bio = "নাহু-সরফ ও ফিকহ উস্তায",
                    contactInfo = "01722334455",
                    madrasaName = "",
                    photoUrl = "preset:scholar_gold"
                )
            )
        }

        // Sign in as User A
        val userAUid = signInTestUser(userAEmail)
        withTimeout(5000L) {
            repo.saveUserProfile(
                UserProfile(
                    userId = userAUid,
                    fullName = "জামিয়া ইসলামিয়া মাদরাসা",
                    accountType = "muhtamim",
                    district = "সিলেট",
                    designation = "মুহতামিম",
                    qualification = "দাওরায়ে হাদীস",
                    experience = "১০ বছর",
                    preferredDistricts = "সিলেট",
                    bio = "সিলেট সদর কওমি মাদরাসা",
                    contactInfo = "01999887766",
                    madrasaName = "জামিয়া ইসলামিয়া মাদরাসা",
                    photoUrl = "preset:muhtamim_royal"
                )
            )
        }

        // Verify User A cannot message themselves
        val selfMsgRes = withTimeout(4000L) {
            repo.sendDirectMessage(
                senderName = "জামিয়া ইসলামিয়া মাদরাসা",
                receiverId = userAUid,
                receiverName = "জামিয়া ইসলামিয়া মাদরাসা",
                subject = "নিজেকে মেসেজ",
                body = "এটি ব্লক হওয়া উচিত"
            )
        }
        assertTrue("Self-messaging must be rejected", selfMsgRes.isFailure)

        // User A sends message 1 to User B
        val send1Res = withTimeout(5000L) {
            repo.sendDirectMessage(
                senderName = "জামিয়া ইসলামিয়া মাদরাসা",
                receiverId = userBUid,
                receiverName = "মাওলানা মাহমুদুল হাসান",
                subject = "সাক্ষাৎকারের আমন্ত্রণ",
                body = "আসসালামু আলাইকুম, আগামী শুক্রবার সকাল ১০টায় মাদরাসায় তাশরীফ আনার অনুরোধ।",
                senderPhotoUrl = "preset:muhtamim_royal"
            )
        }
        assertTrue("User A -> User B message failed: ${send1Res.exceptionOrNull()}", send1Res.isSuccess)
        val msg1Id = send1Res.getOrThrow()

        // Switch to User B and verify message 1 arrived in inbox as unread + notification delivered
        signInTestUser(userBEmail)
        val userBMessages = withTimeout(5000L) {
            repo.observeUserMessages(userBUid).first { list -> list.any { it.id == msg1Id } }
        }
        val receivedMsg1 = userBMessages.first { it.id == msg1Id }
        assertEquals(userAUid, receivedMsg1.senderId)
        assertEquals(userBUid, receivedMsg1.receiverId)
        assertEquals("preset:muhtamim_royal", receivedMsg1.senderPhotoUrl)
        assertFalse("Message 1 should initially be unread for User B", receivedMsg1.isRead)

        // Unread message count for User B with User A is 1
        val unreadForB = userBMessages.count { it.receiverId == userBUid && !it.isRead }
        assertEquals(1, unreadForB)

        // User B marks the conversation with User A as read
        val markConvRes = withTimeout(5000L) {
            repo.markConversationMessagesAsRead(userAUid, userBMessages)
        }
        assertTrue("markConversationMessagesAsRead failed: ${markConvRes.exceptionOrNull()}", markConvRes.isSuccess)

        val userBMessagesAfterRead = withTimeout(5000L) {
            repo.observeUserMessages(userBUid).first { list ->
                list.any { it.id == msg1Id && it.isRead }
            }
        }
        assertEquals(0, userBMessagesAfterRead.count { it.receiverId == userBUid && !it.isRead })

        // User B replies to User A
        val send2Res = withTimeout(5000L) {
            repo.sendDirectMessage(
                senderName = "মাওলানা মাহমুদুল হাসান",
                receiverId = userAUid,
                receiverName = "জামিয়া ইসলামিয়া মাদরাসা",
                subject = "সাক্ষাৎকারের আমন্ত্রণ",
                body = "ওয়া আলাইকুমুস সালাম, ইনশাআল্লাহ আমি যথাসময়ে উপস্থিত থাকব।",
                senderPhotoUrl = "preset:scholar_gold"
            )
        }
        assertTrue("User B -> User A reply failed: ${send2Res.exceptionOrNull()}", send2Res.isSuccess)
        val msg2Id = send2Res.getOrThrow()

        // Switch back to User A and verify full 2-message conversation history & read receipt on msg1
        signInTestUser(userAEmail)
        val userAMessages = withTimeout(5000L) {
            repo.observeUserMessages(userAUid).first { list ->
                list.any { it.id == msg1Id && it.isRead } && list.any { it.id == msg2Id }
            }
        }
        val msg1SeenByA = userAMessages.first { it.id == msg1Id }
        val msg2SeenByA = userAMessages.first { it.id == msg2Id }
        assertTrue("User A should see that msg1 was read by User B", msg1SeenByA.isRead)
        assertFalse("User B's reply should initially be unread for User A", msg2SeenByA.isRead)
        assertEquals("ওয়া আলাইকুমুস সালাম, ইনশাআল্লাহ আমি যথাসময়ে উপস্থিত থাকব।", msg2SeenByA.body)
    }

    @Test
    fun adminPanelOperations_verifyMadrasa_boostPost_andBroadcastAnnouncement() = runBlocking {
        val adminEmail = "fyslahm28@gmail.com"
        val regularEmail = "member_${UUID.randomUUID()}@test.com"

        val adminUid = signInTestUser(adminEmail)
        val repo = QawmiRepository(firestore, auth)

        // Register a madrasa and verify it via Admin function
        val madrasaRes = withTimeout(5000L) {
            repo.createMadrasa(
                name = "জামিয়া আরাবিয়া দারুল উলুম",
                district = "চট্টগ্রাম",
                address = "হাটহাজারী রোড",
                establishedYear = "১৯৮৫",
                highestMarhala = "দাওরায়ে হাদীস",
                facilities = "আবাসিক, কুতুবখানা",
                studentCount = "১২০০",
                contactPhone = "01819001122"
            )
        }
        assertTrue(madrasaRes.isSuccess)
        val madrasaId = madrasaRes.getOrThrow()

        val initialMadrasas = withTimeout(5000L) {
            repo.observePublicMadrasas().first { list -> list.any { it.id == madrasaId } }
        }
        val targetMadrasa = initialMadrasas.first { it.id == madrasaId }

        val verifyMadrasaRes = withTimeout(5000L) {
            repo.adminToggleMadrasaVerification(targetMadrasa, true)
        }
        assertTrue("adminToggleMadrasaVerification failed: ${verifyMadrasaRes.exceptionOrNull()}", verifyMadrasaRes.isSuccess)

        // Publish a post and Boost it via Admin function
        val postRes = withTimeout(5000L) {
            repo.createFeedPost(
                authorName = "জামিয়া আরাবিয়া দারুল উলুম",
                postType = "JOB_CIRCULAR",
                title = "জামিয়া আরাবিয়া দারুল উলুম",
                district = "চট্টগ্রাম",
                upazilaOrLocation = "হাটহাজারী",
                designation = "নাজিমে তালিমাত",
                qualification = "দাওরায়ে হাদীস ও অভিজ্ঞ",
                salaryOrExperience = "আলোচনা সাপেক্ষে",
                deadlineOrPreferredDistricts = "১৫ নভেম্বর ২০২৬",
                description = "প্রশাসনিক কাজে দক্ষ হতে হবে।",
                contactNumber = "01819001122"
            )
        }
        assertTrue(postRes.isSuccess)
        val postId = postRes.getOrThrow()

        val initialPosts = withTimeout(5000L) {
            repo.observePublicFeedPosts().first { list -> list.any { it.id == postId } }
        }
        val targetPost = initialPosts.first { it.id == postId }

        val boostRes = withTimeout(5000L) {
            repo.adminTogglePostBoost(targetPost, true)
        }
        assertTrue("adminTogglePostBoost failed: ${boostRes.exceptionOrNull()}", boostRes.isSuccess)

        // Broadcast an admin announcement
        val announceTitle = "বিশেষ ঘোষণা ${UUID.randomUUID()}"
        val announceRes = withTimeout(5000L) {
            repo.sendAdminAnnouncement(
                adminName = "নাজিমে আলা",
                title = announceTitle,
                message = "সকল মাদরাসার নিবন্ধন তথ্য হালনাগাদ করার অনুরোধ করা হচ্ছে।"
            )
        }
        assertTrue("sendAdminAnnouncement failed: ${announceRes.exceptionOrNull()}", announceRes.isSuccess)

        // Switch to regular user and verify:
        // 1. Madrasa is verified
        // 2. Post is boosted
        // 3. Announcement notification is received and can be marked read
        val regularUid = signInTestUser(regularEmail)
        assertTrue(adminUid != regularUid)

        val madrasas = withTimeout(5000L) {
            repo.observePublicMadrasas().first { list -> list.any { it.id == madrasaId && it.isVerified } }
        }
        assertTrue(madrasas.first { it.id == madrasaId }.isVerified)

        val posts = withTimeout(5000L) {
            repo.observePublicFeedPosts().first { list -> list.any { it.id == postId && it.isBoosted } }
        }
        assertTrue(posts.first { it.id == postId }.isBoosted)

        val notifs = withTimeout(5000L) {
            repo.observeNotifications(regularUid).first { list -> list.any { it.title == announceTitle } }
        }
        val announceNotif = notifs.first { it.title == announceTitle }
        assertFalse(announceNotif.isRead)

        val markAllRes = withTimeout(5000L) {
            repo.markAllNotificationsAsRead(notifs)
        }
        assertTrue(markAllRes.isSuccess)

        val notifsAfterMarkAll = withTimeout(5000L) {
            repo.observeNotifications(regularUid).first { list ->
                list.all { it.isRead }
            }
        }
        assertEquals(0, notifsAfterMarkAll.count { !it.isRead })
    }

    @Test
    fun observePublicFeedPosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = QawmiRepository(firestore, auth)

        try {
            withTimeout(4000L) {
                repository.observePublicFeedPosts().first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val firestoreEx = findFirestoreException(e)
            assertNotNull("Expected underlying FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }
}
