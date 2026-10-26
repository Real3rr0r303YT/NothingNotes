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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ViewMode { GRID, LIST }

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Small local key/value settings. The app language is not stored here:
 * AppCompat persists it itself (see AppLocalesMetadataHolderService in the manifest).
 */
class SettingsRepository(context: Context) {

    private val store = context.dataStore

    val viewMode: Flow<ViewMode> = store.data.map { prefs ->
        prefs[KEY_VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.GRID
    }

    val encryptionEnabled: Flow<Boolean> = store.data.map { it[KEY_ENCRYPTION] ?: false }

    val secureScreen: Flow<Boolean> = store.data.map { it[KEY_SECURE_SCREEN] ?: true }

    suspend fun isEncryptionEnabled(): Boolean = encryptionEnabled.first()

    suspend fun setViewMode(mode: ViewMode) {
        store.edit { it[KEY_VIEW_MODE] = mode.name }
    }

    suspend fun setEncryptionEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENCRYPTION] = enabled }
    }

    suspend fun setSecureScreen(enabled: Boolean) {
        store.edit { it[KEY_SECURE_SCREEN] = enabled }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    private companion object {
        val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
        val KEY_ENCRYPTION = booleanPreferencesKey("encryption_enabled")
        val KEY_SECURE_SCREEN = booleanPreferencesKey("secure_screen")
    }
}
