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

/** To-do helpers working directly on the Markdown source (`- [ ]` / `- [x]`). */
object Checklist {

    private val TODO_MARK = Regex("""^(\s*[-*+]\s\[)([ xX])(])""")

    data class Progress(val done: Int, val total: Int) {
        val hasTasks: Boolean get() = total > 0
    }

    fun progress(source: String): Progress {
        var done = 0
        var total = 0
        for (line in source.lineSequence()) {
            val m = TODO_MARK.find(line) ?: continue
            total++
            if (m.groupValues[2].isNotBlank()) done++
        }
        return Progress(done, total)
    }

    /** Flips the checkbox on [lineIndex]; returns [source] unchanged if it is no to-do line. */
    fun toggle(source: String, lineIndex: Int): String {
        val lines = source.split('\n').toMutableList()
        val line = lines.getOrNull(lineIndex) ?: return source
        val m = TODO_MARK.find(line) ?: return source
        val newMark = if (m.groupValues[2].isBlank()) "x" else " "
        lines[lineIndex] = line.replaceRange(m.groups[2]!!.range, newMark)
        return lines.joinToString("\n")
    }
}
