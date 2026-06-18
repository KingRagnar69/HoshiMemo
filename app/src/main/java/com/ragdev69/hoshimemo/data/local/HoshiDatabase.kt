package com.ragdev69.hoshimemo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class HoshiDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var INSTANCE: HoshiDatabase? = null

        fun getInstance(context: Context): HoshiDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HoshiDatabase::class.java,
                    "hoshi_notes.db"
                ).build().also { INSTANCE = it }
            }
    }
}