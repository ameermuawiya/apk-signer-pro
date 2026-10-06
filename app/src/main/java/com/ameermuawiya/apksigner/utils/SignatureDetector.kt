package com.ameermuawiya.apksigner.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.android.apksig.ApkVerifier
import com.ameermuawiya.apksigner.data.model.AppSignDetails
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Utility for verifying signature schemes and extracting package metadata from APK and APKS archives.
 */
object SignatureDetector {

    /**
     * Inspects target APK or split bundle using ApkVerifier and extracts signatures and package metadata.
     */
    fun inspectApk(context: Context, apkFile: File): AppSignDetails {
        val ext = apkFile.extension.lowercase(Locale.getDefault())
        val isSplit = ext in listOf("apks", "xapk", "apkm", "zip")

        return if (isSplit) {
            inspectSplitBundle(context, apkFile)
        } else {
            inspectSingleApk(context, apkFile)
        }
    }

    /**
     * Inspects standalone APK file signatures and package metadata.
     */
    private fun inspectSingleApk(context: Context, apkFile: File): AppSignDetails {
        var v1 = false
        var v2 = false
        var v3 = false
        var v4 = false

        try {
            val verifier = ApkVerifier.Builder(apkFile).build()
            val result = verifier.verify()
            v1 = result.isVerifiedUsingV1Scheme || result.v1SchemeSigners.isNotEmpty()
            v2 = result.isVerifiedUsingV2Scheme || result.v2SchemeSigners.isNotEmpty()
            v3 = result.isVerifiedUsingV3Scheme || result.v3SchemeSigners.isNotEmpty()
            v4 = result.isVerifiedUsingV4Scheme
        } catch (ignored: Exception) {}

        var appName = apkFile.name
        var packageName = "Unknown Package"
        var versionName = "1.0"
        var icon: Drawable? = null

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
                info.packageName?.let { packageName = it }
                info.versionName?.let { versionName = it }
            }
        } catch (ignored: Exception) {}

        val sizeFormatted = formatFileSize(apkFile.length())

        return AppSignDetails(
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            fileSizeFormatted = sizeFormatted,
            inputFile = apkFile,
            isSplitPackage = false,
            existingV1 = v1,
            existingV2 = v2,
            existingV3 = v3,
            existingV4 = v4,
            icon = icon
        )
    }

    /**
     * Extracts and inspects base APK from split bundle archive to determine signatures and metadata.
     */
    private fun inspectSplitBundle(context: Context, archiveFile: File): AppSignDetails {
        var v1 = false
        var v2 = false
        var v3 = false
        var v4 = false
        var appName = archiveFile.nameWithoutExtension
        var packageName = "Unknown Package"
        var versionName = "1.0"
        var icon: Drawable? = null

        val tempBase = File(context.cacheDir, "inspect_${System.currentTimeMillis()}.apk")
        try {
            ZipFile(archiveFile).use { zip ->
                val apkEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                    .toList()

                val baseEntry = apkEntries.find { it.name.contains("base", ignoreCase = true) }
                    ?: apkEntries.find { it.name.contains("master", ignoreCase = true) }
                    ?: apkEntries.maxByOrNull { it.size }
                    ?: apkEntries.firstOrNull()

                if (baseEntry != null) {
                    zip.getInputStream(baseEntry).use { input ->
                        FileOutputStream(tempBase).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            if (tempBase.exists() && tempBase.length() > 0L) {
                try {
                    val verifier = ApkVerifier.Builder(tempBase).build()
                    val result = verifier.verify()
                    v1 = result.isVerifiedUsingV1Scheme || result.v1SchemeSigners.isNotEmpty()
                    v2 = result.isVerifiedUsingV2Scheme || result.v2SchemeSigners.isNotEmpty()
                    v3 = result.isVerifiedUsingV3Scheme || result.v3SchemeSigners.isNotEmpty()
                    v4 = result.isVerifiedUsingV4Scheme
                } catch (ignored: Exception) {}

                try {
                    val pm = context.packageManager
                    val info = pm.getPackageArchiveInfo(tempBase.absolutePath, PackageManager.GET_ACTIVITIES)
                    if (info != null) {
                        val appInfo = info.applicationInfo
                        if (appInfo != null) {
                            appInfo.sourceDir = tempBase.absolutePath
                            appInfo.publicSourceDir = tempBase.absolutePath
                            appName = pm.getApplicationLabel(appInfo).toString()
                            icon = pm.getApplicationIcon(appInfo)
                        }
                        info.packageName?.let { packageName = it }
                        info.versionName?.let { versionName = it }
                    }
                } catch (ignored: Exception) {}
            }
        } catch (ignored: Exception) {
        } finally {
            if (tempBase.exists()) {
                tempBase.delete()
            }
        }

        val sizeFormatted = formatFileSize(archiveFile.length())

        return AppSignDetails(
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            fileSizeFormatted = sizeFormatted,
            inputFile = archiveFile,
            isSplitPackage = true,
            existingV1 = v1,
            existingV2 = v2,
            existingV3 = v3,
            existingV4 = v4,
            icon = icon
        )
    }

    /**
     * Extracts application icon drawable from standalone APK or APKS archive file.
     */
    fun extractIconFromApk(context: Context, filePath: String): Drawable? {
        val file = File(filePath)
        if (!file.exists()) return null

        val ext = file.extension.lowercase(Locale.getDefault())
        if (ext in listOf("apks", "xapk", "apkm", "zip")) {
            return extractIconFromSplitArchive(context, file)
        }

        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_ACTIVITIES) ?: return null
            val appInfo = info.applicationInfo ?: return null
            appInfo.sourceDir = file.absolutePath
            appInfo.publicSourceDir = file.absolutePath
            pm.getApplicationIcon(appInfo)
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * Unpacks base APK entry from split archive into temporary cache to extract app icon.
     */
    private fun extractIconFromSplitArchive(context: Context, archiveFile: File): Drawable? {
        return try {
            val tempBase = File(context.cacheDir, "temp_icon_${System.currentTimeMillis()}.apk")
            ZipFile(archiveFile).use { zip ->
                val apkEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                    .toList()

                val baseEntry = apkEntries.find { it.name.contains("base", ignoreCase = true) }
                    ?: apkEntries.find { it.name.contains("master", ignoreCase = true) }
                    ?: apkEntries.maxByOrNull { it.size }
                    ?: apkEntries.firstOrNull()

                if (baseEntry != null) {
                    zip.getInputStream(baseEntry).use { input ->
                        FileOutputStream(tempBase).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            if (tempBase.exists() && tempBase.length() > 0L) {
                val icon = extractIconFromApk(context, tempBase.absolutePath)
                tempBase.delete()
                icon
            } else {
                null
            }
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * Formats bytes into human-readable data size string representation.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
