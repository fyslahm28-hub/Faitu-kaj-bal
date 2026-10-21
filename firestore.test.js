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
const ADMIN_UID = "admin_uid_999";

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

test("Non-admin user cannot boost a post, Admin user can boost a post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
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
      contactNumber: "01700000000",
      madrasaId: "",
      isVerifiedMadrasa: false,
      isBoosted: false,
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  // Non-admin cannot set isBoosted = true
  await assertFails(
    aliceDb.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: true,
      updatedAt: nowDate(),
    })
  );

  // Admin can set isBoosted = true
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "fyslahm28@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("feed_posts").doc("post_boost_1").update({
      isBoosted: true,
      updatedAt: nowDate(),
    })
  );
});

test("Notifications and per-user read receipts: broadcast, direct, and admin announcements", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates a broadcast notification for a new circular
  await assertSucceeds(
    aliceDb.collection("notifications").doc("notif_1").set({
      recipientId: "ALL_USERS",
      senderId: ALICE_UID,
      senderName: "দারুল উলুম মাদরাসা",
      title: "নতুন খেদমতের নিয়োগ বিজ্ঞপ্তি",
      message: "দারুল উলুম মাদরাসায় সহকারী শিক্ষক পদে নিয়োগ বিজ্ঞপ্তি প্রকাশিত হয়েছে।",
      type: "NEW_CIRCULAR",
      referenceId: "post_1",
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Non-admin cannot create ANNOUNCEMENT notification
  await assertFails(
    aliceDb.collection("notifications").doc("notif_fake_admin").set({
      recipientId: "ALL_USERS",
      senderId: ALICE_UID,
      senderName: "Alice",
      title: "Fake Announcement",
      message: "Test",
      type: "ANNOUNCEMENT",
      referenceId: "admin_announcement",
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Admin can create ANNOUNCEMENT notification
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "fyslahm28@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("notifications").doc("notif_admin_1").set({
      recipientId: "ALL_USERS",
      senderId: ADMIN_UID,
      senderName: "নাজিমে আলা",
      title: "জরুরি ঘোষণা",
      message: "নতুন শিক্ষাবর্ষের খেদমত বিজ্ঞপ্তি যাচাই চলছে।",
      type: "ANNOUNCEMENT",
      referenceId: "admin_announcement",
      isRead: false,
      createdAt: nowDate(),
    })
  );

  // Bob can read broadcast notifications and mark notif_1 as read in his own notification_reads
  await assertSucceeds(
    bobDb.collection("notifications").where("recipientId", "==", "ALL_USERS").get()
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("notification_reads").doc("notif_1").set({
      userId: BOB_UID,
      notificationId: "notif_1",
      readAt: nowDate(),
    })
  );
});
