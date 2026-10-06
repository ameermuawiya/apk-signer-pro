package com.ameermuawiya.apksigner.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ameermuawiya.apksigner.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Helper object providing safe system package installation and file sharing capabilities.
 */
object InstallerHelper {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Prepares and launches system package installer for single APK or split APKS packages.
     */
    fun installPackage(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, context.getString(R.string.installer_helper_file_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permissionIntent)
                Toast.makeText(context, context.getString(R.string.installer_helper_permission_needed), Toast.LENGTH_LONG).show()
                return
            }
        }

        val extension = file.extension.lowercase(Locale.getDefault())
        if (extension == "apk") {
            installSingleApk(context, file)
        } else if (extension in listOf("apks", "xapk", "zip")) {
            installSplitPackage(context, file)
        } else {
            installSingleApk(context, file)
        }
    }

    /**
     * Launches standard system installer view intent for standalone APK files.
     */
    fun installApk(context: Context, apkFile: File) {
        installPackage(context, apkFile)
    }

    /**
     * Executes single APK installation using FileProvider intent.
     */
    private fun installSingleApk(context: Context, apkFile: File) {
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
            Toast.makeText(
                context,
                context.getString(R.string.installer_helper_install_error, e.localizedMessage ?: ""),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Installs split APK bundles (.apks / .xapk / .zip) via PackageInstaller session asynchronously.
     */
    private fun installSplitPackage(context: Context, archiveFile: File) {
        Toast.makeText(context, context.getString(R.string.installer_helper_installing_split), Toast.LENGTH_SHORT).show()

        executor.execute {
            val tempExtractDir = File(context.cacheDir, "split_install_${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                val extractedApks = mutableListOf<File>()

                ZipInputStream(FileInputStream(archiveFile)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    var entryIndex = 0
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && name.endsWith(".apk", ignoreCase = true)) {
                            val cleanName = "split_${entryIndex++}.apk"
                            val targetFile = File(tempExtractDir, cleanName)
                            FileOutputStream(targetFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            if (targetFile.exists() && targetFile.length() > 0) {
                                extractedApks.add(targetFile)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                if (extractedApks.isEmpty()) {
                    mainHandler.post {
                        Toast.makeText(context, context.getString(R.string.installer_helper_install_error, "No valid APKs found in archive"), Toast.LENGTH_SHORT).show()
                    }
                    tempExtractDir.deleteRecursively()
                    return@execute
                }

                if (extractedApks.size == 1) {
                    mainHandler.post {
                        installSingleApk(context, extractedApks.first())
                    }
                    return@execute
                }

                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)

                for (apk in extractedApks) {
                    val size = apk.length()
                    FileInputStream(apk).use { fis ->
                        session.openWrite(apk.name, 0, size).use { out ->
                            fis.copyTo(out)
                            session.fsync(out)
                        }
                    }
                }

                val callbackIntent = Intent(context, com.ameermuawiya.apksigner.receiver.PackageInstallReceiver::class.java)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(context, sessionId, callbackIntent, flags)
                session.commit(pendingIntent.intentSender)
                session.close()
            } catch (e: Exception) {
                mainHandler.post {
                    Toast.makeText(
                        context,
                        context.getString(R.string.installer_helper_install_error, e.localizedMessage ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                tempExtractDir.deleteRecursively()
            }
        }
    }

    /**
     * Dispatches file share intent using system chooser with read permissions.
     */
    fun shareFile(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, context.getString(R.string.installer_helper_file_not_found), Toast.LENGTH_SHORT).show()
            return
        }

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

            val mimeType = when (safeFile.extension.lowercase(Locale.getDefault())) {
                "apk" -> "application/vnd.android.package-archive"
                "apks", "zip", "xapk" -> "application/zip"
                else -> "application/octet-stream"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, safeFile.name).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.installer_helper_install_error, e.localizedMessage ?: ""),
                Toast.LENGTH_SHORT
            ).show()
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
