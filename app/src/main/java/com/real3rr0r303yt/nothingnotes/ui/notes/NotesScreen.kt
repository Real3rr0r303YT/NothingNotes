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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.real3rr0r303yt.nothingnotes.R
import com.real3rr0r303yt.nothingnotes.data.Note
import com.real3rr0r303yt.nothingnotes.data.ViewMode
import com.real3rr0r303yt.nothingnotes.ui.components.CardContainer
import com.real3rr0r303yt.nothingnotes.ui.components.ConfirmDialog
import com.real3rr0r303yt.nothingnotes.ui.components.DotMatrixText
import com.real3rr0r303yt.nothingnotes.ui.components.EmptyState
import com.real3rr0r303yt.nothingnotes.ui.components.NothingIconButton
import com.real3rr0r303yt.nothingnotes.ui.components.NothingTopBar
import com.real3rr0r303yt.nothingnotes.ui.components.RedDot
import com.real3rr0r303yt.nothingnotes.ui.components.SectionLabel
import com.real3rr0r303yt.nothingnotes.ui.components.nothingCardStroke
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingGreyDim
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingOutline
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingRed
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingSurface
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingWhite
import com.real3rr0r303yt.nothingnotes.util.formatTimestamp

@Composable
fun NotesScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Note?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NothingTopBar(title = stringResource(R.string.notes_title)) {
                NothingIconButton(
                    icon = if (searching) Icons.Outlined.Close else Icons.Outlined.Search,
                    contentDescription = stringResource(
                        if (searching) R.string.action_close_search else R.string.action_search,
                    ),
                    onClick = {
                        if (searching) viewModel.onQueryChange("")
                        searching = !searching
                    },
                )
                NothingIconButton(
                    icon = if (state.viewMode == ViewMode.GRID) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                    contentDescription = stringResource(
                        if (state.viewMode == ViewMode.GRID) R.string.action_list_view else R.string.action_grid_view,
                    ),
                    onClick = { viewModel.toggleViewMode() },
                )
                NothingIconButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.action_settings),
                    onClick = onOpenSettings,
                )
            }

            Text(
                text = pluralStringResource(R.plurals.notes_count, state.totalCount, state.totalCount).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = NothingGrey,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )

            if (searching) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            when {
                state.loading -> Unit
                state.isEmpty -> EmptyState(
                    title = stringResource(
                        if (state.query.isBlank()) R.string.notes_empty_title else R.string.notes_search_empty,
                    ),
                    hint = if (state.query.isBlank()) stringResource(R.string.notes_empty_hint) else null,
                    modifier = Modifier.fillMaxSize().imePadding(),
                )
                else -> NotesGrid(
                    state = state,
                    onOpen = { onOpenNote(it.id) },
                    onTogglePin = viewModel::togglePin,
                    onDelete = { pendingDelete = it },
                )
            }
        }

        FloatingActionButton(
            onClick = onNewNote,
            containerColor = NothingRed,
            contentColor = NothingWhite,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            Icon(Icons.Outlined.Add, stringResource(R.string.action_new_note))
        }
    }

    pendingDelete?.let { note ->
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_note_title),
            text = stringResource(R.string.dialog_delete_note_text),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(note)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.notes_search_hint), color = NothingGreyDim) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = NothingGrey) },
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NothingRed,
            unfocusedBorderColor = NothingOutline,
            cursorColor = NothingRed,
            focusedContainerColor = NothingSurface,
            unfocusedContainerColor = NothingSurface,
        ),
        modifier = modifier.fillMaxWidth().focusRequester(focusRequester),
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun NotesGrid(
    state: NotesUiState,
    onOpen: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onDelete: (Note) -> Unit,
) {
    val columns = if (state.viewMode == ViewMode.GRID) 2 else 1
    val showSections = state.pinned.isNotEmpty() && state.others.isNotEmpty()

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        modifier = Modifier.fillMaxSize().imePadding(),
    ) {
        if (state.pinned.isNotEmpty()) {
            if (showSections) {
                item(key = "h_pinned", span = StaggeredGridItemSpan.FullLine) {
                    SectionLabel(stringResource(R.string.notes_pinned), Modifier.padding(top = 0.dp))
                }
            }
            items(state.pinned, key = { it.note.id }) { card ->
                NoteCard(card, state.viewMode, onOpen, onTogglePin, onDelete)
            }
        }
        if (state.others.isNotEmpty()) {
            if (showSections) {
                item(key = "h_others", span = StaggeredGridItemSpan.FullLine) {
                    SectionLabel(stringResource(R.string.notes_others))
                }
            }
            items(state.others, key = { it.note.id }) { card ->
                NoteCard(card, state.viewMode, onOpen, onTogglePin, onDelete)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    card: NoteCardModel,
    viewMode: ViewMode,
    onOpen: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onDelete: (Note) -> Unit,
) {
    val note = card.note
    var menuOpen by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val previewLines = if (viewMode == ViewMode.GRID) 8 else 3

    Box {
        Card(
            shape = MaterialTheme.shapes.medium,
            border = nothingCardStroke(highlight = note.pinned),
            colors = CardDefaults.cardColors(containerColor = CardContainer),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onOpen(note) }, onLongClick = { menuOpen = true }),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.notes_untitled) },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (note.title.isBlank()) NothingGrey else NothingWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.pinned) {
                        Spacer(Modifier.width(8.dp))
                        RedDot(size = 6.dp)
                    }
                }
                if (card.preview.isNotBlank()) {
                    Text(
                        text = card.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NothingGrey,
                        maxLines = previewLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (card.progress.hasTasks) {
                    TaskProgressBar(card.progress.done, card.progress.total)
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixText(
                        text = formatTimestamp(note.updatedAt, locale),
                        dotSize = 1.6.dp,
                        gap = 0.6.dp,
                        color = NothingGreyDim,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (note.encrypted) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Outlined.Lock, null, tint = NothingGreyDim, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = NothingSurface,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(if (note.pinned) R.string.action_unpin else R.string.action_pin)) },
                leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                onClick = {
                    menuOpen = false
                    onTogglePin(note)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = NothingRed) },
                leadingIcon = { Icon(Icons.Outlined.Close, null, tint = NothingRed) },
                onClick = {
                    menuOpen = false
                    onDelete(note)
                },
            )
        }
    }
}

/** Segmented "dot" progress bar for to-do lists. */
@Composable
fun TaskProgressBar(done: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val shown = total.coerceAtMost(12)
        val doneShown = if (total <= 12) done else (done * 12f / total).toInt()
        repeat(shown) { i ->
            RedDot(size = 6.dp, color = if (i < doneShown) NothingRed else NothingOutline)
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(R.string.editor_tasks_done, done, total),
            style = MaterialTheme.typography.labelSmall,
            color = NothingGrey,
        )
    }
}
