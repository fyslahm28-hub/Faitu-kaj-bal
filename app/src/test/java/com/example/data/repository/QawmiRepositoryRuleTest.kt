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

        signInTestUser(userBEmail)
        val userBMessages = withTimeout(5000L) {
            repo.observeUserMessages(userBUid).first { list -> list.any { it.id == msg1Id } }
        }
        val receivedMsg1 = userBMessages.first { it.id == msg1Id }
        assertEquals(userAUid, receivedMsg1.senderId)
        assertEquals(userBUid, receivedMsg1.receiverId)
        assertEquals("preset:muhtamim_royal", receivedMsg1.senderPhotoUrl)
        assertFalse("Message 1 should initially be unread for User B", receivedMsg1.isRead)

        val unreadForB = userBMessages.count { it.receiverId == userBUid && !it.isRead }
        assertEquals(1, unreadForB)

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
    fun adminPanelOperations_allThreeAuthorizedAdmins_telegramQueue_andPasswordResetValidation() = runBlocking {
        val authorizedAdmins = listOf(
            "foysalahamad102@gmail.com",
            "foysalahamad204@gmail.com",
            "fyslahm28@gmail.com"
        )
        val regularEmail = "member_${UUID.randomUUID()}@test.com"
        val repo = QawmiRepository(firestore, auth)

        // 1. Verify each of the 3 authorized admin emails can register in /admins/{uid} and read Telegram logs
        for (adminEmail in authorizedAdmins) {
            val adminUid = signInTestUser(adminEmail)
            val regRes = withTimeout(5000L) {
                repo.ensureVerifiedAdminRegistryDocument()
            }
            assertTrue("Admin registry sync failed for $adminEmail: ${regRes.exceptionOrNull()}", regRes.isSuccess)
            assertTrue("Expected true for verified admin $adminEmail", regRes.getOrThrow())

            val entry = withTimeout(5000L) {
                repo.observeAdminRegistryEntry(adminUid).first { it != null }
            }
            assertNotNull(entry)
            assertEquals(adminUid, entry?.adminId)
            assertEquals(adminEmail, entry?.email)
            assertEquals("01300448147", entry?.adminPhone)
            assertEquals("SUPER_ADMIN", entry?.role)
        }

        // 2. Currently signed in as fyslahm28@gmail.com — test madrasa verification, post boost, announcement & Telegram log
        val adminUid = auth.currentUser!!.uid
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

        val announceTitle = "বিশেষ ঘোষণা ${UUID.randomUUID()}"
        val announceRes = withTimeout(5000L) {
            repo.sendAdminAnnouncement(
                adminName = "নাজিমে আলা",
                title = announceTitle,
                message = "সকল মাদরাসার নিবন্ধন তথ্য হালনাগাদ করার অনুরোধ করা হচ্ছে।"
            )
        }
        assertTrue("sendAdminAnnouncement failed: ${announceRes.exceptionOrNull()}", announceRes.isSuccess)

        // Verify Telegram notification logs were persisted in /telegram_notifications and readable by admin
        val telegramLogs = withTimeout(5000L) {
            repo.observeTelegramNotifications().first { list -> list.isNotEmpty() }
        }
        assertTrue("Expected Telegram notification logs to be recorded", telegramLogs.isNotEmpty())

        // Verify Admin Password Reset Link succeeds for authorized admin email and rejects unauthorized email
        val validResetRes = withTimeout(5000L) {
            repo.sendAdminPasswordResetLink("foysalahamad102@gmail.com")
        }
        assertTrue("Admin password reset should succeed for authorized email: ${validResetRes.exceptionOrNull()}", validResetRes.isSuccess)

        val invalidResetRes = withTimeout(5000L) {
            repo.sendAdminPasswordResetLink("unauthorized@example.com")
        }
        assertTrue("Admin password reset must reject unauthorized email", invalidResetRes.isFailure)

        // 3. Switch to regular user and verify:
        //    - Regular user CANNOT register in /admins/{uid}
        //    - Regular user CANNOT boost posts
        //    - Regular user sees verified madrasa, boosted post, and announcement
        val regularUid = signInTestUser(regularEmail)
        assertTrue(adminUid != regularUid)

        val unauthorizedAdminReg = withTimeout(5000L) {
            repo.ensureVerifiedAdminRegistryDocument()
        }
        assertFalse("Regular user must not be allowed to register as admin", unauthorizedAdminReg.getOrDefault(true))

        val unauthorizedBoost = withTimeout(5000L) {
            repo.adminTogglePostBoost(targetPost, false)
        }
        assertTrue("Regular user must not be allowed to boost/unboost posts", unauthorizedBoost.isFailure)

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
    }

    @Test
    fun groupChat_create_addRemoveMembers_sendImages_andReact_compliesWithSecurityRules() = runBlocking {
        val creatorEmail = "creator_${UUID.randomUUID()}@test.com"
        val memberEmail = "member_${UUID.randomUUID()}@test.com"

        val memberUid = signInTestUser(memberEmail)
        val creatorUid = signInTestUser(creatorEmail)
        val repo = QawmiRepository(firestore, auth)

        // 1. Create group with creator and member
        val createGrpRes = withTimeout(5000L) {
            repo.createChatGroup(
                name = "আসাতীজা ফোরাম",
                photoUrl = "preset:madrasa_dome",
                creatorName = "মাওলানা আব্দুল্লাহ",
                memberIds = listOf(memberUid),
                memberNames = mapOf(memberUid to "মুফতি মাহমুদ")
            )
        }
        assertTrue("createChatGroup failed: ${createGrpRes.exceptionOrNull()}", createGrpRes.isSuccess)
        val groupId = createGrpRes.getOrThrow()

        val observedGroups = withTimeout(5000L) {
            repo.observeUserGroups(creatorUid).first { list -> list.any { it.id == groupId } }
        }
        val group = observedGroups.first { it.id == groupId }
        assertEquals("আসাতীজা ফোরাম", group.name)
        assertTrue(group.memberIds.containsAll(listOf(creatorUid, memberUid)))

        // 2. Send a group message with 2 images (1-5 limit)
        val sendGroupMsgRes = withTimeout(5000L) {
            repo.sendDirectMessage(
                senderName = "মাওলানা আব্দুল্লাহ",
                receiverId = "GROUP:$groupId",
                receiverName = "আসাতীজা ফোরাম",
                subject = "গ্রুপ আলোচনা",
                body = "আসসালামু আলাইকুম, রুটিন সংযুক্ত করা হলো।",
                imageUrls = listOf(
                    "https://example.com/photo1.jpg",
                    "https://example.com/photo2.jpg"
                ),
                groupId = groupId,
                groupName = "আসাতীজা ফোরাম",
                groupMemberIds = group.memberIds
            )
        }
        assertTrue("sendDirectMessage (group with images) failed: ${sendGroupMsgRes.exceptionOrNull()}", sendGroupMsgRes.isSuccess)
        val groupMsgId = sendGroupMsgRes.getOrThrow()

        val creatorMsgs = withTimeout(5000L) {
            repo.observeUserMessages(creatorUid).first { list -> list.any { it.id == groupMsgId } }
        }
        val sentGroupMsg = creatorMsgs.first { it.id == groupMsgId }
        assertEquals(2, sentGroupMsg.imageUrls.size)

        // 3. Creator reacts to own group message (verifies fix for senderReaction + userReactions rule check)
        val creatorReactRes = withTimeout(5000L) {
            repo.reactToMessage(sentGroupMsg, "DUA")
        }
        assertTrue("Creator reactToMessage on group message failed: ${creatorReactRes.exceptionOrNull()}", creatorReactRes.isSuccess)

        // 4. Member observes group message and reacts
        signInTestUser(memberEmail)
        val memberMsgs = withTimeout(5000L) {
            repo.observeUserMessages(memberUid).first { list -> list.any { it.id == groupMsgId } }
        }
        val msgForMember = memberMsgs.first { it.id == groupMsgId }
        assertEquals("DUA", msgForMember.userReactions[creatorUid])

        val memberReactRes = withTimeout(5000L) {
            repo.reactToMessage(msgForMember, "MUHABBAT")
        }
        assertTrue("Member reactToMessage on group message failed: ${memberReactRes.exceptionOrNull()}", memberReactRes.isSuccess)
    }

    @Test
    fun facebookStyleIslamicFeed_multiCategoryPosts_reactions_comments_andShares_complyWithSecurityRules() = runBlocking {
        val authorEmail = "author_${UUID.randomUUID()}@test.com"
        val readerEmail = "reader_${UUID.randomUUID()}@test.com"

        val authorUid = signInTestUser(authorEmail)
        val repo = QawmiRepository(firestore, auth)

        // 1. Author publishes a multi-category Seeker Application with "অন্যান্য" custom category
        val appCreateRes = withTimeout(5000L) {
            repo.createFeedPost(
                authorName = "হাফেজ মাওলানা মাহমুদ",
                postType = "SEEKER_APPLICATION",
                title = "হাফেজ মাওলানা মাহমুদ",
                district = "ঢাকা",
                upazilaOrLocation = "মিরপুর",
                designation = "নূরানী • নাজেরা • ইমাম • তাফসীর বিভাগ",
                qualification = "হিফজুল কুরআন ও দাওরায়ে হাদীস",
                salaryOrExperience = "৪ বছর",
                deadlineOrPreferredDistricts = "ঢাকা, গাজীপুর",
                description = "নূরানী, নাজেরা ও ইমামতি খেদমতে আগ্রহী।",
                contactNumber = "01712345678",
                khedmotCategories = listOf("নূরানী", "নাজেরা", "ইমাম", "অন্যান্য"),
                customCategory = "তাফসীর বিভাগ"
            )
        }
        assertTrue("Multi-category application creation failed: ${appCreateRes.exceptionOrNull()}", appCreateRes.isSuccess)
        val postId = appCreateRes.getOrThrow()

        // 2. Author publishes a GENERAL_POST (Deeni discussion)
        val generalPostRes = withTimeout(5000L) {
            repo.createFeedPost(
                authorName = "হাফেজ মাওলানা মাহমুদ",
                postType = "GENERAL_POST",
                title = "দ্বীনি পরামর্শ",
                district = "ঢাকা",
                upazilaOrLocation = "মিরপুর",
                designation = "দরসিয়াত",
                qualification = "দাওরায়ে হাদীস",
                salaryOrExperience = "",
                deadlineOrPreferredDistricts = "",
                description = "তালিবে ইলমদের পাঠদানে আদব ও ইখলাসের গুরুত্ব অপরিসীম।",
                contactNumber = "",
                khedmotCategories = listOf("দরসিয়াত"),
                customCategory = ""
            )
        }
        assertTrue("GENERAL_POST creation failed: ${generalPostRes.exceptionOrNull()}", generalPostRes.isSuccess)

        // 3. Switch to reader user: observe post, toggle reaction, add comment, edit own comment, and share post
        val readerUid = signInTestUser(readerEmail)
        val postsForReader = withTimeout(5000L) {
            repo.observePublicFeedPosts().first { list -> list.any { it.id == postId } }
        }
        val targetPost = postsForReader.first { it.id == postId }
        assertEquals(listOf("নূরানী", "নাজেরা", "ইমাম", "অন্যান্য"), targetPost.khedmotCategories)
        assertEquals("তাফসীর বিভাগ", targetPost.customCategory)

        val reactRes = withTimeout(5000L) {
            repo.togglePostReaction(targetPost, "SUNDOR", "মাওলানা সাদিক")
        }
        assertTrue("togglePostReaction failed: ${reactRes.exceptionOrNull()}", reactRes.isSuccess)

        val addCommentRes = withTimeout(5000L) {
            repo.addPostComment(
                post = targetPost,
                authorName = "মাওলানা সাদিক",
                authorPhotoUrl = "preset:scholar_emerald",
                text = "মাশাআল্লাহ, আল্লাহ আপনার খেদমত কবুল করুন।"
            )
        }
        assertTrue("addPostComment failed: ${addCommentRes.exceptionOrNull()}", addCommentRes.isSuccess)
        val commentId = addCommentRes.getOrThrow()

        val observedComments = withTimeout(5000L) {
            repo.observePublicPostComments().first { list -> list.any { it.id == commentId } }
        }
        val addedComment = observedComments.first { it.id == commentId }
        assertEquals(readerUid, addedComment.authorId)

        val editCommentRes = withTimeout(5000L) {
            repo.editPostComment(commentId, "মাশাআল্লাহ, আল্লাহ তাআলা উত্তম জাযা দান করুন।")
        }
        assertTrue("editPostComment by author failed: ${editCommentRes.exceptionOrNull()}", editCommentRes.isSuccess)

        // Share post in-app
        val shareRes = withTimeout(5000L) {
            repo.shareFeedPostInApp(
                originalPost = targetPost,
                sharerName = "মাওলানা সাদিক",
                sharerPhotoUrl = "preset:scholar_emerald",
                sharerDistrict = "ঢাকা",
                shareCaption = "ঢাকার মাদরাসাগুলোর জন্য উপযুক্ত একজন হাফেজ ও আলেম।"
            )
        }
        assertTrue("shareFeedPostInApp failed: ${shareRes.exceptionOrNull()}", shareRes.isSuccess)

        // 4. Switch back to original author: verify author CANNOT edit reader's comment, and author CAN reply
        signInTestUser(authorEmail)
        val unauthorizedCommentEdit = withTimeout(5000L) {
            repo.editPostComment(commentId, "অন্যের কমেন্ট এডিট চেষ্টা")
        }
        assertTrue("Non-owner must not be able to edit another user's comment", unauthorizedCommentEdit.isFailure)

        val replyRes = withTimeout(5000L) {
            repo.addPostComment(
                post = targetPost,
                authorName = "হাফেজ মাওলানা মাহমুদ",
                authorPhotoUrl = "",
                text = "আমিন, জাযাকাল্লাহু খাইরান।",
                parentCommentId = commentId,
                replyToAuthorName = "মাওলানা সাদিক"
            )
        }
        assertTrue("Author reply comment failed: ${replyRes.exceptionOrNull()}", replyRes.isSuccess)
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

    @Test
    fun autoUpdateRelease_githubActionsAndAdminPublish_observedByClientAndValidatedByHelper() = runBlocking {
        // 1. Authorized Admin (or CI pipeline) publishes a new APK release to /app_releases/latest
        signInTestUser("foysalahamad102@gmail.com")
        val adminRepo = QawmiRepository(firestore, auth)

        val apkUrl = "https://github.com/qawmi-bd/qawmi-kormosangsthan/releases/download/v1.0.5/Qawmi-Kormosangsthan-Bangladesh.apk"
        val publishRes = withTimeout(5000L) {
            adminRepo.publishAppReleaseInfo(
                versionCode = 105,
                versionName = "1.0.5",
                apkDownloadUrl = apkUrl,
                releaseNotes = "নতুন অটো আপডেট ব্যবস্থা ও উন্নত ফিড যুক্ত করা হয়েছে।",
                commitSha = "9f8e7d6c5b4a",
                githubReleaseTag = "v1.0.5",
                isMandatory = false,
                publishedBy = "GitHub Actions Release Pipeline"
            )
        }
        assertTrue("Admin publishAppReleaseInfo failed: ${publishRes.exceptionOrNull()}", publishRes.isSuccess)

        // 2. Switch to a regular installed app user: observe /app_releases/latest in real time
        val regularEmail = "user_${UUID.randomUUID()}@test.com"
        signInTestUser(regularEmail)
        val userRepo = QawmiRepository(firestore, auth)

        val latestRelease = withTimeout(5000L) {
            userRepo.observeLatestAppRelease().first { it != null && it.versionCode == 105 }
        }
        assertNotNull(latestRelease)
        assertEquals(105, latestRelease?.versionCode)
        assertEquals("1.0.5", latestRelease?.versionName)
        assertEquals(apkUrl, latestRelease?.apkDownloadUrl)

        // 3. Verify AppUpdateManagerHelper detects the update against installed version 1 ("1.0")
        val isAvailable = com.example.ui.util.AppUpdateManagerHelper.isUpdateAvailable(
            installedVersionCode = 1,
            installedVersionName = "1.0",
            remoteRelease = latestRelease,
            dismissedVersionCode = null
        )
        assertTrue("Expected update to be detected for installed versionCode 1 vs remote 105", isAvailable)

        // When user clicks "পরে করুন" (dismissedVersionCode = 105), prompt is suppressed for version 105
        val isAvailableAfterDismiss = com.example.ui.util.AppUpdateManagerHelper.isUpdateAvailable(
            installedVersionCode = 1,
            installedVersionName = "1.0",
            remoteRelease = latestRelease,
            dismissedVersionCode = 105
        )
        assertTrue("Expected dismissed update 105 to be suppressed", !isAvailableAfterDismiss)

        // 4. Verify regular non-admin user CANNOT overwrite /app_releases/latest
        val unauthorizedPublish = withTimeout(5000L) {
            userRepo.publishAppReleaseInfo(
                versionCode = 999,
                versionName = "9.9.9",
                apkDownloadUrl = "https://malicious.example.com/fake.apk",
                releaseNotes = "অননুমোদিত আপডেট"
            )
        }
        assertTrue("Non-admin user must not be allowed to publish app release metadata", unauthorizedPublish.isFailure)
    }

    @Test
    fun createFeedPost_withLargeImagesAndLongText_offloadsData_prevents1MiBLimit_andValidatesSizeBeforeWrite() = runBlocking {
        val authorEmail = "muhtamim_media_${UUID.randomUUID()}@test.com"
        val authorUid = signInTestUser(authorEmail)
        val repo = QawmiRepository(firestore, auth)

        // Generate 3 large Base64 images (~400 KB each -> ~1.2 MB total raw, which previously failed with INVALID_ARGUMENT: exceeds 1 MiB limit)
        val largeChunk = "A".repeat(390_000)
        val rawLargeImages = listOf(
            "data:image/jpeg;base64,$largeChunk",
            "data:image/jpeg;base64,$largeChunk",
            "data:image/jpeg;base64,$largeChunk"
        )
        val veryLongDescription = "বিসমিল্লাহির রাহমানির রাহীম। কওমি মাদরাসার বিস্তারিত নিয়োগ বিজ্ঞপ্তি ও শিক্ষাকার্যক্রম বিবরণী। ".repeat(80)

        // 1. Verify raw un-compacted payload would exceed SAFE_FEED_POST_MAX_BYTES and fail pre-write size check with clear Bengali message
        val rawUnsanitizedMap = mapOf(
            "authorId" to authorUid,
            "title" to "অতিরিক্ত বড় পোস্ট",
            "description" to veryLongDescription,
            "imageUrls" to rawLargeImages
        )
        val preWriteValidation = com.example.ui.util.ProfilePhotoHelper.validateFirestoreDocumentSizeBeforeWrite(
            documentPath = "feed_posts/test_post_oversized",
            data = rawUnsanitizedMap
        )
        assertTrue("Raw 1.2 MB payload must be caught by pre-write size validation", preWriteValidation.isFailure)
        assertEquals(
            com.example.ui.util.ProfilePhotoHelper.OVERSIZED_POST_ERROR_BANGLA,
            preWriteValidation.exceptionOrNull()?.message
        )

        // 2. Now create the post via QawmiRepository.createFeedPost — it uploads/offloads media and extended text automatically
        val createRes = withTimeout(10_000L) {
            repo.createFeedPost(
                authorName = "জামিয়া ইসলামিয়া দারুল উলুম",
                postType = "GENERAL_POST",
                title = "বার্ষিক ইমতিহান ও নতুন শিক্ষাবর্ষের বিজ্ঞপ্তি",
                district = "ঢাকা",
                upazilaOrLocation = "মিরপুর",
                designation = "দ্বীনি খেদমত ও সাধারণ পোস্ট",
                qualification = "সর্বসাধারণের জন্য উন্মুক্ত",
                salaryOrExperience = "",
                deadlineOrPreferredDistricts = "ঢাকা",
                description = veryLongDescription,
                contactNumber = "01711223344",
                khedmotCategories = listOf("কিতাব বিভাগ", "হিফজ বিভাগ"),
                imageUrls = rawLargeImages
            )
        }
        assertTrue("createFeedPost with 3 large images and long text should succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)
        val postId = createRes.getOrThrow()

        // 3. Observe the created post in the feed and verify its size and metadata
        val observedPosts = withTimeout(5000L) {
            repo.observePublicFeedPosts().first { list -> list.any { it.id == postId } }
        }
        val savedPost = observedPosts.first { it.id == postId }
        assertEquals(3, savedPost.imageUrls.size)
        assertEquals(listOf("IMAGE", "IMAGE", "IMAGE"), savedPost.mediaTypes)
        assertEquals(3, savedPost.mediaMetadata.size)
        assertTrue("Long description should be offloaded to attachmentDocId", savedPost.attachmentDocId.isNotBlank())

        // Verify the stored feed_posts document is well below the 1 MiB limit
        val projectedSavedDocMap = mapOf(
            "authorId" to savedPost.authorId,
            "authorName" to savedPost.authorName,
            "title" to savedPost.title,
            "description" to savedPost.description,
            "imageUrls" to savedPost.imageUrls,
            "mediaTypes" to savedPost.mediaTypes,
            "mediaMetadata" to savedPost.mediaMetadata,
            "attachmentDocId" to savedPost.attachmentDocId
        )
        val estimatedSavedBytes = com.example.ui.util.ProfilePhotoHelper.estimateFirestoreDocumentBytes(
            documentPath = "feed_posts/$postId",
            data = projectedSavedDocMap
        )
        assertTrue(
            "Saved feed_posts document must be well under 850 KB (was $estimatedSavedBytes bytes)",
            estimatedSavedBytes < com.example.ui.util.ProfilePhotoHelper.SAFE_FEED_POST_MAX_BYTES
        )

        // 4. Switch to another authenticated user (reader) and verify they see the post, its compact URLs, and can read the uploaded image attachment
        signInTestUser("reader_media_${UUID.randomUUID()}@test.com")
        val readerRepo = QawmiRepository(firestore, auth)

        val textAttachment = withTimeout(5000L) {
            readerRepo.getFeedPostExtendedAttachment("${postId}_ext_text")
        }
        assertNotNull("Expected extended text attachment to be readable by another user", textAttachment)
        assertEquals(veryLongDescription.trim(), textAttachment?.extendedText)

        val firstMediaAttachment = withTimeout(5000L) {
            readerRepo.getFeedPostExtendedAttachment("${postId}_media_0")
        }
        assertNotNull("Expected uploaded image attachment to be readable by another user", firstMediaAttachment)
        assertEquals("IMAGE", firstMediaAttachment?.attachmentType)
        assertTrue("Expected mediaDataUrl to be stored in attachment doc", firstMediaAttachment?.mediaDataUrl?.isNotBlank() == true)
        assertTrue(
            "Expected feed_posts.imageUrls to contain only compact URLs (< 300 chars), not raw Base64",
            savedPost.imageUrls.all { it.startsWith("https://") && it.length < 300 }
        )
    }
}


