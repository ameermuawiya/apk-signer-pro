package com.ameermuawiya.apksigner.utils

import android.util.Log
import com.ameermuawiya.apksigner.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result state returned when checking remote repository for version updates.
 */
sealed class UpdateCheckResult {
    data class Available(val updateInfo: AppUpdateInfo) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Silent, secure utility to inspect remote GitHub releases for newer application versions.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_REPO_LATEST_API = "https://api.github.com/repos/ameermuawiya/apk-signer-pro/releases/latest"
    private const val GITHUB_REPO_ALL_API = "https://api.github.com/repos/ameermuawiya/apk-signer-pro/releases"

    /**
     * Checks remote GitHub repository for the latest release and evaluates against current version.
     */
    suspend fun checkLatestUpdateDetailed(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val jsonObject = fetchReleaseJson(GITHUB_REPO_LATEST_API) ?: fetchFirstReleaseFromJsonArray(GITHUB_REPO_ALL_API)
        if (jsonObject == null) {
            return@withContext UpdateCheckResult.Error("Could not retrieve release metadata from GitHub repository.")
        }

        try {
            val tagName = jsonObject.optString("tag_name", "").removePrefix("v").trim()
            val releaseName = jsonObject.optString("name", "v$tagName").ifBlank { "v$tagName" }
            val releaseBody = jsonObject.optString("body", "Bug fixes and performance improvements.")
            val htmlUrl = jsonObject.optString("html_url", "https://github.com/ameermuawiya/apk-signer-pro/releases")
            val publishedAt = jsonObject.optString("published_at", "")

            if (tagName.isNotBlank() && isNewerVersion(remote = tagName, current = currentVersion)) {
                UpdateCheckResult.Available(
                    AppUpdateInfo(
                        versionName = tagName,
                        releaseTitle = releaseName,
                        releaseNotes = releaseBody,
                        downloadUrl = htmlUrl,
                        publishedAt = publishedAt
                    )
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing release JSON data", e)
            UpdateCheckResult.Error(e.message ?: "Error parsing release information.")
        }
    }

    /**
     * Convenience method returning AppUpdateInfo if newer version exists or null otherwise.
     */
    suspend fun checkLatestUpdate(currentVersion: String): AppUpdateInfo? {
        return when (val result = checkLatestUpdateDetailed(currentVersion)) {
            is UpdateCheckResult.Available -> result.updateInfo
            else -> null
        }
    }

    /**
     * Performs HTTP GET request to specified endpoint and parses single JSON object.
     */
    private fun fetchReleaseJson(endpoint: String): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "APK-Signer-Pro-Android")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                JSONObject(response)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch from endpoint: $endpoint", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fetches array of releases from GitHub and returns the first available release object.
     */
    private fun fetchFirstReleaseFromJsonArray(endpoint: String): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "APK-Signer-Pro-Android")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) jsonArray.getJSONObject(0) else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch releases array from: $endpoint", e)
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
