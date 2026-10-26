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

package com.real3rr0r303yt.nothingnotes

import android.app.Application
import android.content.Context
import com.real3rr0r303yt.nothingnotes.data.NoteRepository
import com.real3rr0r303yt.nothingnotes.data.NotesDatabase
import com.real3rr0r303yt.nothingnotes.data.SettingsRepository
import com.real3rr0r303yt.nothingnotes.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NothingNotesApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Minimal manual dependency container – no DI framework, no reflection magic. */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val crypto = CryptoManager()
    val database = NotesDatabase.create(context)
    val notes = NoteRepository(database, crypto, settings)

    /** Outlives screens, so pending saves finish even when a ViewModel is cleared. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val cacheDirs = listOf(context.cacheDir)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as NothingNotesApp).container
