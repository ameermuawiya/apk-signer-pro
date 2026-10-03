package com.ameermuawiya.apksigner.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.ameermuawiya.apksigner.data.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository loading and filtering installed application packages from device PackageManager.
 */
class InstalledAppsRepository(private val context: Context) {

    /**
     * Queries package manager for installed apps and returns list of InstalledAppInfo.
     */
    suspend fun getInstalledApps(includeSystemApps: Boolean = true): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            val appList = mutableListOf<InstalledAppInfo>()

            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!includeSystemApps && isSystem) continue

                val name = pm.getApplicationLabel(appInfo).toString()
                val icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
                val apkFile = File(appInfo.publicSourceDir ?: appInfo.sourceDir)

                val splits = mutableListOf<File>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    appInfo.splitPublicSourceDirs?.forEach { splitPath ->
                        val f = File(splitPath)
                        if (f.exists()) splits.add(f)
                    }
                }

                val verCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }

                appList.add(
                    InstalledAppInfo(
                        name = name,
                        packageName = pkg.packageName,
                        versionName = pkg.versionName ?: "1.0",
                        versionCode = verCode,
                        icon = icon,
                        isSystemApp = isSystem,
                        apkFile = apkFile,
                        splitApkFiles = splits
                    )
                )
            }

            appList.sortedBy { it.name.lowercase() }
        }
}
