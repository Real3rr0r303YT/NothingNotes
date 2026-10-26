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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StrikethroughS
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.real3rr0r303yt.nothingnotes.R
import com.real3rr0r303yt.nothingnotes.markdown.Checklist
import com.real3rr0r303yt.nothingnotes.ui.components.ConfirmDialog
import com.real3rr0r303yt.nothingnotes.ui.components.NothingIconButton
import com.real3rr0r303yt.nothingnotes.ui.components.NothingTopBar
import com.real3rr0r303yt.nothingnotes.ui.notes.TaskProgressBar
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingGrey
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingGreyDim
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingOutline
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingRed
import com.real3rr0r303yt.nothingnotes.ui.theme.NothingWhite
import com.real3rr0r303yt.nothingnotes.util.formatTimestamp

@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory),
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]

    val leave = {
        viewModel.flush()
        onBack()
    }
    BackHandler(onBack = leave)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }

    Column(Modifier.fillMaxSize()) {
        NothingTopBar(
            title = viewModel.updatedAt?.let { formatTimestamp(it, locale) }
                ?: stringResource(R.string.editor_new),
            onBack = leave,
        ) {
            NothingIconButton(
                icon = if (viewModel.preview) Icons.Outlined.Edit else Icons.Outlined.Visibility,
                contentDescription = stringResource(
                    if (viewModel.preview) R.string.editor_mode_edit else R.string.editor_mode_preview,
                ),
                onClick = viewModel::togglePreview,
            )
            NothingIconButton(
                icon = Icons.Outlined.PushPin,
                contentDescription = stringResource(if (viewModel.pinned) R.string.action_unpin else R.string.action_pin),
                tint = if (viewModel.pinned) NothingRed else NothingWhite,
                onClick = viewModel::togglePin,
            )
            NothingIconButton(
                icon = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(R.string.action_delete),
                onClick = { confirmDelete = true },
            )
        }

        if (!viewModel.loaded) return@Column

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            BasicTextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = NothingWhite),
                cursorBrush = SolidColor(NothingRed),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                readOnly = viewModel.preview,
                decorationBox = { inner ->
                    Box {
                        if (viewModel.title.text.isEmpty()) {
                            Text(
                                stringResource(R.string.editor_title_hint),
                                style = MaterialTheme.typography.headlineMedium,
                                color = NothingGreyDim,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            )

            val progress = Checklist.progress(viewModel.content.text)
            if (progress.hasTasks) {
                TaskProgressBar(progress.done, progress.total, Modifier.padding(bottom = 12.dp))
            }

            if (viewModel.preview) {
                MarkdownView(
                    source = viewModel.content.text,
                    onToggleTodo = viewModel::toggleTodo,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                )
            } else {
                BasicTextField(
                    value = viewModel.content,
                    onValueChange = viewModel::onContentChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = NothingWhite),
                    cursorBrush = SolidColor(NothingRed),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box {
                            if (viewModel.content.text.isEmpty()) {
                                Text(
                                    stringResource(R.string.editor_content_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NothingGreyDim,
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                        .padding(bottom = 32.dp),
                )
            }
        }

        if (!viewModel.preview) {
            FormatToolbar(onAction = viewModel::format)
        } else {
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_note_title),
            text = stringResource(R.string.dialog_delete_note_text),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun FormatToolbar(onAction: (FormatAction) -> Unit) {
    val tools: List<Triple<FormatAction, ImageVector, Int>> = listOf(
        Triple(FormatAction.TODO, Icons.Outlined.CheckBox, R.string.format_todo),
        Triple(FormatAction.HEADING, Icons.Outlined.FormatSize, R.string.format_heading),
        Triple(FormatAction.BOLD, Icons.Outlined.FormatBold, R.string.format_bold),
        Triple(FormatAction.ITALIC, Icons.Outlined.FormatItalic, R.string.format_italic),
        Triple(FormatAction.STRIKE, Icons.Outlined.StrikethroughS, R.string.format_strike),
        Triple(FormatAction.BULLET, Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.format_bullet),
        Triple(FormatAction.QUOTE, Icons.Outlined.FormatQuote, R.string.format_quote),
        Triple(FormatAction.CODE, Icons.Outlined.Code, R.string.format_code),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(1.dp, NothingOutline, CircleShape)
            .height(52.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tools.forEach { (action, icon, label) ->
            NothingIconButton(
                icon = icon,
                contentDescription = stringResource(label),
                onClick = { onAction(action) },
                tint = if (action == FormatAction.TODO) NothingRed else NothingGrey,
            )
        }
    }
}

