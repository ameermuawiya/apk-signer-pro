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

    /**
     * Initializes database table structure for signing history storage.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FILE_NAME TEXT NOT NULL,
                $COL_FILE_PATH TEXT NOT NULL,
                $COL_APP_NAME TEXT NOT NULL,
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_KEY_ALIAS TEXT NOT NULL DEFAULT 'androiddebugkey',
                $COL_SCHEMES TEXT NOT NULL DEFAULT '',
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    /**
     * Handles schema migration across database version upgrades gracefully.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_KEY_ALIAS TEXT NOT NULL DEFAULT 'androiddebugkey'")
            } catch (ignored: Exception) {}
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_SCHEMES TEXT NOT NULL DEFAULT ''")
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Returns history data access object interface.
     */
    fun historyDao(): HistoryDao = this

    /**
     * Exposes hot StateFlow for history records observable by the UI layer.
     */
    override fun getAllHistory(): Flow<List<HistoryEntity>> = _historyFlow.asStateFlow()

    /**
     * Reads all history records from SQLite database ordered chronologically.
     */
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
            val aliasIdx = cursor.getColumnIndex(COL_KEY_ALIAS)
            val schemesIdx = cursor.getColumnIndex(COL_SCHEMES)
            val timeIdx = cursor.getColumnIndexOrThrow(COL_TIMESTAMP)

            while (cursor.moveToNext()) {
                val alias = if (aliasIdx != -1) cursor.getString(aliasIdx) ?: "androiddebugkey" else "androiddebugkey"
                val schemes = if (schemesIdx != -1) cursor.getString(schemesIdx) ?: "" else ""
                list.add(
                    HistoryEntity(
                        id = cursor.getLong(idIdx),
                        fileName = cursor.getString(nameIdx),
                        filePath = cursor.getString(pathIdx),
                        appName = cursor.getString(appIdx),
                        packageName = cursor.getString(pkgIdx),
                        keyAlias = alias,
                        schemes = schemes,
                        timestamp = cursor.getLong(timeIdx)
                    )
                )
            }
        }
        return list
    }

    /**
     * Refreshes in-memory history StateFlow from current SQLite database content.
     */
    private fun refreshHistory() {
        try {
            val items = loadAllFromDb()
            _historyFlow.value = items
        } catch (e: Exception) {
            _historyFlow.value = emptyList()
        }
    }

    /**
     * Inserts new history entity into SQLite and triggers observer flow updates.
     */
    override suspend fun insertHistory(history: HistoryEntity): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FILE_NAME, history.fileName)
            put(COL_FILE_PATH, history.filePath)
            put(COL_APP_NAME, history.appName)
            put(COL_PACKAGE_NAME, history.packageName)
            put(COL_KEY_ALIAS, history.keyAlias)
            put(COL_SCHEMES, history.schemes)
            put(COL_TIMESTAMP, history.timestamp)
        }
        val id = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshHistory()
        id
    }

    /**
     * Removes history row by ID from SQLite and synchronizes flow state.
     */
    override suspend fun deleteHistoryById(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
        refreshHistory()
    }

    /**
     * Purges all rows from history table and empties active flow state.
     */
    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
        refreshHistory()
    }

    companion object {
        private const val DATABASE_NAME = "apk_signer_db"
        private const val DATABASE_VERSION = 3
        private const val TABLE_NAME = "signing_history"

        private const val COL_ID = "id"
        private const val COL_FILE_NAME = "fileName"
        private const val COL_FILE_PATH = "filePath"
        private const val COL_APP_NAME = "appName"
        private const val COL_PACKAGE_NAME = "packageName"
        private const val COL_KEY_ALIAS = "keyAlias"
        private const val COL_SCHEMES = "schemes"
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
