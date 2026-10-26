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

package com.real3rr0r303yt.nothingnotes.markdown

/**
 * Pure text transformations behind the editor's formatting toolbar.
 * Selections are half-open [start, end) character offsets.
 */
data class TextEdit(val text: String, val selectionStart: Int, val selectionEnd: Int)

object MarkdownEditing {

    private val LIST_PREFIX = Regex("""^(\s*)([-*+]\s\[[ xX]]\s|[-*+]\s|(\d+)([.)])\s|>\s?)""")

    /** Wraps the selection in [marker] (e.g. `**`), or inserts an empty pair at the cursor. */
    fun wrap(text: String, start: Int, end: Int, marker: String): TextEdit {
        val s = minOf(start, end)
        val e = maxOf(start, end)
        val selected = text.substring(s, e)
        val before = text.substring(0, s)
        val after = text.substring(e)
        // Toggle off if the selection is already wrapped.
        if (before.endsWith(marker) && after.startsWith(marker)) {
            val newText = before.dropLast(marker.length) + selected + after.drop(marker.length)
            return TextEdit(newText, s - marker.length, e - marker.length)
        }
        val newText = before + marker + selected + marker + after
        return TextEdit(newText, s + marker.length, e + marker.length)
    }

    /**
     * Toggles a line prefix (e.g. `# `, `- `, `- [ ] `, `> `) on every line touched by the
     * selection. Any other list/quote prefix already present is replaced.
     */
    fun toggleLinePrefix(text: String, start: Int, end: Int, prefix: String): TextEdit {
        val s = minOf(start, end)
        val e = maxOf(start, end)
        val lineStart = text.lastIndexOf('\n', s - 1).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', e).let { if (it < 0) text.length else it }
        val lines = text.substring(lineStart, lineEnd).split('\n')
        val removeAll = lines.all { it.startsWith(prefix) }

        var delta = 0
        var firstDelta = 0
        val newLines = lines.mapIndexed { index, line ->
            val replaced = when {
                removeAll -> line.removePrefix(prefix)
                else -> prefix + stripPrefix(line)
            }
            if (index == 0) firstDelta = replaced.length - line.length
            delta += replaced.length - line.length
            replaced
        }
        val newText = text.substring(0, lineStart) + newLines.joinToString("\n") + text.substring(lineEnd)
        val newStart = (s + firstDelta).coerceIn(lineStart, newText.length)
        val newEnd = (e + delta).coerceIn(newStart, newText.length)
        return TextEdit(newText, newStart, newEnd)
    }

    private fun stripPrefix(line: String): String {
        val heading = Regex("""^#{1,3}\s""").find(line)
        if (heading != null) return line.substring(heading.range.last + 1)
        val list = LIST_PREFIX.find(line) ?: return line
        return line.substring(list.range.last + 1)
    }

    /**
     * Called after the user typed a newline at [cursor] (the newline is at cursor-1).
     * Continues bullet/to-do/numbered lists, or ends the list when the previous item
     * was empty. Returns null when nothing should change.
     */
    fun continueList(text: String, cursor: Int): TextEdit? {
        if (cursor <= 0 || cursor > text.length || text[cursor - 1] != '\n') return null
        val prevEnd = cursor - 1
        val prevStart = text.lastIndexOf('\n', prevEnd - 1).let { if (it < 0) 0 else it + 1 }
        val prevLine = text.substring(prevStart, prevEnd)
        val match = LIST_PREFIX.find(prevLine) ?: return null
        val indent = match.groupValues[1]
        val marker = match.groupValues[2]

        if (prevLine.length == match.range.last + 1) {
            // Empty item: remove it and the inserted newline -> leaves the list.
            val newText = text.substring(0, prevStart) + text.substring(cursor)
            return TextEdit(newText, prevStart, prevStart)
        }

        val nextMarker = when {
            marker.contains('[') -> marker.replace(Regex("""\[[xX]]"""), "[ ]")
            match.groupValues[3].isNotEmpty() ->
                "${match.groupValues[3].toInt() + 1}${match.groupValues[4]} "
            else -> marker
        }
        val insert = indent + nextMarker
        val newText = text.substring(0, cursor) + insert + text.substring(cursor)
        return TextEdit(newText, cursor + insert.length, cursor + insert.length)
    }
}
