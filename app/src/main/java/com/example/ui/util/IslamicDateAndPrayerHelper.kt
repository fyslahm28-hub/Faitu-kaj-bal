package com.example.ui.util

import android.content.Context
import android.os.Build
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

object IslamicDateAndPrayerHelper {

    private const val PREFS_NAME = "qawmi_prayer_schedule_prefs"
    private const val KEY_PINNED_DISTRICT = "pinned_district_bangla"
    private const val KEY_SELECTED_DISTRICT = "selected_district_bangla"
    private const val KEY_FAVORITE_DISTRICTS = "favorite_districts_bangla"
    private const val DEFAULT_DISTRICT = "ঢাকা"

    data class BangladeshDistrictCoord(
        val nameBangla: String,
        val divisionBangla: String,
        val latitude: Double,
        val longitude: Double
    )

    data class PrayerWaqt(
        val nameBangla: String,
        val timeBangla: String,
        val period: String,
        val endTimeBangla: String = "",
        val endPeriod: String = period,
        val startMinutes: Int = 0,
        val endMinutes: Int = 0,
        val isCurrentWaqt: Boolean = false
    ) {
        val startWithPeriodBangla: String
            get() = "$timeBangla $period"

        val endWithPeriodBangla: String
            get() = if (endTimeBangla.isNotBlank()) "$endTimeBangla $endPeriod" else ""
    }

    data class DivisionalPrayerSchedule(
        val divisionBangla: String,
        val waqts: List<PrayerWaqt>,
        val sunriseBangla: String = "",
        val sunsetBangla: String = "",
        val currentWaqtNameBangla: String = "",
        val currentWaqtRangeBangla: String = "",
        val nextWaqtNameBangla: String = "",
        val nextWaqtStartBangla: String = "",
        val madhhabNoteBangla: String = "হানাফি মাজহাব (আসর: মিছলে ছানি)"
    )

    /**
     * Authentic coordinates for all 64 districts of Bangladesh across 8 divisions.
     */
    val allBangladeshDistrictCoords: List<BangladeshDistrictCoord> = listOf(
        // ঢাকা বিভাগ (13)
        BangladeshDistrictCoord("ঢাকা", "ঢাকা", 23.8103, 90.4125),
        BangladeshDistrictCoord("গাজীপুর", "ঢাকা", 24.0023, 90.4264),
        BangladeshDistrictCoord("নারায়ণগঞ্জ", "ঢাকা", 23.6238, 90.5000),
        BangladeshDistrictCoord("নরসিংদী", "ঢাকা", 23.9322, 90.7154),
        BangladeshDistrictCoord("মানিকগঞ্জ", "ঢাকা", 23.8644, 90.0047),
        BangladeshDistrictCoord("মুন্সীগঞ্জ", "ঢাকা", 23.5422, 90.5305),
        BangladeshDistrictCoord("টাঙ্গাইল", "ঢাকা", 24.2513, 89.9167),
        BangladeshDistrictCoord("কিশোরগঞ্জ", "ঢাকা", 24.4449, 90.7766),
        BangladeshDistrictCoord("ফরিদপুর", "ঢাকা", 23.6071, 89.8429),
        BangladeshDistrictCoord("গোপালগঞ্জ", "ঢাকা", 23.0051, 89.8266),
        BangladeshDistrictCoord("মাদারীপুর", "ঢাকা", 23.1641, 90.1897),
        BangladeshDistrictCoord("রাজবাড়ী", "ঢাকা", 23.7574, 89.6445),
        BangladeshDistrictCoord("শরীয়তপুর", "ঢাকা", 23.2423, 90.4348),

        // চট্টগ্রাম বিভাগ (11)
        BangladeshDistrictCoord("চট্টগ্রাম", "চট্টগ্রাম", 22.3569, 91.7832),
        BangladeshDistrictCoord("কক্সবাজার", "চট্টগ্রাম", 21.4272, 92.0058),
        BangladeshDistrictCoord("কুমিল্লা", "চট্টগ্রাম", 23.4607, 91.1809),
        BangladeshDistrictCoord("ব্রাহ্মণবাড়িয়া", "চট্টগ্রাম", 23.9571, 91.1119),
        BangladeshDistrictCoord("চাঁদপুর", "চট্টগ্রাম", 23.2332, 90.6713),
        BangladeshDistrictCoord("নোয়াখালী", "চট্টগ্রাম", 22.8696, 91.0994),
        BangladeshDistrictCoord("ফেনী", "চট্টগ্রাম", 23.0159, 91.3976),
        BangladeshDistrictCoord("লক্ষ্মীপুর", "চট্টগ্রাম", 22.9425, 90.8412),
        BangladeshDistrictCoord("রাঙ্গামাটি", "চট্টগ্রাম", 22.6556, 92.1754),
        BangladeshDistrictCoord("বান্দরবান", "চট্টগ্রাম", 22.1953, 92.2184),
        BangladeshDistrictCoord("খাগড়াছড়ি", "চট্টগ্রাম", 23.1193, 91.9847),

        // সিলেট বিভাগ (4)
        BangladeshDistrictCoord("সিলেট", "সিলেট", 24.8949, 91.8687),
        BangladeshDistrictCoord("মৌলভীবাজার", "সিলেট", 24.4829, 91.7774),
        BangladeshDistrictCoord("হবিগঞ্জ", "সিলেট", 24.3749, 91.4155),
        BangladeshDistrictCoord("সুনামগঞ্জ", "সিলেট", 25.0658, 91.3950),

        // রাজশাহী বিভাগ (8)
        BangladeshDistrictCoord("রাজশাহী", "রাজশাহী", 24.3745, 88.6042),
        BangladeshDistrictCoord("বগুড়া", "রাজশাহী", 24.8481, 89.3730),
        BangladeshDistrictCoord("পাবনা", "রাজশাহী", 24.0063, 89.2372),
        BangladeshDistrictCoord("সিরাজগঞ্জ", "রাজশাহী", 24.4534, 89.7007),
        BangladeshDistrictCoord("নাটোর", "রাজশাহী", 24.4206, 89.0006),
        BangladeshDistrictCoord("নওগাঁ", "রাজশাহী", 24.7936, 88.9318),
        BangladeshDistrictCoord("চাঁপাইনবাবগঞ্জ", "রাজশাহী", 24.5965, 88.2775),
        BangladeshDistrictCoord("জয়পুরহাট", "রাজশাহী", 25.0968, 89.0227),

        // খুলনা বিভাগ (10)
        BangladeshDistrictCoord("খুলনা", "খুলনা", 22.8456, 89.5403),
        BangladeshDistrictCoord("যশোর", "খুলনা", 23.1664, 89.2081),
        BangladeshDistrictCoord("কুষ্টিয়া", "খুলনা", 23.9013, 89.1204),
        BangladeshDistrictCoord("সাতক্ষীরা", "খুলনা", 22.7185, 89.0705),
        BangladeshDistrictCoord("বাগেরহাট", "খুলনা", 22.6516, 89.7859),
        BangladeshDistrictCoord("ঝিনাইদহ", "খুলনা", 23.5450, 89.1726),
        BangladeshDistrictCoord("চুয়াডাঙ্গা", "খুলনা", 23.6402, 88.8418),
        BangladeshDistrictCoord("মাগুরা", "খুলনা", 23.4873, 89.4199),
        BangladeshDistrictCoord("মেহেরপুর", "খুলনা", 23.7622, 88.6318),
        BangladeshDistrictCoord("নড়াইল", "খুলনা", 23.1725, 89.5127),

        // বরিশাল বিভাগ (6)
        BangladeshDistrictCoord("বরিশাল", "বরিশাল", 22.7010, 90.3535),
        BangladeshDistrictCoord("পটুয়াখালী", "বরিশাল", 22.3596, 90.3299),
        BangladeshDistrictCoord("ভোলা", "বরিশাল", 22.6859, 90.6482),
        BangladeshDistrictCoord("পিরোজপুর", "বরিশাল", 22.5841, 89.9720),
        BangladeshDistrictCoord("বরগুনা", "বরিশাল", 22.1592, 90.1266),
        BangladeshDistrictCoord("ঝালকাঠি", "বরিশাল", 22.6406, 90.1987),

        // রংপুর বিভাগ (8)
        BangladeshDistrictCoord("রংপুর", "রংপুর", 25.7439, 89.2752),
        BangladeshDistrictCoord("দিনাজপুর", "রংপুর", 25.6279, 88.6332),
        BangladeshDistrictCoord("কুড়িগ্রাম", "রংপুর", 25.8054, 89.6362),
        BangladeshDistrictCoord("গাইবান্ধা", "রংপুর", 25.3288, 89.5281),
        BangladeshDistrictCoord("নীলফামারী", "রংপুর", 25.9318, 88.8560),
        BangladeshDistrictCoord("পঞ্চগড়", "রংপুর", 26.3411, 88.5542),
        BangladeshDistrictCoord("ঠাকুরগাঁও", "রংপুর", 26.0337, 88.4617),
        BangladeshDistrictCoord("লালমনিরহাট", "রংপুর", 25.9923, 89.2847),

        // ময়মনসিংহ বিভাগ (4)
        BangladeshDistrictCoord("ময়মনসিংহ", "ময়মনসিংহ", 24.7471, 90.4203),
        BangladeshDistrictCoord("জামালপুর", "ময়মনসিংহ", 24.9375, 89.9378),
        BangladeshDistrictCoord("শেরপুর", "ময়মনসিংহ", 25.0205, 90.0153),
        BangladeshDistrictCoord("নেত্রকোনা", "ময়মনসিংহ", 24.8709, 90.7279)
    )

    val allDistrictsBangla: List<String> =
        listOf("সকল জেলা") + allBangladeshDistrictCoords.map { it.nameBangla }

    val selectablePrayerDistrictsBangla: List<String> =
        allBangladeshDistrictCoords.map { it.nameBangla }

    private val _pinnedDistrictFlow = MutableStateFlow(DEFAULT_DISTRICT)
    val pinnedDistrictFlow: StateFlow<String> = _pinnedDistrictFlow.asStateFlow()

    private val _selectedDistrictFlow = MutableStateFlow(DEFAULT_DISTRICT)
    val selectedDistrictFlow: StateFlow<String> = _selectedDistrictFlow.asStateFlow()

    private val _favoriteDistrictsFlow = MutableStateFlow(
        listOf("ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী")
    )
    val favoriteDistrictsFlow: StateFlow<List<String>> = _favoriteDistrictsFlow.asStateFlow()

    @Volatile
    private var prefsInitialized = false

    fun initWithContext(context: Context) {
        if (prefsInitialized) return
        synchronized(this) {
            if (prefsInitialized) return
            try {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val pinned = prefs.getString(KEY_PINNED_DISTRICT, DEFAULT_DISTRICT)
                    ?.takeIf { findDistrictCoord(it) != null }
                    ?: DEFAULT_DISTRICT
                // Pinned district always takes priority when opening the app
                _pinnedDistrictFlow.value = pinned
                _selectedDistrictFlow.value = pinned

                val savedFavsRaw = prefs.getString(KEY_FAVORITE_DISTRICTS, null)
                if (!savedFavsRaw.isNullOrBlank()) {
                    val parsed = savedFavsRaw.split(",")
                        .map { it.trim() }
                        .filter { findDistrictCoord(it) != null }
                        .distinct()
                    if (parsed.isNotEmpty()) {
                        val withPinnedFirst = listOf(pinned) + parsed.filter { it != pinned }
                        _favoriteDistrictsFlow.value = withPinnedFirst
                    }
                } else {
                    val defaults = listOf(pinned, "ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী").distinct()
                    _favoriteDistrictsFlow.value = defaults
                }
                prefsInitialized = true
            } catch (_: Exception) {
                // Keep defaults in test/preview environments if prefs unavailable
            }
        }
    }

    fun selectDistrict(context: Context?, districtBangla: String) {
        val valid = findDistrictCoord(districtBangla)?.nameBangla ?: return
        _selectedDistrictFlow.value = valid
        try {
            context?.applicationContext
                ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_SELECTED_DISTRICT, valid)
                ?.apply()
        } catch (_: Exception) {
        }
    }

    fun pinDistrict(context: Context?, districtBangla: String) {
        val valid = findDistrictCoord(districtBangla)?.nameBangla ?: return
        _pinnedDistrictFlow.value = valid
        _selectedDistrictFlow.value = valid
        val updatedFavs = (listOf(valid) + _favoriteDistrictsFlow.value.filter { it != valid }).take(10)
        _favoriteDistrictsFlow.value = updatedFavs
        try {
            context?.applicationContext
                ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_PINNED_DISTRICT, valid)
                ?.putString(KEY_SELECTED_DISTRICT, valid)
                ?.putString(KEY_FAVORITE_DISTRICTS, updatedFavs.joinToString(","))
                ?.apply()
        } catch (_: Exception) {
        }
    }

    fun toggleFavoriteDistrict(context: Context?, districtBangla: String) {
        val valid = findDistrictCoord(districtBangla)?.nameBangla ?: return
        val current = _favoriteDistrictsFlow.value
        val pinned = _pinnedDistrictFlow.value
        val updated = if (current.contains(valid)) {
            if (valid == pinned && current.size <= 1) {
                current
            } else {
                current.filter { it != valid }.ifEmpty { listOf(pinned) }
            }
        } else {
            (current + valid).distinct().take(10)
        }
        _favoriteDistrictsFlow.value = updated
        try {
            context?.applicationContext
                ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_FAVORITE_DISTRICTS, updated.joinToString(","))
                ?.apply()
        } catch (_: Exception) {
        }
    }

    fun findDistrictCoord(nameBangla: String): BangladeshDistrictCoord? {
        val trimmed = nameBangla.trim()
        return allBangladeshDistrictCoords.firstOrNull { it.nameBangla == trimmed }
    }

    /**
     * Dynamically computed schedules for the 8 divisions of Bangladesh using today's date
     * and the Hanafi Madhhab calculation method.
     */
    val divisionsSchedule: List<DivisionalPrayerSchedule>
        get() = listOf(
            "ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী",
            "খুলনা", "বরিশাল", "রংপুর", "ময়মনসিংহ"
        ).map { division ->
            getScheduleForDistrict(division)
        }

    /**
     * Computes the authentic Hanafi Madhhab prayer schedule (start & end times for all 5 prayers,
     * sunrise, sunset, current waqt status, and next prayer) for any district in Bangladesh on the
     * given date and local time.
     *
     * Calculation Method:
     * - Timezone: Asia/Dhaka (UTC+6)
     * - Fajr Angle: 18.0° below eastern horizon (University of Islamic Sciences, Karachi / IFB)
     * - Sunrise / Sunset Angle: 0.8333° below horizon (solar limb + atmospheric refraction)
     * - Dhuhr Start: Solar transit (Zawal) + 3 minutes precautionary margin (ইহতিয়াত)
     * - Asr Start (Hanafi Madhhab): Shadow ratio = 2 (মিছলে ছানি: দ্বিগুণ ছায়া + যোহরের ছায়া)
     * - Maghrib Start: Sunset + 3 minutes precautionary margin
     * - Isha Start: 18.0° below western horizon
     */
    fun getScheduleForDistrict(
        districtBangla: String,
        calendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
    ): DivisionalPrayerSchedule {
        val coord = findDistrictCoord(districtBangla)
            ?: allBangladeshDistrictCoords.first() // Default Dhaka

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 1..12
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        return calculateHanafiScheduleForCoordinates(
            districtNameBangla = coord.nameBangla,
            latitude = coord.latitude,
            longitude = coord.longitude,
            year = year,
            month = month,
            day = day,
            nowMinutesOfDay = nowMinutes
        )
    }

    fun calculateHanafiScheduleForCoordinates(
        districtNameBangla: String,
        latitude: Double,
        longitude: Double,
        year: Int,
        month: Int,
        day: Int,
        nowMinutesOfDay: Int = 12 * 60
    ): DivisionalPrayerSchedule {
        val tzOffsetHours = 6.0 // Bangladesh Standard Time (UTC+6)

        // 1. Julian Date at local 12:00 noon
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

        // 2. Solar Position (Declination & Equation of Time)
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

        // 3. Solar Transit (Midday / Zawal center in hours 0..24)
        val transitHours = 12.0 + tzOffsetHours - (longitude / 15.0) - eqTimeHours

        val latRad = Math.toRadians(latitude)

        fun hourAngleOffsetHours(altitudeDeg: Double): Double {
            val altRad = Math.toRadians(altitudeDeg)
            val numerator = sin(altRad) - sin(latRad) * sin(declRad)
            val denominator = cos(latRad) * cos(declRad)
            val cosH = (numerator / denominator).coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(cosH)) / 15.0
        }

        // Fajr & Isha: 18° depression below horizon
        val fajrOffset = hourAngleOffsetHours(-18.0)
        // Sunrise & Sunset: 0.8333° depression below horizon
        val sunHorizonOffset = hourAngleOffsetHours(-0.8333)

        // Hanafi Asr: Shadow factor = 2 (মিছলে ছানি — 2x object height + noon shadow)
        val zenithDistanceRad = Math.toRadians(abs(latitude - declDeg))
        val hanafiShadowRatio = 2.0 + tan(zenithDistanceRad)
        val asrAltitudeDeg = Math.toDegrees(atan(1.0 / hanafiShadowRatio))
        val asrOffset = hourAngleOffsetHours(asrAltitudeDeg)

        // Convert to minutes from midnight (with Bangladesh Islamic Foundation precautionary margins)
        val fajrStartMin = ((transitHours - fajrOffset) * 60.0).roundToInt().coerceIn(0, 1439)
        val sunriseMin = ((transitHours - sunHorizonOffset) * 60.0).roundToInt().coerceIn(0, 1439)
        val fajrEndMin = (sunriseMin - 1).coerceAtLeast(fajrStartMin + 1)

        val transitMin = (transitHours * 60.0).roundToInt().coerceIn(0, 1439)
        val dhuhrStartMin = (transitMin + 3).coerceIn(0, 1439) // +3 min post-Zawal ihtiyat
        val asrStartMin = (((transitHours + asrOffset) * 60.0).roundToInt() + 3).coerceIn(0, 1439) // +3 min Hanafi ihtiyat
        val dhuhrEndMin = (asrStartMin - 1).coerceAtLeast(dhuhrStartMin + 1)

        val sunsetMin = ((transitHours + sunHorizonOffset) * 60.0).roundToInt().coerceIn(0, 1439)
        val asrEndMin = sunsetMin
        val maghribStartMin = (sunsetMin + 3).coerceIn(0, 1439) // +3 min post-sunset ihtiyat
        val ishaStartMin = ((transitHours + fajrOffset) * 60.0).roundToInt().coerceIn(0, 1439)
        val maghribEndMin = (ishaStartMin - 1).coerceAtLeast(maghribStartMin + 1)
        val ishaEndMin = (fajrStartMin - 1).coerceAtLeast(0)

        // Determine current active waqt & next prayer based on nowMinutesOfDay
        val isFajrActive = nowMinutesOfDay in fajrStartMin..fajrEndMin
        val isDhuhrActive = nowMinutesOfDay in dhuhrStartMin..dhuhrEndMin
        val isAsrActive = nowMinutesOfDay in asrStartMin..asrEndMin
        val isMaghribActive = nowMinutesOfDay in maghribStartMin..maghribEndMin
        val isIshaActive = nowMinutesOfDay >= ishaStartMin || nowMinutesOfDay <= ishaEndMin

        val (currentWaqtTitle, currentWaqtRange, nextWaqtTitle, nextWaqtStart) = when {
            isFajrActive -> Quadruple(
                "চলমান ওয়াক্ত: ফজর",
                "${formatMinutes12hBangla(fajrStartMin)} - ${formatMinutes12hBangla(fajrEndMin)}",
                "পরবর্তী নামাজ: যোহর",
                formatMinutes12hBangla(dhuhrStartMin)
            )
            nowMinutesOfDay in sunriseMin..(sunriseMin + 15) -> Quadruple(
                "সূর্যোদয় চলছে (নামাজ নিষিদ্ধ সময়)",
                "${formatMinutes12hBangla(sunriseMin)} - ${formatMinutes12hBangla(sunriseMin + 15)}",
                "পরবর্তী নামাজ: যোহর",
                formatMinutes12hBangla(dhuhrStartMin)
            )
            nowMinutesOfDay in (sunriseMin + 16) until (transitMin - 5) -> Quadruple(
                "চাশত / ইশরাক (নফল ওয়াক্ত)",
                "${formatMinutes12hBangla(sunriseMin + 15)} - ${formatMinutes12hBangla(transitMin - 5)}",
                "পরবর্তী নামাজ: যোহর",
                formatMinutes12hBangla(dhuhrStartMin)
            )
            nowMinutesOfDay in (transitMin - 5) until dhuhrStartMin -> Quadruple(
                "দ্বিপ্রহর / যাওয়াল (নিষিদ্ধ সময়)",
                "${formatMinutes12hBangla(transitMin - 5)} - ${formatMinutes12hBangla(dhuhrStartMin)}",
                "পরবর্তী নামাজ: যোহর",
                formatMinutes12hBangla(dhuhrStartMin)
            )
            isDhuhrActive -> Quadruple(
                "চলমান ওয়াক্ত: যোহর",
                "${formatMinutes12hBangla(dhuhrStartMin)} - ${formatMinutes12hBangla(dhuhrEndMin)}",
                "পরবর্তী নামাজ: আসর (হানাফি)",
                formatMinutes12hBangla(asrStartMin)
            )
            isAsrActive -> Quadruple(
                "চলমান ওয়াক্ত: আসর (হানাফি)",
                "${formatMinutes12hBangla(asrStartMin)} - ${formatMinutes12hBangla(asrEndMin)}",
                "পরবর্তী নামাজ: মাগরিব",
                formatMinutes12hBangla(maghribStartMin)
            )
            nowMinutesOfDay in (asrEndMin + 1) until maghribStartMin -> Quadruple(
                "সূর্যাস্ত সতর্কতা সময়",
                "${formatMinutes12hBangla(sunsetMin)} - ${formatMinutes12hBangla(maghribStartMin)}",
                "পরবর্তী নামাজ: মাগরিব",
                formatMinutes12hBangla(maghribStartMin)
            )
            isMaghribActive -> Quadruple(
                "চলমান ওয়াক্ত: মাগরিব",
                "${formatMinutes12hBangla(maghribStartMin)} - ${formatMinutes12hBangla(maghribEndMin)}",
                "পরবর্তী নামাজ: এশা",
                formatMinutes12hBangla(ishaStartMin)
            )
            else -> Quadruple(
                "চলমান ওয়াক্ত: এশা",
                "${formatMinutes12hBangla(ishaStartMin)} - ${formatMinutes12hBangla(ishaEndMin)}",
                "পরবর্তী নামাজ: ফজর",
                formatMinutes12hBangla(fajrStartMin)
            )
        }

        val waqtList = listOf(
            buildPrayerWaqt("ফজর", fajrStartMin, fajrEndMin, isFajrActive),
            buildPrayerWaqt("যোহর", dhuhrStartMin, dhuhrEndMin, isDhuhrActive),
            buildPrayerWaqt("আসর", asrStartMin, asrEndMin, isAsrActive),
            buildPrayerWaqt("মাগরিব", maghribStartMin, maghribEndMin, isMaghribActive),
            buildPrayerWaqt("এশা", ishaStartMin, ishaEndMin, isIshaActive)
        )

        return DivisionalPrayerSchedule(
            divisionBangla = districtNameBangla,
            waqts = waqtList,
            sunriseBangla = formatMinutes12hBangla(sunriseMin),
            sunsetBangla = formatMinutes12hBangla(sunsetMin),
            currentWaqtNameBangla = currentWaqtTitle,
            currentWaqtRangeBangla = currentWaqtRange,
            nextWaqtNameBangla = nextWaqtTitle,
            nextWaqtStartBangla = nextWaqtStart,
            madhhabNoteBangla = "হানাফি মাজহাব (আসর: মিছলে ছানি)"
        )
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    private fun buildPrayerWaqt(
        nameBangla: String,
        startMinutes: Int,
        endMinutes: Int,
        isCurrent: Boolean
    ): PrayerWaqt {
        val (startClock, startPeriod) = splitMinutesTo12hAndPeriod(startMinutes)
        val (endClock, endPeriod) = splitMinutesTo12hAndPeriod(endMinutes)
        return PrayerWaqt(
            nameBangla = nameBangla,
            timeBangla = startClock,
            period = startPeriod,
            endTimeBangla = endClock,
            endPeriod = endPeriod,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            isCurrentWaqt = isCurrent
        )
    }

    private fun splitMinutesTo12hAndPeriod(totalMinutes: Int): Pair<String, String> {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val hour24 = normalized / 60
        val minute = normalized % 60
        val period = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formatted = String.format(Locale.ENGLISH, "%02d:%02d", hour12, minute)
        return toBanglaDigits(formatted) to period
    }

    fun formatMinutes12hBangla(totalMinutes: Int): String {
        val (clock, period) = splitMinutesTo12hAndPeriod(totalMinutes)
        return "$clock $period"
    }

    private fun normalizeDegrees(deg: Double): Double {
        var rem = deg % 360.0
        if (rem < 0.0) rem += 360.0
        return rem
    }

    data class DailyHadith(
        val textBangla: String,
        val referenceBangla: String
    )

    private val hadithCollection = listOf(
        DailyHadith(
            "“জ্ঞান অর্জন করা প্রত্যেক মুসলিমের উপর ফরজ।”",
            "– (ইবনে মাজাহ, ২২৪)"
        ),
        DailyHadith(
            "“তোমাদের মধ্যে সর্বোত্তম ঐ ব্যক্তি, যে নিজে কুরআন শেখে এবং অন্যকে শেখায়।”",
            "– (সহীহ বুখারী, ৫০২৭)"
        ),
        DailyHadith(
            "“আল্লাহ যার কল্যাণ চান, তাকে দ্বীনের গভীর প্রজ্ঞা দান করেন।”",
            "– (সহীহ বুখারী, ৭১)"
        ),
        DailyHadith(
            "“আলিমগণ হলেন নবীগণের উত্তরাধিকারী।”",
            "– (সুনানে আবু দাউদ, ৩৬৪১)"
        )
    )

    fun getTodayHadith(): DailyHadith {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        return hadithCollection[abs(dayOfYear) % hadithCollection.size]
    }

    fun toBanglaDigits(input: String): String {
        val map = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return input.map { map[it] ?: it }.joinToString("")
    }

    fun getFormattedHijriDate(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hijri = HijrahDate.now()
                val day = hijri.get(ChronoField.DAY_OF_MONTH)
                val month = hijri.get(ChronoField.MONTH_OF_YEAR)
                val year = hijri.get(ChronoField.YEAR)
                val hijriMonths = listOf(
                    "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
                    "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শাবান",
                    "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
                )
                val monthName = hijriMonths.getOrElse(month - 1) { "রবিউল আউয়াল" }
                "${toBanglaDigits(day.toString())} $monthName ${toBanglaDigits(year.toString())} হিজরি"
            } else {
                "১০ রবিউল আউয়াল ১৪৪৭ হিজরি"
            }
        } catch (e: Exception) {
            "১০ রবিউল আউয়াল ১৪৪৭ হিজরি"
        }
    }

    fun getFormattedGregorianDate(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val now = LocalDate.now()
                val day = now.dayOfMonth
                val monthIndex = now.monthValue - 1
                val year = now.year
                val banglaMonths = listOf(
                    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
                    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
                )
                val monthName = banglaMonths.getOrElse(monthIndex) { "অক্টোবর" }
                "${toBanglaDigits(day.toString())} $monthName ${toBanglaDigits(year.toString())}"
            } else {
                "২১ অক্টোবর ২০২৬"
            }
        } catch (e: Exception) {
            "২১ অক্টোবর ২০২৬"
        }
    }

    fun getFormattedBanglaDayOfWeek(): String {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "শনিবার"
            Calendar.SUNDAY -> "রবিবার"
            Calendar.MONDAY -> "সোমবার"
            Calendar.TUESDAY -> "মঙ্গলবার"
            Calendar.WEDNESDAY -> "বুধবার"
            Calendar.THURSDAY -> "বৃহস্পতিবার"
            Calendar.FRIDAY -> "শুক্রবার (জুমুআবার)"
            else -> "রবিবার"
        }
    }

    fun formatRelativeTimeBangla(timestamp: Timestamp?): String {
        if (timestamp == null) return "এইমাত্র"
        val nowMillis = System.currentTimeMillis()
        val postMillis = timestamp.toDate().time
        val diffMinutes = ((nowMillis - postMillis) / (1000 * 60)).coerceAtLeast(0)

        return when {
            diffMinutes < 2 -> "এইমাত্র"
            diffMinutes < 60 -> "${toBanglaDigits(diffMinutes.toString())} মিনিট আগে"
            diffMinutes < 1440 -> {
                val hours = diffMinutes / 60
                "${toBanglaDigits(hours.toString())} ঘণ্টা আগে"
            }
            else -> {
                val days = diffMinutes / 1440
                "${toBanglaDigits(days.toString())} দিন আগে"
            }
        }
    }
}
