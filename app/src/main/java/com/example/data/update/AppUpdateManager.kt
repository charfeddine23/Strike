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
import java.net.URLEncoder
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
    data class Error(
        val messageAr: String,
        val fallbackUrl: String = AppUpdateManager.GITHUB_RELEASES_URL,
        val messageEn: String = messageAr
    ) : AppUpdateState() {
        fun getLocalizedMessage(lang: String): String = if (lang == "ar") messageAr else messageEn
    }
}

internal data class FetchResult(
    val code: Int,
    val body: String?,
    val finalUrl: String? = null
) {
    val isSuccessful: Boolean get() = code in 200..299
}

/**
 * In-App Auto-Update & GitHub APK Downloader for https://github.com/charfeddine23/Strike
 */
object AppUpdateManager {

    const val GITHUB_OWNER = "charfeddine23"
    const val GITHUB_REPO = "Strike"
    const val GITHUB_REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val GITHUB_RELEASES_URL = "$GITHUB_REPO_URL/releases"
    const val GITHUB_RELEASES_ATOM_URL = "$GITHUB_REPO_URL/releases.atom"
    const val GITHUB_LATEST_RELEASE_WEB_URL = "$GITHUB_REPO_URL/releases/latest"
    const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val GITHUB_RELEASES_LIST_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=10"
    const val GITHUB_TAGS_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/tags?per_page=10"
    const val GITHUB_CONTENTS_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/contents"

    private val dottedVersionRegex = Regex("""\d+(?:\.\d+)+""")
    private val singleVersionRegex = Regex("""\d+""")

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
     * Compares semantic version strings (e.g. "v1.0.25", "release-1.0.2", "Strike_v2.0" vs "1.0.0").
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
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()

        // Prefer multi-segment semantic version anywhere in the string (e.g., "release-1.0.2", "Strike-v2.1.0-rc1")
        val dottedMatch = dottedVersionRegex.find(trimmed)?.value
        if (dottedMatch != null) {
            return dottedMatch.split(".").mapNotNull { it.toIntOrNull() }
        }

        // Fallback to single integer version (e.g., "v2", "build-5")
        val singleMatch = singleVersionRegex.find(trimmed)?.value
        if (singleMatch != null) {
            return listOfNotNull(singleMatch.toIntOrNull())
        }

        return emptyList()
    }

    internal fun extractVersionString(vararg candidates: String?): String {
        for (candidate in candidates) {
            val text = candidate?.trim().orEmpty()
            if (text.isEmpty()) continue
            dottedVersionRegex.find(text)?.value?.let { return it }
        }
        for (candidate in candidates) {
            val text = candidate?.trim().orEmpty()
            if (text.isEmpty()) continue
            singleVersionRegex.find(text)?.value?.let { return it }
        }
        return candidates.firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            ?.removePrefix("v")
            ?.removePrefix("V")
            .orEmpty()
    }

    /**
     * Parses a GitHub Release JSON object (from /releases/latest) or JSON array (from /releases).
     * When given an array, skips drafts and selects the highest semantic version (breaking ties by APK asset presence).
     */
    fun parseReleaseJson(rawJson: String): GitHubReleaseInfo? {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return null
        return when {
            trimmed.startsWith("[") -> {
                val arr = JSONArray(trimmed)
                val candidates = mutableListOf<GitHubReleaseInfo>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    if (obj.optBoolean("draft", false)) continue
                    parseReleaseObject(obj)?.let { candidates.add(it) }
                }
                pickBestReleaseCandidate(candidates)
            }
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                if (obj.optBoolean("draft", false)) return null
                parseReleaseObject(obj)
            }
            else -> null
        }
    }

    private fun parseReleaseObject(obj: JSONObject): GitHubReleaseInfo? {
        val tagName = obj.optString("tag_name", "").trim()
        if (tagName.isBlank()) return null

        val releaseName = obj.optString("name", tagName).ifBlank { tagName }
        val releaseNotes = obj.optString("body", "").trim()
        val htmlUrl = obj.optString("html_url", GITHUB_RELEASES_URL).ifBlank { GITHUB_RELEASES_URL }

        var apkUrl: String? = null
        var apkFileName: String? = null
        var apkSize = 0L

        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            val apkAssets = mutableListOf<Triple<String, String, Long>>()
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val contentType = asset.optString("content_type", "")
                val dlUrl = asset.optString("browser_download_url", "").trim()
                val isApk = name.endsWith(".apk", ignoreCase = true) ||
                    contentType.equals("application/vnd.android.package-archive", ignoreCase = true) ||
                    dlUrl.substringBefore("?").endsWith(".apk", ignoreCase = true)
                if (isApk && dlUrl.isNotBlank()) {
                    val resolvedName = name.ifBlank { dlUrl.substringAfterLast("/").substringBefore("?") }
                    apkAssets.add(Triple(resolvedName, dlUrl, asset.optLong("size", 0L)))
                }
            }
            // Prefer non-debug release APK when multiple APKs exist
            val bestAsset = apkAssets.firstOrNull { !it.first.contains("debug", ignoreCase = true) }
                ?: apkAssets.firstOrNull()
            if (bestAsset != null) {
                apkFileName = bestAsset.first
                apkUrl = bestAsset.second
                apkSize = bestAsset.third
            }
        }

        val versionName = extractVersionString(tagName, releaseName, apkFileName)

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

    /**
     * Parses GitHub's Atom XML feed (`https://github.com/<owner>/<repo>/releases.atom`)
     * which does not share api.github.com's unauthenticated IP rate limit.
     */
    internal fun parseReleasesAtom(atomXml: String): List<GitHubReleaseInfo> {
        if (atomXml.isBlank() || !atomXml.contains("<entry")) return emptyList()
        val entryRegex = Regex("""<entry>([\s\S]*?)</entry>""")
        val linkRegex = Regex("""<link[^>]*href=["']([^"']+)["']""")
        val titleRegex = Regex("""<title[^>]*>([\s\S]*?)</title>""")
        val results = mutableListOf<GitHubReleaseInfo>()

        for (match in entryRegex.findAll(atomXml)) {
            val entryBlock = match.groupValues[1]
            val href = linkRegex.find(entryBlock)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val tagFromHref = href.substringAfter("/releases/tag/", "").substringBefore("?").substringBefore("#").trim()
            val decodedTag = try {
                java.net.URLDecoder.decode(tagFromHref, "UTF-8")
            } catch (_: Exception) {
                tagFromHref
            }
            if (decodedTag.isBlank()) continue

            val rawTitle = titleRegex.find(entryBlock)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val releaseName = decodeBasicXmlEntities(rawTitle).ifBlank { decodedTag }
            val versionName = extractVersionString(decodedTag, releaseName)
            val htmlUrl = href.ifBlank { "$GITHUB_RELEASES_URL/tag/$tagFromHref" }

            results.add(
                GitHubReleaseInfo(
                    tagName = decodedTag,
                    versionName = versionName,
                    releaseName = releaseName,
                    releaseNotes = "",
                    htmlUrl = htmlUrl,
                    apkDownloadUrl = null,
                    apkFileName = null,
                    apkSizeBytes = 0L
                )
            )
        }
        return results
    }

    /**
     * Parses APK download links from `https://github.com/<owner>/<repo>/releases/expanded_assets/<tag>`.
     */
    internal fun parseExpandedAssetsForApk(html: String): Pair<String, String>? {
        if (html.isBlank()) return null
        val hrefRegex = Regex(
            """href\s*=\s*["']([^"']*?/releases/download/[^"']+\.apk(?:\?[^"']*)?)["']""",
            RegexOption.IGNORE_CASE
        )
        val candidates = hrefRegex.findAll(html)
            .map { it.groupValues[1].trim() }
            .distinct()
            .map { rawHref ->
                val fullUrl = if (rawHref.startsWith("http", ignoreCase = true)) {
                    rawHref
                } else {
                    "https://github.com$rawHref"
                }
                val fileName = fullUrl.substringBefore("?").substringAfterLast("/")
                fileName to fullUrl
            }
            .toList()
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { !it.first.contains("debug", ignoreCase = true) }
            ?: candidates.first()
    }

    /**
     * Parses GitHub Tags JSON (`https://api.github.com/repos/<owner>/<repo>/tags`).
     */
    internal fun parseTagsJson(rawJson: String): GitHubReleaseInfo? {
        val trimmed = rawJson.trim()
        if (!trimmed.startsWith("[")) return null
        val arr = JSONArray(trimmed)
        val candidates = mutableListOf<GitHubReleaseInfo>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val tagName = obj.optString("name", "").trim()
            if (tagName.isBlank()) continue
            val versionName = extractVersionString(tagName)
            if (normalizeVersionParts(versionName).isEmpty()) continue
            val encodedTag = encodePathSegment(tagName)
            candidates.add(
                GitHubReleaseInfo(
                    tagName = tagName,
                    versionName = versionName,
                    releaseName = tagName,
                    releaseNotes = "",
                    htmlUrl = "$GITHUB_RELEASES_URL/tag/$encodedTag",
                    apkDownloadUrl = null,
                    apkFileName = null,
                    apkSizeBytes = 0L
                )
            )
        }
        return pickBestReleaseCandidate(candidates)
    }

    /**
     * Parses repository root contents JSON (`https://api.github.com/repos/<owner>/<repo>/contents`)
     * in case an APK file was committed directly to the repository branch.
     */
    internal fun parseRepoContentsForApk(rawJson: String): GitHubReleaseInfo? {
        val trimmed = rawJson.trim()
        if (!trimmed.startsWith("[")) return null
        val arr = JSONArray(trimmed)
        val candidates = mutableListOf<GitHubReleaseInfo>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val type = obj.optString("type", "")
            val name = obj.optString("name", "").trim()
            if (type == "file" && name.endsWith(".apk", ignoreCase = true)) {
                val dlUrl = obj.optString("download_url", "").trim().takeIf { it.isNotBlank() } ?: continue
                val htmlUrl = obj.optString("html_url", GITHUB_REPO_URL).ifBlank { GITHUB_REPO_URL }
                val size = obj.optLong("size", 0L)
                val versionName = extractVersionString(name)
                if (normalizeVersionParts(versionName).isEmpty()) continue
                candidates.add(
                    GitHubReleaseInfo(
                        tagName = "v$versionName",
                        versionName = versionName,
                        releaseName = name,
                        releaseNotes = "",
                        htmlUrl = htmlUrl,
                        apkDownloadUrl = dlUrl,
                        apkFileName = name,
                        apkSizeBytes = size
                    )
                )
            }
        }
        return pickBestReleaseCandidate(candidates)
    }

    private fun pickBestReleaseCandidate(candidates: List<GitHubReleaseInfo>): GitHubReleaseInfo? {
        if (candidates.isEmpty()) return null
        var best = candidates.first()
        for (candidate in candidates.drop(1)) {
            if (isNewerVersion(candidate.versionName, best.versionName)) {
                best = candidate
            } else if (!isNewerVersion(best.versionName, candidate.versionName)) {
                // Equal version: prefer candidate that includes an APK download URL
                if (best.apkDownloadUrl.isNullOrBlank() && !candidate.apkDownloadUrl.isNullOrBlank()) {
                    best = candidate
                }
            }
        }
        return best
    }

    private fun decodeBasicXmlEntities(text: String): String =
        text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

    private fun encodePathSegment(segment: String): String =
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")

    suspend fun checkForUpdates(
        currentVersion: String,
        isAutoCheck: Boolean = false
    ): AppUpdateState = withContext(Dispatchers.IO) {
        val currentState = _state.value
        if (currentState is AppUpdateState.Checking ||
            currentState is AppUpdateState.Downloading ||
            currentState is AppUpdateState.ReadyToInstall ||
            (isAutoCheck && currentState is AppUpdateState.UpdateAvailable)
        ) {
            return@withContext currentState
        }
        _state.value = AppUpdateState.Checking(isAutoCheck)
        try {
            var sawRateLimit = false
            var sawNetworkFailure = false
            var discoveredAnyEmptySource = false

            // Tier 1: GitHub Releases API (/releases/latest + /releases?per_page=10 for pre-releases/multi-releases)
            val latestRes = fetchUrlResult(GITHUB_LATEST_RELEASE_API, accept = "application/vnd.github+json")
            if (latestRes == null) sawNetworkFailure = true
            else if (latestRes.code == 403 || latestRes.code == 429) sawRateLimit = true

            var bestRelease = latestRes?.takeIf { it.isSuccessful }?.body?.let { parseReleaseJson(it) }

            if (bestRelease == null || bestRelease.apkDownloadUrl.isNullOrBlank()) {
                val listRes = fetchUrlResult(GITHUB_RELEASES_LIST_API, accept = "application/vnd.github+json")
                if (listRes == null) {
                    sawNetworkFailure = true
                } else if (listRes.code == 403 || listRes.code == 429) {
                    sawRateLimit = true
                } else if (listRes.isSuccessful) {
                    val listBody = listRes.body.orEmpty().trim()
                    if (listBody == "[]") {
                        discoveredAnyEmptySource = true
                    } else {
                        val listBest = parseReleaseJson(listBody)
                        if (listBest != null) {
                            bestRelease = pickBestReleaseCandidate(listOfNotNull(bestRelease, listBest))
                        }
                    }
                }
            }

            // Tier 2: Unthrottled GitHub Web Releases Atom Feed & /releases/latest Redirect
            if (bestRelease == null) {
                val atomRes = fetchUrlResult(
                    GITHUB_RELEASES_ATOM_URL,
                    accept = "application/atom+xml,application/xml,text/xml,*/*"
                )
                if (atomRes == null) {
                    sawNetworkFailure = true
                } else if (atomRes.isSuccessful && !atomRes.body.isNullOrBlank()) {
                    val atomCandidates = parseReleasesAtom(atomRes.body)
                    if (atomCandidates.isEmpty()) {
                        discoveredAnyEmptySource = true
                    } else {
                        bestRelease = pickBestReleaseCandidate(atomCandidates)
                    }
                }
            }

            if (bestRelease == null) {
                val webLatestRes = fetchUrlResult(
                    GITHUB_LATEST_RELEASE_WEB_URL,
                    accept = "text/html,application/xhtml+xml,*/*"
                )
                val finalUrl = webLatestRes?.finalUrl.orEmpty()
                val tagFromRedirect = finalUrl.substringAfter("/releases/tag/", "")
                    .substringBefore("?")
                    .substringBefore("#")
                    .trim()
                if (tagFromRedirect.isNotBlank()) {
                    val decodedTag = try {
                        java.net.URLDecoder.decode(tagFromRedirect, "UTF-8")
                    } catch (_: Exception) {
                        tagFromRedirect
                    }
                    bestRelease = GitHubReleaseInfo(
                        tagName = decodedTag,
                        versionName = extractVersionString(decodedTag),
                        releaseName = decodedTag,
                        releaseNotes = "",
                        htmlUrl = finalUrl,
                        apkDownloadUrl = null,
                        apkFileName = null,
                        apkSizeBytes = 0L
                    )
                }
            }

            // Tier 3: GitHub Tags API & Repo Contents APK Fallback (if repository uses tags or direct APK commits)
            if (bestRelease == null) {
                val tagsRes = fetchUrlResult(GITHUB_TAGS_API, accept = "application/vnd.github+json")
                if (tagsRes?.isSuccessful == true && !tagsRes.body.isNullOrBlank()) {
                    val tagBest = parseTagsJson(tagsRes.body)
                    if (tagBest != null) {
                        bestRelease = tagBest
                    } else if (tagsRes.body.trim() == "[]") {
                        discoveredAnyEmptySource = true
                    }
                }
            }

            if (bestRelease == null) {
                val contentsRes = fetchUrlResult(GITHUB_CONTENTS_API, accept = "application/vnd.github+json")
                if (contentsRes?.isSuccessful == true && !contentsRes.body.isNullOrBlank()) {
                    bestRelease = parseRepoContentsForApk(contentsRes.body)
                }
            }

            // Enrich release with APK download URL from expanded_assets if API skipped assets or came from Atom/Redirect/Tag
            if (bestRelease != null && bestRelease.apkDownloadUrl.isNullOrBlank()) {
                val encodedTag = encodePathSegment(bestRelease.tagName)
                val expandedUrl = "$GITHUB_RELEASES_URL/expanded_assets/$encodedTag"
                val expandedRes = fetchUrlResult(expandedUrl, accept = "text/html,*/*")
                if (expandedRes?.isSuccessful == true && !expandedRes.body.isNullOrBlank()) {
                    val apkPair = parseExpandedAssetsForApk(expandedRes.body)
                    if (apkPair != null) {
                        bestRelease = bestRelease.copy(
                            apkFileName = apkPair.first,
                            apkDownloadUrl = apkPair.second
                        )
                    }
                }
            }

            val next: AppUpdateState = when {
                bestRelease != null -> {
                    if (isNewerVersion(bestRelease.versionName, currentVersion)) {
                        AppUpdateState.UpdateAvailable(bestRelease)
                    } else {
                        if (isAutoCheck) AppUpdateState.Idle else AppUpdateState.UpToDate(currentVersion)
                    }
                }
                discoveredAnyEmptySource && !sawNetworkFailure -> {
                    if (isAutoCheck) {
                        AppUpdateState.Idle
                    } else {
                        AppUpdateState.Error(
                            messageAr = "لا توجد إصدارات أو ملفات APK منشورة في مستودع GitHub بعد",
                            fallbackUrl = GITHUB_REPO_URL,
                            messageEn = "No releases or APK packages have been published on GitHub yet"
                        )
                    }
                }
                sawRateLimit -> {
                    if (isAutoCheck) {
                        AppUpdateState.Idle
                    } else {
                        AppUpdateState.Error(
                            messageAr = "تم تجاوز حد طلبات GitHub مؤقتاً؛ يمكنك فتح رابط التحميل مباشرة",
                            fallbackUrl = GITHUB_RELEASES_URL,
                            messageEn = "GitHub rate limit temporarily exceeded; open the download link directly"
                        )
                    }
                }
                else -> {
                    if (isAutoCheck) {
                        AppUpdateState.Idle
                    } else {
                        AppUpdateState.Error(
                            messageAr = "تعذر العثور على نسخة في GitHub؛ تأكد من اتصال الإنترنت أو أن المستودع عام",
                            fallbackUrl = GITHUB_REPO_URL,
                            messageEn = "Could not find a release on GitHub; check your connection or verify the repository is public"
                        )
                    }
                }
            }

            _state.value = next
            next
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            val err = if (isAutoCheck) {
                AppUpdateState.Idle
            } else {
                AppUpdateState.Error(
                    messageAr = "تعذر التحقق من التحديثات: تحقق من اتصال الإنترنت",
                    fallbackUrl = GITHUB_REPO_URL,
                    messageEn = "Unable to check for updates: please verify your internet connection"
                )
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
                fallbackUrl = release.htmlUrl,
                messageEn = "No APK asset attached to this release; you can open GitHub instead"
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
                .header("User-Agent", "StrikePulse-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = AppUpdateState.Error(
                        messageAr = "فشل تحميل التحديث من الخادم (HTTP ${response.code})",
                        fallbackUrl = release.htmlUrl,
                        messageEn = "Failed to download update from server (HTTP ${response.code})"
                    )
                    _state.value = err
                    return@withContext err
                }

                val body = response.body ?: run {
                    val err = AppUpdateState.Error(
                        messageAr = "ملف التحديث فارغ أو غير صالح",
                        fallbackUrl = release.htmlUrl,
                        messageEn = "Update package is empty or invalid"
                    )
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
        } catch (_: Exception) {
            val err = AppUpdateState.Error(
                messageAr = "انقطع التحميل: تحقق من الاتصال وحاول مجدداً",
                fallbackUrl = release.htmlUrl,
                messageEn = "Download interrupted: check your connection and try again"
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
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // No external browser/handler available
        }
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

    private fun fetchUrlResult(
        url: String,
        accept: String = "application/vnd.github+json"
    ): FetchResult? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", accept)
                .header("User-Agent", "StrikePulse-Android/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                FetchResult(
                    code = response.code,
                    body = if (response.isSuccessful) response.body?.string() else null,
                    finalUrl = response.request.url.toString()
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
