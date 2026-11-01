package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * 1. Global KKS Intelligence Engine Configuration (`/intelligence_config/global_weights`).
 * Controlled exclusively by verified Admins via the Admin Intelligence Control Center.
 */
@IgnoreExtraProperties
data class IntelligenceConfig(
    @DocumentId val id: String = GLOBAL_CONFIG_DOC_ID,
    val configId: String = GLOBAL_CONFIG_DOC_ID,
    val skillMatchWeight: Double = 32.0,
    val locationMatchWeight: Double = 22.0,
    val experienceWeight: Double = 12.0,
    val freshnessHalfLifeHours: Double = 48.0,
    val trendingWeight: Double = 14.0,
    val verifiedMadrasaBonus: Double = 12.0,
    val newPostBoostHours: Double = 18.0,
    val minDwellTimeMs: Long = 2_500L,
    val fraudPenaltyWeight: Double = 35.0,
    val smartNotificationThreshold: Double = 58.0,
    @get:PropertyName("isSmartRankingEnabled")
    val isSmartRankingEnabled: Boolean = true,
    @get:PropertyName("isTrendingEnabled")
    val isTrendingEnabled: Boolean = true,
    @get:PropertyName("isFraudDetectionEnabled")
    val isFraudDetectionEnabled: Boolean = true,
    @get:PropertyName("isSmartNotificationEnabled")
    val isSmartNotificationEnabled: Boolean = true,
    val updatedBy: String = "KKS System",
    val updatedAt: Timestamp? = null
) {
    companion object {
        const val GLOBAL_CONFIG_DOC_ID = "global_weights"
    }
}

/**
 * 2. Post-level Aggregated Intelligence & Anti-Fraud Telemetry (`/post_intelligence_metrics/{postId}`).
 * Stored separately from `/feed_posts/{postId}` to avoid write contention and document bloat.
 */
@IgnoreExtraProperties
data class PostIntelligenceMetrics(
    @DocumentId val id: String = "",
    val postId: String = "",
    val uniqueViewCount: Int = 0,
    val uniqueClickCount: Int = 0,
    val applyCount: Int = 0,
    val bookmarkCount: Int = 0,
    val shareCount: Int = 0,
    val totalDwellTimeMs: Long = 0L,
    val trendingScore: Double = 0.0,
    val qualityScore: Double = 50.0,
    val anomalyScore: Double = 0.0,
    @get:PropertyName("isFlagged")
    val isFlagged: Boolean = false,
    val flagReason: String = "",
    val lastInteractionAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val averageDwellTimeMs: Long
        get() = if (uniqueViewCount > 0) (totalDwellTimeMs / uniqueViewCount) else 0L

    val clickThroughRatePercent: Double
        get() = if (uniqueViewCount > 0) {
            ((uniqueClickCount.toDouble() / uniqueViewCount.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

    val applicationConversionPercent: Double
        get() = if (uniqueClickCount > 0) {
            ((applyCount.toDouble() / uniqueClickCount.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else if (uniqueViewCount > 0) {
            ((applyCount.toDouble() / uniqueViewCount.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }
}

/**
 * 3. Deterministic Engagement Deduplication Receipt
 * (`/post_intelligence_metrics/{postId}/engagement_receipts/{uid}_{eventType}`).
 * Guarantees at the Firestore Security Rules level that a single user can only increment
 * a unique counter (`VIEW`, `CLICK`, `APPLY`, `BOOKMARK`, `SHARE`) once per post.
 */
@IgnoreExtraProperties
data class EngagementReceipt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val eventType: String = EVENT_VIEW, // VIEW | CLICK | APPLY | BOOKMARK | SHARE
    val dwellTimeMs: Long = 0L,
    val createdAt: Timestamp? = null
) {
    companion object {
        const val EVENT_VIEW = "VIEW"
        const val EVENT_CLICK = "CLICK"
        const val EVENT_APPLY = "APPLY"
        const val EVENT_BOOKMARK = "BOOKMARK"
        const val EVENT_SHARE = "SHARE"

        val VALID_EVENT_TYPES = setOf(
            EVENT_VIEW,
            EVENT_CLICK,
            EVENT_APPLY,
            EVENT_BOOKMARK,
            EVENT_SHARE
        )

        fun buildDeterministicReceiptId(userId: String, eventType: String): String {
            return "${userId.trim()}_${eventType.trim().uppercase()}"
        }
    }
}

/**
 * 4. Per-User Adaptive Learning & Affinity State (`/users/{userId}/intelligence_state/affinity`).
 * Tracks exponentially time-decayed category and district preferences learned from real interactions.
 */
@IgnoreExtraProperties
data class UserAffinityState(
    @DocumentId val id: String = AFFINITY_DOC_ID,
    val userId: String = "",
    val categoryWeights: Map<String, Double> = emptyMap(),
    val districtWeights: Map<String, Double> = emptyMap(),
    val totalInteractions: Int = 0,
    val lastEventAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        const val AFFINITY_DOC_ID = "affinity"
    }
}

/**
 * 5. Scored & Explained Feed Post returned by KksIntelligenceEngine.
 */
data class ScoredFeedPost(
    val post: FeedPost,
    val finalScore: Double,
    val matchPercentage: Int,
    val qualificationScore: Double,
    val categoryScore: Double,
    val locationScore: Double,
    val experienceScore: Double,
    val userAffinityScore: Double,
    val freshnessScore: Double,
    val trendingVelocityScore: Double,
    val verifiedTrustBonus: Double,
    val fraudPenalty: Double,
    val isTrending: Boolean,
    val isRecommendedForUser: Boolean,
    val isNewFreshBoosted: Boolean,
    val isAnomalyFlagged: Boolean,
    val matchReasonBangla: String = ""
)

/**
 * 6. ML-Ready Feature Vector Representation for Future On-Device / Cloud Model Inference.
 */
data class KksFeatureVector(
    val qualificationMatchNorm: Float, // [0.0, 1.0]
    val categoryMatchNorm: Float,      // [0.0, 1.0]
    val districtMatchNorm: Float,      // [0.0, 1.0]
    val experienceMatchNorm: Float,    // [0.0, 1.0]
    val userAffinityNorm: Float,       // [0.0, 1.0]
    val freshnessDecayNorm: Float,     // [0.0, 1.0]
    val trendingVelocityNorm: Float,   // [0.0, 1.0]
    val verifiedTrustNorm: Float,      // [0.0, 1.0]
    val anomalyRiskNorm: Float         // [0.0, 1.0]
) {
    fun toFloatArray(): FloatArray = floatArrayOf(
        qualificationMatchNorm,
        categoryMatchNorm,
        districtMatchNorm,
        experienceMatchNorm,
        userAffinityNorm,
        freshnessDecayNorm,
        trendingVelocityNorm,
        verifiedTrustNorm,
        anomalyRiskNorm
    )
}

/**
 * Pluggable ML / Heuristic Scorer contract so future TensorFlow Lite or Server ML models
 * can be swapped in without modifying UI or Repository code.
 */
fun interface KksRankingModelScorer {
    fun predictScore(features: KksFeatureVector, config: IntelligenceConfig): Double
}

/**
 * 7. Smart Notification Match Decision.
 */
data class SmartNotificationEvaluation(
    val shouldNotify: Boolean,
    val relevanceScore: Double,
    val matchSummaryBangla: String,
    val matchedQualification: Boolean,
    val matchedDistrict: Boolean,
    val matchedCategory: Boolean
)

/**
 * 8. System-wide Algorithm Performance & Fraud Analytics Summary for Admin Panel.
 */
data class AlgorithmPerformanceSummary(
    val totalTrackedPosts: Int = 0,
    val totalUniqueViews: Int = 0,
    val totalUniqueClicks: Int = 0,
    val totalApplications: Int = 0,
    val totalBookmarks: Int = 0,
    val totalShares: Int = 0,
    val overallCtrPercent: Double = 0.0,
    val overallApplicationConversionPercent: Double = 0.0,
    val shortlistOrAcceptRatePercent: Double = 0.0,
    val averageDwellTimeSeconds: Double = 0.0,
    val activeTrendingPostsCount: Int = 0,
    val flaggedAnomalyPostsCount: Int = 0,
    val topTrendingPosts: List<ScoredFeedPost> = emptyList(),
    val flaggedPosts: List<Pair<FeedPost, PostIntelligenceMetrics>> = emptyList()
)
