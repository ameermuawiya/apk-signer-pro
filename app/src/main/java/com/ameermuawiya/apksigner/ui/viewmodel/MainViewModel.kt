package com.ameermuawiya.apksigner.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ameermuawiya.apksigner.BuildConfig
import com.ameermuawiya.apksigner.data.db.AppDatabase
import com.ameermuawiya.apksigner.data.db.HistoryEntity
import com.ameermuawiya.apksigner.data.keystore.KeystoreInspectionResult
import com.ameermuawiya.apksigner.data.keystore.KeystoreManager
import com.ameermuawiya.apksigner.data.model.AppSignDetails
import com.ameermuawiya.apksigner.data.model.AppUpdateInfo
import com.ameermuawiya.apksigner.data.model.InstalledAppInfo
import com.ameermuawiya.apksigner.data.preferences.CustomKeyRecord
import com.ameermuawiya.apksigner.data.preferences.SettingsManager
import com.ameermuawiya.apksigner.data.repository.HistoryRepository
import com.ameermuawiya.apksigner.data.repository.InstalledAppsRepository
import com.ameermuawiya.apksigner.engine.ApkSignerEngineWrapper
import com.ameermuawiya.apksigner.utils.InstallerHelper
import com.ameermuawiya.apksigner.utils.NotificationHelper
import com.ameermuawiya.apksigner.utils.SignatureDetector
import com.ameermuawiya.apksigner.utils.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
 * Pre-computed model for history UI item to avoid main thread layout stalls.
 */
data class HistoryUiItem(
    val entity: HistoryEntity,
    val formattedSize: String,
    val isInstalled: Boolean,
    val icon: Drawable?,
    val schemes: String
)

/**
 * ViewModel managing app selection, signing workflows, history data, backup tasks, and settings.
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

    private val _isAppsRefreshing = MutableStateFlow(false)
    val isAppsRefreshing: StateFlow<Boolean> = _isAppsRefreshing.asStateFlow()

    private val _isAppDetailsLoading = MutableStateFlow(false)
    val isAppDetailsLoading: StateFlow<Boolean> = _isAppDetailsLoading.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _backupAppName = MutableStateFlow<String?>(null)
    val backupAppName: StateFlow<String?> = _backupAppName.asStateFlow()

    private val _searchQueryHome = MutableStateFlow("")
    val searchQueryHome: StateFlow<String> = _searchQueryHome.asStateFlow()

    private val _homeTabIndex = MutableStateFlow(0)
    val homeTabIndex: StateFlow<Int> = _homeTabIndex.asStateFlow()

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

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private var isTaskCancelled = false

    private val iconCache = android.util.LruCache<String, Drawable>(64)
    private val packageInstalledCache = android.util.LruCache<String, Boolean>(128)

    private val _isHistoryLoading = MutableStateFlow(true)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private val _isHistoryRefreshing = MutableStateFlow(false)
    val isHistoryRefreshing: StateFlow<Boolean> = _isHistoryRefreshing.asStateFlow()

    val historyRecords: StateFlow<List<HistoryEntity>> = historyRepository.allHistory
        .combine(_searchQueryHistory) { list: List<HistoryEntity>, query: String ->
            if (query.isBlank()) list
            else list.filter { it.fileName.contains(query, ignoreCase = true) || it.filePath.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyUiItems: StateFlow<List<HistoryUiItem>> = historyRecords
        .map { records ->
            _isHistoryLoading.value = true
            val mapped = records.map { entity ->
                val file = File(entity.filePath)
                val formattedSize = if (file.exists()) {
                    SignatureDetector.formatFileSize(file.length())
                } else {
                    "File Missing"
                }

                val installed = isPackageInstalledForApk(entity.filePath)
                val icon = getHistoryItemIcon(entity.filePath)

                val displaySchemes = if (entity.schemes.isNotBlank()) {
                    entity.schemes
                } else if (file.exists()) {
                    val inspected = SignatureDetector.inspectApk(getApplication(), file)
                    val list = mutableListOf<String>()
                    if (inspected.existingV1) list.add("v1")
                    if (inspected.existingV2) list.add("v2")
                    if (inspected.existingV3) list.add("v3")
                    if (inspected.existingV4) list.add("v4")
                    if (list.isEmpty()) "Unsigned" else list.joinToString(" + ")
                } else {
                    "Unknown"
                }

                HistoryUiItem(
                    entity = entity,
                    formattedSize = formattedSize,
                    isInstalled = installed,
                    icon = icon,
                    schemes = displaySchemes
                )
            }
            _isHistoryLoading.value = false
            mapped
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInstalledApps: StateFlow<List<InstalledAppInfo>> = _installedApps
        .combine(_searchQueryHome) { list: List<InstalledAppInfo>, query: String ->
            if (query.isBlank()) list
            else list.filter { it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
        checkForUpdates()
    }

    /**
     * Queries package manager asynchronously for all installed user and system apps.
     */
    fun loadInstalledApps(isPullToRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isPullToRefresh) {
                _isAppsRefreshing.value = true
            } else {
                _isAppsLoading.value = true
            }
            val apps = installedAppsRepository.getInstalledApps(includeSystemApps = true)
            _installedApps.value = apps
            _isAppsLoading.value = false
            _isAppsRefreshing.value = false
        }
    }

    /**
     * Refreshes history list and caches asynchronously.
     */
    fun refreshHistory(isPullToRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isHistoryRefreshing.value = true
            packageInstalledCache.evictAll()
            iconCache.evictAll()
            kotlinx.coroutines.delay(200)
            _isHistoryRefreshing.value = false
        }
    }

    /**
     * Checks remote GitHub repository for new application updates.
     */
    fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            val update = UpdateChecker.checkLatestUpdate(BuildConfig.VERSION_NAME)
            _availableUpdate.value = update
        }
    }

    /**
     * Dismisses the active update bottom sheet.
     */
    fun dismissUpdateSheet() {
        _availableUpdate.value = null
    }

    /**
     * Updates search query for installed applications filtering.
     */
    fun updateHomeSearchQuery(query: String) {
        _searchQueryHome.value = query
    }

    /**
     * Updates active tab index on the home screen.
     */
    fun setHomeTabIndex(index: Int) {
        _homeTabIndex.value = index
    }

    /**
     * Updates search query for signing history entries filtering.
     */
    fun updateHistorySearchQuery(query: String) {
        _searchQueryHistory.value = query
    }

    /**
     * Immediately navigates to sign package screen and asynchronously inspects package signatures.
     */
    fun selectInstalledApp(appInfo: InstalledAppInfo) {
        _selectedAppDetails.value = AppSignDetails(
            appName = appInfo.name,
            packageName = appInfo.packageName,
            versionName = appInfo.versionName,
            fileSizeFormatted = appInfo.formattedSize,
            inputFile = appInfo.apkFile,
            isSplitPackage = appInfo.splitApkFiles.isNotEmpty(),
            existingV1 = false,
            existingV2 = false,
            existingV3 = false,
            existingV4 = false,
            icon = appInfo.icon,
            isFromInstalledApp = true,
            originalParentDir = null
        )
        _isAppDetailsLoading.value = true
        _signingState.value = SigningUiState.Idle
        _liveLogs.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val targetFile: File

                if (appInfo.splitApkFiles.isNotEmpty()) {
                    val apksFile = File(context.cacheDir, "${appInfo.packageName}.apks")
                    ZipOutputStream(FileOutputStream(apksFile)).use { zos ->
                        val filesToPackage = listOf(appInfo.apkFile) + appInfo.splitApkFiles
                        filesToPackage.forEach { f ->
                            if (f.exists()) {
                                zos.putNextEntry(ZipEntry(f.name))
                                FileInputStream(f).use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()
                            }
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
            } catch (ignored: Exception) {
            } finally {
                _isAppDetailsLoading.value = false
            }
        }
    }

    /**
     * Imports APK file selected via content URI and launches sign package inspection.
     */
    fun selectFileFromUri(uri: Uri) {
        _selectedAppDetails.value = AppSignDetails(
            appName = "Package Archive",
            packageName = "Analyzing...",
            versionName = "...",
            fileSizeFormatted = "...",
            inputFile = File(getApplication<Application>().cacheDir, "staging.apk"),
            isSplitPackage = false,
            existingV1 = false,
            existingV2 = false,
            existingV3 = false,
            existingV4 = false,
            icon = null,
            isFromInstalledApp = false,
            originalParentDir = null
        )
        _isAppDetailsLoading.value = true
        _signingState.value = SigningUiState.Idle
        _liveLogs.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
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
            } catch (ignored: Exception) {
            } finally {
                _isAppDetailsLoading.value = false
            }
        }
    }

    /**
     * Clears selected app state and returns to main list view.
     */
    fun clearSelectedTarget() {
        _selectedAppDetails.value = null
        _isAppDetailsLoading.value = false
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
     * Executes signing process with designated keys and writes directly to working directory.
     */
    fun startSigning(customPasswordInput: String? = null) {
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
                record?.name?.takeIf { it.isNotBlank() } ?: (record?.alias ?: File(customKey).name)
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
            val rawName = if (details.appName.isNotBlank() && details.appName != "Package Archive") {
                details.appName.replace(" ", "_").replace("/", "_")
            } else {
                input.nameWithoutExtension.removeSuffix("_signed")
            }
            val ext = input.extension.ifEmpty { "apk" }

            val workingDir = settingsManager.getEffectiveWorkingDirectory()
            val finalOutputFile = getUniqueOutputFile(workingDir, "${rawName}_signed", ext)
            val stagingFile = File(context.cacheDir, "staging_${System.currentTimeMillis()}_$rawName.$ext")

            addLiveLog("[INFO] Input file: ${input.name} (${details.fileSizeFormatted})")
            addLiveLog("[INFO] Target folder: ${workingDir.absolutePath}")

            val errorLog = engine.signFile(
                inputFile = input,
                outputFile = stagingFile,
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
                stagingFile.delete()
                addLiveLog("[WARN] Task was cancelled by user.")
                _signingState.value = SigningUiState.Idle
                return@launch
            }

            if (errorLog == null) {
                stagingFile.copyTo(finalOutputFile, overwrite = true)
                stagingFile.delete()

                if (v4) {
                    val idsigStaging = File(stagingFile.parentFile, "${stagingFile.nameWithoutExtension}.idsig")
                    if (idsigStaging.exists()) {
                        val finalIdsig = File(workingDir, "${finalOutputFile.nameWithoutExtension}.idsig")
                        idsigStaging.copyTo(finalIdsig, overwrite = true)
                        idsigStaging.delete()
                        addLiveLog("[OUTPUT] v4 Signature file: ${finalIdsig.name}")
                    }
                }

                val finalPath = finalOutputFile.absolutePath
                val signedBytes = finalOutputFile.length()
                val signedSizeFormatted = SignatureDetector.formatFileSize(signedBytes)
                val schemesString = listOfNotNull(if (v1) "v1" else null, if (v2) "v2" else null, if (v3) "v3" else null, if (v4) "v4" else null).joinToString(" + ")

                addLiveLog("[SUCCESS] Signed package created successfully: ${details.appName}")
                addLiveLog("[OUTPUT] Schemes: $schemesString")
                addLiveLog("[OUTPUT] Size: $signedSizeFormatted")
                addLiveLog("[LOCATION] Saved to: $finalPath")

                historyRepository.addHistory(
                    fileName = finalOutputFile.name,
                    filePath = finalPath,
                    appName = details.appName,
                    packageName = details.packageName,
                    keyAlias = aliasName,
                    schemes = schemesString
                )
                _signingState.value = SigningUiState.Success(finalPath)
                notificationHelper.showCompletionNotification(
                    "Signing Finished Successfully",
                    "${details.appName} signed successfully.",
                    targetFilePath = finalPath
                )
            } else {
                stagingFile.delete()
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
                val detail = buildString {
                    append(e.javaClass.simpleName)
                    if (!e.message.isNullOrBlank()) {
                        append(": ").append(e.message)
                    }
                    var cause = e.cause
                    while (cause != null) {
                        append("\nCaused by: ").append(cause.javaClass.simpleName)
                        if (!cause.message.isNullOrBlank()) {
                            append(": ").append(cause.message)
                        }
                        cause = cause.cause
                    }
                }
                withContext(Dispatchers.Main) {
                    onResult(false, null, detail)
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
     * Invokes package installer helper supporting both standalone APK and split APKS archives.
     */
    fun installSignedApk(path: String) {
        val file = File(path)
        InstallerHelper.installPackage(getApplication(), file)
    }

    /**
     * Shares output file through Android system sharesheet.
     */
    fun shareSignedFile(path: String) {
        val file = File(path)
        InstallerHelper.shareFile(getApplication(), file)
    }

    /**
     * Deletes history record and optionally purges physical file.
     */
    fun deleteHistoryItem(id: Long, filePath: String, deleteFileFromDisk: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteHistory(id)
            if (deleteFileFromDisk) {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
                val idsigFile = File(file.parentFile, "${file.nameWithoutExtension}.idsig")
                if (idsigFile.exists()) {
                    idsigFile.delete()
                }
            }
        }
    }

    /**
     * Determines whether package for signed APK file is currently installed.
     */
    fun isPackageInstalledForApk(filePath: String): Boolean {
        packageInstalledCache.get(filePath)?.let { return it }
        val context = getApplication<Application>()
        val file = File(filePath)
        if (!file.exists()) return false
        val installed = try {
            val pkgInfo = context.packageManager.getPackageArchiveInfo(filePath, 0)
            if (pkgInfo != null) {
                val installedInfo = context.packageManager.getPackageInfo(pkgInfo.packageName, 0)
                installedInfo != null
            } else {
                false
            }
        } catch (ignored: Exception) {
            false
        }
        packageInstalledCache.put(filePath, installed)
        return installed
    }

    /**
     * Extracts icon from APK or APKS archive for history screen rendering.
     */
    fun getHistoryItemIcon(filePath: String): Drawable? {
        iconCache.get(filePath)?.let { return it }
        val drawable = SignatureDetector.extractIconFromApk(getApplication(), filePath)
        if (drawable != null) {
            iconCache.put(filePath, drawable)
        }
        return drawable
    }

    /**
     * Backs up APK or APKS package directly into configured working directory with loading state.
     */
    fun backupInstalledApp(appInfo: InstalledAppInfo, onComplete: (Boolean, String) -> Unit) {
        _isBackingUp.value = true
        _backupAppName.value = appInfo.name

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workingDir = settingsManager.getEffectiveWorkingDirectory()
                val cleanName = appInfo.name.replace(" ", "_").replace("/", "_")
                val isSplit = appInfo.splitApkFiles.isNotEmpty()
                val ext = if (isSplit) "apks" else "apk"
                val destinationFile = getUniqueOutputFile(workingDir, "${cleanName}_backup", ext)

                if (isSplit) {
                    ZipOutputStream(FileOutputStream(destinationFile)).use { zos ->
                        val filesToPackage = listOf(appInfo.apkFile) + appInfo.splitApkFiles
                        filesToPackage.forEach { f ->
                            if (f.exists()) {
                                zos.putNextEntry(ZipEntry(f.name))
                                FileInputStream(f).use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                } else {
                    appInfo.apkFile.copyTo(destinationFile, overwrite = true)
                }

                withContext(Dispatchers.Main) {
                    _isBackingUp.value = false
                    _backupAppName.value = null
                    onComplete(true, destinationFile.absolutePath)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isBackingUp.value = false
                    _backupAppName.value = null
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
                        if (f.exists()) {
                            zos.putNextEntry(ZipEntry(f.name))
                            FileInputStream(f).use { fis -> fis.copyTo(zos) }
                            zos.closeEntry()
                        }
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

    /**
     * Resolves unique file in directory to prevent overwriting existing files.
     */
    private fun getUniqueOutputFile(directory: File, baseName: String, extension: String): File {
        var candidate = File(directory, "$baseName.$extension")
        if (!candidate.exists()) {
            return candidate
        }
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "${baseName}_$index.$extension")
            index++
        }
        return candidate
    }
}
