package com.ameermuawiya.apksigner.data.repository

import com.ameermuawiya.apksigner.data.db.HistoryDao
import com.ameermuawiya.apksigner.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer isolating history database operations from UI view models.
 */
class HistoryRepository(private val historyDao: HistoryDao) {

    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    /**
     * Inserts new signing record into history table.
     */
    suspend fun addHistory(
        fileName: String,
        filePath: String,
        appName: String = "",
        packageName: String = ""
    ): Long {
        return historyDao.insertHistory(
            HistoryEntity(
                fileName = fileName,
                filePath = filePath,
                appName = appName,
                packageName = packageName
            )
        )
    }

    /**
     * Deletes history entry by item id.
     */
    suspend fun deleteHistory(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    /**
     * Clears all recorded history entries.
     */
    suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
