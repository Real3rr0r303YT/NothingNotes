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

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Row as stored in SQLite. When [encrypted] is true, [title] and [content]
 * hold Base64 encoded AES-256-GCM ciphertext instead of plain text.
 */
@Entity(tableName = "notes", indices = [Index("pinned"), Index("updatedAt")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val pinned: Boolean,
    val encrypted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Decrypted, UI-facing note model. */
data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val pinned: Boolean = false,
    val encrypted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    val isBlank: Boolean get() = title.isBlank() && content.isBlank()
}
