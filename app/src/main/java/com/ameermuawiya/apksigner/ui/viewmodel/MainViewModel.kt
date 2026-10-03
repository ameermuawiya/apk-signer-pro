package com.ameermuawiya.apksigner.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ameermuawiya.apksigner.data.db.AppDatabase
import com.ameermuawiya.apksigner.data.db.HistoryEntity
import com.ameermuawiya.apksigner.data.keystore.KeystoreInspectionResult
import com.ameermuawiya.apksigner.data.keystore.KeystoreManager
import com.ameermuawiya.apksigner.data.model.AppSignDetails
import com.ameermuawiya.apksigner.data.model.InstalledAppInfo
import com.ameermuawiya.apksigner.data.preferences.CustomKeyRecord
import com.ameermuawiya.apksigner.data.preferences.SettingsManager
import com.ameermuawiya.apksigner.data.repository.HistoryRepository
import com.ameermuawiya.apksigner.data.repository.InstalledAppsRepository
import com.ameermuawiya.apksigner.engine.ApkSignerEngineWrapper
import com.ameermuawiya.apksigner.utils.InstallerHelper
import com.ameermuawiya.apksigner.utils.NotificationHelper
import com.ameermuawiya.apksigner.utils.SignatureDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * UI state representation for signing process flow.
 */
sealed class SigningUiState {
    object Idle : SigningUiState()
    data class Signing(val step: Int, val totalSteps: Int, val message: String) : SigningUiState()
    data class Success(val outputPath: String) : SigningUiState()
    data class Error(val errorLog: String) : SigningUiState()
}

/**
 * ViewModel managing app selection, signing workflows, history data, and settings.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsManager = SettingsManager(application)
    val keystoreManager = KeystoreManager(application)
    private val installedAppsRepository = InstalledAppsRepository(application)
    private val historyRepository = HistoryRepository(AppDatabase.getDatabase(application).historyDao())
    private val engine = ApkSignerEngineWrapper(application)
    private val notificationHelper = NotificationHelper(application)

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isAppsLoading = MutableStateFlow(false)
    val isAppsLoading: StateFlow<Boolean> = _isAppsLoading.asStateFlow()

    private val _searchQueryHome = MutableStateFlow("")
    val searchQueryHome: StateFlow<String> = _searchQueryHome.asStateFlow()

    private val _searchQueryHistory = MutableStateFlow("")
    val searchQueryHistory: StateFlow<String> = _searchQueryHistory.asStateFlow()

    private val _selectedAppDetails = MutableStateFlow<AppSignDetails?>(null)
    val selectedAppDetails: StateFlow<AppSignDetails?> = _selectedAppDetails.asStateFlow()

    private val _signingState = MutableStateFlow<SigningUiState>(SigningUiState.Idle)
    val signingState: StateFlow<SigningUiState> = _signingState.asStateFlow()

    private val _liveLogs = MutableStateFlow<List<String>>(emptyList())
    val liveLogs: StateFlow<List<String>> = _liveLogs.asStateFlow()

    private val _showKeystoreDialog = MutableStateFlow(false)
    val showKeystoreDialog: StateFlow<Boolean> = _showKeystoreDialog.asStateFlow()

    private var isTaskCancelled = false

    val historyRecords: StateFlow<List<HistoryEntity>> = historyRepository.allHistory
        .combine(_searchQueryHistory) { list: List<HistoryEntity>, query: String ->
            if (query.isBlank()) list
            else list.filter { it.fileName.contains(query, ignoreCase = true) || it.filePath.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInstalledApps: StateFlow<List<InstalledAppInfo>> = _installedApps
        .combine(_searchQueryHome) { list: List<InstalledAppInfo>, query: String ->
            if (query.isBlank()) list
            else list.filter { it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    /**
     * Queries package manager asynchronously for all installed user and system apps.
     */
    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isAppsLoading.value = true
            val apps = installedAppsRepository.getInstalledApps(includeSystemApps = true)
            _installedApps.value = apps
            _isAppsLoading.value = false
        }
    }

    /**
     * Updates search query for installed applications filtering.
     */
    fun updateHomeSearchQuery(query: String) {
        _searchQueryHome.value = query
    }

    /**
     * Updates search query for signing history entries filtering.
     */
    fun updateHistorySearchQuery(query: String) {
        _searchQueryHistory.value = query
    }

    /**
     * Copies installed APK to internal cache safely and analyzes signature structure.
     */
    fun selectInstalledApp(appInfo: InstalledAppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAppsLoading.value = true
            val context = getApplication<Application>()
            val targetFile: File

            if (appInfo.splitApkFiles.isNotEmpty()) {
                val apksFile = File(context.cacheDir, "${appInfo.packageName}.apks")
                ZipOutputStream(FileOutputStream(apksFile)).use { zos ->
                    val filesToPackage = listOf(appInfo.apkFile) + appInfo.splitApkFiles
                    filesToPackage.forEach { f ->
                        zos.putNextEntry(ZipEntry(f.name))
                        FileInputStream(f).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                targetFile = apksFile
            } else {
                val copiedApk = File(context.cacheDir, "${appInfo.packageName}.apk")
                appInfo.apkFile.copyTo(copiedApk, overwrite = true)
                targetFile = copiedApk
            }

            val details = SignatureDetector.inspectApk(context, targetFile).copy(
                appName = appInfo.name,
                packageName = appInfo.packageName,
                versionName = appInfo.versionName,
                icon = appInfo.icon,
                isFromInstalledApp = true,
                originalParentDir = null
            )
            _selectedAppDetails.value = details
            _signingState.value = SigningUiState.Idle
            _liveLogs.value = emptyList()
            _isAppsLoading.value = false
        }
    }

    /**
     * Imports APK file selected via content URI and creates inspection details.
     */
    fun selectFileFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAppsLoading.value = true
            val context = getApplication<Application>()
            var fileName = "file_to_sign.apk"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }

            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val details = SignatureDetector.inspectApk(context, tempFile).copy(
                isFromInstalledApp = false,
                originalParentDir = null
            )
            _selectedAppDetails.value = details
            _signingState.value = SigningUiState.Idle
            _liveLogs.value = emptyList()
            _isAppsLoading.value = false
        }
    }

    /**
     * Clears selected app state and returns to main list view.
     */
    fun clearSelectedTarget() {
        _selectedAppDetails.value = null
        _signingState.value = SigningUiState.Idle
        _liveLogs.value = emptyList()
    }

    /**
     * Adds line message to persistent live logs list.
     */
    private fun addLiveLog(message: String) {
        val current = _liveLogs.value.toMutableList()
        current.add(message)
        _liveLogs.value = current
    }

    /**
     * Executes signing process with designated keys and writes to destination file or SAF URI.
     */
    fun startSigning(targetOutputUri: Uri? = null, customPasswordInput: String? = null) {
        val details = _selectedAppDetails.value ?: return
        isTaskCancelled = false
        _liveLogs.value = emptyList()

        val customKey = settingsManager.customKeyPath.value
        val passwordToUse = customPasswordInput
            ?: (if (!customKey.isNullOrBlank()) settingsManager.getPasswordForKey(customKey) else null)
            ?: keystoreManager.getSessionPassword()

        if (!customKey.isNullOrBlank() && passwordToUse.isNullOrBlank()) {
            _showKeystoreDialog.value = true
            return
        }

        if (!customPasswordInput.isNullOrBlank() && !customKey.isNullOrBlank()) {
            keystoreManager.setSessionPassword(customPasswordInput)
            settingsManager.savePasswordForKey(customKey, customPasswordInput)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val v1 = settingsManager.v1Scheme.value
            val v2 = settingsManager.v2Scheme.value
            val v3 = settingsManager.v3Scheme.value
            val v4 = settingsManager.v4Scheme.value

            val keyName = if (!customKey.isNullOrBlank()) {
                val record = settingsManager.customKeysList.value.find { it.path == customKey }
                if (!record?.name.isNullOrBlank()) record!!.name else (record?.alias ?: File(customKey).name)
            } else {
                "Built-in Debug Key"
            }
            val aliasName = settingsManager.customKeyAlias.value ?: "androiddebugkey"

            addLiveLog("[INFO] Initializing signing task for ${details.appName}...")
            addLiveLog("[CONFIG] Package: ${details.packageName} (v${details.versionName})")
            addLiveLog("[CONFIG] Signing Key: $keyName (Alias: $aliasName)")
            addLiveLog("[CONFIG] Schemes: [v1: $v1, v2: $v2, v3: $v3, v4: $v4]")

            _signingState.value = SigningUiState.Signing(1, 4, "Preparing output target...")

            val input = details.inputFile
            val nameWithoutExt = input.nameWithoutExtension.removeSuffix("_signed")
            val ext = input.extension.ifEmpty { "apk" }

            val localOutputFile = File(context.cacheDir, "signed_${System.currentTimeMillis()}_$nameWithoutExt.$ext")

            addLiveLog("[INFO] Input file: ${input.name} (${details.fileSizeFormatted})")
            addLiveLog("[INFO] Target staging: ${localOutputFile.name}")

            val errorLog = engine.signFile(
                inputFile = input,
                outputFile = localOutputFile,
                customKeyPath = customKey,
                keystorePassword = passwordToUse,
                alias = settingsManager.customKeyAlias.value,
                v1 = v1,
                v2 = v2,
                v3 = v3,
                v4 = v4,
                onProgress = { step, total, msg ->
                    addLiveLog("[STEP $step/$total] $msg")
                    _signingState.value = SigningUiState.Signing(step, total, msg)
                },
                isCancelled = { isTaskCancelled }
            )

            if (isTaskCancelled) {
                addLiveLog("[WARN] Task was cancelled by user.")
                _signingState.value = SigningUiState.Idle
                return@launch
            }

            if (errorLog == null) {
                var finalDestination = localOutputFile.absolutePath
                if (targetOutputUri != null) {
                    addLiveLog("[OUTPUT] Writing directly to selected storage location...")
                    try {
                        context.contentResolver.openOutputStream(targetOutputUri)?.use { out ->
                            FileInputStream(localOutputFile).use { inp ->
                                inp.copyTo(out)
                            }
                        }
                        finalDestination = targetOutputUri.toString()
                    } catch (e: Exception) {
                        addLiveLog("[WARN] Could not write to selected URI: ${e.message}")
                    }
                }

                val signedBytes = localOutputFile.length()
                val signedSizeFormatted = if (signedBytes > 1024 * 1024) {
                    String.format("%.2f MB", signedBytes / (1024.0 * 1024.0))
                } else {
                    String.format("%.1f KB", signedBytes / 1024.0)
                }

                addLiveLog("[SUCCESS] Signed package created successfully: ${details.appName}")
                addLiveLog("[OUTPUT] Size: $signedSizeFormatted")
                addLiveLog("[LOCATION] Saved to: $finalDestination")

                historyRepository.addHistory(
                    fileName = "${details.appName}_signed.$ext",
                    filePath = finalDestination,
                    appName = details.appName,
                    packageName = details.packageName
                )
                _signingState.value = SigningUiState.Success(finalDestination)
                notificationHelper.showCompletionNotification(
                    "Signing Finished Successfully",
                    "${details.appName} signed successfully."
                )
            } else {
                addLiveLog("[ERROR] Signing failed: $errorLog")
                _signingState.value = SigningUiState.Error(errorLog)
                notificationHelper.showCompletionNotification(
                    "Signing Task Failed",
                    "Failed to sign ${input.name}. Tap to view log."
                )
            }
        }
    }

    /**
     * Opens keystore password and alias picker dialog.
     */
    fun openKeyPickerDialog() {
        _showKeystoreDialog.value = true
    }

    /**
     * Signals running background signing job to cancel.
     */
    fun cancelSigning() {
        isTaskCancelled = true
        _signingState.value = SigningUiState.Idle
    }

    /**
     * Inspects keystore credentials and extracts full metadata asynchronously.
     */
    fun inspectKeystore(keyFile: File, password: String, onResult: (Boolean, KeystoreInspectionResult?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = keystoreManager.inspectKeystore(keyFile, password)
                keystoreManager.setSessionPassword(password)
                withContext(Dispatchers.Main) {
                    onResult(true, result, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, null, e.localizedMessage ?: "Invalid password or keystore format.")
                }
            }
        }
    }

    /**
     * Saves new custom keystore metadata entry into settings store with optional custom title.
     */
    fun saveCustomKeystore(path: String, alias: String, format: String, details: String, password: String, customTitle: String = "") {
        val record = CustomKeyRecord(
            id = "key_${System.currentTimeMillis()}",
            name = customTitle.trim(),
            path = path,
            alias = alias,
            format = format,
            details = details
        )
        settingsManager.addCustomKeyRecord(record, password)
        _showKeystoreDialog.value = false
    }

    /**
     * Deletes saved custom keystore record by ID.
     */
    fun deleteCustomKeyRecord(id: String) {
        settingsManager.removeCustomKeyRecord(id)
    }

    /**
     * Sets active custom keystore record or reverts to default debug key.
     */
    fun selectCustomKeyRecord(record: CustomKeyRecord?) {
        if (record == null) {
            resetToDefaultKey()
        } else {
            settingsManager.setCustomKey(record.path, record.alias)
        }
    }

    /**
     * Reverts active signing keystore configuration to default debug key.
     */
    fun resetToDefaultKey() {
        settingsManager.setCustomKey(null, null)
        keystoreManager.setSessionPassword(null)
    }

    /**
     * Closes keystore import dialog without saving changes.
     */
    fun dismissKeystoreDialog() {
        _showKeystoreDialog.value = false
    }

    /**
     * Invokes system package installer for selected APK file path.
     */
    fun installSignedApk(path: String) {
        val file = File(path)
        if (file.exists()) {
            InstallerHelper.installApk(getApplication(), file)
        }
    }

    /**
     * Shares output file through Android system sharesheet.
     */
    fun shareSignedApk(path: String) {
        val context = getApplication<Application>()
        if (path.startsWith("content://")) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse(path))
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share Signed APK").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (ignored: Exception) {}
        } else {
            val file = File(path)
            if (file.exists()) {
                InstallerHelper.shareFile(context, file)
            }
        }
    }

    /**
     * Convenience alias for sharing signed package files.
     */
    fun shareSignedFile(path: String) {
        shareSignedApk(path)
    }

    /**
     * Deletes history record and optionally purges physical file.
     */
    fun deleteHistoryItem(id: Long, filePath: String, deleteFileFromDisk: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteHistory(id)
            if (deleteFileFromDisk) {
                if (filePath.startsWith("content://")) {
                    try {
                        val uri = Uri.parse(filePath)
                        getApplication<Application>().contentResolver.delete(uri, null, null)
                    } catch (ignored: Exception) {}
                } else {
                    val file = File(filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * Determines whether package for signed APK file is currently installed.
     */
    fun isPackageInstalledForApk(filePath: String): Boolean {
        val context = getApplication<Application>()
        val file = File(filePath)
        if (!file.exists() || file.extension.lowercase() != "apk") return false
        return try {
            val pkgInfo = context.packageManager.getPackageArchiveInfo(filePath, 0) ?: return false
            val installedInfo = context.packageManager.getPackageInfo(pkgInfo.packageName, 0)
            installedInfo != null
        } catch (ignored: Exception) {
            false
        }
    }

    /**
     * Backs up APK or APKS package directly to user-selected SAF destination URI.
     */
    fun backupApkToUri(appInfo: InstalledAppInfo, destinationUri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    if (appInfo.splitApkFiles.isNotEmpty()) {
                        ZipOutputStream(outputStream).use { zos ->
                            val filesToPackage = listOf(appInfo.apkFile) + appInfo.splitApkFiles
                            filesToPackage.forEach { f ->
                                zos.putNextEntry(ZipEntry(f.name))
                                FileInputStream(f).use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    } else {
                        FileInputStream(appInfo.apkFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    onComplete(true, destinationUri.toString())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, e.localizedMessage ?: "Failed to backup APK.")
                }
            }
        }
    }

    /**
     * Opens system sharesheet with source APK file or APKS bundle.
     */
    fun shareSourceApk(appInfo: InstalledAppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (appInfo.splitApkFiles.isNotEmpty()) {
                val apksFile = File(context.cacheDir, "${appInfo.packageName}.apks")
                ZipOutputStream(FileOutputStream(apksFile)).use { zos ->
                    val filesToPackage = listOf(appInfo.apkFile) + appInfo.splitApkFiles
                    filesToPackage.forEach { f ->
                        zos.putNextEntry(ZipEntry(f.name))
                        FileInputStream(f).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                withContext(Dispatchers.Main) {
                    InstallerHelper.shareFile(context, apksFile)
                }
            } else {
                withContext(Dispatchers.Main) {
                    InstallerHelper.shareFile(context, appInfo.apkFile)
                }
            }
        }
    }
}
