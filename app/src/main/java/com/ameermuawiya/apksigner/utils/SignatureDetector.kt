package com.ameermuawiya.apksigner.utils

import android.content.Context
import android.content.pm.PackageManager
import com.android.apksig.ApkVerifier
import com.ameermuawiya.apksigner.data.model.AppSignDetails
import java.io.File

/**
 * Utility for verifying and detecting existing signature schemes (V1-V4) and package information on APK files.
 */
object SignatureDetector {

    /**
     * Inspects target APK file using ApkVerifier to detect existing signature schemes.
     */
    fun inspectApk(context: Context, apkFile: File): AppSignDetails {
        var v1 = false
        var v2 = false
        var v3 = false
        var v4 = false

        try {
            val verifier = ApkVerifier.Builder(apkFile).build()
            val result = verifier.verify()
            v1 = result.isVerifiedUsingV1Scheme
            v2 = result.isVerifiedUsingV2Scheme
            v3 = result.isVerifiedUsingV3Scheme
            v4 = result.isVerifiedUsingV4Scheme
        } catch (ignored: Exception) {}

        var appName = apkFile.name
        var packageName = "Unknown Package"
        var versionName = "1.0"
        var icon: android.graphics.drawable.Drawable? = null

        try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_ACTIVITIES)
            if (info != null) {
                val appInfo = info.applicationInfo
                if (appInfo != null) {
                    appInfo.sourceDir = apkFile.absolutePath
                    appInfo.publicSourceDir = apkFile.absolutePath
                    appName = pm.getApplicationLabel(appInfo).toString()
                    icon = pm.getApplicationIcon(appInfo)
                }
                packageName = info.packageName ?: packageName
                versionName = info.versionName ?: "1.0"
            }
        } catch (ignored: Exception) {}

        val sizeFormatted = formatFileSize(apkFile.length())
        val isSplit = apkFile.extension.lowercase() in listOf("apks", "xapk", "zip")

        return AppSignDetails(
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            fileSizeFormatted = sizeFormatted,
            inputFile = apkFile,
            isSplitPackage = isSplit,
            existingV1 = v1,
            existingV2 = v2,
            existingV3 = v3,
            existingV4 = v4,
            icon = icon
        )
    }

    /**
     * Formats bytes into readable size string.
     */
    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
