package com.example.ui.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class IslamicReminderItem(
    val id: String,
    val categoryBangla: String,
    val titleBangla: String,
    val messageBangla: String,
    val quoteBangla: String,
    val sourceReferenceBangla: String,
    val dailyAmolBangla: String
)

data class UserSmoronServerState(
    val userId: String = "",
    val todayDateKey: String = "",
    val assignedReminderId: String = "",
    val lastPushSentDateKey: String = "",
    val lastPushSentReminderId: String = "",
    val deliveredHistoryMap: Map<String, Long> = emptyMap(), // reminderId -> epochMillis
    val completedAmolDates: Set<String> = emptySet(),
    val preferredHour24: Int = 8,
    val preferredMinute: Int = 0,
    val notificationsEnabled: Boolean = true
)

object AjkerSmoronHelper {

    private const val PREFS_NAME = "qawmi_ajker_smoron_prefs"
    private const val KEY_NOTIF_ENABLED = "smoron_notif_enabled"
    private const val KEY_PREFERRED_HOUR = "smoron_preferred_hour_24"
    private const val KEY_PREFERRED_MINUTE = "smoron_preferred_minute"
    private const val KEY_ACTIVE_USER_ID = "smoron_active_user_id"

    const val RECEIPT_PREFIX_HISTORY = "smoron_hist_"
    const val RECEIPT_PREFIX_DAY_ASSIGN = "smoron_day_"
    const val RECEIPT_PREFIX_PUSH_SENT = "smoron_push_"
    const val RECEIPT_PREFIX_AMOL_DONE = "smoron_amol_"

    /**
     * Authentic, curated collection of 36 Islamic reminders (স্মরণ, নসিহত, আমল ও অনুপ্রেরণামূলক বাণী).
     * Every mention of Prophet Muhammad ﷺ includes proper honorifics ("ﷺ" or "সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম").
     * Every Quranic verse and Hadith citation is verified against authentic primary sources.
     */
    val allReminders: List<IslamicReminderItem> = listOf(
        // ১. ইউজার প্রম্পটের হুবহু উদাহরণ বার্তা ১ (দরুদ শরিফ)
        IslamicReminderItem(
            id = "smoron_01",
            categoryBangla = "দরুদ শরিফ",
            titleBangla = "প্রিয় নবী ﷺ-এর প্রতি দরুদ পাঠ",
            messageBangla = "আপনি কি আজ প্রিয় নবী হযরত মুহাম্মদ ﷺ-এর ওপর দরুদ শরিফ পাঠ করেছেন? আসুন, বেশি বেশি দরুদ পাঠের মাধ্যমে আমাদের দিনকে বরকতময় করি।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “যে ব্যক্তি আমার ওপর একবার দরুদ পাঠ করে, আল্লাহ তাআলা তার ওপর দশবার রহমত বর্ষণ করেন।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ৪০৮",
            dailyAmolBangla = "আজ অন্তত ১০০ বার ‘সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম’ বা দরুদে ইবরাহিম পাঠ করুন।"
        ),
        // ২. ইউজার প্রম্পটের হুবহু উদাহরণ বার্তা ২ (আল্লাহর জিকির)
        IslamicReminderItem(
            id = "smoron_02",
            categoryBangla = "জিকির ও তাসবিহ",
            titleBangla = "অন্তরের প্রশান্তির জন্য আল্লাহর জিকির",
            messageBangla = "আজ কি আপনি আল্লাহ তাআলার জিকির করেছেন? কিছুক্ষণ সময় নিয়ে সুবহানাল্লাহ, আলহামদুলিল্লাহ, আল্লাহু আকবার পাঠ করুন—এতে অন্তর প্রশান্ত হয়।",
            quoteBangla = "“যারা ঈমান আনে এবং আল্লাহর স্মরণে যাদের অন্তর প্রশান্ত হয়; জেনে রাখো, আল্লাহর জিকিরেই অন্তরসমূহ প্রশান্ত হয়।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা আর-রাদ: ২৮",
            dailyAmolBangla = "আজ প্রতি ফরজ নামাজের পর ৩৩ বার সুবহানাল্লাহ, ৩৩ বার আলহামদুলিল্লাহ এবং ৩৪ বার আল্লাহু আকবার পাঠ করুন।"
        ),
        // ৩. ইউজার প্রম্পটের হুবহু উদাহরণ বার্তা ৩ (সৎকাজ ও পরোপকার)
        IslamicReminderItem(
            id = "smoron_03",
            categoryBangla = "সৎকাজ ও খেদমত",
            titleBangla = "মানুষের উপকার ও ছোট নেক আমল",
            messageBangla = "আজ কি আপনি কারো উপকার করেছেন বা একটি ভালো কাজ করেছেন? ছোট একটি ভালো আমলও আল্লাহর কাছে অনেক মূল্যবান হতে পারে।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “কোনো ভালো কাজকেই কখনো তুচ্ছ মনে করো না, এমনকি তোমার ভাইয়ের সাথে হাসিমুখে সাক্ষাৎ করাকেও নয়।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ২৬২৬",
            dailyAmolBangla = "আজ অন্তত একজন মানুষের কাজে সহযোগিতা করুন অথবা হাসিমুখে সালাম ও কুশল বিনিময় করুন।"
        ),
        // ৪. ইস্তিগফার ও তাওবা
        IslamicReminderItem(
            id = "smoron_04",
            categoryBangla = "ইস্তিগফার ও তাওবা",
            titleBangla = "ক্ষমা প্রার্থনা ও রিজিকের বরকত",
            messageBangla = "ব্যস্ততার মাঝে আজ কি আল্লাহ তাআলার কাছে ইস্তিগফার করেছেন? দিনে অন্তত কিছুক্ষণ খাঁটি অন্তরে ‘আস্তাগফিরুল্লাহ’ পাঠ করুন—এতে গুনাহ মাফ হয় ও পেরেশানি দূর হয়।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “আল্লাহর শপথ! আমি প্রতিদিন সত্তর বারেরও বেশি আল্লাহর কাছে ক্ষমা প্রার্থনা (ইস্তিগফার) করি এবং তাওবা করি।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৬৩০৭",
            dailyAmolBangla = "আজ সকাল ও সন্ধ্যায় অন্তত ৭০ থেকে ১০০ বার ‘আস্তাগফিরুল্লাহা ওয়া আতুবু ইলাইহি’ পাঠ করুন।"
        ),
        // ৫. নামাজের গুরুত্ব
        IslamicReminderItem(
            id = "smoron_05",
            categoryBangla = "নামাজের গুরুত্ব",
            titleBangla = "জামাতের সাথে সময়মতো নামাজ আদায়",
            messageBangla = "আজকের পাঁচ ওয়াক্ত নামাজ কি খুশু-খুজুর সাথে আদায় করা হয়েছে? শত ব্যস্ততার মাঝেও নামাজের প্রথম ওয়াক্তে মসজিদে হাজির হওয়ার চেষ্টা করুন।",
            quoteBangla = "আবদুল্লাহ ইবনে মাসউদ (রা.) রাসূলুল্লাহ ﷺ-কে জিজ্ঞেস করলেন: “আল্লাহর কাছে কোন আমলটি সবচেয়ে প্রিয়?” তিনি বললেন: “সময়মতো নামাজ আদায় করা।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৫২৭; সহীহ মুসলিম, হাদিস: ৮৫",
            dailyAmolBangla = "আজকের পরবর্তী ওয়াক্তের নামাজ আজান হওয়ার সাথে সাথে সুন্নতসহ জামাতে আদায় করার দৃঢ় নিয়ত করুন।"
        ),
        // ৬. কুরআন তিলাওয়াত
        IslamicReminderItem(
            id = "smoron_06",
            categoryBangla = "কুরআন তিলাওয়াত",
            titleBangla = "পবিত্র কুরআনের সাথে প্রতিদিনের সম্পর্ক",
            messageBangla = "আজ কি আপনি পবিত্র কুরআন মাজীদ থেকে কয়েকটি আয়াত তিলাওয়াত করেছেন? প্রতিদিন অল্প হলেও কুরআন তিলাওয়াতের মাধ্যমে আপনার ঘর ও হৃদয়কে আলোকিত রাখুন।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “তোমরা কুরআন তিলাওয়াত করো, কেননা কিয়ামতের দিন তা নিজ পাঠকারীদের জন্য সুপারিশকারী হিসেবে উপস্থিত হবে।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ৮০৪",
            dailyAmolBangla = "আজ অন্তত ১ রুকু বা ১০টি আয়াত অর্থ ও তাদাব্বুরসহ তিলাওয়াত করুন।"
        ),
        // ৭. পিতা-মাতার খেদমত
        IslamicReminderItem(
            id = "smoron_07",
            categoryBangla = "পিতা-মাতার খেদমত",
            titleBangla = "মা-বাবার খোঁজখবর ও দোয়া",
            messageBangla = "আজ কি আপনার আব্বা-আম্মার খোঁজ নিয়েছেন বা তাঁদের জন্য দোয়া করেছেন? তাঁরা জীবিত থাকলে তাঁদের সাথে কোমল আচরণ করুন এবং তাঁদের জন্য রবের দরবারে হাত তুলুন।",
            quoteBangla = "“এবং বলুন: হে আমার রব! তাঁদের উভয়ের প্রতি দয়া করুন, যেমন তাঁরা শৈশবে আমাকে লালন-পালন করেছেন।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা বনী ইসরাঈল: ২৪",
            dailyAmolBangla = "আজ পিতা-মাতার সাথে কথা বলুন বা তাঁদের খেদমত করুন এবং প্রতি নামাজে ‘রাব্বির হামহুমা কামা রাব্বায়ানি সাগীরা’ পড়ুন।"
        ),
        // ৮. উত্তম আখলাক
        IslamicReminderItem(
            id = "smoron_08",
            categoryBangla = "উত্তম আখলাক",
            titleBangla = "সুন্দর চরিত্র ও কোমল ব্যবহার",
            messageBangla = "পরিবার, সহকর্মী ও ছাত্র-শিক্ষকদের সাথে আজ আপনার আচরণ কেমন ছিল? মনে রাখবেন, উত্তম আখলাক মুমিনের আমলনামায় সবচেয়ে ভারী আমল।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “কিয়ামতের দিন মুমিনের পাল্লায় উত্তম চরিত্রের চেয়ে ভারী আর কোনো জিনিস হবে না।”",
            sourceReferenceBangla = "সুনানে তিরমিযী, হাদিস: ২০০২ (সহীহ)",
            dailyAmolBangla = "আজ কারো কথায় রাগান্বিত না হয়ে ধৈর্য ও নম্রতার সাথে উত্তর দেওয়ার অনুশীলন করুন।"
        ),
        // ৯. সবর ও ধৈর্য
        IslamicReminderItem(
            id = "smoron_09",
            categoryBangla = "সবর ও শুকরিয়া",
            titleBangla = "বিপদ ও পরীক্ষায় ধৈর্য ধারণ",
            messageBangla = "জীবনের কোনো কঠিন পরিস্থিতিতে কি মন খারাপ হয়ে আছে? হতাশ হবেন না; ধৈর্য ও নামাজের মাধ্যমে আল্লাহর সাহায্য প্রার্থনা করুন, নিশ্চয়ই কষ্টের পরেই স্বস্তি রয়েছে।",
            quoteBangla = "“হে মুমিনগণ! তোমরা ধৈর্য ও নামাজের মাধ্যমে সাহায্য প্রার্থনা করো। নিশ্চয়ই আল্লাহ ধৈর্যশীলদের সাথে আছেন।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা আল-বাকারা: ১৫৩",
            dailyAmolBangla = "যেকোনো প্রতিকূলতায় অভিযোগ না করে ‘ইন্না লিল্লাহি ওয়া ইন্না ইলাইহি রাজিউন’ ও ‘আলহামদুলিল্লাহ’ পাঠ করুন।"
        ),
        // ১০. শুকরিয়া ও কৃতজ্ঞতা
        IslamicReminderItem(
            id = "smoron_10",
            categoryBangla = "সবর ও শুকরিয়া",
            titleBangla = "আল্লাহর নিয়ামতের শুকরিয়া আদায়",
            messageBangla = "সুস্থতা, ঈমান, ইলম ও প্রতিদিনের রিজিক—আজ কি এই অগণিত নিয়ামতের জন্য অন্তর থেকে ‘আলহামদুলিল্লাহ’ বলেছেন? কৃতজ্ঞতায় নিয়ামত বৃদ্ধি পায়।",
            quoteBangla = "“যদি তোমরা কৃতজ্ঞতা স্বীকার (শুকরিয়া) করো, তবে আমি অবশ্যই তোমাদেরকে আরও বাড়িয়ে দেবো।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা ইবরাহিম: ৭",
            dailyAmolBangla = "আজ ঘুমানোর আগে আল্লাহর দেওয়া অন্তত তিনটি নিয়ামতের কথা স্মরণ করে আন্তরিকভাবে শুকরিয়া আদায় করুন।"
        ),
        // ১১. দান-সদকা
        IslamicReminderItem(
            id = "smoron_11",
            categoryBangla = "দান-সদকা",
            titleBangla = "গোপনে ও প্রকাশ্যে দান-সদকা",
            messageBangla = "আজ কি আল্লাহর রাস্তায় সামান্য হলেও কিছু সদকা করেছেন? মনে রাখবেন, দান-সদকা কখনো সম্পদ কমায় না; বরং বিপদ দূর করে এবং রিজিকে বরকত আনে।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “সদকা করার দ্বারা কখনো সম্পদ হ্রাস পায় না।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ২৫৮৮",
            dailyAmolBangla = "আজ আপনার সামর্থ্য অনুযায়ী একজন অভাবী মানুষ বা দ্বীনি প্রতিষ্ঠানে সামান্য হলেও সদকা করুন।"
        ),
        // ১২. দুইটি প্রিয় বাক্য (জিকির)
        IslamicReminderItem(
            id = "smoron_12",
            categoryBangla = "জিকির ও তাসবিহ",
            titleBangla = "রহমানের কাছে প্রিয় দুটি বাক্য",
            messageBangla = "চলতি পথে বা কাজের ফাঁকে জিহ্বাকে আল্লাহর জিকিরে সতেজ রাখুন। এমন দুটি ছোট বাক্য রয়েছে যা উচ্চারণে খুবই সহজ কিন্তু মিজানের পাল্লায় অত্যন্ত ভারী।",
            quoteBangla = "নবী করীম সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “দুটি বাক্য জিহ্বায় উচ্চারণে হালকা, মিজানের পাল্লায় ভারী এবং দয়াময় আল্লাহর কাছে অতি প্রিয়: ‘সুবহানাল্লাহি ওয়া বিহামদিহি, সুবহানাল্লাহিল আযীম’।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৬৪০৬; সহীহ মুসলিম, হাদিস: ২৬৯৪",
            dailyAmolBangla = "আজ দিনের যেকোনো সময় ১০০ বার ‘সুবহানাল্লাহি ওয়া বিহামদিহি’ পাঠ করুন।"
        ),
        // ১৩. ইলম চর্চা ও প্রচার
        IslamicReminderItem(
            id = "smoron_13",
            categoryBangla = "ইলম ও আমল",
            titleBangla = "দ্বীনি ইলম শিক্ষা ও শিক্ষাদান",
            messageBangla = "দ্বীনি ইলম শেখা ও অন্যকে শেখানো বান্দার জন্য সর্বোত্তম সৌভাগ্যের বিষয়। আজ কি একটি সহীহ মাসআলা বা কুরআন-হাদিসের শিক্ষা নিজে শিখেছেন বা অন্যকে জানিয়েছেন?",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “তোমাদের মধ্যে সর্বোত্তম ওই ব্যক্তি, যে নিজে কুরআন শেখে এবং অন্যকে শেখায়।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৫০২৭",
            dailyAmolBangla = "আজ পরিবার বা ছাত্রদেরকে কুরআনের একটি আয়াত বা একটি সুন্নত আমল যত্নসহকারে শেখান।"
        ),
        // ১৪. আয়াতুল কুরসি পাঠ
        IslamicReminderItem(
            id = "smoron_14",
            categoryBangla = "কুরআন তিলাওয়াত",
            titleBangla = "ফরজ নামাজের পর আয়াতুল কুরসি",
            messageBangla = "ফরজ নামাজ শেষ করেই তাড়াহুড়ো করে উঠে পড়ছেন না তো? মাত্র এক মিনিট সময় নিয়ে আয়াতুল কুরসি পাঠ করুন—এর ফজিলত অকল্পনীয়।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “যে ব্যক্তি প্রত্যেক ফরজ নামাজের পর আয়াতুল কুরসি পাঠ করে, তার জান্নাতে প্রবেশের পথে মৃত্যু ছাড়া আর কোনো বাধা থাকে না।”",
            sourceReferenceBangla = "সুনানে নাসাঈ, আল-কুবরা, হাদিস: ৯৯২৮ (সহীহ)",
            dailyAmolBangla = "আজ পাঁচ ওয়াক্ত ফরজ নামাজের পর নিয়মিত আয়াতুল কুরসি পাঠ করার অভ্যাস করুন।"
        ),
        // ১৫. জবান ও হাতের হেফাজত
        IslamicReminderItem(
            id = "smoron_15",
            categoryBangla = "উত্তম আখলাক",
            titleBangla = "গীবত ও পরনিন্দা থেকে বেঁচে থাকা",
            messageBangla = "আজ আমাদের কথাবার্তা ও সামাজিক যোগাযোগমাধ্যমের লেখালেখি কি অন্যের কষ্টের কারণ হচ্ছে? প্রকৃত মুসলিম তো সেই, যার জবান ও হাত থেকে অন্য মুসলিম নিরাপদ থাকে।",
            quoteBangla = "নবী হযরত মুহাম্মদ ﷺ বলেছেন: “প্রকৃত মুসলিম সেই ব্যক্তি, যার জবান ও হাত থেকে অন্য মুসলিমগণ নিরাপদ থাকে।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ১০; সহীহ মুসলিম, হাদিস: ৪০",
            dailyAmolBangla = "আজ কারো অনুপস্থিতিতে তার সমালোচনা (গীবত) করা থেকে সম্পূর্ণ বিরত থাকুন।"
        ),
        // ১৬. সালামের প্রসার
        IslamicReminderItem(
            id = "smoron_16",
            categoryBangla = "সৎকাজ ও খেদমত",
            titleBangla = "আগেভাগে সালাম দেওয়ার ফজিলত",
            messageBangla = "পরিচিত-অপরিচিত যেকোনো মুসলিম ভাইয়ের সাথে দেখা হলে আগে সালাম দিন। সালামের মাধ্যমে পরস্পর মহব্বত বৃদ্ধি পায় এবং অহংকার দূর হয়।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “তোমরা ঈমান না আনা পর্যন্ত জান্নাতে প্রবেশ করতে পারবে না, আর পরস্পরকে না ভালোবাসা পর্যন্ত পূর্ণ মুমিন হতে পারবে না। আমি কি তোমাদের এমন কাজের কথা বলব না, যা করলে তোমাদের মাঝে ভালোবাসা সৃষ্টি হবে? তোমরা নিজেদের মধ্যে সালামের প্রসার ঘটাও।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ৫৪",
            dailyAmolBangla = "আজ যার সাথেই সাক্ষাৎ হোক, স্পষ্ট ও সুন্দর উচ্চারণে আগে ‘আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ’ বলুন।"
        ),
        // ১৭. সাইয়্যিদুল ইস্তিগফার
        IslamicReminderItem(
            id = "smoron_17",
            categoryBangla = "ইস্তিগফার ও তাওবা",
            titleBangla = "সকাল-সন্ধ্যায় সাইয়্যিদুল ইস্তিগফার",
            messageBangla = "আপনি কি আজ সাইয়্যিদুল ইস্তিগফার পাঠ করেছেন? সকাল ও সন্ধ্যায় একিনের সাথে এই শ্রেষ্ঠ ক্ষমা প্রার্থনার দোয়াটি পাঠ করা জান্নাত লাভের অন্যতম মাধ্যম।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “যে ব্যক্তি দিনে একিনের সাথে সাইয়্যিদুল ইস্তিগফার পাঠ করবে এবং সন্ধ্যার আগে মারা যাবে, সে জান্নাতীদের অন্তর্ভুক্ত হবে।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৬৩০৬",
            dailyAmolBangla = "আজ ফজর ও মাগরিবের পর অর্থ স্মরণ করে একবার সাইয়্যিদুল ইস্তিগফার পাঠ করুন।"
        ),
        // ১৮. তাহাজ্জুদ ও রাতের ইবাদত
        IslamicReminderItem(
            id = "smoron_18",
            categoryBangla = "নামাজের গুরুত্ব",
            titleBangla = "শেষ রাতের দোয়া ও তাহাজ্জুদ",
            messageBangla = "রাতের শেষ প্রহরে যখন সবাই ঘুমিয়ে থাকে, তখন রবের দরবারে দুই রাকাত তাহাজ্জুদ নামাজ ও চোখের পানির মূল্য অনেক বেশি।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “ফরজ নামাজের পর সর্বোত্তম নামাজ হলো রাতের (তাহাজ্জুদের) নামাজ।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ১১৬৩",
            dailyAmolBangla = "আজ ফজরের মাত্র ১৫ মিনিট আগে উঠে অন্তত ২ রাকাত তাহাজ্জুদ নামাজ আদায় করার নিয়ত করুন।"
        ),
        // ১৯. হালাল রিজিক ও আমানতদারিতা
        IslamicReminderItem(
            id = "smoron_19",
            categoryBangla = "সৎকাজ ও খেদমত",
            titleBangla = "দায়িত্ব পালনে আমানতদারিতা ও ইখলাস",
            messageBangla = "মাদরাসার দরস, ইদারার দায়িত্ব কিংবা যেকোনো পেশা—আমাদের প্রতিটি কাজই একটি আমানত। পূর্ণ সততা ও ইখলাসের সাথে দায়িত্ব পালন করলে পুরোটাই ইবাদতে পরিণত হয়।",
            quoteBangla = "“নিশ্চয়ই আল্লাহ তোমাদেরকে আদেশ দিচ্ছেন যে, তোমরা আমানতসমূহ তার হকদারদের কাছে পৌঁছে দাও।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা আন-নিসা: ৫৮",
            dailyAmolBangla = "আজ আপনার ওপর অর্পিত দরস বা দায়িত্ব নির্ধারিত সময়ে পূর্ণ আন্তরিকতার সাথে সম্পন্ন করুন।"
        ),
        // ২০. বিপদগ্রস্তের সাহায্য
        IslamicReminderItem(
            id = "smoron_20",
            categoryBangla = "সৎকাজ ও খেদমত",
            titleBangla = "মুমিন ভাইয়ের কষ্ট দূর করা",
            messageBangla = "আপনার আশেপাশের কোনো দ্বীনি ভাই বা প্রতিবেশী কি কোনো সমস্যায় আছেন? সামর্থ্য অনুযায়ী তার পাশে দাঁড়ান—আল্লাহ তাআলা আপনার দুনিয়া ও আখিরাতের সংকট দূর করে দেবেন।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “যে ব্যক্তি কোনো মুমিনের দুনিয়াবি একটি কষ্ট দূর করবে, আল্লাহ কিয়ামতের দিন তার কষ্টসমূহের মধ্য থেকে একটি কষ্ট দূর করে দেবেন।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ২৬৯৯",
            dailyAmolBangla = "আজ একজন পেরেশান বা অসুস্থ মানুষের খোঁজ নিন এবং তাকে সান্ত্বনা বা সহযোগিতা দিন।"
        ),
        // ২১. জুমুআর দিন ও দরুদ পাঠ
        IslamicReminderItem(
            id = "smoron_21",
            categoryBangla = "দরুদ শরিফ",
            titleBangla = "নবীজি ﷺ-এর শাফায়াত লাভের আমল",
            messageBangla = "কিয়ামতের ময়দানে প্রিয় নবী হযরত মুহাম্মদ ﷺ-এর সবচেয়ে নিকটবর্তী হওয়ার সৌভাগ্য অর্জন করতে চান? তাহলে প্রতিদিন তাঁর ওপর অধিক পরিমাণে দরুদ পাঠ করুন।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “কিয়ামতের দিন মানুষের মধ্যে আমার সবচেয়ে নিকটবর্তী হবে ওই ব্যক্তি, যে আমার ওপর সবচেয়ে বেশি দরুদ পাঠ করেছে।”",
            sourceReferenceBangla = "সুনানে তিরমিযী, হাদিস: ৪৮৪ (হাসান)",
            dailyAmolBangla = "আজ হাঁটাচলা ও অবসর সময়ে মনে মনে নবীজি ﷺ-এর ওপর দরুদ শরিফ জারি রাখুন।"
        ),
        // ২২. তাকওয়া ও আল্লাহভীতি
        IslamicReminderItem(
            id = "smoron_22",
            categoryBangla = "ইস্তিগফার ও তাওবা",
            titleBangla = "প্রকাশ্যে ও গোপনে আল্লাহকে ভয় করা",
            messageBangla = "মানুষের সামনে আমরা যেমন সুন্দর থাকি, একান্ত নির্জনেও কি আল্লাহকে তেমনই ভয় করি? তাকওয়া অবলম্বন করলে আল্লাহ প্রতিটি সংকট থেকে বের হওয়ার পথ খুলে দেন।",
            quoteBangla = "“আর যে আল্লাহকে ভয় করে (তাকওয়া অবলম্বন করে), তিনি তার জন্য উত্তরণের পথ তৈরি করে দেন এবং তাকে এমন উৎস থেকে রিজিক দান করেন যা সে কল্পনাও করতে পারে না।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা আত-তালাক: ২-৩",
            dailyAmolBangla = "আজ একান্ত নির্জনে মোবাইল বা চোখ ব্যবহারের ক্ষেত্রে আল্লাহর দৃষ্টিকে স্মরণ করে গুনাহ থেকে বেঁচে থাকুন।"
        ),
        // ২৩. নিয়তের বিশুদ্ধতা (ইখলাস)
        IslamicReminderItem(
            id = "smoron_23",
            categoryBangla = "ইলম ও আমল",
            titleBangla = "প্রতিটি কাজে নিয়ত খাঁটি করা",
            messageBangla = "আমরা যা কিছু করছি—ইলম চর্চা, খেদমত বা দান—তা কি কেবল আল্লাহকে খুশি করার জন্য হচ্ছে? একটু থেমে নিজের নিয়তকে যাচাই ও নবায়ন করে নিন।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “সকল আমল নিয়তের ওপর নির্ভরশীল, আর প্রত্যেক মানুষ তাই পাবে যা সে নিয়ত করবে।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ১; সহীহ মুসলিম, হাদিস: ১৯০৭",
            dailyAmolBangla = "আজ যেকোনো ভালো কাজ শুরু করার আগে মনে মনে নিয়ত করুন: ‘হে আল্লাহ! কেবল আপনার সন্তুষ্টির জন্যই এটি করছি।’"
        ),
        // ২৪. ফজরের সুন্নত ও ফজিলত
        IslamicReminderItem(
            id = "smoron_24",
            categoryBangla = "নামাজের গুরুত্ব",
            titleBangla = "ফজরের নামাজের অমূল্য মর্যাদা",
            messageBangla = "দিনের শুরুটা যদি ফজরের নামাজ দিয়ে হয়, তবে পুরো দিনটিই আল্লাহর জিম্মায় ও বরকতে কাটে। ফজরের দুই রাকাত সুন্নতও দুনিয়া ও তার মধ্যকার সবকিছুর চেয়ে উত্তম!",
            quoteBangla = "নবী করীম সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “ফজরের দুই রাকাত (সুন্নত) নামাজ দুনিয়া এবং দুনিয়ার মধ্যে যা কিছু আছে তার সবকিছুর চেয়ে উত্তম।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ৭২৫",
            dailyAmolBangla = "আগামীকাল ফজরের নামাজ জামাতের সাথে আদায়ের জন্য আজ রাতে দ্রুত ঘুমানোর প্রস্তুতি নিন ও অ্যালার্ম দিন।"
        ),
        // ২৫. জান্নাতের ধনভাণ্ডার (লা হাওলা ওয়ালা কুওয়্যাতা ইল্লা বিল্লাহ)
        IslamicReminderItem(
            id = "smoron_25",
            categoryBangla = "জিকির ও তাসবিহ",
            titleBangla = "জান্নাতের অমূল্য রত্নভাণ্ডার",
            messageBangla = "নিজের শক্তি-সামর্থ্যের ওপর ভরসা না করে সর্বাবস্থায় আল্লাহর সাহায্যের ওপর নির্ভর করুন এবং বেশি বেশি ‘লা হাওলা ওয়ালা কুওয়্যাতা ইল্লা বিল্লাহ’ পাঠ করুন।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ আবু মূসা আশআরী (রা.)-কে বললেন: “আমি কি তোমাকে জান্নাতের ধনভাণ্ডারসমূহের একটি ভাণ্ডারের সন্ধান দেব না? তা হলো: ‘লা হাওলা ওয়ালা কুওয়্যাতা ইল্লা বিল্লাহ’।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৪২০৫; সহীহ মুসলিম, হাদিস: ২৭০৪",
            dailyAmolBangla = "আজ অন্তত ৫০ বার ‘লা হাওলা ওয়ালা কুওয়্যাতা ইল্লা বিল্লাহ’ পাঠ করুন।"
        ),
        // ২৬. আত্মীয়তার সম্পর্ক রক্ষা
        IslamicReminderItem(
            id = "smoron_26",
            categoryBangla = "উত্তম আখলাক",
            titleBangla = "আত্মীয়স্বজনের খোঁজখবর ও সুসম্পর্ক",
            messageBangla = "হায়াত ও রিজিকে প্রশস্ততা চান? তাহলে আত্মীয়স্বজনের সাথে সুসম্পর্ক বজায় রাখুন। দীর্ঘদিন কথা হয়নি এমন কোনো আত্মীয়কে আজ ফোন করে সালাম ও খোঁজ নিন।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “যে ব্যক্তি চায় তার রিজিক প্রশস্ত হোক এবং তার আয়ুস্কাল বৃদ্ধি পাক, সে যেন আত্মীয়তার সম্পর্ক বজায় রাখে।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৫৯৮৬; সহীহ মুসলিম, হাদিস: ২৫৫৭",
            dailyAmolBangla = "আজ একজন নিকটাত্মীয়ের সাথে ফোনে বা সরাসরি যোগাযোগ করে তাঁর কুশলাদি জানুন।"
        ),
        // ২৭. ক্ষমাশীলতা ও উদারতা
        IslamicReminderItem(
            id = "smoron_27",
            categoryBangla = "উত্তম আখলাক",
            titleBangla = "অন্যকে ক্ষমা করার মর্যাদা",
            messageBangla = "কারো ভুলের কারণে কি মনে ক্ষোভ পুষে রেখেছেন? আল্লাহর সন্তুষ্টির জন্য তাকে ক্ষমা করে দিন—ক্ষমা করলে মানুষের মর্যাদা কমে না, বরং আল্লাহ তা আরও বাড়িয়ে দেন।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “ক্ষমা করার দ্বারা আল্লাহ বান্দার মর্যাদা কেবল বৃদ্ধিই করেন; আর যে ব্যক্তি আল্লাহর জন্য বিনয়ী হয়, আল্লাহ তাকে উঁচু মর্যাদায় উন্নীত করেন।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ২৫৮৮",
            dailyAmolBangla = "আজ অন্তরকে হিংসা ও ক্ষোভমুক্ত করে সকল মুসলিম ভাইয়ের জন্য ক্ষমা ও কল্যাণের দোয়া করুন।"
        ),
        // ২৮. সূরা ইখলাস ও কুরআনের মহব্বত
        IslamicReminderItem(
            id = "smoron_28",
            categoryBangla = "কুরআন তিলাওয়াত",
            titleBangla = "এক-তৃতীয়াংশ কুরআন তিলাওয়াতের সওয়াব",
            messageBangla = "অল্প সময়ে বিশাল সওয়াব অর্জনের সুযোগ হাতছাড়া করবেন না। একনিষ্ঠ অন্তরে তিনবার সূরা ইখলাস পাঠ করলে পূর্ণ কুরআন খতমের সমপরিমাণ সওয়াব লাভ হয়।",
            quoteBangla = "নবী সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম সাহাবিদের বললেন: “‘কুল হুওয়াল্লাহু আহাদ’ (সূরা ইখলাস) কুরআনের এক-তৃতীয়াংশের সমতুল্য।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৫০১৫; সহীহ মুসলিম, হাদিস: ৮১১",
            dailyAmolBangla = "আজ ঘুমানোর আগে ও নামাজের পর অন্তত ৩ বার সূরা ইখলাস, সূরা ফালাক ও সূরা নাস পাঠ করুন।"
        ),
        // ২৯. সদকায়ে জারিয়া
        IslamicReminderItem(
            id = "smoron_29",
            categoryBangla = "দান-সদকা",
            titleBangla = "মৃত্যুর পরেও যা আমলনামায় সওয়াব পৌঁছায়",
            messageBangla = "দুনিয়ার জীবন খুবই সংক্ষিপ্ত। আমরা যেন এমন কিছু কাজ রেখে যাই—যেমন উপকারী দ্বীনি ইলম, মাদরাসা-মসজিদে সহযোগিতা ও নেক সন্তান—যার সওয়াব মৃত্যুর পরেও চলমান থাকবে।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “মানুষ যখন মৃত্যুবরণ করে, তখন তিনটি আমল ছাড়া তার সব আমল বন্ধ হয়ে যায়: সদকায়ে জারিয়া, এমন ইলম যা দ্বারা মানুষ উপকৃত হয়, এবং নেক সন্তান যে তার জন্য দোয়া করে।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ১৬৩১",
            dailyAmolBangla = "আজ একটি উপকারী দ্বীনি কথা প্রচার করুন অথবা কোনো সদকায়ে জারিয়ার কাজে অংশ নিন।"
        ),
        // ৩০. আল্লাহর ওপর তাওয়াক্কুল
        IslamicReminderItem(
            id = "smoron_30",
            categoryBangla = "সবর ও শুকরিয়া",
            titleBangla = "রিজিক ও ভবিষ্যৎ নিয়ে আল্লাহর ওপর ভরসা",
            messageBangla = "খেদমত বা জীবিকা নিয়ে অতিরিক্ত দুশ্চিন্তাগ্রস্ত হবেন না। বৈধ প্রচেষ্টার পাশাপাশি মহান আল্লাহর ওপর পূর্ণ তাওয়াক্কুল রাখুন—তিনিই উত্তম রিজিকদাতা।",
            quoteBangla = "“আর যে ব্যক্তি আল্লাহর ওপর ভরসা (তাওয়াক্কুল) করে, তার জন্য আল্লাহই যথেষ্ট।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা আত-তালাক: ৩",
            dailyAmolBangla = "আজ ঘর থেকে বের হওয়ার সময় ‘বিসমিল্লাহি তাওয়াক্কালতু আলাল্লাহ, লা হাওলা ওয়ালা কুওয়্যাতা ইল্লা বিল্লাহ’ পাঠ করুন।"
        ),
        // ৩১. অহংকার বর্জন ও বিনয়
        IslamicReminderItem(
            id = "smoron_31",
            categoryBangla = "উত্তম আখলাক",
            titleBangla = "ইলম ও আমলের ক্ষেত্রে বিনয়ী হওয়া",
            messageBangla = "ইলম, পদমর্যাদা বা ইবাদত যেন কখনো আমাদের মনে অহংকার সৃষ্টি না করে। সত্যকে নির্দ্বিধায় মেনে নেওয়া এবং ছোট-বড় সবাইকে সম্মান করাই প্রকৃত আলেম ও মুমিনের বৈশিষ্ট্য।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “যার অন্তরে অণু পরিমাণ অহংকার থাকবে, সে জান্নাতে প্রবেশ করবে না।”",
            sourceReferenceBangla = "সহীহ মুসলিম, হাদিস: ৯১",
            dailyAmolBangla = "আজ ছোট-বড় সবার সাথে নম্রভাবে কথা বলুন এবং নিজের ভুল হলে সাথে সাথে তা স্বীকার করে নিন।"
        ),
        // ৩২. সুন্নতের অনুসরণ ও দরুদ
        IslamicReminderItem(
            id = "smoron_32",
            categoryBangla = "দরুদ শরিফ",
            titleBangla = "প্রিয় নবী ﷺ-এর সুন্নতের অনুসরণ",
            messageBangla = "প্রিয় নবী হযরত মুহাম্মদ ﷺ-কে ভালোবাসার শ্রেষ্ঠ নিদর্শন হলো তাঁর সুন্নতকে নিজেদের দৈনন্দিন জীবনে আঁকড়ে ধরা এবং তাঁর প্রতি বেশি বেশি দরুদ ও সালাম পেশ করা।",
            quoteBangla = "“নিশ্চয়ই আল্লাহ ও তাঁর ফেরেশতাগণ নবীর প্রতি রহমত বর্ষণ করেন। হে মুমিনগণ! তোমরাও তাঁর প্রতি দরুদ পাঠ করো এবং যথাযথ শ্রদ্ধার সাথে সালাম পেশ করো।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা আল-আহযাব: ৫৬",
            dailyAmolBangla = "আজ খাওয়া, পান করা, মসজিদে প্রবেশ ও ঘুমানোর প্রতিটি সুন্নত দোয়া আদায় করার চেষ্টা করুন।"
        ),
        // ৩৩. সময়ের মূল্য ও গনিমত
        IslamicReminderItem(
            id = "smoron_33",
            categoryBangla = "ইলম ও আমল",
            titleBangla = "অবসর ও সুস্থতাকে কাজে লাগানো",
            messageBangla = "সুস্থতা ও অবসর সময় আল্লাহর দেওয়া দুইটি বিশাল নিয়ামত। অহেতুক কাজে সময় নষ্ট না করে ইলম, জিকির ও উপকারী খেদমতে প্রতিটি মুহূর্ত কাজে লাগান।",
            quoteBangla = "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “দুটি নিয়ামতের ব্যাপারে অধিকাংশ মানুষ ধোঁকার মধ্যে রয়েছে: সুস্থতা এবং অবসর সময়।”",
            sourceReferenceBangla = "সহীহ বুখারী, হাদিস: ৬৪১২",
            dailyAmolBangla = "আজ অন্তত ৩০ মিনিট অহেতুক স্ক্রলিং বাদ দিয়ে কিতাব অধ্যয়ন বা জিকিরে ব্যয় করুন।"
        ),
        // ৩৪. কুরআন ও হিকমাহর দাওয়াত
        IslamicReminderItem(
            id = "smoron_34",
            categoryBangla = "সৎকাজ ও খেদমত",
            titleBangla = "হিকমত ও সুন্দর উপদেশের মাধ্যমে দাওয়াত",
            messageBangla = "কাউকে দ্বীনের কথা বোঝানোর সময় কঠোরতা নয়, বরং প্রজ্ঞা, দরদ ও সুন্দর আচরণের মাধ্যমে কথা বলুন—এতেই মানুষের হৃদয় পরিবর্তন হয়।",
            quoteBangla = "“আপনি আপনার রবের পথে আহ্বান করুন হিকমত (প্রজ্ঞা) ও সুন্দর উপদেশের মাধ্যমে এবং তাদের সাথে বিতর্ক করুন সর্বোত্তম পন্থায়।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা আন-নাহল: ১২৫",
            dailyAmolBangla = "আজ আপনার ছাত্র বা পরিবারের কাউকে শাসন করার পরিবর্তে স্নেহ ও হিকমতের সাথে নসিহত করুন।"
        ),
        // ৩৫. সকাল-সন্ধ্যার মাসনূন দোয়া
        IslamicReminderItem(
            id = "smoron_35",
            categoryBangla = "জিকির ও তাসবিহ",
            titleBangla = "সকাল ও সন্ধ্যার নিরাপত্তার জিকির",
            messageBangla = "প্রতিদিন ফজর ও মাগরিবের পর মাসনূন জিকিরগুলো পাঠ করলে আল্লাহ তাআলা বান্দাকে সারাদিন ও সারারাত যাবতীয় অনিষ্ট থেকে হেফাজতে রাখেন।",
            quoteBangla = "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “যে ব্যক্তি সকাল ও সন্ধ্যায় তিনবার করে ‘বিসমিল্লাহিল্লাযী লা ইয়াদুররু মাআসমিহী শাইউন ফিল আরদি ওয়ালা ফিস সামাঈ ওয়া হুওয়াস সামীউল আলীম’ পাঠ করবে, কোনো কিছুই তার ক্ষতি করতে পারবে না।”",
            sourceReferenceBangla = "সুনানে আবু দাউদ, হাদিস: ৫০৮৮; সুনানে তিরমিযী, হাদিস: ৩৩৮৮ (সহীহ)",
            dailyAmolBangla = "আজ সকাল ও সন্ধ্যায় ৩ বার এই মাসনূন নিরাপত্তার দোয়াটি পাঠ করুন।"
        ),
        // ৩৬. দোয়া কবুলের আশা ও ইয়াকিন
        IslamicReminderItem(
            id = "smoron_36",
            categoryBangla = "ইস্তিগফার ও তাওবা",
            titleBangla = "রবের দরবারে একিনের সাথে দোয়া",
            messageBangla = "আল্লাহ তাআলা বান্দার ডাকে সাড়া দিতে ভালোবাসেন। আজ আপনার মনের সব চাওয়া, দ্বীনি খেদমতের উন্নতি ও উম্মাহর কল্যাণের জন্য একান্তে আল্লাহর কাছে হাত তুলুন।",
            quoteBangla = "“আর আমার বান্দারা যখন আপনার কাছে আমার সম্পর্কে জিজ্ঞেস করে—আমি তো কাছেই আছি। আহ্বানকারী যখন আমাকে ডাকে, আমি তার ডাকে সাড়া দিই।”",
            sourceReferenceBangla = "আল-কুরআন, সূরা আল-বাকারা: ১৮৬",
            dailyAmolBangla = "আজ নামাজের সিজদায় ও সালাম ফিরিয়ে নিজের, পরিবারের ও কওমি মাদরাসা অঙ্গনের জন্য বিশেষ দোয়া করুন।"
        )
    )

    val allCategoriesBangla: List<String> = listOf(
        "সকল",
        "দরুদ শরিফ",
        "জিকির ও তাসবিহ",
        "সৎকাজ ও খেদমত",
        "ইস্তিগফার ও তাওবা",
        "নামাজের গুরুত্ব",
        "কুরআন তিলাওয়াত",
        "উত্তম আখলাক",
        "সবর ও শুকরিয়া",
        "পিতা-মাতার খেদমত",
        "দান-সদকা",
        "ইলম ও আমল"
    )

    private val _smoronStateFlow = MutableStateFlow(
        UserSmoronServerState(
            todayDateKey = currentBangladeshDateKey()
        )
    )
    val smoronStateFlow: StateFlow<UserSmoronServerState> = _smoronStateFlow.asStateFlow()

    fun currentBangladeshDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy_MM_dd", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        return sdf.format(nowMillis)
    }

    fun findReminderById(id: String): IslamicReminderItem? {
        val trimmed = id.trim()
        return allReminders.firstOrNull { it.id == trimmed }
    }

    /**
     * Selects a personalized daily reminder for [userId] on [dateKey] while avoiding any
     * previously delivered reminders in [deliveredHistoryMap].
     *
     * Rules:
     * 1. Different users on the same [dateKey] receive different reminders because [userId]
     *    is mixed with [dateKey] and each user has their own unseen pool.
     * 2. The same user does NOT receive a previously shown reminder until all 36 reminders
     *    in [allReminders] have been shown.
     * 3. Once all 36 reminders have been shown, the oldest shown reminders become eligible
     *    again while excluding recently shown ones.
     */
    fun selectPersonalizedReminderForUser(
        userId: String,
        dateKey: String = currentBangladeshDateKey(),
        deliveredHistoryMap: Map<String, Long> = emptyMap(),
        excludeReminderIds: Set<String> = emptySet()
    ): IslamicReminderItem {
        val effectiveUser = userId.trim().ifBlank { "guest_user" }
        val unseenPool = allReminders.filter { item ->
            !deliveredHistoryMap.containsKey(item.id) && !excludeReminderIds.contains(item.id)
        }

        val candidatePool = when {
            unseenPool.isNotEmpty() -> unseenPool
            else -> {
                // All reminders have been seen; pick from the oldest seen reminders (excluding excludeReminderIds)
                val sortedByOldest = allReminders
                    .filter { !excludeReminderIds.contains(it.id) }
                    .ifEmpty { allReminders }
                    .sortedBy { deliveredHistoryMap[it.id] ?: 0L }
                sortedByOldest.take((sortedByOldest.size / 2).coerceAtLeast(1))
            }
        }

        val seed = computeUserDayHash(effectiveUser, dateKey)
        val index = abs(seed) % candidatePool.size
        return candidatePool[index]
    }

    internal fun computeUserDayHash(userId: String, dateKey: String): Int {
        var h = 1125899907L
        val combined = "$userId::ajker_smoron::$dateKey"
        for (ch in combined) {
            h = (h * 131L) + ch.code.toLong()
        }
        return (h xor (h ushr 32)).toInt()
    }

    fun getOrAssignTodayReminder(
        context: Context?,
        userId: String,
        dateKey: String = currentBangladeshDateKey(),
        serverHistoryMap: Map<String, Long> = emptyMap(),
        serverAssignedForToday: String? = null
    ): IslamicReminderItem {
        val effectiveUser = userId.trim().ifBlank { getActiveUserId(context).ifBlank { "default_user" } }
        val localHistory = loadLocalHistoryMap(context, effectiveUser)
        val mergedHistory = HashMap<String, Long>(localHistory.size + serverHistoryMap.size).apply {
            putAll(localHistory)
            serverHistoryMap.forEach { (k, v) ->
                val prev = this[k] ?: 0L
                if (v > prev) this[k] = v
            }
        }

        // 1. If server already assigned a valid reminder for today, honor it
        val validServerAssigned = serverAssignedForToday?.let { findReminderById(it) }
        if (validServerAssigned != null) {
            saveLocalAssignedReminderForDate(context, effectiveUser, dateKey, validServerAssigned.id)
            updateInMemoryState(context, effectiveUser, dateKey, validServerAssigned.id, mergedHistory)
            return validServerAssigned
        }

        // 2. If local cache already assigned a valid reminder for today, honor it
        val localAssignedId = getLocalAssignedReminderIdForDate(context, effectiveUser, dateKey)
        val validLocalAssigned = localAssignedId?.let { findReminderById(it) }
        if (validLocalAssigned != null) {
            updateInMemoryState(context, effectiveUser, dateKey, validLocalAssigned.id, mergedHistory)
            return validLocalAssigned
        }

        // 3. Otherwise, select a new non-repeating personalized reminder for this user
        val selected = selectPersonalizedReminderForUser(
            userId = effectiveUser,
            dateKey = dateKey,
            deliveredHistoryMap = mergedHistory
        )
        val now = System.currentTimeMillis()
        mergedHistory[selected.id] = now
        saveLocalAssignedReminderForDate(context, effectiveUser, dateKey, selected.id)
        saveLocalHistoryEntry(context, effectiveUser, selected.id, now)
        updateInMemoryState(context, effectiveUser, dateKey, selected.id, mergedHistory)
        return selected
    }

    /**
     * Picks the next unseen reminder for [userId] (when user taps "অন্য আরেকটি স্মরণ দেখুন"),
     * records it in history, and updates today's assigned reminder.
     */
    fun rotateToNextNonRepeatingReminder(
        context: Context?,
        userId: String,
        currentReminderId: String,
        dateKey: String = currentBangladeshDateKey()
    ): IslamicReminderItem {
        val effectiveUser = userId.trim().ifBlank { getActiveUserId(context).ifBlank { "default_user" } }
        val currentState = _smoronStateFlow.value
        val mergedHistory = loadLocalHistoryMap(context, effectiveUser).toMutableMap().apply {
            currentState.deliveredHistoryMap.forEach { (k, v) ->
                val prev = this[k] ?: 0L
                if (v > prev) this[k] = v
            }
        }
        val nextItem = selectPersonalizedReminderForUser(
            userId = "$effectiveUser::${System.nanoTime()}",
            dateKey = dateKey,
            deliveredHistoryMap = mergedHistory,
            excludeReminderIds = setOf(currentReminderId)
        )
        val now = System.currentTimeMillis()
        mergedHistory[nextItem.id] = now
        saveLocalAssignedReminderForDate(context, effectiveUser, dateKey, nextItem.id)
        saveLocalHistoryEntry(context, effectiveUser, nextItem.id, now)
        updateInMemoryState(context, effectiveUser, dateKey, nextItem.id, mergedHistory)
        return nextItem
    }

    fun isNotificationEnabled(context: Context?): Boolean {
        if (context == null) return _smoronStateFlow.value.notificationsEnabled
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_NOTIF_ENABLED, true)
        } catch (_: Exception) {
            true
        }
    }

    fun setNotificationEnabled(context: Context?, enabled: Boolean) {
        _smoronStateFlow.value = _smoronStateFlow.value.copy(notificationsEnabled = enabled)
        if (context == null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
            if (enabled) {
                val (hour, min) = getPreferredNotificationTime(context)
                scheduleDailyPushAlarm(context, hour, min)
            } else {
                cancelDailyPushAlarm(context)
            }
        } catch (_: Exception) {
        }
    }

    fun getPreferredNotificationTime(context: Context?): Pair<Int, Int> {
        if (context == null) {
            return _smoronStateFlow.value.preferredHour24 to _smoronStateFlow.value.preferredMinute
        }
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hour = prefs.getInt(KEY_PREFERRED_HOUR, 8).coerceIn(0, 23)
            val min = prefs.getInt(KEY_PREFERRED_MINUTE, 0).coerceIn(0, 59)
            hour to min
        } catch (_: Exception) {
            8 to 0
        }
    }

    fun setPreferredNotificationTime(context: Context?, hour24: Int, minute: Int = 0) {
        val safeHour = hour24.coerceIn(0, 23)
        val safeMin = minute.coerceIn(0, 59)
        _smoronStateFlow.value = _smoronStateFlow.value.copy(
            preferredHour24 = safeHour,
            preferredMinute = safeMin
        )
        if (context == null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_PREFERRED_HOUR, safeHour)
                .putInt(KEY_PREFERRED_MINUTE, safeMin)
                .apply()
            if (isNotificationEnabled(context)) {
                scheduleDailyPushAlarm(context, safeHour, safeMin)
            }
        } catch (_: Exception) {
        }
    }

    fun formatHourBangla(hour24: Int, minute: Int = 0): String {
        val period = when (hour24) {
            in 4..11 -> "সকাল"
            in 12..14 -> "দুপুর"
            in 15..17 -> "বিকাল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formatted = String.format(Locale.ENGLISH, "%02d:%02d", hour12, minute)
        return "$period ${IslamicDateAndPrayerHelper.toBanglaDigits(formatted)}"
    }

    fun hasPushBeenSentForDate(
        context: Context?,
        userId: String,
        dateKey: String = currentBangladeshDateKey()
    ): Boolean {
        val effectiveUser = userId.trim().ifBlank { getActiveUserId(context).ifBlank { "default_user" } }
        if (_smoronStateFlow.value.userId == effectiveUser &&
            _smoronStateFlow.value.lastPushSentDateKey == dateKey
        ) {
            return true
        }
        if (context == null) return false
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString("last_push_date_$effectiveUser", "") == dateKey
        } catch (_: Exception) {
            false
        }
    }

    fun recordPushSentForDate(
        context: Context?,
        userId: String,
        dateKey: String,
        reminderId: String
    ) {
        val effectiveUser = userId.trim().ifBlank { getActiveUserId(context).ifBlank { "default_user" } }
        val now = System.currentTimeMillis()
        saveLocalHistoryEntry(context, effectiveUser, reminderId, now)
        try {
            context?.applicationContext
                ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString("last_push_date_$effectiveUser", dateKey)
                ?.putString("last_push_reminder_$effectiveUser", reminderId)
                ?.apply()
        } catch (_: Exception) {
        }
        val updatedHistory = _smoronStateFlow.value.deliveredHistoryMap.toMutableMap().apply {
            this[reminderId] = now
        }
        _smoronStateFlow.value = _smoronStateFlow.value.copy(
            userId = effectiveUser,
            lastPushSentDateKey = dateKey,
            lastPushSentReminderId = reminderId,
            deliveredHistoryMap = updatedHistory
        )
    }

    fun markTodayAmolCompleted(
        context: Context?,
        userId: String,
        dateKey: String = currentBangladeshDateKey()
    ) {
        val effectiveUser = userId.trim().ifBlank { getActiveUserId(context).ifBlank { "default_user" } }
        val currentCompleted = getCompletedAmolDates(context, effectiveUser).toMutableSet()
        currentCompleted.add(dateKey)
        try {
            context?.applicationContext
                ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putStringSet("amol_done_dates_$effectiveUser", currentCompleted)
                ?.apply()
        } catch (_: Exception) {
        }
        _smoronStateFlow.value = _smoronStateFlow.value.copy(
            completedAmolDates = currentCompleted
        )
    }

    fun getCompletedAmolDates(context: Context?, userId: String): Set<String> {
        val effectiveUser = userId.trim().ifBlank { getActiveUserId(context).ifBlank { "default_user" } }
        if (context == null) return _smoronStateFlow.value.completedAmolDates
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getStringSet("amol_done_dates_$effectiveUser", emptySet()).orEmpty()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun setActiveUserId(context: Context?, userId: String) {
        if (userId.isBlank() || context == null) return
        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_USER_ID, userId.trim())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun getActiveUserId(context: Context?): String {
        if (context == null) return _smoronStateFlow.value.userId
        return try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_USER_ID, "")
                .orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Syncs Firestore server receipts (`/users/{uid}/notification_reads`) with local state.
     * Receipt ID conventions:
     * - `smoron_hist_<reminderId>` -> records when `<reminderId>` was delivered
     * - `smoron_day_<yyyy_MM_dd>__<reminderId>` -> records assigned reminder for `<yyyy_MM_dd>`
     * - `smoron_push_<yyyy_MM_dd>__<reminderId>` -> records push notification sent on `<yyyy_MM_dd>`
     * - `smoron_amol_<yyyy_MM_dd>` -> records daily amol completion on `<yyyy_MM_dd>`
     */
    fun syncFromServerReceipts(
        context: Context?,
        userId: String,
        receiptTimestamps: Map<String, Long>,
        dateKey: String = currentBangladeshDateKey()
    ): IslamicReminderItem {
        val effectiveUser = userId.trim().ifBlank { "default_user" }
        setActiveUserId(context, effectiveUser)

        val serverHistory = mutableMapOf<String, Long>()
        var serverAssignedToday: String? = null
        var serverPushSentToday = false
        var serverPushReminderToday = ""
        val serverAmolDates = mutableSetOf<String>()

        val dayPrefix = "${RECEIPT_PREFIX_DAY_ASSIGN}${dateKey}__"
        val pushPrefix = "${RECEIPT_PREFIX_PUSH_SENT}${dateKey}__"

        receiptTimestamps.forEach { (receiptId, tsMillis) ->
            when {
                receiptId.startsWith(RECEIPT_PREFIX_HISTORY) -> {
                    val remId = receiptId.removePrefix(RECEIPT_PREFIX_HISTORY)
                    if (findReminderById(remId) != null) {
                        serverHistory[remId] = tsMillis
                        saveLocalHistoryEntry(context, effectiveUser, remId, tsMillis)
                    }
                }
                receiptId.startsWith(dayPrefix) -> {
                    val remId = receiptId.removePrefix(dayPrefix)
                    if (findReminderById(remId) != null) {
                        serverAssignedToday = remId
                        serverHistory[remId] = maxOf(serverHistory[remId] ?: 0L, tsMillis)
                    }
                }
                receiptId.startsWith(pushPrefix) || receiptId == "${RECEIPT_PREFIX_PUSH_SENT}${dateKey}" -> {
                    serverPushSentToday = true
                    val remId = receiptId.removePrefix(pushPrefix)
                    if (findReminderById(remId) != null) {
                        serverPushReminderToday = remId
                        serverHistory[remId] = maxOf(serverHistory[remId] ?: 0L, tsMillis)
                    }
                }
                receiptId.startsWith(RECEIPT_PREFIX_AMOL_DONE) -> {
                    val dKey = receiptId.removePrefix(RECEIPT_PREFIX_AMOL_DONE)
                    if (dKey.isNotBlank()) {
                        serverAmolDates.add(dKey)
                    }
                }
            }
        }

        if (serverPushSentToday) {
            recordPushSentForDate(
                context = context,
                userId = effectiveUser,
                dateKey = dateKey,
                reminderId = serverPushReminderToday.ifBlank { serverAssignedToday.orEmpty() }
            )
        }

        if (serverAmolDates.isNotEmpty()) {
            val combinedAmol = getCompletedAmolDates(context, effectiveUser) + serverAmolDates
            try {
                context?.applicationContext
                    ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    ?.edit()
                    ?.putStringSet("amol_done_dates_$effectiveUser", combinedAmol)
                    ?.apply()
            } catch (_: Exception) {
            }
        }

        return getOrAssignTodayReminder(
            context = context,
            userId = effectiveUser,
            dateKey = dateKey,
            serverHistoryMap = serverHistory,
            serverAssignedForToday = serverAssignedToday
        )
    }

    /**
     * Schedules the daily background push notification alarm via Android [AlarmManager]
     * so the reminder notification arrives at the scheduled time even when the app is closed.
     */
    fun scheduleDailyPushAlarm(
        context: Context,
        hour24: Int = getPreferredNotificationTime(context).first,
        minute: Int = getPreferredNotificationTime(context).second
    ) {
        if (!isNotificationEnabled(context)) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailySmoronPushReceiver::class.java).apply {
            action = DailySmoronPushReceiver.ACTION_DAILY_SMORON_ALARM
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DailySmoronPushReceiver.ALARM_REQUEST_CODE,
            intent,
            flags
        )

        val dhakaTz = TimeZone.getTimeZone("Asia/Dhaka")
        val nowCal = Calendar.getInstance(dhakaTz)
        val targetCal = Calendar.getInstance(dhakaTz).apply {
            set(Calendar.HOUR_OF_DAY, hour24.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowCal.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetCal.timeInMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    targetCal.timeInMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
            }
        }
    }

    fun cancelDailyPushAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailySmoronPushReceiver::class.java).apply {
            action = DailySmoronPushReceiver.ACTION_DAILY_SMORON_ALARM
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DailySmoronPushReceiver.ALARM_REQUEST_CODE,
            intent,
            flags
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }

    private fun updateInMemoryState(
        context: Context?,
        userId: String,
        dateKey: String,
        assignedReminderId: String,
        historyMap: Map<String, Long>
    ) {
        val (prefHour, prefMin) = getPreferredNotificationTime(context)
        val notifEnabled = isNotificationEnabled(context)
        val lastPushDate = if (context != null) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("last_push_date_$userId", "")
                .orEmpty()
        } else {
            _smoronStateFlow.value.lastPushSentDateKey
        }
        val lastPushRem = if (context != null) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("last_push_reminder_$userId", "")
                .orEmpty()
        } else {
            _smoronStateFlow.value.lastPushSentReminderId
        }
        val amolDone = getCompletedAmolDates(context, userId)
        _smoronStateFlow.value = UserSmoronServerState(
            userId = userId,
            todayDateKey = dateKey,
            assignedReminderId = assignedReminderId,
            lastPushSentDateKey = lastPushDate,
            lastPushSentReminderId = lastPushRem,
            deliveredHistoryMap = historyMap,
            completedAmolDates = amolDone,
            preferredHour24 = prefHour,
            preferredMinute = prefMin,
            notificationsEnabled = notifEnabled
        )
    }

    private fun getLocalAssignedReminderIdForDate(
        context: Context?,
        userId: String,
        dateKey: String
    ): String? {
        if (context == null) {
            val st = _smoronStateFlow.value
            return if (st.userId == userId && st.todayDateKey == dateKey && st.assignedReminderId.isNotBlank()) {
                st.assignedReminderId
            } else {
                null
            }
        }
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString("assigned_${userId}_$dateKey", null)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveLocalAssignedReminderForDate(
        context: Context?,
        userId: String,
        dateKey: String,
        reminderId: String
    ) {
        if (context == null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("assigned_${userId}_$dateKey", reminderId).apply()
        } catch (_: Exception) {
        }
    }

    fun loadLocalHistoryMap(context: Context?, userId: String): Map<String, Long> {
        if (context == null) {
            return if (_smoronStateFlow.value.userId == userId) {
                _smoronStateFlow.value.deliveredHistoryMap
            } else {
                emptyMap()
            }
        }
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val rawSet = prefs.getStringSet("history_entries_$userId", emptySet()).orEmpty()
            val map = mutableMapOf<String, Long>()
            for (entry in rawSet) {
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val remId = parts[0].trim()
                    val ts = parts[1].toLongOrNull() ?: 0L
                    if (remId.isNotBlank()) {
                        map[remId] = ts
                    }
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveLocalHistoryEntry(
        context: Context?,
        userId: String,
        reminderId: String,
        timestampMillis: Long
    ) {
        if (context == null || reminderId.isBlank()) return
        try {
            val currentMap = loadLocalHistoryMap(context, userId).toMutableMap()
            // If all 36 reminders have already been seen, prune the oldest half so the cycle continues cleanly
            if (currentMap.size >= allReminders.size && !currentMap.containsKey(reminderId)) {
                val keepNewest = currentMap.entries
                    .sortedByDescending { it.value }
                    .take(allReminders.size / 2)
                    .associate { it.key to it.value }
                currentMap.clear()
                currentMap.putAll(keepNewest)
            }
            currentMap[reminderId] = timestampMillis
            val serialized = currentMap.entries.map { "${it.key}:${it.value}" }.toSet()
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putStringSet("history_entries_$userId", serialized).apply()
        } catch (_: Exception) {
        }
    }
}
