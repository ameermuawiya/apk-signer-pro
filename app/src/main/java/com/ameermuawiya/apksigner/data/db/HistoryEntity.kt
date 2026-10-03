package com.ameermuawiya.apksigner.data.db

/**
 * Data entity representing a signed APK file history record with app metadata.
 */
data class HistoryEntity(
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val appName: String = "",
    val packageName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
