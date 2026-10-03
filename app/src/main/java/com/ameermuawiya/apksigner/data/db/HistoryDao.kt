package com.ameermuawiya.apksigner.data.db

import kotlinx.coroutines.flow.Flow

/**
 * Data access object for history database operations.
 */
interface HistoryDao {

    /**
     * Retrieves all recorded signing history items ordered by time descending.
     */
    fun getAllHistory(): Flow<List<HistoryEntity>>

    /**
     * Inserts a new signing record into history table.
     */
    suspend fun insertHistory(history: HistoryEntity): Long

    /**
     * Deletes a history record by its primary key id.
     */
    suspend fun deleteHistoryById(id: Long)

    /**
     * Clears all history entries from the database.
     */
    suspend fun clearHistory()
}
