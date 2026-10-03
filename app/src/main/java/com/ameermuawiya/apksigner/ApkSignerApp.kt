package com.ameermuawiya.apksigner

import android.app.Application
import com.ameermuawiya.apksigner.data.preferences.SettingsManager
import com.ameermuawiya.apksigner.utils.NotificationHelper

/**
 * Main application class initializing default preferences, theme configs, and notification channels upon startup.
 */
class ApkSignerApp : Application() {

    lateinit var settingsManager: SettingsManager
        private set

    /**
     * Called when application starts; initializes core settings and notification helper instance.
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsManager = SettingsManager(this)
        NotificationHelper(this)
    }

    companion object {
        lateinit var instance: ApkSignerApp
            private set
    }
}
