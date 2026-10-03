package com.ameermuawiya.apksigner.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Main SQLite database instance for local persistence.
 */
class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), HistoryDao {

    private val _historyFlow = MutableStateFlow<List<HistoryEntity>>(emptyList())

    init {
        refreshHistory()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FILE_NAME TEXT NOT NULL,
                $COL_FILE_PATH TEXT NOT NULL,
                $COL_APP_NAME TEXT NOT NULL,
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun historyDao(): HistoryDao = this

    override fun getAllHistory(): Flow<List<HistoryEntity>> = _historyFlow.asStateFlow()

    private fun loadAllFromDb(): List<HistoryEntity> {
        val list = mutableListOf<HistoryEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(COL_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(COL_FILE_NAME)
            val pathIdx = cursor.getColumnIndexOrThrow(COL_FILE_PATH)
            val appIdx = cursor.getColumnIndexOrThrow(COL_APP_NAME)
            val pkgIdx = cursor.getColumnIndexOrThrow(COL_PACKAGE_NAME)
            val timeIdx = cursor.getColumnIndexOrThrow(COL_TIMESTAMP)

            while (cursor.moveToNext()) {
                list.add(
                    HistoryEntity(
                        id = cursor.getLong(idIdx),
                        fileName = cursor.getString(nameIdx),
                        filePath = cursor.getString(pathIdx),
                        appName = cursor.getString(appIdx),
                        packageName = cursor.getString(pkgIdx),
                        timestamp = cursor.getLong(timeIdx)
                    )
                )
            }
        }
        return list
    }

    private fun refreshHistory() {
        try {
            val items = loadAllFromDb()
            _historyFlow.value = items
        } catch (e: Exception) {
            _historyFlow.value = emptyList()
        }
    }

    override suspend fun insertHistory(history: HistoryEntity): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FILE_NAME, history.fileName)
            put(COL_FILE_PATH, history.filePath)
            put(COL_APP_NAME, history.appName)
            put(COL_PACKAGE_NAME, history.packageName)
            put(COL_TIMESTAMP, history.timestamp)
        }
        val id = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshHistory()
        id
    }

    override suspend fun deleteHistoryById(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
        refreshHistory()
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
        refreshHistory()
    }

    companion object {
        private const val DATABASE_NAME = "apk_signer_db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "signing_history"

        private const val COL_ID = "id"
        private const val COL_FILE_NAME = "fileName"
        private const val COL_FILE_PATH = "filePath"
        private const val COL_APP_NAME = "appName"
        private const val COL_PACKAGE_NAME = "packageName"
        private const val COL_TIMESTAMP = "timestamp"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns singleton instance of AppDatabase for thread-safe data operations.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
