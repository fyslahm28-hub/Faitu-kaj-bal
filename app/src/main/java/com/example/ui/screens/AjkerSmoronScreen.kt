package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.IslamicStarEmblem
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.util.AjkerSmoronHelper
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.util.IslamicReminderItem

@Composable
fun AjkerSmoronScreen(
    currentUserId: String,
    serverReceiptsMap: Map<String, Long>,
    focusedReminderId: String? = null,
    onConsumeFocusedReminder: () -> Unit = {},
    onSyncAssignmentToServer: (dateKey: String, reminderId: String) -> Unit = { _, _ -> },
    onSyncAmolCompletedToServer: (dateKey: String) -> Unit = {},
    onShowFeedback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val todayDateKey = remember { AjkerSmoronHelper.currentBangladeshDateKey() }
    val hijriDate = remember { IslamicDateAndPrayerHelper.getFormattedHijriDate() }
    val gregorianDate = remember { IslamicDateAndPrayerHelper.getFormattedGregorianDate() }

    val smoronState by AjkerSmoronHelper.smoronStateFlow.collectAsState()

    var activeReminder by remember(currentUserId, todayDateKey) {
        mutableStateOf(
            AjkerSmoronHelper.getOrAssignTodayReminder(
                context = context,
                userId = currentUserId,
                dateKey = todayDateKey
            )
        )
    }

    LaunchedEffect(currentUserId, serverReceiptsMap, todayDateKey) {
        val synced = AjkerSmoronHelper.syncFromServerReceipts(
            context = context,
            userId = currentUserId,
            receiptTimestamps = serverReceiptsMap,
            dateKey = todayDateKey
        )
        if (focusedReminderId.isNullOrBlank()) {
            activeReminder = synced
        }
        onSyncAssignmentToServer(todayDateKey, synced.id)
    }

    LaunchedEffect(focusedReminderId) {
        val targetId = focusedReminderId?.trim().orEmpty()
        if (targetId.isNotBlank()) {
            AjkerSmoronHelper.findReminderById(targetId)?.let { matched ->
                activeReminder = matched
            }
            onConsumeFocusedReminder()
        }
    }

    var selectedCategory by rememberSaveable { mutableStateOf("সকল") }
    var showHistorySection by rememberSaveable { mutableStateOf(true) }

    val isTodayAmolDone = smoronState.completedAmolDates.contains(todayDateKey) ||
        AjkerSmoronHelper.getCompletedAmolDates(context, currentUserId).contains(todayDateKey)

    val notifEnabled = smoronState.notificationsEnabled
    val preferredHour = smoronState.preferredHour24
    val preferredMinute = smoronState.preferredMinute
    val isTodayPushDelivered = AjkerSmoronHelper.hasPushBeenSentForDate(context, currentUserId, todayDateKey)

    val historyReminders = remember(smoronState.deliveredHistoryMap, serverReceiptsMap, activeReminder) {
        val combinedMap = AjkerSmoronHelper.loadLocalHistoryMap(context, currentUserId).toMutableMap()
        smoronState.deliveredHistoryMap.forEach { (k, v) ->
            combinedMap[k] = maxOf(combinedMap[k] ?: 0L, v)
        }
        combinedMap.entries
            .sortedByDescending { it.value }
            .mapNotNull { (remId, _) -> AjkerSmoronHelper.findReminderById(remId) }
    }

    val filteredCollection = remember(selectedCategory) {
        if (selectedCategory == "সকল") {
            AjkerSmoronHelper.allReminders
        } else {
            AjkerSmoronHelper.allReminders.filter { it.categoryBangla == selectedCategory }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ajker_smoron_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero Card: আজকের স্মরণ (Today's Personalized Islamic Reminder)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, QawmiGold, RoundedCornerShape(18.dp))
                    .testTag("ajker_smoron_hero_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = QawmiDarkGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IslamicStarEmblem(size = 18.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আজকের স্মরণ",
                                style = MaterialTheme.typography.titleMedium,
                                color = QawmiGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF165B48),
                            modifier = Modifier.border(1.dp, QawmiGold.copy(alpha = 0.6f), RoundedCornerShape(50))
                        ) {
                            Text(
                                text = activeReminder.categoryBangla,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "$hijriDate • $gregorianDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB5DCD0)
                    )

                    Text(
                        text = activeReminder.titleBangla,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )

                    // Main Reminder Message Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0A4B39),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1D6E57), RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            text = "“${activeReminder.messageBangla}”",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                                .testTag("ajker_smoron_active_message"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        )
                    }

                    // Authentic Quran / Hadith Quote & Source Citation
                    if (activeReminder.quoteBangla.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF125A45),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = QawmiGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "কুরআন ও সুন্নাহর দলিল:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = QawmiGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = activeReminder.quoteBangla,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE8F7F1),
                                    lineHeight = 21.sp
                                )
                                Text(
                                    text = "সূত্র: ${activeReminder.sourceReferenceBangla}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QawmiGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("ajker_smoron_active_source")
                                )
                            }
                        }
                    }

                    // Daily Practical Amol
                    if (activeReminder.dailyAmolBangla.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF093E30),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, QawmiGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = QawmiGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "আজকের করণীয় আমল",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = QawmiGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeReminder.dailyAmolBangla,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                AjkerSmoronHelper.markTodayAmolCompleted(context, currentUserId, todayDateKey)
                                onSyncAmolCompletedToServer(todayDateKey)
                                onShowFeedback("আলহামদুলিল্লাহ! আল্লাহ তাআলা আপনার আজকের আমল কবুল করুন।")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTodayAmolDone) Color(0xFF2E7D32) else QawmiGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ajker_smoron_amol_done_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isTodayAmolDone) Color.White else QawmiDarkGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTodayAmolDone) "আমল সম্পন্ন" else "আমল করেছি",
                                color = if (isTodayAmolDone) Color.White else QawmiDarkGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val nextReminder = AjkerSmoronHelper.rotateToNextNonRepeatingReminder(
                                    context = context,
                                    userId = currentUserId,
                                    currentReminderId = activeReminder.id,
                                    dateKey = todayDateKey
                                )
                                activeReminder = nextReminder
                                onSyncAssignmentToServer(todayDateKey, nextReminder.id)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ajker_smoron_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = QawmiGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "পরবর্তী স্মরণ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Automated Daily Push Notification & Server Deduplication Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp))
                    .testTag("ajker_smoron_notification_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = QawmiMintSurface,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = QawmiDeepGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "প্রতিদিন স্বয়ংক্রিয় Push Notification",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QawmiTextDark,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "প্রতিদিন নির্ধারিত সময়ে সর্বোচ্চ ১টি ইসলামিক স্মরণবার্তা",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QawmiTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Switch(
                            checked = notifEnabled,
                            onCheckedChange = { enabled ->
                                AjkerSmoronHelper.setNotificationEnabled(context, enabled)
                                onShowFeedback(
                                    if (enabled) {
                                        "আলহামদুলিল্লাহ, প্রতিদিনের স্বয়ংক্রিয় ‘আজকের স্মরণ’ নোটিফিকেশন চালু করা হয়েছে।"
                                    } else {
                                        "‘আজকের স্মরণ’ দৈনিক নোটিফিকেশন বন্ধ করা হয়েছে।"
                                    }
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = QawmiDeepGreen
                            ),
                            modifier = Modifier.testTag("ajker_smoron_notif_switch")
                        )
                    }

                    HorizontalDivider(color = QawmiMintSurface)

                    // Time Selection Row (Daytime or Evening)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "নোটিফিকেশনের নির্ধারিত সময়: ${AjkerSmoronHelper.formatHourBangla(preferredHour, preferredMinute)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val schedulePresets = listOf(
                        8 to "সকাল ০৮:০০",
                        14 to "দুপুর ০২:০০",
                        17 to "বিকাল ০৫:০০",
                        21 to "রাত ০৯:০০"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        schedulePresets.forEach { (hour24, label) ->
                            val selected = preferredHour == hour24
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) QawmiDeepGreen else QawmiMintSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        AjkerSmoronHelper.setPreferredNotificationTime(context, hour24, 0)
                                        onShowFeedback("নোটিফিকেশনের সময় ‘$label’ নির্ধারণ করা হয়েছে।")
                                    }
                                    .testTag("smoron_time_preset_$hour24")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color.White else QawmiTextDark,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Server History & Daily Deduplication Status Strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = QawmiMintSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = QawmiDeepGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "পঠিত স্মরণ ইতিহাস: ${IslamicDateAndPrayerHelper.toBanglaDigits(historyReminders.size.toString())}/${IslamicDateAndPrayerHelper.toBanglaDigits(AjkerSmoronHelper.allReminders.size.toString())} টি",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = QawmiTextDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = if (isTodayPushDelivered) {
                                    "আজকের পুশ প্রেরিত ✓"
                                } else {
                                    "দৈনিক সর্বোচ্চ ১টি পুশ"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Previously Shown Reminders History (পুনরাবৃত্তি রোধ ও পূর্বে প্রাপ্ত স্মরণবার্তার ইতিহাস)
        if (historyReminders.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHistorySection = !showHistorySection },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = QawmiDeepGreen,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "আপনার পূর্বে প্রাপ্ত স্মরণবার্তার ইতিহাস (${IslamicDateAndPrayerHelper.toBanglaDigits(historyReminders.size.toString())})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = QawmiDeepGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (showHistorySection) "সংক্ষিপ্ত করুন" else "সব দেখুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showHistorySection) {
                            Text(
                                text = "পুনরাবৃত্তি এড়াতে আপনার পঠিত ও প্রাপ্ত বার্তাগুলো সার্ভারে সংরক্ষিত থাকে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = QawmiTextSecondary,
                                fontSize = 11.5.sp
                            )
                            historyReminders.take(5).forEach { histItem ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = QawmiMintSurface.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { activeReminder = histItem }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = histItem.titleBangla,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = QawmiDeepGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = histItem.categoryBangla,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = QawmiTextSecondary
                                            )
                                        }
                                        Text(
                                            text = histItem.messageBangla,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QawmiTextDark,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = "সূত্র: ${histItem.sourceReferenceBangla}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = QawmiTextSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Categorized Authentic Islamic Reminder Collection Header & Filter Chips
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "বিষয়ভিত্তিক স্মরণ ও নসিহত ভান্ডার",
                    style = MaterialTheme.typography.titleMedium,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AjkerSmoronHelper.allCategoriesBangla.forEach { cat ->
                        val selected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) QawmiDeepGreen else Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(
                                    width = 1.dp,
                                    color = if (selected) QawmiDeepGreen else QawmiMintBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { selectedCategory = cat }
                                .testTag("smoron_category_chip_$cat")
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) Color.White else QawmiTextDark,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 5. List of Authentic Reminders in Selected Category
        items(filteredCollection, key = { it.id }) { item ->
            SmoronCollectionCard(
                item = item,
                isCurrentlyFeatured = item.id == activeReminder.id,
                onSelect = { activeReminder = item }
            )
        }
    }
}

@Composable
private fun SmoronCollectionCard(
    item: IslamicReminderItem,
    isCurrentlyFeatured: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentlyFeatured) 1.5.dp else 1.dp,
                color = if (isCurrentlyFeatured) QawmiDeepGreen else QawmiMintBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .testTag("smoron_collection_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyFeatured) QawmiMintSurface else Color.White
        )
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
                    IslamicStarEmblem(size = 14.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.titleBangla,
                        style = MaterialTheme.typography.titleSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = QawmiMintSurface
                ) {
                    Text(
                        text = item.categoryBangla,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    )
                }
            }

            Text(
                text = item.messageBangla,
                style = MaterialTheme.typography.bodyMedium,
                color = QawmiTextDark,
                lineHeight = 21.sp
            )

            if (item.quoteBangla.isNotBlank()) {
                Text(
                    text = item.quoteBangla,
                    style = MaterialTheme.typography.bodySmall,
                    color = QawmiTextSecondary,
                    lineHeight = 19.sp
                )
            }

            Text(
                text = "দলিল ও সূত্র: ${item.sourceReferenceBangla}",
                style = MaterialTheme.typography.labelSmall,
                color = QawmiDeepGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
