/*
 * NothingNotes – privacy-first notes for Android
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.real3rr0r303yt.nothingnotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    /**
     * Rewrites the database file without the freed pages and truncates the
     * WAL, so deleted notes do not linger on disk.
     */
    fun compact() {
        val db = openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
        db.execSQL("VACUUM")
    }

    companion object {
        const val NAME = "nothingnotes.db"

        fun create(context: Context): NotesDatabase =
            Room.databaseBuilder(context, NotesDatabase::class.java, NAME)
                .addCallback(SecureDeleteCallback)
                .build()
    }

    /** Makes SQLite overwrite deleted content with zeros instead of just unlinking it. */
    private object SecureDeleteCallback : Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.query("PRAGMA secure_delete = ON").close()
        }
    }
}
