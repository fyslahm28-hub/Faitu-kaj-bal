package com.example.ui.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar
import java.util.TimeZone

enum class PrayerWidgetSize(
    val id: String,
    val titleBangla: String,
    val titleEnglish: String,
    val cellsLabelBangla: String,
    val cellsLabelEnglish: String
) {
    SMALL(
        id = "SMALL",
        titleBangla = "ছোট উইজেট (Small)",
        titleEnglish = "Small Widget",
        cellsLabelBangla = "৩×২ ঘর • সংক্ষিপ্ত ও স্পষ্ট",
        cellsLabelEnglish = "3×2 Cells • Compact View"
    ),
    MEDIUM(
        id = "MEDIUM",
        titleBangla = "মাঝারি উইজেট (Medium)",
        titleEnglish = "Medium Widget",
        cellsLabelBangla = "৪×২ ঘর • ৩ তারিখ, কাউন্টডাউন ও ৫ ওয়াক্ত",
        cellsLabelEnglish = "4×2 Cells • 3 Dates, Countdown & 5 Waqts"
    ),
    LARGE(
        id = "LARGE",
        titleBangla = "বড় উইজেট (Large)",
        titleEnglish = "Large Widget",
        cellsLabelBangla = "৪×৪ ঘর • পূর্ণাঙ্গ শুরু-শেষ সময়সূচি ও বিস্তারিত",
        cellsLabelEnglish = "4×4 Cells • Full Start-End Schedule & Details"
    )
}

/**
 * Small (ছোট) Home Screen Live Prayer Time Widget Provider.
 */
class PrayerTimeSmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        PrayerTimeWidgetManager.updateWidgetIds(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            size = PrayerWidgetSize.SMALL
        )
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PrayerTimeWidgetManager.updateAllWidgetsAndLockScreen(context)
    }
}

/**
 * Medium (মাঝারি) Home Screen Live Prayer Time Widget Provider.
 */
class PrayerTimeMediumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        PrayerTimeWidgetManager.updateWidgetIds(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            size = PrayerWidgetSize.MEDIUM
        )
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PrayerTimeWidgetManager.updateAllWidgetsAndLockScreen(context)
    }
}

/**
 * Large (বড়) Home Screen Live Prayer Time Widget Provider.
 */
class PrayerTimeLargeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        PrayerTimeWidgetManager.updateWidgetIds(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            size = PrayerWidgetSize.LARGE
        )
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PrayerTimeWidgetManager.updateAllWidgetsAndLockScreen(context)
    }
}

/**
 * Centralized manager for the standalone Live Prayer Time Home Screen Widgets (Small, Medium, Large),
 * direct Home Screen Pinned Shortcut, and Optional Lock Screen Live Prayer Time Notification.
 *
 * Strictly obeys Android widget update limits, uses zero floating overlays (no SYSTEM_ALERT_WINDOW),
 * and requires no background services.
 */
object PrayerTimeWidgetManager {

    const val LOCK_SCREEN_PRAYER_CHANNEL_ID = "qawmi_prayer_lockscreen_live_v1"
    const val LOCK_SCREEN_PRAYER_NOTIFICATION_ID = 786_950
    const val SHORTCUT_ID_PRAYER_TIMES = "qawmi_live_prayer_times_shortcut"

    private const val COLOR_GOLD = -0x2A51 // #FFD54F
    private const val COLOR_WHITE = Color.WHITE
    private const val COLOR_MINT_TEXT = -0x220C14 // #DDF3EC

    fun providerComponentForSize(context: Context, size: PrayerWidgetSize): ComponentName {
        val cls = when (size) {
            PrayerWidgetSize.SMALL -> PrayerTimeSmallWidgetProvider::class.java
            PrayerWidgetSize.MEDIUM -> PrayerTimeMediumWidgetProvider::class.java
            PrayerWidgetSize.LARGE -> PrayerTimeLargeWidgetProvider::class.java
        }
        return ComponentName(context.applicationContext, cls)
    }

    /**
     * Builds the [Intent] that opens the app directly to the current Prayer Times page
     * (`PrayerTimesAndAzanScreen`), without touching any other feature.
     */
    fun createOpenPrayerScreenIntent(context: Context, sourceTag: String = "widget"): Intent {
        return Intent(context.applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("qawmikormo://prayer_schedule/$sourceTag")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PrayerAzanManager.EXTRA_OPEN_PRAYER_SCREEN, true)
        }
    }

    fun createOpenPrayerPendingIntent(
        context: Context,
        requestCode: Int,
        sourceTag: String
    ): PendingIntent {
        val appContext = context.applicationContext
        val intent = createOpenPrayerScreenIntent(appContext, sourceTag)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(appContext, requestCode, intent, flags)
    }

    /**
     * Updates all Small, Medium, and Large Home Screen Prayer Widgets, the optional Lock Screen
     * Live Prayer Notification, and the dynamic launcher shortcut using the latest schedule.
     */
    fun updateAllWidgetsAndLockScreen(
        context: Context,
        calendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
    ) {
        try {
            val appContext = context.applicationContext
            IslamicDateAndPrayerHelper.initWithContext(appContext)
            val prefs = PrayerAzanManager.preferencesFlow.value
            val selectedDistrict = IslamicDateAndPrayerHelper.selectedDistrictFlow.value
            val schedule = PrayerAzanManager.computeDailyPrayerSchedule(
                districtBangla = selectedDistrict,
                preferences = prefs,
                calendar = calendar
            )

            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            if (appWidgetManager != null) {
                PrayerWidgetSize.entries.forEach { size ->
                    val component = providerComponentForSize(appContext, size)
                    val ids = runCatching { appWidgetManager.getAppWidgetIds(component) }
                        .getOrDefault(IntArray(0))
                    if (ids.isNotEmpty()) {
                        updateWidgetIds(
                            context = appContext,
                            appWidgetManager = appWidgetManager,
                            appWidgetIds = ids,
                            size = size,
                            precomputedSchedule = schedule,
                            preferences = prefs
                        )
                    }
                }
            }

            updateLockScreenPrayerNotification(
                context = appContext,
                schedule = schedule,
                preferences = prefs
            )
            updateDynamicPrayerShortcut(
                context = appContext,
                schedule = schedule,
                preferences = prefs
            )
        } catch (_: Throwable) {
        }
    }

    fun updateWidgetIds(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        size: PrayerWidgetSize,
        precomputedSchedule: ComputedDailyPrayerSchedule? = null,
        preferences: PrayerSystemPreferences? = null
    ) {
        if (appWidgetIds.isEmpty()) return
        try {
            val appContext = context.applicationContext
            IslamicDateAndPrayerHelper.initWithContext(appContext)
            val prefs = preferences ?: PrayerAzanManager.preferencesFlow.value
            val schedule = precomputedSchedule ?: PrayerAzanManager.computeDailyPrayerSchedule(
                districtBangla = IslamicDateAndPrayerHelper.selectedDistrictFlow.value,
                preferences = prefs,
                calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
            )
            val views = buildRemoteViews(
                context = appContext,
                size = size,
                schedule = schedule,
                preferences = prefs
            )
            for (widgetId in appWidgetIds) {
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        } catch (_: Throwable) {
        }
    }

    /**
     * Constructs the populated [RemoteViews] for Small, Medium, or Large widget sizes.
     * Exposed as public so Robolectric tests can directly inspect the rendered widget views.
     */
    fun buildRemoteViews(
        context: Context,
        size: PrayerWidgetSize,
        schedule: ComputedDailyPrayerSchedule = PrayerAzanManager.computeDailyPrayerSchedule(),
        preferences: PrayerSystemPreferences = PrayerAzanManager.preferencesFlow.value
    ): RemoteViews {
        val appContext = context.applicationContext
        val isBangla = preferences.language == PrayerUiLanguage.BANGLA
        val layoutRes = when (size) {
            PrayerWidgetSize.SMALL -> R.layout.widget_prayer_small
            PrayerWidgetSize.MEDIUM -> R.layout.widget_prayer_medium
            PrayerWidgetSize.LARGE -> R.layout.widget_prayer_large
        }

        val views = RemoteViews(appContext.packageName, layoutRes)

        // 1. District Name
        val districtTitle = when (size) {
            PrayerWidgetSize.SMALL -> if (isBangla) {
                "🕌 জেলা: ${schedule.locationNameBangla}"
            } else {
                "🕌 ${schedule.locationNameEnglish}"
            }
            PrayerWidgetSize.MEDIUM -> if (isBangla) {
                "🕌 জেলা: ${schedule.locationNameBangla} (${preferences.asrMadhhab.titleBangla.substringBefore(" ")})"
            } else {
                "🕌 District: ${schedule.locationNameEnglish}"
            }
            PrayerWidgetSize.LARGE -> if (isBangla) {
                "🕌 নামাজের সময়সূচি • জেলা: ${schedule.locationNameBangla}"
            } else {
                "🕌 Prayer Times • District: ${schedule.locationNameEnglish}"
            }
        }
        views.setTextViewText(R.id.widget_district_name, districtTitle)

        // 2. Bangla, English (Gregorian), and Hijri Dates
        val hijriText = "🌙 ${if (isBangla) schedule.hijriDateBangla else schedule.hijriDateEnglish}"
        val banglaDateText = "🗓️ ${if (isBangla) schedule.banglaCalendarDateBangla else schedule.banglaCalendarDateEnglish}"
        val englishDateText = if (isBangla) {
            "${schedule.gregorianDateBangla} • ${schedule.gregorianDateEnglish.substringBefore(" (")}"
        } else {
            schedule.gregorianDateEnglish
        }
        val combinedDatesText = "${schedule.banglaCalendarDateBangla} • ${schedule.gregorianDateBangla} (${schedule.gregorianDateEnglish.substringBefore(" (")})"

        views.setTextViewText(R.id.widget_hijri_date, hijriText)
        views.setTextViewText(R.id.widget_bangla_date, banglaDateText)
        views.setTextViewText(R.id.widget_english_date, englishDateText)
        views.setTextViewText(R.id.widget_dates_combined, combinedDatesText)

        // 3. Highlighted Next Prayer Name & Start Time
        val nextPrayerTitle = if (isBangla) {
            "পরবর্তী নামাজ: ${schedule.nextWaqtNameBangla} • ${schedule.nextWaqtStartBangla}"
        } else {
            "Next Prayer: ${schedule.nextWaqtNameEnglish} • ${schedule.nextWaqtStartEnglish}"
        }
        views.setTextViewText(R.id.widget_next_prayer_title, nextPrayerTitle)

        // 4. Live Countdown (Native Chronometer + Formatted Bangla/English Countdown Text)
        val remainingMillis = (schedule.totalRemainingSeconds.coerceAtLeast(1L)) * 1000L
        val chronometerBase = SystemClock.elapsedRealtime() + remainingMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            views.setChronometerCountDown(R.id.widget_countdown_chronometer, true)
        }
        views.setChronometer(
            R.id.widget_countdown_chronometer,
            chronometerBase,
            if (isBangla) "⏱ %s" else "⏱ %s",
            true
        )
        val countdownLabel = if (isBangla) {
            "কাউন্টডাউন: ${schedule.countdownBangla}"
        } else {
            "Countdown: ${schedule.countdownEnglish}"
        }
        views.setTextViewText(R.id.widget_countdown_text, countdownLabel)

        // 5. Current Waqt & Sunrise/Sunset/Sehri/Iftar Info (Medium & Large)
        if (size == PrayerWidgetSize.MEDIUM || size == PrayerWidgetSize.LARGE) {
            val currentWaqtLine = if (isBangla) {
                "${schedule.currentWaqtLabelBangla} (${schedule.currentWaqtRangeBangla})"
            } else {
                "${schedule.currentWaqtLabelEnglish} (${schedule.currentWaqtRangeEnglish})"
            }
            views.setTextViewText(R.id.widget_current_waqt_text, currentWaqtLine)
        }
        if (size == PrayerWidgetSize.LARGE) {
            val sunBadge = if (isBangla) {
                "সাহরি: ${schedule.sehriEndBangla.substringBefore(" ")} • ইফতার: ${schedule.iftarStartBangla.substringBefore(" ")}"
            } else {
                "Sehri: ${schedule.sehriEndEnglish} • Iftar: ${schedule.iftarStartEnglish}"
            }
            views.setTextViewText(R.id.widget_sun_strip_text, sunBadge)
        }

        // 6. All 5 Daily Waqts with Next Prayer Highlighting
        bindWaqtSlot(
            views = views,
            size = size,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.FAJR },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_fajr,
            nameId = R.id.widget_waqt_name_fajr,
            timeId = R.id.widget_waqt_time_fajr,
            badgeId = R.id.widget_waqt_badge_fajr
        )
        bindWaqtSlot(
            views = views,
            size = size,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.DHUHR },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_dhuhr,
            nameId = R.id.widget_waqt_name_dhuhr,
            timeId = R.id.widget_waqt_time_dhuhr,
            badgeId = R.id.widget_waqt_badge_dhuhr
        )
        bindWaqtSlot(
            views = views,
            size = size,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.ASR },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_asr,
            nameId = R.id.widget_waqt_name_asr,
            timeId = R.id.widget_waqt_time_asr,
            badgeId = R.id.widget_waqt_badge_asr
        )
        bindWaqtSlot(
            views = views,
            size = size,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.MAGHRIB },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_maghrib,
            nameId = R.id.widget_waqt_name_maghrib,
            timeId = R.id.widget_waqt_time_maghrib,
            badgeId = R.id.widget_waqt_badge_maghrib
        )
        bindWaqtSlot(
            views = views,
            size = size,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.ISHA },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_isha,
            nameId = R.id.widget_waqt_name_isha,
            timeId = R.id.widget_waqt_time_isha,
            badgeId = R.id.widget_waqt_badge_isha
        )

        // 7. Click PendingIntent to open directly to the current Prayer Times page
        val requestCode = 789_000 + size.ordinal
        val openPending = createOpenPrayerPendingIntent(
            context = appContext,
            requestCode = requestCode,
            sourceTag = "widget_${size.id.lowercase()}"
        )
        views.setOnClickPendingIntent(R.id.widget_root_container, openPending)
        views.setOnClickPendingIntent(R.id.widget_next_prayer_box, openPending)

        return views
    }

    private fun bindWaqtSlot(
        views: RemoteViews,
        size: PrayerWidgetSize,
        waqt: ComputedWaqtItem,
        isBangla: Boolean,
        boxId: Int,
        nameId: Int,
        timeId: Int,
        badgeId: Int
    ) {
        val isNext = waqt.isNextWaqt
        val isCurrent = waqt.isCurrentWaqt

        val baseName = if (isBangla) waqt.nameBangla else waqt.nameEnglish
        val displayName = when {
            size != PrayerWidgetSize.LARGE && isNext -> "★ $baseName"
            size == PrayerWidgetSize.LARGE && isNext -> "▶ $baseName"
            else -> baseName
        }

        val displayTime = when (size) {
            PrayerWidgetSize.SMALL -> if (isBangla) {
                waqt.startFormattedBangla.substringBefore(" ")
            } else {
                waqt.startFormattedEnglish
            }
            PrayerWidgetSize.MEDIUM -> if (isBangla) {
                waqt.startFormattedBangla
            } else {
                waqt.startFormattedEnglish
            }
            PrayerWidgetSize.LARGE -> if (isBangla) {
                "${waqt.startFormattedBangla} - ${waqt.endFormattedBangla}"
            } else {
                "${waqt.startFormattedEnglish} - ${waqt.endFormattedEnglish}"
            }
        }

        views.setTextViewText(nameId, displayName)
        views.setTextViewText(timeId, displayTime)

        val bgRes = if (isNext) {
            R.drawable.bg_prayer_widget_highlight_box
        } else {
            R.drawable.bg_prayer_widget_waqt_normal
        }
        views.setInt(boxId, "setBackgroundResource", bgRes)

        val nameColor = if (isNext) COLOR_GOLD else COLOR_MINT_TEXT
        val timeColor = if (isNext) COLOR_GOLD else COLOR_WHITE
        views.setTextColor(nameId, nameColor)
        views.setTextColor(timeId, timeColor)

        if (size == PrayerWidgetSize.LARGE) {
            if (isNext) {
                views.setViewVisibility(badgeId, View.VISIBLE)
                views.setTextViewText(badgeId, if (isBangla) "পরবর্তী ওয়াক্ত" else "NEXT")
            } else if (isCurrent) {
                views.setViewVisibility(badgeId, View.VISIBLE)
                views.setTextViewText(badgeId, if (isBangla) "চলমান" else "ACTIVE")
            } else {
                views.setViewVisibility(badgeId, View.GONE)
            }
        }
    }

    /**
     * Requests Android Launcher to pin the selected [PrayerWidgetSize] directly to the Home Screen.
     * Returns true if the launcher accepted the pin request dialog, false if unsupported.
     */
    fun requestPinPrayerWidget(context: Context, size: PrayerWidgetSize): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return false
                if (!appWidgetManager.isRequestPinAppWidgetSupported) {
                    return false
                }
                val provider = providerComponentForSize(context, size)
                val callbackIntent = createOpenPrayerScreenIntent(context, "pin_widget_${size.id.lowercase()}")
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val successCallback = PendingIntent.getActivity(
                    context.applicationContext,
                    789_100 + size.ordinal,
                    callbackIntent,
                    flags
                )
                appWidgetManager.requestPinAppWidget(provider, null, successCallback)
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Pins a standalone Home Screen Shortcut that opens exclusively the current Prayer Times page
     * (`PrayerTimesAndAzanScreen`), showing the selected district and next prayer.
     */
    fun requestPinPrayerPageShortcut(context: Context): Boolean {
        return try {
            val appContext = context.applicationContext
            val schedule = PrayerAzanManager.computeDailyPrayerSchedule()
            val shortcutIntent = createOpenPrayerScreenIntent(appContext, "pinned_shortcut")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val shortcutManager = appContext.getSystemService(ShortcutManager::class.java)
                    ?: return false
                if (!shortcutManager.isRequestPinShortcutSupported) {
                    return false
                }
                val shortLabel = "নামাজের সময় (${schedule.locationNameBangla})"
                val longLabel = "নামাজের সময়সূচি • ${schedule.locationNameBangla} (পরবর্তী: ${schedule.nextWaqtNameBangla} ${schedule.nextWaqtStartBangla})"
                val shortcutInfo = ShortcutInfo.Builder(appContext, SHORTCUT_ID_PRAYER_TIMES)
                    .setShortLabel(shortLabel)
                    .setLongLabel(longLabel)
                    .setIcon(Icon.createWithResource(appContext, R.mipmap.ic_launcher))
                    .setIntent(shortcutIntent)
                    .build()

                val callbackFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pinnedCallback = PendingIntent.getBroadcast(
                    appContext,
                    789_200,
                    Intent("com.example.action.PRAYER_SHORTCUT_PINNED"),
                    callbackFlags
                )
                shortcutManager.requestPinShortcut(shortcutInfo, pinnedCallback.intentSender)
            } else {
                @Suppress("DEPRECATION")
                val installIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                    putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                    putExtra(Intent.EXTRA_SHORTCUT_NAME, "নামাজের সময়সূচি (${schedule.locationNameBangla})")
                    putExtra(
                        Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(appContext, R.mipmap.ic_launcher)
                    )
                    putExtra("duplicate", false)
                }
                appContext.sendBroadcast(installIntent)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Keeps the dynamic launcher shortcut updated with the latest district and next prayer time.
     */
    private fun updateDynamicPrayerShortcut(
        context: Context,
        schedule: ComputedDailyPrayerSchedule,
        preferences: PrayerSystemPreferences
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            val appContext = context.applicationContext
            val shortcutManager = appContext.getSystemService(ShortcutManager::class.java) ?: return
            val isBangla = preferences.language == PrayerUiLanguage.BANGLA
            val shortLabel = if (isBangla) {
                "নামাজের সময় (${schedule.locationNameBangla})"
            } else {
                "Prayer Times (${schedule.locationNameEnglish})"
            }
            val longLabel = if (isBangla) {
                "পরবর্তী: ${schedule.nextWaqtNameBangla} (${schedule.nextWaqtStartBangla}) • ${schedule.locationNameBangla}"
            } else {
                "Next: ${schedule.nextWaqtNameEnglish} (${schedule.nextWaqtStartEnglish}) • ${schedule.locationNameEnglish}"
            }
            val shortcutInfo = ShortcutInfo.Builder(appContext, SHORTCUT_ID_PRAYER_TIMES)
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIcon(Icon.createWithResource(appContext, R.mipmap.ic_launcher))
                .setIntent(createOpenPrayerScreenIntent(appContext, "dynamic_shortcut"))
                .build()
            shortcutManager.dynamicShortcuts = listOf(shortcutInfo)
            runCatching { shortcutManager.updateShortcuts(listOf(shortcutInfo)) }
        } catch (_: Throwable) {
        }
    }

    /**
     * Posts or cancels the optional Lock Screen Live Prayer Time Notification.
     * Uses NotificationCompat.VISIBILITY_PUBLIC + live countdown chronometer + 5 waqt times + 3 dates.
     */
    @Suppress("MissingPermission")
    fun updateLockScreenPrayerNotification(
        context: Context,
        schedule: ComputedDailyPrayerSchedule = PrayerAzanManager.computeDailyPrayerSchedule(),
        preferences: PrayerSystemPreferences = PrayerAzanManager.preferencesFlow.value
    ) {
        val appContext = context.applicationContext
        val notificationManager = NotificationManagerCompat.from(appContext)

        if (!preferences.lockScreenNotificationEnabled) {
            runCatching { notificationManager.cancel(LOCK_SCREEN_PRAYER_NOTIFICATION_ID) }
            return
        }

        if (!QawmiNotificationHelper.hasNotificationPermission(appContext)) {
            return
        }

        try {
            ensureLockScreenChannelCreated(appContext)
            val isBangla = preferences.language == PrayerUiLanguage.BANGLA

            val openPending = createOpenPrayerPendingIntent(
                context = appContext,
                requestCode = LOCK_SCREEN_PRAYER_NOTIFICATION_ID,
                sourceTag = "lock_screen_notification"
            )

            val title = if (isBangla) {
                "🕌 ${schedule.locationNameBangla} • পরবর্তী: ${schedule.nextWaqtNameBangla} (${schedule.nextWaqtStartBangla})"
            } else {
                "🕌 ${schedule.locationNameEnglish} • Next: ${schedule.nextWaqtNameEnglish} (${schedule.nextWaqtStartEnglish})"
            }

            val fiveWaqtsCompact = schedule.waqts.joinToString(" | ") { w ->
                val marker = if (w.isNextWaqt) "▶" else ""
                if (isBangla) {
                    "$marker${w.nameBangla}: ${w.startFormattedBangla.substringBefore(" ")}"
                } else {
                    "$marker${w.nameEnglish}: ${w.startFormattedEnglish}"
                }
            }

            val datesLine = if (isBangla) {
                "🌙 ${schedule.hijriDateBangla} • 🗓️ ${schedule.banglaCalendarDateBangla} • ${schedule.gregorianDateBangla}"
            } else {
                "🌙 ${schedule.hijriDateEnglish} • 🗓️ ${schedule.banglaCalendarDateEnglish} • ${schedule.gregorianDateEnglish}"
            }

            val shortBody = if (isBangla) {
                "বাকি: ${schedule.countdownBangla} • $fiveWaqtsCompact"
            } else {
                "Remaining: ${schedule.countdownEnglish} • $fiveWaqtsCompact"
            }

            val expandedBigText = buildString {
                append(datesLine)
                append("\n")
                if (isBangla) {
                    append("পরবর্তী ওয়াক্ত: ${schedule.nextWaqtNameBangla} (${schedule.nextWaqtStartBangla}) • ${schedule.countdownBangla}\n")
                } else {
                    append("Next Prayer: ${schedule.nextWaqtNameEnglish} (${schedule.nextWaqtStartEnglish}) • ${schedule.countdownEnglish}\n")
                }
                append(fiveWaqtsCompact)
            }

            val targetEpochMillis = System.currentTimeMillis() + (schedule.totalRemainingSeconds.coerceAtLeast(1L) * 1000L)

            val builder = NotificationCompat.Builder(appContext, LOCK_SCREEN_PRAYER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_qawmi_notification)
                .setContentTitle(title)
                .setContentText(shortBody)
                .setSubText(if (isBangla) schedule.hijriDateBangla else schedule.hijriDateEnglish)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBigText))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(true)
                .setWhen(targetEpochMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setContentIntent(openPending)

            notificationManager.notify(LOCK_SCREEN_PRAYER_NOTIFICATION_ID, builder.build())
        } catch (_: Throwable) {
        }
    }

    private fun ensureLockScreenChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                LOCK_SCREEN_PRAYER_CHANNEL_ID,
                "লক স্ক্রিন লাইভ নামাজের সময়সূচি",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "লক স্ক্রিনে ও নোটিফিকেশন বারে পাঁচ ওয়াক্ত নামাজের সময়, তারিখ ও পরবর্তী নামাজের লাইভ কাউন্টডাউন"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }
}
