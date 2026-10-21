package com.example.ui.util

import android.os.Build
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

object IslamicDateAndPrayerHelper {

    data class PrayerWaqt(
        val nameBangla: String,
        val timeBangla: String,
        val period: String
    )

    data class DivisionalPrayerSchedule(
        val divisionBangla: String,
        val waqts: List<PrayerWaqt>
    )

    val divisionsSchedule = listOf(
        DivisionalPrayerSchedule(
            divisionBangla = "ঢাকা",
            waqts = listOf(
                PrayerWaqt("ফজর", "০৪:৪২", "AM"),
                PrayerWaqt("যোহর", "১২:০০", "PM"),
                PrayerWaqt("আসর", "০৪:৪৫", "PM"),
                PrayerWaqt("মাগরিব", "০৬:২০", "PM"),
                PrayerWaqt("এশা", "০৭:৩৮", "PM")
            )
        ),
        DivisionalPrayerSchedule(
            divisionBangla = "চট্টগ্রাম",
            waqts = listOf(
                PrayerWaqt("ফজর", "০৪:৩৭", "AM"),
                PrayerWaqt("যোহর", "১১:৫৫", "AM"),
                PrayerWaqt("আসর", "০৪:৪০", "PM"),
                PrayerWaqt("মাগরিব", "০৬:১৫", "PM"),
                PrayerWaqt("এশা", "০৭:৩৩", "PM")
            )
        ),
        DivisionalPrayerSchedule(
            divisionBangla = "সিলেট",
            waqts = listOf(
                PrayerWaqt("ফজর", "০৪:৩৬", "AM"),
                PrayerWaqt("যোহর", "১১:৫৪", "AM"),
                PrayerWaqt("আসর", "০৪:৩৯", "PM"),
                PrayerWaqt("মাগরিব", "০৬:১৪", "PM"),
                PrayerWaqt("এশা", "০৭:৩২", "PM")
            )
        ),
        DivisionalPrayerSchedule(
            divisionBangla = "রাজশাহী",
            waqts = listOf(
                PrayerWaqt("ফজর", "০৪:৪৮", "AM"),
                PrayerWaqt("যোহর", "১২:০৬", "PM"),
                PrayerWaqt("আসর", "০৪:৫১", "PM"),
                PrayerWaqt("মাগরিব", "০৬:২৬", "PM"),
                PrayerWaqt("এশা", "০৭:৪৪", "PM")
            )
        ),
        DivisionalPrayerSchedule(
            divisionBangla = "খুলনা",
            waqts = listOf(
                PrayerWaqt("ফজর", "০৪:৪৬", "AM"),
                PrayerWaqt("যোহর", "১২:০৪", "PM"),
                PrayerWaqt("আসর", "০৪:৪৯", "PM"),
                PrayerWaqt("মাগরিব", "০৬:২৪", "PM"),
                PrayerWaqt("এশা", "০৭:৪২", "PM")
            )
        ),
        DivisionalPrayerSchedule(
            divisionBangla = "বরিশাল",
            waqts = listOf(
                PrayerWaqt("ফজর", "০৪:৪৩", "AM"),
                PrayerWaqt("যোহর", "১২:০১", "PM"),
                PrayerWaqt("আসর", "০৪:৪৬", "PM"),
                PrayerWaqt("মাগরিব", "০৬:২১", "PM"),
                PrayerWaqt("এশা", "০৭:৩৯", "PM")
            )
        ),
        DivisionalPrayerSchedule(
            divisionBangla = "রংপুর",
            waqts = listOf(
                PrayerWaqt("ফজর", "০৪:৪৭", "AM"),
                PrayerWaqt("যোহর", "১২:০৫", "PM"),
                PrayerWaqt("আসর", "০৪:৫০", "PM"),
                PrayerWaqt("মাগরিব", "০৬:২৫", "PM"),
                PrayerWaqt("এশা", "০৭:৪৩", "PM")
            )
        ),
        DivisionalPrayerSchedule(
            divisionBangla = "ময়মনসিংহ",
            waqts = listOf(
                PrayerWaqt("ফজর", "০৪:৪১", "AM"),
                PrayerWaqt("যোহর", "১১:৫৯", "AM"),
                PrayerWaqt("আসর", "০৪:৪৪", "PM"),
                PrayerWaqt("মাগরিব", "০৬:১৯", "PM"),
                PrayerWaqt("এশা", "০৭:৩৭", "PM")
            )
        )
    )

    val allDistrictsBangla = listOf(
        "সকল জেলা", "ঢাকা", "কুমিল্লা", "নোয়াখালী", "সিলেট", "রাজশাহী", "চট্টগ্রাম",
        "বরিশাল", "ময়মনসিংহ", "খুলনা", "রংপুর", "বগুড়া", "কিশোরগঞ্জ", "ব্রাহ্মণবাড়িয়া",
        "ফেনী", "লক্ষ্মীপুর", "চাঁদপুর", "গাজীপুর", "নারায়ণগঞ্জ", "টাঙ্গাইল", "ফরিদপুর",
        "যশোর", "কুষ্টিয়া", "নাটোর", "পাবনা", "সিরাজগঞ্জ", "দিনাজপুর", "হবিগঞ্জ",
        "মৌলভীবাজার", "সুনামগঞ্জ", "কক্সবাজার", "পটুয়াখালী", "ভোলা", "ঝালকাঠি"
    )

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
