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

import androidx.room.withTransaction
import com.real3rr0r303yt.nothingnotes.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Single source of truth for notes. Encrypts/decrypts transparently so the
 * rest of the app only ever sees [Note] with plain text.
 */
class NoteRepository(
    private val database: NotesDatabase,
    private val crypto: CryptoManager,
    private val settings: SettingsRepository,
) {
    private val dao = database.noteDao()

    /** Serialises writes against encryption-mode switches. */
    private val writeLock = Mutex()

    val notes: Flow<List<Note>> = dao.observeAll()
        .map { rows -> rows.map(::decode) }
        .flowOn(Dispatchers.Default)

    suspend fun get(id: Long): Note? = withContext(Dispatchers.IO) {
        dao.getById(id)?.let(::decode)
    }

    /** Inserts or updates [note] and returns its id. */
    suspend fun save(note: Note): Long = writeLock.withLock {
        withContext(Dispatchers.IO) {
            val encrypt = settings.isEncryptionEnabled()
            val id = dao.upsert(encode(note, encrypt))
            if (note.id != 0L) note.id else id
        }
    }

    suspend fun setPinned(id: Long, pinned: Boolean) = withContext(Dispatchers.IO) {
        dao.setPinned(id, pinned)
    }

    suspend fun delete(id: Long) = writeLock.withLock {
        withContext(Dispatchers.IO) { dao.deleteById(id) }
    }

    /** Re-encodes every stored note for the new mode, then persists the mode. */
    suspend fun setEncryptionEnabled(enabled: Boolean) = writeLock.withLock {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val migrated = dao.getAll().map { encode(decode(it), enabled) }
                dao.updateAll(migrated)
            }
            settings.setEncryptionEnabled(enabled)
            if (!enabled) {
                // Plain text again – drop the unused key and scrub the old ciphertext pages.
                crypto.deleteKey()
            }
            database.compact()
        }
    }

    /** "Delete all data": notes, settings, key material and caches. */
    suspend fun wipeEverything(cacheDirs: List<java.io.File>) = writeLock.withLock {
        withContext(Dispatchers.IO) {
            dao.deleteAll()
            database.compact()
            crypto.deleteKey()
            settings.clear()
            cacheDirs.forEach { dir -> dir.listFiles()?.forEach { it.deleteRecursively() } }
        }
    }

    private fun decode(row: NoteEntity): Note {
        val title = if (row.encrypted) crypto.decryptOrNull(row.title) else row.title
        val content = if (row.encrypted) crypto.decryptOrNull(row.content) else row.content
        return Note(
            id = row.id,
            title = title ?: UNREADABLE,
            content = content ?: "",
            pinned = row.pinned,
            encrypted = row.encrypted,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
        )
    }

    private fun encode(note: Note, encrypt: Boolean) = NoteEntity(
        id = note.id,
        title = if (encrypt) crypto.encrypt(note.title) else note.title,
        content = if (encrypt) crypto.encrypt(note.content) else note.content,
        pinned = note.pinned,
        encrypted = encrypt,
        createdAt = note.createdAt,
        updatedAt = note.updatedAt,
    )

    private companion object {
        /** Shown if a row can no longer be decrypted (e.g. key removed by the OS). */
        const val UNREADABLE = "⚠ ••••••"
    }
}
