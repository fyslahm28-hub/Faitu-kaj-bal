package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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
    fun createFeedPost_andObservePublicFeed_succeedsForAuthenticatedUsers() = runBlocking {
        signInTestUser("muhtamim_${UUID.randomUUID()}@test.com")
        val repository = QawmiRepository(firestore, auth)

        val uniqueTitle = "দারুল উলুম মাদরাসা ${UUID.randomUUID()}"
        val createResult = withTimeout(5000L) {
            repository.createFeedPost(
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
        assertTrue(createResult.isSuccess)
        val postId = createResult.getOrThrow()

        // Another user observes public feed
        signInTestUser("khadim_${UUID.randomUUID()}@test.com")
        val khadimRepo = QawmiRepository(firestore, auth)
        val posts = withTimeout(4000L) {
            khadimRepo.observePublicFeedPosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId && it.title == uniqueTitle })
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
    fun sendDirectMessage_deliversToReceiver_andBlocksThirdParty() = runBlocking {
        val receiverUid = signInTestUser("receiver_${UUID.randomUUID()}@test.com")

        signInTestUser("sender_msg_${UUID.randomUUID()}@test.com")
        val senderRepo = QawmiRepository(firestore, auth)
        val sendRes = withTimeout(5000L) {
            senderRepo.sendDirectMessage(
                senderName = "মাওলানা রফিকুল ইসলাম",
                receiverId = receiverUid,
                receiverName = "দারুল উলুম মাদরাসা",
                subject = "খেদমতের আবেদন প্রসঙ্গে",
                body = "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ, আমি খেদমতে আগ্রহী।"
            )
        }
        assertTrue(sendRes.isSuccess)
    }
}
