const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID_1 = "admin_uid_102";
const ADMIN_UID_2 = "admin_uid_204";
const ADMIN_UID_3 = "admin_uid_28";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const nowDate = () => new Date(Date.now() - 1000);

test("Unauthenticated user: cannot read feed_posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("feed_posts").where("visibility", "==", "PUBLIC").get());
});

test("Authenticated user: can create and query public feed_posts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("feed_posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "দারুল উলুম কাওমি মাদরাসা",
      postType: "JOB_CIRCULAR",
      title: "দারুল উলুম কাওমি মাদরাসা",
      district: "কুমিল্লা",
      upazilaOrLocation: "কুমিল্লা সদর, কুমিল্লা",
      designation: "সহকারী শিক্ষক (আরবি, ফিকহ, তাফসীর)",
      qualification: "দাওরায়ে হাদীস / সম্মান",
      salaryOrExperience: "১২,০০০ - ১৮,০০০ টাকা",
      deadlineOrPreferredDistricts: "২০ অক্টোবর ২০২৫",
      description: "আবাসন ও খানা মাদরাসা বহন করবে।",
      contactNumber: "01700000000",
      madrasaId: "madrasa_1",
      isVerifiedMadrasa: false,
      isBoosted: false,
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("feed_posts").where("visibility", "==", "PUBLIC").get()
  );
});

test("All 3 authorized admin emails and custom claims can perform admin operations; non-admins are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "regular_user@gmail.com",
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("feed_posts").doc("post_boost_1").set({
      authorId: ALICE_UID,
      authorName: "জামিয়া ইসলামিয়া",
      postType: "JOB_CIRCULAR",
      title: "জামিয়া ইসলামিয়া",
      district: "ঢাকা",
      upazilaOrLocation: "ঢাকা",
      designation: "মুহাদ্দিস",
      qualification: "দাওরায়ে হাদীস",
      salaryOrExperience: "আলোচনা সাপেক্ষ",
      deadlineOrPreferredDistricts: "চলমান",
      description: "বিশেষ বিজ্ঞপ্তি",
      contactNumber: "01300448147",
      madrasaId: "",
      isVerifiedMadrasa: false,
      isBoosted: false,
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Non-admin cannot set isBoosted = true or write to /admins
  await assertFails(
    aliceDb.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: true,
      updatedAt: nowDate(),
    })
  );
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      adminId: ALICE_UID,
      email: "foysalahamad102@gmail.com",
      adminPhone: "01300448147",
      role: "SUPER_ADMIN",
      verifiedByAuthToken: true,
      updatedAt: nowDate(),
    })
  );

  // Authorized Admin 1: foysalahamad102@gmail.com
  const admin1Db = testEnv.authenticatedContext(ADMIN_UID_1, {
    email: "foysalahamad102@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    admin1Db.collection("admins").doc(ADMIN_UID_1).set({
      adminId: ADMIN_UID_1,
      email: "foysalahamad102@gmail.com",
      adminPhone: "01300448147",
      role: "SUPER_ADMIN",
      verifiedByAuthToken: true,
      updatedAt: nowDate(),
    })
  );
  await assertSucceeds(
    admin1Db.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: true,
      updatedAt: nowDate(),
    })
  );

  // Authorized Admin 2: foysalahamad204@gmail.com
  const admin2Db = testEnv.authenticatedContext(ADMIN_UID_2, {
    email: "foysalahamad204@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    admin2Db.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: false,
      updatedAt: nowDate(),
    })
  );

  // Authorized Admin 3: fyslahm28@gmail.com
  const admin3Db = testEnv.authenticatedContext(ADMIN_UID_3, {
    email: "fyslahm28@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    admin3Db.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: true,
      updatedAt: nowDate(),
    })
  );
});

test("Telegram notifications queue: regular users can enqueue event logs, only admins can read/list them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "regular@gmail.com",
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("telegram_notifications").doc("tg_1").set({
      eventType: "NEW_CIRCULAR",
      title: "নতুন নিয়োগ বিজ্ঞপ্তি প্রকাশিত হয়েছে",
      message: "জামিয়া ইসলামিয়ায় মুহাদ্দিস পদে নতুন নিয়োগ বিজ্ঞপ্তি।",
      actorId: ALICE_UID,
      actorName: "জামিয়া ইসলামিয়া",
      targetPhone: "01300448147",
      referenceId: "post_1",
      deliveryStatus: "QUEUED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Regular user cannot read or list telegram_notifications
  await assertFails(aliceDb.collection("telegram_notifications").get());

  // Verified Admin can read and list telegram_notifications
  const adminDb = testEnv.authenticatedContext(ADMIN_UID_1, {
    email: "foysalahamad102@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("telegram_notifications").get());
});

test("DirectMessage: supports Islamic reactions, sender-only edit and delete-for-everyone, and per-user delete-for-me", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const createdTs = nowDate();
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_adv_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "জামিয়া দারুল উলুম",
      subject: "খেদমত সংক্রান্ত যোগাযোগ",
      body: "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ",
      isRead: false,
      createdAt: createdTs,
    })
  );

  // Receiver (Bob) can add an Islamic reaction (DUA) and mark read
  await assertSucceeds(
    bobDb.collection("messages").doc("msg_adv_1").update({
      isRead: true,
      receiverReaction: "DUA",
    })
  );

  // Receiver (Bob) CANNOT edit the message body or delete for everyone
  await assertFails(
    bobDb.collection("messages").doc("msg_adv_1").update({
      body: "অননুমোদিত পরিবর্তন",
      isEdited: true,
    })
  );
  await assertFails(
    bobDb.collection("messages").doc("msg_adv_1").update({
      isDeletedForEveryone: true,
    })
  );

  // Sender (Alice) can add her own Islamic reaction (DEENI_PRERONA) and edit her message
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_adv_1").update({
      body: "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহি ওয়া বারাকাতুহু",
      isEdited: true,
      editedAt: nowDate(),
      senderReaction: "DEENI_PRERONA",
    })
  );

  // Receiver (Bob) can delete for himself (deletedForReceiver)
  await assertSucceeds(
    bobDb.collection("messages").doc("msg_adv_1").update({
      deletedForReceiver: true,
    })
  );

  // Sender (Alice) can delete for everyone (isDeletedForEveryone)
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_adv_1").update({
      body: "এই পয়গামটি প্রত্যাহার করা হয়েছে",
      isDeletedForEveryone: true,
    })
  );
});

test("Blocking & Chat Themes: blocking prevents sending messages in both directions until unblocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const ts = nowDate();
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "মুফতি মাহমুদ",
      accountType: "muhtamim",
      district: "ঢাকা",
      designation: "মুহতামিম",
      qualification: "দাওরায়ে হাদীস",
      experience: "১০ বছর",
      preferredDistricts: "ঢাকা",
      bio: "",
      contactInfo: "01700000000",
      madrasaName: "জামিয়া রাহমানিয়া",
      blockedUserIds: [ALICE_UID],
      chatThemes: { [ALICE_UID]: "ROYAL_GOLD" },
      isVerified: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // While Bob has blocked Alice, Alice cannot send a message to Bob
  await assertFails(
    aliceDb.collection("messages").doc("msg_blocked_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "মুফতি মাহমুদ",
      subject: "যোগাযোগ",
      body: "ব্লক অবস্থায় পয়গাম",
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Bob unblocks Alice
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).update({
      blockedUserIds: [],
      updatedAt: nowDate(),
    })
  );

  // Now Alice can send a message to Bob
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_unblocked_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "মুফতি মাহমুদ",
      subject: "যোগাযোগ",
      body: "আনব্লক করার পর পয়গাম",
      isRead: false,
      createdAt: nowDate(),
    })
  );
});

test("ChatGroup & Image Attachments: members can create groups, add/remove members, and send 1-5 images (rejects >5)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext("outsider_uid_999").firestore();

  const ts = nowDate();

  // Alice creates a chat group with Bob
  await assertSucceeds(
    aliceDb.collection("chat_groups").doc("group_1").set({
      name: "দাওরায়ে হাদীস ফোরাম",
      photoUrl: "preset:scholar_emerald",
      creatorId: ALICE_UID,
      creatorName: "মাওলানা আব্দুল্লাহ",
      memberIds: [ALICE_UID, BOB_UID],
      memberNames: {
        [ALICE_UID]: "মাওলানা আব্দুল্লাহ",
        [BOB_UID]: "মুফতি মাহমুদ",
      },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Outsider cannot read the group
  await assertFails(outsiderDb.collection("chat_groups").doc("group_1").get());

  // Member (Bob) can read the group and update group name/members
  await assertSucceeds(bobDb.collection("chat_groups").doc("group_1").get());
  await assertSucceeds(
    bobDb.collection("chat_groups").doc("group_1").update({
      memberIds: [ALICE_UID, BOB_UID, "member_3"],
      updatedAt: nowDate(),
    })
  );

  // Alice can send a group message with 3 attached images (1-5 allowed)
  await assertSucceeds(
    aliceDb.collection("messages").doc("group_msg_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: "GROUP:group_1",
      receiverName: "দাওরায়ে হাদীস ফোরাম",
      groupId: "group_1",
      groupName: "দাওরায়ে হাদীস ফোরাম",
      groupMemberIds: [ALICE_UID, BOB_UID, "member_3"],
      subject: "গ্রুপ কথোপকথন",
      body: "গ্রুপের সবাইকে সালাম ও ছবি প্রেরণ",
      imageUrls: [
        "data:image/jpeg;base64,img1",
        "data:image/jpeg;base64,img2",
        "data:image/jpeg;base64,img3",
      ],
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Sending more than 5 images (6 images) is rejected by security rules
  await assertFails(
    aliceDb.collection("messages").doc("group_msg_too_many_imgs").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: "GROUP:group_1",
      receiverName: "দাওরায়ে হাদীস ফোরাম",
      groupId: "group_1",
      groupName: "দাওরায়ে হাদীস ফোরাম",
      groupMemberIds: [ALICE_UID, BOB_UID],
      subject: "গ্রুপ কথোপকথন",
      body: "অতিরিক্ত ছবি",
      imageUrls: ["img1", "img2", "img3", "img4", "img5", "img6"],
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Group member (Bob) can add his reaction via userReactions map
  await assertSucceeds(
    bobDb.collection("messages").doc("group_msg_1").update({
      userReactions: { [BOB_UID]: "DUA" },
      readByUserIds: [BOB_UID],
    })
  );
});


