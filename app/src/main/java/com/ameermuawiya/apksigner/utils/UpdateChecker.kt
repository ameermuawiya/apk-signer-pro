package com.ameermuawiya.apksigner.utils

import android.util.Log
import com.ameermuawiya.apksigner.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Silent, secure utility to inspect remote GitHub releases for newer application versions.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_REPO_API = "https://api.github.com/repos/ameermuawiya/apksigner-m3-expressive/releases/latest"

    /**
     * Silently fetches latest GitHub release details asynchronously and compares against current version.
     */
    suspend fun checkLatestUpdate(currentVersion: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_REPO_API)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "APK-Signer-Pro-Android")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val json = JSONObject(response)

            val tagName = json.optString("tag_name", "").removePrefix("v").trim()
            val releaseName = json.optString("name", "v$tagName").ifBlank { "v$tagName" }
            val releaseBody = json.optString("body", "Bug fixes and performance improvements.")
            val htmlUrl = json.optString("html_url", "https://github.com/ameermuawiya/apksigner-m3-expressive/releases")
            val publishedAt = json.optString("published_at", "")

            if (isNewerVersion(remote = tagName, current = currentVersion)) {
                AppUpdateInfo(
                    versionName = tagName,
                    releaseTitle = releaseName,
                    releaseNotes = releaseBody,
                    downloadUrl = htmlUrl,
                    publishedAt = publishedAt
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to silently check for remote updates", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares semver version strings to determine if remote release is newer than current installed version.
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val cleanRemote = remote.removePrefix("v").trim()
        val cleanCurrent = current.removePrefix("v").trim()
        if (cleanRemote == cleanCurrent) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
