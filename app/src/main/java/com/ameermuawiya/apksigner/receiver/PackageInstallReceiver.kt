package com.ameermuawiya.apksigner.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.utils.NotificationHelper

/**
 * BroadcastReceiver handling system PackageInstaller status callbacks for split APK installation sessions.
 */
class PackageInstallReceiver : BroadcastReceiver() {

    /**
     * Dispatches user confirmation dialog or displays status feedback on install completion.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, context.getString(R.string.installer_helper_install_success), Toast.LENGTH_SHORT).show()
                NotificationHelper(context).showInstallationSuccessNotification(context.getString(R.string.app_name))
            }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Installation aborted or failed"
                if (status != PackageInstaller.STATUS_FAILURE_ABORTED) {
                    Toast.makeText(context, context.getString(R.string.installer_helper_install_error, msg), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
