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

package com.real3rr0r303yt.nothingnotes.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.real3rr0r303yt.nothingnotes.ui.components.dotGridBackground
import com.real3rr0r303yt.nothingnotes.ui.editor.EditorScreen
import com.real3rr0r303yt.nothingnotes.ui.notes.NotesScreen
import com.real3rr0r303yt.nothingnotes.ui.settings.SettingsScreen

object Routes {
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val ARG_NOTE_ID = "noteId"
    const val EDITOR = "editor?$ARG_NOTE_ID={$ARG_NOTE_ID}"
    const val NEW_NOTE = -1L

    fun editor(noteId: Long = NEW_NOTE) = "editor?$ARG_NOTE_ID=$noteId"
}

@Composable
fun NothingNotesNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.NOTES,
        modifier = Modifier.fillMaxSize().dotGridBackground(),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(Routes.NOTES) {
            NotesScreen(
                onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                onNewNote = { navController.navigate(Routes.editor()) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument(Routes.ARG_NOTE_ID) {
                    type = NavType.LongType
                    defaultValue = Routes.NEW_NOTE
                },
            ),
        ) {
            EditorScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
