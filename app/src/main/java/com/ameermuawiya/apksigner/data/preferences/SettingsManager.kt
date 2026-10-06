package com.ameermuawiya.apksigner.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Data class representing a saved custom keystore metadata record.
 */
data class CustomKeyRecord(
    val id: String,
    val name: String,
    val path: String,
    val alias: String,
    val format: String = "JKS",
    val details: String = ""
)

/**
 * Manages user preferences, signature scheme toggles, keystores, and storage locations.
 */
class SettingsManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("apk_signer_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean("dynamic_color", true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _v1Scheme = MutableStateFlow(prefs.getBoolean("v1_scheme", true))
    val v1Scheme: StateFlow<Boolean> = _v1Scheme.asStateFlow()

    private val _v2Scheme = MutableStateFlow(prefs.getBoolean("v2_scheme", true))
    val v2Scheme: StateFlow<Boolean> = _v2Scheme.asStateFlow()

    private val _v3Scheme = MutableStateFlow(prefs.getBoolean("v3_scheme", true))
    val v3Scheme: StateFlow<Boolean> = _v3Scheme.asStateFlow()

    private val _v4Scheme = MutableStateFlow(prefs.getBoolean("v4_scheme", false))
    val v4Scheme: StateFlow<Boolean> = _v4Scheme.asStateFlow()

    private val _customKeyPath = MutableStateFlow(prefs.getString("custom_key_path", null))
    val customKeyPath: StateFlow<String?> = _customKeyPath.asStateFlow()

    private val _customKeyAlias = MutableStateFlow(prefs.getString("custom_key_alias", null))
    val customKeyAlias: StateFlow<String?> = _customKeyAlias.asStateFlow()

    private val _workingDirectoryPath = MutableStateFlow(loadInitialWorkingDirectory())
    val workingDirectoryPath: StateFlow<String> = _workingDirectoryPath.asStateFlow()

    /**
     * Resolves default working directory folder in external public downloads or app files.
     */
    fun getDefaultWorkingDirectory(): File {
        return try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val defaultDir = File(downloadDir, "APK Signer Pro")
            if (!defaultDir.exists()) {
                defaultDir.mkdirs()
            }
            defaultDir
        } catch (e: Exception) {
            val fallback = File(context.getExternalFilesDir(null), "APK Signer Pro")
            if (!fallback.exists()) fallback.mkdirs()
            fallback
        }
    }

    /**
     * Loads working directory path from storage preferences or default folder.
     */
    private fun loadInitialWorkingDirectory(): String {
        val saved = prefs.getString("working_directory_path", null)
        if (!saved.isNullOrBlank()) {
            val file = File(saved)
            if (file.exists() || file.mkdirs()) {
                return file.absolutePath
            }
        }
        return getDefaultWorkingDirectory().absolutePath
    }

    /**
     * Obtains valid output working directory File instance ready for writing.
     */
    fun getEffectiveWorkingDirectory(): File {
        val path = _workingDirectoryPath.value
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return if (dir.exists()) dir else getDefaultWorkingDirectory()
    }

    /**
     * Updates working directory path setting and persists to preferences.
     */
    fun setWorkingDirectory(path: String) {
        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }
        val cleanPath = file.absolutePath
        prefs.edit().putString("working_directory_path", cleanPath).apply()
        _workingDirectoryPath.value = cleanPath
    }

    /**
     * Resets working directory path back to the standard default folder.
     */
    fun resetWorkingDirectory() {
        val defaultPath = getDefaultWorkingDirectory().absolutePath
        prefs.edit().remove("working_directory_path").apply()
        _workingDirectoryPath.value = defaultPath
    }

    /**
     * Loads custom keystore records from persistent string set.
     */
    private fun loadKeyRecords(): List<CustomKeyRecord> {
        val set = prefs.getStringSet("custom_keys_set", emptySet()) ?: emptySet()
        return set.mapNotNull { item ->
            val parts = item.split("|")
            if (parts.size >= 5) {
                val details = if (parts.size >= 6) parts[5] else ""
                CustomKeyRecord(parts[0], parts[1], parts[2], parts[3], parts[4], details)
            } else null
        }.sortedBy { it.name }
    }

    private val _customKeysList = MutableStateFlow<List<CustomKeyRecord>>(loadKeyRecords())
    val customKeysList: StateFlow<List<CustomKeyRecord>> = _customKeysList.asStateFlow()

    /**
     * Updates theme preference mode between system, light, and dark.
     */
    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    /**
     * Updates dynamic color scheme toggle for Material You styling.
     */
    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
        _dynamicColor.value = enabled
    }

    /**
     * Updates state for V1 signature scheme toggle.
     */
    fun setV1Scheme(enabled: Boolean) {
        prefs.edit().putBoolean("v1_scheme", enabled).apply()
        _v1Scheme.value = enabled
    }

    /**
     * Updates state for V2 signature scheme toggle.
     */
    fun setV2Scheme(enabled: Boolean) {
        prefs.edit().putBoolean("v2_scheme", enabled).apply()
        _v2Scheme.value = enabled
    }

    /**
     * Updates state for V3 signature scheme toggle.
     */
    fun setV3Scheme(enabled: Boolean) {
        prefs.edit().putBoolean("v3_scheme", enabled).apply()
        _v3Scheme.value = enabled
    }

    /**
     * Updates state for V4 signature scheme toggle.
     */
    fun setV4Scheme(enabled: Boolean) {
        prefs.edit().putBoolean("v4_scheme", enabled).apply()
        _v4Scheme.value = enabled
    }

    /**
     * Sets active custom keystore file path and alias information.
     */
    fun setCustomKey(path: String?, alias: String?) {
        prefs.edit()
            .putString("custom_key_path", path)
            .putString("custom_key_alias", alias)
            .apply()
        _customKeyPath.value = path
        _customKeyAlias.value = alias
    }

    /**
     * Adds custom key record to persistent list and selects it.
     */
    fun addCustomKeyRecord(record: CustomKeyRecord, password: String? = null) {
        val current = _customKeysList.value.toMutableList()
        current.removeAll { it.id == record.id || it.path == record.path }
        current.add(record)
        saveKeyRecords(current)
        _customKeysList.value = current
        if (!password.isNullOrBlank()) {
            savePasswordForKey(record.path, password)
        }
        setCustomKey(record.path, record.alias)
    }

    /**
     * Removes custom key record and its stored password from persistent list.
     */
    fun removeCustomKeyRecord(id: String) {
        val current = _customKeysList.value.toMutableList()
        val itemToRemove = current.find { it.id == id }
        current.removeAll { it.id == id }
        saveKeyRecords(current)
        _customKeysList.value = current

        if (itemToRemove != null) {
            clearPasswordForKey(itemToRemove.path)
            if (_customKeyPath.value == itemToRemove.path) {
                setCustomKey(null, null)
            }
        }
    }

    /**
     * Serializes key record list and commits to persistent preferences.
     */
    private fun saveKeyRecords(list: List<CustomKeyRecord>) {
        val set = list.map { "${it.id}|${it.name}|${it.path}|${it.alias}|${it.format}|${it.details}" }.toSet()
        prefs.edit().putStringSet("custom_keys_set", set).apply()
    }

    /**
     * Saves password associated with specific keystore path.
     */
    fun savePasswordForKey(path: String, password: String) {
        prefs.edit().putString("key_pw_$path", password).apply()
    }

    /**
     * Retrieves stored password associated with specific keystore path.
     */
    fun getPasswordForKey(path: String): String? {
        return prefs.getString("key_pw_$path", null)
    }

    /**
     * Removes stored password for a removed keystore path.
     */
    fun clearPasswordForKey(path: String) {
        prefs.edit().remove("key_pw_$path").apply()
    }
}
