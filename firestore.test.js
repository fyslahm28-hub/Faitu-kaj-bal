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
