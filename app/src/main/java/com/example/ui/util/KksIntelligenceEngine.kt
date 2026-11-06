package com.example.ui.util

import com.example.data.model.AlgorithmPerformanceSummary
import com.example.data.model.EngagementReceipt
import com.example.data.model.FeedPost
import com.example.data.model.IntelligenceConfig
import com.example.data.model.KhedmotCategoryHelper
import com.example.data.model.KhedmotSubmission
import com.example.data.model.KksFeatureVector
import com.example.data.model.KksRankingModelScorer
import com.example.data.model.PostIntelligenceMetrics
import com.example.data.model.ScoredFeedPost
import com.example.data.model.SmartNotificationEvaluation
import com.example.data.model.UserAffinityState
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * KKS Intelligence Engine — Core Ranking, Recommendation, Trending, Anti-Fraud,
 * Adaptive Learning, and Smart Notification Engine for Qawmi Kormosangsthan Bangladesh.
 */
object KksIntelligenceEngine {

    // Canonical Qawmi Qualification Groups for semantic equivalence matching
    private val qualificationHierarchyGroups: List<Pair<Int, List<String>>> = listOf(
        5 to listOf("ইফতা", "মুফতি", "তাখাসসুস", "উলুমুল হাদিস", "তাফসীর", "আদব", "ফতোয়া"),
        4 to listOf("দাওরায়ে হাদিস", "দাওরায়ে হাদিস", "তাকমীল", "তাকমিল", "মাস্টার্স", "মুহাদ্দিস", "শায়খুল হাদিস", "মুহতামিম", "নাজিমে তালিমাত", "দরসিয়াত", "দরসিয়াত", "কিতাব বিভাগ"),
        3 to listOf("মিশকাত", "জালালাইন", "শরহে বেকায়া", "হেদায়া", "ফাযিল", "কামিল", "খতিব", "ইমাম"),
        2 to listOf("হিফয", "হিফজ", "হাফেজ", "হাফেয", "ক্বারী", "কারী", "কিরাত", "তাজবীদ", "নাজেরা"),
        1 to listOf("নূরানী", "নুরানী", "মুআল্লিম", "মুয়াল্লিম", "মক্তব", "মুয়াজ্জিন", "মুয়াজ্জিন", "খাদেম")
    )

    /**
     * Default linear-weighted ML-ready scorer that operates on normalized [0..1] feature vectors
     * and the Admin-configured [IntelligenceConfig]. Can be replaced at runtime by an on-device
     * TFLite or server-trained model scorer.
     */
    val defaultLinearModelScorer: KksRankingModelScorer = KksRankingModelScorer { features, config ->
        val skillComponent = ((features.qualificationMatchNorm * 0.55f) + (features.categoryMatchNorm * 0.45f)) * config.skillMatchWeight
        val locationComponent = features.districtMatchNorm * config.locationMatchWeight
        val experienceComponent = features.experienceMatchNorm * config.experienceWeight
        val affinityComponent = features.userAffinityNorm * 14.0
        val freshnessComponent = features.freshnessDecayNorm * 16.0
        val trendingComponent = if (config.isTrendingEnabled) {
            features.trendingVelocityNorm * config.trendingWeight
        } else {
            0.0
        }
        val trustComponent = features.verifiedTrustNorm * config.verifiedMadrasaBonus
        val penaltyComponent = if (config.isFraudDetectionEnabled) {
            features.anomalyRiskNorm * config.fraudPenaltyWeight
        } else {
            0.0
        }
        (skillComponent + locationComponent + experienceComponent + affinityComponent + freshnessComponent + trendingComponent + trustComponent - penaltyComponent)
            .coerceIn(0.0, 100.0)
    }

    @Volatile
    var activeModelScorer: KksRankingModelScorer = defaultLinearModelScorer

    /**
     * Ranks a list of [FeedPost] items for a given user using the KKS Intelligence Engine.
     * Balances user qualification/category/district relevance, adaptive affinity, freshness boost
     * for new posts, verified madrasa trust, genuine trending velocity, and anti-fraud penalties.
     */
    fun rankFeedPosts(
        posts: List<FeedPost>,
        userProfile: UserProfile?,
        affinityState: UserAffinityState?,
        metricsMap: Map<String, PostIntelligenceMetrics>,
        config: IntelligenceConfig = IntelligenceConfig(),
        nowMillis: Long = System.currentTimeMillis()
    ): List<ScoredFeedPost> {
        if (posts.isEmpty()) return emptyList()

        val scoredList = posts.map { post ->
            scoreSinglePost(
                post = post,
                userProfile = userProfile,
                affinityState = affinityState,
                metrics = metricsMap[post.id],
                config = config,
                nowMillis = nowMillis
            )
        }

        if (!config.isSmartRankingEnabled) {
            // When Smart Ranking is disabled by Admin, keep Admin Boosted posts first, then chronological
            return scoredList.sortedWith(
                compareByDescending<ScoredFeedPost> { it.post.isBoosted }
                    .thenByDescending { it.post.createdAt?.toDate()?.time ?: 0L }
                    .thenBy { it.post.id }
            )
        }

        return scoredList.sortedWith(
            compareByDescending<ScoredFeedPost> { it.post.isBoosted && !it.isAnomalyFlagged }
                .thenByDescending { (it.finalScore * 10.0).roundToInt() }
                .thenByDescending { it.post.createdAt?.toDate()?.time ?: 0L }
                .thenBy { it.post.id }
        )
    }

    /**
     * Returns top personalized recommendations for the user (excluding the user's own posts
     * and excluding any fraud-flagged posts).
     */
    fun selectRecommendedPosts(
        scoredPosts: List<ScoredFeedPost>,
        currentUserId: String,
        limit: Int = 6
    ): List<ScoredFeedPost> {
        return scoredPosts
            .filter { scored ->
                scored.post.authorId != currentUserId &&
                    !scored.isAnomalyFlagged &&
                    (scored.post.postType == "JOB_CIRCULAR" || scored.post.postType == "SEEKER_APPLICATION") &&
                    (scored.isRecommendedForUser || scored.matchPercentage >= 55)
            }
            .sortedByDescending { it.finalScore }
            .take(limit)
    }

    /**
     * Returns genuinely trending posts that pass anti-fraud & conversion quality checks.
     */
    fun selectTrendingPosts(
        scoredPosts: List<ScoredFeedPost>,
        limit: Int = 5
    ): List<ScoredFeedPost> {
        return scoredPosts
            .filter { it.isTrending && !it.isAnomalyFlagged }
            .sortedWith(
                compareByDescending<ScoredFeedPost> { it.trendingVelocityScore }
                    .thenByDescending { it.finalScore }
            )
            .take(limit)
    }

    /**
     * Computes comprehensive multi-factor score & normalized ML feature vector for a single post.
     */
    fun scoreSinglePost(
        post: FeedPost,
        userProfile: UserProfile?,
        affinityState: UserAffinityState?,
        metrics: PostIntelligenceMetrics?,
        config: IntelligenceConfig = IntelligenceConfig(),
        nowMillis: Long = System.currentTimeMillis()
    ): ScoredFeedPost {
        val qualNorm = computeQualificationSimilarity(userProfile, post)
        val catNorm = computeCategoryAndDesignationSimilarity(userProfile, post)
        val distNorm = computeDistrictSimilarity(userProfile, post)
        val expNorm = computeExperienceSimilarity(userProfile, post)
        val affinityNorm = computeUserAffinitySimilarity(affinityState, post, nowMillis)
        val ageHours = computePostAgeHours(post.createdAt, nowMillis)
        val freshnessNorm = computeFreshnessFactor(
            ageHours = ageHours,
            halfLifeHours = config.freshnessHalfLifeHours.coerceAtLeast(4.0),
            newPostBoostHours = config.newPostBoostHours.coerceAtLeast(1.0)
        )

        val anomalyAssessment = evaluateAnomalyAndFraud(post, metrics, config)
        val rawTrendingVelocity = computeTrendingVelocity(post, metrics, ageHours, anomalyAssessment.anomalyRiskNorm)
        val trendingNorm = (rawTrendingVelocity / 25.0).coerceIn(0.0, 1.0).toFloat()

        val trustNorm = when {
            post.isBoosted && post.isVerifiedMadrasa -> 1.0f
            post.isVerifiedMadrasa -> 0.85f
            post.isBoosted -> 0.75f
            else -> 0.0f
        }

        val featureVector = KksFeatureVector(
            qualificationMatchNorm = qualNorm,
            categoryMatchNorm = catNorm,
            districtMatchNorm = distNorm,
            experienceMatchNorm = expNorm,
            userAffinityNorm = affinityNorm,
            freshnessDecayNorm = freshnessNorm,
            trendingVelocityNorm = trendingNorm,
            verifiedTrustNorm = trustNorm,
            anomalyRiskNorm = anomalyAssessment.anomalyRiskNorm
        )

        val qualificationScore = qualNorm * (config.skillMatchWeight * 0.55)
        val categoryScore = catNorm * (config.skillMatchWeight * 0.45)
        val locationScore = distNorm * config.locationMatchWeight
        val experienceScore = expNorm * config.experienceWeight
        val userAffinityScore = affinityNorm * 14.0
        val freshnessScore = freshnessNorm * 16.0
        val trendingScore = if (config.isTrendingEnabled) trendingNorm * config.trendingWeight else 0.0
        val verifiedBonus = trustNorm * config.verifiedMadrasaBonus
        val fraudPenalty = if (config.isFraudDetectionEnabled) {
            anomalyAssessment.anomalyRiskNorm * config.fraudPenaltyWeight
        } else {
            0.0
        }

        // Role complementarity bonus: Muhtamim prefers Seeker Applications; Khadim prefers Job Circulars
        val accountTypeBonus = when {
            userProfile == null -> 0.0
            userProfile.accountType.equals("muhtamim", ignoreCase = true) && post.postType == "SEEKER_APPLICATION" -> 6.0
            userProfile.accountType.equals("khadim", ignoreCase = true) && post.postType == "JOB_CIRCULAR" -> 6.0
            else -> 2.0
        }

        val isNewBoostActive = ageHours <= config.newPostBoostHours && !anomalyAssessment.isFlagged
        val newPostBalanceBonus = if (isNewBoostActive) {
            val remainingRatio = ((config.newPostBoostHours - ageHours) / config.newPostBoostHours.coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
            6.5 * remainingRatio
        } else {
            0.0
        }

        val modelBaseScore = activeModelScorer.predictScore(featureVector, config)
        val finalScore = (modelBaseScore + accountTypeBonus + newPostBalanceBonus).coerceIn(0.0, 100.0)

        // Personal relevance match percentage (independent of age decay)
        val relevanceNumerator =
            (qualNorm * 35.0) + (catNorm * 30.0) + (distNorm * 20.0) + (expNorm * 10.0) + (affinityNorm * 5.0)
        val matchPercentage = relevanceNumerator.roundToInt().coerceIn(15, 99)

        val isGenuineTrending = config.isTrendingEnabled &&
            !anomalyAssessment.isFlagged &&
            rawTrendingVelocity >= 4.2 &&
            hasGenuineConversionSignal(post, metrics)

        val isRecommended = !anomalyAssessment.isFlagged &&
            (qualNorm >= 0.55f || catNorm >= 0.55f || affinityNorm >= 0.5f) &&
            (distNorm >= 0.45f || qualNorm >= 0.75f || catNorm >= 0.75f)

        val matchReasonBangla = buildMatchReasonBangla(
            qualNorm = qualNorm,
            catNorm = catNorm,
            distNorm = distNorm,
            affinityNorm = affinityNorm,
            isTrending = isGenuineTrending,
            isNewFresh = isNewBoostActive,
            post = post
        )

        return ScoredFeedPost(
            post = post,
            finalScore = finalScore,
            matchPercentage = matchPercentage,
            qualificationScore = qualificationScore,
            categoryScore = categoryScore,
            locationScore = locationScore,
            experienceScore = experienceScore,
            userAffinityScore = userAffinityScore,
            freshnessScore = freshnessScore + newPostBalanceBonus,
            trendingVelocityScore = rawTrendingVelocity,
            verifiedTrustBonus = verifiedBonus,
            fraudPenalty = fraudPenalty,
            isTrending = isGenuineTrending,
            isRecommendedForUser = isRecommended,
            isNewFreshBoosted = isNewBoostActive,
            isAnomalyFlagged = anomalyAssessment.isFlagged,
            matchReasonBangla = matchReasonBangla
        )
    }

    /**
     * Evaluates whether a post has genuine high-intent engagement rather than just raw views/likes.
     * Requirement: "শুধু লাইক বা ভিউ বেশি হলেই কোনো পোস্টকে ভাইরাল হিসেবে বিবেচনা করবে না।"
     */
    fun hasGenuineConversionSignal(
        post: FeedPost,
        metrics: PostIntelligenceMetrics?
    ): Boolean {
        val m = metrics ?: return (post.commentCount >= 2 || post.shareCount >= 1)
        val highIntentActions = m.applyCount + m.bookmarkCount + max(m.shareCount, post.shareCount) + post.commentCount
        val hasMeaningfulDwell = m.uniqueViewCount == 0 || m.averageDwellTimeMs >= 2_200L
        val hasClickOrApply = m.uniqueClickCount >= 1 || m.applyCount >= 1 || highIntentActions >= 2
        return hasMeaningfulDwell && hasClickOrApply && highIntentActions >= 1
    }

    /**
     * Computes time-decayed trending velocity from verified unique interactions.
     * High-intent actions (Apply, Bookmark, Share, Comment, Click) carry high weight,
     * while raw reactions/views alone cannot push a post into trending.
     */
    fun computeTrendingVelocity(
        post: FeedPost,
        metrics: PostIntelligenceMetrics?,
        ageHours: Double,
        anomalyRiskNorm: Float = 0f
    ): Double {
        val uniqueViews = metrics?.uniqueViewCount ?: 0
        val uniqueClicks = metrics?.uniqueClickCount ?: 0
        val applies = metrics?.applyCount ?: 0
        val bookmarks = metrics?.bookmarkCount ?: 0
        val shares = max(metrics?.shareCount ?: 0, post.shareCount)
        val comments = post.commentCount.coerceAtLeast(0)
        // Cap reaction contribution strictly so reaction-flooding never creates fake viral posts
        val cappedReactions = min(post.userReactions.size, max(3, uniqueViews + uniqueClicks + 2))

        val highIntentScore = (applies * 6.5) +
            (bookmarks * 3.2) +
            (shares * 2.8) +
            (comments * 2.0) +
            (uniqueClicks * 1.6)
        val passiveScore = (uniqueViews * 0.35) + (cappedReactions * 0.25)

        // If there are zero high-intent actions (no applies, bookmarks, shares, comments, or clicks),
        // cap velocity below the trending threshold so likes/views alone never make a post viral.
        if (highIntentScore <= 0.1) {
            return min(1.5, passiveScore * 0.25)
        }

        val dwellMultiplier = when {
            metrics == null || metrics.uniqueViewCount == 0 -> 1.0
            metrics.averageDwellTimeMs >= 6_000L -> 1.25
            metrics.averageDwellTimeMs >= 2_500L -> 1.05
            metrics.averageDwellTimeMs < 1_500L -> 0.45
            else -> 0.85
        }

        val timeDenominator = (ageHours.coerceAtLeast(0.5) + 2.0).pow(1.35)
        val rawVelocity = ((highIntentScore + passiveScore) * dwellMultiplier * 4.5) / timeDenominator
        val fraudDiscount = (1.0 - (anomalyRiskNorm * 0.85)).coerceIn(0.1, 1.0)
        return (rawVelocity * fraudDiscount).coerceIn(0.0, 100.0)
    }

    data class AnomalyEvaluationResult(
        val anomalyScore: Double,
        val anomalyRiskNorm: Float,
        val isFlagged: Boolean,
        val reasonBangla: String
    )

    /**
     * Fraud & Artificial Engagement Detector:
     * Detects rapid low-dwell views, reaction/like manipulation without real views or clicks,
     * and abnormal click spikes with zero conversion or dwell time.
     */
    fun evaluateAnomalyAndFraud(
        post: FeedPost,
        metrics: PostIntelligenceMetrics?,
        config: IntelligenceConfig = IntelligenceConfig()
    ): AnomalyEvaluationResult {
        if (!config.isFraudDetectionEnabled) {
            return AnomalyEvaluationResult(0.0, 0f, false, "")
        }

        // Preserve explicit Admin manual override if already flagged by Admin
        if (metrics != null && metrics.isFlagged && metrics.flagReason.startsWith("[অ্যাডমিন")) {
            val score = metrics.anomalyScore.coerceAtLeast(80.0)
            return AnomalyEvaluationResult(
                anomalyScore = score,
                anomalyRiskNorm = (score / 100.0).coerceIn(0.0, 1.0).toFloat(),
                isFlagged = true,
                reasonBangla = metrics.flagReason
            )
        }

        var riskPoints = 0.0
        val reasons = mutableListOf<String>()

        val reactionsCount = post.userReactions.size
        val views = metrics?.uniqueViewCount ?: 0
        val clicks = metrics?.uniqueClickCount ?: 0
        val applies = metrics?.applyCount ?: 0
        val avgDwellMs = metrics?.averageDwellTimeMs ?: 0L

        // Rule 1: High reaction count with almost no verified unique views/clicks (Like/Reaction farming)
        if (reactionsCount >= 8 && views <= 2 && clicks == 0) {
            riskPoints += 55.0
            reasons.add("যাচাইকৃত ভিউ ছাড়াই অস্বাভাবিক রিঅ্যাকশন বৃদ্ধি")
        } else if (reactionsCount >= 12 && views > 0 && reactionsCount > (views * 3)) {
            riskPoints += 42.0
            reasons.add("ইউনিক ভিউয়ের তুলনায় কৃত্রিম রিঅ্যাকশন অনুপাত বেশি")
        } else if (reactionsCount >= 15 && clicks == 0 && applies == 0 && (metrics?.bookmarkCount ?: 0) == 0) {
            riskPoints += 28.0
            reasons.add("কোনো প্রকৃত ক্লিক বা আবেদন ছাড়াই অস্বাভাবিক রিঅ্যাকশন")
        }

        // Rule 2: High view count with very low average dwell time (Bot / rapid scroll manipulation)
        if (views >= 10 && avgDwellMs in 1L until 1_600L) {
            riskPoints += 52.0
            reasons.add("অস্বাভাবিক কম পঠন সময় (গড় ${avgDwellMs / 100} ডেসিসেকেন্ড)")
        }

        // Rule 3: Abnormal click-through spike with zero dwell and zero applications (Click spam)
        if (clicks >= 12 && applies == 0 && avgDwellMs < 1_800L) {
            riskPoints += 40.0
            reasons.add("প্রকৃত আবেদন ও পঠন ছাড়াই সন্দেহজনক ক্লিক স্পাইক")
        }

        // Rule 4: Duplicate/spammy text pattern in post description
        if (post.description.length > 40) {
            val words = post.description.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 12) {
                val uniqueWordRatio = words.distinct().size.toDouble() / words.size.toDouble()
                if (uniqueWordRatio < 0.28) {
                    riskPoints += 35.0
                    reasons.add("পুনরাবৃত্তিমূলক বা স্প্যাম টেক্সট প্যাটার্ন")
                }
            }
        }

        val finalAnomalyScore = max(riskPoints, metrics?.anomalyScore ?: 0.0).coerceIn(0.0, 100.0)
        val isFlagged = finalAnomalyScore >= 50.0 || (metrics?.isFlagged == true && metrics.flagReason.isNotBlank())
        val combinedReason = when {
            reasons.isNotEmpty() -> reasons.joinToString(" • ")
            metrics?.flagReason?.isNotBlank() == true -> metrics.flagReason
            else -> ""
        }

        return AnomalyEvaluationResult(
            anomalyScore = finalAnomalyScore,
            anomalyRiskNorm = (finalAnomalyScore / 100.0).coerceIn(0.0, 1.0).toFloat(),
            isFlagged = isFlagged,
            reasonBangla = combinedReason
        )
    }

    /**
     * Adaptive Learning: Updates the user's [UserAffinityState] based on a verified interaction
     * (`VIEW`, `CLICK`, `BOOKMARK`, `SHARE`, `APPLY`) with exponential time decay on older weights.
     */
    fun computeUpdatedAffinityState(
        currentState: UserAffinityState?,
        userId: String,
        post: FeedPost,
        eventType: String,
        dwellTimeMs: Long = 0L,
        nowMillis: Long = System.currentTimeMillis()
    ): UserAffinityState {
        val baseIncrement = when (eventType.uppercase()) {
            EngagementReceipt.EVENT_APPLY -> 5.0
            EngagementReceipt.EVENT_BOOKMARK -> 2.8
            EngagementReceipt.EVENT_SHARE -> 2.5
            EngagementReceipt.EVENT_CLICK -> 1.5
            EngagementReceipt.EVENT_VIEW -> if (dwellTimeMs >= 6_000L) 1.2 else 0.75
            else -> 0.5
        }

        val lastEventMillis = currentState?.lastEventAt?.toDate()?.time ?: nowMillis
        val elapsedDays = ((nowMillis - lastEventMillis).coerceAtLeast(0L).toDouble()) / (1000.0 * 3600.0 * 24.0)
        // Exponential decay with 21-day half-life so recent interests adapt smoothly
        val decayFactor = exp(-ln(2.0) * (elapsedDays / 21.0)).coerceIn(0.35, 1.0)

        val updatedCategories = mutableMapOf<String, Double>()
        currentState?.categoryWeights?.forEach { (cat, weight) ->
            val decayed = (weight * decayFactor)
            if (decayed >= 0.15) {
                updatedCategories[cat] = decayed.coerceAtMost(50.0)
            }
        }

        val postCategories = KhedmotCategoryHelper.resolveDisplayBadges(post)
            .ifEmpty {
                inferCategoriesFromText("${post.designation} ${post.title} ${post.qualification}")
            }

        postCategories.forEach { cat ->
            val cleanKey = cat.trim().take(60)
            if (cleanKey.isNotBlank()) {
                val prev = updatedCategories[cleanKey] ?: 0.0
                updatedCategories[cleanKey] = (prev + baseIncrement).coerceIn(0.0, 50.0)
            }
        }

        val updatedDistricts = mutableMapOf<String, Double>()
        currentState?.districtWeights?.forEach { (dist, weight) ->
            val decayed = (weight * decayFactor)
            if (decayed >= 0.15) {
                updatedDistricts[dist] = decayed.coerceAtMost(50.0)
            }
        }

        val cleanDistrict = post.district.trim().take(60)
        if (cleanDistrict.isNotBlank() && cleanDistrict != "সকল জেলা") {
            val prevDist = updatedDistricts[cleanDistrict] ?: 0.0
            updatedDistricts[cleanDistrict] = (prevDist + (baseIncrement * 0.85)).coerceIn(0.0, 50.0)
        }

        // Keep top 15 categories and top 15 districts to guarantee bounded document size
        val prunedCategories = updatedCategories.entries
            .sortedByDescending { it.value }
            .take(15)
            .associate { it.key to ((it.value * 100.0).roundToInt() / 100.0) }

        val prunedDistricts = updatedDistricts.entries
            .sortedByDescending { it.value }
            .take(15)
            .associate { it.key to ((it.value * 100.0).roundToInt() / 100.0) }

        val nowTs = Timestamp(java.util.Date(nowMillis))
        return UserAffinityState(
            id = UserAffinityState.AFFINITY_DOC_ID,
            userId = userId,
            categoryWeights = prunedCategories,
            districtWeights = prunedDistricts,
            totalInteractions = (currentState?.totalInteractions ?: 0) + 1,
            lastEventAt = nowTs,
            updatedAt = nowTs
        )
    }

    /**
     * Smart Notification Evaluator:
     * Determines whether a newly published circular or seeker application is genuinely relevant
     * to a candidate/madrasa user, preventing notification spam.
     */
    fun evaluateSmartNotificationCandidate(
        post: FeedPost,
        recipientProfile: UserProfile?,
        affinityState: UserAffinityState?,
        config: IntelligenceConfig = IntelligenceConfig()
    ): SmartNotificationEvaluation {
        if (!config.isSmartNotificationEnabled) {
            return SmartNotificationEvaluation(
                shouldNotify = false,
                relevanceScore = 0.0,
                matchSummaryBangla = "স্মার্ট নোটিফিকেশন সাময়িকভাবে বন্ধ রয়েছে",
                matchedQualification = false,
                matchedDistrict = false,
                matchedCategory = false
            )
        }

        if (recipientProfile == null) {
            return SmartNotificationEvaluation(
                shouldNotify = true,
                relevanceScore = 60.0,
                matchSummaryBangla = "নতুন খেদমত বিজ্ঞপ্তি",
                matchedQualification = false,
                matchedDistrict = false,
                matchedCategory = false
            )
        }

        val qualNorm = computeQualificationSimilarity(recipientProfile, post)
        val catNorm = computeCategoryAndDesignationSimilarity(recipientProfile, post)
        val distNorm = computeDistrictSimilarity(recipientProfile, post)
        val affinityNorm = computeUserAffinitySimilarity(affinityState, post, System.currentTimeMillis())

        val relevanceScore = ((qualNorm * 38.0) + (catNorm * 32.0) + (distNorm * 20.0) + (affinityNorm * 10.0))
            .coerceIn(0.0, 100.0)

        val matchedQualification = qualNorm >= 0.55f
        val matchedCategory = catNorm >= 0.55f
        val matchedDistrict = distNorm >= 0.55f

        val shouldNotify = relevanceScore >= config.smartNotificationThreshold ||
            (matchedQualification && matchedDistrict) ||
            (matchedCategory && matchedDistrict) ||
            post.isBoosted

        val reasons = mutableListOf<String>()
        if (matchedQualification) reasons.add("যোগ্যতা (${post.qualification.take(24)})")
        if (matchedCategory) reasons.add("বিভাগ (${post.designation.take(24)})")
        if (matchedDistrict) reasons.add("জেলা (${post.district})")

        val summaryBangla = if (reasons.isNotEmpty()) {
            "আপনার ${reasons.joinToString(", ")}-এর সাথে ${relevanceScore.roundToInt()}% সামঞ্জস্যপূর্ণ"
        } else {
            "প্রাসঙ্গিকতা স্কোর: ${relevanceScore.roundToInt()}%"
        }

        return SmartNotificationEvaluation(
            shouldNotify = shouldNotify,
            relevanceScore = relevanceScore,
            matchSummaryBangla = summaryBangla,
            matchedQualification = matchedQualification,
            matchedDistrict = matchedDistrict,
            matchedCategory = matchedCategory
        )
    }

    /**
     * Computes real-time System & Algorithm Performance Analytics for the Admin Control Center.
     */
    fun computeSystemAnalytics(
        posts: List<FeedPost>,
        metricsMap: Map<String, PostIntelligenceMetrics>,
        submissions: List<KhedmotSubmission>,
        config: IntelligenceConfig
    ): AlgorithmPerformanceSummary {
        val scoredAll = rankFeedPosts(
            posts = posts,
            userProfile = null,
            affinityState = null,
            metricsMap = metricsMap,
            config = config
        )

        var totalViews = 0
        var totalClicks = 0
        var totalApplies = 0
        var totalBookmarks = 0
        var totalShares = 0
        var totalDwellMs = 0L

        posts.forEach { post ->
            val m = metricsMap[post.id]
            totalViews += m?.uniqueViewCount ?: 0
            totalClicks += m?.uniqueClickCount ?: 0
            totalApplies += m?.applyCount ?: 0
            totalBookmarks += m?.bookmarkCount ?: 0
            totalShares += max(m?.shareCount ?: 0, post.shareCount)
            totalDwellMs += m?.totalDwellTimeMs ?: 0L
        }

        // Cross-check with actual KhedmotSubmissions collection count so real submissions are always reflected
        val effectiveApplies = max(totalApplies, submissions.size)
        val effectiveViews = max(totalViews, effectiveApplies + totalClicks)
        val effectiveClicks = max(totalClicks, effectiveApplies)

        val ctrPercent = if (effectiveViews > 0) {
            ((effectiveClicks.toDouble() / effectiveViews.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val conversionPercent = if (effectiveClicks > 0) {
            ((effectiveApplies.toDouble() / effectiveClicks.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val shortlistedOrAccepted = submissions.count {
            it.status.equals("SHORTLISTED", ignoreCase = true) ||
                it.status.equals("ACCEPTED", ignoreCase = true)
        }
        val shortlistRatePercent = if (submissions.isNotEmpty()) {
            ((shortlistedOrAccepted.toDouble() / submissions.size.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val avgDwellSeconds = if (totalViews > 0) {
            (totalDwellMs.toDouble() / totalViews.toDouble()) / 1000.0
        } else {
            0.0
        }

        val trendingPosts = selectTrendingPosts(scoredAll, limit = 8)
        val flaggedPairs = posts.mapNotNull { post ->
            val existingMetrics = metricsMap[post.id]
            val eval = evaluateAnomalyAndFraud(post, existingMetrics, config)
            if (eval.isFlagged) {
                val resolvedMetrics = (existingMetrics ?: PostIntelligenceMetrics(id = post.id, postId = post.id)).copy(
                    anomalyScore = eval.anomalyScore,
                    isFlagged = true,
                    flagReason = eval.reasonBangla.ifBlank { existingMetrics?.flagReason.orEmpty() }
                )
                post to resolvedMetrics
            } else {
                null
            }
        }.sortedByDescending { it.second.anomalyScore }

        return AlgorithmPerformanceSummary(
            totalTrackedPosts = posts.size,
            totalUniqueViews = effectiveViews,
            totalUniqueClicks = effectiveClicks,
            totalApplications = effectiveApplies,
            totalBookmarks = totalBookmarks,
            totalShares = totalShares,
            overallCtrPercent = (ctrPercent * 10.0).roundToInt() / 10.0,
            overallApplicationConversionPercent = (conversionPercent * 10.0).roundToInt() / 10.0,
            shortlistOrAcceptRatePercent = (shortlistRatePercent * 10.0).roundToInt() / 10.0,
            averageDwellTimeSeconds = (avgDwellSeconds * 10.0).roundToInt() / 10.0,
            activeTrendingPostsCount = trendingPosts.size,
            flaggedAnomalyPostsCount = flaggedPairs.size,
            topTrendingPosts = trendingPosts,
            flaggedPosts = flaggedPairs
        )
    }

    // =========================================================================
    // Internal Semantic & Mathematical Scoring Helpers
    // =========================================================================

    private fun computeQualificationSimilarity(userProfile: UserProfile?, post: FeedPost): Float {
        val isGeneralPost = post.postType.equals("GENERAL_POST", ignoreCase = true)
        val hasVisualMedia = post.imageUrls.isNotEmpty() ||
            post.mediaMetadata.isNotEmpty() ||
            (post.attachmentDocId.isNotBlank() && !post.attachmentDocId.endsWith("_ext_text"))
        if (isGeneralPost && post.qualification.isBlank()) {
            return if (hasVisualMedia) 0.78f else 0.70f
        }
        if (userProfile == null) return if (hasVisualMedia) 0.65f else 0.5f
        val userQualText = "${userProfile.qualification} ${userProfile.designation} ${userProfile.bio}".lowercase()
        val postQualText = "${post.qualification} ${post.designation} ${post.title}".lowercase()
        if (userQualText.isBlank() || postQualText.isBlank()) return if (hasVisualMedia) 0.65f else 0.45f

        // Direct token overlap check
        val userTokens = tokenizeBanglaAndEnglish(userProfile.qualification)
        val postTokens = tokenizeBanglaAndEnglish(post.qualification)
        if (userTokens.isNotEmpty() && postTokens.isNotEmpty()) {
            val intersection = userTokens.intersect(postTokens)
            if (intersection.isNotEmpty()) {
                return (0.75f + (0.25f * (intersection.size.toFloat() / max(1, postTokens.size)))).coerceIn(0f, 1f)
            }
        }

        // Semantic Marhala Hierarchy match
        val userLevels = qualificationHierarchyGroups.filter { (_, keywords) ->
            keywords.any { kw -> userQualText.contains(kw.lowercase()) }
        }.map { it.first }

        val postLevels = qualificationHierarchyGroups.filter { (_, keywords) ->
            keywords.any { kw -> postQualText.contains(kw.lowercase()) }
        }.map { it.first }

        if (userLevels.isNotEmpty() && postLevels.isNotEmpty()) {
            if (userLevels.intersect(postLevels.toSet()).isNotEmpty()) {
                return 0.92f
            }
            val maxUserLevel = userLevels.maxOrNull() ?: 0
            val maxPostLevel = postLevels.maxOrNull() ?: 0
            // Higher Qawmi qualification (e.g. Ifta=5) qualifies for Dawra/Darsiyat (4)
            if (maxUserLevel >= maxPostLevel && (maxUserLevel - maxPostLevel) == 1) {
                return 0.78f
            }
            if (kotlin.math.abs(maxUserLevel - maxPostLevel) == 1) {
                return 0.60f
            }
        }

        return if (isGeneralPost || hasVisualMedia) 0.62f else 0.25f
    }

    private fun computeCategoryAndDesignationSimilarity(userProfile: UserProfile?, post: FeedPost): Float {
        val isGeneralPost = post.postType.equals("GENERAL_POST", ignoreCase = true)
        val hasVisualMedia = post.imageUrls.isNotEmpty() ||
            post.mediaMetadata.isNotEmpty() ||
            (post.attachmentDocId.isNotBlank() && !post.attachmentDocId.endsWith("_ext_text"))
        if (userProfile == null) return if (hasVisualMedia) 0.65f else 0.5f
        val userCategories = inferCategoriesFromText(
            "${userProfile.designation} ${userProfile.qualification} ${userProfile.bio}"
        ).toSet()
        val postCategories = KhedmotCategoryHelper.resolveDisplayBadges(post).ifEmpty {
            inferCategoriesFromText("${post.designation} ${post.title} ${post.qualification}")
        }.toSet()

        if (userCategories.isNotEmpty() && postCategories.isNotEmpty()) {
            val overlap = userCategories.intersect(postCategories)
            if (overlap.isNotEmpty()) {
                val ratio = overlap.size.toFloat() / max(1, min(userCategories.size, postCategories.size))
                return (0.70f + (0.30f * ratio)).coerceIn(0f, 1f)
            }
        }

        val userDesigTokens = tokenizeBanglaAndEnglish(userProfile.designation)
        val postDesigTokens = tokenizeBanglaAndEnglish("${post.designation} ${post.title}")
        if (userDesigTokens.isNotEmpty() && postDesigTokens.isNotEmpty()) {
            val common = userDesigTokens.intersect(postDesigTokens)
            if (common.isNotEmpty()) return 0.80f
        }

        if (isGeneralPost && postCategories.isEmpty()) {
            return if (hasVisualMedia) 0.76f else 0.68f
        }

        return if (isGeneralPost || hasVisualMedia) 0.62f else 0.25f
    }

    private fun computeDistrictSimilarity(userProfile: UserProfile?, post: FeedPost): Float {
        if (userProfile == null) return 0.5f
        val postDistrict = post.district.trim()
        if (postDistrict.isBlank() || postDistrict == "সকল জেলা") return 0.65f

        val userDistrict = userProfile.district.trim()
        if (userDistrict.isNotBlank() && userDistrict.equals(postDistrict, ignoreCase = true)) {
            return 1.0f
        }

        val userPreferred = userProfile.preferredDistricts
        if (userPreferred.isNotBlank()) {
            if (userPreferred.contains("সকল জেলা") || userPreferred.contains("যেকোনো জেলা") || userPreferred.contains("সারা বাংলাদেশ")) {
                return 0.82f
            }
            if (userPreferred.contains(postDistrict, ignoreCase = true)) {
                return 0.90f
            }
        }

        val postPreferred = post.deadlineOrPreferredDistricts
        if (userDistrict.isNotBlank() && postPreferred.contains(userDistrict, ignoreCase = true)) {
            return 0.85f
        }

        return 0.20f
    }

    private fun computeExperienceSimilarity(userProfile: UserProfile?, post: FeedPost): Float {
        if (userProfile == null) return 0.6f
        val userYears = extractYearsOfExperience(userProfile.experience)
        val postYears = extractYearsOfExperience("${post.salaryOrExperience} ${post.description}")
        if (postYears == null) return 0.75f // Post does not require strict years of experience
        if (userYears == null) return 0.50f
        return when {
            userYears >= postYears -> 1.0f
            (postYears - userYears) == 1 -> 0.75f
            else -> 0.40f
        }
    }

    private fun computeUserAffinitySimilarity(
        affinityState: UserAffinityState?,
        post: FeedPost,
        nowMillis: Long
    ): Float {
        if (affinityState == null || affinityState.totalInteractions <= 0) return 0.35f

        val lastEventMs = affinityState.lastEventAt?.toDate()?.time ?: nowMillis
        val elapsedDays = ((nowMillis - lastEventMs).coerceAtLeast(0L).toDouble()) / (1000.0 * 3600.0 * 24.0)
        val recencyMultiplier = exp(-ln(2.0) * (elapsedDays / 21.0)).coerceIn(0.35, 1.0)

        val postCategories = KhedmotCategoryHelper.resolveDisplayBadges(post).ifEmpty {
            inferCategoriesFromText("${post.designation} ${post.title}")
        }
        val maxCatWeight = affinityState.categoryWeights.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val matchedCatWeight = postCategories.maxOfOrNull { cat ->
            affinityState.categoryWeights[cat] ?: 0.0
        } ?: 0.0
        val normCatAffinity = (matchedCatWeight / maxCatWeight).coerceIn(0.0, 1.0)

        val maxDistWeight = affinityState.districtWeights.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val matchedDistWeight = affinityState.districtWeights[post.district.trim()] ?: 0.0
        val normDistAffinity = (matchedDistWeight / maxDistWeight).coerceIn(0.0, 1.0)

        val combined = ((normCatAffinity * 0.68) + (normDistAffinity * 0.32)) * recencyMultiplier
        return combined.coerceIn(0.0, 1.0).toFloat()
    }

    private fun computePostAgeHours(createdAt: Timestamp?, nowMillis: Long): Double {
        val createdMillis = createdAt?.toDate()?.time ?: return 6.0
        val diffMillis = (nowMillis - createdMillis).coerceAtLeast(0L)
        return diffMillis.toDouble() / (1000.0 * 3600.0)
    }

    private fun computeFreshnessFactor(
        ageHours: Double,
        halfLifeHours: Double,
        newPostBoostHours: Double
    ): Float {
        val exponentialDecay = 2.0.pow(-ageHours / halfLifeHours.coerceAtLeast(4.0))
        val coldStartBoost = if (ageHours <= newPostBoostHours) {
            0.18 * ((newPostBoostHours - ageHours) / newPostBoostHours.coerceAtLeast(1.0))
        } else {
            0.0
        }
        return (exponentialDecay + coldStartBoost).coerceIn(0.05, 1.0).toFloat()
    }

    private fun buildMatchReasonBangla(
        qualNorm: Float,
        catNorm: Float,
        distNorm: Float,
        affinityNorm: Float,
        isTrending: Boolean,
        isNewFresh: Boolean,
        post: FeedPost
    ): String {
        val tags = mutableListOf<String>()
        if (qualNorm >= 0.72f) tags.add("যোগ্যতা সামঞ্জস্যপূর্ণ")
        if (catNorm >= 0.70f) tags.add("বিভাগ মিল")
        if (distNorm >= 0.80f && post.district.isNotBlank()) tags.add("${post.district} জেলা")
        if (tags.isEmpty() && affinityNorm >= 0.55f) tags.add("আপনার আগ্রহের বিভাগ")
        if (isTrending) tags.add("ট্রেন্ডিং খেদমত")
        if (isNewFresh && tags.size < 2) tags.add("নতুন প্রকাশিত")
        return tags.take(2).joinToString(" • ")
    }

    private fun tokenizeBanglaAndEnglish(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("[\\s,।|/\\-•():;]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .toSet()
    }

    fun extractYearsOfExperience(text: String): Int? {
        if (text.isBlank()) return null
        val normalized = normalizeBanglaDigitsToAscii(text)
        val regex = Regex("(\\d{1,2})\\s*(?:বছর|বৎসর|year|years|yr)", RegexOption.IGNORE_CASE)
        val match = regex.find(normalized) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }

    fun inferCategoriesFromText(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val lower = text.lowercase()
        val matched = mutableListOf<String>()
        if (lower.contains("নূরানী") || lower.contains("নুরানী") || lower.contains("মক্তব")) {
            matched.add(KhedmotCategoryHelper.CAT_NOORANI)
        }
        if (lower.contains("নাজেরা") || lower.contains("হিফজ") || lower.contains("হিফয") || lower.contains("হাফেজ") || lower.contains("কিরাত")) {
            matched.add(KhedmotCategoryHelper.CAT_NAZERA)
        }
        if (lower.contains("দরসিয়াত") || lower.contains("দরসিয়াত") || lower.contains("মুহাদ্দিস") || lower.contains("মুফতি") || lower.contains("দাওরা") || lower.contains("ইফতা") || lower.contains("কিতাব") || lower.contains("মুদাররিস")) {
            matched.add(KhedmotCategoryHelper.CAT_DARSIYAT)
        }
        if (lower.contains("ইমাম")) {
            matched.add(KhedmotCategoryHelper.CAT_IMAM)
        }
        if (lower.contains("মুয়াজ্জিন") || lower.contains("মুয়াজ্জিন") || lower.contains("খাদেম")) {
            matched.add(KhedmotCategoryHelper.CAT_MUAZZIN)
        }
        if (lower.contains("খতিব") || lower.contains("খতীব")) {
            matched.add(KhedmotCategoryHelper.CAT_KHATIB)
        }
        return matched.distinct()
    }

    private fun normalizeBanglaDigitsToAscii(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '০'..'৯' -> sb.append(('0'.code + (ch.code - '০'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}

