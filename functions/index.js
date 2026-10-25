/**
 * Firebase Cloud Functions (v2) for Qawmi Kormosongsthan Bangladesh:
 * 1. "আজকের স্মরণ" (Daily Islamic Reminder) via Cloud Scheduler (`sendDailyAjkerSmoronPush`)
 * 2. Real-time Server-Side Telegram Bot Dispatch (`dispatchTelegramNotificationOnCreated`)
 *    with `getMe` + `sendMessage` verification, Secret Manager protection, idempotency,
 *    and truthful `QUEUED` -> `DELIVERED` / `FAILED` status tracking.
 */

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

admin.initializeApp();

// Secure server-side secret managed via Firebase Secret Manager / Google Cloud Secret Manager
const telegramBotTokenSecret = defineSecret("TELEGRAM_BOT_TOKEN");
const DEFAULT_ADMIN_CHAT_ID = "6255562134";
const DEFAULT_ADMIN_BOT_ID = "8982853197";

function sanitizeSecrets(rawText) {
  if (!rawText) return "";
  return String(rawText)
    .replace(/\b\d{8,12}:[A-Za-z0-9_-]{25,}\b/g, "[REDACTED_BOT_TOKEN]")
    .replace(/\bAIza[0-9A-Za-z_-]{20,}\b/g, "[REDACTED_FIREBASE_SECRET]")
    .replace(/https?:\/\/[^\s]*(oobCode|resetPassword|apiKey=)[^\s]*/gi, "[REDACTED_RESET_LINK]")
    .replace(/(password|পাসওয়ার্ড|পাসওয়ার্ড|otp|secret|token)\s*[:=]\s*\S+/gi, "$1: [REDACTED]")
    .trim();
}

function formatServerTelegramMessage(data) {
  const chatId = process.env.TELEGRAM_CHAT_ID || DEFAULT_ADMIN_CHAT_ID;
  const nowBangla = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Dhaka",
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: true,
  }).format(new Date());

  return [
    "【কওমি কর্মসংস্থান • স্বয়ংক্রিয় অ্যাডমিন বট অ্যালার্ট】",
    `টেলিগ্রাম চ্যাট আইডি: ${chatId}`,
    `টেলিগ্রাম বট আইডি: ${DEFAULT_ADMIN_BOT_ID}`,
    `সময়: ${nowBangla} (BDT)`,
    `কার্যক্রমের ধরন: ${sanitizeSecrets(data.eventType || "ADMIN_SECURITY")}`,
    `শিরোনাম: ${sanitizeSecrets(data.title || "কওমি কর্মসংস্থান ইত্তেলা")}`,
    `বিস্তারিত পরিবর্তন/কার্যকলাপ: ${sanitizeSecrets(data.message || "")}`,
    `সম্পাদনকারী অ্যাডমিন/পরিচয়: ${sanitizeSecrets(data.actorName || "কওমি কর্মসংস্থান")}`,
    `অ্যাডমিন হটলাইন: ${sanitizeSecrets(data.targetPhone || "01300448147")}`,
  ].join("\n");
}

/**
 * Firestore Trigger: Automatically processes newly created documents in `telegram_notifications/{logId}`
 * when `deliveryStatus` is `"QUEUED"` (or `"FAILED"` prior to server retry).
 * Strictly verifies `getMe` and `sendMessage` (`"ok": true` + `result.message_id`) before marking `"DELIVERED"`.
 */
exports.dispatchTelegramNotificationOnCreated = onDocumentCreated(
  {
    document: "telegram_notifications/{logId}",
    region: "asia-south1",
    secrets: [telegramBotTokenSecret],
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const data = snap.data() || {};
    // Idempotency guard: never re-send if already delivered by client or prior trigger execution
    if (data.deliveryStatus === "DELIVERED") {
      return;
    }

    const token = (
      telegramBotTokenSecret.value() ||
      process.env.TELEGRAM_BOT_TOKEN ||
      ""
    ).trim();
    const chatId = (process.env.TELEGRAM_CHAT_ID || DEFAULT_ADMIN_CHAT_ID).trim();

    if (!token || token === "TELEGRAM_BOT_TOKEN_DEFAULT_VALUE" || !token.includes(":")) {
      const configErr =
        "সার্ভার ত্রুটি: TELEGRAM_BOT_TOKEN কনফিগার করা নেই (Secret Manager-এ বট ৮৯৮২৮৫৩১৯৭-এর সক্রিয় টোকেন সেট করুন)।";
      await snap.ref.update({
        deliveryStatus: "FAILED",
        message: `${sanitizeSecrets(data.message || "")} [ডেলিভারি ত্রুটি: ${configErr}]`.slice(0, 1950),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      return;
    }

    try {
      // 1. Verify Bot Token via Telegram getMe API
      const getMeRes = await fetch(`https://api.telegram.org/bot${token}/getMe`, {
        method: "GET",
        headers: { Accept: "application/json" },
      });
      const getMeJson = await getMeRes.json();

      if (!getMeRes.ok || getMeJson.ok !== true) {
        const desc = sanitizeSecrets(getMeJson.description || `HTTP ${getMeRes.status}`);
        const errBangla =
          getMeRes.status === 401
            ? `Telegram API 401 (Unauthorized): বট টোকেনটি বৈধ নয় বা @BotFather-এ বাতিল হয়েছে (${desc})।`
            : `Telegram getMe ব্যর্থ (HTTP ${getMeRes.status}): ${desc}`;
        await snap.ref.update({
          deliveryStatus: "FAILED",
          message: `${sanitizeSecrets(data.message || "")} [ডেলিভারি ত্রুটি: ${errBangla}]`.slice(0, 1950),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        return;
      }

      // 2. Dispatch message via Telegram sendMessage API and verify "ok": true + message_id
      const formattedText = formatServerTelegramMessage(data);
      const sendRes = await fetch(`https://api.telegram.org/bot${token}/sendMessage`, {
        method: "POST",
        headers: { "Content-Type": "application/json; charset=UTF-8" },
        body: JSON.stringify({
          chat_id: chatId,
          text: formattedText,
          disable_web_page_preview: true,
        }),
      });
      const sendJson = await sendRes.json();

      if (sendRes.ok && sendJson.ok === true && sendJson.result && sendJson.result.message_id) {
        await snap.ref.update({
          deliveryStatus: "DELIVERED",
          referenceId: `${data.referenceId || "tg"}#msg_${sendJson.result.message_id}`.slice(0, 128),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      } else {
        const desc = sanitizeSecrets(sendJson.description || `HTTP ${sendRes.status}`);
        await snap.ref.update({
          deliveryStatus: "FAILED",
          message: `${sanitizeSecrets(data.message || "")} [Telegram API ত্রুটি (${sendRes.status}): ${desc}]`.slice(0, 1950),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
    } catch (err) {
      const safeNetErr = sanitizeSecrets(err && err.message ? err.message : "Network error");
      await snap.ref.update({
        deliveryStatus: "FAILED",
        message: `${sanitizeSecrets(data.message || "")} [নেটওয়ার্ক ত্রুটি: ${safeNetErr}]`.slice(0, 1950),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }
  }
);

const AJKER_SMORON_CATALOG = [
  {
    id: "smoron_01",
    categoryBangla: "দরুদ শরিফ",
    titleBangla: "প্রিয় নবী ﷺ-এর প্রতি দরুদ পাঠ",
    messageBangla:
      "আপনি কি আজ প্রিয় নবী হযরত মুহাম্মদ ﷺ-এর ওপর দরুদ শরিফ পাঠ করেছেন? আসুন, বেশি বেশি দরুদ পাঠের মাধ্যমে আমাদের দিনকে বরকতময় করি।",
    quoteBangla:
      "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “যে ব্যক্তি আমার ওপর একবার দরুদ পাঠ করে, আল্লাহ তাআলা তার ওপর দশবার রহমত বর্ষণ করেন।”",
    sourceReferenceBangla: "সহীহ মুসলিম, হাদিস: ৪০৮",
  },
  {
    id: "smoron_02",
    categoryBangla: "জিকির ও তাসবিহ",
    titleBangla: "অন্তরের প্রশান্তির জন্য আল্লাহর জিকির",
    messageBangla:
      "আজ কি আপনি আল্লাহ তাআলার জিকির করেছেন? কিছুক্ষণ সময় নিয়ে সুবহানাল্লাহ, আলহামদুলিল্লাহ, আল্লাহু আকবার পাঠ করুন—এতে অন্তর প্রশান্ত হয়।",
    quoteBangla:
      "“যারা ঈমান আনে এবং আল্লাহর স্মরণে যাদের অন্তর প্রশান্ত হয়; জেনে রাখো, আল্লাহর জিকিরেই অন্তরসমূহ প্রশান্ত হয়।”",
    sourceReferenceBangla: "আল-কুরআন, সূরা আর-রাদ: ২৮",
  },
  {
    id: "smoron_03",
    categoryBangla: "সৎকাজ ও খেদমত",
    titleBangla: "মানুষের উপকার ও ছোট নেক আমল",
    messageBangla:
      "আজ কি আপনি কারো উপকার করেছেন বা একটি ভালো কাজ করেছেন? ছোট একটি ভালো আমলও আল্লাহর কাছে অনেক মূল্যবান হতে পারে।",
    quoteBangla:
      "রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ বলেছেন: “কোনো ভালো কাজকেই কখনো তুচ্ছ মনে করো না, এমনকি তোমার ভাইয়ের সাথে হাসিমুখে সাক্ষাৎ করাকেও নয়।”",
    sourceReferenceBangla: "সহীহ মুসলিম, হাদিস: ২৬২৬",
  },
  {
    id: "smoron_04",
    categoryBangla: "ইস্তিগফার ও তাওবা",
    titleBangla: "ক্ষমা প্রার্থনা ও রিজিকের বরকত",
    messageBangla:
      "ব্যস্ততার মাঝে আজ কি আল্লাহ তাআলার কাছে ইস্তিগফার করেছেন? দিনে অন্তত কিছুক্ষণ খাঁটি অন্তরে ‘আস্তাগফিরুল্লাহ’ পাঠ করুন—এতে গুনাহ মাফ হয় ও পেরেশানি দূর হয়।",
    quoteBangla:
      "রাসূলুল্লাহ সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম বলেছেন: “আল্লাহর শপথ! আমি প্রতিদিন সত্তর বারেরও বেশি আল্লাহর কাছে ক্ষমা প্রার্থনা (ইস্তিগফার) করি এবং তাওবা করি।”",
    sourceReferenceBangla: "সহীহ বুখারী, হাদিস: ৬৩০৭",
  },
  {
    id: "smoron_05",
    categoryBangla: "নামাজের গুরুত্ব",
    titleBangla: "যথাসময়ে খুশু-খুজুর সাথে নামাজ আদায়",
    messageBangla:
      "আজকের পাঁচ ওয়াক্ত নামাজ কি জামাতের সাথে ও যথাসময়ে আদায় করার চেষ্টা করেছেন? নামাজ মুমিনের চক্ষু শীতলকারী ও আল্লাহর নৈকট্য লাভের শ্রেষ্ঠ মাধ্যম।",
    quoteBangla:
      "আবদুল্লাহ ইবনু মাসঊদ (রা.) থেকে বর্ণিত, আমি রাসূলুল্লাহ হযরত মুহাম্মদ ﷺ-কে জিজ্ঞাসা করলাম: “আল্লাহর কাছে কোন আমলটি সবচেয়ে বেশি প্রিয়?” তিনি বললেন: “যথাসময়ে নামাজ আদায় করা।”",
    sourceReferenceBangla: "সহীহ বুখারী, হাদিস: ৫২৭",
  },
];

const REMINDER_IDS = Array.from({ length: 36 }, (_, i) =>
  `smoron_${String(i + 1).padStart(2, "0")}`
);

function computeUserDayHash(userId, dateKey) {
  const combined = `${userId}::ajker_smoron::${dateKey}`;
  let h = 1125899907;
  for (let i = 0; i < combined.length; i++) {
    h = Math.imul(h, 131) + combined.charCodeAt(i);
  }
  return Math.abs(h | 0);
}

function getReminderMeta(reminderId) {
  return (
    AJKER_SMORON_CATALOG.find((item) => item.id === reminderId) ||
    AJKER_SMORON_CATALOG[0]
  );
}

exports.sendDailyAjkerSmoronPush = onSchedule(
  {
    schedule: "0 8 * * *", // প্রতিদিন সকাল ০৮:০০ মিনিটে (বাংলাদেশ সময় Asia/Dhaka)
    timeZone: "Asia/Dhaka",
    region: "asia-south1",
  },
  async () => {
    const db = admin.firestore();
    const now = new Date();
    const dateKey = new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Dhaka",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    })
      .format(now)
      .replace(/-/g, "_");

    const usersSnap = await db.collection("users").get();

    for (const userDoc of usersSnap.docs) {
      const userId = userDoc.id;
      const userData = userDoc.data() || {};
      if (userData.smoronNotificationsEnabled === false) continue;

      const readsRef = db
        .collection("users")
        .doc(userId)
        .collection("notification_reads");
      const readsSnap = await readsRef.get();
      const receipts = {};
      readsSnap.forEach((doc) => {
        receipts[doc.id] = doc.data().readAtMillis || 0;
      });

      // একই ব্যবহারকারীর কাছে দিনে ১টির বেশি নোটিফিকেশন পাঠানো হবে না
      if (receipts[`smoron_push_${dateKey}`]) continue;

      // পূর্বে না পাঠানো স্মরণবার্তা থেকে ব্যবহারকারীভেদে নির্বাচন
      const unseen = REMINDER_IDS.filter(
        (id) => !receipts[`smoron_hist_${id}`]
      );
      const candidatePool = unseen.length > 0 ? unseen : REMINDER_IDS;
      const hash = computeUserDayHash(userId, dateKey);
      const selectedReminderId = candidatePool[hash % candidatePool.length];
      const reminderMeta = getReminderMeta(selectedReminderId);
      const nowMillis = Date.now();

      // Firebase Firestore-এ আজকের স্মরণ, ইতিহাস ও পুশ স্ট্যাটাস সংরক্ষণ
      await Promise.all([
        readsRef.doc(`smoron_day_${dateKey}`).set({
          notificationId: selectedReminderId,
          userId: userId,
          readAtMillis: nowMillis,
        }),
        readsRef.doc(`smoron_hist_${selectedReminderId}`).set({
          notificationId: selectedReminderId,
          userId: userId,
          readAtMillis: nowMillis,
        }),
        readsRef.doc(`smoron_push_${dateKey}`).set({
          notificationId: selectedReminderId,
          userId: userId,
          readAtMillis: nowMillis,
        }),
      ]);

      // FCM Token থাকলে সরাসরি Android Push Notification প্রেরণ
      if (userData.fcmToken) {
        await admin.messaging().send({
          token: userData.fcmToken,
          notification: {
            title: `আজকের স্মরণ • ${reminderMeta.categoryBangla}`,
            body: reminderMeta.messageBangla,
          },
          data: {
            type: "AJKER_SMORON",
            reminderId: selectedReminderId,
            dateKey: dateKey,
            userId: userId,
            extra_open_ajker_smoron: "true",
            extra_ajker_smoron_id: selectedReminderId,
          },
          android: {
            priority: "high",
            notification: {
              channelId: "qawmi_ajker_smoron_channel",
              clickAction: "OPEN_AJKER_SMORON",
            },
          },
        });
      }
    }
  }
);
