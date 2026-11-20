/*
 * NothingNotes – privacy-first notes for Android
 * Copyright (C) 2026 Real3rr0r303YT – Made by 3rr0r303YT on TikTok
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

package com.real3rr0r303yt.nothingnotes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.real3rr0r303yt.nothingnotes.MainActivity
import com.real3rr0r303yt.nothingnotes.R
import com.real3rr0r303yt.nothingnotes.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * "NOTES" in dot-matrix, how many notes / pinned, and two buttons: new note and open.
 *
 * Privacy: the widget never shows titles or content – notes may be encrypted and the app
 * blocks screenshots, so nothing readable ever lands on the home or lock screen.
 */
class NotesWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAll(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, NotesWidget::class.java))
            if (ids.isNotEmpty()) scope.launch { render(app, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val dao = context.appContainer.database.noteDao()
            val total = dao.count()
            val pinned = dao.pinnedCount()
            val density = context.resources.displayMetrics.density
            val locale = context.resources.configuration.locales[0]

            val views = RemoteViews(context.packageName, R.layout.widget_notes)
            views.setImageViewBitmap(R.id.widget_title, DotRender.text("NOTES", 5f * density))
            val count = context.resources.getQuantityString(R.plurals.widget_notes_count, total, total)
            val info = if (pinned > 0) "$count • ${context.getString(R.string.widget_notes_pinned, pinned)}" else count
            views.setTextViewText(R.id.widget_info, info.uppercase(locale))

            views.setOnClickPendingIntent(R.id.widget_root, activity(context, 0, newNote = false))
            views.setOnClickPendingIntent(R.id.widget_open, activity(context, 0, newNote = false))
            views.setOnClickPendingIntent(R.id.widget_new, activity(context, 1, newNote = true))
            manager.updateAppWidget(ids, views)
        }

        private fun activity(context: Context, request: Int, newNote: Boolean): PendingIntent =
            PendingIntent.getActivity(
                context, request,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_NEW_NOTE, newNote)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
