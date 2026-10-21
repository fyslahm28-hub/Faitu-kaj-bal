package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.IslamicStarEmblem
import com.example.ui.components.MosqueHeroIllustration
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.viewmodel.FeedFilterTab
import com.example.ui.viewmodel.MainDestination

@Composable
fun HomeScreen(
    onNavigate: (MainDestination) -> Unit,
    onOpenFeedFilter: (FeedFilterTab) -> Unit
) {
    val dhakaPrayer = remember { IslamicDateAndPrayerHelper.divisionsSchedule.first() }
    val todayHadith = remember { IslamicDateAndPrayerHelper.getTodayHadith() }
    val hijriDate = remember { IslamicDateAndPrayerHelper.getFormattedHijriDate() }
    val gregorianDate = remember { IslamicDateAndPrayerHelper.getFormattedGregorianDate() }
    val banglaDay = remember { IslamicDateAndPrayerHelper.getFormattedBanglaDayOfWeek() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Hero Banner ("বাংলাদেশের কওমি মাদরাসা খেদমতের বিশ্বস্ত মাধ্যম")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MosqueHeroIllustration(
                        modifier = Modifier
                            .size(width = 122.dp, height = 88.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "বাংলাদেশের কওমি মাদরাসা\nখেদমতের বিশ্বস্ত মাধ্যম",
                            style = MaterialTheme.typography.titleMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "শিক্ষক খুঁজুন, চাকরি খুঁজুন, খেদমত এগিয়ে নিন",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. Row 1 of 2x3 Grid: আবেদন প্রকাশ করুন | বিজ্ঞপ্তি দিন
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeActionSquareCard(
                    title = "আবেদন প্রকাশ করুন",
                    subtitle = "নিজের যোগ্যতা ও অভিজ্ঞতা নিয়ে আবেদন করুন",
                    icon = Icons.Default.EditNote,
                    testTag = "home_card_publish_application",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(MainDestination.PUBLISH_APPLICATION) }
                )
                HomeActionSquareCard(
                    title = "বিজ্ঞপ্তি দিন",
                    subtitle = "মাদরাসার নিয়োগ বিজ্ঞপ্তি প্রকাশ করুন",
                    icon = Icons.Default.Campaign,
                    testTag = "home_card_publish_circular",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(MainDestination.PUBLISH_CIRCULAR) }
                )
            }
        }

        // 3. Row 2 of 2x3 Grid: অন্যদের আবেদন দেখুন | আমার প্রোফাইল
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeActionSquareCard(
                    title = "অন্যদের আবেদন দেখুন",
                    subtitle = "অন্যান্য প্রার্থীদের আবেদন ও প্রোফাইল দেখুন",
                    icon = Icons.Default.Groups,
                    testTag = "home_card_view_applications",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenFeedFilter(FeedFilterTab.APPLICATIONS) }
                )
                HomeActionSquareCard(
                    title = "আমার প্রোফাইল",
                    subtitle = "নিজের তথ্য, যোগ্যতা ও অভিজ্ঞতা আপডেট করুন",
                    icon = Icons.Default.Person,
                    testTag = "home_card_my_profile",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(MainDestination.PROFILE) }
                )
            }
        }

        // 4. Row 3 of 2x3 Grid: পছন্দের খেদমত | যাচাইকৃত মাদরাসা
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeActionSquareCard(
                    title = "পছন্দের খেদমত",
                    subtitle = "পছন্দের চাকরি সংরক্ষণ করে পরে আবেদন করুন",
                    icon = Icons.Default.Bookmark,
                    testTag = "home_card_bookmarks",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(MainDestination.BOOKMARKS) }
                )
                HomeActionSquareCard(
                    title = "যাচাইকৃত মাদরাসা",
                    subtitle = "যাচাইকৃত মাদরাসাগুলো আলাদাভাবে দেখুন",
                    icon = Icons.Default.VerifiedUser,
                    testTag = "home_card_verified_madrasas",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(MainDestination.VERIFIED_MADRASAS) }
                )
            }
        }

        // 5. Full-width Card: মাদরাসা
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigate(MainDestination.MADRASA) }
                    .testTag("home_card_madrasa_directory"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = QawmiDeepGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "মাদরাসা",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "মাদরাসা",
                            style = MaterialTheme.typography.titleMedium,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "মাদরাসার তথ্য, অবস্থান ও বিশেষ সুবিধা দেখুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = QawmiTextDark
                    )
                }
            }
        }

        // 6. Dark Emerald Prayer Times Card: নামাজের সময়সূচী (ঢাকা)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_prayer_times_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiDarkGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = QawmiGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "নামাজের সময়সূচী (ঢাকা)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onNavigate(MainDestination.PRAYER_SCHEDULE) }
                                .testTag("full_prayer_schedule_button"),
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF165B48)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "সম্পূর্ণ সময়সূচী",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDDF3EC)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFFDDF3EC),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dhakaPrayer.waqts.forEach { waqt ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF1D6E57),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0A4B39)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = waqt.nameBangla,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFDDF3EC)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = waqt.timeBangla,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = waqt.period,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB5DCD0),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Today's Date Card (আজকের তারিখ)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "আজকের তারিখ",
                                style = MaterialTheme.typography.labelMedium,
                                color = QawmiTextSecondary
                            )
                            Text(
                                text = hijriDate,
                                style = MaterialTheme.typography.titleSmall,
                                color = QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = gregorianDate,
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiTextDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = banglaDay,
                            style = MaterialTheme.typography.labelMedium,
                            color = QawmiTextSecondary
                        )
                    }
                }
            }
        }

        // 8. Daily Hadith Card (আজকের বাণী)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IslamicStarEmblem(size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "আজকের বাণী",
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = todayHadith.textBangla,
                        style = MaterialTheme.typography.bodyMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = todayHadith.referenceBangla,
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 9. Bottom Banner Card ("খেদমতই আমাদের লক্ষ্য")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFD2EFE4), Color(0xFFE5F7F0))
                            )
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MosqueHeroIllustration(
                        modifier = Modifier
                            .size(width = 110.dp, height = 74.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IslamicStarEmblem(size = 16.dp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "খেদমতই আমাদের লক্ষ্য",
                                style = MaterialTheme.typography.titleMedium,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            IslamicStarEmblem(size = 16.dp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "কওমি মাদরাসার উন্নয়নে\nএকসাথে এগিয়ে চলি",
                            style = MaterialTheme.typography.bodySmall,
                            color = QawmiTextDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeActionSquareCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = QawmiDeepGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = QawmiTextDark
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = QawmiTextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = QawmiTextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}
