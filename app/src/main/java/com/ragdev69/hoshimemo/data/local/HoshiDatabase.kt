package com.ragdev69.hoshimemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, DiaryEntity::class, TodoEntity::class, TodoGroupEntity::class],
    version = 3,
    exportSchema = false
)
abstract class HoshiDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun diaryDao(): DiaryDao
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile private var INSTANCE: HoshiDatabase? = null

        // Migration from version 1 → 2: adds diary_entries table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS diary_entries (
                        dateKey TEXT NOT NULL PRIMARY KEY,
                        diaryText TEXT NOT NULL DEFAULT '',
                        codeText TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migration from version 2 → 3: adds todo_items and todo_groups tables
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todo_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL DEFAULT '',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        isFailed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        priority INTEGER NOT NULL DEFAULT 0,
                        timerSeconds INTEGER NOT NULL DEFAULT 0,
                        timerStartedAt INTEGER NOT NULL DEFAULT 0,
                        groupId TEXT NOT NULL DEFAULT '',
                        groupName TEXT NOT NULL DEFAULT '',
                        orderInGroup INTEGER NOT NULL DEFAULT 0,
                        isSynced INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todo_groups (
                        groupId TEXT NOT NULL PRIMARY KEY,
                        groupName TEXT NOT NULL DEFAULT '',
                        userId TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): HoshiDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HoshiDatabase::class.java,
                    "hoshi_notes.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}