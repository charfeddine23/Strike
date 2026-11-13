package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long
)

sealed class AppUpdateState {
    data object Idle : AppUpdateState()
    data class Checking(val isAutoCheck: Boolean) : AppUpdateState()
    data class UpToDate(val currentVersion: String) : AppUpdateState()
    data class UpdateAvailable(val release: GitHubReleaseInfo) : AppUpdateState()
    data class Downloading(
        val release: GitHubReleaseInfo,
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : AppUpdateState()
    data class ReadyToInstall(val release: GitHubReleaseInfo, val apkFile: File) : AppUpdateState()
    data class Error(val messageAr: String, val fallbackUrl: String = AppUpdateManager.GITHUB_RELEASES_URL) : AppUpdateState()
}

/**
 * In-App Auto-Update & GitHub APK Downloader for https://github.com/charfeddine23/Strike
 */
object AppUpdateManager {

    const val GITHUB_OWNER = "charfeddine23"
    const val GITHUB_REPO = "Strike"
    const val GITHUB_REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val GITHUB_RELEASES_URL = "$GITHUB_REPO_URL/releases"
    const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val GITHUB_RELEASES_LIST_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=1"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    @Volatile
    private var activeDownloadJob: Job? = null

    fun resetToIdle() {
        _state.value = AppUpdateState.Idle
    }

    fun cancelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        val current = _state.value
        if (current is AppUpdateState.Downloading) {
            _state.value = AppUpdateState.UpdateAvailable(current.release)
        }
    }

    /**
     * Compares semantic version strings (e.g. "v1.0.25" vs "1.0.0").
     * Returns true iff [remoteVersion] is strictly newer than [currentVersion].
     */
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remoteParts = normalizeVersionParts(remoteVersion)
        val currentParts = normalizeVersionParts(currentVersion)
        if (remoteParts.isEmpty()) return false
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    internal fun normalizeVersionParts(raw: String): List<Int> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        if (cleaned.isBlank()) return emptyList()
        return cleaned.split(".").map { part ->
            part.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    /**
     * Parses a GitHub Release JSON object (from /releases/latest) or JSON array (from /releases?per_page=1).
     */
    fun parseReleaseJson(rawJson: String): GitHubReleaseInfo? {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return null
        val obj: JSONObject = when {
            trimmed.startsWith("[") -> {
                val arr = JSONArray(trimmed)
                if (arr.length() == 0) return null
                arr.getJSONObject(0)
            }
            trimmed.startsWith("{") -> JSONObject(trimmed)
            else -> return null
        }

        val tagName = obj.optString("tag_name", "").trim()
        if (tagName.isBlank()) return null

        val versionName = tagName.removePrefix("v").removePrefix("V")
        val releaseName = obj.optString("name", tagName).ifBlank { tagName }
        val releaseNotes = obj.optString("body", "").trim()
        val htmlUrl = obj.optString("html_url", GITHUB_RELEASES_URL).ifBlank { GITHUB_RELEASES_URL }

        var apkUrl: String? = null
        var apkFileName: String? = null
        var apkSize = 0L

        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "").takeIf { it.isNotBlank() }
                    apkFileName = name
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        return GitHubReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            releaseName = releaseName,
            releaseNotes = releaseNotes,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            apkFileName = apkFileName,
            apkSizeBytes = apkSize
        )
    }

    suspend fun checkForUpdates(
        currentVersion: String,
        isAutoCheck: Boolean = false
    ): AppUpdateState = withContext(Dispatchers.IO) {
        if (_state.value is AppUpdateState.Downloading) return@withContext _state.value
        _state.value = AppUpdateState.Checking(isAutoCheck)
        try {
            val latestJson = fetchUrlString(GITHUB_LATEST_RELEASE_API)
            val json = latestJson ?: fetchUrlString(GITHUB_RELEASES_LIST_API)
            if (json == null) {
                val err = if (isAutoCheck) {
                    AppUpdateState.Idle
                } else {
                    AppUpdateState.Error("تعذر الاتصال بخادم GitHub أو لا توجد إصدارات منشورة بعد")
                }
                _state.value = err
                return@withContext err
            }

            val release = parseReleaseJson(json)
            if (release == null) {
                val next = if (isAutoCheck) AppUpdateState.Idle else AppUpdateState.UpToDate(currentVersion)
                _state.value = next
                return@withContext next
            }

            val next = if (isNewerVersion(release.versionName, currentVersion)) {
                AppUpdateState.UpdateAvailable(release)
            } else {
                if (isAutoCheck) AppUpdateState.Idle else AppUpdateState.UpToDate(currentVersion)
            }
            _state.value = next
            next
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val err = if (isAutoCheck) {
                AppUpdateState.Idle
            } else {
                AppUpdateState.Error("تعذر التحقق من التحديثات: تحقق من اتصال الإنترنت")
            }
            _state.value = err
            err
        }
    }

    suspend fun downloadUpdateInApp(
        context: Context,
        release: GitHubReleaseInfo
    ): AppUpdateState = withContext(Dispatchers.IO) {
        val downloadUrl = release.apkDownloadUrl
        if (downloadUrl.isNullOrBlank()) {
            val err = AppUpdateState.Error(
                messageAr = "لا يوجد ملف APK مرفق في هذا الإصدار؛ يمكنك الفتح من GitHub",
                fallbackUrl = release.htmlUrl
            )
            _state.value = err
            return@withContext err
        }

        val job = currentCoroutineContext()[Job]
        activeDownloadJob = job

        try {
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
                // Clean old cached APKs before downloading
                listFiles()?.forEach { old -> old.delete() }
            }

            val safeName = (release.apkFileName ?: "StrikePulse-v${release.versionName}.apk")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val partFile = File(updatesDir, "$safeName.part")
            val finalFile = File(updatesDir, safeName)

            val request = Request.Builder()
                .url(downloadUrl)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "StrikePulse-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = AppUpdateState.Error(
                        messageAr = "فشل تحميل التحديث من الخادم (HTTP ${response.code})",
                        fallbackUrl = release.htmlUrl
                    )
                    _state.value = err
                    return@withContext err
                }

                val body = response.body ?: run {
                    val err = AppUpdateState.Error("ملف التحديث فارغ أو غير صالح", release.htmlUrl)
                    _state.value = err
                    return@withContext err
                }

                val totalBytes = body.contentLength().let { len ->
                    if (len > 0L) len else release.apkSizeBytes
                }

                _state.value = AppUpdateState.Downloading(
                    release = release,
                    progress = if (totalBytes > 0L) 0f else -1f,
                    bytesDownloaded = 0L,
                    totalBytes = totalBytes
                )

                val buffer = ByteArray(16 * 1024)
                var downloaded = 0L
                var lastEmitTime = 0L

                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 80L || downloaded == totalBytes) {
                                lastEmitTime = now
                                val progress = if (totalBytes > 0L) {
                                    (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    -1f
                                }
                                _state.value = AppUpdateState.Downloading(
                                    release = release,
                                    progress = progress,
                                    bytesDownloaded = downloaded,
                                    totalBytes = totalBytes
                                )
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            val ready = AppUpdateState.ReadyToInstall(release, finalFile)
            _state.value = ready
            ready
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val err = AppUpdateState.Error(
                messageAr = "انقطع التحميل: تحقق من الاتصال وحاول مجدداً",
                fallbackUrl = release.htmlUrl
            )
            _state.value = err
            err
        } finally {
            activeDownloadJob = null
        }
    }

    /**
     * Triggers the Android PackageInstaller directly from the FileProvider URI.
     * Returns true if the installer intent was launched, false if user must first allow unknown sources.
     */
    fun installDownloadedApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return false
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
        return true
    }

    fun openUrlExternally(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "—"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            String.format(Locale.US, "%.0f KB", kb)
        }
    }

    private fun fetchUrlString(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "StrikePulse-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }
}
