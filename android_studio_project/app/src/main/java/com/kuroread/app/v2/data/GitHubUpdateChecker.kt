package com.kuroread.app.v2.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseUpdateInfo(
    val latestVersionName: String = "1.1.1",
    val releaseTitle: String = "Pembaruan Versi",
    val releaseNotes: String = "",
    val downloadUrl: String = "https://github.com/Epithap/kuro-readV2/releases",
    val isUpdateAvailable: Boolean = false
)

object GitHubUpdateChecker {
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/Epithap/kuro-readV2/releases/latest"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) KuroReadV2/1.1.1"

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.1.1"
        } catch (_: Exception) {
            "1.1.1"
        }
    }

    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.trim().removePrefix("v").split(".")
        val clean2 = v2.trim().removePrefix("v").split(".")
        val length = maxOf(clean1.size, clean2.size)

        for (i in 0 until length) {
            val n1 = clean1.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            val n2 = clean2.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            if (n1 != n2) {
                return n1.compareTo(n2)
            }
        }
        return 0
    }

    suspend fun checkForUpdates(context: Context): AppReleaseUpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersionName(context)
        try {
            val conn = (URL(GITHUB_LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode != 200) {
                return@withContext AppReleaseUpdateInfo(
                    latestVersionName = currentVersion,
                    isUpdateAvailable = false
                )
            }

            val rawJson = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(rawJson)

            val rawTag = root.optString("tag_name", "").ifBlank { "v$currentVersion" }
            val latestVersionName = rawTag.trim().removePrefix("v")
            val releaseTitle = root.optString("name", "KuroRead v$latestVersionName Release").ifBlank { "KuroRead v$latestVersionName Release" }
            val releaseNotes = root.optString("body", "Pembaruan versi $latestVersionName telah rilis di GitHub.").ifBlank {
                "Pembaruan versi $latestVersionName telah rilis di GitHub."
            }

            var downloadUrl = root.optString("html_url", "https://github.com/Epithap/kuro-readV2/releases")
            val assetsArr = root.optJSONArray("assets")
            if (assetsArr != null && assetsArr.length() > 0) {
                for (a in 0 until assetsArr.length()) {
                    val assetObj = assetsArr.optJSONObject(a) ?: continue
                    val name = assetObj.optString("name", "").lowercase()
                    val url = assetObj.optString("browser_download_url", "")
                    if (name.endsWith(".apk") && url.isNotBlank()) {
                        downloadUrl = url
                        break
                    }
                }
            }

            val isNewer = compareVersions(latestVersionName, currentVersion) > 0

            AppReleaseUpdateInfo(
                latestVersionName = latestVersionName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                isUpdateAvailable = isNewer
            )
        } catch (e: Exception) {
            AppReleaseUpdateInfo(
                latestVersionName = currentVersion,
                isUpdateAvailable = false
            )
        }
    }
}
