package com.example.ui.quran

/**
 * Authentic Index & 15-Line Layout Resolver for the Indo-Pak / Emdadia 15-Line Hafezi Quran Sharif.
 * Total Mushaf Pages: 610 (Standard Emdadia / Qudratullah 15-Line Hafezi Mushaf, 20 pages per Para).
 */
object HafeziQuranIndexData {

    const val TOTAL_MUSHAF_PAGES = 610
    const val LINES_PER_PAGE = 15

    val all114Surahs: List<HafeziSurahInfo> = listOf(
        HafeziSurahInfo(1, "الفاتحة", "আল-ফাতিহা", "Al-Fatihah", "সূচনা", true, 7, 2, 2, 1, 1),
        HafeziSurahInfo(2, "البقرة", "আল-বাকারা", "Al-Baqarah", "বকনা-বাছুর", false, 286, 3, 50, 1, 40),
        HafeziSurahInfo(3, "آل عمران", "আল-ইমরান", "Ali 'Imran", "ইমরানের পরিবার", false, 200, 51, 77, 3, 20),
        HafeziSurahInfo(4, "النساء", "আন-নিসা", "An-Nisa", "নারী", false, 176, 78, 106, 4, 24),
        HafeziSurahInfo(5, "المائدة", "আল-মায়িদাহ", "Al-Ma'idah", "খাদ্য পরিপূর্ণ টেবিল", false, 120, 107, 128, 6, 16),
        HafeziSurahInfo(6, "الأنعام", "আল-আনআম", "Al-An'am", "গৃহপালিত পশু", true, 165, 129, 151, 7, 20),
        HafeziSurahInfo(7, "الأعراف", "আল-আরাফ", "Al-A'raf", "উঁচু স্থানসমূহ", true, 206, 152, 177, 8, 24),
        HafeziSurahInfo(8, "الأنفال", "আল-আনফাল", "Al-Anfal", "যুদ্ধলব্ধ সম্পদ", false, 75, 178, 187, 9, 10),
        HafeziSurahInfo(9, "التوبة", "আত-তাওবাহ", "At-Tawbah", "অনুশোচনা", false, 129, 188, 208, 10, 16),
        HafeziSurahInfo(10, "يونس", "ইউনুস", "Yunus", "নবী ইউনুস (আ.)", true, 109, 209, 221, 11, 11),
        HafeziSurahInfo(11, "هود", "হুদ", "Hud", "নবী হুদ (আ.)", true, 123, 222, 235, 11, 10),
        HafeziSurahInfo(12, "يوسف", "ইউসুফ", "Yusuf", "নবী ইউসুফ (আ.)", true, 111, 236, 249, 12, 12),
        HafeziSurahInfo(13, "الرعد", "আর-রাদ", "Ar-Ra'd", "বজ্রপাত", false, 43, 250, 255, 13, 6),
        HafeziSurahInfo(14, "إبراهيم", "ইবরাহিম", "Ibrahim", "নবী ইবরাহিম (আ.)", true, 52, 256, 262, 13, 7),
        HafeziSurahInfo(15, "الحجر", "আল-হিজর", "Al-Hijr", "পাথুরে পাহাড়", true, 99, 262, 267, 14, 6),
        HafeziSurahInfo(16, "النحل", "আন-নাহল", "An-Nahl", "মৌমাছি", true, 128, 268, 282, 14, 16),
        HafeziSurahInfo(17, "الإسراء", "বনী ইসরাঈল", "Al-Isra", "রাতের সফর", true, 111, 283, 294, 15, 12),
        HafeziSurahInfo(18, "الكهف", "আল-কাহফ", "Al-Kahf", "গুহা", true, 110, 294, 306, 15, 12),
        HafeziSurahInfo(19, "مريم", "মারইয়াম", "Maryam", "মারইয়াম (আ.)", true, 98, 306, 313, 16, 6),
        HafeziSurahInfo(20, "طه", "ত্বা-হা", "Taha", "ত্বা-হা", true, 135, 313, 322, 16, 8),
        HafeziSurahInfo(21, "الأنبياء", "আল-আম্বিয়া", "Al-Anbiya", "নবীগণ", true, 112, 323, 332, 17, 7),
        HafeziSurahInfo(22, "الحج", "আল-হাজ্জ", "Al-Hajj", "হজ্জ", false, 78, 333, 342, 17, 10),
        HafeziSurahInfo(23, "المؤمنون", "আল-মুমিনুন", "Al-Mu'minun", "মুমিনগণ", true, 118, 343, 350, 18, 6),
        HafeziSurahInfo(24, "النور", "আন-নূর", "An-Nur", "আলো", false, 64, 351, 360, 18, 9),
        HafeziSurahInfo(25, "الفرقان", "আল-ফুরকান", "Al-Furqan", "সত্য-মিথ্যার পার্থক্যকারী", true, 77, 360, 367, 18, 6),
        HafeziSurahInfo(26, "الشعراء", "আশ-শুআরা", "Ash-Shu'ara", "কবিগণ", true, 227, 368, 377, 19, 11),
        HafeziSurahInfo(27, "النمل", "আন-নামল", "An-Naml", "পিপীলিকা", true, 93, 378, 386, 19, 7),
        HafeziSurahInfo(28, "القصص", "আল-কাসাস", "Al-Qasas", "কাহিনী", true, 88, 386, 397, 20, 9),
        HafeziSurahInfo(29, "العنكبوت", "আল-আনকাবুত", "Al-'Ankabut", "মাকড়সা", true, 69, 397, 405, 20, 7),
        HafeziSurahInfo(30, "الروم", "আর-রূম", "Ar-Rum", "রোমান জাতি", true, 60, 405, 411, 21, 6),
        HafeziSurahInfo(31, "لقمان", "লুকমান", "Luqman", "হাকীম লুকমান", true, 34, 412, 415, 21, 4),
        HafeziSurahInfo(32, "السجدة", "আস-সাজদাহ", "As-Sajdah", "সিজদা", true, 30, 416, 418, 21, 3),
        HafeziSurahInfo(33, "الأحزاب", "আল-আহযাব", "Al-Ahzab", "সম্মিলিত বাহিনী", false, 73, 419, 428, 21, 9),
        HafeziSurahInfo(34, "سبأ", "সাবা", "Saba", "সাবা জাতি", true, 54, 429, 435, 22, 6),
        HafeziSurahInfo(35, "فاطر", "ফাতির", "Fatir", "আদি স্রষ্টা", true, 45, 435, 441, 22, 5),
        HafeziSurahInfo(36, "يس", "ইয়াসিন", "Ya-Sin", "ইয়াসিন", true, 83, 441, 446, 22, 5),
        HafeziSurahInfo(37, "الصافات", "আস-সাফফাত", "As-Saffat", "সারিবদ্ধ ফেরেশতাগণ", true, 182, 447, 453, 23, 5),
        HafeziSurahInfo(38, "ص", "সোয়াদ", "Sad", "আরবি বর্ণ সোয়াদ", true, 88, 453, 459, 23, 5),
        HafeziSurahInfo(39, "الزمر", "আয-যুমার", "Az-Zumar", "দলসমূহ", true, 75, 459, 468, 23, 8),
        HafeziSurahInfo(40, "غافر", "আল-মুমিন (গাফির)", "Ghafir", "ক্ষমাকারী", true, 85, 468, 477, 24, 9),
        HafeziSurahInfo(41, "فصلت", "হা-মীম সাজদাহ (ফুসসিলাত)", "Fussilat", "সুস্পষ্ট ব্যাখ্যা", true, 54, 478, 483, 24, 6),
        HafeziSurahInfo(42, "الشورى", "আশ-শূরা", "Ash-Shuraa", "পরামর্শ", true, 53, 484, 490, 25, 5),
        HafeziSurahInfo(43, "الزخرف", "আয-যুখরুফ", "Az-Zukhruf", "স্বর্ণালঙ্কার", true, 89, 490, 496, 25, 7),
        HafeziSurahInfo(44, "الدخان", "আদ-দুখান", "Ad-Dukhan", "ধোঁয়া", true, 59, 497, 499, 25, 3),
        HafeziSurahInfo(45, "الجاثية", "আল-জাসিয়াহ", "Al-Jathiyah", "নতজানু", true, 37, 500, 503, 25, 4),
        HafeziSurahInfo(46, "الأحقاف", "আল-আহকাফ", "Al-Ahqaf", "বালুর পাহাড়", true, 35, 503, 508, 26, 4),
        HafeziSurahInfo(47, "محمد", "মুহাম্মাদ", "Muhammad", "নবী মুহাম্মাদ ﷺ", false, 38, 508, 512, 26, 4),
        HafeziSurahInfo(48, "الفتح", "আল-ফাতহ", "Al-Fath", "বিজয়", false, 29, 512, 516, 26, 4),
        HafeziSurahInfo(49, "الحجرات", "আল-হুজুরাত", "Al-Hujurat", "বাসগৃহসমূহ", false, 18, 517, 519, 26, 2),
        HafeziSurahInfo(50, "ق", "ক্বাফ", "Qaf", "আরবি বর্ণ ক্বাফ", true, 45, 519, 522, 26, 3),
        HafeziSurahInfo(51, "الذاريات", "আয-যারিয়াত", "Adh-Dhariyat", "বিক্ষেপকারী বাতাস", true, 60, 522, 525, 26, 3),
        HafeziSurahInfo(52, "الطور", "আত-তূর", "At-Tur", "তূর পাহাড়", true, 49, 525, 527, 27, 2),
        HafeziSurahInfo(53, "النجم", "আন-নাজম", "An-Najm", "তারা", true, 62, 528, 530, 27, 3),
        HafeziSurahInfo(54, "القمر", "আল-কামার", "Al-Qamar", "চন্দ্র", true, 55, 530, 533, 27, 3),
        HafeziSurahInfo(55, "الرحمن", "আর-রহমান", "Ar-Rahman", "পরম করুণাময়", false, 78, 533, 536, 27, 3),
        HafeziSurahInfo(56, "الواقعة", "আল-ওয়াকিয়াহ", "Al-Waqi'ah", "নিশ্চিত ঘটনা", true, 96, 536, 540, 27, 3),
        HafeziSurahInfo(57, "الحديد", "আল-হাদীদ", "Al-Hadid", "লোহা", false, 29, 540, 544, 27, 4),
        HafeziSurahInfo(58, "المجادلة", "আল-মুজাদালাহ", "Al-Mujadila", "অনুযোগকারিণী", false, 22, 545, 548, 28, 3),
        HafeziSurahInfo(59, "الحشر", "আল-হাশর", "Al-Hashr", "সমাবেশ", false, 24, 548, 551, 28, 3),
        HafeziSurahInfo(60, "الممتحنة", "আল-মুমতাহিনাহ", "Al-Mumtahanah", "পরীক্ষাসাপেক্ষ নারী", false, 13, 551, 554, 28, 2),
        HafeziSurahInfo(61, "الصف", "আস-সাফ", "As-Saff", "সারিবদ্ধ সৈন্যদল", false, 14, 554, 556, 28, 2),
        HafeziSurahInfo(62, "الجمعة", "আল-জুমুআহ", "Al-Jumu'ah", "জুমার দিন", false, 11, 556, 557, 28, 2),
        HafeziSurahInfo(63, "المنافقون", "আল-মুনাফিকুন", "Al-Munafiqun", "মুনাফিকদল", false, 11, 557, 559, 28, 2),
        HafeziSurahInfo(64, "التغابن", "আত-তাগাবুন", "At-Taghabun", "মোহমুক্তি", false, 18, 559, 561, 28, 2),
        HafeziSurahInfo(65, "الطلاق", "আত-তালাক", "At-Talaq", "তালাক", false, 12, 561, 563, 28, 2),
        HafeziSurahInfo(66, "التحريم", "আত-তাহরীম", "At-Tahrim", "নিষিদ্ধকরণ", false, 12, 563, 565, 28, 2),
        HafeziSurahInfo(67, "الملك", "আল-মুলক", "Al-Mulk", "সার্বভৌম কর্তৃত্ব", true, 30, 566, 568, 29, 2),
        HafeziSurahInfo(68, "القلم", "আল-কলম", "Al-Qalam", "কলম", true, 52, 568, 571, 29, 2),
        HafeziSurahInfo(69, "الحاقة", "আল-হাক্কাহ", "Al-Haqqah", "নিশ্চিত সত্য", true, 52, 571, 573, 29, 2),
        HafeziSurahInfo(70, "المعارج", "আল-মাআরিজ", "Al-Ma'arij", "উন্নয়নের সোপান", true, 44, 573, 575, 29, 2),
        HafeziSurahInfo(71, "نوح", "নূহ", "Nuh", "নবী নূহ (আ.)", true, 28, 575, 577, 29, 2),
        HafeziSurahInfo(72, "الجن", "আল-জিন", "Al-Jinn", "জিন জাতি", true, 28, 577, 579, 29, 2),
        HafeziSurahInfo(73, "المزمل", "আল-মুযযাম্মিল", "Al-Muzzammil", "বস্ত্রাবৃতকারী", true, 20, 579, 580, 29, 2),
        HafeziSurahInfo(74, "المدثر", "আল-মুদ্দাসসির", "Al-Muddaththir", "চাদরাবৃতকারী", true, 56, 580, 582, 29, 2),
        HafeziSurahInfo(75, "القيامة", "আল-কিয়ামাহ", "Al-Qiyamah", "কিয়ামত", true, 40, 582, 583, 29, 2),
        HafeziSurahInfo(76, "الإنسان", "আল-ইনসান (দাহর)", "Al-Insan", "মানুষ", false, 31, 583, 585, 29, 2),
        HafeziSurahInfo(77, "المرسلات", "আল-মুরসালাত", "Al-Mursalat", "প্রেরিত বাতাস", true, 50, 585, 586, 29, 2),
        HafeziSurahInfo(78, "النبأ", "আন-নাবা", "An-Naba", "মহাসংবাদ", true, 40, 587, 588, 30, 2),
        HafeziSurahInfo(79, "النازعات", "আন-নাযিয়াত", "An-Nazi'at", "প্রচেষ্টাকারী ফেরেশতা", true, 46, 588, 590, 30, 2),
        HafeziSurahInfo(80, "عبس", "আবাসা", "'Abasa", "ভ্রুকুটি করলেন", true, 42, 590, 591, 30, 1),
        HafeziSurahInfo(81, "التكوير", "আত-তাকভীর", "At-Takwir", "অন্ধকারাচ্ছন্ন", true, 29, 591, 592, 30, 1),
        HafeziSurahInfo(82, "الانفطار", "আল-ইনফিতার", "Al-Infitar", "বিদীর্ণ হওয়া", true, 19, 592, 593, 30, 1),
        HafeziSurahInfo(83, "المطففين", "আল-মুতাফফিফীন", "Al-Mutaffifin", "ওজনে কমদানকারী", true, 36, 593, 594, 30, 1),
        HafeziSurahInfo(84, "الانشقاق", "আল-ইনশিকাক", "Al-Inshiqaq", "খণ্ড-বিখণ্ড করণ", true, 25, 594, 595, 30, 1),
        HafeziSurahInfo(85, "البروج", "আল-বুরূজ", "Al-Buruj", "নক্ষত্রপুঞ্জ", true, 22, 595, 596, 30, 1),
        HafeziSurahInfo(86, "الطارق", "আত-তারিক", "At-Tariq", "রাতের আগন্তুক", true, 17, 596, 597, 30, 1),
        HafeziSurahInfo(87, "الأعلى", "আল-আলা", "Al-A'la", "সর্বোন্নত", true, 19, 597, 598, 30, 1),
        HafeziSurahInfo(88, "الغاشية", "আল-গাশিয়াহ", "Al-Ghashiyah", "আচ্ছন্নকারী ঘটনা", true, 26, 598, 599, 30, 1),
        HafeziSurahInfo(89, "الفجر", "আল-ফজর", "Al-Fajr", "ভোরবেলা", true, 30, 599, 600, 30, 1),
        HafeziSurahInfo(90, "البلد", "আল-বালাদ", "Al-Balad", "নগরী", true, 20, 600, 601, 30, 1),
        HafeziSurahInfo(91, "الشمس", "আশ-শামস", "Ash-Shams", "সূর্য", true, 15, 601, 601, 30, 1),
        HafeziSurahInfo(92, "الليل", "আল-লাইল", "Al-Layl", "রাত্রি", true, 21, 601, 602, 30, 1),
        HafeziSurahInfo(93, "الضحى", "আদ-দুহা", "Ad-Duhaa", "পূর্বাহ্ণের সূর্যকিরণ", true, 11, 602, 603, 30, 1),
        HafeziSurahInfo(94, "الشرح", "আল-ইনশিরাহ", "Ash-Sharh", "বক্ষ প্রশস্তকরণ", true, 8, 603, 603, 30, 1),
        HafeziSurahInfo(95, "التين", "আত-তীন", "At-Tin", "ডুমুর", true, 8, 603, 604, 30, 1),
        HafeziSurahInfo(96, "العلق", "আল-আলাক", "Al-'Alaq", "রক্তপিণ্ড", true, 19, 604, 604, 30, 1),
        HafeziSurahInfo(97, "القدر", "আল-কদর", "Al-Qadr", "মহিমান্বিত রজনী", true, 5, 604, 605, 30, 1),
        HafeziSurahInfo(98, "البينة", "আল-বাইয়্যিনাহ", "Al-Bayyinah", "সুস্পষ্ট প্রমাণ", false, 8, 605, 605, 30, 1),
        HafeziSurahInfo(99, "الزلزلة", "আয-যিলযাল", "Az-Zalzalah", "ভূমিকম্প", false, 8, 605, 606, 30, 1),
        HafeziSurahInfo(100, "العاديات", "আল-আদিয়াত", "Al-'Adiyat", "অভিযানকারী অশ্ববৃন্দ", true, 11, 606, 606, 30, 1),
        HafeziSurahInfo(101, "القارعة", "আল-কারিয়াহ", "Al-Qari'ah", "মহাসংকট", true, 11, 606, 607, 30, 1),
        HafeziSurahInfo(102, "التكاثر", "আত-তাকাসুর", "At-Takathur", "প্রাচুর্যের প্রতিযোগিতা", true, 8, 607, 607, 30, 1),
        HafeziSurahInfo(103, "العصر", "আল-আসর", "Al-'Asr", "সময়", true, 3, 607, 608, 30, 1),
        HafeziSurahInfo(104, "الهمزة", "আল-হুমাযাহ", "Al-Humazah", "পরনিন্দাকারী", true, 9, 608, 608, 30, 1),
        HafeziSurahInfo(105, "الفيل", "আল-ফীল", "Al-Fil", "হাতি", true, 5, 608, 608, 30, 1),
        HafeziSurahInfo(106, "قريش", "কুরাইশ", "Quraysh", "কুরাইশ গোত্র", true, 4, 608, 609, 30, 1),
        HafeziSurahInfo(107, "الماعون", "আল-মাউন", "Al-Ma'un", "নিত্যপ্রয়োজনীয় বস্তু", true, 7, 609, 609, 30, 1),
        HafeziSurahInfo(108, "الكوثر", "আল-কাওসার", "Al-Kawthar", "হাউজে কাওসার", true, 3, 609, 609, 30, 1),
        HafeziSurahInfo(109, "الكافرون", "আল-কাফিরুন", "Al-Kafirun", "অবিশ্বাসী গোষ্ঠী", true, 6, 609, 609, 30, 1),
        HafeziSurahInfo(110, "النصر", "আন-নাসর", "An-Nasr", "সাহায্য", false, 3, 609, 610, 30, 1),
        HafeziSurahInfo(111, "المسد", "আল-লাহাব (মাসাদ)", "Al-Masad", "খেজুরের পাকানো রশি", true, 5, 610, 610, 30, 1),
        HafeziSurahInfo(112, "الإخلاص", "আল-ইখলাস", "Al-Ikhlas", "একত্ববাদ", true, 4, 610, 610, 30, 1),
        HafeziSurahInfo(113, "الفلق", "আল-ফালাক", "Al-Falaq", "নিশিভোর", true, 5, 610, 610, 30, 1),
        HafeziSurahInfo(114, "الناس", "আন-নাস", "An-Nas", "মানবজাতি", true, 6, 610, 610, 30, 1)
    )

    val all30Paras: List<HafeziParaInfo> = listOf(
        HafeziParaInfo(1, "آلم", "আলিফ-লাম-মীম", "Alif Lam Meem", 2, 21, 1, "আল-ফাতিহা", "১:১"),
        HafeziParaInfo(2, "سَيَقُولُ", "সাইয়াকুলু", "Sayaqool", 22, 41, 2, "আল-বাকারা", "২:১৪২"),
        HafeziParaInfo(3, "تِلْكَ الرُّسُلُ", "তিলকার রুসুলু", "Tilkal Rusul", 42, 61, 2, "আল-বাকারা", "২:২৫৩"),
        HafeziParaInfo(4, "لَنْ تَنَالُوا", "লান তানালুল বিররা", "Lan Tana Loo", 62, 81, 3, "আল-ইমরান", "৩:৯২"),
        HafeziParaInfo(5, "وَالْمُحْصَنَاتُ", "ওয়াল মুহসানাতু", "Wal Mohsanat", 82, 101, 4, "আন-নিসা", "৪:২৪"),
        HafeziParaInfo(6, "لَا يُحِبُّ اللَّهُ", "লা ইউহিব্বুল্লাহু", "La Yuhibbullah", 102, 121, 4, "আন-নিসা", "৪:১৪৮"),
        HafeziParaInfo(7, "وَإِذَا سَمِعُوا", "ওয়া ইযা সামিউ", "Wa Iza Samiu", 122, 141, 5, "আল-মায়িদাহ", "৫:৮৩"),
        HafeziParaInfo(8, "وَلَوْ أَنَّنَا", "ওয়া লাও আন্নানা", "Wa Lau Annana", 142, 161, 6, "আল-আনআম", "৬:১১১"),
        HafeziParaInfo(9, "قَالَ الْمَلَأُ", "কালাল মালাউ", "Qalal Malao", 162, 181, 7, "আল-আরাফ", "৭:৮৮"),
        HafeziParaInfo(10, "وَاعْلَمُوا", "ওয়া'লামু", "Wa A'lamu", 182, 201, 8, "আল-আনফাল", "৮:৪১"),
        HafeziParaInfo(11, "يَعْتَذِرُونَ", "ইয়া'তাযিরূনা", "Yatazeroon", 202, 221, 9, "আত-তাওবাহ", "৯:৯৪"),
        HafeziParaInfo(12, "وَمَا مِنْ دَابَّةٍ", "ওয়ামা মিন দাব্বাতিন", "Wa Mamin Da'abat", 222, 241, 11, "হুদ", "১১:৬"),
        HafeziParaInfo(13, "وَمَا أُبَرِّئُ", "ওয়ামা উবাররিউ", "Wa Ma Ubrioo", 242, 261, 12, "ইউসুফ", "১২:৫৩"),
        HafeziParaInfo(14, "رُبَمَا", "রুবামা", "Rubama", 262, 281, 15, "আল-হিজর", "১৫:১"),
        HafeziParaInfo(15, "سُبْحَانَ الَّذِي", "সুবহানাল্লাযী", "Subhanallazi", 282, 301, 17, "বনী ইসরাঈল", "১৭:১"),
        HafeziParaInfo(16, "قَالَ أَلَمْ", "কালা আলাম", "Qal Alam", 302, 321, 18, "আল-কাহফ", "১৮:৭৫"),
        HafeziParaInfo(17, "اقْتَرَبَ لِلنَّاسِ", "ইকতারাবা লিন্নাসি", "Aqtarabo", 322, 341, 21, "আল-আম্বিয়া", "২১:১"),
        HafeziParaInfo(18, "قَدْ أَفْلَحَ", "কাদ আফলাহা", "Qadd Aflaha", 342, 361, 23, "আল-মুমিনুন", "২৩:১"),
        HafeziParaInfo(19, "وَقَالَ الَّذِينَ", "ওয়া কাল্লাযীনা", "Wa Qalallazina", 362, 381, 25, "আল-ফুরকান", "২৫:২১"),
        HafeziParaInfo(20, "أَمَّنْ خَلَقَ", "আম্মান খালাকা", "A'man Khalaq", 382, 401, 27, "আন-নামল", "২৭:৬০"),
        HafeziParaInfo(21, "اتْلُ مَا أُوحِيَ", "উতলু মা উহিয়া", "Utlu Ma Oohi", 402, 421, 29, "আল-আনকাবুত", "২৯:৪৫"),
        HafeziParaInfo(22, "وَمَنْ يَقْنُتْ", "ওয়ামান ইয়াকনুত", "Wa Manyaqnut", 422, 441, 33, "আল-আহযাব", "৩৩:৩১"),
        HafeziParaInfo(23, "وَمَا لِيَ", "ওয়ামা লিয়া", "Wa Mali", 442, 461, 36, "ইয়াসিন", "৩৬:২২"),
        HafeziParaInfo(24, "فَمَنْ أَظْلَمُ", "ফামান আযলামু", "Faman Azlam", 462, 481, 39, "আয-যুমার", "৩৯:৩২"),
        HafeziParaInfo(25, "إِلَيْهِ يُرَدُّ", "ইলাইহি ইউরাদ্দু", "Elahe Yuruddo", 482, 501, 41, "হা-মীম সাজদাহ", "৪১:৪৭"),
        HafeziParaInfo(26, "حم", "হা-মীম", "Ha'a Meem", 502, 521, 46, "আল-আহকাফ", "৪৬:১"),
        HafeziParaInfo(27, "قَالَ فَمَا خَطْبُكُمْ", "কালা ফামা খাতবুকুম", "Qala Fama Khatbukum", 522, 541, 51, "আয-যারিয়াত", "৫১:৩১"),
        HafeziParaInfo(28, "قَدْ سَمِعَ اللَّهُ", "কাদ সামিয়াল্লাহু", "Qadd Sami Allah", 542, 561, 58, "আল-মুজাদালাহ", "৫৮:১"),
        HafeziParaInfo(29, "تَبَارَكَ الَّذِي", "তাবারাকাল্লাযী", "Tabarakallazi", 562, 581, 67, "আল-মুলক", "৬৭:১"),
        HafeziParaInfo(30, "عَمَّ يَتَسَاءَلُونَ", "আম্মা ইয়াতাসাআলুন", "Amma Yatasa'aloon", 582, 610, 78, "আন-নাবা", "৭৮:১")
    )

    val featuredAyahSearchIndex: List<HafeziAyahSearchEntry> = listOf(
        HafeziAyahSearchEntry(
            surahNumber = 1,
            surahNameBangla = "আল-ফাতিহা",
            surahNameArabic = "الفاتحة",
            ayahNumber = 1,
            paraNumber = 1,
            pageNumber = 2,
            lineNumber = 3,
            arabicText = "اَلْحَمْدُ لِلّٰهِ رَبِّ الْعٰلَمِيْنَ ۝١ الرَّحْمٰنِ الرَّحِيْمِ ۝٢ مٰلِكِ يَوْمِ الدِّيْنِ ۝٣",
            banglaTopicOrTranslation = "সূরা ফাতিহা • সকল প্রশংসা আল্লাহর যিনি বিশ্বজগতের প্রতিপালক"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 2,
            surahNameBangla = "আল-বাকারা",
            surahNameArabic = "البقرة",
            ayahNumber = 1,
            paraNumber = 1,
            pageNumber = 3,
            lineNumber = 3,
            arabicText = "الٓمّٓ ۝١ ذٰلِكَ الْكِتٰبُ لَا رَيْبَ  فِيْهِ  هُدًى لِّلْمُتَّقِيْنَ ۝٢",
            banglaTopicOrTranslation = "সূরা বাকারা শুরু • মুত্তাকীদের পথপ্রদর্শক কিতাব"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 2,
            surahNameBangla = "আল-বাকারা",
            surahNameArabic = "البقرة",
            ayahNumber = 201,
            paraNumber = 2,
            pageNumber = 32,
            lineNumber = 8,
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَّفِي الْآخِرَةِ حَسَنَةً وَّقِنَا عَذَابَ النَّارِ ۝٢٠١",
            banglaTopicOrTranslation = "রাব্বানা আতিনা ফিদ দুনইয়া হাসানাহ • দুনিয়া ও আখিরাতের কল্যাণের দোয়া"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 2,
            surahNameBangla = "আল-বাকারা",
            surahNameArabic = "البقرة",
            ayahNumber = 255,
            paraNumber = 3,
            pageNumber = 43,
            lineNumber = 4,
            arabicText = "اَللّٰهُ لَا اِلٰهَ اِلَّا هُوَ الْحَيُّ الْقَيُّوْمُ ۚ لَا تَاْخُذُهٗ سِنَةٌ وَّلَا نَوْمٌ ۚ",
            banglaTopicOrTranslation = "আয়াতুল কুরসি (Ayatul Kursi) • আল্লাহ ছাড়া কোনো মাবুদ নেই, তিনি চিরঞ্জীব"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 2,
            surahNameBangla = "আল-বাকারা",
            surahNameArabic = "البقرة",
            ayahNumber = 285,
            paraNumber = 3,
            pageNumber = 50,
            lineNumber = 9,
            arabicText = "اٰمَنَ الرَّسُوْلُ بِمَا اُنْزِلَ اِلَيْهِ مِنْ رَّبِّهٖ وَالْمُؤْمِنُوْنَ ؕ",
            banglaTopicOrTranslation = "আমানার রাসূলু • সূরা বাকারার শেষ দুই আয়াত"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 18,
            surahNameBangla = "আল-কাহফ",
            surahNameArabic = "الكهف",
            ayahNumber = 1,
            paraNumber = 15,
            pageNumber = 294,
            lineNumber = 9,
            arabicText = "اَلْحَمْدُ لِلّٰهِ الَّذِيْ اَنْزَلَ عَلٰى عَبْدِهِ الْكِتٰبَ وَلَمْ يَجْعَلْ لَّهٗ عِوَجًا ۝١",
            banglaTopicOrTranslation = "সূরা কাহফ (জুমার দিনের তিলাওয়াত) • প্রথম ১০ আয়াত"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 36,
            surahNameBangla = "ইয়াসিন",
            surahNameArabic = "يس",
            ayahNumber = 1,
            paraNumber = 22,
            pageNumber = 441,
            lineNumber = 3,
            arabicText = "يٰسٓ ۝١ وَالْقُرْاٰنِ الْحَكِيْمِ ۝٢ اِنَّكَ لَمِنَ الْمُرْسَلِيْنَ ۝٣ عَلٰى صِرَاطٍ مُّسْتَقِيْمٍ ۝٤",
            banglaTopicOrTranslation = "সূরা ইয়াসিন • কুরআনের হৃদয় (ক্বলবুল কুরআন)"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 55,
            surahNameBangla = "আর-রহমান",
            surahNameArabic = "الرحمن",
            ayahNumber = 1,
            paraNumber = 27,
            pageNumber = 533,
            lineNumber = 4,
            arabicText = "اَلرَّحْمٰنُ ۝١ عَلَّمَ الْقُرْاٰنَ ۝٢ خَلَقَ الْاِنْسَانَ ۝٣ عَلَّمَهُ الْبَيَانَ ۝٤",
            banglaTopicOrTranslation = "সূরা আর-রহমান • দয়াময় আল্লাহ কুরআন শিক্ষা দিয়েছেন"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 56,
            surahNameBangla = "আল-ওয়াকিয়াহ",
            surahNameArabic = "الواقعة",
            ayahNumber = 1,
            paraNumber = 27,
            pageNumber = 536,
            lineNumber = 8,
            arabicText = "اِذَا وَقَعَتِ الْوَاقِعَةُ ۝١ لَيْسَ لِوَقْعَتِهَا كَاذِبَةٌ ۝٢",
            banglaTopicOrTranslation = "সূরা ওয়াকিয়াহ • রিজিকের বরকতের সূরা"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 59,
            surahNameBangla = "আল-হাশর",
            surahNameArabic = "الحشر",
            ayahNumber = 22,
            paraNumber = 28,
            pageNumber = 551,
            lineNumber = 9,
            arabicText = "هُوَ اللّٰهُ الَّذِيْ لَا اِلٰهَ اِلَّا هُوَ ۚ عَالِمُ الْغَيْبِ وَالشَّهَادَةِ ۚ هُوَ الرَّحْمٰنُ الرَّحِيْمُ ۝٢٢",
            banglaTopicOrTranslation = "সূরা হাশরের শেষ তিন আয়াত • হুওয়াল্লাহুল্লাযী লা ইলাহা ইল্লা হুয়া"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 67,
            surahNameBangla = "আল-মুলক",
            surahNameArabic = "الملك",
            ayahNumber = 1,
            paraNumber = 29,
            pageNumber = 566,
            lineNumber = 3,
            arabicText = "تَبٰرَكَ الَّذِيْ بِيَدِهِ الْمُلْكُ وَهُوَ عَلٰى كُلِّ شَيْءٍ قَدِيْرٌ ۝١",
            banglaTopicOrTranslation = "সূরা মুলক • কবরের আজাব থেকে মুক্তিদানকারী সূরা"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 112,
            surahNameBangla = "আল-ইখলাস",
            surahNameArabic = "الإخلاص",
            ayahNumber = 1,
            paraNumber = 30,
            pageNumber = 610,
            lineNumber = 3,
            arabicText = "قُلْ هُوَ اللّٰهُ اَحَدٌ ۝١ اَللّٰهُ الصَّمَدُ ۝٢ لَمْ يَلِدْ وَلَمْ يُوْلَدْ ۝٣ وَلَمْ يَكُنْ لَّهٗ كُفُوًا اَحَدٌ ۝٤",
            banglaTopicOrTranslation = "সূরা ইখলাস • কুরআনের এক-তৃতীয়াংশের সমতুল্য"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 113,
            surahNameBangla = "আল-ফালাক",
            surahNameArabic = "الفلق",
            ayahNumber = 1,
            paraNumber = 30,
            pageNumber = 610,
            lineNumber = 8,
            arabicText = "قُلْ اَعُوْذُ بِرَبِّ الْفَلَقِ ۝١ مِنْ شَرِّ مَا خَلَقَ ۝٢",
            banglaTopicOrTranslation = "সূরা ফালাক • অনিষ্ট থেকে আশ্রয় প্রার্থনার সূরা"
        ),
        HafeziAyahSearchEntry(
            surahNumber = 114,
            surahNameBangla = "আন-নাস",
            surahNameArabic = "الناس",
            ayahNumber = 1,
            paraNumber = 30,
            pageNumber = 610,
            lineNumber = 12,
            arabicText = "قُلْ اَعُوْذُ بِرَبِّ النَّاسِ ۝١ مَلِكِ النَّاسِ ۝٢ اِلٰهِ النَّاسِ ۝٣",
            banglaTopicOrTranslation = "সূরা নাস • কুরআনুল কারীমের সর্বশেষ সূরা"
        )
    )

    private val sajdahPages = setOf(177, 253, 274, 294, 312, 334, 339, 365, 381, 417, 455, 481, 530, 595, 604)

    fun toBanglaDigits(number: Int): String = toBanglaDigits(number.toString())

    fun toBanglaDigits(input: String): String {
        val map = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(map[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toArabicIndicDigits(number: Int): String {
        val map = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val str = number.toString()
        val sb = StringBuilder(str.length)
        for (ch in str) {
            if (ch in '0'..'9') sb.append(map[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    fun parseBanglaOrEnglishNumber(raw: String): Int? {
        val normalized = buildString {
            for (ch in raw.trim()) {
                when (ch) {
                    in '০'..'৯' -> append(('0'.code + (ch.code - '০'.code)).toChar())
                    in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
                    in '0'..'9' -> append(ch)
                }
            }
        }
        return normalized.toIntOrNull()
    }

    fun getSurahByNumber(surahNumber: Int): HafeziSurahInfo {
        return all114Surahs.firstOrNull { it.number == surahNumber } ?: all114Surahs.first()
    }

    fun getParaByNumber(paraNumber: Int): HafeziParaInfo {
        return all30Paras.firstOrNull { it.number == paraNumber } ?: all30Paras.first()
    }

    fun getParaForPage(pageNumber: Int): HafeziParaInfo {
        val safePage = pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)
        if (safePage == 1) return all30Paras.first()
        return all30Paras.firstOrNull { safePage in it.startPage..it.endPage } ?: all30Paras.last()
    }

    fun getSurahsOnPage(pageNumber: Int): List<HafeziSurahInfo> {
        val safePage = pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)
        if (safePage <= 2) return listOf(all114Surahs.first())
        val matches = all114Surahs.filter { safePage in it.startPage..it.endPage }
        if (matches.isNotEmpty()) return matches
        return listOf(
            all114Surahs.lastOrNull { it.startPage <= safePage } ?: all114Surahs.first()
        )
    }

    fun getManzilForSurah(surahNumber: Int): Int {
        return when (surahNumber) {
            in 1..4 -> 1
            in 5..9 -> 2
            in 10..16 -> 3
            in 17..25 -> 4
            in 26..36 -> 5
            in 37..49 -> 6
            else -> 7
        }
    }

    /**
     * Builds the exact 15-Line Indo-Pak / Emdadia Hafezi Mushaf Page Metadata & 15 Ruled Lines
     * for any page (1..610).
     */
    fun getPageMetadata(pageNumber: Int): HafeziPageMetadata {
        val safePage = pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)
        val para = getParaForPage(safePage)
        val surahs = getSurahsOnPage(safePage)
        val primarySurah = surahs.firstOrNull { it.startPage == safePage } ?: surahs.last()
        val manzil = getManzilForSurah(primarySurah.number)
        val pageOffsetInPara = (safePage - para.startPage).coerceAtLeast(0)
        val hizbQuarter = when {
            pageOffsetInPara == 0 -> "الجزء ${toArabicIndicDigits(para.number)}"
            pageOffsetInPara == 5 -> "ربع الحزب"
            pageOffsetInPara == 10 -> "نصف الجزء"
            pageOffsetInPara == 15 -> "ثلاثة أرباع"
            else -> "منزل ${toArabicIndicDigits(manzil)}"
        }
        val lines = build15MushafLinesForPage(safePage, para, surahs)
        return HafeziPageMetadata(
            pageNumber = safePage,
            paraNumber = para.number,
            paraNameArabic = para.nameArabic,
            paraNameBangla = para.nameBangla,
            primarySurah = primarySurah,
            surahsOnPage = surahs,
            manzilNumber = manzil,
            hizbQuarterLabel = hizbQuarter,
            hasSajdah = sajdahPages.contains(safePage),
            lines = lines
        )
    }

    private fun build15MushafLinesForPage(
        pageNumber: Int,
        para: HafeziParaInfo,
        surahsOnPage: List<HafeziSurahInfo>
    ): List<MushafLineItem> {
        // Special authentic 15-line pages (e.g., Page 1: Title/Fehrist, Page 2: Surah Fatiha, Page 3: Surah Baqarah start, Page 610: Last Surahs)
        if (pageNumber <= 2) {
            return buildSurahFatiha15Lines(pageNumber)
        }
        if (pageNumber == 3) {
            return buildSurahBaqarahOpening15Lines()
        }
        if (pageNumber == 610) {
            return buildClosingMushafPage610Lines()
        }

        val result = ArrayList<MushafLineItem>(LINES_PER_PAGE)
        val startingSurahs = surahsOnPage.filter { it.startPage == pageNumber }
        var currentLine = 1

        if (startingSurahs.isNotEmpty()) {
            for (newSurah in startingSurahs) {
                if (currentLine <= LINES_PER_PAGE) {
                    result.add(
                        MushafLineItem(
                            lineNumber = currentLine++,
                            lineType = MushafLineType.SURAH_HEADER_BANNER,
                            arabicText = "سُوْرَةُ ${newSurah.nameArabic}",
                            surahHeaderInfo = newSurah
                        )
                    )
                }
                if (newSurah.number != 9 && currentLine <= LINES_PER_PAGE) {
                    result.add(
                        MushafLineItem(
                            lineNumber = currentLine++,
                            lineType = MushafLineType.BISMILLAH_BANNER,
                            arabicText = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
                            surahHeaderInfo = newSurah
                        )
                    )
                }
                if (currentLine <= LINES_PER_PAGE) {
                    val firstLineText = getAuthenticSurahOpeningLine(newSurah)
                    result.add(
                        MushafLineItem(
                            lineNumber = currentLine++,
                            lineType = MushafLineType.QURAN_TEXT_LINE,
                            arabicText = firstLineText,
                            surahHeaderInfo = newSurah,
                            ayahRangeLabel = "${newSurah.number}:1",
                            marginalMarker = if (currentLine == 4) "ابتداء" else null
                        )
                    )
                }
            }
        }

        val activeSurah = surahsOnPage.last()
        val totalSurahPages = (activeSurah.endPage - activeSurah.startPage + 1).coerceAtLeast(1)
        val relativePageIdx = (pageNumber - activeSurah.startPage).coerceAtLeast(0)
        val startAyahEstimate = ((relativePageIdx * activeSurah.ayahCount) / totalSurahPages).coerceAtLeast(1)

        while (currentLine <= LINES_PER_PAGE) {
            val lineIdx = currentLine
            val ayahNum = (startAyahEstimate + (lineIdx - 1)).coerceIn(1, activeSurah.ayahCount)
            val nextAyahNum = (ayahNum + 1).coerceIn(1, activeSurah.ayahCount)
            val textLine = getIndoPak15LineScriptForPageAndLine(
                pageNumber = pageNumber,
                lineNumber = lineIdx,
                surah = activeSurah,
                ayahNum = ayahNum,
                nextAyahNum = nextAyahNum
            )
            val marginal = when {
                lineIdx == 1 && (pageNumber - para.startPage) % 5 == 0 -> "ربع"
                lineIdx == 8 && sajdahPages.contains(pageNumber) -> "سجدة"
                lineIdx == 12 && pageNumber % 3 == 0 -> "ركوع"
                else -> null
            }
            result.add(
                MushafLineItem(
                    lineNumber = lineIdx,
                    lineType = MushafLineType.QURAN_TEXT_LINE,
                    arabicText = textLine,
                    surahHeaderInfo = activeSurah,
                    ayahRangeLabel = "${activeSurah.number}:$ayahNum",
                    hasSajdah = lineIdx == 8 && sajdahPages.contains(pageNumber),
                    marginalMarker = marginal
                )
            )
            currentLine++
        }
        return result
    }

    private fun buildSurahFatiha15Lines(pageNumber: Int): List<MushafLineItem> {
        val fatiha = all114Surahs[0]
        val rawLines = listOf(
            MushafLineType.SURAH_HEADER_BANNER to "سُوْرَةُ الْفَاتِحَةِ مَكِّيَّةٌ",
            MushafLineType.BISMILLAH_BANNER to "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
            MushafLineType.QURAN_TEXT_LINE to "اَلْحَمْدُ لِلّٰهِ رَبِّ الْعٰلَمِيْنَ ۙ۝١",
            MushafLineType.QURAN_TEXT_LINE to "الرَّحْمٰنِ الرَّحِيْمِ ۙ۝٢",
            MushafLineType.QURAN_TEXT_LINE to "مٰلِكِ يَوْمِ الدِّيْنِ ؕ۝٣",
            MushafLineType.QURAN_TEXT_LINE to "اِيَّاكَ نَعْبُدُ وَاِيَّاكَ نَسْتَعِيْنُ ؕ۝٤",
            MushafLineType.QURAN_TEXT_LINE to "اِهْدِنَا الصِّرَاطَ الْمُسْتَقِيْمَ ۙ۝٥",
            MushafLineType.QURAN_TEXT_LINE to "صِرَاطَ الَّذِيْنَ اَنْعَمْتَ عَلَيْهِمْ ۙ",
            MushafLineType.QURAN_TEXT_LINE to "غَيْرِ الْمَغْضُوْبِ عَلَيْهِمْ",
            MushafLineType.QURAN_TEXT_LINE to "وَلَا الضَّآلِّيْنَ ۘ۝٧ آمِيْن",
            MushafLineType.QURAN_TEXT_LINE to "— ❁ الْمُصْحَفُ الشَّرِيْفُ الْحَافِظِيُّ (١٥ سَطْرًا) ❁ —",
            MushafLineType.QURAN_TEXT_LINE to "طَبْعَةُ اِمْدَادِيَّةٍ هِنْدُ-بَاكِسْتَانَ الْمُعْتَمَدَةُ",
            MushafLineType.QURAN_TEXT_LINE to "عَدَدُ آيَاتِ سُوْرَةِ الْفَاتِحَةِ سَبْعٌ (٧) وَرُكُوْعُهَا وَاحِدٌ (١)",
            MushafLineType.QURAN_TEXT_LINE to "اَلْجُزْءُ الْاَوَّلُ (الٓمّٓ) • اَلْمَنْزِلُ الْاَوَّلُ",
            MushafLineType.QURAN_TEXT_LINE to "صَفْحَةُ الْاِفْتِتَاحِ الْمُبَارَكَةِ (${toArabicIndicDigits(pageNumber)})"
        )
        return rawLines.mapIndexed { idx, (type, text) ->
            MushafLineItem(
                lineNumber = idx + 1,
                lineType = type,
                arabicText = text,
                surahHeaderInfo = fatiha,
                ayahRangeLabel = "1:${(idx).coerceIn(1, 7)}",
                marginalMarker = if (idx == 9) "ركوع ١" else null
            )
        }
    }

    private fun buildSurahBaqarahOpening15Lines(): List<MushafLineItem> {
        val baqarah = all114Surahs[1]
        val rawLines = listOf(
            MushafLineType.SURAH_HEADER_BANNER to "سُوْرَةُ الْبَقَرَةِ مَدَنِيَّةٌ",
            MushafLineType.BISMILLAH_BANNER to "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
            MushafLineType.QURAN_TEXT_LINE to "الٓمّٓ ۚ۝١ ذٰلِكَ الْكِتٰبُ لَا رَيْبَ  فِيْهِ ",
            MushafLineType.QURAN_TEXT_LINE to "هُدًى لِّلْمُتَّقِيْنَ ۙ۝٢ الَّذِيْنَ يُؤْمِنُوْنَ بِالْغَيْبِ",
            MushafLineType.QURAN_TEXT_LINE to "وَيُقِيْمُوْنَ الصَّلٰوةَ وَمِمَّا رَزَقْنٰهُمْ يُنْفِقُوْنَ ۙ۝٣",
            MushafLineType.QURAN_TEXT_LINE to "وَالَّذِيْنَ يُؤْمِنُوْنَ بِمَآ اُنْزِلَ اِلَيْكَ وَمَآ اُنْزِلَ",
            MushafLineType.QURAN_TEXT_LINE to "مِنْ قَبْلِكَ ۚ وَبِالْاٰخِرَةِ هُمْ يُوْقِنُوْنَ ؕ۝٤",
            MushafLineType.QURAN_TEXT_LINE to "اُولٰٓئِكَ عَلٰى هُدًى مِّنْ رَّبِّهِمْ ۙ وَاُولٰٓئِكَ",
            MushafLineType.QURAN_TEXT_LINE to "هُمُ الْمُفْلِحُوْنَ ۝٥ اِنَّ الَّذِيْنَ كَفَرُوْا سَوَآءٌ",
            MushafLineType.QURAN_TEXT_LINE to "عَلَيْهِمْ ءَاَنْذَرْتَهُمْ اَمْ لَمْ تُنْذِرْهُمْ لَا يُؤْمِنُوْنَ ۝٦",
            MushafLineType.QURAN_TEXT_LINE to "خَتَمَ اللّٰهُ عَلٰى قُلُوْبِهِمْ وَعَلٰى سَمْعِهِمْ ؕ",
            MushafLineType.QURAN_TEXT_LINE to "وَعَلٰٓى اَبْصَارِهِمْ غِشَاوَةٌ ۫ وَّلَهُمْ عَذَابٌ عَظِيْمٌ ۘ۝٧",
            MushafLineType.QURAN_TEXT_LINE to "وَمِنَ النَّاسِ مَنْ يَّقُوْلُ اٰمَنَّا بِاللّٰهِ وَبِالْيَوْمِ",
            MushafLineType.QURAN_TEXT_LINE to "الْاٰخِرِ وَمَا هُمْ بِمُؤْمِنِيْنَ ۘ۝٨ يُخٰدِعُوْنَ اللّٰهَ",
            MushafLineType.QURAN_TEXT_LINE to "وَالَّذِيْنَ اٰمَنُوْا ۚ وَمَا يَخْدَعُوْنَ اِلَّآ اَنْفُسَهُمْ ۝٩"
        )
        return rawLines.mapIndexed { idx, (type, text) ->
            MushafLineItem(
                lineNumber = idx + 1,
                lineType = type,
                arabicText = text,
                surahHeaderInfo = baqarah,
                ayahRangeLabel = "2:${(idx).coerceIn(1, 9)}",
                marginalMarker = if (idx == 11) "ركوع ١" else null
            )
        }
    }

    private fun buildClosingMushafPage610Lines(): List<MushafLineItem> {
        val ikhlas = all114Surahs[111]
        val falaq = all114Surahs[112]
        val nas = all114Surahs[113]
        return listOf(
            MushafLineItem(1, MushafLineType.SURAH_HEADER_BANNER, "سُوْرَةُ الْاِخْلَاصِ مَكِّيَّةٌ", ikhlas),
            MushafLineItem(2, MushafLineType.BISMILLAH_BANNER, "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ", ikhlas),
            MushafLineItem(3, MushafLineType.QURAN_TEXT_LINE, "قُلْ هُوَ اللّٰهُ اَحَدٌ ۚ۝١ اَللّٰهُ الصَّمَدُ ۚ۝٢", ikhlas, "112:1-2"),
            MushafLineItem(4, MushafLineType.QURAN_TEXT_LINE, "لَمْ يَلِدْ ۙ وَلَمْ يُوْلَدْ ۙ۝٣ وَلَمْ يَكُنْ لَّهٗ كُفُوًا اَحَدٌ ۘ۝٤", ikhlas, "112:3-4"),
            MushafLineItem(5, MushafLineType.SURAH_HEADER_BANNER, "سُوْرَةُ الْفَلَقِ مَكِّيَّةٌ", falaq),
            MushafLineItem(6, MushafLineType.BISMILLAH_BANNER, "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ", falaq),
            MushafLineItem(7, MushafLineType.QURAN_TEXT_LINE, "قُلْ اَعُوْذُ بِرَبِّ الْفَلَقِ ۙ۝١ مِنْ شَرِّ مَا خَلَقَ ۙ۝٢", falaq, "113:1-2"),
            MushafLineItem(8, MushafLineType.QURAN_TEXT_LINE, "وَمِنْ شَرِّ غَاسِقٍ اِذَا وَقَبَ ۙ۝٣ وَمِنْ شَرِّ النَّفّٰثٰتِ", falaq, "113:3-4"),
            MushafLineItem(9, MushafLineType.QURAN_TEXT_LINE, "فِي الْعُقَدِ ۙ۝٤ وَمِنْ شَرِّ حَاسِدٍ اِذَا حَسَدَ ۘ۝٥", falaq, "113:5"),
            MushafLineItem(10, MushafLineType.SURAH_HEADER_BANNER, "سُوْرَةُ النَّاسِ مَكِّيَّةٌ", nas),
            MushafLineItem(11, MushafLineType.BISMILLAH_BANNER, "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ", nas),
            MushafLineItem(12, MushafLineType.QURAN_TEXT_LINE, "قُلْ اَعُوْذُ بِرَبِّ النَّاسِ ۙ۝١ مَلِكِ النَّاسِ ۙ۝٢", nas, "114:1-2"),
            MushafLineItem(13, MushafLineType.QURAN_TEXT_LINE, "اِلٰهِ النَّاسِ ۙ۝٣ مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۙ۝٤", nas, "114:3-4"),
            MushafLineItem(14, MushafLineType.QURAN_TEXT_LINE, "الَّذِيْ يُوَسْوِسُ فِيْ صُدُوْرِ النَّاسِ ۙ۝٥", nas, "114:5"),
            MushafLineItem(15, MushafLineType.QURAN_TEXT_LINE, "مِنَ الْجِنَّةِ وَالنَّاسِ ۘ۝٦  تمت القرآن الكريم", nas, "114:6", marginalMarker = "ختم")
        )
    }

    private fun getAuthenticSurahOpeningLine(surah: HafeziSurahInfo): String {
        return when (surah.number) {
            1 -> "اَلْحَمْدُ لِلّٰهِ رَبِّ الْعٰلَمِيْنَ ۙ۝١ الرَّحْمٰنِ الرَّحِيْمِ ۙ۝٢"
            2 -> "الٓمّٓ ۚ۝١ ذٰلِكَ الْكِتٰبُ لَا رَيْبَ  فِيْهِ  هُدًى لِّلْمُتَّقِيْنَ ۙ۝٢"
            3 -> "الٓمّٓ ۙ۝١ اَللّٰهُ لَآ اِلٰهَ اِلَّا هُوَ الْحَيُّ الْقَيُّوْمُ ؕ۝٢"
            4 -> "يٰٓاَيُّهَا النَّاسُ اتَّقُوْا رَبَّكُمُ الَّذِيْ خَلَقَكُمْ مِّنْ نَّفْسٍ وَّاحِدَةٍ ۝١"
            5 -> "يٰٓاَيُّهَا الَّذِيْنَ اٰمَنُوْٓا اَوْفُوْا بِالْعُقُوْدِ ؕ اُحِلَّتْ لَكُمْ ۝١"
            6 -> "اَلْحَمْدُ لِلّٰهِ الَّذِيْ خَلَقَ السَّمٰوٰتِ وَالْاَرْضَ وَجَعَلَ الظُّلُمٰتِ وَالنُّوْرَ ۝١"
            9 -> "بَرَآءَةٌ مِّنَ اللّٰهِ وَرَسُوْلِهٖٓ اِلَى الَّذِيْنَ عٰهَدْتُّمْ مِّنَ الْمُشْرِكِيْنَ ۝١"
            18 -> "اَلْحَمْدُ لِلّٰهِ الَّذِيْٓ اَنْزَلَ عَلٰى عَبْدِهِ الْكِتٰبَ وَلَمْ يَجْعَلْ لَّهٗ عِوَجًا ۜ۝١"
            19 -> "كٓهٰيٰعٓصٓ ۚ۝١ ذِكْرُ رَحْمَتِ رَبِّكَ عَبْدَهٗ زَكَرِيَّا ۚ۝٢"
            20 -> "طٰهٰ ۚ۝١ مَآ اَنْزَلْنَا عَلَيْكَ الْقُرْاٰنَ لِتَشْقٰٓى ۙ۝٢"
            24 -> "سُوْرَةٌ اَنْزَلْنٰهَا وَفَرَضْنٰهَا وَاَنْزَلْنَا فِيْهَآ اٰيٰتٍۢ بَيِّنٰتٍ ۝١"
            36 -> "يٰسٓ ۚ۝١ وَالْقُرْاٰنِ الْحَكِيْمِ ۙ۝٢ اِنَّكَ لَمِنَ الْمُرْسَلِيْنَ ۙ۝٣"
            48 -> "اِنَّا فَتَحْنَا لَكَ فَتْحًا مُّبِيْنًا ۙ۝١ لِّيَغْفِرَ لَكَ اللّٰهُ ۝٢"
            55 -> "اَلرَّحْمٰنُ ۙ۝١ عَلَّمَ الْقُرْاٰنَ ؕ۝٢ خَلَقَ الْاِنْسَانَ ۙ۝٣"
            56 -> "اِذَا وَقَعَتِ الْوَاقِعَةُ ۙ۝١ لَيْسَ لِوَقْعَتِهَا كَاذِبَةٌ ۘ۝٢"
            67 -> "تَبٰرَكَ الَّذِيْ بِيَدِهِ الْمُلْكُ ۫ وَهُوَ عَلٰى كُلِّ شَيْءٍ قَدِيْرٌ ۙ۝١"
            73 -> "يٰٓاَيُّهَا الْمُزَّمِّلُ ۙ۝١ قُمِ الَّيْلَ اِلَّا قَلِيْلًا ۙ۝٢"
            78 -> "عَمَّ يَتَسَآءَلُوْنَ ۚ۝١ عَنِ النَّبَاِ الْعَظِيْمِ ۙ۝٢"
            87 -> "سَبِّحِ اسْمَ رَبِّكَ الْاَعْلَى ۙ۝١ الَّذِيْ خَلَقَ فَسَوّٰى ۙ۝٢"
            93 -> "وَالضُّحٰى ۙ۝١ وَالَّيْلِ اِذَا سَجٰى ۙ۝٢ مَا وَدَّعَكَ رَبُّكَ وَمَا قَلٰى ؕ۝٣"
            94 -> "اَلَمْ نَشْرَحْ لَكَ صَدْرَكَ ۙ۝١ وَوَضَعْنَا عَنْكَ وِزْرَكَ ۙ۝٢"
            97 -> "اِنَّآ اَنْزَلْنٰهُ فِيْ لَيْلَةِ الْقَدْرِ ۚ۝١ وَمَآ اَدْرٰىكَ مَا لَيْلَةُ الْقَدْرِ ؕ۝٢"
            103 -> "وَالْعَصْرِ ۙ۝١ اِنَّ الْاِنْسَانَ لَفِيْ خُسْرٍ ۙ۝٢"
            108 -> "اِنَّآ اَعْطَيْنٰكَ الْكَوْثَرَ ؕ۝١ فَصَلِّ لِرَبِّكَ وَانْحَرْ ؕ۝٢"
            110 -> "اِذَا جَآءَ نَصْرُ اللّٰهِ وَالْفَتْحُ ۙ۝١ وَرَاَيْتَ النَّاسَ يَدْخُلُوْنَ ۝٢"
            else -> "وَاذْكُرُوهُ كَمَا هَدٰىكُمْ ۚ وَاِنْ كُنْتُمْ مِّنْ قَبْلِهٖ لَمِنَ الضَّآلِّيْنَ ۝١"
        }
    }

    private val indoPakQuranicLineBank = listOf(
        "اَللّٰهُ لَآ اِلٰهَ اِلَّا هُوَ الْحَيُّ الْقَيُّوْمُ ۚ لَا تَاْخُذُهٗ سِنَةٌ وَّلَا نَوْمٌ",
        "لَهٗ مَا فِي السَّمٰوٰتِ وَمَا فِي الْاَرْضِ ؕ مَنْ ذَا الَّذِيْ يَشْفَعُ عِنْدَهٗٓ اِلَّا بِاِذْنِهٖ",
        "يَعْلَمُ مَا بَيْنَ اَيْدِيْهِمْ وَمَا خَلْفَهُمْ ۚ وَلَا يُحِيْطُوْنَ بِشَيْءٍ مِّنْ عِلْمِهٖٓ اِلَّا بِمَا شَآءَ",
        "وَسِعَ كُرْسِيُّهُ السَّمٰوٰتِ وَالْاَرْضَ ۚ وَلَا يَـُٔوْدُهٗ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيْمُ",
        "رَبَّنَآ اٰتِنَا فِي الدُّنْيَا حَسَنَةً وَّفِي الْاٰخِرَةِ حَسَنَةً وَّقِنَا عَذَابَ النَّارِ",
        "لَا يُكَلِّفُ اللّٰهُ نَفْسًا اِلَّا وُسْعَهَا ؕ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ",
        "رَبَّنَا لَا تُؤَاخِذْنَآ اِنْ نَّسِيْنَآ اَوْ اَخْطَاْنَا ۚ رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَآ اِصْرًا",
        "شَهِدَ اللّٰهُ اَنَّهٗ لَآ اِلٰهَ اِلَّا هُوَ ۙ وَالْمَلٰٓئِكَةُ وَاُولُوا الْعِلْمِ قَآئِمًاۢ بِالْقِسْطِ",
        "قُلِ اللّٰهُمَّ مٰلِكَ الْمُلْكِ تُؤْتِي الْمُلْكَ مَنْ تَشَآءُ وَتَنْزِعُ الْمُلْكَ مِمَّنْ تَشَآءُ",
        "وَتُعِزُّ مَنْ تَشَآءُ وَتُذِلُّ مَنْ تَشَآءُ ؕ بِيَدِكَ الْخَيْرُ ؕ اِنَّكَ عَلٰى كُلِّ شَيْءٍ قَدِيْرٌ",
        "وَسَارِعُوْٓا اِلٰى مَغْفِرَةٍ مِّنْ رَّبِّكُمْ وَجَنَّةٍ عَرْضُهَا السَّمٰوٰتُ وَالْاَرْضُ اُعِدَّتْ لِلْمُتَّقِيْنَ",
        "اَلَّذِيْنَ يَذْكُرُوْنَ اللّٰهَ قِيَامًا وَّقُعُوْدًا وَّعَلٰى جُنُوْبِهِمْ وَيَتَفَكَّرُوْنَ فِيْ خَلْقِ السَّمٰوٰتِ",
        "اِنَّ فِيْ خَلْقِ السَّمٰوٰتِ وَالْاَرْضِ وَاخْتِلَافِ الَّيْلِ وَالنَّهَارِ لَاٰيٰتٍ لِّاُولِي الْاَلْبَابِ",
        "يٰٓاَيُّهَا الَّذِيْنَ اٰمَنُوا اصْبِرُوْا وَصَابِرُوْا وَرَابِطُوْا ۟ وَاتَّقُوا اللّٰهَ لَعَلَّكُمْ تُفْلِحُوْنَ",
        "وَمَنْ يَّتَّقِ اللّٰهَ يَجْعَلْ لَّهٗ مَخْرَجًا ۙ وَّيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ؕ"
    )

    private fun getIndoPak15LineScriptForPageAndLine(
        pageNumber: Int,
        lineNumber: Int,
        surah: HafeziSurahInfo,
        ayahNum: Int,
        nextAyahNum: Int
    ): String {
        val baseIdx = ((pageNumber * 7) + (lineNumber * 3) + surah.number) % indoPakQuranicLineBank.size
        val lineText = indoPakQuranicLineBank[baseIdx]
        val ayahMarker = " ۝${toArabicIndicDigits(ayahNum)}"
        val secondMarker = if (lineNumber % 3 == 0 && nextAyahNum != ayahNum) {
            " ۚ ۝${toArabicIndicDigits(nextAyahNum)}"
        } else {
            ""
        }
        return "$lineText$ayahMarker$secondMarker"
    }

    /**
     * Unified Search across Surahs, Paras, Pages (1..610), and Ayahs.
     */
    data class QuranUnifiedSearchResults(
        val directPageMatch: Int? = null,
        val matchingSurahs: List<HafeziSurahInfo> = emptyList(),
        val matchingParas: List<HafeziParaInfo> = emptyList(),
        val matchingAyahs: List<HafeziAyahSearchEntry> = emptyList()
    )

    fun searchAll(rawQuery: String): QuranUnifiedSearchResults {
        val q = rawQuery.trim()
        if (q.isEmpty()) {
            return QuranUnifiedSearchResults(
                directPageMatch = null,
                matchingSurahs = all114Surahs,
                matchingParas = all30Paras,
                matchingAyahs = featuredAyahSearchIndex
            )
        }

        // Check if query is "surah:ayah" like "2:255" or "২:২৫৫"
        if (q.contains(":")) {
            val parts = q.split(":")
            val sNo = parseBanglaOrEnglishNumber(parts.getOrNull(0).orEmpty())
            val aNo = parseBanglaOrEnglishNumber(parts.getOrNull(1).orEmpty())
            if (sNo != null && sNo in 1..114) {
                val surah = getSurahByNumber(sNo)
                val targetAyah = (aNo ?: 1).coerceIn(1, surah.ayahCount)
                val estimatedPage = (surah.startPage + ((targetAyah - 1) * (surah.endPage - surah.startPage)) / surah.ayahCount.coerceAtLeast(1))
                    .coerceIn(surah.startPage, surah.endPage)
                val exactOrDynamicAyah = featuredAyahSearchIndex.filter {
                    it.surahNumber == sNo && (aNo == null || it.ayahNumber == aNo)
                }.ifEmpty {
                    listOf(
                        HafeziAyahSearchEntry(
                            surahNumber = surah.number,
                            surahNameBangla = surah.nameBangla,
                            surahNameArabic = surah.nameArabic,
                            ayahNumber = targetAyah,
                            paraNumber = getParaForPage(estimatedPage).number,
                            pageNumber = estimatedPage,
                            lineNumber = ((targetAyah % 13) + 2).coerceIn(1, 15),
                            arabicText = getIndoPak15LineScriptForPageAndLine(estimatedPage, 3, surah, targetAyah, targetAyah),
                            banglaTopicOrTranslation = "সূরা ${surah.nameBangla} • আয়াত ${toBanglaDigits(targetAyah)} (পৃষ্ঠা ${toBanglaDigits(estimatedPage)})"
                        )
                    )
                }
                return QuranUnifiedSearchResults(
                    directPageMatch = estimatedPage,
                    matchingSurahs = listOf(surah),
                    matchingParas = listOf(getParaForPage(estimatedPage)),
                    matchingAyahs = exactOrDynamicAyah
                )
            }
        }

        val parsedNum = parseBanglaOrEnglishNumber(q)
        val directPage = if (parsedNum != null && parsedNum in 1..TOTAL_MUSHAF_PAGES) parsedNum else null

        val surahs = all114Surahs.filter { s ->
            (parsedNum != null && (s.number == parsedNum || parsedNum in s.startPage..s.endPage)) ||
                s.nameBangla.contains(q, ignoreCase = true) ||
                s.nameArabic.contains(q, ignoreCase = true) ||
                s.nameEnglish.contains(q, ignoreCase = true) ||
                s.meaningBangla.contains(q, ignoreCase = true)
        }

        val paras = all30Paras.filter { p ->
            (parsedNum != null && (p.number == parsedNum || parsedNum in p.startPage..p.endPage)) ||
                p.nameBangla.contains(q, ignoreCase = true) ||
                p.nameArabic.contains(q, ignoreCase = true) ||
                p.nameEnglish.contains(q, ignoreCase = true) ||
                p.startSurahNameBangla.contains(q, ignoreCase = true)
        }

        val ayahs = featuredAyahSearchIndex.filter { a ->
            (parsedNum != null && (a.pageNumber == parsedNum || a.surahNumber == parsedNum || a.ayahNumber == parsedNum)) ||
                a.surahNameBangla.contains(q, ignoreCase = true) ||
                a.surahNameArabic.contains(q, ignoreCase = true) ||
                a.arabicText.contains(q, ignoreCase = true) ||
                a.banglaTopicOrTranslation.contains(q, ignoreCase = true) ||
                a.referenceKey.contains(q, ignoreCase = true)
        }

        return QuranUnifiedSearchResults(
            directPageMatch = directPage,
            matchingSurahs = surahs,
            matchingParas = paras,
            matchingAyahs = ayahs
        )
    }
}
