package com.example.ui.util

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
        titleBangla = "মিনি উইজেট (২×১)",
        titleEnglish = "Mini Widget (2×1)",
        cellsLabelBangla = "২×১ ঘর • সংক্ষিপ্ত ও স্পষ্ট",
        cellsLabelEnglish = "2×1 Cells • Compact View"
    ),
    MEDIUM(
        id = "MEDIUM",
        titleBangla = "মিনি উইজেট (২×২)",
        titleEnglish = "Mini Widget (2×2)",
        cellsLabelBangla = "২×২ ঘর • ৩ ভাষায় তারিখ, কাউন্টডাউন ও ৫ ওয়াক্ত",
        cellsLabelEnglish = "2×2 Cells • 3 Dates, Countdown & 5 Waqts"
    ),
    LARGE(
        id = "LARGE",
        titleBangla = "লাইভ উইজেট (৪×২)",
        titleEnglish = "Live Widget (4×2)",
        cellsLabelBangla = "৪×২ ঘর • পূর্ণাঙ্গ এক নজরে সময়সূচি",
        cellsLabelEnglish = "4×2 Cells • Full Compact Schedule"
    )
}

/**
 * 2×1 Mini Home Screen Live Prayer Time Widget Provider.
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
 * 2×2 Mini Home Screen Live Prayer Time Widget Provider.
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
 * 4×2 Compact Home Screen Live Prayer Time Widget Provider.
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
 * Centralized manager for the Mini Live Prayer Times Android Home Screen Widget.
 *
 * Strictly renders as a native Launcher Home Screen Widget (`AppWidgetProvider`) alongside app icons,
 * never posts to the Notification Bar/Panel, uses zero floating overlays, and requires no background service.
 */
object PrayerTimeWidgetManager {

    const val LOCK_SCREEN_PRAYER_CHANNEL_ID = "qawmi_prayer_lockscreen_live_v1"
    const val LOCK_SCREEN_PRAYER_NOTIFICATION_ID = 786_950
    const val SHORTCUT_ID_PRAYER_TIMES = "qawmi_live_prayer_times_shortcut"

    private val COLOR_DEEP_GREEN = Color.parseColor("#084B2A")
    private val COLOR_DARK_TEXT = Color.parseColor("#0B3D24")
    private val COLOR_HIGHLIGHT_GREEN = Color.parseColor("#063B20")

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
     * Updates all Home Screen Prayer Widgets and dynamic launcher shortcut using the latest schedule.
     * Ensures no notification is shown in the Notification Bar/Panel.
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

            // Ensure no notification is ever left in the Notification Bar/Panel
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
     * Constructs the populated [RemoteViews] for the Mini Live Prayer Times Home Screen Widget.
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

        // 1. Top-Left: Current Local Time (12-Hour AM/PM format, e.g. "08:56 AM") + Short Date & District Pill
        val currentTime12h = schedule.currentTime12hEnglish.ifBlank {
            schedule.currentTimeEnglish
        }
        views.setTextViewText(R.id.widget_current_time_text, currentTime12h)

        val shortDayDate = schedule.shortDayDateEnglish.ifBlank { "Sat, Oct 31" }
        val districtPillText = if (isBangla) {
            "$shortDayDate • ${schedule.locationNameBangla}"
        } else {
            "$shortDayDate • ${schedule.locationNameEnglish}"
        }
        views.setTextViewText(R.id.widget_district_name, districtPillText)

        // 2. Top-Right: "পরবর্তী নামাজ" badge, Next Prayer Name (e.g. "যোহর"), "( শুরু হতে বাকি )", and Live Countdown ("⏱ 03:24:37")
        views.setTextViewText(
            R.id.widget_next_prayer_badge,
            if (isBangla) "🕌 পরবর্তী নামাজ" else "🕌 Next Prayer"
        )
        val nextPrayerName = if (isBangla) {
            schedule.nextWaqtNameBangla
        } else {
            schedule.nextWaqtNameEnglish
        }
        views.setTextViewText(R.id.widget_next_prayer_title, nextPrayerName)
        views.setTextViewText(
            R.id.widget_current_waqt_text,
            if (isBangla) "( শুরু হতে বাকি )" else "( Remaining )"
        )

        val remainingMillis = (schedule.totalRemainingSeconds.coerceAtLeast(1L)) * 1000L
        val chronometerBase = SystemClock.elapsedRealtime() + remainingMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            views.setChronometerCountDown(R.id.widget_countdown_chronometer, true)
        }
        views.setChronometer(
            R.id.widget_countdown_chronometer,
            chronometerBase,
            "⏱ %s",
            true
        )

        val digitalCountdown = schedule.countdownDigitalEnglish.ifBlank {
            String.format(
                java.util.Locale.ENGLISH,
                "%02d:%02d:%02d",
                schedule.remainingHours,
                schedule.remainingMinutes,
                schedule.remainingSeconds
            )
        }
        views.setTextViewText(R.id.widget_countdown_text, "⏱ $digitalCountdown")

        // 3. Middle Row: All 5 Daily Waqts in a Single Horizontal Row with 12-Hour AM/PM Times
        bindWaqtSlot(
            views = views,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.FAJR },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_fajr,
            nameId = R.id.widget_waqt_name_fajr,
            timeId = R.id.widget_waqt_time_fajr,
            badgeId = R.id.widget_waqt_badge_fajr
        )
        bindWaqtSlot(
            views = views,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.DHUHR },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_dhuhr,
            nameId = R.id.widget_waqt_name_dhuhr,
            timeId = R.id.widget_waqt_time_dhuhr,
            badgeId = R.id.widget_waqt_badge_dhuhr
        )
        bindWaqtSlot(
            views = views,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.ASR },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_asr,
            nameId = R.id.widget_waqt_name_asr,
            timeId = R.id.widget_waqt_time_asr,
            badgeId = R.id.widget_waqt_badge_asr
        )
        bindWaqtSlot(
            views = views,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.MAGHRIB },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_maghrib,
            nameId = R.id.widget_waqt_name_maghrib,
            timeId = R.id.widget_waqt_time_maghrib,
            badgeId = R.id.widget_waqt_badge_maghrib
        )
        bindWaqtSlot(
            views = views,
            waqt = schedule.waqts.first { it.key == PrayerWaqtKey.ISHA },
            isBangla = isBangla,
            boxId = R.id.widget_waqt_box_isha,
            nameId = R.id.widget_waqt_name_isha,
            timeId = R.id.widget_waqt_time_isha,
            badgeId = R.id.widget_waqt_badge_isha
        )

        // 4. Bottom Section: Mandatory 3-Language Dates (Bangla, English, Arabic)
        val banglaDateLine = schedule.widgetDateLineBangla.ifBlank {
            "${schedule.hijriDateBangla} | ${schedule.banglaCalendarDateBangla} | ${schedule.gregorianDateBangla.substringBefore(" (")}"
        }
        val englishDateLine = schedule.widgetDateLineEnglish.ifBlank {
            "${schedule.gregorianDateEnglish.substringBefore(" (")} | ${schedule.hijriDateEnglish}"
        }
        val arabicDateLine = schedule.widgetDateLineArabic.ifBlank {
            "${schedule.gregorianDateArabic} | ${schedule.hijriDateArabic}"
        }

        views.setTextViewText(R.id.widget_bangla_date, banglaDateLine)
        views.setTextViewText(R.id.widget_english_date, englishDateLine)
        views.setTextViewText(R.id.widget_arabic_date, arabicDateLine)
        views.setTextViewText(R.id.widget_hijri_date, schedule.hijriDateBangla)
        views.setTextViewText(
            R.id.widget_dates_combined,
            "$banglaDateLine • $englishDateLine • $arabicDateLine"
        )

        // 5. Click PendingIntent to open directly to the current Prayer Times page
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
        waqt: ComputedWaqtItem,
        isBangla: Boolean,
        boxId: Int,
        nameId: Int,
        timeId: Int,
        badgeId: Int
    ) {
        val isNext = waqt.isNextWaqt
        val baseName = if (isBangla) waqt.nameBangla else waqt.nameEnglish
        // Show clean waqt name, and subtle highlight on next prayer slot
        views.setTextViewText(nameId, baseName)
        // Always show 12-Hour AM/PM format (e.g. "05:07 AM", "11:45 AM", "03:38 PM", "05:27 PM", "06:47 PM")
        views.setTextViewText(timeId, waqt.startFormattedEnglish)

        val bgRes = if (isNext) {
            R.drawable.bg_prayer_widget_highlight_box
        } else {
            R.drawable.bg_prayer_widget_waqt_normal
        }
        views.setInt(boxId, "setBackgroundResource", bgRes)

        val nameColor = if (isNext) COLOR_HIGHLIGHT_GREEN else COLOR_DEEP_GREEN
        val timeColor = if (isNext) COLOR_HIGHLIGHT_GREEN else COLOR_DARK_TEXT
        views.setTextColor(nameId, nameColor)
        views.setTextColor(timeId, timeColor)
        views.setViewVisibility(badgeId, View.GONE)
    }

    /**
     * Requests Android Launcher to pin the selected [PrayerWidgetSize] directly to the Home Screen.
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
     * Ensures the widget NEVER posts any item to the Notification Bar or Notification Panel.
     */
    fun updateLockScreenPrayerNotification(
        context: Context,
        @Suppress("UNUSED_PARAMETER") schedule: ComputedDailyPrayerSchedule = PrayerAzanManager.computeDailyPrayerSchedule(),
        @Suppress("UNUSED_PARAMETER") preferences: PrayerSystemPreferences = PrayerAzanManager.preferencesFlow.value
    ) {
        runCatching {
            NotificationManagerCompat.from(context.applicationContext)
                .cancel(LOCK_SCREEN_PRAYER_NOTIFICATION_ID)
        }
    }
}
