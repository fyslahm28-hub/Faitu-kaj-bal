package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlgorithmPerformanceSummary
import com.example.data.model.FeedPost
import com.example.data.model.IntelligenceConfig
import com.example.data.model.PostIntelligenceMetrics
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextMuted
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.IslamicDateAndPrayerHelper
import kotlin.math.roundToInt

private val KksHeaderGreen = Color(0xFF0A4D3C)
private val KksDeepGreen = Color(0xFF06372A)
private val KksActiveGreen = Color(0xFF1F7A53)
private val KksWarningAmber = Color(0xFFD9822B)
private val KksDangerRed = Color(0xFFC83E3B)
private val KksCardBorder = Color(0xFFDCE7E1)

@Composable
fun AdminIntelligenceControlCenterView(
    intelligenceConfig: IntelligenceConfig,
    algorithmPerformance: AlgorithmPerformanceSummary,
    postMetricsMap: Map<String, PostIntelligenceMetrics>,
    allPosts: List<FeedPost>,
    isSubmitting: Boolean,
    onSaveConfig: (IntelligenceConfig) -> Unit,
    onOverridePostAnomaly: (post: FeedPost, flagPost: Boolean, reasonBangla: String) -> Unit,
    onToggleBoostPost: (FeedPost, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var selectedSection by rememberSaveable { mutableStateOf(0) } // 0 = অ্যানালিটিক্স, 1 = র‍্যাঙ্কিং নিয়ন্ত্রণ, 2 = ট্রেন্ডিং, 3 = অ্যান্টি-ফ্রড

    // Editable local state synced with intelligenceConfig
    var isSmartRankingEnabled by remember(intelligenceConfig) { mutableStateOf(intelligenceConfig.isSmartRankingEnabled) }
    var isTrendingEnabled by remember(intelligenceConfig) { mutableStateOf(intelligenceConfig.isTrendingEnabled) }
    var isFraudDetectionEnabled by remember(intelligenceConfig) { mutableStateOf(intelligenceConfig.isFraudDetectionEnabled) }
    var isSmartNotificationEnabled by remember(intelligenceConfig) { mutableStateOf(intelligenceConfig.isSmartNotificationEnabled) }

    var skillMatchWeight by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.skillMatchWeight) }
    var locationMatchWeight by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.locationMatchWeight) }
    var experienceWeight by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.experienceWeight) }
    var trendingWeight by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.trendingWeight) }
    var verifiedMadrasaBonus by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.verifiedMadrasaBonus) }
    var newPostBoostHours by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.newPostBoostHours) }
    var freshnessHalfLifeHours by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.freshnessHalfLifeHours) }
    var minDwellTimeMs by remember(intelligenceConfig) { mutableLongStateOf(intelligenceConfig.minDwellTimeMs) }
    var fraudPenaltyWeight by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.fraudPenaltyWeight) }
    var smartNotificationThreshold by remember(intelligenceConfig) { mutableDoubleStateOf(intelligenceConfig.smartNotificationThreshold) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_intelligence_control_screen")
    ) {
        // Header Bar
        Surface(
            color = KksHeaderGreen,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("admin_intelligence_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = QawmiGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KKS Intelligence ও অ্যালগরিদম কন্ট্রোল",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "স্মার্ট র‍্যাঙ্কিং • রেকমেন্ডেশন • ট্রেন্ডিং • অ্যান্টি-ফ্রড শিল্ড",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD9F2E6)
                    )
                }
            }
        }

        // Section Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                Triple(0, "অ্যানালিটিক্স", "admin_intel_tab_analytics"),
                Triple(1, "র‍্যাঙ্কিং ও ওয়েট", "admin_intel_tab_weights"),
                Triple(2, "ট্রেন্ডিং মনিটর", "admin_intel_tab_trending"),
                Triple(3, "অ্যান্টি-ফ্রড (${IslamicDateAndPrayerHelper.toBanglaDigits(algorithmPerformance.flaggedAnomalyPostsCount.toString())})", "admin_intel_tab_fraud")
            )
            tabs.forEach { (idx, label, tag) ->
                val isSelected = selectedSection == idx
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) KksHeaderGreen else QawmiMintSurface,
                    border = BorderStroke(1.dp, if (isSelected) KksHeaderGreen else KksCardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedSection = idx }
                        .testTag(tag)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else KksHeaderGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = KksCardBorder)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedSection) {
                0 -> IntelligenceAnalyticsOverviewSection(
                    config = intelligenceConfig,
                    performance = algorithmPerformance,
                    onJumpToWeights = { selectedSection = 1 },
                    onJumpToFraud = { selectedSection = 3 }
                )

                1 -> IntelligenceWeightsControlSection(
                    isSmartRankingEnabled = isSmartRankingEnabled,
                    isTrendingEnabled = isTrendingEnabled,
                    isFraudDetectionEnabled = isFraudDetectionEnabled,
                    isSmartNotificationEnabled = isSmartNotificationEnabled,
                    skillMatchWeight = skillMatchWeight,
                    locationMatchWeight = locationMatchWeight,
                    experienceWeight = experienceWeight,
                    trendingWeight = trendingWeight,
                    verifiedMadrasaBonus = verifiedMadrasaBonus,
                    newPostBoostHours = newPostBoostHours,
                    freshnessHalfLifeHours = freshnessHalfLifeHours,
                    minDwellTimeMs = minDwellTimeMs,
                    fraudPenaltyWeight = fraudPenaltyWeight,
                    smartNotificationThreshold = smartNotificationThreshold,
                    isSubmitting = isSubmitting,
                    onSmartRankingChange = { isSmartRankingEnabled = it },
                    onTrendingChange = { isTrendingEnabled = it },
                    onFraudDetectionChange = { isFraudDetectionEnabled = it },
                    onSmartNotificationChange = { isSmartNotificationEnabled = it },
                    onSkillWeightChange = { skillMatchWeight = it },
                    onLocationWeightChange = { locationMatchWeight = it },
                    onExperienceWeightChange = { experienceWeight = it },
                    onTrendingWeightChange = { trendingWeight = it },
                    onVerifiedBonusChange = { verifiedMadrasaBonus = it },
                    onNewPostBoostHoursChange = { newPostBoostHours = it },
                    onFreshnessHalfLifeChange = { freshnessHalfLifeHours = it },
                    onMinDwellTimeChange = { minDwellTimeMs = it },
                    onFraudPenaltyChange = { fraudPenaltyWeight = it },
                    onSmartNotifThresholdChange = { smartNotificationThreshold = it },
                    onResetDefaults = {
                        val def = IntelligenceConfig()
                        isSmartRankingEnabled = def.isSmartRankingEnabled
                        isTrendingEnabled = def.isTrendingEnabled
                        isFraudDetectionEnabled = def.isFraudDetectionEnabled
                        isSmartNotificationEnabled = def.isSmartNotificationEnabled
                        skillMatchWeight = def.skillMatchWeight
                        locationMatchWeight = def.locationMatchWeight
                        experienceWeight = def.experienceWeight
                        trendingWeight = def.trendingWeight
                        verifiedMadrasaBonus = def.verifiedMadrasaBonus
                        newPostBoostHours = def.newPostBoostHours
                        freshnessHalfLifeHours = def.freshnessHalfLifeHours
                        minDwellTimeMs = def.minDwellTimeMs
                        fraudPenaltyWeight = def.fraudPenaltyWeight
                        smartNotificationThreshold = def.smartNotificationThreshold
                    },
                    onSave = {
                        onSaveConfig(
                            intelligenceConfig.copy(
                                isSmartRankingEnabled = isSmartRankingEnabled,
                                isTrendingEnabled = isTrendingEnabled,
                                isFraudDetectionEnabled = isFraudDetectionEnabled,
                                isSmartNotificationEnabled = isSmartNotificationEnabled,
                                skillMatchWeight = skillMatchWeight,
                                locationMatchWeight = locationMatchWeight,
                                experienceWeight = experienceWeight,
                                trendingWeight = trendingWeight,
                                verifiedMadrasaBonus = verifiedMadrasaBonus,
                                newPostBoostHours = newPostBoostHours,
                                freshnessHalfLifeHours = freshnessHalfLifeHours,
                                minDwellTimeMs = minDwellTimeMs,
                                fraudPenaltyWeight = fraudPenaltyWeight,
                                smartNotificationThreshold = smartNotificationThreshold
                            )
                        )
                    }
                )

                2 -> IntelligenceTrendingMonitorSection(
                    performance = algorithmPerformance,
                    postMetricsMap = postMetricsMap,
                    onToggleBoostPost = onToggleBoostPost
                )

                3 -> IntelligenceFraudAndAnomalySection(
                    performance = algorithmPerformance,
                    allPosts = allPosts,
                    postMetricsMap = postMetricsMap,
                    onOverridePostAnomaly = onOverridePostAnomaly
                )
            }
        }
    }
}

@Composable
private fun IntelligenceAnalyticsOverviewSection(
    config: IntelligenceConfig,
    performance: AlgorithmPerformanceSummary,
    onJumpToWeights: () -> Unit,
    onJumpToFraud: () -> Unit
) {
    // Active Engine Banner
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KksCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(KksDeepGreen, KksHeaderGreen)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = QawmiGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KKS Intelligence Engine সক্রিয়",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (config.isSmartRankingEnabled) KksActiveGreen else KksWarningAmber
                ) {
                    Text(
                        text = if (config.isSmartRankingEnabled) "AI-Ready • LIVE" else "ক্রনোলজিক্যাল মোড",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "ব্যবহারকারীর ইলমি যোগ্যতা, খেদমতের বিভাগ, জেলা, অভিজ্ঞতা এবং প্রকৃত এনগেজমেন্ট বিশ্লেষণ করে স্বয়ংক্রিয়ভাবে ফিড র‍্যাঙ্কিং ও রেকমেন্ডেশন পরিচালিত হচ্ছে।",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2F5EC)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPillBadge(
                    label = "র‍্যাঙ্কিং: ${if (config.isSmartRankingEnabled) "চালু" else "বন্ধ"}",
                    active = config.isSmartRankingEnabled
                )
                StatusPillBadge(
                    label = "ট্রেন্ডিং: ${if (config.isTrendingEnabled) "চালু" else "বন্ধ"}",
                    active = config.isTrendingEnabled
                )
                StatusPillBadge(
                    label = "অ্যান্টি-ফ্রড: ${if (config.isFraudDetectionEnabled) "চালু" else "বন্ধ"}",
                    active = config.isFraudDetectionEnabled
                )
            }
        }
    }

    // Conversion & Engagement KPI Grid
    Text(
        text = "অ্যালগরিদম ও এনগেজমেন্ট পারফরম্যান্স",
        style = MaterialTheme.typography.titleSmall,
        color = KksHeaderGreen,
        fontWeight = FontWeight.Bold
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IntelMetricCard(
            title = "ইউনিক ভিউ (যাচাইকৃত)",
            value = IslamicDateAndPrayerHelper.toBanglaDigits(performance.totalUniqueViews.toString()),
            subtitle = "ন্যূনতম ${(config.minDwellTimeMs / 1000.0)} সে. পঠন",
            accentColor = KksHeaderGreen,
            modifier = Modifier.weight(1f)
        )
        IntelMetricCard(
            title = "ইউনিক ক্লিক ও CTR",
            value = "${IslamicDateAndPrayerHelper.toBanglaDigits(performance.totalUniqueClicks.toString())} (${IslamicDateAndPrayerHelper.toBanglaDigits(performance.overallCtrPercent.toString())}%)",
            subtitle = "ক্লিক-থ্রু রেট",
            accentColor = Color(0xFF1565C0),
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IntelMetricCard(
            title = "প্রকৃত খেদমত আবেদন",
            value = "${IslamicDateAndPrayerHelper.toBanglaDigits(performance.totalApplications.toString())} (${IslamicDateAndPrayerHelper.toBanglaDigits(performance.overallApplicationConversionPercent.toString())}%)",
            subtitle = "কনভার্সন হার",
            accentColor = KksActiveGreen,
            modifier = Modifier.weight(1f)
        )
        IntelMetricCard(
            title = "শর্টলিস্ট ও গৃহীত হার",
            value = "${IslamicDateAndPrayerHelper.toBanglaDigits(performance.shortlistOrAcceptRatePercent.toString())}%",
            subtitle = "সফল ম্যাচিং গুণমান",
            accentColor = Color(0xFF8E24AA),
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IntelMetricCard(
            title = "সংরক্ষণ ও শেয়ার",
            value = "${IslamicDateAndPrayerHelper.toBanglaDigits(performance.totalBookmarks.toString())} / ${IslamicDateAndPrayerHelper.toBanglaDigits(performance.totalShares.toString())}",
            subtitle = "বুকমার্ক • শেয়ার",
            accentColor = Color(0xFF00838F),
            modifier = Modifier.weight(1f)
        )
        IntelMetricCard(
            title = "অ্যান্টি-ফ্রড সতর্কতা",
            value = IslamicDateAndPrayerHelper.toBanglaDigits(performance.flaggedAnomalyPostsCount.toString()),
            subtitle = if (performance.flaggedAnomalyPostsCount > 0) "পর্যালোচনা প্রয়োজন" else "কোনো স্প্যাম নেই",
            accentColor = if (performance.flaggedAnomalyPostsCount > 0) KksDangerRed else KksActiveGreen,
            modifier = Modifier
                .weight(1f)
                .clickable { onJumpToFraud() }
        )
    }

    // Quick Navigation Actions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onJumpToWeights,
            colors = ButtonDefaults.buttonColors(containerColor = KksHeaderGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("admin_intel_jump_weights_btn")
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("র‍্যাঙ্কিং ওয়েট টিউনিং", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onJumpToFraud,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, KksHeaderGreen),
            modifier = Modifier
                .weight(1f)
                .testTag("admin_intel_jump_fraud_btn")
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = KksHeaderGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("অ্যান্টি-ফ্রড কিউ", color = KksHeaderGreen, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IntelligenceWeightsControlSection(
    isSmartRankingEnabled: Boolean,
    isTrendingEnabled: Boolean,
    isFraudDetectionEnabled: Boolean,
    isSmartNotificationEnabled: Boolean,
    skillMatchWeight: Double,
    locationMatchWeight: Double,
    experienceWeight: Double,
    trendingWeight: Double,
    verifiedMadrasaBonus: Double,
    newPostBoostHours: Double,
    freshnessHalfLifeHours: Double,
    minDwellTimeMs: Long,
    fraudPenaltyWeight: Double,
    smartNotificationThreshold: Double,
    isSubmitting: Boolean,
    onSmartRankingChange: (Boolean) -> Unit,
    onTrendingChange: (Boolean) -> Unit,
    onFraudDetectionChange: (Boolean) -> Unit,
    onSmartNotificationChange: (Boolean) -> Unit,
    onSkillWeightChange: (Double) -> Unit,
    onLocationWeightChange: (Double) -> Unit,
    onExperienceWeightChange: (Double) -> Unit,
    onTrendingWeightChange: (Double) -> Unit,
    onVerifiedBonusChange: (Double) -> Unit,
    onNewPostBoostHoursChange: (Double) -> Unit,
    onFreshnessHalfLifeChange: (Double) -> Unit,
    onMinDwellTimeChange: (Long) -> Unit,
    onFraudPenaltyChange: (Double) -> Unit,
    onSmartNotifThresholdChange: (Double) -> Unit,
    onResetDefaults: () -> Unit,
    onSave: () -> Unit
) {
    // Module Switches Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KksCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "মডিউল সক্রিয়করণ নিয়ন্ত্রণ",
                style = MaterialTheme.typography.titleSmall,
                color = KksHeaderGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            IntelModuleToggleRow(
                icon = Icons.Default.Psychology,
                title = "স্মার্ট র‍্যাঙ্কিং ও রেকমেন্ডেশন ইঞ্জিন",
                subtitle = "ব্যবহারকারীর যোগ্যতা, বিভাগ ও পছন্দ অনুযায়ী ফিড সাজাবে",
                checked = isSmartRankingEnabled,
                testTag = "admin_intel_toggle_smart_ranking",
                onCheckedChange = onSmartRankingChange
            )
            HorizontalDivider(color = QawmiMintSurface)
            IntelModuleToggleRow(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "ট্রেন্ডিং ভেলোসিটি ডিটেকশন",
                subtitle = "প্রকৃত আবেদন, সংরক্ষণ ও পঠন সময়ের ভিত্তিতে জনপ্রিয় পোস্ট নির্ধারণ",
                checked = isTrendingEnabled,
                testTag = "admin_intel_toggle_trending",
                onCheckedChange = onTrendingChange
            )
            HorizontalDivider(color = QawmiMintSurface)
            IntelModuleToggleRow(
                icon = Icons.Default.Security,
                title = "অ্যান্টি-ফ্রড ও কৃত্রিম এনগেজমেন্ট শিল্ড",
                subtitle = "ফেক ভিউ, বট ক্লিক ও অস্বাভাবিক রিঅ্যাকশন শনাক্ত ও ডিমোট করবে",
                checked = isFraudDetectionEnabled,
                testTag = "admin_intel_toggle_fraud",
                onCheckedChange = onFraudDetectionChange
            )
            HorizontalDivider(color = QawmiMintSurface)
            IntelModuleToggleRow(
                icon = Icons.Default.NotificationsActive,
                title = "স্মার্ট প্রাসঙ্গিক নোটিফিকেশন ফিল্টার",
                subtitle = "শুধুমাত্র যোগ্যতা ও জেলার সাথে সামঞ্জস্যপূর্ণ বিজ্ঞপ্তির ইত্তেলা পাঠাবে",
                checked = isSmartNotificationEnabled,
                testTag = "admin_intel_toggle_smart_notif",
                onCheckedChange = onSmartNotificationChange
            )
        }
    }

    // Ranking Weights Sliders Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KksCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "র‍্যাঙ্কিং ও ম্যাচিং গুরুত্ব (Weights)",
                style = MaterialTheme.typography.titleSmall,
                color = KksHeaderGreen,
                fontWeight = FontWeight.Bold
            )

            IntelWeightSliderRow(
                label = "ইলমি যোগ্যতা ও বিভাগ ম্যাচিং গুরুত্ব",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits(skillMatchWeight.roundToInt().toString())} পয়েন্ট",
                value = skillMatchWeight.toFloat(),
                valueRange = 5f..60f,
                onValueChange = { onSkillWeightChange(it.roundToInt().toDouble()) }
            )

            IntelWeightSliderRow(
                label = "জেলা ও পছন্দের কর্মস্থল গুরুত্ব",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits(locationMatchWeight.roundToInt().toString())} পয়েন্ট",
                value = locationMatchWeight.toFloat(),
                valueRange = 5f..45f,
                onValueChange = { onLocationWeightChange(it.roundToInt().toDouble()) }
            )

            IntelWeightSliderRow(
                label = "অভিজ্ঞতা সামঞ্জস্যতা গুরুত্ব",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits(experienceWeight.roundToInt().toString())} পয়েন্ট",
                value = experienceWeight.toFloat(),
                valueRange = 0f..30f,
                onValueChange = { onExperienceWeightChange(it.roundToInt().toDouble()) }
            )

            IntelWeightSliderRow(
                label = "ট্রেন্ডিং ভেলোসিটি গুরুত্ব",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits(trendingWeight.roundToInt().toString())} পয়েন্ট",
                value = trendingWeight.toFloat(),
                valueRange = 0f..35f,
                onValueChange = { onTrendingWeightChange(it.roundToInt().toDouble()) }
            )

            IntelWeightSliderRow(
                label = "যাচাইকৃত মাদরাসা ট্রাস্ট বোনাস",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits(verifiedMadrasaBonus.roundToInt().toString())} পয়েন্ট",
                value = verifiedMadrasaBonus.toFloat(),
                valueRange = 0f..30f,
                onValueChange = { onVerifiedBonusChange(it.roundToInt().toDouble()) }
            )

            HorizontalDivider(color = QawmiMintSurface)

            Text(
                text = "নতুন ও জনপ্রিয় পোস্টের ভারসাম্য এবং নিরাপত্তা থ্রেশহোল্ড",
                style = MaterialTheme.typography.titleSmall,
                color = KksHeaderGreen,
                fontWeight = FontWeight.Bold
            )

            IntelWeightSliderRow(
                label = "নতুন পোস্ট বুস্ট সময়কাল (Cold-Start Boost)",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits(newPostBoostHours.roundToInt().toString())} ঘণ্টা",
                value = newPostBoostHours.toFloat(),
                valueRange = 2f..72f,
                onValueChange = { onNewPostBoostHoursChange(it.roundToInt().toDouble()) }
            )

            IntelWeightSliderRow(
                label = "ফ্রেশনেস হাফ-লাইফ (Time Decay)",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits(freshnessHalfLifeHours.roundToInt().toString())} ঘণ্টা",
                value = freshnessHalfLifeHours.toFloat(),
                valueRange = 12f..168f,
                onValueChange = { onFreshnessHalfLifeChange(it.roundToInt().toDouble()) }
            )

            IntelWeightSliderRow(
                label = "ইউনিক ভিউ ন্যূনতম পঠন সময় (Dwell Gate)",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits((minDwellTimeMs / 500L * 0.5).toString())} সেকেন্ড",
                value = minDwellTimeMs.toFloat(),
                valueRange = 1500f..8000f,
                onValueChange = { onMinDwellTimeChange((it.roundToInt() / 500) * 500L) }
            )

            IntelWeightSliderRow(
                label = "কৃত্রিম এনগেজমেন্ট পেনাল্টি (Fraud Penalty)",
                valueText = "-${IslamicDateAndPrayerHelper.toBanglaDigits(fraudPenaltyWeight.roundToInt().toString())} পয়েন্ট",
                value = fraudPenaltyWeight.toFloat(),
                valueRange = 0f..70f,
                onValueChange = { onFraudPenaltyChange(it.roundToInt().toDouble()) }
            )

            IntelWeightSliderRow(
                label = "স্মার্ট নোটিফিকেশন ন্যূনতম ম্যাচ স্কোর",
                valueText = "${IslamicDateAndPrayerHelper.toBanglaDigits(smartNotificationThreshold.roundToInt().toString())}%",
                value = smartNotificationThreshold.toFloat(),
                valueRange = 30f..90f,
                onValueChange = { onSmartNotifThresholdChange(it.roundToInt().toDouble()) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onResetDefaults,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_reset_intelligence_config_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ডিফল্ট রিসেট", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onSave,
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = KksHeaderGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("admin_save_intelligence_config_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSubmitting) "সংরক্ষণ হচ্ছে..." else "নিয়ম সংরক্ষণ করুন",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun IntelligenceTrendingMonitorSection(
    performance: AlgorithmPerformanceSummary,
    postMetricsMap: Map<String, PostIntelligenceMetrics>,
    onToggleBoostPost: (FeedPost, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KksCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "প্রকৃত ট্রেন্ডিং ডিটেকশন নীতি",
                style = MaterialTheme.typography.titleSmall,
                color = KksHeaderGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "শুধুমাত্র লাইক বা সাধারণ ভিউ বেশি হলেই কোনো পোস্ট ট্রেন্ডিং হিসেবে গণ্য হয় না। ইউনিক পঠন সময় (Dwell Time), বিস্তারিত দেখা (Click), সংরক্ষণ (Bookmark), শেয়ার এবং প্রকৃত আবেদনের (Apply) হারের ওপর ভিত্তি করে ট্রেন্ডিং ভেলোসিটি নির্ধারিত হয়।",
                style = MaterialTheme.typography.bodySmall,
                color = QawmiTextDark
            )
        }
    }

    if (performance.topTrendingPosts.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KksCardBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = KksHeaderGreen,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "বর্তমানে পর্যাপ্ত হাই-ইনটেন্ট কনভার্সনসহ কোনো ট্রেন্ডিং পোস্ট নেই।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QawmiTextSecondary
                )
            }
        }
    } else {
        performance.topTrendingPosts.forEachIndexed { index, scored ->
            val m = postMetricsMap[scored.post.id]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KksCardBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = KksHeaderGreen
                        ) {
                            Text(
                                text = "#${IslamicDateAndPrayerHelper.toBanglaDigits((index + 1).toString())} ট্রেন্ডিং • স্কোর ${IslamicDateAndPrayerHelper.toBanglaDigits(scored.finalScore.roundToInt().toString())}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "ভেলোসিটি: ${IslamicDateAndPrayerHelper.toBanglaDigits(((scored.trendingVelocityScore * 10).roundToInt() / 10.0).toString())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = KksActiveGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = scored.post.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${scored.post.designation.ifBlank { "সাধারণ পোস্ট" }} • ${scored.post.district.ifBlank { "বাংলাদেশ" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ভিউ: ${IslamicDateAndPrayerHelper.toBanglaDigits((m?.uniqueViewCount ?: 0).toString())} • ক্লিক: ${IslamicDateAndPrayerHelper.toBanglaDigits((m?.uniqueClickCount ?: 0).toString())} • আবেদন: ${IslamicDateAndPrayerHelper.toBanglaDigits((m?.applyCount ?: 0).toString())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiTextDark
                        )
                        OutlinedButton(
                            onClick = { onToggleBoostPost(scored.post, !scored.post.isBoosted) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (scored.post.isBoosted) "Boost সরান" else "Boost করুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = KksHeaderGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntelligenceFraudAndAnomalySection(
    performance: AlgorithmPerformanceSummary,
    allPosts: List<FeedPost>,
    postMetricsMap: Map<String, PostIntelligenceMetrics>,
    onOverridePostAnomaly: (post: FeedPost, flagPost: Boolean, reasonBangla: String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KksCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = KksHeaderGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "অ্যান্টি-ফ্রড ও অ্যানোমালি ডিটেকশন সিস্টেম",
                    style = MaterialTheme.typography.titleSmall,
                    color = KksHeaderGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "• প্রতিটি ইউনিক ভিউয়ের জন্য ডিটারমিনিস্টিক রিসিট ({uid}_{eventType}) ও ন্যূনতম পঠন সময় যাচাই করা হয়।\n• ইউনিক ভিউ ছাড়া কৃত্রিম রিঅ্যাকশন বা বট ক্লিক শনাক্ত হলে স্বয়ংক্রিয়ভাবে অ্যানোমালি ফ্ল্যাগ ও র‍্যাঙ্কিং পেনাল্টি যুক্ত হয়।",
                style = MaterialTheme.typography.bodySmall,
                color = QawmiTextDark
            )
        }
    }

    if (performance.flaggedPosts.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KksCardBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = KksActiveGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "আলহামদুলিল্লাহ, কোনো কৃত্রিম এনগেজমেন্ট বা সন্দেহজনক পোস্ট পাওয়া যায়নি",
                        style = MaterialTheme.typography.titleSmall,
                        color = KksActiveGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "সকল সক্রিয় বিজ্ঞপ্তির এনগেজমেন্ট স্বাভাবিক ও যাচাইকৃত রয়েছে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                }
            }
        }
    } else {
        Text(
            text = "সন্দেহজনক / ফ্ল্যাগড পোস্ট তালিকা (${IslamicDateAndPrayerHelper.toBanglaDigits(performance.flaggedPosts.size.toString())})",
            style = MaterialTheme.typography.titleSmall,
            color = KksDangerRed,
            fontWeight = FontWeight.Bold
        )
        performance.flaggedPosts.forEach { (post, metrics) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KksDangerRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("admin_intel_flagged_post_${post.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F8))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GppBad,
                                contentDescription = null,
                                tint = KksDangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "অ্যানোমালি স্কোর: ${IslamicDateAndPrayerHelper.toBanglaDigits(metrics.anomalyScore.roundToInt().toString())}/১০০",
                                style = MaterialTheme.typography.labelMedium,
                                color = KksDangerRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KksDangerRed
                        ) {
                            Text(
                                text = "পেনাল্টি সক্রিয়",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "কারণ: ${metrics.flagReason.ifBlank { "কৃত্রিম এনগেজমেন্ট অনুপাত" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KksDangerRed
                    )
                    Text(
                        text = "ইউনিক ভিউ: ${metrics.uniqueViewCount} • ক্লিক: ${metrics.uniqueClickCount} • রিঅ্যাকশন: ${post.userReactions.size} • আবেদন: ${metrics.applyCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onOverridePostAnomaly(post, false, "") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("admin_intel_clear_flag_${post.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KksActiveGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ফ্ল্যাগ প্রত্যাহার করুন", color = KksActiveGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Recent Posts Inspection Table so Admin can manually flag or clear any post
    if (allPosts.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "সকল পোস্টের এনগেজমেন্ট ও অ্যান্টি-ফ্রড নিরীক্ষা",
            style = MaterialTheme.typography.titleSmall,
            color = KksHeaderGreen,
            fontWeight = FontWeight.Bold
        )
        allPosts.take(10).forEach { post ->
            val m = postMetricsMap[post.id]
            val isFlagged = m?.isFlagged == true
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KksCardBorder, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ভিউ: ${m?.uniqueViewCount ?: 0} • ক্লিক: ${m?.uniqueClickCount ?: 0} • আবেদন: ${m?.applyCount ?: 0} • রিঅ্যাকশন: ${post.userReactions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            onOverridePostAnomaly(
                                post,
                                !isFlagged,
                                if (!isFlagged) "কৃত্রিম এনগেজমেন্ট বা স্প্যাম সন্দেহে অ্যাডমিন পেনাল্টি" else ""
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isFlagged) KksActiveGreen else KksDangerRed)
                    ) {
                        Text(
                            text = if (isFlagged) "আন-ফ্ল্যাগ" else "ফ্ল্যাগ করুন",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFlagged) KksActiveGreen else KksDangerRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPillBadge(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) Color.White.copy(alpha = 0.18f) else Color(0x33FFCDD2)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp
        )
    }
}

@Composable
private fun IntelMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, KksCardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = QawmiTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = QawmiTextMuted,
                fontSize = 10.5.sp
            )
        }
    }
}

@Composable
private fun IntelModuleToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = QawmiMintSurface,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = KksHeaderGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiTextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KksActiveGreen
            )
        )
    }
}

@Composable
private fun IntelWeightSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = QawmiTextDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = QawmiMintSurface
            ) {
                Text(
                    text = valueText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = KksHeaderGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = KksHeaderGreen,
                activeTrackColor = KksActiveGreen,
                inactiveTrackColor = QawmiMintBorder
            )
        )
    }
}
