package com.ragdev69.hoshimemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, DiaryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HoshiDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun diaryDao(): DiaryDao

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

        fun getInstance(context: Context): HoshiDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HoshiDatabase::class.java,
                    "hoshi_notes.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}