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

test("Authenticated user: cannot spoof authorId when creating feed_post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("feed_posts").doc("post_spoof").set({
      authorId: BOB_UID,
      authorName: "Spoof",
      postType: "JOB_CIRCULAR",
      title: "দারুল উলুম",
      district: "কুমিল্লা",
      upazilaOrLocation: "কুমিল্লা",
      designation: "মুহতামিম",
      qualification: "দাওরায়ে হাদীস",
      salaryOrExperience: "আলোচনা সাপেক্ষ",
      deadlineOrPreferredDistricts: "৩০ অক্টোবর",
      description: "বিস্তারিত",
      contactNumber: "01700000000",
      madrasaId: "",
      isVerifiedMadrasa: false,
      visibility: "PUBLIC",
      approvalStatus: "APPROVED",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );
});

test("Direct messages: only sender and receiver can read, third party fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      senderName: "মাওলানা আব্দুল্লাহ",
      receiverId: BOB_UID,
      receiverName: "দারুল উলুম মাদরাসা",
      subject: "সহকারী শিক্ষক পদের ব্যাপারে",
      body: "আসসালামু আলাইকুম ওয়া রাহমাতুল্লাহ, আমি খেদমতে আগ্রহী।",
      isRead: false,
      createdAt: nowDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("messages").where("receiverId", "==", BOB_UID).get()
  );

  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  await assertFails(
    charlieDb.collection("messages").doc("msg_1").get()
  );
});

test("Khedmot submissions: applicant can submit, recipient can update status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("khedmot_submissions").doc("sub_1").set({
      postId: "post_1",
      postTitle: "দারুল উলুম কাওমি মাদরাসা",
      postDesignation: "সহকারী শিক্ষক",
      applicantId: ALICE_UID,
      applicantName: "মাওলানা রফিকুল ইসলাম",
      applicantQualification: "দাওরায়ে হাদীস",
      applicantExperience: "৫ বছর",
      applicantPhone: "01800000000",
      coverNote: "আমি উক্ত খেদমতের জন্য আবেদন করছি।",
      recipientId: BOB_UID,
      status: "PENDING",
      createdAt: nowDate(),
      updatedAt: nowDate(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("khedmot_submissions").doc("sub_1").update({
      status: "SHORTLISTED",
      updatedAt: nowDate(),
    })
  );
});
