package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.R
import com.example.data.model.AppReleaseInfo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class InstalledAppVersion(
    val versionCode: Int,
    val versionName: String
)

object AppUpdateManagerHelper {

    private const val PREFS_NAME = "qawmi_app_update_prefs"
    private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
    private const val KEY_DISMISSED_TIMESTAMP_MS = "dismissed_timestamp_ms"
    private const val DISMISS_COOLDOWN_MS = 6 * 60 * 60 * 1000L // 6 hours cooldown for "পরে করুন"

    @Volatile
    var lastDownloadedApkFile: File? = null

    @Volatile
    var lastLaunchedInstallIntent: Intent? = null

    /**
     * Optional override for testing or custom APK download stream injection.
     */
    @Volatile
    var customApkFetcher: (suspend (url: String, targetFile: File, onProgress: (Int) -> Unit) -> Result<File>)? = null

    fun getInstalledVersion(context: Context): InstalledAppVersion {
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = PackageInfoCompat.getLongVersionCode(pkgInfo).toInt().let {
                if (it > 0) it else BuildConfig.VERSION_CODE
            }
            val name = pkgInfo.versionName?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME
            InstalledAppVersion(versionCode = code, versionName = name)
        } catch (_: Throwable) {
            InstalledAppVersion(
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME
            )
        }
    }

    /**
     * Records when the user taps "পরে করুন" so the update prompt is snoozed for [DISMISS_COOLDOWN_MS].
     */
    fun saveDismissedUpdatePrompt(context: Context?, versionCode: Int) {
        if (context == null || versionCode <= 0) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_DISMISSED_VERSION_CODE, versionCode)
                .putLong(KEY_DISMISSED_TIMESTAMP_MS, System.currentTimeMillis())
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun clearDismissedUpdatePrompt(context: Context?) {
        if (context == null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_DISMISSED_VERSION_CODE)
                .remove(KEY_DISMISSED_TIMESTAMP_MS)
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun getActiveCooldownDismissedVersionCode(context: Context?): Int? {
        if (context == null) return null
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val code = prefs.getInt(KEY_DISMISSED_VERSION_CODE, -1)
            val ts = prefs.getLong(KEY_DISMISSED_TIMESTAMP_MS, 0L)
            if (code > 0 && (System.currentTimeMillis() - ts) in 0..DISMISS_COOLDOWN_MS) {
                code
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Compares remote Firebase release metadata against the currently installed version
     * and any session-dismissed version ("পরে করুন").
     */
    fun isUpdateAvailable(
        installedVersionCode: Int,
        installedVersionName: String,
        remoteRelease: AppReleaseInfo?,
        dismissedVersionCode: Int? = null
    ): Boolean {
        if (remoteRelease == null) return false
        if (!remoteRelease.isPublished) return false
        if (remoteRelease.apkDownloadUrl.isBlank()) return false
        if (!remoteRelease.apkDownloadUrl.startsWith("https://", ignoreCase = true) &&
            !remoteRelease.apkDownloadUrl.startsWith("http://", ignoreCase = true)
        ) {
            return false
        }

        val isNewerCode = remoteRelease.versionCode > installedVersionCode
        val isNewerSemantic = remoteRelease.versionCode == installedVersionCode &&
            compareSemanticVersion(remoteRelease.versionName, installedVersionName) > 0

        if (!isNewerCode && !isNewerSemantic) return false

        if (!remoteRelease.isMandatory &&
            dismissedVersionCode != null &&
            dismissedVersionCode >= remoteRelease.versionCode
        ) {
            return false
        }
        return true
    }

    fun compareSemanticVersion(v1: String, v2: String): Int {
        val parts1 = v1.trim().removePrefix("v").removePrefix("V")
            .split(".", "-", "_")
            .mapNotNull { it.toIntOrNull() }
        val parts2 = v2.trim().removePrefix("v").removePrefix("V")
            .split(".", "-", "_")
            .mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    /**
     * Checks whether a cached APK file is already downloaded and valid for [releaseInfo].
     */
    fun isReusableCachedApk(context: Context, apkFile: File, releaseInfo: AppReleaseInfo): Boolean {
        if (!apkFile.exists() || apkFile.length() < 64L) return false
        return try {
            val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (archiveInfo != null) {
                val archiveCode = PackageInfoCompat.getLongVersionCode(archiveInfo).toInt()
                val pkgMatches = archiveInfo.packageName.isNullOrBlank() ||
                    archiveInfo.packageName == context.packageName
                pkgMatches && (archiveCode <= 0 || archiveCode == releaseInfo.versionCode)
            } else {
                // Verify ZIP/APK magic header (PK\x03\x04)
                val header = ByteArray(4)
                val readCount = apkFile.inputStream().use { it.read(header) }
                readCount == 4 &&
                    header[0] == 'P'.code.toByte() &&
                    header[1] == 'K'.code.toByte()
            }
        } catch (_: Throwable) {
            apkFile.length() > 1024L
        }
    }

    /**
     * Safely fetches `/app_releases/latest` from Firestore on startup (including on AuthScreen)
     * without ever throwing or blocking the app if offline or uninitialized.
     */
    suspend fun fetchLatestReleaseSafely(context: Context): AppReleaseInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val dbId = try {
                    context.applicationContext.getString(R.string.firestore_database_id).trim()
                } catch (_: Throwable) {
                    ""
                }
                val db = if (dbId.isNotBlank() && dbId != "(default)") {
                    FirebaseFirestore.getInstance(dbId)
                } else {
                    FirebaseFirestore.getInstance()
                }
                val snap = db.collection("app_releases").document("latest").get().await()
                parseReleaseSnapshotSafely(snap)
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun parseReleaseSnapshotSafely(snap: DocumentSnapshot?): AppReleaseInfo? {
        if (snap == null || !snap.exists()) return null
        return try {
            val obj = runCatching {
                snap.toObject(AppReleaseInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull()
            val resolvedVersionCode = runCatching { snap.getLong("versionCode")?.toInt() }.getOrNull()
                ?: runCatching { snap.getString("versionCode")?.trim()?.toIntOrNull() }.getOrNull()
                ?: obj?.versionCode
                ?: 1
            val resolvedVersionName = runCatching { snap.getString("versionName")?.trim() }.getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: obj?.versionName?.takeIf { it.isNotBlank() }
                ?: "1.0"
            val resolvedApkUrl = runCatching { snap.getString("apkDownloadUrl")?.trim() }.getOrNull()
                ?: obj?.apkDownloadUrl?.trim()
                ?: ""
            if (resolvedApkUrl.isBlank()) return null
            val resolvedNotes = runCatching { snap.getString("releaseNotes") }.getOrNull()
                ?: obj?.releaseNotes
                ?: ""
            val resolvedSha = runCatching { snap.getString("commitSha") }.getOrNull()
                ?: obj?.commitSha
                ?: ""
            val resolvedTag = runCatching { snap.getString("githubReleaseTag") }.getOrNull()
                ?: obj?.githubReleaseTag
                ?: ""
            val resolvedMandatory = runCatching { snap.getBoolean("isMandatory") }.getOrNull()
                ?: obj?.isMandatory
                ?: false
            val resolvedPublished = runCatching { snap.getBoolean("isPublished") }.getOrNull()
                ?: obj?.isPublished
                ?: true
            val resolvedPublishedBy = runCatching { snap.getString("publishedBy") }.getOrNull()
                ?: obj?.publishedBy
                ?: "GitHub Actions CI"
            val resolvedPublishedAt = runCatching {
                snap.getTimestamp("publishedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull() ?: obj?.publishedAt
            val resolvedUpdatedAt = runCatching {
                snap.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.getOrNull() ?: obj?.updatedAt

            AppReleaseInfo(
                id = snap.id,
                releaseId = runCatching { snap.getString("releaseId") }.getOrNull()?.ifBlank { snap.id } ?: snap.id,
                versionCode = resolvedVersionCode,
                versionName = resolvedVersionName,
                apkDownloadUrl = resolvedApkUrl,
                releaseNotes = resolvedNotes,
                commitSha = resolvedSha,
                githubReleaseTag = resolvedTag,
                isMandatory = resolvedMandatory,
                isPublished = resolvedPublished,
                publishedBy = resolvedPublishedBy,
                publishedAt = resolvedPublishedAt,
                updatedAt = resolvedUpdatedAt
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Downloads the APK from [apkDownloadUrl] (following GitHub Release 301/302 redirects)
     * into the app's `cacheDir/updates/` directory and reports progress (0..100).
     * Reuses an already-downloaded valid APK for the same version if present.
     */
    suspend fun downloadApkWithProgress(
        context: Context,
        releaseInfo: AppReleaseInfo,
        onProgress: (Int) -> Unit
    ): Result<File> {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val safeVersion = releaseInfo.versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetFile = File(
            updatesDir,
            "Qawmi-Kormosangsthan-v${safeVersion}-${releaseInfo.versionCode}.apk"
        )

        customApkFetcher?.let { fetcher ->
            val res = fetcher(releaseInfo.apkDownloadUrl, targetFile, onProgress)
            res.getOrNull()?.let { lastDownloadedApkFile = it }
            return res
        }

        // Reuse existing valid APK in cache if already downloaded for this exact version
        if (isReusableCachedApk(context, targetFile, releaseInfo)) {
            onProgress(100)
            lastDownloadedApkFile = targetFile
            return Result.success(targetFile)
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                onProgress(5)
                var currentUrl = releaseInfo.apkDownloadUrl.trim()
                var redirectCount = 0
                var connection: HttpURLConnection? = null

                while (redirectCount < 8) {
                    val url = URL(currentUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 25_000
                        readTimeout = 45_000
                        requestMethod = "GET"
                        setRequestProperty(
                            "Accept",
                            "application/vnd.android.package-archive, application/octet-stream, */*"
                        )
                        setRequestProperty("User-Agent", "QawmiKormosangsthan-AutoUpdater/1.0 (Android)")
                    }
                    conn.connect()
                    val status = conn.responseCode
                    if (status in listOf(
                            HttpURLConnection.HTTP_MOVED_PERM,
                            HttpURLConnection.HTTP_MOVED_TEMP,
                            HttpURLConnection.HTTP_SEE_OTHER,
                            307,
                            308
                        )
                    ) {
                        val location = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (location.isNullOrBlank()) {
                            throw IllegalStateException("ডাউনলোড রিডাইরেক্ট লিংক পাওয়া যায়নি।")
                        }
                        currentUrl = URL(url, location).toString()
                        redirectCount++
                    } else {
                        connection = conn
                        break
                    }
                }

                val activeConnection = connection
                    ?: throw IllegalStateException("ডাউনলোড সার্ভারে সংযোগ স্থাপন করা যায়নি।")

                try {
                    val responseCode = activeConnection.responseCode
                    if (responseCode !in 200..299) {
                        throw IllegalStateException("APK ডাউনলোড ব্যর্থ হয়েছে (HTTP $responseCode)।")
                    }

                    val totalBytes = activeConnection.contentLengthLong
                    val tempFile = File(updatesDir, "${targetFile.name}.part")
                    if (tempFile.exists()) tempFile.delete()

                    activeConnection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var downloadedBytes = 0L
                            var bytesRead: Int
                            var lastReportedProgress = 5

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                if (totalBytes > 0L) {
                                    val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(5, 99)
                                    if (pct > lastReportedProgress) {
                                        lastReportedProgress = pct
                                        onProgress(pct)
                                    }
                                } else {
                                    val stepped = (lastReportedProgress + 5).coerceAtMost(95)
                                    if (stepped > lastReportedProgress) {
                                        lastReportedProgress = stepped
                                        onProgress(stepped)
                                    }
                                }
                            }
                            output.flush()
                        }
                    }

                    if (tempFile.length() <= 0L) {
                        tempFile.delete()
                        throw IllegalStateException("ডাউনলোডকৃত APK ফাইলটি খালি বা অসম্পূর্ণ।")
                    }

                    // Validate APK archive header ("PK")
                    val header = ByteArray(2)
                    val readHeaderBytes = tempFile.inputStream().use { it.read(header) }
                    if (readHeaderBytes < 2 || header[0] != 'P'.code.toByte() || header[1] != 'K'.code.toByte()) {
                        tempFile.delete()
                        throw IllegalStateException("ডাউনলোডকৃত ফাইলটি বৈধ Android APK নয়। অনুগ্রহ করে ডাউনলোড লিংক যাচাই করুন।")
                    }

                    if (targetFile.exists()) targetFile.delete()
                    if (!tempFile.renameTo(targetFile)) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }

                    // Clean up older cached APKs to save storage space
                    runCatching {
                        updatesDir.listFiles()?.forEach { f ->
                            if (f.name != targetFile.name) f.delete()
                        }
                    }

                    onProgress(100)
                    lastDownloadedApkFile = targetFile
                    targetFile
                } finally {
                    activeConnection.disconnect()
                }
            }
        }
    }

    /**
     * Checks whether the app is allowed to request package installation on Android 8.0+.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.packageManager.canRequestPackageInstalls()
            } catch (_: Throwable) {
                true
            }
        } else {
            true
        }
    }

    /**
     * Opens the Android system screen to grant "Install unknown apps" permission for this package.
     */
    fun openUnknownSourcesSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Launches Android's standard approved package installation dialog for [apkFile]
     * using [FileProvider] and `application/vnd.android.package-archive`.
     */
    fun launchApkInstallation(
        context: Context,
        apkFile: File,
        fallbackDownloadUrl: String = ""
    ): Result<Intent> = runCatching {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            throw IllegalStateException("ইনস্টল করার জন্য APK ফাইলটি পাওয়া যায়নি। পুনরায় ডাউনলোড করুন।")
        }

        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = try {
            FileProvider.getUriForFile(context, authority, apkFile)
        } catch (_: Throwable) {
            Uri.fromFile(apkFile)
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
        }

        lastLaunchedInstallIntent = installIntent
        try {
            context.startActivity(installIntent)
        } catch (err: Throwable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canRequestPackageInstalls(context)) {
                if (openUnknownSourcesSettings(context)) {
                    return@runCatching installIntent
                }
            }
            if (fallbackDownloadUrl.isNotBlank()) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackDownloadUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                lastLaunchedInstallIntent = browserIntent
                context.startActivity(browserIntent)
                return@runCatching browserIntent
            }
            throw err
        }
        installIntent
    }
}
