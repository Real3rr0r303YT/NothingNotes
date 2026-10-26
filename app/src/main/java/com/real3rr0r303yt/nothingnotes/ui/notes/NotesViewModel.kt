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

package com.real3rr0r303yt.nothingnotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.real3rr0r303yt.nothingnotes.appContainer
import com.real3rr0r303yt.nothingnotes.data.Note
import com.real3rr0r303yt.nothingnotes.data.NoteRepository
import com.real3rr0r303yt.nothingnotes.data.SettingsRepository
import com.real3rr0r303yt.nothingnotes.data.ViewMode
import com.real3rr0r303yt.nothingnotes.markdown.Checklist
import com.real3rr0r303yt.nothingnotes.markdown.Markdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A note prepared for display in the overview. */
data class NoteCardModel(
    val note: Note,
    val preview: String,
    val progress: Checklist.Progress,
)

data class NotesUiState(
    val loading: Boolean = true,
    val pinned: List<NoteCardModel> = emptyList(),
    val others: List<NoteCardModel> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val viewMode: ViewMode = ViewMode.GRID,
) {
    val isEmpty: Boolean get() = pinned.isEmpty() && others.isEmpty()
}

class NotesViewModel(
    private val repository: NoteRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val cards = repository.notes.map { notes ->
        notes.map { note ->
            NoteCardModel(
                note = note,
                preview = Markdown.toPlainText(note.content).take(PREVIEW_CHARS),
                progress = Checklist.progress(note.content),
            )
        }
    }

    val uiState: StateFlow<NotesUiState> = combine(cards, query, settings.viewMode) { all, q, mode ->
        val filtered = if (q.isBlank()) all else all.filter { it.matches(q.trim()) }
        NotesUiState(
            loading = false,
            pinned = filtered.filter { it.note.pinned },
            others = filtered.filterNot { it.note.pinned },
            totalCount = all.size,
            query = q,
            viewMode = mode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun toggleViewMode() = viewModelScope.launch {
        val next = if (uiState.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        settings.setViewMode(next)
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.setPinned(note.id, !note.pinned)
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note.id)
    }

    private fun NoteCardModel.matches(q: String) =
        note.title.contains(q, ignoreCase = true) || note.content.contains(q, ignoreCase = true)

    companion object {
        private const val PREVIEW_CHARS = 400

        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                NotesViewModel(container.notes, container.settings)
            }
        }
    }
}
