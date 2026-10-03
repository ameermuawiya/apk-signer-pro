package com.ameermuawiya.apksigner.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helper object providing safe system package installation and file sharing capabilities.
 */
object InstallerHelper {

    /**
     * Prepares and launches system package installer for the target APK archive.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        try {
            val safeFile = ensureSafeSharableFile(context, apkFile)
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    safeFile
                )
            } else {
                Uri.fromFile(safeFile)
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Install error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dispatches file share intent using system chooser with read permissions.
     */
    fun shareFile(context: Context, file: File) {
        if (!file.exists()) return

        try {
            val safeFile = ensureSafeSharableFile(context, file)
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    safeFile
                )
            } else {
                Uri.fromFile(safeFile)
            }

            val mimeType = when (safeFile.extension.lowercase()) {
                "apk" -> "application/vnd.android.package-archive"
                "apks", "zip", "xapk" -> "application/zip"
                else -> "application/octet-stream"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share APK").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Ensures target file is situated within authorized FileProvider directories.
     */
    private fun ensureSafeSharableFile(context: Context, originalFile: File): File {
        val path = originalFile.absolutePath
        val isInsideCache = path.startsWith(context.cacheDir.absolutePath)
        val isInsideFiles = path.startsWith(context.filesDir.absolutePath)
        val isInsideExtFiles = context.getExternalFilesDir(null)?.let { path.startsWith(it.absolutePath) } ?: false
        val isInsideStorage = try {
            path.startsWith(Environment.getExternalStorageDirectory().absolutePath)
        } catch (e: Exception) { false }

        if (isInsideCache || isInsideFiles || isInsideExtFiles || isInsideStorage) {
            return originalFile
        }

        val shareDir = File(context.cacheDir, "shared_apks").apply { if (!exists()) mkdirs() }
        val targetCopy = File(shareDir, originalFile.name)
        originalFile.copyTo(targetCopy, overwrite = true)
        return targetCopy
    }
}
