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

function extractXmlConfig(xmlPath) {
  try {
    if (!fs.existsSync(xmlPath)) return {};
    const xml = fs.readFileSync(xmlPath, "utf8");
    const projectIdMatch = xml.match(/<string name="project_id">([^<]+)<\/string>/);
    const dbIdMatch = xml.match(/<string name="fire(?:base|store)_database_id">([^<]+)<\/string>/);
    return {
      projectId: projectIdMatch ? projectIdMatch[1].trim() : undefined,
      databaseId: dbIdMatch ? dbIdMatch[1].trim() : undefined,
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
    const decoded = Buffer.from(trimmed, "base64").toString("utf8");
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
            reject(
              new Error(
                `HTTP ${res.statusCode} for ${method} ${url.pathname}: ${
                  typeof parsed === "string" ? parsed : JSON.stringify(parsed)
                }`
              )
            );
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

function buildFirestoreDocumentFields(release) {
  const timestampIso = new Date(Date.now() - 1000).toISOString();
  return {
    fields: {
      releaseId: { stringValue: String(release.releaseId) },
      versionCode: { integerValue: String(release.versionCode) },
      versionName: { stringValue: String(release.versionName) },
      apkDownloadUrl: { stringValue: String(release.apkDownloadUrl) },
      releaseNotes: { stringValue: String(release.releaseNotes) },
      commitSha: { stringValue: String(release.commitSha || "") },
      githubReleaseTag: { stringValue: String(release.githubReleaseTag || "") },
      isMandatory: { booleanValue: Boolean(release.isMandatory) },
      publishedBy: { stringValue: String(release.publishedBy) },
      publishedAt: { timestampValue: timestampIso },
      updatedAt: { timestampValue: timestampIso },
    },
  };
}

async function writeReleaseDoc({
  baseOrigin,
  projectId,
  databaseId,
  docId,
  release,
  bearerToken,
}) {
  const encodedDb = encodeURIComponent(databaseId);
  const url = `${baseOrigin}/v1/projects/${encodeURIComponent(
    projectId
  )}/databases/${encodedDb}/documents/app_releases/${encodeURIComponent(docId)}`;

  const body = buildFirestoreDocumentFields({
    ...release,
    releaseId: docId,
  });

  await httpRequestJson(url, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${bearerToken}`,
    },
    body,
  });
}

async function main() {
  const args = parseArgs(process.argv);
  const xmlDefaults = extractXmlConfig("app/src/main/res/values/firebase_applet_config.xml");

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
  const publishedBy =
    args["published-by"] ||
    process.env.GITHUB_ACTOR ||
    "GitHub Actions Release Pipeline";

  if (!apkDownloadUrl || !/^https?:\/\//i.test(apkDownloadUrl)) {
    throw new Error(
      `Invalid or missing --apk-url / RELEASE_APK_URL: "${apkDownloadUrl}". Must start with http:// or https://`
    );
  }
  if (!Number.isInteger(versionCode) || versionCode < 1) {
    throw new Error(`Invalid --version-code: ${versionCode}`);
  }

  const emulatorHost =
    args["emulator-host"] || process.env.FIRESTORE_EMULATOR_HOST || "";

  const serviceAccount = parseServiceAccount(
    args["service-account"] || process.env.FIREBASE_SERVICE_ACCOUNT || ""
  );

  const projectId =
    args["project-id"] ||
    process.env.FIREBASE_PROJECT_ID ||
    (serviceAccount && serviceAccount.project_id) ||
    xmlDefaults.projectId ||
    "gen-lang-client-0048059944";

  const configuredDatabaseId =
    args["database-id"] ||
    process.env.FIRESTORE_DATABASE_ID ||
    xmlDefaults.databaseId ||
    "(default)";

  const releaseData = {
    versionCode,
    versionName,
    apkDownloadUrl,
    releaseNotes,
    commitSha,
    githubReleaseTag,
    isMandatory,
    publishedBy,
  };

  if (emulatorHost) {
    const baseOrigin = emulatorHost.startsWith("http")
      ? emulatorHost
      : `http://${emulatorHost}`;
    // In Firestore Emulator, "owner" Bearer token bypasses security rules just like a service account
    const bearerToken = "owner";
    const dbTargets = Array.from(new Set([configuredDatabaseId, "(default)"]));
    for (const dbId of dbTargets) {
      await writeReleaseDoc({
        baseOrigin,
        projectId,
        databaseId: dbId,
        docId: "latest",
        release: releaseData,
        bearerToken,
      });
      await writeReleaseDoc({
        baseOrigin,
        projectId,
        databaseId: dbId,
        docId: `v_${versionCode}`,
        release: releaseData,
        bearerToken,
      });
    }
    console.log(
      JSON.stringify({
        status: "SUCCESS",
        mode: "EMULATOR",
        emulatorHost,
        projectId,
        databases: dbTargets,
        versionCode,
        versionName,
        apkDownloadUrl,
      })
    );
    return;
  }

  if (!serviceAccount) {
    console.warn(
      "⚠️ FIREBASE_SERVICE_ACCOUNT is not set and FIRESTORE_EMULATOR_HOST is not active. Skipping remote Firestore publish."
    );
    process.exit(0);
  }

  const accessToken = await mintGoogleAccessToken(serviceAccount);
  const baseOrigin = "https://firestore.googleapis.com";
  const dbTargets = Array.from(new Set([configuredDatabaseId, "(default)"]));

  const updatedDatabases = [];
  for (const dbId of dbTargets) {
    try {
      await writeReleaseDoc({
        baseOrigin,
        projectId,
        databaseId: dbId,
        docId: "latest",
        release: releaseData,
        bearerToken: accessToken,
      });
      await writeReleaseDoc({
        baseOrigin,
        projectId,
        databaseId: dbId,
        docId: `v_${versionCode}`,
        release: releaseData,
        bearerToken: accessToken,
      });
      updatedDatabases.push(dbId);
    } catch (err) {
      console.warn(`Could not write to database "${dbId}": ${err.message}`);
    }
  }

  if (updatedDatabases.length === 0) {
    throw new Error(
      `Failed to publish release metadata to any Firestore database in project ${projectId}`
    );
  }

  console.log(
    JSON.stringify({
      status: "SUCCESS",
      mode: "PRODUCTION",
      projectId,
      databases: updatedDatabases,
      versionCode,
      versionName,
      apkDownloadUrl,
    })
  );
}

main().catch((err) => {
  console.error("❌ Failed to publish release metadata to Firebase:", err.message);
  process.exit(1);
});
