package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.data.model.AppReleaseInfo
import kotlinx.coroutines.Dispatchers
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
        } catch (_: Exception) {
            InstalledAppVersion(
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME
            )
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
     * Downloads the APK from [apkDownloadUrl] (following GitHub Release 301/302 redirects)
     * into the app's `cacheDir/updates/` directory and reports progress (0..100).
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

        return withContext(Dispatchers.IO) {
            runCatching {
            onProgress(5)
            var currentUrl = releaseInfo.apkDownloadUrl.trim()
            var redirectCount = 0
            var connection: HttpURLConnection? = null

            while (redirectCount < 6) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
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
                        val buffer = ByteArray(8 * 1024)
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
                    throw IllegalStateException("ডাউনলোডকৃত APK ফাইলটি খালি।")
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
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
     * Launches Android's standard approved package installation dialog for [apkFile]
     * using [FileProvider] and `application/vnd.android.package-archive`.
     */
    fun launchApkInstallation(
        context: Context,
        apkFile: File,
        fallbackDownloadUrl: String = ""
    ): Result<Intent> = runCatching {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = try {
            FileProvider.getUriForFile(context, authority, apkFile)
        } catch (_: Exception) {
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
        } catch (err: Exception) {
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
