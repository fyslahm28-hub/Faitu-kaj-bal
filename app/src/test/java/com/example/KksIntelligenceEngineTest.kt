package com.example

import com.example.data.model.FeedPost
import com.example.data.model.IntelligenceConfig
import com.example.data.model.PostIntelligenceMetrics
import com.example.data.model.UserAffinityState
import com.example.data.model.UserProfile
import com.example.ui.util.KksIntelligenceEngine
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class KksIntelligenceEngineTest {

    private val nowMs = 1_730_448_000_000L // Fixed reference timestamp

    private fun tsHoursAgo(hours: Double): Timestamp {
        val ms = nowMs - (hours * 3_600_000.0).toLong()
        return Timestamp(Date(ms))
    }

    @Test
    fun `rankFeedPosts prioritizes matching qualification, designation, and district`() {
        val user = UserProfile(
            userId = "u_mudarris_1",
            fullName = "মাওলানা আব্দুল্লাহ",
            district = "ঢাকা",
            preferredDistricts = "ঢাকা, গাজীপুর",
            designation = "মুহাদ্দিস ও ফিকহ শিক্ষক",
            qualification = "দাওরায়ে হাদিস ও ইফতা",
            experience = "৫ বছর"
        )

        val matchedPost = FeedPost(
            id = "post_matched",
            authorId = "m_1",
            postType = "JOB_CIRCULAR",
            title = "জামিয়া রাহমানিয়া ঢাকা",
            district = "ঢাকা",
            designation = "মুহাদ্দিস ও ফিকহ উস্তায",
            qualification = "দাওরায়ে হাদিস / ইফতা",
            salaryOrExperience = "৩ বছর অভিজ্ঞতা",
            createdAt = tsHoursAgo(6.0)
        )

        val unmatchedPost = FeedPost(
            id = "post_unmatched",
            authorId = "m_2",
            postType = "JOB_CIRCULAR",
            title = "নূরানী মাদরাসা সিলেট",
            district = "সিলেট",
            designation = "নূরানী মুয়াল্লিম",
            qualification = "নূরানী মুয়াল্লিম প্রশিক্ষণ",
            salaryOrExperience = "১০ বছর অভিজ্ঞতা",
            createdAt = tsHoursAgo(6.0)
        )

        val ranked = KksIntelligenceEngine.rankFeedPosts(
            posts = listOf(unmatchedPost, matchedPost),
            userProfile = user,
            affinityState = null,
            metricsMap = emptyMap(),
            config = IntelligenceConfig(),
            nowMillis = nowMs
        )

        assertEquals(2, ranked.size)
        assertEquals("post_matched", ranked.first().post.id)
        assertTrue(ranked.first().qualificationScore > ranked.last().qualificationScore)
        assertTrue(ranked.first().locationScore > ranked.last().locationScore)
    }

    @Test
    fun `anti-fraud prevents high reaction or low-dwell view spikes from becoming trending without conversion`() {
        val fakeViralPost = FeedPost(
            id = "post_fake_viral",
            authorId = "m_spam",
            postType = "JOB_CIRCULAR",
            title = "সন্দেহজনক বিজ্ঞপ্তি",
            district = "ঢাকা",
            designation = "মুদাররিস",
            qualification = "দাওরায়ে হাদিস",
            userReactions = (1..40).associate { "bot_$it" to "LOVE" },
            createdAt = tsHoursAgo(2.0)
        )

        val fakeMetrics = PostIntelligenceMetrics(
            id = "post_fake_viral",
            postId = "post_fake_viral",
            uniqueViewCount = 80,
            uniqueClickCount = 0,
            applyCount = 0,
            bookmarkCount = 0,
            shareCount = 0,
            totalDwellTimeMs = 80 * 900L // Avg 900ms (< 1600ms rapid scroll / bot pattern)
        )

        val genuinePost = FeedPost(
            id = "post_genuine",
            authorId = "m_real",
            postType = "JOB_CIRCULAR",
            title = "বাস্তব নিয়োগ বিজ্ঞপ্তি",
            district = "ঢাকা",
            designation = "মুহাদ্দিস",
            qualification = "দাওরায়ে হাদিস",
            userReactions = mapOf("u1" to "LIKE", "u2" to "LIKE"),
            createdAt = tsHoursAgo(3.0)
        )

        val genuineMetrics = PostIntelligenceMetrics(
            id = "post_genuine",
            postId = "post_genuine",
            uniqueViewCount = 22,
            uniqueClickCount = 9,
            applyCount = 3,
            bookmarkCount = 4,
            shareCount = 2,
            totalDwellTimeMs = 22 * 8_500L
        )

        val config = IntelligenceConfig()
        val fakeEval = KksIntelligenceEngine.evaluateAnomalyAndFraud(fakeViralPost, fakeMetrics, config)
        assertTrue("Fake post should be flagged for anomalous engagement", fakeEval.isFlagged)
        assertTrue("Fake post anomaly score should be >= 58", fakeEval.anomalyScore >= 58.0)

        val ranked = KksIntelligenceEngine.rankFeedPosts(
            posts = listOf(fakeViralPost, genuinePost),
            userProfile = null,
            affinityState = null,
            metricsMap = mapOf(
                "post_fake_viral" to fakeMetrics,
                "post_genuine" to genuineMetrics
            ),
            config = config,
            nowMillis = nowMs
        )

        val trending = KksIntelligenceEngine.selectTrendingPosts(ranked, limit = 5)
        assertTrue("Genuine post with real applications should be trending", trending.any { it.post.id == "post_genuine" })
        assertFalse("Fake viral post with zero conversions and high anomaly must NOT be trending", trending.any { it.post.id == "post_fake_viral" })
    }

    @Test
    fun `adaptive learning updates category and district affinity weights with time decay`() {
        val post = FeedPost(
            id = "p_ifta",
            postType = "JOB_CIRCULAR",
            title = "দারুল ইফতা নিয়োগ",
            district = "চট্টগ্রাম",
            designation = "মুফতি ও ফতোয়া বিভাগ",
            qualification = "ইফতা",
            khedmotCategories = listOf("দরসিয়াত")
        )

        val stateAfterApply = KksIntelligenceEngine.computeUpdatedAffinityState(
            currentState = null,
            userId = "u_learner",
            post = post,
            eventType = "APPLY",
            dwellTimeMs = 12_000L,
            nowMillis = nowMs
        )

        assertEquals(1, stateAfterApply.totalInteractions)
        assertTrue((stateAfterApply.districtWeights["চট্টগ্রাম"] ?: 0.0) >= 4.0)
        assertTrue(stateAfterApply.categoryWeights.values.any { it >= 5.0 })
    }

    @Test
    fun `smart notification evaluator filters out low-relevance posts and approves high-relevance matches`() {
        val targetUser = UserProfile(
            userId = "u_hifz",
            fullName = "হাফেজ কারী মাহমুদ",
            district = "কুমিল্লা",
            preferredDistricts = "কুমিল্লা, চাঁদপুর",
            designation = "হিফজ ও কিরাত শিক্ষক",
            qualification = "হিফজুল কুরআন ও কিরাত"
        )

        val relevantCircular = FeedPost(
            id = "p_hifz_cumilla",
            authorId = "other_author",
            postType = "JOB_CIRCULAR",
            title = "কুমিল্লা তাহফিজুল কুরআন মাদরাসা",
            district = "কুমিল্লা",
            designation = "হিফজ বিভাগের প্রধান শিক্ষক",
            qualification = "হাফেজ ও কিরাত",
            isVerifiedMadrasa = true,
            createdAt = tsHoursAgo(1.0)
        )

        val irrelevantCircular = FeedPost(
            id = "p_english_rangpur",
            authorId = "other_author",
            postType = "JOB_CIRCULAR",
            title = "রংপুর জেনারেল একাডেমি",
            district = "রংপুর",
            designation = "হিসাবরক্ষক",
            qualification = "স্নাতক",
            isVerifiedMadrasa = false,
            createdAt = tsHoursAgo(48.0)
        )

        val config = IntelligenceConfig(smartNotificationThreshold = 62.0)

        val relevantDecision = KksIntelligenceEngine.evaluateSmartNotificationCandidate(
            post = relevantCircular,
            recipientProfile = targetUser,
            affinityState = null,
            config = config
        )
        val irrelevantDecision = KksIntelligenceEngine.evaluateSmartNotificationCandidate(
            post = irrelevantCircular,
            recipientProfile = targetUser,
            affinityState = null,
            config = config
        )

        assertTrue("High-matching Hifz circular in Cumilla should trigger Smart Notification", relevantDecision.shouldNotify)
        assertFalse("Unrelated circular in Rangpur should NOT trigger Smart Notification", irrelevantDecision.shouldNotify)
    }

    @Test
    fun `FirestoreConnectionMonitor handles offline cache fallback, Wi-Fi to Cellular switch, and pending write flush`() {
        val monitor = com.example.data.repository.FirestoreConnectionMonitor
        monitor.resetForTesting()

        // 1. Initial state on Wi-Fi
        monitor.onNetworkTypeChanged("WIFI", available = true)
        assertTrue(monitor.isNetworkAvailable.value)
        assertEquals("WIFI", monitor.activeNetworkType.value)

        // 2. Internet Disconnect -> immediately transitions to offline + cache-serving mode
        monitor.onNetworkTypeChanged("NONE", available = false)
        assertFalse(monitor.isNetworkAvailable.value)
        assertFalse(monitor.isServerConnected.value)
        assertTrue(monitor.isServingFromCache.value)

        // 3. Cached snapshot with pending offline writes arrives while offline
        monitor.onSnapshotReceived(path = "feed_posts", isFromCache = true, hasPendingWrites = true)
        assertTrue(monitor.hasPendingWrites.value)
        assertTrue(monitor.isServingFromCache.value)
        assertFalse(monitor.isServerConnected.value)

        // 4. Internet Reconnect on Wi-Fi -> triggers automatic recovery generation
        val genBeforeReconnect = monitor.recoveryGeneration.value
        monitor.onNetworkTypeChanged("WIFI", available = true)
        assertTrue(monitor.isNetworkAvailable.value)
        assertTrue(monitor.isReconnecting.value)
        assertTrue(monitor.recoveryGeneration.value > genBeforeReconnect)

        // 5. Server snapshot arrives -> clears cache-only and reconnecting flags
        monitor.onSnapshotReceived(path = "feed_posts", isFromCache = false, hasPendingWrites = false)
        assertTrue(monitor.isServerConnected.value)
        assertFalse(monitor.isServingFromCache.value)
        assertFalse(monitor.isReconnecting.value)
        assertFalse(monitor.hasPendingWrites.value)

        // 6. Wi-Fi -> Mobile Data (CELLULAR) live switch while online triggers immediate reconnect generation
        val genBeforeSwitch = monitor.recoveryGeneration.value
        monitor.onNetworkTypeChanged("CELLULAR", available = true)
        assertEquals("CELLULAR", monitor.activeNetworkType.value)
        assertTrue(monitor.recoveryGeneration.value > genBeforeSwitch)

        // 7. Startup & foreground full sync indicator and per-path server confirmation deduplication
        assertFalse(monitor.isSyncing.value)
        monitor.onFullSyncStarted("APP_STARTUP")
        assertTrue(monitor.isSyncing.value)
        assertFalse(monitor.hasRecentServerConfirmation("notifications/direct/u_101", withinMs = 1_800L))
        monitor.onSnapshotReceived(path = "notifications/direct/u_101", isFromCache = false, hasPendingWrites = false)
        assertTrue(monitor.hasRecentServerConfirmation("notifications/direct/u_101", withinMs = 1_800L))
        monitor.onFullSyncFinished()
        assertFalse(monitor.isSyncing.value)

        monitor.resetForTesting()
    }
}

