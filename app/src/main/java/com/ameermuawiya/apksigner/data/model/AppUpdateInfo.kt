package com.ameermuawiya.apksigner.data.model

/**
 * Data model representing remote GitHub release metadata for app updates.
 */
data class AppUpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val publishedAt: String
)
