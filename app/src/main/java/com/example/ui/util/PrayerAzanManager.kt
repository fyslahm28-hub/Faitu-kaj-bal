package com.example.ui.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class PrayerWaqtKey(
    val id: String,
    val nameBangla: String,
    val nameEnglish: String,
    val alarmCodeOffset: Int
) {
    FAJR("FAJR", "ফজর", "Fajr", 1),
    DHUHR("DHUHR", "যোহর", "Dhuhr", 2),
    ASR("ASR", "আসর", "Asr", 3),
    MAGHRIB("MAGHRIB", "মাগরিব", "Maghrib", 4),
    ISHA("ISHA", "এশা", "Isha", 5);

    companion object {
        fun fromId(id: String?): PrayerWaqtKey {
            return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: FAJR
        }
    }
}

enum class AzanSoundType(
    val id: String,
    val titleBangla: String,
    val titleEnglish: String,
    val subtitleBangla: String,
    val subtitleEnglish: String
) {
    FULL_AZAN(
        id = "FULL_AZAN",
        titleBangla = "সম্পূর্ণ আজান",
        titleEnglish = "Full Azan",
        subtitleBangla = "পূর্ণাঙ্গ আজানের সুমধুর ধ্বনি ও নোটিফিকেশন",
        subtitleEnglish = "Complete melodious Azan call with notification"
    ),
    SHORT_AZAN(
        id = "SHORT_AZAN",
        titleBangla = "সংক্ষিপ্ত আজান সাউন্ড",
        titleEnglish = "Short Azan Sound",
        subtitleBangla = "সংক্ষিপ্ত তাকবীর ও শাহাদাত আজান ধ্বনি",
        subtitleEnglish = "Short Takbeer & Shahadah Azan sound"
    ),
    REMINDER_TONE(
        id = "REMINDER_TONE",
        titleBangla = "সাধারণ রিমাইন্ডার টোন",
        titleEnglish = "General Reminder Tone",
        subtitleBangla = "কোমল ইসলামিক রিমাইন্ডার টোন ও সতর্কবার্তা",
        subtitleEnglish = "Gentle Islamic chime reminder tone"
    ),
    NOTIFICATION_ONLY(
        id = "NOTIFICATION_ONLY",
        titleBangla = "শুধু নোটিফিকেশন",
        titleEnglish = "Notification Only",
        subtitleBangla = "সাউন্ড ছাড়া শুধু নোটিফিকেশন বার্তা",
        subtitleEnglish = "Silent visual notification without speaker audio"
    );

    companion object {
        fun fromId(id: String?): AzanSoundType {
            return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: FULL_AZAN
        }
    }
}

enum class AsrMadhhab(
    val id: String,
    val titleBangla: String,
    val titleEnglish: String,
    val shadowFactor: Int
) {
    HANAFI(
        id = "HANAFI",
        titleBangla = "হানাফি মাজহাব (মিছলে ছানি)",
        titleEnglish = "Hanafi Madhhab (Shadow Factor 2)",
        shadowFactor = 2
    ),
    SHAFII(
        id = "SHAFII",
        titleBangla = "শাফেয়ী / স্ট্যান্ডার্ড (মিছলে আউয়াল)",
        titleEnglish = "Shafi'i / Standard (Shadow Factor 1)",
        shadowFactor = 1
    );

    companion object {
        fun fromId(id: String?): AsrMadhhab {
            return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: HANAFI
        }
    }
}

enum class PrayerCalculationMethod(
    val id: String,
    val titleBangla: String,
    val titleEnglish: String,
    val fajrAngleDeg: Double,
    val ishaAngleDeg: Double
) {
    BANGLADESH_ISLAMIC_FOUNDATION(
        id = "BIF_KARACHI",
        titleBangla = "ইসলামিক ফাউন্ডেশন বাংলাদেশ / করাচি (১৮°/১৮°)",
        titleEnglish = "Islamic Foundation Bangladesh / Karachi (18°/18°)",
        fajrAngleDeg = 18.0,
        ishaAngleDeg = 18.0
    ),
    UMM_AL_QURA_MAKKAH(
        id = "UMM_AL_QURA",
        titleBangla = "উম্মুল কুরা বিশ্ববিদ্যালয়, মক্কা (১৮.৫°)",
        titleEnglish = "Umm al-Qura University, Makkah (18.5°)",
        fajrAngleDeg = 18.5,
        ishaAngleDeg = 18.5
    ),
    MUSLIM_WORLD_LEAGUE(
        id = "MWL",
        titleBangla = "মুসলিম ওয়ার্ল্ড লীগ (১৮°/১৭°)",
        titleEnglish = "Muslim World League (18°/17°)",
        fajrAngleDeg = 18.0,
        ishaAngleDeg = 17.0
    ),
    EGYPTIAN_GENERAL_AUTHORITY(
        id = "EGYPT",
        titleBangla = "মিশরীয় জেনারেল অথরিটি (১৯.৫°/১৭.৫°)",
        titleEnglish = "Egyptian General Authority (19.5°/17.5°)",
        fajrAngleDeg = 19.5,
        ishaAngleDeg = 17.5
    );

    companion object {
        fun fromId(id: String?): PrayerCalculationMethod {
            return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }
                ?: BANGLADESH_ISLAMIC_FOUNDATION
        }
    }
}

enum class PrayerUiLanguage(val code: String) {
    BANGLA("BN"),
    ENGLISH("EN");

    companion object {
        fun fromCode(code: String?): PrayerUiLanguage {
            return if (code.equals("EN", ignoreCase = true)) ENGLISH else BANGLA
        }
    }
}

data class WaqtAzanConfig(
    val waqtKey: PrayerWaqtKey,
    val azanEnabled: Boolean = true,
    val soundType: AzanSoundType = AzanSoundType.FULL_AZAN,
    val volumePercent: Int = 85,
    val preReminderMinutes: Int = 0 // 0 = off, 5 / 10 / 15 or custom minutes before waqt
)

data class PrayerSystemPreferences(
    val masterAzanEnabled: Boolean = true,
    val defaultSoundType: AzanSoundType = AzanSoundType.FULL_AZAN,
    val calculationMethod: PrayerCalculationMethod = PrayerCalculationMethod.BANGLADESH_ISLAMIC_FOUNDATION,
    val asrMadhhab: AsrMadhhab = AsrMadhhab.HANAFI,
    val language: PrayerUiLanguage = PrayerUiLanguage.BANGLA,
    val useGpsLocation: Boolean = false,
    val gpsLatitude: Double = 23.8103,
    val gpsLongitude: Double = 90.4125,
    val gpsLocationLabelBangla: String = "",
    val gpsLocationLabelEnglish: String = "",
    val respectSilentAndDndMode: Boolean = true,
    val waqtConfigs: Map<PrayerWaqtKey, WaqtAzanConfig> = PrayerWaqtKey.entries.associateWith {
        WaqtAzanConfig(waqtKey = it)
    }
) {
    fun configFor(waqtKey: PrayerWaqtKey): WaqtAzanConfig {
        return waqtConfigs[waqtKey] ?: WaqtAzanConfig(waqtKey = waqtKey, soundType = defaultSoundType)
    }
}

data class ComputedWaqtItem(
    val key: PrayerWaqtKey,
    val nameBangla: String,
    val nameEnglish: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val startFormattedBangla: String,
    val endFormattedBangla: String,
    val startFormattedEnglish: String,
    val endFormattedEnglish: String,
    val isCurrentWaqt: Boolean,
    val isNextWaqt: Boolean
)

data class ComputedDailyPrayerSchedule(
    val locationNameBangla: String,
    val locationNameEnglish: String,
    val divisionNameBangla: String,
    val latitude: Double,
    val longitude: Double,
    val calculationMethod: PrayerCalculationMethod,
    val asrMadhhab: AsrMadhhab,
    val waqts: List<ComputedWaqtItem>,
    val sunriseMinutes: Int,
    val sunsetMinutes: Int,
    val sehriEndMinutes: Int,
    val iftarStartMinutes: Int,
    val sunriseBangla: String,
    val sunsetBangla: String,
    val sunriseEnglish: String,
    val sunsetEnglish: String,
    val sehriEndBangla: String,
    val iftarStartBangla: String,
    val sehriEndEnglish: String,
    val iftarStartEnglish: String,
    val currentWaqtKey: PrayerWaqtKey?,
    val currentWaqtLabelBangla: String,
    val currentWaqtLabelEnglish: String,
    val currentWaqtRangeBangla: String,
    val currentWaqtRangeEnglish: String,
    val nextWaqtKey: PrayerWaqtKey,
    val nextWaqtNameBangla: String,
    val nextWaqtNameEnglish: String,
    val nextWaqtStartBangla: String,
    val nextWaqtStartEnglish: String,
    val remainingHours: Int,
    val remainingMinutes: Int,
    val remainingSeconds: Int,
    val totalRemainingSeconds: Long,
    val countdownBangla: String,
    val countdownEnglish: String,
    val currentTimeBangla: String,
    val currentTimeEnglish: String,
    val hijriDateBangla: String,
    val hijriDateEnglish: String,
    val banglaCalendarDateBangla: String,
    val banglaCalendarDateEnglish: String,
    val gregorianDateBangla: String,
    val gregorianDateEnglish: String
)

data class AzanPlaybackState(
    val isPlaying: Boolean = false,
    val activeWaqtKey: PrayerWaqtKey? = null,
    val activeSoundType: AzanSoundType? = null,
    val isPreview: Boolean = false,
    val statusBangla: String = "",
    val statusEnglish: String = ""
)

data class DeviceAlarmPermissionStatus(
    val hasNotificationPermission: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val isIgnoringBatteryOptimizations: Boolean = true,
    val isDoNotDisturbActive: Boolean = false,
    val isPhoneSilentOrVibrate: Boolean = false
)

object PrayerAzanManager {

    private const val PREFS_NAME = "qawmi_prayer_azan_system_prefs"
    private const val KEY_MASTER_AZAN_ENABLED = "master_azan_enabled"
    private const val KEY_DEFAULT_SOUND_TYPE = "default_sound_type"
    private const val KEY_CALC_METHOD = "calculation_method"
    private const val KEY_ASR_MADHHAB = "asr_madhhab"
    private const val KEY_UI_LANGUAGE = "ui_language"
    private const val KEY_USE_GPS_LOCATION = "use_gps_location"
    private const val KEY_GPS_LAT = "gps_latitude"
    private const val KEY_GPS_LON = "gps_longitude"
    private const val KEY_GPS_LABEL_BN = "gps_label_bn"
    private const val KEY_GPS_LABEL_EN = "gps_label_en"
    private const val KEY_RESPECT_SILENT_DND = "respect_silent_dnd"
    private const val KEY_FIRED_HISTORY_SET = "fired_azan_history_keys"

    const val AZAN_CHANNEL_ID = "qawmi_prayer_azan_channel_v1"
    const val PRE_REMINDER_CHANNEL_ID = "qawmi_prayer_prereminder_channel_v1"
    const val AZAN_NOTIFICATION_ID = 786_901
    const val PRE_REMINDER_NOTIFICATION_BASE_ID = 786_920
    const val EXTRA_OPEN_PRAYER_SCREEN = "open_prayer_schedule_screen"

    private const val ALARM_BASE_CODE_AZAN = 788_100
    private const val ALARM_BASE_CODE_PRE = 788_200
    private const val ALARM_CODE_DAILY_ROLLOVER = 788_999

    private val _preferencesFlow = MutableStateFlow(PrayerSystemPreferences())
    val preferencesFlow: StateFlow<PrayerSystemPreferences> = _preferencesFlow.asStateFlow()

    private val _playbackStateFlow = MutableStateFlow(AzanPlaybackState())
    val playbackStateFlow: StateFlow<AzanPlaybackState> = _playbackStateFlow.asStateFlow()

    @Volatile
    private var initialized = false

    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activePlaybackJob: Job? = null
    @Volatile
    private var activeMediaPlayer: MediaPlayer? = null
    @Volatile
    private var activeWakeLock: PowerManager.WakeLock? = null

    /**
     * Test hook for deterministic unit/Robolectric tests without blocking on real audio hardware.
     */
    @Volatile
    var testAudioOverride: ((AzanSoundType, PrayerWaqtKey?, Int, Boolean) -> Unit)? = null

    private val districtEnglishNames: Map<String, String> = mapOf(
        "ঢাকা" to "Dhaka", "গাজীপুর" to "Gazipur", "নারায়ণগঞ্জ" to "Narayanganj",
        "নরসিংদী" to "Narsingdi", "মানিকগঞ্জ" to "Manikganj", "মুন্সীগঞ্জ" to "Munshiganj",
        "টাঙ্গাইল" to "Tangail", "কিশোরগঞ্জ" to "Kishoreganj", "ফরিদপুর" to "Faridpur",
        "গোপালগঞ্জ" to "Gopalganj", "মাদারীপুর" to "Madaripur", "রাজবাড়ী" to "Rajbari",
        "শরীয়তপুর" to "Shariatpur", "চট্টগ্রাম" to "Chattogram", "কক্সবাজার" to "Cox's Bazar",
        "কুমিল্লা" to "Cumilla", "ব্রাহ্মণবাড়িয়া" to "Brahmanbaria", "চাঁদপুর" to "Chandpur",
        "নোয়াখালী" to "Noakhali", "ফেনী" to "Feni", "লক্ষ্মীপুর" to "Lakshmipur",
        "রাঙ্গামাটি" to "Rangamati", "বান্দরবান" to "Bandarban", "খাগড়াছড়ি" to "Khagrachhari",
        "সিলেট" to "Sylhet", "মৌলভীবাজার" to "Moulvibazar", "হবিগঞ্জ" to "Habiganj",
        "সুনামগঞ্জ" to "Sunamganj", "রাজশাহী" to "Rajshahi", "বগুড়া" to "Bogura",
        "পাবনা" to "Pabna", "সিরাজগঞ্জ" to "Sirajganj", "নাটোর" to "Natore",
        "নওগাঁ" to "Naogaon", "চাঁপাইনবাবগঞ্জ" to "Chapainawabganj", "জয়পুরহাট" to "Joypurhat",
        "খুলনা" to "Khulna", "যশোর" to "Jashore", "কুষ্টিয়া" to "Kushtia",
        "সাতক্ষীরা" to "Satkhira", "বাগেরহাট" to "Bagerhat", "ঝিনাইদহ" to "Jhenaidah",
        "চুয়াডাঙ্গা" to "Chuadanga", "মাগুরা" to "Magura", "মেহেরপুর" to "Meherpur",
        "নড়াইল" to "Narail", "বরিশাল" to "Barishal", "পটুয়াখালী" to "Patuakhali",
        "ভোলা" to "Bhola", "পিরোজপুর" to "Pirojpur", "বরগুনা" to "Barguna",
        "ঝালকাঠি" to "Jhalokati", "রংপুর" to "Rangpur", "দিনাজপুর" to "Dinajpur",
        "কুড়িগ্রাম" to "Kurigram", "গাইবান্ধা" to "Gaibandha", "নীলফামারী" to "Nilphamari",
        "পঞ্চগড়" to "Panchagarh", "ঠাকুরগাঁও" to "Thakurgaon", "লালমনিরহাট" to "Lalmonirhat",
        "ময়মনসিংহ" to "Mymensingh", "জামালপুর" to "Jamalpur", "শেরপুর" to "Sherpur",
        "নেত্রকোনা" to "Netrokona"
    )

    fun getDistrictEnglishName(districtBangla: String): String {
        return districtEnglishNames[districtBangla.trim()] ?: districtBangla
    }

    fun initWithContext(context: Context) {
        IslamicDateAndPrayerHelper.initWithContext(context)
        ensureChannelsCreated(context)
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            loadPreferencesFromDisk(context)
            initialized = true
        }
        scheduleAllUpcomingPrayerAlarms(context)
    }

    fun reloadFromDisk(context: Context) {
        loadPreferencesFromDisk(context)
    }

    private fun loadPreferencesFromDisk(context: Context) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val masterEnabled = prefs.getBoolean(KEY_MASTER_AZAN_ENABLED, true)
            val defaultSound = AzanSoundType.fromId(prefs.getString(KEY_DEFAULT_SOUND_TYPE, AzanSoundType.FULL_AZAN.id))
            val calcMethod = PrayerCalculationMethod.fromId(
                prefs.getString(KEY_CALC_METHOD, PrayerCalculationMethod.BANGLADESH_ISLAMIC_FOUNDATION.id)
            )
            val madhhab = AsrMadhhab.fromId(prefs.getString(KEY_ASR_MADHHAB, AsrMadhhab.HANAFI.id))
            val lang = PrayerUiLanguage.fromCode(prefs.getString(KEY_UI_LANGUAGE, PrayerUiLanguage.BANGLA.code))
            val useGps = prefs.getBoolean(KEY_USE_GPS_LOCATION, false)
            val gpsLat = prefs.getString(KEY_GPS_LAT, "23.8103")?.toDoubleOrNull() ?: 23.8103
            val gpsLon = prefs.getString(KEY_GPS_LON, "90.4125")?.toDoubleOrNull() ?: 90.4125
            val gpsLabelBn = prefs.getString(KEY_GPS_LABEL_BN, "").orEmpty()
            val gpsLabelEn = prefs.getString(KEY_GPS_LABEL_EN, "").orEmpty()
            val respectSilent = prefs.getBoolean(KEY_RESPECT_SILENT_DND, true)

            val waqtMap = PrayerWaqtKey.entries.associateWith { waqt ->
                val enabled = prefs.getBoolean("waqt_${waqt.id}_enabled", true)
                val sound = AzanSoundType.fromId(prefs.getString("waqt_${waqt.id}_sound", defaultSound.id))
                val volume = prefs.getInt("waqt_${waqt.id}_volume", 85).coerceIn(0, 100)
                val preMin = prefs.getInt("waqt_${waqt.id}_prereminder", 0).coerceIn(0, 120)
                WaqtAzanConfig(
                    waqtKey = waqt,
                    azanEnabled = enabled,
                    soundType = sound,
                    volumePercent = volume,
                    preReminderMinutes = preMin
                )
            }

            _preferencesFlow.value = PrayerSystemPreferences(
                masterAzanEnabled = masterEnabled,
                defaultSoundType = defaultSound,
                calculationMethod = calcMethod,
                asrMadhhab = madhhab,
                language = lang,
                useGpsLocation = useGps,
                gpsLatitude = gpsLat,
                gpsLongitude = gpsLon,
                gpsLocationLabelBangla = gpsLabelBn,
                gpsLocationLabelEnglish = gpsLabelEn,
                respectSilentAndDndMode = respectSilent,
                waqtConfigs = waqtMap
            )
        } catch (_: Exception) {
        }
    }

    private fun savePreferencesToDisk(context: Context, prefsState: PrayerSystemPreferences) {
        _preferencesFlow.value = prefsState
        try {
            val editor = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MASTER_AZAN_ENABLED, prefsState.masterAzanEnabled)
                .putString(KEY_DEFAULT_SOUND_TYPE, prefsState.defaultSoundType.id)
                .putString(KEY_CALC_METHOD, prefsState.calculationMethod.id)
                .putString(KEY_ASR_MADHHAB, prefsState.asrMadhhab.id)
                .putString(KEY_UI_LANGUAGE, prefsState.language.code)
                .putBoolean(KEY_USE_GPS_LOCATION, prefsState.useGpsLocation)
                .putString(KEY_GPS_LAT, prefsState.gpsLatitude.toString())
                .putString(KEY_GPS_LON, prefsState.gpsLongitude.toString())
                .putString(KEY_GPS_LABEL_BN, prefsState.gpsLocationLabelBangla)
                .putString(KEY_GPS_LABEL_EN, prefsState.gpsLocationLabelEnglish)
                .putBoolean(KEY_RESPECT_SILENT_DND, prefsState.respectSilentAndDndMode)

            prefsState.waqtConfigs.forEach { (waqt, cfg) ->
                editor.putBoolean("waqt_${waqt.id}_enabled", cfg.azanEnabled)
                editor.putString("waqt_${waqt.id}_sound", cfg.soundType.id)
                editor.putInt("waqt_${waqt.id}_volume", cfg.volumePercent.coerceIn(0, 100))
                editor.putInt("waqt_${waqt.id}_prereminder", cfg.preReminderMinutes.coerceIn(0, 120))
            }
            editor.apply()
        } catch (_: Exception) {
        }
    }

    fun setMasterAzanEnabled(context: Context, enabled: Boolean) {
        val updated = _preferencesFlow.value.copy(masterAzanEnabled = enabled)
        savePreferencesToDisk(context, updated)
        if (!enabled) {
            stopAzanPlayback(context)
        }
        scheduleAllUpcomingPrayerAlarms(context)
    }

    fun setDefaultSoundTypeForAll(context: Context, soundType: AzanSoundType) {
        val current = _preferencesFlow.value
        val updatedMap = current.waqtConfigs.mapValues { (_, cfg) ->
            cfg.copy(soundType = soundType)
        }
        val updated = current.copy(defaultSoundType = soundType, waqtConfigs = updatedMap)
        savePreferencesToDisk(context, updated)
        scheduleAllUpcomingPrayerAlarms(context)
    }

    fun setCalculationMethod(context: Context, method: PrayerCalculationMethod) {
        val updated = _preferencesFlow.value.copy(calculationMethod = method)
        savePreferencesToDisk(context, updated)
        scheduleAllUpcomingPrayerAlarms(context)
    }

    fun setAsrMadhhab(context: Context, madhhab: AsrMadhhab) {
        val updated = _preferencesFlow.value.copy(asrMadhhab = madhhab)
        savePreferencesToDisk(context, updated)
        scheduleAllUpcomingPrayerAlarms(context)
    }

    fun setUiLanguage(context: Context, language: PrayerUiLanguage) {
        val updated = _preferencesFlow.value.copy(language = language)
        savePreferencesToDisk(context, updated)
    }

    fun setRespectSilentAndDndMode(context: Context, respect: Boolean) {
        val updated = _preferencesFlow.value.copy(respectSilentAndDndMode = respect)
        savePreferencesToDisk(context, updated)
    }

    fun updateWaqtConfig(
        context: Context,
        waqtKey: PrayerWaqtKey,
        azanEnabled: Boolean? = null,
        soundType: AzanSoundType? = null,
        volumePercent: Int? = null,
        preReminderMinutes: Int? = null
    ) {
        val current = _preferencesFlow.value
        val existing = current.configFor(waqtKey)
        val newCfg = existing.copy(
            azanEnabled = azanEnabled ?: existing.azanEnabled,
            soundType = soundType ?: existing.soundType,
            volumePercent = (volumePercent ?: existing.volumePercent).coerceIn(0, 100),
            preReminderMinutes = (preReminderMinutes ?: existing.preReminderMinutes).coerceIn(0, 120)
        )
        val updatedMap = current.waqtConfigs + (waqtKey to newCfg)
        savePreferencesToDisk(context, current.copy(waqtConfigs = updatedMap))
        scheduleAllUpcomingPrayerAlarms(context)
    }

    fun setManualDistrictSelection(context: Context, districtBangla: String) {
        IslamicDateAndPrayerHelper.selectDistrict(context, districtBangla)
        val updated = _preferencesFlow.value.copy(useGpsLocation = false)
        savePreferencesToDisk(context, updated)
        scheduleAllUpcomingPrayerAlarms(context)
    }

    fun setPinnedDistrictSelection(context: Context, districtBangla: String) {
        IslamicDateAndPrayerHelper.pinDistrict(context, districtBangla)
        val updated = _preferencesFlow.value.copy(useGpsLocation = false)
        savePreferencesToDisk(context, updated)
        scheduleAllUpcomingPrayerAlarms(context)
    }

    /**
     * Applies GPS/Network coordinates, matches the nearest Bangladesh district, and updates schedules & alarms.
     */
    fun applyGpsCoordinates(context: Context, latitude: Double, longitude: Double): IslamicDateAndPrayerHelper.BangladeshDistrictCoord {
        val nearest = findNearestBangladeshDistrict(latitude, longitude)
        IslamicDateAndPrayerHelper.selectDistrict(context, nearest.nameBangla)
        val labelBn = "${nearest.nameBangla} (GPS: ${String.format(Locale.US, "%.2f, %.2f", latitude, longitude)})"
        val labelEn = "${getDistrictEnglishName(nearest.nameBangla)} (GPS: ${String.format(Locale.US, "%.2f, %.2f", latitude, longitude)})"
        val updated = _preferencesFlow.value.copy(
            useGpsLocation = true,
            gpsLatitude = latitude,
            gpsLongitude = longitude,
            gpsLocationLabelBangla = labelBn,
            gpsLocationLabelEnglish = labelEn
        )
        savePreferencesToDisk(context, updated)
        scheduleAllUpcomingPrayerAlarms(context)
        return nearest
    }

    /**
     * Attempts to read the user's current location from Android LocationManager (GPS or Network)
     * when location permission has been granted.
     */
    @Suppress("MissingPermission")
    fun detectCurrentDeviceLocation(context: Context): Result<IslamicDateAndPrayerHelper.BangladeshDistrictCoord> {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return Result.failure(SecurityException("লোকেশন অনুমতি প্রদান করা হয়নি।"))
        }

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return Result.failure(IllegalStateException("Location service পাওয়া যায়নি।"))

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null) {
                    if (bestLocation == null || loc.time > bestLocation.time) {
                        bestLocation = loc
                    }
                }
            }

            if (bestLocation != null) {
                val matched = applyGpsCoordinates(context, bestLocation.latitude, bestLocation.longitude)
                Result.success(matched)
            } else {
                // Fallback to currently selected/pinned district coordinates if device GPS has no fix yet
                val currentDist = IslamicDateAndPrayerHelper.selectedDistrictFlow.value
                val coord = IslamicDateAndPrayerHelper.findDistrictCoord(currentDist)
                    ?: IslamicDateAndPrayerHelper.allBangladeshDistrictCoords.first()
                val matched = applyGpsCoordinates(context, coord.latitude, coord.longitude)
                Result.success(matched)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun findNearestBangladeshDistrict(
        latitude: Double,
        longitude: Double
    ): IslamicDateAndPrayerHelper.BangladeshDistrictCoord {
        val all = IslamicDateAndPrayerHelper.allBangladeshDistrictCoords
        return all.minByOrNull { coord ->
            haversineDistanceKm(latitude, longitude, coord.latitude, coord.longitude)
        } ?: all.first()
    }

    private fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Computes the full daily schedule, sunrise, sunset, sehri, iftar, active waqt, next prayer,
     * and live second-by-second countdown for the given district/GPS coordinates, calculation method, and madhhab.
     */
    fun computeDailyPrayerSchedule(
        districtBangla: String = IslamicDateAndPrayerHelper.selectedDistrictFlow.value,
        preferences: PrayerSystemPreferences = _preferencesFlow.value,
        calendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
    ): ComputedDailyPrayerSchedule {
        val districtCoord = IslamicDateAndPrayerHelper.findDistrictCoord(districtBangla)
            ?: IslamicDateAndPrayerHelper.allBangladeshDistrictCoords.first()

        val effectiveLat = if (preferences.useGpsLocation) preferences.gpsLatitude else districtCoord.latitude
        val effectiveLon = if (preferences.useGpsLocation) preferences.gpsLongitude else districtCoord.longitude

        val locationNameBn = if (preferences.useGpsLocation && preferences.gpsLocationLabelBangla.isNotBlank()) {
            districtCoord.nameBangla
        } else {
            districtCoord.nameBangla
        }
        val locationNameEn = getDistrictEnglishName(districtCoord.nameBangla)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val nowMinutes = hour24 * 60 + minute
        val nowTotalSeconds = hour24 * 3600L + minute * 60L + second

        val solar = computeSolarAndPrayerMinutes(
            latitude = effectiveLat,
            longitude = effectiveLon,
            year = year,
            month = month,
            day = day,
            calculationMethod = preferences.calculationMethod,
            asrMadhhab = preferences.asrMadhhab
        )

        val isFajrActive = nowMinutes in solar.fajrStartMin..solar.fajrEndMin
        val isDhuhrActive = nowMinutes in solar.dhuhrStartMin..solar.dhuhrEndMin
        val isAsrActive = nowMinutes in solar.asrStartMin..solar.asrEndMin
        val isMaghribActive = nowMinutes in solar.maghribStartMin..solar.maghribEndMin
        val isIshaActive = nowMinutes >= solar.ishaStartMin || nowMinutes <= solar.ishaEndMin

        val currentWaqtKey: PrayerWaqtKey? = when {
            isFajrActive -> PrayerWaqtKey.FAJR
            isDhuhrActive -> PrayerWaqtKey.DHUHR
            isAsrActive -> PrayerWaqtKey.ASR
            isMaghribActive -> PrayerWaqtKey.MAGHRIB
            isIshaActive -> PrayerWaqtKey.ISHA
            else -> null
        }

        val (nextWaqtKey, nextStartMinutes, isNextTomorrow) = when {
            nowMinutes < solar.fajrStartMin -> Triple(PrayerWaqtKey.FAJR, solar.fajrStartMin, false)
            nowMinutes < solar.dhuhrStartMin -> Triple(PrayerWaqtKey.DHUHR, solar.dhuhrStartMin, false)
            nowMinutes < solar.asrStartMin -> Triple(PrayerWaqtKey.ASR, solar.asrStartMin, false)
            nowMinutes < solar.maghribStartMin -> Triple(PrayerWaqtKey.MAGHRIB, solar.maghribStartMin, false)
            nowMinutes < solar.ishaStartMin -> Triple(PrayerWaqtKey.ISHA, solar.ishaStartMin, false)
            else -> Triple(PrayerWaqtKey.FAJR, solar.fajrStartMin, true)
        }

        val targetSeconds = if (isNextTomorrow) {
            (24L * 3600L - nowTotalSeconds) + (nextStartMinutes * 60L)
        } else {
            (nextStartMinutes * 60L - nowTotalSeconds).coerceAtLeast(0L)
        }

        val remHours = (targetSeconds / 3600L).toInt()
        val remMins = ((targetSeconds % 3600L) / 60L).toInt()
        val remSecs = (targetSeconds % 60L).toInt()

        val countdownBn = buildString {
            if (remHours > 0) {
                append("${IslamicDateAndPrayerHelper.toBanglaDigits(String.format(Locale.US, "%02d", remHours))} ঘণ্টা ")
            }
            append("${IslamicDateAndPrayerHelper.toBanglaDigits(String.format(Locale.US, "%02d", remMins))} মিনিট ")
            append("${IslamicDateAndPrayerHelper.toBanglaDigits(String.format(Locale.US, "%02d", remSecs))} সেকেন্ড বাকি")
        }
        val countdownEn = String.format(Locale.US, "%02dh %02dm %02ds remaining", remHours, remMins, remSecs)

        val (currLabelBn, currLabelEn, currRangeBn, currRangeEn) = when {
            isFajrActive -> Quad(
                "চলমান ওয়াক্ত: ফজর",
                "Current Waqt: Fajr",
                "${formatMinutes12hBangla(solar.fajrStartMin)} - ${formatMinutes12hBangla(solar.fajrEndMin)}",
                "${formatMinutes12hEnglish(solar.fajrStartMin)} - ${formatMinutes12hEnglish(solar.fajrEndMin)}"
            )
            nowMinutes in solar.sunriseMin..(solar.sunriseMin + 15) -> Quad(
                "সূর্যোদয় চলছে (নামাজ নিষিদ্ধ সময়)",
                "Sunrise (Forbidden Prayer Time)",
                "${formatMinutes12hBangla(solar.sunriseMin)} - ${formatMinutes12hBangla(solar.sunriseMin + 15)}",
                "${formatMinutes12hEnglish(solar.sunriseMin)} - ${formatMinutes12hEnglish(solar.sunriseMin + 15)}"
            )
            nowMinutes in (solar.sunriseMin + 16) until (solar.transitMin - 5) -> Quad(
                "চাশত / ইশরাক (নফল ওয়াক্ত)",
                "Ishraq / Duha (Nafl Time)",
                "${formatMinutes12hBangla(solar.sunriseMin + 15)} - ${formatMinutes12hBangla(solar.transitMin - 5)}",
                "${formatMinutes12hEnglish(solar.sunriseMin + 15)} - ${formatMinutes12hEnglish(solar.transitMin - 5)}"
            )
            nowMinutes in (solar.transitMin - 5) until solar.dhuhrStartMin -> Quad(
                "দ্বিপ্রহর / যাওয়াল (নিষিদ্ধ সময়)",
                "Solar Noon / Zawal (Forbidden Time)",
                "${formatMinutes12hBangla(solar.transitMin - 5)} - ${formatMinutes12hBangla(solar.dhuhrStartMin)}",
                "${formatMinutes12hEnglish(solar.transitMin - 5)} - ${formatMinutes12hEnglish(solar.dhuhrStartMin)}"
            )
            isDhuhrActive -> Quad(
                "চলমান ওয়াক্ত: যোহর",
                "Current Waqt: Dhuhr",
                "${formatMinutes12hBangla(solar.dhuhrStartMin)} - ${formatMinutes12hBangla(solar.dhuhrEndMin)}",
                "${formatMinutes12hEnglish(solar.dhuhrStartMin)} - ${formatMinutes12hEnglish(solar.dhuhrEndMin)}"
            )
            isAsrActive -> Quad(
                "চলমান ওয়াক্ত: আসর (${if (preferences.asrMadhhab == AsrMadhhab.HANAFI) "হানাফি" else "শাফেয়ী"})",
                "Current Waqt: Asr (${if (preferences.asrMadhhab == AsrMadhhab.HANAFI) "Hanafi" else "Shafi'i"})",
                "${formatMinutes12hBangla(solar.asrStartMin)} - ${formatMinutes12hBangla(solar.asrEndMin)}",
                "${formatMinutes12hEnglish(solar.asrStartMin)} - ${formatMinutes12hEnglish(solar.asrEndMin)}"
            )
            nowMinutes in (solar.asrEndMin + 1) until solar.maghribStartMin -> Quad(
                "সূর্যাস্ত সতর্কতা সময়",
                "Sunset Precautionary Time",
                "${formatMinutes12hBangla(solar.sunsetMin)} - ${formatMinutes12hBangla(solar.maghribStartMin)}",
                "${formatMinutes12hEnglish(solar.sunsetMin)} - ${formatMinutes12hEnglish(solar.maghribStartMin)}"
            )
            isMaghribActive -> Quad(
                "চলমান ওয়াক্ত: মাগরিব",
                "Current Waqt: Maghrib",
                "${formatMinutes12hBangla(solar.maghribStartMin)} - ${formatMinutes12hBangla(solar.maghribEndMin)}",
                "${formatMinutes12hEnglish(solar.maghribStartMin)} - ${formatMinutes12hEnglish(solar.maghribEndMin)}"
            )
            else -> Quad(
                "চলমান ওয়াক্ত: এশা",
                "Current Waqt: Isha",
                "${formatMinutes12hBangla(solar.ishaStartMin)} - ${formatMinutes12hBangla(solar.ishaEndMin)}",
                "${formatMinutes12hEnglish(solar.ishaStartMin)} - ${formatMinutes12hEnglish(solar.ishaEndMin)}"
            )
        }

        val waqtItems = listOf(
            buildComputedWaqt(PrayerWaqtKey.FAJR, solar.fajrStartMin, solar.fajrEndMin, isFajrActive, nextWaqtKey == PrayerWaqtKey.FAJR),
            buildComputedWaqt(PrayerWaqtKey.DHUHR, solar.dhuhrStartMin, solar.dhuhrEndMin, isDhuhrActive, nextWaqtKey == PrayerWaqtKey.DHUHR),
            buildComputedWaqt(PrayerWaqtKey.ASR, solar.asrStartMin, solar.asrEndMin, isAsrActive, nextWaqtKey == PrayerWaqtKey.ASR),
            buildComputedWaqt(PrayerWaqtKey.MAGHRIB, solar.maghribStartMin, solar.maghribEndMin, isMaghribActive, nextWaqtKey == PrayerWaqtKey.MAGHRIB),
            buildComputedWaqt(PrayerWaqtKey.ISHA, solar.ishaStartMin, solar.ishaEndMin, isIshaActive, nextWaqtKey == PrayerWaqtKey.ISHA)
        )

        val currentClockEn = formatClockWithSecondsEnglish(hour24, minute, second)
        val currentClockBn = IslamicDateAndPrayerHelper.toBanglaDigits(currentClockEn)

        val (banglaDateBn, banglaDateEn) = getFormattedBanglaPonjikaDate(year, month, day)

        return ComputedDailyPrayerSchedule(
            locationNameBangla = locationNameBn,
            locationNameEnglish = locationNameEn,
            divisionNameBangla = districtCoord.divisionBangla,
            latitude = effectiveLat,
            longitude = effectiveLon,
            calculationMethod = preferences.calculationMethod,
            asrMadhhab = preferences.asrMadhhab,
            waqts = waqtItems,
            sunriseMinutes = solar.sunriseMin,
            sunsetMinutes = solar.sunsetMin,
            sehriEndMinutes = solar.sehriEndMin,
            iftarStartMinutes = solar.maghribStartMin,
            sunriseBangla = formatMinutes12hBangla(solar.sunriseMin),
            sunsetBangla = formatMinutes12hBangla(solar.sunsetMin),
            sunriseEnglish = formatMinutes12hEnglish(solar.sunriseMin),
            sunsetEnglish = formatMinutes12hEnglish(solar.sunsetMin),
            sehriEndBangla = formatMinutes12hBangla(solar.sehriEndMin),
            iftarStartBangla = formatMinutes12hBangla(solar.maghribStartMin),
            sehriEndEnglish = formatMinutes12hEnglish(solar.sehriEndMin),
            iftarStartEnglish = formatMinutes12hEnglish(solar.maghribStartMin),
            currentWaqtKey = currentWaqtKey,
            currentWaqtLabelBangla = currLabelBn,
            currentWaqtLabelEnglish = currLabelEn,
            currentWaqtRangeBangla = currRangeBn,
            currentWaqtRangeEnglish = currRangeEn,
            nextWaqtKey = nextWaqtKey,
            nextWaqtNameBangla = nextWaqtKey.nameBangla,
            nextWaqtNameEnglish = nextWaqtKey.nameEnglish,
            nextWaqtStartBangla = formatMinutes12hBangla(nextStartMinutes),
            nextWaqtStartEnglish = formatMinutes12hEnglish(nextStartMinutes),
            remainingHours = remHours,
            remainingMinutes = remMins,
            remainingSeconds = remSecs,
            totalRemainingSeconds = targetSeconds,
            countdownBangla = countdownBn,
            countdownEnglish = countdownEn,
            currentTimeBangla = currentClockBn,
            currentTimeEnglish = currentClockEn,
            hijriDateBangla = IslamicDateAndPrayerHelper.getFormattedHijriDate(),
            hijriDateEnglish = getFormattedHijriDateEnglish(),
            banglaCalendarDateBangla = banglaDateBn,
            banglaCalendarDateEnglish = banglaDateEn,
            gregorianDateBangla = "${IslamicDateAndPrayerHelper.getFormattedGregorianDate()} (${IslamicDateAndPrayerHelper.getFormattedBanglaDayOfWeek()})",
            gregorianDateEnglish = getFormattedGregorianDateEnglish(calendar)
        )
    }

    private data class SolarTimesResult(
        val fajrStartMin: Int,
        val fajrEndMin: Int,
        val sehriEndMin: Int,
        val sunriseMin: Int,
        val transitMin: Int,
        val dhuhrStartMin: Int,
        val dhuhrEndMin: Int,
        val asrStartMin: Int,
        val asrEndMin: Int,
        val sunsetMin: Int,
        val maghribStartMin: Int,
        val maghribEndMin: Int,
        val ishaStartMin: Int,
        val ishaEndMin: Int
    )

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun computeSolarAndPrayerMinutes(
        latitude: Double,
        longitude: Double,
        year: Int,
        month: Int,
        day: Int,
        calculationMethod: PrayerCalculationMethod,
        asrMadhhab: AsrMadhhab
    ): SolarTimesResult {
        val tzOffsetHours = 6.0 // Bangladesh Standard Time (UTC+6)
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) +
            floor(30.6001 * (m + 1)) +
            day + b - 1524.5 - (tzOffsetHours / 24.0) + 0.5

        val d = jd - 2451545.0
        val gDeg = normalizeDegrees(357.529 + 0.98560028 * d)
        val qDeg = normalizeDegrees(280.459 + 0.98564736 * d)
        val gRad = Math.toRadians(gDeg)
        val lDeg = normalizeDegrees(qDeg + 1.915 * sin(gRad) + 0.020 * sin(2.0 * gRad))
        val lRad = Math.toRadians(lDeg)
        val eDeg = 23.439 - 0.00000036 * d
        val eRad = Math.toRadians(eDeg)

        val sinDecl = sin(eRad) * sin(lRad)
        val declRad = asin(sinDecl.coerceIn(-1.0, 1.0))
        val declDeg = Math.toDegrees(declRad)

        var raHours = Math.toDegrees(atan2(cos(eRad) * sin(lRad), cos(lRad))) / 15.0
        if (raHours < 0.0) raHours += 24.0

        var eqTimeHours = (qDeg / 15.0) - raHours
        if (eqTimeHours > 12.0) eqTimeHours -= 24.0
        if (eqTimeHours < -12.0) eqTimeHours += 24.0

        val transitHours = 12.0 + tzOffsetHours - (longitude / 15.0) - eqTimeHours
        val latRad = Math.toRadians(latitude)

        fun hourAngleOffsetHours(altitudeDeg: Double): Double {
            val altRad = Math.toRadians(altitudeDeg)
            val numerator = sin(altRad) - sin(latRad) * sin(declRad)
            val denominator = cos(latRad) * cos(declRad)
            val cosH = (numerator / denominator).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosH)) / 15.0
        }

        val fajrOffset = hourAngleOffsetHours(-calculationMethod.fajrAngleDeg)
        val ishaOffset = hourAngleOffsetHours(-calculationMethod.ishaAngleDeg)
        val sunHorizonOffset = hourAngleOffsetHours(-0.8333)

        val zenithDistanceRad = Math.toRadians(abs(latitude - declDeg))
        val shadowRatio = asrMadhhab.shadowFactor.toDouble() + tan(zenithDistanceRad)
        val asrAltitudeDeg = Math.toDegrees(atan(1.0 / shadowRatio))
        val asrOffset = hourAngleOffsetHours(asrAltitudeDeg)

        val fajrStartMin = ((transitHours - fajrOffset) * 60.0).roundToInt().coerceIn(0, 1439)
        val sehriEndMin = (fajrStartMin - 1).coerceAtLeast(0)
        val sunriseMin = ((transitHours - sunHorizonOffset) * 60.0).roundToInt().coerceIn(0, 1439)
        val fajrEndMin = (sunriseMin - 1).coerceAtLeast(fajrStartMin + 1)

        val transitMin = (transitHours * 60.0).roundToInt().coerceIn(0, 1439)
        val dhuhrStartMin = (transitMin + 3).coerceIn(0, 1439)
        val asrStartMin = (((transitHours + asrOffset) * 60.0).roundToInt() + 3).coerceIn(0, 1439)
        val dhuhrEndMin = (asrStartMin - 1).coerceAtLeast(dhuhrStartMin + 1)

        val sunsetMin = ((transitHours + sunHorizonOffset) * 60.0).roundToInt().coerceIn(0, 1439)
        val asrEndMin = sunsetMin
        val maghribStartMin = (sunsetMin + 3).coerceIn(0, 1439)
        val ishaStartMin = ((transitHours + ishaOffset) * 60.0).roundToInt().coerceIn(0, 1439)
        val maghribEndMin = (ishaStartMin - 1).coerceAtLeast(maghribStartMin + 1)
        val ishaEndMin = (fajrStartMin - 1).coerceAtLeast(0)

        return SolarTimesResult(
            fajrStartMin = fajrStartMin,
            fajrEndMin = fajrEndMin,
            sehriEndMin = sehriEndMin,
            sunriseMin = sunriseMin,
            transitMin = transitMin,
            dhuhrStartMin = dhuhrStartMin,
            dhuhrEndMin = dhuhrEndMin,
            asrStartMin = asrStartMin,
            asrEndMin = asrEndMin,
            sunsetMin = sunsetMin,
            maghribStartMin = maghribStartMin,
            maghribEndMin = maghribEndMin,
            ishaStartMin = ishaStartMin,
            ishaEndMin = ishaEndMin
        )
    }

    private fun buildComputedWaqt(
        key: PrayerWaqtKey,
        startMinutes: Int,
        endMinutes: Int,
        isCurrent: Boolean,
        isNext: Boolean
    ): ComputedWaqtItem {
        return ComputedWaqtItem(
            key = key,
            nameBangla = key.nameBangla,
            nameEnglish = key.nameEnglish,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            startFormattedBangla = formatMinutes12hBangla(startMinutes),
            endFormattedBangla = formatMinutes12hBangla(endMinutes),
            startFormattedEnglish = formatMinutes12hEnglish(startMinutes),
            endFormattedEnglish = formatMinutes12hEnglish(endMinutes),
            isCurrentWaqt = isCurrent,
            isNextWaqt = isNext
        )
    }

    fun formatMinutes12hBangla(totalMinutes: Int): String {
        return IslamicDateAndPrayerHelper.formatMinutes12hBangla(totalMinutes)
    }

    fun formatMinutes12hEnglish(totalMinutes: Int): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val hour24 = normalized / 60
        val minute = normalized % 60
        val period = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.ENGLISH, "%02d:%02d %s", hour12, minute, period)
    }

    private fun formatClockWithSecondsEnglish(hour24: Int, minute: Int, second: Int): String {
        val period = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d %s", hour12, minute, second, period)
    }

    private fun normalizeDegrees(deg: Double): Double {
        var rem = deg % 360.0
        if (rem < 0.0) rem += 360.0
        return rem
    }

    /**
     * Computes the authentic Bangladesh Bangla Academy Ponjika date (বঙ্গাব্দ).
     */
    fun getFormattedBanglaPonjikaDate(year: Int, month: Int, day: Int): Pair<String, String> {
        val banglaMonthsBn = listOf(
            "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
            "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
        )
        val banglaMonthsEn = listOf(
            "Boishakh", "Jyoishtho", "Asharh", "Shrabon", "Bhadro", "Ashwin",
            "Kartik", "Ogrohayon", "Poush", "Magh", "Falgun", "Choitro"
        )

        val isLeapYear = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        val banglaMonthLengths = intArrayOf(
            31, 31, 31, 31, 31, 31, // Boishakh..Ashwin (31 days)
            30, 30, 30, 30,         // Kartik..Magh (30 days)
            if (isLeapYear) 30 else 29, // Falgun
            30                      // Choitro
        )

        val calCurrent = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(year, month - 1, day, 12, 0, 0)
        }
        val calPohelaBoishakh = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(year, Calendar.APRIL, 14, 12, 0, 0)
        }

        val diffDaysFromApr14 = ((calCurrent.timeInMillis - calPohelaBoishakh.timeInMillis) / (24L * 3600_000L)).toInt()
        val banglaYear: Int
        var remainingDays: Int

        if (diffDaysFromApr14 >= 0) {
            banglaYear = year - 593
            remainingDays = diffDaysFromApr14
        } else {
            banglaYear = year - 594
            val prevYearLeap = ((year - 1) % 4 == 0 && (year - 1) % 100 != 0) || ((year - 1) % 400 == 0)
            val totalDaysPrevYear = if (prevYearLeap) 366 else 365
            remainingDays = totalDaysPrevYear + diffDaysFromApr14
        }

        var bMonthIndex = 0
        for (i in banglaMonthLengths.indices) {
            val len = banglaMonthLengths[i]
            if (remainingDays < len) {
                bMonthIndex = i
                break
            }
            remainingDays -= len
        }
        val bDay = (remainingDays + 1).coerceAtLeast(1)

        val bnStr = "${IslamicDateAndPrayerHelper.toBanglaDigits(bDay.toString())} ${banglaMonthsBn[bMonthIndex]} ${IslamicDateAndPrayerHelper.toBanglaDigits(banglaYear.toString())} বঙ্গাব্দ"
        val enStr = "$bDay ${banglaMonthsEn[bMonthIndex]} $banglaYear BS"
        return bnStr to enStr
    }

    private fun getFormattedHijriDateEnglish(): String {
        val hijriMonthsEn = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hijri = HijrahDate.now()
                val day = hijri.get(ChronoField.DAY_OF_MONTH)
                val month = hijri.get(ChronoField.MONTH_OF_YEAR)
                val year = hijri.get(ChronoField.YEAR)
                val monthName = hijriMonthsEn.getOrElse(month - 1) { "Rabi' al-Awwal" }
                return "$day $monthName $year AH"
            }
        } catch (_: Throwable) {
        }
        return "10 Rabi' al-Awwal 1448 AH"
    }

    private fun getFormattedGregorianDateEnglish(cal: Calendar): String {
        val monthsEn = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val daysEn = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday (Jumu'ah)"
            else -> "Sunday"
        }
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = monthsEn.getOrElse(cal.get(Calendar.MONTH)) { "October" }
        val y = cal.get(Calendar.YEAR)
        return "$d $m $y ($daysEn)"
    }

    fun currentBangladeshDateKey(
        calendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
    ): String {
        val y = calendar.get(Calendar.YEAR)
        val m = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "${y}_${m}_$d"
    }

    /**
     * Checks whether Azan or pre-reminder has already been triggered for this date & waqt,
     * strictly preventing duplicate Azan playback in the same waqt.
     */
    fun hasEventFiredToday(
        context: Context,
        dateKey: String,
        waqtKey: PrayerWaqtKey,
        eventTag: String = "AZAN"
    ): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_FIRED_HISTORY_SET, emptySet()) ?: emptySet()
        return set.contains("${dateKey}_${waqtKey.id}_$eventTag")
    }

    fun markEventFiredToday(
        context: Context,
        dateKey: String,
        waqtKey: PrayerWaqtKey,
        eventTag: String = "AZAN"
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_FIRED_HISTORY_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
        // Keep only recent entries starting with current dateKey or last 30 keys to avoid unbounded growth
        val filtered = currentSet.filter { it.startsWith(dateKey) }.toMutableSet()
        filtered.add("${dateKey}_${waqtKey.id}_$eventTag")
        prefs.edit().putStringSet(KEY_FIRED_HISTORY_SET, filtered).apply()
    }

    fun clearFiredEventsForTest(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FIRED_HISTORY_SET)
            .commit()
    }

    fun checkDeviceAlarmPermissions(context: Context): DeviceAlarmPermissionStatus {
        val hasNotif = QawmiNotificationHelper.hasNotificationPermission(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { alarmManager?.canScheduleExactAlarms() ?: true }.getOrDefault(true)
        } else {
            true
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true }.getOrDefault(true)
        } else {
            true
        }
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val isDnd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val filter = runCatching { notifManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL }
                .getOrDefault(NotificationManager.INTERRUPTION_FILTER_ALL)
            filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } else {
            false
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = runCatching { audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL }
            .getOrDefault(AudioManager.RINGER_MODE_NORMAL)
        val isSilentOrVibrate = ringerMode != AudioManager.RINGER_MODE_NORMAL

        return DeviceAlarmPermissionStatus(
            hasNotificationPermission = hasNotif,
            canScheduleExactAlarms = canExact,
            isIgnoringBatteryOptimizations = ignoringBattery,
            isDoNotDisturbActive = isDnd,
            isPhoneSilentOrVibrate = isSilentOrVibrate
        )
    }

    fun openExactAlarmPermissionSettings(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSoundAndDndSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val azanChannel = NotificationChannel(
                AZAN_CHANNEL_ID,
                "নামাজের ওয়াক্ত ও স্বয়ংক্রিয় আজান",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "পাঁচ ওয়াক্ত নামাজের আজান ও ওয়াক্ত শুরু হওয়ার অ্যালার্ট"
                enableVibration(true)
                setSound(null, null) // Speaker sound is played with precision by AzanAudioPlayer
            }
            val preChannel = NotificationChannel(
                PRE_REMINDER_CHANNEL_ID,
                "নামাজের পূর্ব-রিমাইন্ডার সতর্কতা",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "নামাজের ওয়াক্ত শুরুর ৫, ১০ বা ১৫ মিনিট আগের রিমাইন্ডার নোটিফিকেশন"
                enableVibration(true)
            }
            manager.createNotificationChannel(azanChannel)
            manager.createNotificationChannel(preChannel)
        }
    }

    /**
     * Schedules exact alarms (or window fallback when exact permission is not yet granted)
     * for all 5 daily prayers (both pre-waqt reminders and main Azan alarms) plus a midnight rollover alarm.
     */
    fun scheduleAllUpcomingPrayerAlarms(context: Context) {
        try {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val prefs = _preferencesFlow.value
            val selectedDistrict = IslamicDateAndPrayerHelper.selectedDistrictFlow.value

            val nowCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
            val nowMillis = nowCal.timeInMillis
            val todaySchedule = computeDailyPrayerSchedule(selectedDistrict, prefs, nowCal)

            val tomorrowCal = (nowCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val tomorrowSchedule = computeDailyPrayerSchedule(selectedDistrict, prefs, tomorrowCal)

            for (waqtItem in todaySchedule.waqts) {
                val waqtKey = waqtItem.key
                val cfg = prefs.configFor(waqtKey)
                val azanRequestCode = ALARM_BASE_CODE_AZAN + waqtKey.alarmCodeOffset
                val preRequestCode = ALARM_BASE_CODE_PRE + waqtKey.alarmCodeOffset

                val azanIntent = Intent(appContext, PrayerAzanAlarmReceiver::class.java).apply {
                    action = PrayerAzanAlarmReceiver.ACTION_PRAYER_AZAN_ALARM
                    putExtra(PrayerAzanAlarmReceiver.EXTRA_WAQT_ID, waqtKey.id)
                }
                val preIntent = Intent(appContext, PrayerAzanAlarmReceiver::class.java).apply {
                    action = PrayerAzanAlarmReceiver.ACTION_PRAYER_PRE_REMINDER_ALARM
                    putExtra(PrayerAzanAlarmReceiver.EXTRA_WAQT_ID, waqtKey.id)
                }

                val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val azanPending = PendingIntent.getBroadcast(appContext, azanRequestCode, azanIntent, pendingFlags)
                val prePending = PendingIntent.getBroadcast(appContext, preRequestCode, preIntent, pendingFlags)

                if (!prefs.masterAzanEnabled || !cfg.azanEnabled) {
                    alarmManager.cancel(azanPending)
                } else {
                    val targetMillis = resolveNextTriggerMillisForWaqt(
                        nowCal = nowCal,
                        todayMinutes = waqtItem.startMinutes,
                        tomorrowMinutes = tomorrowSchedule.waqts.first { it.key == waqtKey }.startMinutes
                    )
                    if (targetMillis > nowMillis) {
                        scheduleAlarmSafely(alarmManager, targetMillis, azanPending)
                    }
                }

                if (!prefs.masterAzanEnabled || cfg.preReminderMinutes <= 0) {
                    alarmManager.cancel(prePending)
                } else {
                    val todayPreMinutes = (waqtItem.startMinutes - cfg.preReminderMinutes).coerceAtLeast(0)
                    val tomorrowPreMinutes = (tomorrowSchedule.waqts.first { it.key == waqtKey }.startMinutes - cfg.preReminderMinutes).coerceAtLeast(0)
                    val preTargetMillis = resolveNextTriggerMillisForWaqt(
                        nowCal = nowCal,
                        todayMinutes = todayPreMinutes,
                        tomorrowMinutes = tomorrowPreMinutes
                    )
                    if (preTargetMillis > nowMillis) {
                        scheduleAlarmSafely(alarmManager, preTargetMillis, prePending)
                    }
                }
            }

            // Schedule daily midnight rollover at 00:02 AM Asia/Dhaka so the new day's shifted solar times are always scheduled
            val rolloverCal = (nowCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 2)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }
            val rolloverIntent = Intent(appContext, PrayerAzanAlarmReceiver::class.java).apply {
                action = PrayerAzanAlarmReceiver.ACTION_PRAYER_DAILY_RESCHEDULE
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val rolloverPending = PendingIntent.getBroadcast(
                appContext,
                ALARM_CODE_DAILY_ROLLOVER,
                rolloverIntent,
                pendingFlags
            )
            scheduleAlarmSafely(alarmManager, rolloverCal.timeInMillis, rolloverPending)
        } catch (_: Exception) {
        }
    }

    private fun resolveNextTriggerMillisForWaqt(
        nowCal: Calendar,
        todayMinutes: Int,
        tomorrowMinutes: Int
    ): Long {
        val candidate = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, todayMinutes / 60)
            set(Calendar.MINUTE, todayMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return if (candidate.timeInMillis > nowCal.timeInMillis + 2_000L) {
            candidate.timeInMillis
        } else {
            (nowCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, tomorrowMinutes / 60)
                set(Calendar.MINUTE, tomorrowMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    private fun scheduleAlarmSafely(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
            } else {
                true
            }
            if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Triggers the scheduled Azan notification + speaker audio for [waqtKey] once per day.
     * Returns true if triggered, false if already fired today or disabled.
     */
    fun triggerScheduledWaqtAzan(
        context: Context,
        waqtKey: PrayerWaqtKey,
        dateKey: String = currentBangladeshDateKey()
    ): Boolean {
        initWithContext(context)
        val prefs = _preferencesFlow.value
        val cfg = prefs.configFor(waqtKey)
        if (!prefs.masterAzanEnabled || !cfg.azanEnabled) return false

        // Enforce strict once-per-waqt-per-day deduplication
        if (hasEventFiredToday(context, dateKey, waqtKey, "AZAN")) {
            return false
        }
        markEventFiredToday(context, dateKey, waqtKey, "AZAN")

        postAzanSystemNotification(context, waqtKey, cfg.soundType)

        if (cfg.soundType != AzanSoundType.NOTIFICATION_ONLY && cfg.volumePercent > 0) {
            val permStatus = checkDeviceAlarmPermissions(context)
            val blockedBySilent = prefs.respectSilentAndDndMode &&
                (permStatus.isDoNotDisturbActive || permStatus.isPhoneSilentOrVibrate)
            if (!blockedBySilent) {
                startAzanAudioPlayback(
                    context = context,
                    soundType = cfg.soundType,
                    waqtKey = waqtKey,
                    volumePercent = cfg.volumePercent,
                    isPreview = false
                )
            }
        }
        return true
    }

    /**
     * Triggers the pre-waqt reminder notification (e.g. 5, 10, or 15 minutes before the waqt).
     */
    fun triggerScheduledPreWaqtReminder(
        context: Context,
        waqtKey: PrayerWaqtKey,
        dateKey: String = currentBangladeshDateKey()
    ): Boolean {
        initWithContext(context)
        val prefs = _preferencesFlow.value
        val cfg = prefs.configFor(waqtKey)
        if (!prefs.masterAzanEnabled || cfg.preReminderMinutes <= 0) return false

        val eventTag = "PRE_${cfg.preReminderMinutes}"
        if (hasEventFiredToday(context, dateKey, waqtKey, eventTag)) {
            return false
        }
        markEventFiredToday(context, dateKey, waqtKey, eventTag)

        postPreReminderSystemNotification(context, waqtKey, cfg.preReminderMinutes)
        return true
    }

    fun startSoundPreview(
        context: Context,
        soundType: AzanSoundType,
        waqtKey: PrayerWaqtKey = PrayerWaqtKey.FAJR,
        volumePercent: Int = 85
    ) {
        if (soundType == AzanSoundType.NOTIFICATION_ONLY) {
            postAzanSystemNotification(context, waqtKey, soundType)
            _playbackStateFlow.value = AzanPlaybackState(
                isPlaying = false,
                activeWaqtKey = waqtKey,
                activeSoundType = soundType,
                isPreview = true,
                statusBangla = "শুধু নোটিফিকেশন মোড পরীক্ষামূলকভাবে পাঠানো হয়েছে",
                statusEnglish = "Notification-only preview posted"
            )
            return
        }
        startAzanAudioPlayback(
            context = context,
            soundType = soundType,
            waqtKey = waqtKey,
            volumePercent = volumePercent.coerceIn(10, 100),
            isPreview = true
        )
    }

    fun startAzanAudioPlayback(
        context: Context,
        soundType: AzanSoundType,
        waqtKey: PrayerWaqtKey?,
        volumePercent: Int,
        isPreview: Boolean
    ) {
        stopAzanPlayback(context)

        val overrideHook = testAudioOverride
        if (overrideHook != null) {
            _playbackStateFlow.value = AzanPlaybackState(
                isPlaying = true,
                activeWaqtKey = waqtKey,
                activeSoundType = soundType,
                isPreview = isPreview,
                statusBangla = "${waqtKey?.nameBangla ?: "আজান"} • ${soundType.titleBangla} বাজছে...",
                statusEnglish = "Playing ${soundType.titleEnglish}..."
            )
            overrideHook.invoke(soundType, waqtKey, volumePercent, isPreview)
            return
        }

        // Start foreground/background service & acquire bounded WakeLock so playback continues with screen off
        acquireBoundedWakeLock(context)
        runCatching {
            val serviceIntent = Intent(context.applicationContext, AzanPlaybackService::class.java).apply {
                putExtra(PrayerAzanAlarmReceiver.EXTRA_WAQT_ID, waqtKey?.id.orEmpty())
                putExtra(PrayerAzanAlarmReceiver.EXTRA_SOUND_TYPE, soundType.id)
            }
            context.applicationContext.startService(serviceIntent)
        }

        _playbackStateFlow.value = AzanPlaybackState(
            isPlaying = true,
            activeWaqtKey = waqtKey,
            activeSoundType = soundType,
            isPreview = isPreview,
            statusBangla = "${waqtKey?.nameBangla ?: "পরীক্ষামূলক"} • ${soundType.titleBangla} বাজছে...",
            statusEnglish = "Playing ${soundType.titleEnglish} (${waqtKey?.nameEnglish ?: "Preview"})..."
        )

        activePlaybackJob = audioScope.launch {
            try {
                playRealAzanAudio(
                    appContext = context.applicationContext,
                    soundType = soundType,
                    volumeFraction = (volumePercent.coerceIn(0, 100) / 100f)
                )
            } catch (_: Throwable) {
            } finally {
                releaseMediaPlayerSafely()
                releaseWakeLockSafely()
                _playbackStateFlow.value = AzanPlaybackState(isPlaying = false)
                runCatching {
                    context.applicationContext.stopService(
                        Intent(context.applicationContext, AzanPlaybackService::class.java)
                    )
                }
            }
        }
    }

    fun stopAzanPlayback(context: Context? = null) {
        activePlaybackJob?.cancel()
        activePlaybackJob = null
        releaseMediaPlayerSafely()
        releaseWakeLockSafely()
        _playbackStateFlow.value = AzanPlaybackState(isPlaying = false)
        if (context != null) {
            try {
                NotificationManagerCompat.from(context.applicationContext).cancel(AZAN_NOTIFICATION_ID)
                context.applicationContext.stopService(
                    Intent(context.applicationContext, AzanPlaybackService::class.java)
                )
            } catch (_: Throwable) {
            }
        }
    }

    private fun acquireBoundedWakeLock(context: Context) {
        try {
            releaseWakeLockSafely()
            val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QawmiKormo:AzanWakeLock")
            wl.setReferenceCounted(false)
            wl.acquire(180_000L) // Max 3 minutes safety bound
            activeWakeLock = wl
        } catch (_: Throwable) {
        }
    }

    private fun releaseWakeLockSafely() {
        try {
            val wl = activeWakeLock
            activeWakeLock = null
            if (wl != null && wl.isHeld) {
                wl.release()
            }
        } catch (_: Throwable) {
        }
    }

    private fun releaseMediaPlayerSafely() {
        try {
            val player = activeMediaPlayer
            activeMediaPlayer = null
            if (player != null) {
                runCatching {
                    if (player.isPlaying) {
                        player.stop()
                    }
                }
                runCatching { player.reset() }
                runCatching { player.release() }
            }
        } catch (_: Throwable) {
        }
    }

    /**
     * Plays authentic recorded vocal Azan audio from bundled raw resources (100% offline capable)
     * using Android MediaPlayer over the alarm stream and stops automatically on completion.
     */
    private suspend fun kotlinx.coroutines.CoroutineScope.playRealAzanAudio(
        appContext: Context,
        soundType: AzanSoundType,
        volumeFraction: Float
    ) {
        if (soundType == AzanSoundType.NOTIFICATION_ONLY) return

        val rawResId = when (soundType) {
            AzanSoundType.FULL_AZAN -> R.raw.azan_full
            AzanSoundType.SHORT_AZAN -> R.raw.azan_short
            AzanSoundType.REMINDER_TONE -> R.raw.azan_reminder
            AzanSoundType.NOTIFICATION_ONLY -> return
        }

        releaseMediaPlayerSafely()

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val mp = MediaPlayer()
        var prepared = false

        if (soundType == AzanSoundType.REMINDER_TONE) {
            val defaultUri = runCatching {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }.getOrNull()
            if (defaultUri != null) {
                prepared = runCatching {
                    mp.setAudioAttributes(audioAttrs)
                    mp.setDataSource(appContext, defaultUri)
                    mp.isLooping = false
                    mp.prepare()
                    true
                }.getOrDefault(false)
            }
        }

        if (!prepared) {
            runCatching { mp.reset() }
            val afd = appContext.resources.openRawResourceFd(rawResId) ?: run {
                runCatching { mp.release() }
                return
            }
            afd.use { descriptor ->
                mp.setAudioAttributes(audioAttrs)
                mp.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            mp.isLooping = false
            mp.prepare()
        }

        val clampedVol = volumeFraction.coerceIn(0f, 1f)
        mp.setVolume(clampedVol, clampedVol)

        val playbackFinished = java.util.concurrent.atomic.AtomicBoolean(false)
        mp.setOnCompletionListener {
            playbackFinished.set(true)
        }
        mp.setOnErrorListener { _, _, _ ->
            playbackFinished.set(true)
            true
        }

        activeMediaPlayer = mp
        mp.start()

        val durationMs = runCatching { mp.duration.toLong() }
            .getOrDefault(140_000L)
            .coerceIn(2_000L, 240_000L) + 2_500L
        val startElapsed = System.currentTimeMillis()

        while (isActive && !playbackFinished.get()) {
            if (System.currentTimeMillis() - startElapsed > durationMs) {
                break
            }
            delay(150L)
        }
    }

    @Suppress("MissingPermission")
    private fun postAzanSystemNotification(
        context: Context,
        waqtKey: PrayerWaqtKey,
        soundType: AzanSoundType
    ) {
        if (!QawmiNotificationHelper.hasNotificationPermission(context)) return
        try {
            ensureChannelsCreated(context)
            val appContext = context.applicationContext

            val openIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_PRAYER_SCREEN, true)
            }
            val stopIntent = Intent(appContext, PrayerAzanAlarmReceiver::class.java).apply {
                action = PrayerAzanAlarmReceiver.ACTION_STOP_AZAN_PLAYBACK
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val contentPending = PendingIntent.getActivity(appContext, AZAN_NOTIFICATION_ID, openIntent, pendingFlags)
            val stopPending = PendingIntent.getBroadcast(appContext, AZAN_NOTIFICATION_ID + 1, stopIntent, pendingFlags)

            val schedule = computeDailyPrayerSchedule()
            val waqtItem = schedule.waqts.firstOrNull { it.key == waqtKey }
            val timeRangeText = if (waqtItem != null) {
                "${waqtItem.startFormattedBangla} - ${waqtItem.endFormattedBangla}"
            } else {
                ""
            }

            val title = "🕌 ${waqtKey.nameBangla} নামাজের ওয়াক্ত শুরু হয়েছে (${schedule.locationNameBangla})"
            val body = buildString {
                append("হাইয়্যা আলাস সালাহ • হাইয়্যা আলাল ফালাহ। ")
                if (timeRangeText.isNotBlank()) {
                    append("${waqtKey.nameBangla} ওয়াক্তের সময়: $timeRangeText। ")
                }
                append("সাউন্ড: ${soundType.titleBangla}।")
            }

            val builder = NotificationCompat.Builder(appContext, AZAN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_qawmi_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPending)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "আজান বন্ধ করুন",
                    stopPending
                )

            NotificationManagerCompat.from(appContext).notify(AZAN_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }

    @Suppress("MissingPermission")
    private fun postPreReminderSystemNotification(
        context: Context,
        waqtKey: PrayerWaqtKey,
        minutesBefore: Int
    ) {
        if (!QawmiNotificationHelper.hasNotificationPermission(context)) return
        try {
            ensureChannelsCreated(context)
            val appContext = context.applicationContext
            val openIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_PRAYER_SCREEN, true)
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val notifId = PRE_REMINDER_NOTIFICATION_BASE_ID + waqtKey.alarmCodeOffset
            val contentPending = PendingIntent.getActivity(appContext, notifId, openIntent, pendingFlags)

            val minBn = IslamicDateAndPrayerHelper.toBanglaDigits(minutesBefore.toString())
            val schedule = computeDailyPrayerSchedule()
            val waqtItem = schedule.waqts.firstOrNull { it.key == waqtKey }
            val startStr = waqtItem?.startFormattedBangla.orEmpty()

            val title = "⏰ ${waqtKey.nameBangla} নামাজের ওয়াক্ত শুরু হতে $minBn মিনিট বাকি"
            val body = "অজু ও প্রস্তুতির সময় হয়েছে। ${waqtKey.nameBangla} শুরু হবে $startStr (${schedule.locationNameBangla})।"

            val builder = NotificationCompat.Builder(appContext, PRE_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_qawmi_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPending)

            NotificationManagerCompat.from(appContext).notify(notifId, builder.build())
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }
}
