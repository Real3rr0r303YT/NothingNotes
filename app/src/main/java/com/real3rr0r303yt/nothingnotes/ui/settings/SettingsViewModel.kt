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

package com.real3rr0r303yt.nothingnotes.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.real3rr0r303yt.nothingnotes.AppContainer
import com.real3rr0r303yt.nothingnotes.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppLanguage(val tag: String) { SYSTEM(""), ENGLISH("en"), GERMAN("de") }

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val encryptionEnabled: StateFlow<Boolean> = container.settings.encryptionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val secureScreen: StateFlow<Boolean> = container.settings.secureScreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _busy = MutableStateFlow(false)
    /** True while notes are being re-encrypted or wiped. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _language = MutableStateFlow(currentLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        // AppCompat persists the choice and recreates the activity with the new locale.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun setEncryption(enabled: Boolean) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                container.notes.setEncryptionEnabled(enabled)
            } finally {
                _busy.value = false
            }
        }
    }

    fun setSecureScreen(enabled: Boolean) = viewModelScope.launch {
        container.settings.setSecureScreen(enabled)
    }

    fun deleteAllData(onDone: () -> Unit) {
        if (_busy.value) return
        // App scope: the wipe must complete even if the screen goes away.
        container.appScope.launch {
            _busy.value = true
            try {
                container.notes.wipeEverything(container.cacheDirs)
            } finally {
                _busy.value = false
            }
            onDone()
            if (_language.value != AppLanguage.SYSTEM) setLanguage(AppLanguage.SYSTEM)
        }
    }

    private fun currentLanguage(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language
        return AppLanguage.entries.firstOrNull { it.tag.isNotEmpty() && it.tag == tag } ?: AppLanguage.SYSTEM
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY]!!.appContainer) }
        }
    }
}
