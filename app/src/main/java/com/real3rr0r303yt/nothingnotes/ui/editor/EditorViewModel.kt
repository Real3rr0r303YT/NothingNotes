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

package com.real3rr0r303yt.nothingnotes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.real3rr0r303yt.nothingnotes.appContainer
import com.real3rr0r303yt.nothingnotes.data.Note
import com.real3rr0r303yt.nothingnotes.data.NoteRepository
import com.real3rr0r303yt.nothingnotes.markdown.Checklist
import com.real3rr0r303yt.nothingnotes.markdown.MarkdownEditing
import com.real3rr0r303yt.nothingnotes.markdown.TextEdit
import com.real3rr0r303yt.nothingnotes.ui.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class FormatAction { HEADING, BOLD, ITALIC, STRIKE, BULLET, TODO, CODE, QUOTE }

/**
 * Holds the note being edited and auto-saves it shortly after every change.
 * Saves run in the application scope so they survive leaving the screen.
 */
class EditorViewModel(
    private val savedState: SavedStateHandle,
    private val repository: NoteRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    var title by mutableStateOf(TextFieldValue(""))
        private set
    var content by mutableStateOf(TextFieldValue(""))
        private set
    var pinned by mutableStateOf(false)
        private set
    var preview by mutableStateOf(false)
        private set
    var updatedAt by mutableStateOf<Long?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set

    private var noteId: Long = savedState.get<Long>(Routes.ARG_NOTE_ID)?.takeIf { it > 0 } ?: 0L
    private var createdAt = System.currentTimeMillis()

    /** Bumped on every edit; compared with [savedVersion] to know if a save is needed. */
    private var editVersion = 0
    private var savedVersion = 0
    private var deleted = false

    private val saveLock = Mutex()
    private var autosaveJob: Job? = null

    val isNew: Boolean get() = noteId == 0L

    init {
        if (noteId != 0L) {
            viewModelScope.launch {
                repository.get(noteId)?.let { note ->
                    title = TextFieldValue(note.title)
                    content = TextFieldValue(note.content)
                    pinned = note.pinned
                    createdAt = note.createdAt
                    updatedAt = note.updatedAt
                    preview = note.content.isNotBlank()
                } ?: run { noteId = 0L }
                loaded = true
            }
        } else {
            loaded = true
        }
    }

    fun onTitleChange(value: TextFieldValue) {
        val changed = value.text != title.text
        title = value
        if (changed) markDirty()
    }

    fun onContentChange(value: TextFieldValue) {
        val old = content
        var next = value
        if (isSingleNewlineInsert(old, value)) {
            MarkdownEditing.continueList(value.text, value.selection.start)?.let { next = it.toValue() }
        }
        val changed = next.text != old.text
        content = next
        if (changed) markDirty()
    }

    fun format(action: FormatAction) {
        val text = content.text
        val start = content.selection.min
        val end = content.selection.max
        val edit = when (action) {
            FormatAction.BOLD -> MarkdownEditing.wrap(text, start, end, "**")
            FormatAction.ITALIC -> MarkdownEditing.wrap(text, start, end, "*")
            FormatAction.STRIKE -> MarkdownEditing.wrap(text, start, end, "~~")
            FormatAction.CODE -> MarkdownEditing.wrap(text, start, end, "`")
            FormatAction.HEADING -> MarkdownEditing.toggleLinePrefix(text, start, end, "# ")
            FormatAction.BULLET -> MarkdownEditing.toggleLinePrefix(text, start, end, "- ")
            FormatAction.TODO -> MarkdownEditing.toggleLinePrefix(text, start, end, "- [ ] ")
            FormatAction.QUOTE -> MarkdownEditing.toggleLinePrefix(text, start, end, "> ")
        }
        content = edit.toValue()
        markDirty()
    }

    fun toggleTodo(line: Int) {
        val newText = Checklist.toggle(content.text, line)
        if (newText == content.text) return
        content = TextFieldValue(newText, TextRange(content.selection.start.coerceAtMost(newText.length)))
        markDirty()
    }

    fun togglePin() {
        pinned = !pinned
        markDirty()
    }

    fun togglePreview() {
        preview = !preview
    }

    /** Persist immediately; called when leaving the screen or the app goes to background. */
    fun flush() {
        autosaveJob?.cancel()
        appScope.launch { persist(final = true) }
    }

    fun delete() {
        deleted = true
        autosaveJob?.cancel()
        appScope.launch {
            saveLock.withLock { if (noteId != 0L) repository.delete(noteId) }
        }
    }

    override fun onCleared() {
        if (!deleted) flush()
    }

    private fun markDirty() {
        editVersion++
        autosaveJob?.cancel()
        autosaveJob = appScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            persist(final = false)
        }
    }

    private suspend fun persist(final: Boolean) = saveLock.withLock {
        if (deleted) return@withLock
        val version = editVersion
        val note = Note(
            id = noteId,
            title = title.text,
            content = content.text,
            pinned = pinned,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
        )
        if (note.isBlank) {
            // Never keep empty notes around.
            if (final && noteId != 0L) {
                repository.delete(noteId)
                noteId = 0L
            }
            return@withLock
        }
        if (version == savedVersion) return@withLock
        noteId = repository.save(note)
        savedState[Routes.ARG_NOTE_ID] = noteId
        savedVersion = version
        updatedAt = note.updatedAt
    }

    private fun isSingleNewlineInsert(old: TextFieldValue, new: TextFieldValue): Boolean {
        if (new.text.length != old.text.length + 1 || !new.selection.collapsed) return false
        val cursor = new.selection.start
        if (cursor <= 0 || new.text[cursor - 1] != '\n') return false
        return new.text.removeRange(cursor - 1, cursor) == old.text
    }

    private fun TextEdit.toValue() = TextFieldValue(text, TextRange(selectionStart, selectionEnd))

    companion object {
        private const val AUTOSAVE_DELAY_MS = 700L

        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                EditorViewModel(createSavedStateHandle(), container.notes, container.appScope)
            }
        }
    }
}
