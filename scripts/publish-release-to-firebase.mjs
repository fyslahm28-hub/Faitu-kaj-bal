#!/usr/bin/env node
/**
 * কওমি কর্মসংস্থান বাংলাদেশ — GitHub Actions to Firebase Auto-Update Publisher
 *
 * Publishes the newly built APK release metadata (versionCode, versionName,
 * apkDownloadUrl, releaseNotes, commitSha, githubReleaseTag) to Firebase Firestore
 * at `/app_releases/latest` (and `/app_releases/v_<versionCode>`).
 *
 * Supports:
 * 1. Production Firebase Firestore via Google Service Account (`FIREBASE_SERVICE_ACCOUNT`)
 * 2. Local / CI Firestore Emulator via `FIRESTORE_EMULATOR_HOST` (or `--emulator-host`)
 */

import crypto from "node:crypto";
import fs from "node:fs";
import http from "node:http";
import https from "node:https";

function parseArgs(argv) {
  const args = {};
  for (let i = 2; i < argv.length; i++) {
    const token = argv[i];
    if (token.startsWith("--")) {
      const eqIdx = token.indexOf("=");
      if (eqIdx !== -1) {
        const key = token.slice(2, eqIdx);
        args[key] = token.slice(eqIdx + 1);
      } else {
        const key = token.slice(2);
        const next = argv[i + 1];
        if (next && !next.startsWith("--")) {
          args[key] = next;
          i++;
        } else {
          args[key] = "true";
        }
      }
    }
  }
  return args;
}

/**
 * Extracts project_id and firestore_database_id from Android XML resource files
 * (such as `app/src/main/res/values/firebase_applet_config.xml`), properly handling
 * additional XML attributes like `translatable="false"`.
 */
export function extractXmlConfig(xmlPath) {
  try {
    if (!fs.existsSync(xmlPath)) return {};
    const xml = fs.readFileSync(xmlPath, "utf8");
    const projectIdMatch = xml.match(
      /<string\b[^>]*\bname=["']project_id["'][^>]*>([^<]+)<\/string>/i
    );
    const dbIdMatch = xml.match(
      /<string\b[^>]*\bname=["']fire(?:base|store)_database_id["'][^>]*>([^<]+)<\/string>/i
    );
    return {
      projectId: projectIdMatch ? projectIdMatch[1].trim() : undefined,
      databaseId: dbIdMatch ? dbIdMatch[1].trim() : undefined,
    };
  } catch {
    return {};
  }
}

/**
 * Extracts the configured Firestore database ID from `firebase.json` if present.
 */
export function extractFirebaseJsonConfig(firebaseJsonPath) {
  try {
    if (!fs.existsSync(firebaseJsonPath)) return {};
    const parsed = JSON.parse(fs.readFileSync(firebaseJsonPath, "utf8"));
    if (Array.isArray(parsed.firestore) && parsed.firestore.length > 0) {
      const db = parsed.firestore[0]?.database;
      if (typeof db === "string" && db.trim()) {
        return { databaseId: db.trim() };
      }
    } else if (parsed.firestore && typeof parsed.firestore.database === "string") {
      const db = parsed.firestore.database.trim();
      if (db) return { databaseId: db };
    }
    return {};
  } catch {
    return {};
  }
}

/**
 * Extracts `project_info.project_id` from `app/google-services.json` if present.
 */
export function extractGoogleServicesConfig(googleServicesPath) {
  try {
    if (!fs.existsSync(googleServicesPath)) return {};
    const parsed = JSON.parse(fs.readFileSync(googleServicesPath, "utf8"));
    const projectId = parsed?.project_info?.project_id;
    return {
      projectId: typeof projectId === "string" && projectId.trim() ? projectId.trim() : undefined,
    };
  } catch {
    return {};
  }
}

function base64UrlEncode(input) {
  const buf = Buffer.isBuffer(input) ? input : Buffer.from(String(input), "utf8");
  return buf
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function parseServiceAccount(rawInput) {
  if (!rawInput || !rawInput.trim()) return null;
  const trimmed = rawInput.trim();
  try {
    if (trimmed.startsWith("{")) {
      return JSON.parse(trimmed);
    }
    if (fs.existsSync(trimmed)) {
      return JSON.parse(fs.readFileSync(trimmed, "utf8"));
    }
    const decoded = Buffer.from(trimmed.replace(/\s+/g, ""), "base64").toString("utf8");
    if (decoded.trim().startsWith("{")) {
      return JSON.parse(decoded);
    }
  } catch (err) {
    throw new Error(`Failed to parse FIREBASE_SERVICE_ACCOUNT JSON: ${err.message}`);
  }
  return null;
}

function httpRequestJson(urlString, { method = "GET", headers = {}, body = null } = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlString);
    const client = url.protocol === "https:" ? https : http;
    const payload = body ? (typeof body === "string" ? body : JSON.stringify(body)) : null;

    const req = client.request(
      url,
      {
        method,
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          ...(payload ? { "Content-Length": Buffer.byteLength(payload) } : {}),
          ...headers,
        },
      },
      (res) => {
        const chunks = [];
        res.on("data", (c) => chunks.push(c));
        res.on("end", () => {
          const text = Buffer.concat(chunks).toString("utf8");
          let parsed = text;
          try {
            parsed = text ? JSON.parse(text) : {};
          } catch {
            // keep raw text
          }
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve({ statusCode: res.statusCode, data: parsed });
          } else {
            const err = new Error(
              `HTTP ${res.statusCode} for ${method} ${url.pathname}: ${
                typeof parsed === "string" ? parsed : JSON.stringify(parsed)
              }`
            );
            err.statusCode = res.statusCode;
            err.responseBody = parsed;
            reject(err);
          }
        });
      }
    );

    req.on("error", reject);
    if (payload) req.write(payload);
    req.end();
  });
}

async function mintGoogleAccessToken(serviceAccount) {
  const nowSec = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const claimSet = {
    iss: serviceAccount.client_email,
    sub: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/cloud-platform",
    aud: serviceAccount.token_uri || "https://oauth2.googleapis.com/token",
    iat: nowSec,
    exp: nowSec + 3600,
  };

  const unsignedJwt = `${base64UrlEncode(JSON.stringify(header))}.${base64UrlEncode(
    JSON.stringify(claimSet)
  )}`;
  const signer = crypto.createSign("RSA-SHA256");
  signer.update(unsignedJwt);
  signer.end();
  const signature = signer.sign(serviceAccount.private_key);
  const signedJwt = `${unsignedJwt}.${base64UrlEncode(signature)}`;

  const formBody = new URLSearchParams({
    grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
    assertion: signedJwt,
  }).toString();

  const tokenUrl = serviceAccount.token_uri || "https://oauth2.googleapis.com/token";
  const res = await httpRequestJson(tokenUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: formBody,
  });

  if (!res.data || !res.data.access_token) {
    throw new Error("OAuth2 response did not contain an access_token");
  }
  return res.data.access_token;
}

async function fetchServerTimestampIso(baseOrigin = "https://firestore.googleapis.com") {
  if (!baseOrigin.startsWith("https://")) {
    return new Date(Date.now() - 1000).toISOString();
  }
  return new Promise((resolve) => {
    const req = https.request(
      baseOrigin,
      { method: "HEAD", timeout: 5000 },
      (res) => {
        const dateHeader = res.headers?.date;
        if (dateHeader) {
          const parsedMs = Date.parse(dateHeader);
          if (!Number.isNaN(parsedMs)) {
            resolve(new Date(parsedMs - 2000).toISOString());
            return;
          }
        }
        resolve(new Date(Date.now() - 1000).toISOString());
      }
    );
    req.on("error", () => resolve(new Date(Date.now() - 1000).toISOString()));
    req.on("timeout", () => {
      req.destroy();
      resolve(new Date(Date.now() - 1000).toISOString());
    });
    req.end();
  });
}

function buildFirestoreDocumentFields(release, customTimestampIso = null) {
  const timestampIso = customTimestampIso || new Date(Date.now() - 1000).toISOString();
  const status = String(release.releaseStatus || "PUBLISHED").toUpperCase();
  const isPublished = release.isPublished !== undefined ? Boolean(release.isPublished) : status === "PUBLISHED";
  return {
    fields: {
      releaseId: { stringValue: String(release.releaseId) },
      versionCode: { integerValue: String(release.versionCode) },
      versionName: { stringValue: String(release.versionName) },
      apkDownloadUrl: { stringValue: String(release.apkDownloadUrl) },
      apkSha256: { stringValue: String(release.apkSha256 || "") },
      apkFileSizeBytes: { integerValue: String(release.apkFileSizeBytes || 0) },
      releaseNotes: { stringValue: String(release.releaseNotes) },
      commitSha: { stringValue: String(release.commitSha || "") },
      githubReleaseTag: { stringValue: String(release.githubReleaseTag || "") },
      isMandatory: { booleanValue: Boolean(release.isMandatory) },
      isPublished: { booleanValue: isPublished },
      releaseStatus: { stringValue: status },
      publishedBy: { stringValue: String(release.publishedBy) },
      publishedAt: { timestampValue: timestampIso },
      updatedAt: { timestampValue: timestampIso },
    },
  };
}

/**
 * Safely merges release metadata fields into `/app_releases/{docId}` using
 * `updateMask.fieldPaths` so any other existing fields on the document are preserved.
 */
async function writeReleaseDoc({
  baseOrigin,
  projectId,
  databaseId,
  docId,
  release,
  bearerToken,
  timestampIso = null,
}) {
  const encodedProject = encodeURIComponent(projectId);
  const encodedDb = encodeURIComponent(databaseId);
  const encodedDoc = encodeURIComponent(docId);

  const body = buildFirestoreDocumentFields(
    {
      ...release,
      releaseId: docId,
    },
    timestampIso
  );

  const fieldNames = Object.keys(body.fields);
  const maskQuery = fieldNames
    .map((field) => `updateMask.fieldPaths=${encodeURIComponent(field)}`)
    .join("&");

  const url = `${baseOrigin}/v1/projects/${encodedProject}/databases/${encodedDb}/documents/app_releases/${encodedDoc}?${maskQuery}`;

  const headers = bearerToken ? { Authorization: `Bearer ${bearerToken}` } : {};

  await httpRequestJson(url, {
    method: "PATCH",
    headers,
    body,
  });
}

/**
 * Reads back `/app_releases/latest` from Firestore to verify that the release metadata
 * was genuinely persisted in the target database.
 */
async function verifyReleaseDoc({
  baseOrigin,
  projectId,
  databaseId,
  docId,
  expectedRelease,
  bearerToken,
}) {
  const encodedProject = encodeURIComponent(projectId);
  const encodedDb = encodeURIComponent(databaseId);
  const encodedDoc = encodeURIComponent(docId);
  const url = `${baseOrigin}/v1/projects/${encodedProject}/databases/${encodedDb}/documents/app_releases/${encodedDoc}`;

  const headers = bearerToken ? { Authorization: `Bearer ${bearerToken}` } : {};

  const res = await httpRequestJson(url, {
    method: "GET",
    headers,
  });

  const fields = res.data?.fields;
  if (!fields) {
    throw new Error(
      `Verification GET succeeded but returned no fields for /app_releases/${docId} in database "${databaseId}" (project "${projectId}").`
    );
  }

  const actualVersionCode = parseInt(fields.versionCode?.integerValue ?? "0", 10);
  const actualVersionName = fields.versionName?.stringValue ?? "";
  const actualApkUrl = fields.apkDownloadUrl?.stringValue ?? "";

  if (
    actualVersionCode !== expectedRelease.versionCode ||
    actualVersionName !== expectedRelease.versionName ||
    actualApkUrl !== expectedRelease.apkDownloadUrl
  ) {
    throw new Error(
      `Verification mismatch on /app_releases/${docId} in database "${databaseId}" (project "${projectId}"): ` +
        `expected versionCode=${expectedRelease.versionCode}, versionName="${expectedRelease.versionName}", apkDownloadUrl="${expectedRelease.apkDownloadUrl}", ` +
        `but found versionCode=${actualVersionCode}, versionName="${actualVersionName}", apkDownloadUrl="${actualApkUrl}".`
    );
  }

  return {
    versionCode: actualVersionCode,
    versionName: actualVersionName,
    apkDownloadUrl: actualApkUrl,
  };
}

export function resolveProjectAndDatabaseConfig({
  args = {},
  env = process.env,
  serviceAccount = null,
  xmlPath = "app/src/main/res/values/firebase_applet_config.xml",
  firebaseJsonPath = "firebase.json",
  googleServicesPath = "app/google-services.json",
} = {}) {
  const xmlDefaults = extractXmlConfig(xmlPath);
  const firebaseJsonDefaults = extractFirebaseJsonConfig(firebaseJsonPath);
  const googleServicesDefaults = extractGoogleServicesConfig(googleServicesPath);

  const appConfiguredProjectId =
    googleServicesDefaults.projectId || xmlDefaults.projectId || "gen-lang-client-0048059944";

  const projectId =
    (args["project-id"] && args["project-id"].trim()) ||
    (env.FIREBASE_PROJECT_ID && env.FIREBASE_PROJECT_ID.trim()) ||
    appConfiguredProjectId ||
    (serviceAccount && serviceAccount.project_id);

  const databaseId =
    (args["database-id"] && args["database-id"].trim()) ||
    (env.FIRESTORE_DATABASE_ID && env.FIRESTORE_DATABASE_ID.trim()) ||
    xmlDefaults.databaseId ||
    firebaseJsonDefaults.databaseId ||
    "(default)";

  const serviceAccountProjectMismatch =
    Boolean(
      serviceAccount &&
        serviceAccount.project_id &&
        projectId &&
        serviceAccount.project_id !== projectId
    );

  return {
    projectId,
    databaseId,
    appConfiguredProjectId,
    serviceAccountProjectId: serviceAccount?.project_id || null,
    serviceAccountProjectMismatch,
    sources: {
      xmlDatabaseId: xmlDefaults.databaseId || null,
      firebaseJsonDatabaseId: firebaseJsonDefaults.databaseId || null,
      googleServicesProjectId: googleServicesDefaults.projectId || null,
    },
  };
}

async function main() {
  const args = parseArgs(process.argv);

  const versionCode = parseInt(
    args["version-code"] || process.env.RELEASE_VERSION_CODE || "2",
    10
  );
  const versionName =
    args["version-name"] || process.env.RELEASE_VERSION_NAME || `1.0.${versionCode}`;
  const apkDownloadUrl =
    args["apk-url"] ||
    process.env.RELEASE_APK_URL ||
    "";
  const apkSha256 =
    (args["apk-sha256"] || process.env.RELEASE_APK_SHA256 || "").trim().toLowerCase();
  const apkFileSizeBytes = parseInt(
    args["apk-size-bytes"] || process.env.RELEASE_APK_SIZE_BYTES || "0",
    10
  ) || 0;
  const releaseNotes =
    args["release-notes"] ||
    process.env.RELEASE_NOTES ||
    `কওমি কর্মসংস্থান বাংলাদেশ নতুন সংস্করণ (${versionName}) প্রকাশিত হয়েছে।`;
  const commitSha =
    args["commit-sha"] || process.env.GITHUB_SHA || "local-build";
  const githubReleaseTag =
    args["release-tag"] || process.env.RELEASE_TAG || `v${versionName}`;
  const isMandatory =
    (args["mandatory"] || process.env.RELEASE_MANDATORY || "false") === "true";
  const releaseStatus =
    (args["release-status"] || process.env.RELEASE_STATUS || "PUBLISHED").trim().toUpperCase();
  const publishedBy =
    args["published-by"] ||
    process.env.GITHUB_ACTOR ||
    "GitHub Actions Release Pipeline";
  const requireServiceAccount =
    (args["require-service-account"] || process.env.REQUIRE_FIREBASE_SERVICE_ACCOUNT || "false") === "true";

  if (!apkDownloadUrl || !/^https?:\/\//i.test(apkDownloadUrl)) {
    throw new Error(
      `[Step: Validate Inputs] Invalid or missing --apk-url / RELEASE_APK_URL: "${apkDownloadUrl}". Must start with http:// or https://`
    );
  }
  if (!Number.isInteger(versionCode) || versionCode < 1) {
    throw new Error(`[Step: Validate Inputs] Invalid --version-code: ${versionCode}`);
  }

  const emulatorHost =
    args["emulator-host"] || process.env.FIRESTORE_EMULATOR_HOST || "";

  const serviceAccount = parseServiceAccount(
    args["service-account"] || process.env.FIREBASE_SERVICE_ACCOUNT || ""
  );

  const {
    projectId,
    databaseId,
    serviceAccountProjectId,
    serviceAccountProjectMismatch,
    sources,
  } = resolveProjectAndDatabaseConfig({ args, env: process.env, serviceAccount });

  console.log(
    JSON.stringify({
      info: "Resolved Firebase target configuration",
      projectId,
      databaseId,
      sources,
      emulatorMode: Boolean(emulatorHost),
    })
  );

  if (serviceAccountProjectMismatch) {
    console.warn(
      `⚠️ Warning: FIREBASE_SERVICE_ACCOUNT belongs to project "${serviceAccountProjectId}", ` +
        `while the Android app is configured for project "${projectId}". ` +
        `Targeting app project "${projectId}" and database "${databaseId}".`
    );
  }

  const releaseData = {
    versionCode,
    versionName,
    apkDownloadUrl,
    apkSha256,
    apkFileSizeBytes,
    releaseNotes,
    commitSha,
    githubReleaseTag,
    isMandatory,
    isPublished: releaseStatus === "PUBLISHED",
    releaseStatus,
    publishedBy,
  };

  if (emulatorHost) {
    const baseOrigin = emulatorHost.startsWith("http")
      ? emulatorHost
      : `http://${emulatorHost}`;
    // In Firestore Emulator, "owner" Bearer token bypasses security rules just like a service account
    const bearerToken = "owner";
    let activeStep = `PATCH /app_releases/latest (emulator database: "${databaseId}")`;
    try {
      await writeReleaseDoc({
        baseOrigin,
        projectId,
        databaseId,
        docId: "latest",
        release: releaseData,
        bearerToken,
      });
      activeStep = `PATCH /app_releases/v_${versionCode} (emulator database: "${databaseId}")`;
      await writeReleaseDoc({
        baseOrigin,
        projectId,
        databaseId,
        docId: `v_${versionCode}`,
        release: releaseData,
        bearerToken,
      });
      activeStep = `GET /app_releases/latest verification (emulator database: "${databaseId}")`;
      const verified = await verifyReleaseDoc({
        baseOrigin,
        projectId,
        databaseId,
        docId: "latest",
        expectedRelease: releaseData,
        bearerToken,
      });
      console.log(
        JSON.stringify({
          status: "SUCCESS",
          mode: "EMULATOR",
          emulatorHost,
          projectId,
          databaseId,
          verifiedDocument: `/app_releases/latest`,
          ...verified,
        })
      );
      return;
    } catch (err) {
      throw new Error(
        `[Step: ${activeStep}] Failed for projectId="${projectId}", databaseId="${databaseId}": ${err.message}`
      );
    }
  }

  let accessToken = null;
  if (!serviceAccount) {
    const msg =
      `[Step: Load Service Account] FIREBASE_SERVICE_ACCOUNT secret is missing or empty ` +
      `(target projectId="${projectId}", databaseId="${databaseId}", document="/app_releases/latest").`;
    if (requireServiceAccount) {
      throw new Error(
        `${msg} Configure a valid Firebase Service Account JSON in GitHub repository Secrets -> FIREBASE_SERVICE_ACCOUNT.`
      );
    }
    console.warn(`ℹ️ ${msg} Proceeding with verified Firestore REST publish.`);
  } else {
    try {
      accessToken = await mintGoogleAccessToken(serviceAccount);
    } catch (err) {
      if (requireServiceAccount) {
        throw new Error(
          `[Step: OAuth2 Access Token Exchange] Failed for projectId="${projectId}", databaseId="${databaseId}": ${err.message}`
        );
      }
      console.warn(
        `⚠️ [Step: OAuth2 Access Token Exchange] ${err.message}. Falling back to verified Firestore REST publish.`
      );
    }
  }

  const baseOrigin = "https://firestore.googleapis.com";
  const serverTimestampIso = await fetchServerTimestampIso(baseOrigin);
  let currentStep = `PATCH /app_releases/latest`;

  try {
    await writeReleaseDoc({
      baseOrigin,
      projectId,
      databaseId,
      docId: "latest",
      release: releaseData,
      bearerToken: accessToken,
      timestampIso: serverTimestampIso,
    });

    currentStep = `PATCH /app_releases/v_${versionCode}`;
    await writeReleaseDoc({
      baseOrigin,
      projectId,
      databaseId,
      docId: `v_${versionCode}`,
      release: releaseData,
      bearerToken: accessToken,
      timestampIso: serverTimestampIso,
    });

    currentStep = `GET /app_releases/latest (Read-After-Write Verification)`;
    const verified = await verifyReleaseDoc({
      baseOrigin,
      projectId,
      databaseId,
      docId: "latest",
      expectedRelease: releaseData,
      bearerToken: accessToken,
    });

    console.log(
      JSON.stringify({
        status: "SUCCESS",
        mode: "PRODUCTION",
        projectId,
        databaseId,
        verifiedDocuments: ["/app_releases/latest", `/app_releases/v_${versionCode}`],
        ...verified,
      })
    );
  } catch (err) {
    const statusHint =
      err.statusCode === 404
        ? ` Ensure that Firestore database "${databaseId}" exists in Firebase project "${projectId}".`
        : err.statusCode === 403
        ? ` Ensure the service account in FIREBASE_SERVICE_ACCOUNT belongs to project "${projectId}" and has the "Cloud Datastore User" or "Firebase Admin" IAM role.`
        : "";
    throw new Error(
      `[Step: ${currentStep}] Firestore update failed for projectId="${projectId}", databaseId="${databaseId}": ${err.message}.${statusHint}`
    );
  }
}

const isDirectRun =
  process.argv[1] &&
  import.meta.url.endsWith(process.argv[1].replace(/\\/g, "/").split("/").pop());

if (isDirectRun) {
  main().catch((err) => {
    console.error("❌ Failed to publish release metadata to Firebase:", err.message);
    process.exit(1);
  });
}
