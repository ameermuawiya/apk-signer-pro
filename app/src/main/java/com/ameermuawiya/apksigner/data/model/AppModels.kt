package com.ameermuawiya.apksigner.data.model

import android.graphics.drawable.Drawable
import java.io.File

/**
 * Data class representing an installed application on the device.
 */
data class InstalledAppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val apkFile: File,
    val splitApkFiles: List<File> = emptyList()
) {
    val totalSizeBytes: Long
        get() = (if (apkFile.exists()) apkFile.length() else 0L) + splitApkFiles.sumOf { if (it.exists()) it.length() else 0L }

    val formattedSize: String
        get() {
            val bytes = totalSizeBytes
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
}

/**
 * Extracted details of selected target APK file before signing.
 */
data class AppSignDetails(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val fileSizeFormatted: String,
    val inputFile: File,
    val isSplitPackage: Boolean,
    val existingV1: Boolean,
    val existingV2: Boolean,
    val existingV3: Boolean,
    val existingV4: Boolean,
    val icon: Drawable? = null,
    val isFromInstalledApp: Boolean = false,
    val originalParentDir: File? = null
)
