package com.ameermuawiya.apksigner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ameermuawiya.apksigner.ui.navigation.NavContainer
import com.ameermuawiya.apksigner.ui.theme.ApkSignerTheme
import com.ameermuawiya.apksigner.ui.viewmodel.MainViewModel

/**
 * Main entrance activity setting up Compose UI scaffold, edge-to-edge drawing, and incoming file intents.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /**
     * Initializes activity window, requests notification permissions, handles intent files, and sets Compose content.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestAppPermissions()
        handleIncomingIntent(intent)

        setContent {
            val themeMode by viewModel.settingsManager.themeMode.collectAsState()
            val dynamicColor by viewModel.settingsManager.dynamicColor.collectAsState()

            ApkSignerTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                NavContainer(viewModel = viewModel)
            }
        }
    }

    /**
     * Handles single top activity re-launch for new incoming intents.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Processes intent data or EXTRA_STREAM URIs when shared or opened from external apps.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (Intent.ACTION_SEND == action) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            uri?.let { viewModel.selectFileFromUri(it) }
        } else if (Intent.ACTION_VIEW == action) {
            intent.data?.let { viewModel.selectFileFromUri(it) }
        }
    }

    /**
     * Checks and requests runtime notification permission on Android 13+.
     */
    private fun requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
}
